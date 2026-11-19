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
import java.util.concurrent.TimeUnit;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;

/** Runs only the real menu selector. Never runs the cup workflow or starts a race. */
final class CupSelectionDeviceCheck implements LuaRuntime.Host {
    private final Instrumentation test;
    private final UiAutomation ui;
    private TouchService touch;
    private String game;
    private String expectedTitle;
    private Bitmap frame;
    private PixelFrame pixels;
    private File root;
    private int rotation,width,height,snapshots,targetTaps,position,result;
    CupSelectionDeviceCheck(Instrumentation test){this.test=test;ui=test.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);}
    void run(int position,String expectedTitle,Bundle report)throws Exception{
        if(SessionState.running)throw new IllegalStateException("Stop the current task before checking cup selection");
        if(position==0||position>6||position< -30)throw new IllegalArgumentException("Device check supports positions 1–6 and -1–-30");
        if(expectedTitle.isBlank())throw new IllegalArgumentException("Supply cupTitle to verify the actual event, not only tap coordinates");
        this.expectedTitle=expectedTitle;
        this.position=position;LocalStore store=new LocalStore(test.getTargetContext());rotation=store.config.optInt("rotation",90);
        game=store.config.getString("gamePkg");Intent launch=test.getTargetContext().getPackageManager().getLaunchIntentForPackage(game);
        if(launch==null)throw new IllegalStateException("Game unavailable");
        test.getTargetContext().startActivity(new Intent().setComponent(launch.getComponent()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Thread.sleep(1200);
        root=new File(test.getTargetContext().getCacheDir(),"cup-selection-check");root.mkdirs();
        try{
            capture();
            long ready=SystemClock.elapsedRealtime()+10000;
            while(TouchService.instance==null&&SystemClock.elapsedRealtime()<ready)Thread.sleep(100);
            touch=TouchService.instance;if(touch==null)throw new IllegalStateException("Accessibility service unavailable");
            touch.configure(width,height,rotation);touch.arm(game);
            try(LuaRuntime lua=new LuaRuntime(this,List.of("keepCapture","releaseCapture","tap","swipe","findColor","findPic","snapShot","print","toast","done","_cupCards"))){
                lua.set("LOCAL_ROOT",root.getAbsolutePath());lua.set("run",Map.of("index",3,"data",Map.of("杯赛位置",String.valueOf(position))));lua.set("data",Map.of());
                lua.load("bootstrap",store.asset("lua/bootstrap.lua"));lua.load("utilities",store.asset("lua/utilities.lua"));
                lua.load("selection-only","function selectionOnly()done(utils.chooseCup('"+position+"'))end");lua.start("selectionOnly");
                long start=SystemClock.elapsedRealtime();boolean alive=true;
                while(alive&&SystemClock.elapsedRealtime()-start<120000){alive=lua.step(SystemClock.elapsedRealtime()-start);Thread.sleep(25);}
                if(alive)throw new AssertionError("Cup selector timed out");
            }
            Thread.sleep(1000);
            if(result!=1||snapshots!=1||targetTaps<1)throw new AssertionError("result="+result+", snapshots="+snapshots+", targetTaps="+targetTaps);
            verifyTitle(true);
            File screenshot=new File(test.getTargetContext().getExternalFilesDir(null),"cup-selection-after.png");
            Bitmap actual=ui.takeScreenshot();if(actual==null)throw new AssertionError("No screenshot");
            try(FileOutputStream out=new FileOutputStream(screenshot)){actual.compress(Bitmap.CompressFormat.PNG,100,out);}finally{actual.recycle();}
            report.putString("result","PASS: real chooseCup selected position "+position+", verified event title "+expectedTitle+" before and after entering; no race started");
            report.putString("screenshot",screenshot.getAbsolutePath());
        }finally{if(touch!=null)touch.disarm();if(frame!=null)frame.recycle();}
    }
    private void verifyTitle(boolean detail)throws Exception{
        TextRecognizer ocr=TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
        String title="";
        try{
            for(int attempt=0;attempt<6;attempt++){
                Bitmap screen=ui.takeScreenshot();if(screen==null)throw new AssertionError("No screenshot for event verification");
                Bitmap header=Bitmap.createBitmap(screen,0,60,screen.getWidth(),Math.min(screen.getHeight()-60,330));
                Bitmap enlarged=Bitmap.createScaledBitmap(header,header.getWidth()*2,header.getHeight()*2,true);
                try{title=Tasks.await(ocr.process(InputImage.fromBitmap(enlarged,0)),15,TimeUnit.SECONDS).getText().replaceAll("\\s+","");}
                finally{enlarged.recycle();header.recycle();screen.recycle();}
                if(MapMatcher.normalize(title).contains(MapMatcher.normalize(expectedTitle))&&(!detail||title.contains("单人奖励")))return;
                Thread.sleep(700);
            }
            throw new AssertionError("Wrong event "+(detail?"detail":"selection")+": expected "+expectedTitle+", visible "+title);
        }finally{ocr.close();}
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
    private void tap(int x,int y){requireGame();touch.tap(8,x,y);}
    private void swipe(int x1,int y1,int x2,int y2,int duration){requireGame();touch.swipe(x1,y1,x2,y2,duration);}
    @Override public int call(String name,Lua lua){
        try{
            switch(name){
                case "keepCapture":capture();return 0;
                case "_cupCards":{
                    var cards=CupCarousel.cards(pixels);
                    android.util.Log.i("A9CupCheck","cards "+cards+" capture="+width+"x"+height+" rotation="+rotation);
                    try(FileOutputStream out=new FileOutputStream(new File(test.getTargetContext().getExternalFilesDir(null),"cup-cards-debug.png"))){frame.compress(Bitmap.CompressFormat.PNG,100,out);}
                    LuaRuntime.push(lua,cards);return 1;
                }
                case "releaseCapture":return 0;
                case "tap":{
                    int x=number(lua,1),y=number(lua,2);if(x==100&&(position<0||y==(position==1?150:120+180*(position-1))))targetTaps++;
                    android.util.Log.i("A9CupCheck","tap "+x+","+y);
                    tap(x,y);return 0;
                }
                case "swipe":swipe(number(lua,1),number(lua,2),number(lua,3),number(lua,4),number(lua,5));return 0;
                case "findColor":{
                    android.util.Log.i("A9CupCheck","find "+lua.toString(5)+" at "+number(lua,1)+","+number(lua,2)+" actual="+Integer.toHexString(pixels.pixel(number(lua,1)+2,number(lua,2)+2)));
                    for(int y=number(lua,2);y<=number(lua,4);y++)for(int x=number(lua,1);x<=number(lua,3);x++)
                        if(pixels.inBounds(x,y)&&ColorPattern.color(pixels.pixel(x,y),lua.toString(5),1)){lua.push(x);lua.push(y);return 2;}
                    lua.push(-1);lua.push(-1);return 2;
                }
                case "findPic":throw new AssertionError("New selection reused an old image cache");
                case "snapShot":{
                    verifyTitle(false);
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
