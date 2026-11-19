package dev.funlee.a9;

import android.app.Instrumentation;
import android.graphics.*;
import android.media.*;
import android.os.*;
import android.view.Surface;
import android.system.Os;
import java.io.File;

/** Bounded synthetic ImageReader exercise: never launches a game or an automation task. */
final class CaptureResourceDeviceCheck {
    private static int fences(){
        int count=0;String[] names=new File("/proc/self/fd").list();
        if(names==null)throw new IllegalStateException("Cannot inspect own descriptors");
        for(String name:names)try{if(Os.readlink("/proc/self/fd/"+name).contains("sync_file"))count++;}catch(Exception ignored){}
        return count;
    }
    private static void exercise(Bundle report,int width,int height,String... modes)throws Exception{
        ImageReader reader=CaptureSession.createReader(width,height);
        Surface surface=reader.getSurface();Paint paint=new Paint();
        try{
            for(String mode:modes){
                int start=fences();
                for(int i=0;i<120;i++){
                    int left=i%2==0?Color.RED:Color.BLUE,right=i%2==0?Color.BLUE:Color.RED;
                    Canvas canvas=surface.lockHardwareCanvas();
                    canvas.drawColor(left);paint.setColor(right);canvas.drawRect(width/2,0,width,height,paint);surface.unlockCanvasAndPost(canvas);
                    Image image=null;long until=SystemClock.elapsedRealtime()+2000;
                    while(image==null&&SystemClock.elapsedRealtime()<until){image=reader.acquireLatestImage();if(image==null)Thread.sleep(5);}
                    if(image==null)throw new AssertionError("Synthetic frame unavailable");
                    try{
                        if(mode.equals("planes"))image.getPlanes()[0].getBuffer().get(0);
                        if(mode.equals("ownedCopy")){
                            Bitmap copy=CaptureSession.copyImage(image);
                            try{
                                if(copy.getWidth()!=width||copy.getHeight()!=height||copy.getPixel(width/4,height/2)!=left||copy.getPixel(width*3/4,height/2)!=right)
                                    throw new AssertionError("Captured pixels differ: "+width+"x"+height);
                            }finally{copy.recycle();}
                        }
                    }finally{image.close();}
                }
                int end=fences();String key=mode+"-"+width+"x"+height;
                report.putString(key,start+" -> "+end);
                android.util.Log.i("A9CaptureTest",key+": "+start+" -> "+end);
                if(mode.equals("ownedCopy")&&end-start>20)throw new AssertionError("Capture leaked fences: "+start+" -> "+end);
            }
        }finally{surface.release();reader.close();}
    }
    static void run(Instrumentation test,Bundle report)throws Exception{
        if(SessionState.running)throw new IllegalStateException("Stop automation before capture resource check");
        exercise(report,128,128,"acquireOnly","planes","ownedCopy");
        exercise(report,1280,720,"ownedCopy");
        exercise(report,720,1280,"ownedCopy");
        report.putString("result","PASS: 360 owned copies preserve pixels across square, landscape and portrait sizes with bounded fence counts; no game task");
    }
}
