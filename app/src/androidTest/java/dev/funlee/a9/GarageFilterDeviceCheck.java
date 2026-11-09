package dev.funlee.a9;
import android.app.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import dev.funlee.a9.core.*;
import party.iroiro.luajava.Lua;
import java.io.*;
import java.util.*;

/** Real filter helper only: no car selection, race, fuel, ticket or purchase input. */
final class GarageFilterDeviceCheck implements LuaRuntime.Host {
    private final Instrumentation test;
    private final UiAutomation ui;
    private PixelFrame frame;
    private int width,height,optionTaps;
    GarageFilterDeviceCheck(Instrumentation test){this.test=test;ui=test.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);}
    private PixelFrame pixels(Bitmap bitmap){
        Bitmap scaled=Bitmap.createScaledBitmap(bitmap,1280,720,false);Matrix m=new Matrix();m.postRotate(90);
        Bitmap side=Bitmap.createBitmap(scaled,0,0,1280,720,m,false);int[] p=new int[720*1280];side.getPixels(p,0,720,0,0,720,1280);
        side.recycle();if(scaled!=bitmap)scaled.recycle();return new PixelFrame(720,1280,p);
    }
    private void requireGame()throws Exception {
        try(ParcelFileDescriptor fd=ui.executeShellCommand("dumpsys window");InputStream in=new FileInputStream(fd.getFileDescriptor())){
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int length;while((length=in.read(buffer))!=-1)bytes.write(buffer,0,length);
            for(String line:bytes.toString("UTF-8").split("\n"))
                if(line.contains("mCurrentFocus=")&&line.contains(" com.aligames.kuang.kybc.aligames/"))return;
        }
        throw new IllegalStateException("Game must be foreground");
    }
    private void capture()throws Exception {
        requireGame();Bitmap b=ui.takeScreenshot();if(b==null)throw new IllegalStateException("No screenshot");
        width=b.getWidth();height=b.getHeight();frame=pixels(b);b.recycle();
    }
    private void save(String name)throws Exception {
        Bitmap b=ui.takeScreenshot();try(FileOutputStream out=new FileOutputStream(new File(test.getTargetContext().getExternalFilesDir(null),name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}finally{b.recycle();}
    }
    private void tap(int x,int y)throws Exception {
        requireGame();float px=y*(width-1)/1279f,py=(719-x)*(height-1)/719f;long now=SystemClock.uptimeMillis();
        for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){
            MotionEvent e=MotionEvent.obtain(now,SystemClock.uptimeMillis(),action,px,py,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            try{if(!ui.injectInputEvent(e,true))throw new IllegalStateException("Input rejected");}finally{e.recycle();}
        }
    }
    void run(Bundle report)throws Exception {
        if(SessionState.running)throw new IllegalStateException("Stop automation first");
        for(String name:List.of("reference-upright","filter-off","filter-on")){
            Bitmap b=BitmapFactory.decodeStream(test.getContext().getAssets().open("garage-"+name+".png"));Map<String,Object> state=GarageFilter.read(pixels(b));b.recycle();
            int expected=name.equals("filter-on")?1:0;
            if(!state.equals(Map.of("present",true,"owned",expected,"rank",expected,"descending",expected)))throw new AssertionError(name+": "+state);
        }
        capture();if(!Boolean.TRUE.equals(GarageFilter.read(frame).get("present")))throw new IllegalStateException("Open filter panel first");
        LocalStore store=new LocalStore(test.getTargetContext());
        try(LuaRuntime lua=new LuaRuntime(this,List.of("keepCapture","tap","_garageFilterState"))){
            lua.load("utilities",store.asset("lua/utilities.lua"));
            lua.load("filter-only","data={};function test()utils.configureGarageFilter('0,1,2') end");
            for(int run=0;run<2;run++){
                int before=optionTaps;lua.start("test");long start=SystemClock.elapsedRealtime();boolean alive=true;
                while(alive&&SystemClock.elapsedRealtime()-start<30000){alive=lua.step(SystemClock.elapsedRealtime()-start);Thread.sleep(25);}
                if(alive)throw new AssertionError("Filter timed out");
                capture();if(Boolean.TRUE.equals(GarageFilter.read(frame).get("present")))throw new AssertionError("Filter still open");
                if(run==1&&optionTaps!=before)throw new AssertionError("Repeated invocation toggled selected options");
                save("garage-applied.png");tap(600,1192);Thread.sleep(800);capture();
                if(!GarageFilter.read(frame).equals(Map.of("present",true,"owned",1,"rank",1,"descending",1)))throw new AssertionError("Incorrect saved filter: "+GarageFilter.read(frame));
            }
            save("garage-verified.png");tap(134,240);
        }
        report.putString("result","PASS: supplied screenshot + live off/on fixtures; real Lua applied owned/rank/descending, reopened and verified persistence; repeated call preserved all options; no race started");
        report.putInt("optionTaps",optionTaps);
    }
    @Override public int call(String name,Lua lua){try{
        if(name.equals("keepCapture")){capture();return 0;}
        if(name.equals("_garageFilterState")){LuaRuntime.push(lua,GarageFilter.read(frame));return 1;}
        if(name.equals("tap")){
            int x=(int)lua.toInteger(1),y=(int)lua.toInteger(2);
            if((x==509||x==304||x==436)&&y==340)optionTaps++;
            else if(!(x==134&&y==240)&&!(x==600&&y==1192))throw new AssertionError("Non-filter tap");
            tap(x,y);return 0;
        }
        throw new AssertionError(name);
    }catch(Exception e){throw new IllegalStateException(e);}}
}
