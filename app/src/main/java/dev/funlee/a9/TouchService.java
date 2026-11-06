package dev.funlee.a9;

import android.accessibilityservice.*;
import android.graphics.Path;
import android.os.*;
import android.view.accessibility.AccessibilityEvent;
import java.util.*;
import java.util.concurrent.*;
import dev.funlee.a9.core.RouteEngine;

public final class TouchService extends AccessibilityService implements RouteEngine.Touch {
    public static volatile TouchService instance;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Map<Integer,GestureDescription.StrokeDescription> strokes=new LinkedHashMap<>();
    private final Map<Integer,float[]> points=new LinkedHashMap<>();
    private volatile int width=1280,height=720,rotation=90;
    private volatile boolean armed;
    private volatile String allowedPackage="";
    public final dev.funlee.a9.core.VehicleUnlockGuard unlockGuard=new dev.funlee.a9.core.VehicleUnlockGuard();
    private long gestureGeneration;
    @Override protected void onServiceConnected() { instance=this;SessionState.log("无障碍触控已连接"); }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if(event.getEventType()==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED&&event.getPackageName()!=null){foregroundWindowId=event.getWindowId();foreground=event.getPackageName().toString();}
    }
    public static volatile String foreground="";
    private volatile int foregroundWindowId=-1;
    @Override public void onInterrupt() { disarm(); }
    @Override public void onDestroy() { instance=null;super.onDestroy(); }
    public void configure(int w,int h,int degrees) {width=w;height=h;rotation=degrees;}
    public void arm(String gamePackage){allowedPackage=gamePackage;armed=true;}
    public boolean gameInForeground() {
        android.view.accessibility.AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null){
            // Surface-rendered games can expose a focused window without an accessible root.
            // Require both the current focused window ID and its package event to agree.
            for(android.view.accessibility.AccessibilityWindowInfo window:getWindows()) {
                try {if(window.isFocused()&&window.getType()==android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION
                        &&window.getId()==foregroundWindowId&&allowedPackage.equals(foreground))return true;}
                finally {window.recycle();}
            }
            return false;
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
            return count==1&&!bounds.isEmpty()?new int[]{bounds.centerX()*1280/width,bounds.centerY()*720/height}:null;
        }finally{root.recycle();}
    }
    public void disarm(){armed=false;releaseAll();}
    private float[] transform(int x,int y) {
        if(x<0||y<0||x>=720||y>=1280) throw new IllegalArgumentException("触控坐标超出画面："+x+","+y);
        switch(rotation) {
            case 90: return new float[]{y*(width-1)/1279f,(719-x)*(height-1)/719f};
            case 270: return new float[]{(1279-y)*(width-1)/1279f,x*(height-1)/719f};
            case 180: return new float[]{(719-x)*(width-1)/719f,(1279-y)*(height-1)/1279f};
            default: return new float[]{x*(width-1)/719f,y*(height-1)/1279f};
        }
    }
    private interface Build { void run(CountDownLatch latch,boolean[] ok); }
    private void perform(Build build) {
        if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("触控需在工作线程执行");
        if(!armed||Thread.currentThread().isInterrupted())throw new IllegalStateException("触控已停止");
        requireGameForeground();
        Thread owner=Thread.currentThread();
        for(int attempt=0;attempt<3;attempt++) {
        CountDownLatch latch=new CountDownLatch(1);boolean[] ok={false};main.post(()->{
            if(!armed||owner.isInterrupted()){latch.countDown();return;}
            try { requireGameForeground();build.run(latch,ok); }catch(Exception e){SessionState.log(e.toString());latch.countDown();}
        });
        try {
            if(!latch.await(3,TimeUnit.SECONDS))throw new IllegalStateException("触控确认超时");
            if(ok[0])return;
            if(!armed||owner.isInterrupted())throw new IllegalStateException("触控已停止");
            requireGameForeground();
            if(attempt<2){SessionState.log("触控被取消，重建触点并重试 "+(attempt+1)+"/2");Thread.sleep(80);}
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
    @Override public void tap(int finger,int x,int y) {unlockGuard.requireTap(y,719-x);change(finger,transform(x,y),true,20);}
    @Override public void down(int finger,int x,int y) {unlockGuard.requireOtherGesture();change(finger,transform(x,y),false,16);}
    public void move(int finger,int x,int y) {unlockGuard.requireOtherGesture();change(finger,transform(x,y),false,40);}
    @Override public void up(int finger) {change(finger,null,true,16);}
    public void swipe(int x1,int y1,int x2,int y2,long duration) {
        down(9,x1,y1);change(9,transform(x2,y2),true,Math.max(16,Math.min(2000,duration)));
    }
    public void key(String key) {
        unlockGuard.requireOtherGesture();
        if(!armed||Thread.currentThread().isInterrupted())throw new IllegalStateException("触控已停止");
        requireGameForeground();
        main.post(()->{if(armed&&gameInForeground())performGlobalAction(key.equals("home")?GLOBAL_ACTION_HOME:GLOBAL_ACTION_BACK);});
    }
    public void releaseAll() {
        main.post(()->{
            gestureGeneration++;
            // A fresh complete gesture cancels any continued stroke retained by Android.
            if(strokes.isEmpty()){points.clear();return;}
            GestureDescription.Builder b=new GestureDescription.Builder();
            for(Integer id:strokes.keySet()) {float[] p=points.get(id);Path path=new Path();path.moveTo(p[0],p[1]);b.addStroke(strokes.get(id).continueStroke(path,0,1,false));}
            strokes.clear();points.clear();dispatchGesture(b.build(),null,null);
        });
    }
}
