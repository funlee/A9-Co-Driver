package dev.funlee.a9;

import android.content.*;
import android.graphics.*;
import android.hardware.display.*;
import android.media.*;
import android.media.projection.*;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import java.nio.ByteBuffer;

/** Captures the full display. Owns every Image/Bitmap and closes them promptly. */
public final class CaptureSession implements AutoCloseable {
    private MediaProjection projection;
    private ImageReader reader;
    private VirtualDisplay display;
    private final HandlerThread thread=new HandlerThread("a9-capture");
    private Bitmap latest;
    private long frameTime,lastReadFrame;
    public volatile int width,height;
    private final int density;
    private final DisplayManager displays;
    private final Runnable onStopped;
    private final MediaProjection.Callback callback=new MediaProjection.Callback() {
        @Override public void onStop() { onStopped.run(); }
        @Override public void onCapturedContentResize(int w,int h) { resize(w,h); }
    };
    private final DisplayManager.DisplayListener displayListener=new DisplayManager.DisplayListener() {
        public void onDisplayAdded(int id){}
        public void onDisplayRemoved(int id){}
        public void onDisplayChanged(int id){
            if(Build.VERSION.SDK_INT>=34)return; // The projection callback is authoritative on current Android.
            if(id!=android.view.Display.DEFAULT_DISPLAY)return;
            DisplayMetrics m=new DisplayMetrics();android.view.Display d=displays.getDisplay(id);
            if(d!=null){d.getRealMetrics(m);resize(m.widthPixels,m.heightPixels);}
        }
    };
    @SuppressWarnings("deprecation") public CaptureSession(Context context,int resultCode,Intent data,Runnable onStopped) {
        this.onStopped=onStopped;
        DisplayMetrics metrics=new DisplayMetrics(); ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(metrics);
        // Service WindowMetrics can describe the natural portrait bounds even while MuMu's
        // display is landscape. MediaProjection requires the actual current display dimensions.
        width=metrics.widthPixels;height=metrics.heightPixels;
        SessionState.log("创建截屏："+width+"×"+height);
        density=context.getResources().getConfiguration().densityDpi;
        displays=(DisplayManager)context.getSystemService(Context.DISPLAY_SERVICE);
        thread.start(); Handler h=new Handler(thread.getLooper());
        projection=((MediaProjectionManager)context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)).getMediaProjection(resultCode,data);
        projection.registerCallback(callback,h);
        reader=newReader(width,height,h);
        display=projection.createVirtualDisplay("A9Local",width,height,density,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,h);
        displays.registerDisplayListener(displayListener,h);
    }
    private ImageReader newReader(int w,int h,Handler handler) {
        ImageReader next=ImageReader.newInstance(w,h,PixelFormat.RGBA_8888,3);
        next.setOnImageAvailableListener(r->{
            try(Image image=r.acquireLatestImage()) {
                if(image==null)return;
                Image.Plane p=image.getPlanes()[0]; ByteBuffer buffer=p.getBuffer(); int stride=p.getPixelStride();
                int paddedWidth=p.getRowStride()/stride;
                Bitmap padded=Bitmap.createBitmap(paddedWidth,image.getHeight(),Bitmap.Config.ARGB_8888); padded.copyPixelsFromBuffer(buffer);
                Bitmap frame=Bitmap.createBitmap(padded,0,0,image.getWidth(),image.getHeight()); if(frame!=padded)padded.recycle();
                synchronized(this) {
                    if(r!=reader){frame.recycle();return;}
                    if(latest!=null)latest.recycle();latest=frame;frameTime=SystemClock.elapsedRealtime();notifyAll();
                }
            } catch(Exception e) { SessionState.log("截屏失败："+e.getMessage()); }
        },handler);
        return next;
    }
    private synchronized void resize(int w,int h) {
        if(display==null||w<=0||h<=0||(width==w&&height==h))return;
        ImageReader previous=reader;
        display.setSurface(null);
        reader=newReader(w,h,new Handler(thread.getLooper()));
        ImageReader next=reader;
        display.resize(w,h,density);width=w;height=h;
        // Let WindowManager apply the new recording bounds before attaching the replacement surface.
        new Handler(thread.getLooper()).postDelayed(()->{synchronized(this){if(display!=null&&reader==next)display.setSurface(next.getSurface());}},200);
        if(previous!=null)previous.close();
        if(latest!=null){latest.recycle();latest=null;}
        SessionState.log("画面尺寸更新："+w+"×"+h);
    }
    public synchronized void awaitFrameAfter(long after) throws InterruptedException {
        long until=SystemClock.elapsedRealtime()+3000;
        while(frameTime<=after&&display!=null&&SystemClock.elapsedRealtime()<until)wait(Math.max(1,until-SystemClock.elapsedRealtime()));
        if(frameTime<=after)throw new IllegalStateException("等待悬浮窗避让后的新截图超时");
    }
    public synchronized Bitmap frame(int clockwise) {
        long until=SystemClock.elapsedRealtime()+3000;
        while(latest==null&&display!=null&&SystemClock.elapsedRealtime()<until){
            try{wait(Math.max(1,until-SystemClock.elapsedRealtime()));}
            catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("截屏已停止");}
        }
        if(latest==null) throw new IllegalStateException("未收到截屏画面，请停止后重新授权截屏");
        lastReadFrame=frameTime;
        Matrix m=new Matrix();m.postRotate(clockwise);
        Bitmap rotated=Bitmap.createBitmap(latest,0,0,latest.getWidth(),latest.getHeight(),m,false);
        if(Math.abs(rotated.getWidth()/(double)rotated.getHeight()-720.0/1280)>.08) {
            if(rotated!=latest)rotated.recycle();
            throw new IllegalStateException("画面方向或比例不匹配，请在设置中校准旋转方向（当前 "+width+"×"+height+"）");
        }
        Bitmap scaled=Bitmap.createScaledBitmap(rotated,720,1280,false);
        if(rotated!=latest && rotated!=scaled)rotated.recycle();
        return scaled==latest?scaled.copy(Bitmap.Config.ARGB_8888,false):scaled;
    }
    public synchronized long frameId(){return lastReadFrame;}
    @Override public synchronized void close() {
        displays.unregisterDisplayListener(displayListener);
        if(display!=null) {display.release();display=null;}
        if(reader!=null) {reader.close();reader=null;}
        if(projection!=null) {projection.unregisterCallback(callback);projection.stop();projection=null;}
        if(latest!=null) {latest.recycle();latest=null;}
        notifyAll();
        thread.quitSafely();
    }
}
