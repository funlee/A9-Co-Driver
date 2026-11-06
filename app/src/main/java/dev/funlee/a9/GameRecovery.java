package dev.funlee.a9;

import android.app.KeyguardManager;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

/** Bounded, cancellable recovery. System clicks are semantic and scoped to this game's app-info. */
final class GameRecovery {
    enum Reason { MATCH_TIMEOUT, FOREGROUND_LOST }
    private final Context context;
    private final TouchService touch;
    private final String game,label;
    private final BooleanSupplier stopped,paused;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Set<String> launchers=new HashSet<>();
    private final ArrayDeque<Long> attempts=new ArrayDeque<>();
    private long pausedMillis;
    GameRecovery(Context context,TouchService touch,String game,BooleanSupplier stopped,BooleanSupplier paused)throws Exception{
        this.context=context;this.touch=touch;this.game=game;this.stopped=stopped;this.paused=paused;
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
    boolean recover(Reason reason)throws Exception{
        gate();touch.releaseAll();
        if(reason==Reason.FOREGROUND_LOST){
            status("游戏离开前台，确认闪退状态");
            // A surface/root transition is not by itself a crash.
            long homeSince=-1,otherSince=-1,until=activeTime()+6000;
            while(activeTime()<until){
                gate();if(touch.gameInForeground()){
                    if(homeSince>=0){recordAttempt();SessionState.log("游戏退出后已重新进入前台，恢复任务检查点");return true;}
                    return false;
                }
                String foreground=touch.foregroundPackage();
                if(launchers.contains(foreground)||context.getPackageName().equals(foreground)){
                    otherSince=-1;
                    if(homeSince<0)homeSince=activeTime();
                    if(activeTime()-homeSince>=1000)break;
                }else{
                    homeSince=-1;
                    if(foreground.equals("android")&&closeCrashDialog()){until=activeTime()+6000;waitBriefly();continue;}
                    if(!foreground.isEmpty()){
                        if(otherSince<0){otherSince=activeTime();SessionState.log("等待退出后的前台窗口稳定："+foreground);}
                        if(activeTime()-otherSince>1800)throw new IllegalStateException("当前前台为 "+foreground+"，不是游戏、桌面或助手；已停止恢复，避免打断其他应用");
                    }
                }
                waitBriefly();
            }
            if(homeSince<0||activeTime()-homeSince<1000)throw new IllegalStateException("无法确认游戏退出到桌面或助手，已停止恢复");
        }else if(!touch.gameInForeground())throw new IllegalStateException("匹配重启前游戏已离开前台，已停止恢复");
        recordAttempt();
        status(reason==Reason.MATCH_TIMEOUT?"匹配超过 90 秒且返回按钮消失，正在重启游戏":"游戏已退出到桌面或助手，正在重新启动");
        if(reason==Reason.MATCH_TIMEOUT){
            forceStop();
            // Leave our settings task behind before relaunch; a later crash should land on Home.
            if(!onMain(()->touch.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)))
                throw new IllegalStateException("无法退出系统应用信息页，已停止恢复");
            Thread.sleep(350);
        }
        gate();
        Intent launch=context.getPackageManager().getLaunchIntentForPackage(game);
        if(launch==null)throw new IllegalStateException("找不到游戏启动入口");
        onMain(()->{context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));return true;});
        long until=activeTime()+45_000,stable=-1;
        while(activeTime()<until){
            gate();
            if(touch.gameInForeground()){
                if(stable<0)stable=activeTime();
                if(activeTime()-stable>=2000){SessionState.log("游戏已重新进入前台，保留完赛数并继续剩余任务");return true;}
            }else stable=-1;
            waitBriefly();
        }
        throw new IllegalStateException("重启后未能确认游戏前台，已停止；已完成局数仍保留");
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
        int phase=0;long until=activeTime()+20_000;
        while(activeTime()<until){
            gate();final int stage=phase;
            final int[] outcome={0};
            onMain(()->{
                AccessibilityNodeInfo root=touch.getRootInActiveWindow();if(root==null)return false;
                try{
                    if(!settings.contentEquals(root.getPackageName()==null?"":root.getPackageName()))return false;
                    String all=text(root);
                    if(stage==0&&all.contains(normalize(label))){
                        AccessibilityNodeInfo stop=find(root,GameRecovery::stopLabel);
                        if(stop!=null){try{if(!enabled(stop)){outcome[0]=3;return true;}if(click(stop)){outcome[0]=1;return true;}}finally{stop.recycle();}}
                    }else if(stage==1&&hasStopText(all)){
                        // Only a dialog reached immediately after our game-specific Force Stop click.
                        AccessibilityNodeInfo confirm=find(root,n->"android:id/button1".equals(n.getViewIdResourceName())&&confirmLabel(n));
                        if(confirm==null&&(all.contains("异常")||all.contains("正常")||all.contains("misbehave")))confirm=find(root,GameRecovery::confirmLabel);
                        if(confirm!=null){try{if(click(confirm)){outcome[0]=2;return true;}}finally{confirm.recycle();}}
                    }else if(stage==2&&all.contains(normalize(label))){
                        AccessibilityNodeInfo stop=find(root,GameRecovery::stopLabel);
                        if(stop!=null){try{if(!enabled(stop)){outcome[0]=3;return true;}}finally{stop.recycle();}}
                    }
                    return false;
                }finally{root.recycle();}
            });
            if(outcome[0]==3)return;
            if(outcome[0]>0)phase=outcome[0];
            waitBriefly();
        }
        throw new IllegalStateException("未能在系统应用信息页确认强行停止，请手动处理；任务计数保留");
    }
    private boolean closeCrashDialog()throws Exception{
        return onMain(()->{
            AccessibilityNodeInfo root=touch.getRootInActiveWindow();if(root==null)return false;
            try{
                if(!"android".contentEquals(root.getPackageName()==null?"":root.getPackageName()))return false;
                String all=text(root);
                if(!all.contains(normalize(label))||!(all.contains("停止运行")||all.contains("keepsstopping")||all.contains("hasstopped")))return false;
                AccessibilityNodeInfo close=find(root,n->"android:id/aerr_close".equals(n.getViewIdResourceName())||normalize(n.getText()).equals("关闭应用")||normalize(n.getText()).equals("closeapp"));
                if(close==null)return false;
                try{return click(close);}finally{close.recycle();}
            }finally{root.recycle();}
        });
    }
    private boolean onMain(Callable<Boolean> action)throws Exception{
        gate();
        FutureTask<Boolean> task=new FutureTask<>(()->{
            if(stopped.getAsBoolean()||paused.getAsBoolean())return false;
            return action.call();
        });
        main.post(task);
        try{return task.get(3,TimeUnit.SECONDS);}finally{task.cancel(false);main.removeCallbacks(task);}
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
