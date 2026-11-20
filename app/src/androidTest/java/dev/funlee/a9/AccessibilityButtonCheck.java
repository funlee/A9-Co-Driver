package dev.funlee.a9;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.*;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.*;
import android.view.accessibility.*;

/** Exercises the real System UI shortcut and service action, without starting a race. */
public final class AccessibilityButtonCheck extends Instrumentation {
    private boolean gameForeground,gameOverlay;
    private int shortcutX,shortcutY;
    @Override public void onCreate(Bundle args){
        super.onCreate(args);
        gameForeground="true".equals(args.getString("gameForeground"));
        gameOverlay="true".equals(args.getString("gameOverlay"));
        shortcutX=Integer.parseInt(args.getString("shortcutX","-1"));
        shortcutY=Integer.parseInt(args.getString("shortcutY","-1"));
        start();
    }
    @Override public void onStart(){
        Bundle report=new Bundle();boolean simulated=false;int result=0;
        try{
            UiAutomation ui=getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
            AccessibilityServiceInfo info=ui.getServiceInfo();info.flags|=AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;ui.setServiceInfo(info);
            if(!gameForeground){
                Activity activity=startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                runOnMainSync(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
            }
            long until=SystemClock.elapsedRealtime()+10000;
            while(TouchService.instance==null&&SystemClock.elapsedRealtime()<until)Thread.sleep(100);
            TouchService service=TouchService.instance;
            if(service==null)throw new AssertionError("Accessibility service unavailable");
            if(SessionState.running)throw new AssertionError("Stop the current task before the shortcut check");
            if((service.getServiceInfo().flags&AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON)==0)
                throw new AssertionError("Shortcut still toggles the accessibility service");
            Thread.sleep(1000);
            if(!gameOverlay){clickShortcut(ui);Thread.sleep(400);}
            if(TouchService.instance!=service||SessionState.running)throw new AssertionError("Idle click changed service/task state");
            // Only the session flags are simulated. The shortcut, overlay and service intents are real.
            simulated=true;SessionState.running=true;SessionState.paused=false;
            if(gameOverlay){
                service.arm(gameForeground?"com.aligames.kuang.kybc.aligames":getTargetContext().getPackageName());
                Thread.sleep(500);
                checkActivityHint(service);
            }
            clickShortcut(ui);
            await(()->SessionState.controlsOpen);
            Thread.sleep(1500);
            android.graphics.Bitmap capture=ui.takeScreenshot();
            if(capture!=null){
                report.putString("display",capture.getWidth()+"×"+capture.getHeight());
                try(java.io.FileOutputStream out=getTargetContext().openFileOutput("task-control-menu-test.png",0)){
                    capture.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);
                }
                capture.recycle();
            }
            if(SessionState.paused)throw new AssertionError("Opening the menu toggled task pause");
            clickControl(ui,"关闭菜单");
            await(()->!SessionState.controlsOpen&&!SessionState.paused);
            clickShortcut(ui);clickControl(ui,"暂停任务");
            await(()->SessionState.paused&&!SessionState.controlsOpen);
            clickShortcut(ui);clickControl(ui,"继续任务");
            await(()->!SessionState.paused&&!SessionState.controlsOpen&&SessionState.running);
            clickShortcut(ui);clickControl(ui,"终止任务");
            await(()->!SessionState.running&&!SessionState.controlsOpen);
            if(TouchService.instance!=service)throw new AssertionError("Stopping disconnected accessibility");
            RunStatusRecord.save(getContext(),true,"准备中");
            if(!RunStatusRecord.idleStatus(getContext()).contains("异常中断"))throw new AssertionError("Interrupted status disappeared");
            RunStatusRecord.save(getContext(),false,"已手动终止任务");
            if(!RunStatusRecord.idleStatus(getContext()).equals("已手动终止任务"))throw new AssertionError("Stop status disappeared");
            report.putString("result","PASS: "+(gameOverlay?"Game-side button":"System UI shortcut")+" opens controls; dismiss, pause, resume and terminate; accessibility stays connected");
            result=-1;
        }catch(Throwable e){report.putString("error",e.toString());}
        finally{
            if(simulated){getTargetContext().stopService(new Intent(getTargetContext(),AutomationService.class));SessionState.running=false;SessionState.paused=false;}
        }
        finish(result,report);
    }
    private void await(java.util.function.BooleanSupplier condition)throws InterruptedException{
        long until=SystemClock.elapsedRealtime()+6000;
        while(!condition.getAsBoolean()&&SystemClock.elapsedRealtime()<until)Thread.sleep(50);
        if(!condition.getAsBoolean())throw new AssertionError("Control state did not update");
    }
    private void checkActivityHint(TouchService service)throws Exception{
        await(()->service.activityHintBounds(SystemClock.elapsedRealtime())==null);
        long offeredAt=SystemClock.elapsedRealtime();
        service.offerActivityHint("正在选择配置车辆");
        await(()->service.activityHintBounds(SystemClock.elapsedRealtime())!=null);
        long visibleAt=SystemClock.elapsedRealtime();
        if(visibleAt-offeredAt>1000)throw new AssertionError("First phase was delayed");
        var hintField=TouchService.class.getDeclaredField("activityHint");
        hintField.setAccessible(true);
        var viewField=GameActivityHint.class.getDeclaredField("view");
        viewField.setAccessible(true);
        runOnMainSync(()->{
            try{
                var view=(android.view.View)viewField.get(hintField.get(service));
                var params=(android.view.WindowManager.LayoutParams)view.getLayoutParams();
                if((params.flags&android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)==0)
                    throw new AssertionError("Activity hint intercepts input");
            }catch(ReflectiveOperationException e){throw new AssertionError(e);}
        });
        Thread.sleep(350);
        service.offerActivityHint("正在选择配置车辆");
        await(()->service.activityHintBounds(SystemClock.elapsedRealtime())==null);
        long duration=SystemClock.elapsedRealtime()-visibleAt;
        if(duration<1550||duration>2400)throw new AssertionError("Unexpected activity hint duration: "+duration);
        Thread.sleep(250);
        if(service.activityHintBounds(SystemClock.elapsedRealtime())!=null)
            throw new AssertionError("Duplicate activity hint appeared again");
    }
    private void clickControl(UiAutomation ui,String label)throws InterruptedException{
        if(gameOverlay){
            await(()->SessionState.controlsOpen);
            Thread.sleep(250);
            android.graphics.Rect bounds=new android.graphics.Rect();
            runOnMainSync(()->{
                try{
                    var menuField=TouchService.class.getDeclaredField("controlMenu");
                    menuField.setAccessible(true);
                    var panelField=TaskControlMenu.class.getDeclaredField("panel");
                    panelField.setAccessible(true);
                    var panel=(android.widget.LinearLayout)panelField.get(menuField.get(TouchService.instance));
                    for(int i=0;i<panel.getChildCount();i++){
                        if(panel.getChildAt(i) instanceof android.widget.Button){
                            android.widget.Button button=(android.widget.Button)panel.getChildAt(i);
                            if(!label.contentEquals(button.getText()))continue;
                            int[] location=new int[2];
                            button.getLocationOnScreen(location);
                            bounds.set(location[0],location[1],location[0]+button.getWidth(),location[1]+button.getHeight());
                            break;
                        }
                    }
                }catch(ReflectiveOperationException e){throw new AssertionError(e);}
            });
            if(bounds.isEmpty())throw new AssertionError("Cannot find task menu control "+label);
            tap(ui,bounds.centerX(),bounds.centerY());
            return;
        }
        try{await(()->{
            for(AccessibilityWindowInfo window:ui.getWindows()){
                try{
                    AccessibilityNodeInfo root=window.getRoot();if(root==null)continue;
                    try{
                        for(AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(label)){
                            try{
                                if(getTargetContext().getPackageName().contentEquals(node.getPackageName()==null?"":node.getPackageName())
                                        &&label.contentEquals(node.getText()==null?"":node.getText())&&node.isClickable()
                                        &&node.performAction(AccessibilityNodeInfo.ACTION_CLICK))return true;
                            }finally{node.recycle();}
                        }
                    }finally{root.recycle();}
                }finally{window.recycle();}
            }
            return false;
        });}catch(AssertionError e){throw new AssertionError("Cannot click task menu control "+label,e);}
    }
    private void clickShortcut(UiAutomation ui){
        for(AccessibilityWindowInfo window:ui.getWindows()){
            try{
                AccessibilityNodeInfo root=window.getRoot();if(root==null)continue;
                try{if(click(root))return;}finally{root.recycle();}
            }finally{window.recycle();}
        }
        if(shortcutX>=0&&shortcutY>=0){
            tap(ui,shortcutX,shortcutY);
            return;
        }
        throw new AssertionError("Right-side A9 shortcut not found");
    }
    private void tap(UiAutomation ui,int x,int y){
        long time=SystemClock.uptimeMillis();
        for(int action:new int[]{android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP}){
            var event=android.view.MotionEvent.obtain(time,SystemClock.uptimeMillis(),action,x,y,0);
            try{
                event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
                if(!ui.injectInputEvent(event,true))throw new AssertionError("Screen touch failed");
            }finally{event.recycle();}
        }
    }
    private boolean click(AccessibilityNodeInfo node){
        String label=String.valueOf(node.getContentDescription())+node.getText();
        String owner=gameOverlay?getTargetContext().getPackageName():"com.android.systemui";
        if(owner.contentEquals(node.getPackageName()==null?"":node.getPackageName())
                &&label.contains(gameOverlay?"A9 任务控制":"A9辅助驾驶")&&node.isClickable())
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        for(int i=0;i<node.getChildCount();i++){
            AccessibilityNodeInfo child=node.getChild(i);if(child==null)continue;
            try{if(click(child))return true;}finally{child.recycle();}
        }
        return false;
    }
}
