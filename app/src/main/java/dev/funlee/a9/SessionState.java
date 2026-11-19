package dev.funlee.a9;

import android.os.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class SessionState {
    private static final List<String> lines=new ArrayList<>();
    private static final Handler main=new Handler(Looper.getMainLooper());
    public static volatile String status="尚未启动", hud="";
    public static volatile boolean running,paused,controlsOpen;
    static boolean inputPaused(){return paused||controlsOpen;}
    public static volatile boolean inspecting;
    public static volatile dev.funlee.a9.core.RunIssue issue;
    public static volatile boolean dashboardEnabled;
    public static volatile dev.funlee.a9.core.TaskStats stats=new dev.funlee.a9.core.TaskStats();
    public static String panelText(){return dashboardEnabled?stats.panel(paused?"已暂停":running?hud:status):hud;}
    public static volatile android.graphics.Rect overlayBounds;
    public static volatile boolean resultOverlay;
    public static volatile boolean overlayMenuCompact;
    public static volatile boolean racePanelVisible;
    public static void racePanel(boolean visible) {
        if(racePanelVisible==visible)return;
        racePanelVisible=visible;
        TouchService touch=TouchService.instance;
        if(visible&&touch!=null)touch.clearActivityHint();
        Runnable refresh=overlayRefresh;if(refresh!=null)main.post(refresh);
    }
    public static volatile Runnable overlayRefresh;
    public static void resultOverlay(boolean value) throws InterruptedException {
        overlayMode(value,overlayMenuCompact);
    }
    public static boolean overlayMode(boolean result,boolean menu) throws InterruptedException {
        if(resultOverlay==result&&overlayMenuCompact==menu)return false;
        boolean wasFull=racePanelVisible&&!resultOverlay&&!overlayMenuCompact;
        resultOverlay=result;overlayMenuCompact=menu;
        Runnable refresh=overlayRefresh;if(refresh==null)return false;
        java.util.concurrent.CountDownLatch done=new java.util.concurrent.CountDownLatch(1);
        main.post(()->{try{refresh.run();}finally{done.countDown();}});
        if(!done.await(2,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("悬浮窗避让超时");
        return wasFull!=(racePanelVisible&&!resultOverlay&&!overlayMenuCompact);
    }
    public static volatile Runnable listener;
    public static synchronized void log(String message) {
        android.util.Log.i("A9Local",message);
        String stamp=new SimpleDateFormat("HH:mm:ss",Locale.ROOT).format(new Date());
        lines.add(stamp+"  "+message); while(lines.size()>400) lines.remove(0);
        changed();
    }
    public static synchronized String logs() { return String.join("\n",lines); }
    public static void hud(String value){
        String previous=hintLine(hud);
        hud=value;
        String message=hintLine(value);
        if(!message.equals(previous))hint(message);
        changed();
    }
    private static String hintLine(String value){
        if(value==null)return "";
        int end=value.indexOf('\n');
        String first=(end<0?value:value.substring(0,end)).trim();
        if(first.startsWith("匹配 "))return "正在匹配对手";
        if(first.startsWith("设置车库筛选"))return "正在设置选车筛选";
        return first;
    }
    static void hint(String value){
        TouchService touch=TouchService.instance;
        if(running&&!inspecting&&!racePanelVisible&&touch!=null){
            String message=hintLine(value);
            if(message.startsWith("多人流程：")||message.startsWith("智能选路 ")
                    ||message.equals("比赛中")||message.equals("多人选路执行中")
                    ||message.equals("等待可靠的进度画面"))return;
            touch.offerActivityHint(message);
        }
    }
    public static void status(String value) { status=value; log(value);hint(value); }
    public static void failure(String detail) {
        issue=dev.funlee.a9.core.RunIssue.from(detail);
        log(detail);
        status="任务停止："+issue.title;
        changed();
    }
    public static void clearIssue() { issue=null;changed(); }
    public static void changed() { Runnable l=listener; if(l!=null) main.post(l); }
}
