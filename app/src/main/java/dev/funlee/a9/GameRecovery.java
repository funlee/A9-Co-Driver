package dev.funlee.a9;

import android.app.KeyguardManager;
import android.accessibilityservice.GestureDescription;
import android.content.*;
import android.content.pm.*;
import android.graphics.Path;
import android.graphics.Rect;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import dev.funlee.a9.core.MatchmakingWatchdog;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

/** Bounded, cancellable recovery. System clicks are semantic and scoped to this game's app-info. */
final class GameRecovery {
    private static final long STARTUP_TIMEOUT_MS = 180_000;
    enum Reason { MATCH_TIMEOUT, FOREGROUND_LOST, INPUT_INTERRUPTED, EVENT_START_TIMEOUT }
    static final class IncompatibleStartup extends Exception {}
    private final Context context;
    private final TouchService touch;
    private final String game,label;
    private final BooleanSupplier stopped,paused;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Set<String> launchers=new HashSet<>();
    private final ArrayDeque<Long> attempts=new ArrayDeque<>();
    private long pausedMillis;
    private final Callable<Boolean> ready;
    GameRecovery(Context context,TouchService touch,String game,BooleanSupplier stopped,BooleanSupplier paused)throws Exception{
        this(context,touch,game,stopped,paused,()->true);
    }
    GameRecovery(Context context,TouchService touch,String game,BooleanSupplier stopped,BooleanSupplier paused,Callable<Boolean> ready)throws Exception{
        this.context=context;this.touch=touch;this.game=game;this.stopped=stopped;this.paused=paused;
        this.ready=ready;
        PackageManager pm=context.getPackageManager();label=pm.getApplicationLabel(pm.getApplicationInfo(game,0)).toString();
        for(ResolveInfo r:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),0))launchers.add(r.activityInfo.packageName);
    }
    private void gate()throws InterruptedException{
        long started=SystemClock.elapsedRealtime();
        while(paused.getAsBoolean()&&!stopped.getAsBoolean()){touch.releaseAll();Thread.sleep(100);}
        pausedMillis+=SystemClock.elapsedRealtime()-started;
        if(stopped.getAsBoolean()||Thread.currentThread().isInterrupted())throw new InterruptedException();
        if(context.getSystemService(KeyguardManager.class).isKeyguardLocked())throw new IllegalStateException("设备已锁屏，已停止自动恢复");
    }
    private long activeTime(){return SystemClock.elapsedRealtime()-pausedMillis;}
    private void status(String message){SessionState.hud=message;SessionState.status(message);}
    private void waitBriefly()throws InterruptedException{gate();Thread.sleep(200);}
    /** false means a transient window change resolved itself; the existing VM can continue. */
    interface RestartPermit { void reserve() throws Exception; }
    boolean recover(Reason reason)throws Exception{return recover(reason,null);}
    boolean recover(Reason reason,RestartPermit permit)throws Exception{
        gate();touch.releaseAll();
        if(reason==Reason.FOREGROUND_LOST){
            status("游戏离开前台，确认闪退状态");
            // A surface/root transition is not by itself a crash.
            long homeSince=-1,otherSince=-1,until=activeTime()+6000;
            int dialogActions=0;
            while(activeTime()<until){
                gate();if(touch.gameInForeground()){
                    if(homeSince>=0){if(permit!=null)permit.reserve();else recordAttempt();restartGame();awaitReady(permit==null);SessionState.log("游戏退出后已重新进入前台，恢复任务检查点");return true;}
                    return false;
                }
                String foreground=touch.foregroundPackage();
                if(launchers.contains(foreground)||context.getPackageName().equals(foreground)){
                    otherSince=-1;
                    if(homeSince<0)homeSince=activeTime();
                    if(activeTime()-homeSince>=1000)break;
                }else{
                    homeSince=-1;
                    if(foreground.equals("android")&&dialogActions<2&&closeCrashDialog()){
                        dialogActions++;otherSince=-1;until=activeTime()+6000;waitBriefly();continue;
                    }
                    if(!foreground.isEmpty()){
                        if(otherSince<0){otherSince=activeTime();SessionState.log("等待退出后的前台窗口稳定："+foreground);}
                        if(activeTime()-otherSince>1800)throw new IllegalStateException("当前前台为 "+foreground+"，不是游戏、桌面或助手；已停止恢复，避免打断其他应用");
                    }
                }
                waitBriefly();
            }
            if(homeSince<0||activeTime()-homeSince<1000)throw new IllegalStateException("无法确认游戏退出到桌面或助手，已停止恢复");
        }else if(!touch.gameInForeground())throw new IllegalStateException("匹配重启前游戏已离开前台，已停止恢复");
        if(permit!=null)permit.reserve();else recordAttempt();
        switch (reason) {
            case MATCH_TIMEOUT: status("匹配超过 "+MatchmakingWatchdog.TIMEOUT_SECONDS+" 秒，正在重启游戏"); break;
            case INPUT_INTERRUPTED: status("触控中断，重启游戏后从检查点继续"); break;
            case EVENT_START_TIMEOUT: status("点击赛事开始后选车未打开，重启游戏后从检查点继续"); break;
            case FOREGROUND_LOST: status("游戏已退出到桌面或助手，正在重新启动"); break;
        }
        if(reason==Reason.FOREGROUND_LOST) launchGame(true);
        else restartGame();
        awaitReady(permit==null);return true;
    }
    private void restartGame()throws Exception{
        // Returning from Home may resume a live race and retain exhausted game FDs.
        // Checkpoint recovery needs a fresh process, even for FOREGROUND_LOST.
        forceStop();
        // Leave our settings task behind before relaunch; a later crash should land on Home.
        if(!onMain(()->touch.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)))
            throw new IllegalStateException("无法退出系统应用信息页，已停止恢复");
        Thread.sleep(350);
        launchGame(false);
    }
    private void launchGame(boolean replaceTask)throws Exception{
        gate();
        Intent launch=context.getPackageManager().getLaunchIntentForPackage(game);
        if(launch==null)throw new IllegalStateException("找不到游戏启动入口");
        // After the game leaves for Home, start a fresh activity task. The in-game
        // timeout paths still stop the process before launching it.
        int flags=Intent.FLAG_ACTIVITY_NEW_TASK|
                (replaceTask?Intent.FLAG_ACTIVITY_CLEAR_TASK:Intent.FLAG_ACTIVITY_CLEAR_TOP);
        onMain(()->{context.startActivity(launch.addFlags(flags));return true;});
    }
    void restartForHome()throws Exception{
        gate();
        if(!touch.gameInForeground())throw new IllegalStateException("游戏不在前台，无法重启并确认主界面");
        status("回主未成功，正在重启游戏");
        restartGame();
        awaitReady(false);
    }
    void awaitReady()throws Exception{awaitReady(true);}
    private void awaitReady(boolean allowRelaunch)throws Exception{
        status("等待游戏登录与菜单加载完成");
        long launchedAt=activeTime(),until=launchedAt+STARTUP_TIMEOUT_MS,stable=-1,homeSince=-1;
        boolean loggedOcrTimeout=false;
        while(activeTime()<until){
            gate();
            if(touch.gameInForeground()){
                homeSince=-1;
                boolean actionable;
                // The foreground callback can precede the projection's landscape resize.
                // Keep waiting within the existing startup timeout for a usable frame.
                try{actionable=ready.call();}
                catch(IncompatibleStartup failure){
                    if(!allowRelaunch)throw new IllegalStateException("重启后游戏报告设备不兼容");
                    recordAttempt();
                    status("游戏启动显示设备不兼容，重新启动并保留任务检查点");
                    restartGame();
                    launchedAt=activeTime();until=launchedAt+STARTUP_TIMEOUT_MS;stable=-1;
                    continue;
                }
                catch(TouchService.ForegroundLost|CaptureSession.OrientationPending transition){actionable=false;}
                catch(TimeoutException slowOcr){
                    if(!loggedOcrTimeout){
                        SessionState.log("重启后的画面识别暂时超时，继续等待游戏菜单");
                        loggedOcrTimeout=true;
                    }
                    actionable=false;
                }
                if(actionable){
                    if(stable<0)stable=activeTime();
                    if(activeTime()-stable>=2000){SessionState.log("已确认游戏菜单加载完成，保留完赛数并继续剩余任务");return;}
                }else stable=-1;
            }else {
                stable=-1;
                String foreground=touch.foregroundPackage();
                if(foreground.equals("android"))closeCrashDialog();
                if(launchers.contains(foreground)||context.getPackageName().equals(foreground)){
                    if(homeSince<0)homeSince=activeTime();
                    // The installer and login activity can briefly give focus back to Home.
                    if(activeTime()-launchedAt>=20_000&&activeTime()-homeSince>=5000){
                        if(!allowRelaunch)throw new IllegalStateException("恢复主界面时游戏再次退出");
                        recordAttempt();
                        status("游戏加载期间再次退出，重新启动并保留任务检查点");
                        restartGame();homeSince=-1;launchedAt=activeTime();
                    }
                }else homeSince=-1;
            }
            waitBriefly();
        }
        throw new IllegalStateException("重启后未能确认游戏菜单加载完成，已停止；已完成局数仍保留");
    }
    private void recordAttempt(){
        long now=SystemClock.elapsedRealtime();
        while(!attempts.isEmpty()&&now-attempts.peekFirst()>600_000)attempts.removeFirst();
        if(attempts.size()>=3)throw new IllegalStateException("10 分钟内已尝试恢复 3 次，已停止以避免反复重启；已完成局数仍保留");
        attempts.addLast(now);
    }
    private void forceStop()throws Exception{
        Intent details=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+game)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_NO_HISTORY);
        ResolveInfo resolved=context.getPackageManager().resolveActivity(details,PackageManager.MATCH_DEFAULT_ONLY);
        if(resolved==null||(resolved.activityInfo.applicationInfo.flags&ApplicationInfo.FLAG_SYSTEM)==0)throw new IllegalStateException("找不到可信的系统应用信息页");
        String settings=resolved.activityInfo.packageName;
        onMain(()->{context.startActivity(details);return true;});
        int phase=0, confirmations=0;
        boolean dismissedOldDialog=false;
        long lastConfirmation=0, until=activeTime()+30_000;
        while(activeTime()<until){
            gate();final int stage=phase;
            // Some Settings builds leave Force stop visually enabled after Android stops the app.
            if ((context.getPackageManager().getApplicationInfo(game, 0).flags & ApplicationInfo.FLAG_STOPPED) != 0) return;
            boolean retryConfirmation=stage==2&&confirmations<3&&activeTime()-lastConfirmation>=1200;
            boolean canDismissOldDialog=stage==0&&!dismissedOldDialog;
            final int[] outcome={0};
            onMain(()->{
                AccessibilityNodeInfo root=settingsRoot(settings);if(root==null)return false;
                try{
                    String all=text(root);
                    if(canDismissOldDialog&&hasStopText(all)&&(all.contains("异常")||all.contains("正常")||all.contains("misbehave"))){
                        // Settings can reuse the last app-info task, including its pending dialog.
                        // Dismiss it before verifying the current target label and sending a fresh stop.
                        AccessibilityNodeInfo cancel=find(root,n->{
                            String title=normalize(n.getText());
                            return title.equals("取消")||title.equals("cancel");
                        });
                        if(cancel!=null){
                            try{if(click(cancel)||tapNode(cancel)){outcome[0]=4;return true;}}
                            finally{cancel.recycle();}
                        }
                    }
                    if((stage==1||retryConfirmation)&&hasStopText(all)){
                        AccessibilityNodeInfo confirm=find(root,n->"android:id/button1".equals(n.getViewIdResourceName())&&confirmLabel(n));
                        if(confirm==null&&(all.contains("异常")||all.contains("正常")||all.contains("misbehave")))
                            confirm=find(root,n->confirmLabel(n)&&!stopLabel(n));
                        if(confirm!=null){
                            try {
                                if (enabled(confirm) && ((stage==1&&click(confirm)) || tapNode(confirm))) {
                                    outcome[0]=2;return true;
                                }
                            } finally {confirm.recycle();}
                        }
                    }
                    if(stage==0&&all.contains(normalize(label))){
                        AccessibilityNodeInfo stop=find(root,GameRecovery::stopLabel);
                        if(stop!=null){try{if(!enabled(stop)){outcome[0]=3;return true;}if(click(stop)||tapNode(stop)){outcome[0]=1;return true;}}finally{stop.recycle();}}
                    }
                    if(stage==2&&all.contains(normalize(label))){
                        AccessibilityNodeInfo stop=find(root,GameRecovery::stopLabel);
                        if(stop!=null){try{if(!enabled(stop)){outcome[0]=3;return true;}}finally{stop.recycle();}}
                    }
                    return false;
                }finally{root.recycle();}
            });
            if(outcome[0]==3)return;
            if(outcome[0]==4)dismissedOldDialog=true;
            if(outcome[0]==1||outcome[0]==2)phase=outcome[0];
            if(outcome[0]==2){confirmations++;lastConfirmation=activeTime();}
            waitBriefly();
        }
        SessionState.log("重启确认超时：应用信息操作阶段 "+phase+"，已发送确认 "+confirmations+" 次");
        throw new IllegalStateException("未能在系统应用信息页确认强行停止，请手动处理；任务计数保留");
    }
    private AccessibilityNodeInfo settingsRoot(String settings) {
        // The Force stop dialog can take window focus before it becomes the active root.
        List<AccessibilityWindowInfo> windows=touch.getWindows();
        try {
            for(AccessibilityWindowInfo window:windows){
                if(!window.isFocused()||window.getType()!=AccessibilityWindowInfo.TYPE_APPLICATION)continue;
                AccessibilityNodeInfo root=window.getRoot();
                if(root==null)continue;
                if(settings.equals(String.valueOf(root.getPackageName())))return root;
                root.recycle();
                return null;
            }
        } finally {
            for(AccessibilityWindowInfo window:windows)window.recycle();
        }
        AccessibilityNodeInfo root=touch.getRootInActiveWindow();
        if(root==null||settings.equals(String.valueOf(root.getPackageName())))return root;
        root.recycle();
        return null;
    }
    private boolean tapNode(AccessibilityNodeInfo button) {
        Rect bounds = new Rect();
        button.getBoundsInScreen(bounds);
        if (bounds.isEmpty()) return false;
        Path path = new Path();
        path.moveTo(bounds.exactCenterX(), bounds.exactCenterY());
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 70)).build();
        return touch.dispatchGesture(gesture, null, null);
    }
    private boolean closeCrashDialog()throws Exception{
        return onMain(()->{
            AccessibilityNodeInfo root=touch.getRootInActiveWindow();if(root==null)return false;
            try{
                if(waitForSystemUi(root))return true;
                if(!"android".contentEquals(root.getPackageName()==null?"":root.getPackageName()))return false;
                String all=text(root);
                if(!all.contains(normalize(label))||!(all.contains("停止运行")||all.contains("keepsstopping")||all.contains("hasstopped")))return false;
                AccessibilityNodeInfo close=find(root,n->"android:id/aerr_close".equals(n.getViewIdResourceName())||normalize(n.getText()).equals("关闭应用")||normalize(n.getText()).equals("closeapp"));
                if(close==null)return false;
                try{return click(close);}finally{close.recycle();}
            }finally{root.recycle();}
        });
    }
    static boolean waitForSystemUi(AccessibilityNodeInfo root){
        if(root==null||!"android".contentEquals(root.getPackageName()==null?"":root.getPackageName()))return false;
        AccessibilityNodeInfo title=find(root,n->"android:id/alertTitle".equals(n.getViewIdResourceName())&&
                systemUiNotResponding(n.getText()));
        if(title==null)return false;
        title.recycle();
        AccessibilityNodeInfo wait=find(root,n->"android:id/aerr_wait".equals(n.getViewIdResourceName())&&
                (normalize(n.getText()).equals("wait")||normalize(n.getText()).equals("等待")));
        if(wait==null)return false;
        try{
            if(!click(wait))return false;
            SessionState.log("系统界面无响应，已选择等待系统恢复");
            return true;
        }finally{wait.recycle();}
    }
    private static boolean systemUiNotResponding(CharSequence title){
        String normalized=normalize(title).replace('’','\'');
        return normalized.equals("systemuiisn'tresponding")||normalized.equals("系统界面无响应")||
                normalized.equals("系统界面没有响应");
    }
    private boolean onMain(Callable<Boolean> action)throws Exception{
        gate();
        FutureTask<Boolean> task=new FutureTask<>(()->{
            if(stopped.getAsBoolean()||paused.getAsBoolean())return false;
            return action.call();
        });
        main.post(task);
        try {
            return task.get(7, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new IllegalStateException("等待系统响应自动恢复操作超时，请重新打开游戏后续跑", e);
        } finally {
            task.cancel(false);
            main.removeCallbacks(task);
        }
    }
    private interface Match {boolean test(AccessibilityNodeInfo node);}
    private static AccessibilityNodeInfo find(AccessibilityNodeInfo node,Match predicate){
        if(node.isVisibleToUser()&&predicate.test(node))return AccessibilityNodeInfo.obtain(node);
        for(int i=0;i<node.getChildCount();i++){
            AccessibilityNodeInfo child=node.getChild(i);if(child==null)continue;
            try{AccessibilityNodeInfo found=find(child,predicate);if(found!=null)return found;}finally{child.recycle();}
        }return null;
    }
    private static String normalize(CharSequence s){return s==null?"":s.toString().replaceAll("\\s+","").toLowerCase(Locale.ROOT);}
    private static String text(AccessibilityNodeInfo node){
        StringBuilder s=new StringBuilder(normalize(node.getText())).append(normalize(node.getContentDescription()));
        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null){try{s.append(text(child));}finally{child.recycle();}}}return s.toString();
    }
    private static boolean hasStopText(String s){return s.contains("强行停止")||s.contains("强制停止")||s.contains("结束运行")||s.contains("forcestop");}
    private static boolean stopLabel(AccessibilityNodeInfo n){String s=normalize(n.getText());return s.equals("强行停止")||s.equals("强制停止")||s.equals("结束运行")||s.equals("forcestop");}
    private static boolean confirmLabel(AccessibilityNodeInfo n){String s=normalize(n.getText());return s.equals("确定")||s.equals("确认")||s.equals("ok")||s.equals("强行停止")||s.equals("强制停止")||s.equals("结束运行");}
    private static boolean enabled(AccessibilityNodeInfo n){
        if(!n.isEnabled())return false;
        AccessibilityNodeInfo parent=n.getParent();try{return parent==null||parent.isEnabled();}finally{if(parent!=null)parent.recycle();}
    }
    private static boolean click(AccessibilityNodeInfo node){
        AccessibilityNodeInfo n=AccessibilityNodeInfo.obtain(node);
        try{for(int depth=0;depth<3&&n!=null;depth++){
            if(!n.isEnabled()||!n.isVisibleToUser())return false;
            if(n.isClickable())return n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            AccessibilityNodeInfo parent=n.getParent();n.recycle();n=parent;
        }return false;}finally{if(n!=null)n.recycle();}
    }
}
