package dev.funlee.a9;

import android.content.*;
import android.graphics.*;
import android.hardware.display.*;
import android.hardware.HardwareBuffer;
import android.hardware.SyncFence;
import android.media.*;
import android.media.projection.*;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import dev.funlee.a9.core.GameViewport;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

/** Captures the full display. Owns every Image/Bitmap and closes them promptly. */
public final class CaptureSession implements AutoCloseable {
    static final class OrientationPending extends IllegalStateException {
        OrientationPending(String message){super(message);}
    }
    static final class Snapshot {
        final Bitmap bitmap;
        final GameViewport viewport;
        Snapshot(Bitmap bitmap,GameViewport viewport){this.bitmap=bitmap;this.viewport=viewport;}
    }
    private MediaProjection projection;
    private ImageReader reader;
    private VirtualDisplay display;
    private final HandlerThread thread=new HandlerThread("a9-capture");
    private Bitmap latest;
    private Bitmap beforeResize;
    private long frameTime,lastReadFrame,lastMotionSample;
    private final int[] motionReference=new int[12*8],motionSample=new int[12*8];

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
        ImageReader next=createReader(w,h);
        next.setOnImageAvailableListener(r->{
            // Closing an ImageReader invalidates acquired plane buffers. Hold the same lock
            // as close/resize until the copy is owned by our Bitmap, including image.close().
            synchronized(this) {
                if(r!=reader)return;
                try(Image image=r.acquireLatestImage()) {
                    if(image==null)return;
                    Bitmap frame=copyImage(image);
                    if(latest!=null)latest.recycle();latest=frame;frameTime=SystemClock.elapsedRealtime();observeMotion(frame,frameTime);notifyAll();
                    if(beforeResize!=null){beforeResize.recycle();beforeResize=null;}
                } catch(Exception e) { SessionState.log("截屏失败："+e.getMessage()); }
            }
        },handler);
        return next;
    }
    private void observeMotion(Bitmap frame,long now){
        if(now-lastMotionSample<500)return;
        // Exclude the top counters and bottom activity hint from the motion sample.
        int changed=0;
        for(int i=0;i<motionSample.length;i++){
            int x=(2*(i%12)+1)*frame.getWidth()/24,y=(i/12+2)*frame.getHeight()/13;
            int c=frame.getPixel(x,y),old=motionReference[i];
            motionSample[i]=c;
            if(Math.abs(Color.red(c)-Color.red(old))+Math.abs(Color.green(c)-Color.green(old))
                    +Math.abs(Color.blue(c)-Color.blue(old))>75)changed++;
        }
        if(lastMotionSample==0||changed>=4){
            System.arraycopy(motionSample,0,motionReference,0,motionSample.length);
            SessionState.lastScreenChange=now;
        }
        lastMotionSample=now;
    }
    static ImageReader createReader(int w,int h) {
        if(Build.VERSION.SDK_INT>=33)return ImageReader.newInstance(w,h,PixelFormat.RGBA_8888,3,
                HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE|HardwareBuffer.USAGE_CPU_READ_OFTEN);
        return ImageReader.newInstance(w,h,PixelFormat.RGBA_8888,3);
    }
    static Bitmap copyImage(Image image)throws Exception {
        if(Build.VERSION.SDK_INT>=33){
            // API 36 emulator plane locking leaked one sync_file per frame in a bounded
            // replay despite Image.close(). Avoid that path; retain only an owned CPU copy.
            try(SyncFence fence=image.getFence()){
                if(!fence.await(java.time.Duration.ofSeconds(2)))throw new IllegalStateException("等待截屏像素就绪超时");
            }
            try(HardwareBuffer buffer=image.getHardwareBuffer()){
                if(buffer==null)throw new IllegalStateException("截屏缓冲区不可用");
                Bitmap hardware=Bitmap.wrapHardwareBuffer(buffer,ColorSpace.get(ColorSpace.Named.SRGB));
                if(hardware==null)throw new IllegalStateException("无法读取截屏缓冲区");
                try{
                    Bitmap copy=hardware.copy(Bitmap.Config.ARGB_8888,false);
                    if(copy==null)throw new IllegalStateException("无法复制截屏像素");
                    return copy;
                }finally{hardware.recycle();}
            }
        }
        Image.Plane plane=image.getPlanes()[0];ByteBuffer buffer=plane.getBuffer();
        Bitmap padded=Bitmap.createBitmap(plane.getRowStride()/plane.getPixelStride(),image.getHeight(),Bitmap.Config.ARGB_8888);
        try{
            padded.copyPixelsFromBuffer(buffer);
            Bitmap copy=Bitmap.createBitmap(padded,0,0,image.getWidth(),image.getHeight());
            if(copy==padded){Bitmap owned=padded;padded=null;return owned;}
            return copy;
        }finally{if(padded!=null)padded.recycle();}
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
        if(latest!=null){
            if(beforeResize!=null)beforeResize.recycle();
            beforeResize=latest;latest=null;
        }
        SessionState.log("画面尺寸更新："+w+"×"+h);
    }
    public synchronized void awaitFrameAfter(long after) throws InterruptedException {
        long until=SystemClock.elapsedRealtime()+3000;
        while(frameTime<=after&&display!=null&&SystemClock.elapsedRealtime()<until)wait(Math.max(1,until-SystemClock.elapsedRealtime()));
        if(frameTime<=after)throw new IllegalStateException("等待悬浮窗避让后的新截图超时");
    }
    synchronized Snapshot frame(int clockwise,Supplier<Rect> gameBounds) {
        long until=SystemClock.elapsedRealtime()+3000;
        while (true) {
            OrientationPending pending = null;
            if (latest != null) {
                try {
                    GameViewport viewport = viewport(latest.getWidth(), latest.getHeight(), clockwise, gameBounds);
                    lastReadFrame = frameTime;
                    return new Snapshot(normalizeFrame(latest, viewport), viewport);
                } catch (OrientationPending e) {
                    pending = e;
                }
            }
            long remaining = until - SystemClock.elapsedRealtime();
            if (display == null || remaining <= 0) {
                if (pending != null) throw pending;
                throw new IllegalStateException("未收到截屏画面，请停止后重新授权截屏");
            }
            // Accessibility window bounds can settle after the first landscape frame.
            try { wait(remaining); }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("截屏已停止");
            }
        }
    }
    private static GameViewport viewport(int width,int height,int clockwise,Supplier<Rect> gameBounds) {
        if(!GameViewport.supports(width,height,clockwise))throw orientationPending(width,height);
        // A framed game on a wide display may occupy a smaller, offset application window.
        if(width>height*2.07&&gameBounds!=null){
            Rect bounds=gameBounds.get();
            if(bounds!=null){
                GameViewport window=new GameViewport(bounds.left,bounds.top,bounds.width(),bounds.height(),clockwise);
                if(!window.isFullHeightWindow(width,height))throw orientationPending(width,height);
                return window;
            }
        }
        return new GameViewport(0,0,width,height,clockwise);
    }
    private static OrientationPending orientationPending(int width,int height) {
        return new OrientationPending("画面方向或比例不匹配，请让游戏保持横屏并关闭分屏或小窗（当前 "+width+"×"+height+"）");
    }
    /** Converts an owned snapshot without consuming it; shared by live capture and rotation replay. */
    static Bitmap normalizeFrame(Bitmap source,int clockwise,int width,int height) {
        return normalizeFrame(source,viewport(width,height,clockwise,null));
    }
    static Bitmap normalizeFrame(Bitmap source,GameViewport viewport) {
        Matrix m=new Matrix();m.postRotate(viewport.rotation);
        Bitmap rotated=Bitmap.createBitmap(source,viewport.left,viewport.top,viewport.width,viewport.height,m,false);
        Bitmap scaled=Bitmap.createScaledBitmap(rotated,720,1280,false);
        if(rotated!=source && rotated!=scaled)rotated.recycle();
        return scaled==source?scaled.copy(Bitmap.Config.ARGB_8888,false):scaled;
    }
    public synchronized long frameId(){return lastReadFrame;}

    // Inspect just the opaque navy header and the pink close-button area before spending an OCR pass.
    // Capture coordinates here are the upright display, independent of the automation's rotation setting.
    synchronized Bitmap possibleAccountDialog() {
        if(latest==null||!hasDialogHeader(latest))return null;
        int w=Math.min(1280,latest.getWidth());
        int h=Math.max(1,Math.round((float)w*latest.getHeight()/latest.getWidth()));
        Bitmap scaled=Bitmap.createScaledBitmap(latest,w,h,true);
        return scaled==latest?latest.copy(Bitmap.Config.ARGB_8888,false):scaled;
    }
    static boolean hasDialogHeader(Bitmap image) {
        int w=image.getWidth(),h=image.getHeight();
        if(w<h)return false;
        for(int y=h/6;y<h*2/5;y+=Math.max(1,h/80)){
            int center=image.getPixel(w/2,y);
            if(Color.red(center)>45||Color.green(center)>60||Color.blue(center)>80||
                    Color.blue(center)<=Color.red(center))continue;
            int left=image.getPixel(w/3,y);
            if(Math.abs(Color.red(left)-Color.red(center))>15||Math.abs(Color.blue(left)-Color.blue(center))>18)continue;
            for(int x=w*7/10;x<w*97/100;x+=Math.max(1,w/90)){
                int right=image.getPixel(x,y);
                if(Color.red(right)>225&&Color.green(right)<45&&Color.blue(right)>55&&Color.blue(right)<155)return true;
            }
        }
        return false;
    }

    synchronized Bitmap copyLatestFrame() {
        Bitmap frame = latest == null ? beforeResize : latest;
        return frame == null ? null : frame.copy(Bitmap.Config.ARGB_8888, false);
    }

    @Override public synchronized void close() {
        displays.unregisterDisplayListener(displayListener);
        if(display!=null) {display.release();display=null;}
        if(reader!=null) {reader.close();reader=null;}
        if(projection!=null) {projection.unregisterCallback(callback);projection.stop();projection=null;}
        if(latest!=null) {latest.recycle();latest=null;}
        if(beforeResize!=null) {beforeResize.recycle();beforeResize=null;}
        notifyAll();
        thread.quitSafely();
    }
}
