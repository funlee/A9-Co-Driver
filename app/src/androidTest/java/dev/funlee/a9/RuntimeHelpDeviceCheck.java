package dev.funlee.a9;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

/** Exercises navigation and error recovery UI without starting tasks or touching game data. */
public final class RuntimeHelpDeviceCheck extends Instrumentation {
    private MainActivity activity;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();int code=0;
        try{
            if(SessionState.running)throw new AssertionError("A task is active");
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            runOnMainSync(()->{
                require(!contains("运行诊断"),"Diagnostics still appears in navigation");
                require(findButton("任务编排")!=null&&findButton("任务偏好")!=null&&findButton("地图配置")!=null&&findButton("偏好设置")!=null,"Missing primary navigation");
                require(!contains("运行记录"),"Raw logs appear in run center");
                click("偏好设置");
                require(contains("运行权限")&&contains("自动操作")&&contains("悬浮控制")&&contains("通知控制"),"Permissions are missing");
                require(!contains("高级与帮助")&&!contains("开发者工具")&&!contains("识别与操作参数")&&!contains("第三方运行库许可"),"Advanced/help entries remain visible");
                click("任务编排");
                SessionState.running=true;
                SessionState.failure("[string multiplayer.lua]:91: 未收到截屏画面，请停止后重新授权截屏\nstack traceback: simulated-only");
                refresh();require(!findButton("重新授权截屏").isEnabled(),"Retry allowed while stopping");
                SessionState.running=false;refresh();require(findButton("重新授权截屏").isEnabled(),"Retry stays disabled after stopping");
                require(contains("无法获取游戏画面")&&!contains("stack traceback"),"Raw error leaked into run center");
            });
            screenshot("run-capture-help.png");
            runOnMainSync(()->{
                SessionState.failure("tap：触控确认超时");refresh();
                require(contains("游戏触控未完成")&&findButton("打开游戏")!=null
                        &&findButton("开启触控权限")==null,"Gesture timeout reported as missing permission");
            });
            screenshot("run-touch-timeout-help.png");
            runOnMainSync(()->{
                SessionState.failure("画面方向或比例不匹配");refresh();
                require(findButton("打开游戏")!=null&&findButton("调整画面设置")==null,"Orientation recovery links to removed settings");
                SessionState.failure("请先开启悬浮窗或通知");refresh();click("开启控制权限");
                require(contains("运行权限"),"Permission recovery lost its destination");click("任务编排");
                SessionState.failure("未找到游戏：com.example.missing");refresh();click("检查游戏设置");
                require(contains("目标游戏")&&!contains("运行权限"),"Game recovery must open task preferences");click("任务编排");
                SessionState.failure("请至少启用一个项目");refresh();click("查看项目队列");require(SessionState.issue==null,"Queue guidance not dismissed");
                SessionState.failure("unexpected-engine-detail");refresh();
                require(findButton("打开游戏")!=null&&findButton("查看问题记录")==null&&!contains("unexpected-engine-detail"),"Unknown failure exposes removed tools or raw details");
            });
            // Old secondary page state must land on the surviving permission page.
            for(int oldPage:new int[]{2,4}){
                ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);
                runOnMainSync(()->{
                    try{Field tab=MainActivity.class.getDeclaredField("tab");tab.setAccessible(true);tab.setInt(activity,oldPage);}catch(Exception e){throw new AssertionError(e);}
                    activity.recreate();
                });
                Activity restored=waitForMonitorWithTimeout(monitor,10000);removeMonitor(monitor);
                require(restored instanceof MainActivity,"Activity did not recreate");activity=(MainActivity)restored;waitForIdleSync();
                runOnMainSync(()->require(contains("运行权限")&&!contains("高级与帮助"),"Old secondary page did not return to permissions"));
            }
            screenshot("permissions-only.png");
            runOnMainSync(()->{
                SessionState.clearIssue();click("任务编排");refresh();require(!contains("任务遇到问题，已停止"),"Stale warning after clearing issue");
            });
            screenshot("run-center.png");
            result.putString("result","PASS: four tabs; permissions retained; advanced/help entries removed; old secondary pages restore to permissions; recovery actions valid; retry enabled after stop; raw logs hidden; no game input");code=-1;
        }catch(Throwable e){result.putString("error",e.toString());}
        finally{runOnMainSync(()->{SessionState.running=false;SessionState.clearIssue();SessionState.status="尚未启动";});}
        finish(code,result);
    }
    private void refresh(){try{Method m=MainActivity.class.getDeclaredMethod("updateSession");m.setAccessible(true);m.invoke(activity);}catch(Exception e){throw new AssertionError(e);}}
    private View root(){return activity.getWindow().getDecorView();}
    private Button findButton(String text){return (Button)find(root(),text,true);}
    private boolean contains(String text){return find(root(),text,false)!=null;}
    private View find(View view,String text,boolean button){
        if(view.getVisibility()!=View.VISIBLE)return null;
        if(view instanceof TextView&&(!button||view instanceof Button)){
            String value=((TextView)view).getText().toString();if(button?value.equals(text):value.contains(text))return view;
        }
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=find(((ViewGroup)view).getChildAt(i),text,button);if(found!=null)return found;}
        return null;
    }
    private void click(String text){View target=root().findViewWithTag(text);if(target==null)target=findButton(text);require(target!=null,"Missing action "+text);target.performClick();}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private void screenshot(String name)throws Exception{
        waitForIdleSync();Thread.sleep(200);
        android.graphics.Bitmap bitmap=getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).takeScreenshot();
        File directory=new File(getTargetContext().getExternalFilesDir(null),"runtime-help-check");directory.mkdirs();
        try(FileOutputStream out=new FileOutputStream(new File(directory,name))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{bitmap.recycle();}
    }
}
