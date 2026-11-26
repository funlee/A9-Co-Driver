package dev.funlee.a9;

import android.accessibilityservice.*;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.ImageButton;
import java.util.*;
import java.util.concurrent.*;
import dev.funlee.a9.core.RouteEngine;
import dev.funlee.a9.core.GameViewport;

public final class TouchService extends AccessibilityService implements RouteEngine.Touch {
    public static volatile TouchService instance;
    static boolean enabledInSettings(Context context) {
        String enabled=Settings.Secure.getString(context.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if(enabled==null)return false;
        ComponentName expected=new ComponentName(context,TouchService.class);
        for(String entry:enabled.split(":"))if(expected.equals(ComponentName.unflattenFromString(entry)))return true;
        return false;
    }
    static String unavailableReason(Context context) {
        return enabledInSettings(context)?"无障碍服务已开启但未连接":"请先在设置中开启无障碍服务";
    }
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Map<Integer,GestureDescription.StrokeDescription> strokes=new LinkedHashMap<>();
    private final Map<Integer,float[]> points=new LinkedHashMap<>();
    private volatile GameViewport viewport=new GameViewport(0,0,1280,720,90);
    private volatile boolean armed;
    private volatile String allowedPackage="";
    public final dev.funlee.a9.core.VehicleUnlockGuard unlockGuard=new dev.funlee.a9.core.VehicleUnlockGuard();
    private long gestureGeneration;
    private TaskControlMenu controlMenu;
    private AlertDialog taskFailureDialog;
    private String failureGamePackage="";
    private ImageButton controlButton;
    private volatile GameActivityHint activityHint;
    private final Runnable refreshControlButton=this::updateControlButton;
    private final AccessibilityButtonController.AccessibilityButtonCallback shortcutCallback=
            new AccessibilityButtonController.AccessibilityButtonCallback(){
                @Override public void onClicked(AccessibilityButtonController controller){
                    if(!SessionState.running){
                        android.widget.Toast.makeText(TouchService.this,"暂无运行中的任务，请先在 A9辅助驾驶中开始任务",android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    toggleTaskControls();
                }
            };
    @Override protected void onServiceConnected() {
        instance=this;
        if(activityHint==null)activityHint=new GameActivityHint(this,main,
                ()->armed&&SessionState.running&&!SessionState.inputPaused()&&gameInForeground());
        AccessibilityButtonController controller=getAccessibilityButtonController();
        controller.unregisterAccessibilityButtonCallback(shortcutCallback);
        controller.registerAccessibilityButtonCallback(shortcutCallback,main);
        SessionState.log("无障碍触控已连接");
        main.post(this::showPendingTaskFailure);
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if(event.getEventType()==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED&&event.getPackageName()!=null){
            foregroundWindowId=event.getWindowId();foreground=event.getPackageName().toString();
            if(taskFailureDialog!=null&&!failureGamePackage.equals(foreground)){
                for(AccessibilityWindowInfo window:getWindows()){
                    try{
                        if(window.getId()==foregroundWindowId&&window.isFocused()
                                &&window.getType()==AccessibilityWindowInfo.TYPE_APPLICATION){
                            dismissTaskFailure();
                        }
                    }finally{window.recycle();}
                }
            }
            if(!SessionState.running)main.post(this::showPendingTaskFailure);
            if(armed)scheduleControlButton();
        }
    }
    public static volatile String foreground="";
    private volatile int foregroundWindowId=-1;
    private void toggleTaskControls(){
        if(controlMenu!=null)closeTaskControls();
        else if(SessionState.running)openTaskControls();
    }
    private void scheduleControlButton(){
        main.removeCallbacks(refreshControlButton);
        main.post(refreshControlButton);
        // Window focus can settle just after the accessibility event.
        main.postDelayed(refreshControlButton,250);
    }
    private void updateControlButton(){
        if(!armed||!SessionState.running||(!gameInForeground()&&foregroundGameBounds()==null)){
            if(controlButton!=null){closeTaskControls();removeControlButton();}
            if(activityHint!=null)activityHint.clear();
            return;
        }
        if(controlButton!=null)return;
        ImageButton button=new ImageButton(this);
        button.setImageResource(R.drawable.ic_steering_mark);
        button.setContentDescription("A9 任务控制");
        button.setPadding(dp(3),dp(3),dp(3),dp(3));
        GradientDrawable background=new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(0xeeecf6f3);
        background.setStroke(dp(1),0xff087f73);
        button.setBackground(background);
        button.setOnClickListener(v->toggleTaskControls());
        WindowManager.LayoutParams params=new WindowManager.LayoutParams(dp(32),dp(32),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.RIGHT|Gravity.CENTER_VERTICAL;
        params.x=dp(8);
        params.setTitle("A9 游戏控制按钮");
        try{
            getSystemService(WindowManager.class).addView(button,params);
            controlButton=button;
            SessionState.hint(SessionState.hud.isEmpty()?SessionState.status:SessionState.hud);
        }catch(WindowManager.BadTokenException|SecurityException e){
            SessionState.log("无法显示右侧任务控制按钮："+e.getMessage());
        }
    }
    private void removeControlButton(){
        if(controlButton==null)return;
        getSystemService(WindowManager.class).removeViewImmediate(controlButton);
        controlButton=null;
    }
    private int dp(int value){return Math.round(getResources().getDisplayMetrics().density*value);}
    private void openTaskControls(){
        if(activityHint!=null)activityHint.clear();
        TaskControlMenu menu=new TaskControlMenu(this,SessionState.inspecting,SessionState.paused,
                this::handleTaskControl,this::closeTaskControls);
        SessionState.controlsOpen=!SessionState.inspecting;
        try{
            menu.show();
            controlMenu=menu;
            if(SessionState.controlsOpen)releaseAll();
        }catch(WindowManager.BadTokenException|SecurityException e){
            SessionState.controlsOpen=false;
            SessionState.log("无法打开任务控制菜单："+e.getMessage());
            android.widget.Toast.makeText(this,"无法打开控制菜单，请从助手或通知栏控制任务",android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    private void handleTaskControl(String action){
        if(!SessionState.running){closeTaskControls();return;}
        try{
            startService(new Intent(this,AutomationService.class).setAction(action));
        }catch(IllegalStateException|SecurityException e){
            closeTaskControls();
            SessionState.log("无法发送任务控制指令："+e.getMessage());
        }
    }
    void closeTaskControls(){
        if(controlMenu!=null){controlMenu.hide();controlMenu=null;}
        SessionState.controlsOpen=false;
    }
    boolean showPendingTaskFailure(){
        if(taskFailureDialog!=null)return true;
        if(SessionState.running)return false;
        String gamePackage=RunStatusRecord.failureGame(this);
        if(gamePackage.isEmpty())return false;
        if(!gamePackage.equals(foregroundPackage())&&
                !(gamePackage.equals(allowedPackage)&&foregroundGameBounds()!=null))return false;
        String message=RunStatusRecord.failureMessage(this);
        if(message.isEmpty())return false;
        AlertDialog dialog=new AlertDialog.Builder(this)
                .setTitle("任务已停止")
                .setMessage(message)
                .setPositiveButton("确定",(ignored,which)->{RunStatusRecord.clearFailure(this);dismissTaskFailure();})
                .setCancelable(false)
                .create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        try{
            dialog.show();
            taskFailureDialog=dialog;
            failureGamePackage=gamePackage;
            return true;
        }catch(WindowManager.BadTokenException|SecurityException e){
            SessionState.log("无法显示任务停止提示："+e.getMessage());
            return false;
        }
    }
    void dismissTaskFailure(){
        if(taskFailureDialog==null)return;
        taskFailureDialog.dismiss();
        taskFailureDialog=null;
        failureGamePackage="";
    }
    @Override public void onConfigurationChanged(Configuration configuration){
        super.onConfigurationChanged(configuration);
        closeTaskControls();
        removeControlButton();
        if(activityHint!=null)activityHint.clear();
        if(armed)scheduleControlButton();
    }
    @Override public void onInterrupt() { closeTaskControls();disarm(); }
    @Override public void onDestroy() {
        dismissTaskFailure();
        closeTaskControls();
        disarm();
        getAccessibilityButtonController().unregisterAccessibilityButtonCallback(shortcutCallback);
        instance=null;super.onDestroy();
    }
    void configure(GameViewport viewport){this.viewport=viewport;}
    void offerActivityHint(String message){
        main.post(()->{if(activityHint!=null)activityHint.offer(message);});
    }
    /** Called on the main looper before recovery opens system settings. */
    void showRestartNotice(String message){
        if(activityHint!=null&&armed&&gameInForeground()&&activityHint.showNow(message,3500))return;
        android.widget.Toast.makeText(this,message,android.widget.Toast.LENGTH_LONG).show();
    }
    void updateRecoveryWait(String message){
        main.post(()->{if(activityHint!=null)activityHint.updateWait(message);});
    }
    void showTaskNotice(String message,int duration){
        main.post(()->{
            if(activityHint!=null&&armed&&SessionState.running&&gameInForeground())activityHint.showNow(message,duration);
        });
    }
    Rect activityHintBounds(long frameTime){
        GameActivityHint hint=activityHint;
        return hint==null?null:hint.boundsAt(frameTime);
    }
    public void configure(int w,int h,int degrees){configure(new GameViewport(0,0,w,h,degrees));}
    public void arm(String gamePackage){allowedPackage=gamePackage;armed=true;scheduleControlButton();}
    /**
     * 服务刚连上、或者游戏早就开着才开启无障碍时，一条窗口事件都没收到，foreground 还是空的。
     * 此时按「当前聚焦的应用窗口」补一次，否则已经在玩的游戏会被判成不在前台。
     */
    private void primeForegroundIfUnknown(){
        if(!foreground.isEmpty())return;
        try{
            for(android.view.accessibility.AccessibilityWindowInfo window:getWindows()){
                try{
                    if(!window.isFocused()||window.getType()!=android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION)continue;
                    android.view.accessibility.AccessibilityNodeInfo root=window.getRoot();
                    if(root==null)continue;
                    try{
                        CharSequence name=root.getPackageName();
                        if(name!=null&&name.length()>0){foreground=name.toString();foregroundWindowId=window.getId();}
                    }finally{root.recycle();}
                    return;
                }finally{window.recycle();}
            }
        }catch(RuntimeException ignored){}
    }
    public boolean gameInForeground() {
        primeForegroundIfUnknown();
        android.view.accessibility.AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null){
            // Surface-rendered games can expose a focused window without an accessible root.
            // Require both the current focused window ID and its package event to agree.
            boolean focusedApplication=false;
            for(android.view.accessibility.AccessibilityWindowInfo window:getWindows()) {
                try {
                    if(!window.isFocused()||window.getType()!=android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION)continue;
                    focusedApplication=true;
                    if(window.getId()==foregroundWindowId&&allowedPackage.equals(foreground))return true;
                    // 事件里的前台可能还停在我们自己的窗口上（游戏本来就在前台、再切回来不会发事件），
                    // 这时直接读聚焦窗口的根节点包名。
                    android.view.accessibility.AccessibilityNodeInfo focusedRoot=window.getRoot();
                    if(focusedRoot!=null){
                        try{
                            CharSequence name=focusedRoot.getPackageName();
                            if(name!=null&&allowedPackage.contentEquals(name))return true;
                        }finally{focusedRoot.recycle();}
                    }
                } finally {window.recycle();}
            }
            // 事件里的窗口 id 会过期（服务刚连上、窗口重建），此时只要前台包名还是游戏、
            // 并且确实有一个聚焦的应用窗口，就按在前台处理，避免白等三分钟超时。
            return focusedApplication&&allowedPackage.equals(foreground);
        }
        try{return allowedPackage.contentEquals(root.getPackageName()==null?"":root.getPackageName());}
        finally{root.recycle();}
    }
    public void requireGameForeground(){
        if(armed&&!gameInForeground())throw new ForegroundLost();
    }
    public static final class ForegroundLost extends IllegalStateException {
        public ForegroundLost(){super("游戏已离开前台，正在确认是否需要恢复");}
    }
    Rect foregroundGameBounds(){
        for(AccessibilityWindowInfo window:getWindows()){
            try{
                if(!window.isFocused()||window.getType()!=AccessibilityWindowInfo.TYPE_APPLICATION)continue;
                AccessibilityNodeInfo root=window.getRoot();
                boolean game;
                if(root==null)game=window.getId()==foregroundWindowId&&allowedPackage.equals(foreground);
                else try{game=allowedPackage.contentEquals(root.getPackageName()==null?"":root.getPackageName());}
                finally{root.recycle();}
                if(game){Rect bounds=new Rect();window.getBoundsInScreen(bounds);return bounds;}
            }finally{window.recycle();}
        }
        return null;
    }
    public String foregroundPackage(){
        android.view.accessibility.AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root!=null){try{return String.valueOf(root.getPackageName());}finally{root.recycle();}}
        for(android.view.accessibility.AccessibilityWindowInfo window:getWindows()){
            try{if(window.isFocused()&&window.getType()==android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION&&window.getId()==foregroundWindowId)return foreground;}
            finally{window.recycle();}
        }
        return "";
    }
    /** Native SDK picker: only an exact, unique recent-account badge on a login-history page. */
    public int[] recentLogin(){
        android.view.accessibility.AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return null;
        try{
            if(!allowedPackage.contentEquals(root.getPackageName()==null?"":root.getPackageName()))return null;
            boolean history=false;int count=0;android.graphics.Rect bounds=new android.graphics.Rect();
            for(android.view.accessibility.AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByText("登录历史")){
                try{if(n.isVisibleToUser()&&"登录历史".contentEquals(n.getText()==null?"":n.getText()))history=true;}finally{n.recycle();}
            }
            if(!history)return null;
            for(android.view.accessibility.AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByText("最近登录")){
                try{if(n.isVisibleToUser()&&"最近登录".contentEquals(n.getText()==null?"":n.getText())){count++;n.getBoundsInScreen(bounds);}}finally{n.recycle();}
            }
            if(count!=1||bounds.isEmpty())return null;
            float[] point=viewport.toUpright(bounds.centerX(),bounds.centerY());
            if(point[0]<0||point[0]>1279||point[1]<0||point[1]>719)return null;
            return new int[]{Math.round(point[0]),Math.round(point[1])};
        }finally{root.recycle();}
    }
    public void disarm(){
        armed=false;
        main.removeCallbacks(refreshControlButton);
        if(Looper.myLooper()==Looper.getMainLooper())updateControlButton();
        else main.post(refreshControlButton);
        releaseAll();
    }
    /**
     * Frames outside 720 × 1280 are dropped with a log entry: injecting whatever
     * coordinates the scripts computed would let the system discard off-screen pointers, so a stale
     * anchor degrades to a no-op instead of aborting the run.
     */
    private float[] point(int x,int y){
        if(x<0||y<0||x>=GameViewport.FRAME_WIDTH||y>=GameViewport.FRAME_HEIGHT){
            SessionState.log("触控坐标越界，已忽略："+x+","+y);
            return null;
        }
        return viewport.toScreen(x,y);
    }
    private interface Build { void run(CountDownLatch latch,boolean[] ok); }
    private void awaitInput(){
        try{
            while(armed&&SessionState.inputPaused())Thread.sleep(40);
        }catch(InterruptedException e){Thread.currentThread().interrupt();}
        if(!armed||Thread.currentThread().isInterrupted())throw new IllegalStateException("触控已停止");
    }
    private void perform(Build build) {
        if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("触控需在工作线程执行");
        Thread owner=Thread.currentThread();
        for(int attempt=0;attempt<3;) {
        awaitInput();
        requireGameForeground();
        CountDownLatch latch=new CountDownLatch(1);boolean[] ok={false};boolean[] deferred={false};main.post(()->{
            if(!armed||owner.isInterrupted()){latch.countDown();return;}
            if(SessionState.inputPaused()){deferred[0]=true;latch.countDown();return;}
            try { requireGameForeground();build.run(latch,ok); }catch(Exception e){SessionState.log(e.toString());latch.countDown();}
        });
        try {
            if(!latch.await(3,TimeUnit.SECONDS))throw new IllegalStateException("触控确认超时");
            if(ok[0])return;
            if(deferred[0])continue;
            if(SessionState.inputPaused()){awaitInput();return;}
            if(!armed||owner.isInterrupted())throw new IllegalStateException("触控已停止");
            requireGameForeground();
            if(++attempt<3){SessionState.log("触控被取消，重建触点并重试 "+attempt+"/2");Thread.sleep(80);}
        }
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("触控已停止");}
        }
        throw new IllegalStateException("触控连续被系统取消；请检查无障碍和画面方向");
    }
    private void dispatch(GestureDescription gesture,CountDownLatch latch,boolean[] ok,
                          Map<Integer,GestureDescription.StrokeDescription> next,Map<Integer,float[]> nextPoints) {
        long generation=++gestureGeneration;
        if(!dispatchGesture(gesture,new GestureResultCallback(){
            @Override public void onCompleted(GestureDescription g){
                if(generation==gestureGeneration&&armed){strokes.clear();strokes.putAll(next);points.clear();points.putAll(nextPoints);ok[0]=true;}
                latch.countDown();
            }
            @Override public void onCancelled(GestureDescription g){if(generation==gestureGeneration)strokes.clear();latch.countDown();}
        },main)) {strokes.clear();latch.countDown();}
    }
    private void change(int finger,float[] target,boolean end,long duration) {
        perform((latch,ok)->{
            GestureDescription.Builder builder=new GestureDescription.Builder();
            Map<Integer,GestureDescription.StrokeDescription> next=new LinkedHashMap<>();
            Map<Integer,float[]> nextPoints=new LinkedHashMap<>();int count=0;
            for(Integer id:points.keySet()) {
                // Cancellation already releases the physical pointers. An interrupted Up
                // needs no new tap; other held fingers are recreated with new stroke IDs.
                if(id==finger&&end&&target==null&&!strokes.containsKey(id))continue;
                float[] old=points.get(id), p=id==finger&&target!=null?target:old;
                Path path=new Path();path.moveTo(old[0],old[1]);if(p!=old)path.lineTo(p[0],p[1]);
                boolean keep=!(id==finger&&end);
                GestureDescription.StrokeDescription previous=strokes.get(id);
                GestureDescription.StrokeDescription stroke=previous==null?new GestureDescription.StrokeDescription(path,0,duration,keep):previous.continueStroke(path,0,duration,keep);
                builder.addStroke(stroke);count++;if(keep){next.put(id,stroke);nextPoints.put(id,p);}
            }
            if(!points.containsKey(finger)&&target!=null) {
                Path path=new Path();path.moveTo(target[0],target[1]);
                GestureDescription.StrokeDescription stroke=new GestureDescription.StrokeDescription(path,0,duration,!end);
                builder.addStroke(stroke);count++;if(!end){next.put(finger,stroke);nextPoints.put(finger,target);}
            }
            if(count==0){strokes.clear();points.clear();ok[0]=true;latch.countDown();return;}
            dispatch(builder.build(),latch,ok,next,nextPoints);
        });
    }
    @Override public void tap(int finger,int x,int y) {
        float[] at=point(x,y);if(at==null)return;
        unlockGuard.requireTap(y,719-x);change(finger,at,true,20);
    }
    @Override public void down(int finger,int x,int y) {
        float[] at=point(x,y);if(at==null)return;
        unlockGuard.requireOtherGesture();change(finger,at,false,16);
    }
    public void move(int finger,int x,int y) {
        float[] at=point(x,y);if(at==null)return;
        unlockGuard.requireOtherGesture();change(finger,at,false,40);
    }
    @Override public void up(int finger) {
        if(SessionState.inputPaused()){releaseAll();return;}
        change(finger,null,true,16);
    }
    public void swipe(int x1,int y1,int x2,int y2,long duration) {
        if(point(x1,y1)==null||point(x2,y2)==null)return;
        down(9,x1,y1);change(9,viewport.toScreen(x2,y2),true,Math.max(16,Math.min(2000,duration)));
    }
    public void dragColumn(int x1,int y1,int x2,int y2,long duration,long hold){
        if(point(x1,y1)==null||point(x2,y2)==null)return;
        down(9,x1,y1);
        change(9,viewport.toScreen(x2,y2),false,Math.max(16,Math.min(2000,duration)));
        // Let the carousel stop under the finger before release to avoid a fling.
        change(9,null,true,Math.max(16,Math.min(500,hold)));
    }
    public void key(String key) {
        unlockGuard.requireOtherGesture();
        awaitInput();
        requireGameForeground();
        main.post(()->{if(armed&&!SessionState.inputPaused()&&gameInForeground())performGlobalAction(key.equals("home")?GLOBAL_ACTION_HOME:GLOBAL_ACTION_BACK);});
    }
    public void releaseAll() {
        Runnable release=()->{
            gestureGeneration++;
            // A fresh complete gesture cancels any continued stroke retained by Android.
            if(strokes.isEmpty()){points.clear();return;}
            GestureDescription.Builder b=new GestureDescription.Builder();
            for(Integer id:strokes.keySet()) {float[] p=points.get(id);Path path=new Path();path.moveTo(p[0],p[1]);b.addStroke(strokes.get(id).continueStroke(path,0,1,false));}
            strokes.clear();points.clear();dispatchGesture(b.build(),null,null);
        };
        if(Looper.myLooper()==Looper.getMainLooper())release.run();
        else main.post(release);
    }
}
