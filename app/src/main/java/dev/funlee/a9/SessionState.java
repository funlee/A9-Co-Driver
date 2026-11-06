package dev.funlee.a9;

import android.os.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class SessionState {
    private static final List<String> lines=new ArrayList<>();
    private static final Handler main=new Handler(Looper.getMainLooper());
    public static volatile String status="尚未启动", hud="";
    public static volatile boolean running,paused;
    public static volatile boolean dashboardEnabled;
    public static volatile dev.funlee.a9.core.TaskStats stats=new dev.funlee.a9.core.TaskStats();
    public static String panelText(){return dashboardEnabled?stats.panel(paused?"已暂停":running?hud:status):hud;}
    public static volatile android.graphics.Rect overlayBounds;
    public static volatile boolean resultOverlay;
    public static volatile Runnable overlayRefresh;
    public static void resultOverlay(boolean value) throws InterruptedException {
        if(resultOverlay==value)return;
        resultOverlay=value;
        Runnable refresh=overlayRefresh;if(refresh==null)return;
        java.util.concurrent.CountDownLatch done=new java.util.concurrent.CountDownLatch(1);
        main.post(()->{try{refresh.run();}finally{done.countDown();}});
        if(!done.await(2,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("悬浮窗避让超时");
    }
    public static volatile Runnable listener;
    public static synchronized void log(String message) {
        android.util.Log.i("A9Local",message);
        String stamp=new SimpleDateFormat("HH:mm:ss",Locale.ROOT).format(new Date());
        lines.add(stamp+"  "+message); while(lines.size()>400) lines.remove(0);
        changed();
    }
    public static synchronized String logs() { return String.join("\n",lines); }
    public static void status(String value) { status=value; log(value); }
    public static void changed() { Runnable l=listener; if(l!=null) main.post(l); }
}
