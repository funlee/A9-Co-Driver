package dev.funlee.a9;

import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.*;
import dev.funlee.a9.core.GameViewport;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.*;

/** Real display rotations and production frame normalization/readiness; no game or race is launched.
 * UiAutomation snapshots substitute for MediaProjection acquisition, so projection resize callbacks
 * are deliberately outside this check's coverage. */
final class RotationRecoveryDeviceCheck {
    private final Instrumentation test;
    private final Handler main=new Handler(Looper.getMainLooper());
    private UiAutomation ui;
    private Instrumentation.ActivityMonitor monitor;
    private MainActivity activity;
    private TouchService touch;
    private final AtomicReference<Throwable> asyncFailure=new AtomicReference<>();
    private RotationRecoveryDeviceCheck(Instrumentation test){this.test=test;}
    static void run(Instrumentation test,Bundle report)throws Exception{new RotationRecoveryDeviceCheck(test).run(report);}
    private MainActivity current(){
        Activity latest=monitor.getLastActivity();
        return latest instanceof MainActivity?(MainActivity)latest:activity;
    }
    private void rotate(int orientation){
        try{current().setRequestedOrientation(orientation);}catch(Throwable e){asyncFailure.compareAndSet(null,e);}
    }
    private Bitmap screenshot(){
        Bitmap bitmap=ui.takeScreenshot();if(bitmap==null)throw new AssertionError("No real display snapshot");return bitmap;
    }
    private void awaitShape(boolean landscape)throws Exception{
        long until=SystemClock.elapsedRealtime()+10000;
        while(SystemClock.elapsedRealtime()<until){
            if(asyncFailure.get()!=null)throw new AssertionError(asyncFailure.get());
            Bitmap snapshot=screenshot();boolean matches;
            try{matches=(snapshot.getWidth()>snapshot.getHeight())==landscape;}finally{snapshot.recycle();}
            if(matches&&touch.gameInForeground())return;
            Thread.sleep(100);
        }
        throw new AssertionError("Display did not reach "+(landscape?"landscape":"portrait")+" with helper foreground");
    }
    private boolean normalizedSnapshot(){
        Bitmap snapshot=screenshot();
        try{
            Bitmap normalized=CaptureSession.normalizeFrame(snapshot,90,snapshot.getWidth(),snapshot.getHeight());
            try{if(normalized.getWidth()!=720||normalized.getHeight()!=1280)throw new AssertionError("Wrong normalized bounds");}
            finally{normalized.recycle();}
            return true;
        }finally{snapshot.recycle();}
    }
    static void displaySupport(Instrumentation test,Bundle report){
        android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();
        ((android.view.WindowManager)test.getTargetContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(metrics);
        boolean supported=GameViewport.supportsDevice(metrics.widthPixels,metrics.heightPixels);
        try{
            CaptureSession.requireSupportedDisplay(test.getTargetContext());
            if(!supported)throw new AssertionError("Unsupported real display admitted");
        }catch(CaptureSession.Unavailable expected){
            if(supported||!expected.getMessage().contains(GameViewport.DISPLAY_REQUIREMENT))throw expected;
        }
        report.putString("displaySize",metrics.widthPixels+"×"+metrics.heightPixels);
        report.putString("displayAdmission",supported?"supported":"rejected before capture allocation");
        for(int[] size:new int[][]{{1920,1080},{1440,720},{2670,1200},{720,1600},{800,1280}}){
            Bitmap raw=Bitmap.createBitmap(size[0],size[1],Bitmap.Config.ARGB_8888);
            try{
                try{
                    Bitmap unexpected=CaptureSession.normalizeFrame(raw,90,size[0],size[1]);unexpected.recycle();
                    throw new AssertionError("Unexpected resolution normalized: "+Arrays.toString(size));
                }catch(CaptureSession.Unavailable expected){
                    if(!expected.getMessage().contains(GameViewport.DISPLAY_REQUIREMENT))throw expected;
                }
            }finally{raw.recycle();}
        }
        Bitmap landscape=Bitmap.createBitmap(1280,720,Bitmap.Config.ARGB_8888);
        GameViewport viewport=new GameViewport(0,0,1280,720,90);
        try{
            landscape.eraseColor(Color.BLUE);
            float[] target=viewport.toScreen(200,1000);
            landscape.setPixel(Math.round(target[0]),Math.round(target[1]),Color.RED);
            Bitmap normalized=CaptureSession.normalizeFrame(landscape,90,1280,720);
            try{
                if(normalized.getWidth()!=720||normalized.getHeight()!=1280||
                        normalized.getPixel(0,0)!=Color.BLUE||normalized.getPixel(200,1000)!=Color.RED)
                    throw new AssertionError("Original capture and touch must agree without resampling");
                landscape.setPixel(0,0,Color.GREEN);
                if(normalized.getPixel(719,0)!=Color.BLUE)throw new AssertionError("Snapshot must own its bitmap");
            }finally{normalized.recycle();}
            try{
                Bitmap unexpected=CaptureSession.normalizeFrame(landscape,new GameViewport(80,0,1200,720,90));unexpected.recycle();
                throw new AssertionError("Offset crop admitted");
            }catch(CaptureSession.Unavailable expected){
                if(!expected.getMessage().contains(GameViewport.DISPLAY_REQUIREMENT))throw expected;
            }
            Bitmap portrait=CaptureSession.normalizeFrame(landscape,90,1280,720);
            try{
                Bitmap copy=CaptureSession.normalizeFrame(portrait,0,720,1280);
                try{if(copy==portrait||copy.getPixel(200,1000)!=Color.RED)throw new AssertionError("Portrait snapshot ownership broken");}
                finally{copy.recycle();}
            }finally{portrait.recycle();}
            report.putString("result","PASS: exact original display admission, other dimensions rejected, no crop/resampling, rotation pixels and bitmap ownership verified");
        }finally{landscape.recycle();}
    }
    private GameRecovery recovery(java.util.function.BooleanSupplier stopped,java.util.function.BooleanSupplier paused,Callable<Boolean> ready)throws Exception{
        return new GameRecovery(test.getTargetContext(),touch,test.getTargetContext().getPackageName(),stopped,paused,ready);
    }
    private void run(Bundle report)throws Exception{
        if(SessionState.running)throw new IllegalStateException("Stop automation before rotation check");
        Context context=test.getTargetContext();File saved=new File(context.getFilesDir(),"runtime/task-session.json");
        byte[] before=saved.isFile()?Files.readAllBytes(saved.toPath()):null;
        String originalStatus=SessionState.status,originalHud=SessionState.hud;
        try{
            displaySupport(test,report);
            ui=test.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
            monitor=test.addMonitor(MainActivity.class.getName(),null,false);
            activity=(MainActivity)test.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            long until=SystemClock.elapsedRealtime()+15000;
            while(TouchService.instance==null&&SystemClock.elapsedRealtime()<until)Thread.sleep(100);
            touch=TouchService.instance;if(touch==null)throw new IllegalStateException("Accessibility service must be enabled");
            touch.arm(context.getPackageName());
            for(int round=1;round<=2;round++){
                test.runOnMainSync(()->rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));awaitShape(false);
                AtomicInteger pending=new AtomicInteger(),valid=new AtomicInteger();AtomicLong firstValid=new AtomicLong();
                long started=SystemClock.elapsedRealtime();
                recovery(()->false,()->false,()->{
                    try{
                        boolean ready=normalizedSnapshot();valid.incrementAndGet();firstValid.compareAndSet(0,SystemClock.elapsedRealtime());return ready;
                    }catch(CaptureSession.OrientationPending e){
                        if(pending.incrementAndGet()==1)main.postDelayed(()->rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE),350);
                        throw e;
                    }
                }).awaitReady();
                if(pending.get()==0||valid.get()<2||SystemClock.elapsedRealtime()-firstValid.get()<2000)
                    throw new AssertionError("Recovery skipped observed portrait/stability window");
                report.putString("rotation"+round,"portrait retries="+pending.get()+", landscape reads="+valid.get()+", elapsedMs="+(SystemClock.elapsedRealtime()-started));
            }
            Bitmap landscape=screenshot();
            try{report.putString("landscapeSize",landscape.getWidth()+"×"+landscape.getHeight());}
            finally{landscape.recycle();}
            IllegalStateException actual=new IllegalStateException("non-transient capture failure");
            try{recovery(()->false,()->false,()->{throw actual;}).awaitReady();throw new AssertionError("Real error was swallowed");}
            catch(IllegalStateException expected){if(expected!=actual)throw expected;}
            report.putBoolean("realErrorsPropagate",true);
            test.runOnMainSync(()->rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));awaitShape(false);
            AtomicBoolean stopped=new AtomicBoolean();AtomicInteger pending=new AtomicInteger();long stopStarted=SystemClock.elapsedRealtime();
            try{
                recovery(stopped::get,()->false,()->{
                    try{return normalizedSnapshot();}
                    catch(CaptureSession.OrientationPending e){if(pending.incrementAndGet()==1)main.postDelayed(()->stopped.set(true),250);throw e;}
                }).awaitReady();throw new AssertionError("Stop request did not cancel recovery");
            }catch(InterruptedException expected){
                if(pending.get()==0||SystemClock.elapsedRealtime()-stopStarted>3000)throw new AssertionError("Stop not prompt during rotation");
            }
            report.putBoolean("stopCancelsRotation",true);
            AtomicBoolean pausedStop=new AtomicBoolean();main.postDelayed(()->pausedStop.set(true),250);
            try{recovery(pausedStop::get,()->true,()->{throw new AssertionError("Paused recovery sampled display");}).awaitReady();throw new AssertionError("Paused stop ignored");}
            catch(InterruptedException expected){report.putBoolean("stopCancelsPause",true);}
            if(asyncFailure.get()!=null)throw new AssertionError(asyncFailure.get());
            if(!Arrays.equals(before,saved.isFile()?Files.readAllBytes(saved.toPath()):null))throw new AssertionError("Saved task changed");
            report.putString("result","PASS: two real portrait-to-landscape recoveries through production normalization and readiness; stable frames required; non-transient errors propagate; stop cancels rotation and pause; saved task untouched. MediaProjection callbacks not covered.");
        }finally{
            main.removeCallbacksAndMessages(null);
            if(touch!=null)touch.disarm();
            if(activity!=null)test.runOnMainSync(()->{MainActivity a=current();a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);a.finish();});
            if(monitor!=null)test.removeMonitor(monitor);
            SessionState.status=originalStatus;SessionState.hud=originalHud;SessionState.changed();
        }
    }
}
