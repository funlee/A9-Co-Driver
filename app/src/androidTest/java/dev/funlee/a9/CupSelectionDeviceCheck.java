package dev.funlee.a9;

import android.app.*;
import android.graphics.*;
import android.os.*;
import android.content.Intent;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import dev.funlee.a9.core.*;
import party.iroiro.luajava.Lua;
import java.io.*;
import java.util.*;

/** Runs only the real menu selector. Never runs the cup workflow or starts a race. */
final class CupSelectionDeviceCheck implements LuaRuntime.Host {
    private final Instrumentation test;
    private final UiAutomation ui;
    private String game;
    private Bitmap frame;
    private PixelFrame pixels;
    private File root;
    private int rotation,width,height,snapshots,targetTaps,position,result;
    CupSelectionDeviceCheck(Instrumentation test){this.test=test;ui=test.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);}
    void run(int position,Bundle report)throws Exception{
        if(SessionState.running)throw new IllegalStateException("Stop the current task before checking cup selection");
        if(position<1||position>6)throw new IllegalArgumentException("Device check supports visible positions 1–6 only");
        this.position=position;LocalStore store=new LocalStore(test.getTargetContext());rotation=store.config.optInt("rotation",90);
        game=store.config.getString("gamePkg");Intent launch=test.getTargetContext().getPackageManager().getLaunchIntentForPackage(game);
        if(launch==null)throw new IllegalStateException("Game unavailable");
        test.getTargetContext().startActivity(new Intent().setComponent(launch.getComponent()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Thread.sleep(1200);
        root=new File(test.getTargetContext().getFilesDir(),"runtime");root.mkdirs();
        try{
            capture();
            try(LuaRuntime lua=new LuaRuntime(this,List.of("keepCapture","releaseCapture","tap","swipe","findColor","findPic","snapShot","print","toast","done"))){
                lua.set("LOCAL_ROOT",root.getAbsolutePath());lua.set("run",Map.of("index",3,"data",Map.of("杯赛位置",String.valueOf(position))));lua.set("data",Map.of());
                lua.load("bootstrap",store.asset("lua/bootstrap.lua"));lua.load("utilities",store.asset("lua/utilities.lua"));
                lua.load("selection-only","function selectionOnly()done(utils.chooseCup('"+position+"'))end");lua.start("selectionOnly");
                long start=SystemClock.elapsedRealtime();boolean alive=true;
                while(alive&&SystemClock.elapsedRealtime()-start<60000){alive=lua.step(SystemClock.elapsedRealtime()-start);Thread.sleep(25);}
                if(alive)throw new AssertionError("Cup selector timed out");
            }
            Thread.sleep(1000);
            if(result!=1||snapshots!=1||targetTaps<2)throw new AssertionError("result="+result+", snapshots="+snapshots+", targetTaps="+targetTaps);
            File screenshot=new File(test.getTargetContext().getExternalFilesDir(null),"cup-selection-after.png");
            Bitmap actual=ui.takeScreenshot();if(actual==null)throw new AssertionError("No screenshot");
            try(FileOutputStream out=new FileOutputStream(screenshot)){actual.compress(Bitmap.CompressFormat.PNG,100,out);}finally{actual.recycle();}
            report.putString("result","PASS: real chooseCup selected configured position "+position+", fresh cache saved; selector only, no race started");
            report.putString("screenshot",screenshot.getAbsolutePath());
        }finally{if(frame!=null)frame.recycle();}
    }
    private void requireGame(){
        AccessibilityNodeInfo node=ui.getRootInActiveWindow();try{if(node!=null&&game.contentEquals(node.getPackageName()))return;}finally{if(node!=null)node.recycle();}
        // Some native game surfaces expose no accessibility root; verify the focused window instead.
        try(ParcelFileDescriptor fd=ui.executeShellCommand("dumpsys window");InputStream in=new FileInputStream(fd.getFileDescriptor())){
            String windows=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
            for(String line:windows.split("\n"))if(line.contains("mCurrentFocus=")&&line.contains(" "+game+"/"))return;
        }catch(IOException e){throw new IllegalStateException("Cannot verify game foreground",e);}
        throw new IllegalStateException("Game is not foreground");
    }
    private void capture(){
        requireGame();Bitmap actual=ui.takeScreenshot();if(actual==null)throw new IllegalStateException("No screenshot");
        width=actual.getWidth();height=actual.getHeight();Matrix m=new Matrix();m.postRotate(rotation);
        Bitmap rotated=Bitmap.createBitmap(actual,0,0,actual.getWidth(),actual.getHeight(),m,false);
        Bitmap scaled=Bitmap.createScaledBitmap(rotated,720,1280,false);Bitmap next=scaled.copy(Bitmap.Config.ARGB_8888,false);
        if(scaled!=rotated&&scaled!=actual)scaled.recycle();
        if(rotated!=actual)rotated.recycle();actual.recycle();if(frame!=null)frame.recycle();frame=next;
        int[] values=new int[720*1280];frame.getPixels(values,0,720,0,0,720,1280);pixels=new PixelFrame(720,1280,values);
    }
    private int number(Lua lua,int index){return (int)lua.toInteger(index);}
    private float[] point(int x,int y){
        if(rotation!=90)throw new IllegalStateException("Device cup check expects the configured 90 degree game layout");
        return new float[]{y*(width-1)/1279f,(719-x)*(height-1)/719f};
    }
    private void input(long start,int action,float x,float y){MotionEvent event=MotionEvent.obtain(start,SystemClock.uptimeMillis(),action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);try{if(!ui.injectInputEvent(event,true))throw new IllegalStateException("Input rejected");}finally{event.recycle();}}
    private void tap(int x,int y){requireGame();float[] p=point(x,y);long start=SystemClock.uptimeMillis();input(start,MotionEvent.ACTION_DOWN,p[0],p[1]);input(start,MotionEvent.ACTION_UP,p[0],p[1]);}
    private void swipe(int x1,int y1,int x2,int y2,int duration)throws InterruptedException{
        requireGame();float[] a=point(x1,y1),b=point(x2,y2);long start=SystemClock.uptimeMillis();input(start,MotionEvent.ACTION_DOWN,a[0],a[1]);
        for(int n=1;n<=5;n++){Thread.sleep(Math.max(1,duration/5));input(start,MotionEvent.ACTION_MOVE,a[0]+(b[0]-a[0])*n/5,a[1]+(b[1]-a[1])*n/5);}
        input(start,MotionEvent.ACTION_UP,b[0],b[1]);
    }
    @Override public int call(String name,Lua lua){
        try{
            switch(name){
                case "keepCapture":capture();return 0;
                case "releaseCapture":return 0;
                case "tap":{
                    int x=number(lua,1),y=number(lua,2);if(x==100&&y==120+180*(position-1))targetTaps++;
                    tap(x,y);return 0;
                }
                case "swipe":swipe(number(lua,1),number(lua,2),number(lua,3),number(lua,4),number(lua,5));return 0;
                case "findColor":{
                    for(int y=number(lua,2);y<=number(lua,4);y++)for(int x=number(lua,1);x<=number(lua,3);x++)
                        if(pixels.inBounds(x,y)&&ColorPattern.color(pixels.pixel(x,y),lua.toString(5),1)){lua.push(x);lua.push(y);return 2;}
                    lua.push(-1);lua.push(-1);return 2;
                }
                case "findPic":throw new AssertionError("New selection reused an old image cache");
                case "snapShot":{
                    Bitmap crop=Bitmap.createBitmap(frame,number(lua,2),number(lua,3),number(lua,4)-number(lua,2)+1,number(lua,5)-number(lua,3)+1);
                    try(FileOutputStream out=new FileOutputStream(lua.toString(1))){crop.compress(Bitmap.CompressFormat.PNG,100,out);}finally{crop.recycle();}
                    snapshots++;return 0;
                }
                case "done":result=number(lua,1);return 0;
                default:return 0;
            }
        }catch(Exception e){throw new IllegalStateException(name+": "+e,e);}
    }
}
