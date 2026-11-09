package dev.funlee.a9;

import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.media.projection.MediaProjectionManager;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import dev.funlee.a9.core.*;
import java.util.*;

public final class AutomationService extends Service {
    public static final String START="dev.funlee.a9.START",STOP="dev.funlee.a9.STOP",PAUSE="dev.funlee.a9.PAUSE",SAMPLE="dev.funlee.a9.SAMPLE";
    private static final String CHANNEL="automation";
    private final Handler main=new Handler(Looper.getMainLooper());
    private Thread worker;
    private CaptureSession capture;
    private Intent captureConsent;
    private int captureResult;
    private org.json.JSONObject resumeState;
    private volatile LuaRuntime runtime;
    private volatile boolean stopping,paused;
    private volatile boolean inspecting,sampleRequested;
    private WindowManager windows;
    private TaskPanelView overlay;
    private WindowManager.LayoutParams overlayParams;
    private boolean resultDocked;
    private int savedOverlayX,savedOverlayY;
    private boolean overlayPositioned;
    private DragLabel overlayLabel;
    // The service uses the platform Material theme and native widgets.
    @android.annotation.SuppressLint("AppCompatCustomView")
    private static final class DragLabel extends TextView {
        DragLabel(Context context){super(context);}
        @Override public boolean performClick(){super.performClick();return true;}
    }
    private Button pauseButton;
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int id) {
        if(intent==null){stopSelf();return START_NOT_STICKY;}
        if(STOP.equals(intent.getAction())){stopRun();return START_NOT_STICKY;}
        if(SAMPLE.equals(intent.getAction())){sampleRequested=true;return START_NOT_STICKY;}
        if(PAUSE.equals(intent.getAction())){paused=!paused;SessionState.paused=paused;SessionState.status(paused?"暂停任务":"继续运行");updateOverlay();return START_NOT_STICKY;}
        if(worker!=null){SessionState.log("已有运行中的任务");return START_NOT_STICKY;}
        try {
            inspecting=intent.getBooleanExtra("inspect",false);sampleRequested=false;
            NotificationManager notifications=getSystemService(NotificationManager.class);
            notifications.createNotificationChannel(new NotificationChannel(CHANNEL,"本地自动化",NotificationManager.IMPORTANCE_LOW));
            Notification notification=notification();
            if(Build.VERSION.SDK_INT>=29)startForeground(1,notification,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);else startForeground(1,notification);
            Intent consent=intent.getParcelableExtra("consent");
            if(consent==null)throw new IllegalStateException("缺少截屏授权，请重新启动");
            captureConsent=consent;captureResult=intent.getIntExtra("result",Activity.RESULT_CANCELED);
            stopping=false;paused=false;SessionState.running=true;SessionState.paused=false;SessionState.hud="";
            resumeState=intent.getBooleanExtra("resume",false)?SessionRecord.read(this):null;
            if(intent.getBooleanExtra("resume",false)&&(resumeState==null||resumeState.optBoolean("complete")))throw new IllegalStateException("没有可继续的未完成任务");
            SessionState.stats=resumeState==null?new TaskStats():TaskStats.restore((Map<String,Object>)LocalStore.javaValue(resumeState.getJSONObject("stats")));
            SessionState.dashboardEnabled=!inspecting;SessionState.status(resumeState==null?"正在准备本地任务":"正在恢复上次任务，已完赛 "+SessionState.stats.finished()+" 局");
            worker=new Thread(()->execute(inspecting),"a9-automation");worker.start();
        }catch(Exception e){SessionState.status("启动失败："+e.getMessage());stopRun();}
        return START_NOT_STICKY;
    }
    private PendingIntent action(String value,int id) {
        return PendingIntent.getService(this,id,new Intent(this,AutomationService.class).setAction(value),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private Notification notification() {
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("A9伴侣")
                .setContentText("截屏与任务运行中，可随时暂停或停止").setContentIntent(open).setOngoing(true)
                .addAction(new Notification.Action.Builder(null,inspecting?"取样":"暂停 / 继续",action(inspecting?SAMPLE:PAUSE,1)).build())
                .addAction(new Notification.Action.Builder(null,"停止",action(STOP,2)).build()).build();
    }
    private void execute(boolean inspect) {
        try {
            LocalStore store=new LocalStore(this);TouchService touch=TouchService.instance;
            if(resumeState!=null)store.config=new org.json.JSONObject(resumeState.getJSONObject("config").toString());
            int limit=store.config.optInt("maxRaces",0);
            SessionState.stats.target(Math.max(0,limit));
            if(touch==null&&!inspect)throw new IllegalStateException("请先开启 A9伴侣的无障碍服务");
            // The activity launches the game immediately after consent. Let its display rotate first.
            Thread.sleep(3000);
            synchronized(this){
                if(stopping)return;
                capture=new CaptureSession(this,captureResult,captureConsent,()->main.post(this::stopRun));
                captureConsent=null;
            }
            main.post(()->{if(!stopping)addOverlay();});
            try(AndroidHost host=new AndroidHost(this,store,capture,touch)) {
                if(inspect){
                    SessionState.hud="屏幕诊断 · 进入目标画面后点击取样";SessionState.status("诊断已就绪，可通过悬浮窗或通知取样");
                    while(!stopping){
                        if(sampleRequested){sampleRequested=false;Thread.sleep(350);host.diagnostic();SessionState.status("取样完成，可继续取样或停止诊断");}
                        Thread.sleep(50);
                    }
                    return;
                }
                touch.arm(store.config.getString("gamePkg"));
                if(resumeState!=null)host.restoreQueue(resumeState);
                GameRecovery recovery=new GameRecovery(this,touch,store.config.getString("gamePkg"),()->stopping,()->paused);
                long origin=SystemClock.elapsedRealtime()-SessionState.stats.elapsedMillis(),pauseAt=0,pausedTotal=0;
                while(!stopping&&!host.finished){
                    boolean restarted=false;
                    try(LuaRuntime lua=new LuaRuntime(host,AndroidHost.FUNCTIONS)) {
                        runtime=lua;lua.set("LOCAL_ROOT",host.workPath());lua.set("UIconfig",store.luaConfig());
                        Map<String,Object> routes=new LinkedHashMap<>();for(Route route:store.routes.values())routes.put(route.name,route.raw);lua.set("ROUTES",routes);
                        for(String name:List.of("bootstrap","shared","utilities","workflows","multiplayer_up","multiplayer_down","multiplayer_current","cup","special","club","career","daily"))
                            lua.load(name,store.asset("lua/"+name+".lua"));
                        lua.start("main");
                        SessionState.status(SessionState.stats.restartCount()>0?"已恢复，继续剩余任务":"任务已启动");
                        while(!stopping&&!host.finished) {
                            long now=SystemClock.elapsedRealtime();
                            if(paused){if(pauseAt==0){pauseAt=now;host.pause();SessionState.log("已暂停，触点与待执行动作已释放");}Thread.sleep(50);continue;}
                            if(pauseAt>0){pausedTotal+=now-pauseAt;pauseAt=0;lua.load("resume","data['心跳时间']=os.time()");}
                            if(!touch.gameInForeground()){
                                long recoveryAt=SystemClock.elapsedRealtime();
                                restarted=recovery.recover(GameRecovery.Reason.FOREGROUND_LOST);
                                pausedTotal+=SystemClock.elapsedRealtime()-recoveryAt;
                                if(restarted)break;
                                host.pause();continue;
                            }
                            host.clock=now-origin-pausedTotal;
                            SessionState.stats.elapsed(host.clock);
                            host.periodicSave();
                            if(!lua.step(host.clock))break;
                            Thread.sleep(10);
                        }
                    }catch(RuntimeException e){
                        if(stopping||host.recoveryReason==null)throw e;
                        long recoveryAt=SystemClock.elapsedRealtime();
                        restarted=recovery.recover(host.recoveryReason);
                        pausedTotal+=SystemClock.elapsedRealtime()-recoveryAt;
                        if(!restarted)throw e; // A failed Lua input cannot be replayed after a mere window flicker.
                    }finally{runtime=null;}
                    if(restarted){host.recovered();continue;}
                    break;
                }
                if(!stopping)SessionState.status("本轮任务已完成");
            }
        }catch(InterruptedException e){Thread.currentThread().interrupt();}
        catch(Throwable e){if(!stopping){
            String detail=String.valueOf(e.getMessage()),message=detail;
            int trace=message.indexOf("\nstack traceback:");if(trace>=0)message=message.substring(0,trace);
            String prefix="java.lang.IllegalStateException: ";int api=message.indexOf(prefix);
            if(api>=0)message=message.substring(api+prefix.length());
            else message=message.replaceFirst("^\\[string .*?\\]:\\d+: ","");
            SessionState.log(detail);SessionState.status("任务停止："+message);
        }}
        finally{main.post(()->{if(!stopping)stopRun();});}
    }
    private synchronized void stopRun() {
        if(stopping)return;stopping=true;
        LuaRuntime lua=runtime;if(lua!=null)lua.requestStop();
        if(worker!=null)worker.interrupt();
        TouchService touch=TouchService.instance;if(touch!=null)touch.disarm();
        SessionState.running=false;SessionState.paused=false;SessionState.changed();
        if(capture!=null){capture.close();capture=null;}
        captureConsent=null;
        removeOverlay();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
    }
    private final Runnable overlayTick=new Runnable(){@Override public void run(){if(overlay==null)return;updateOverlay();main.postDelayed(this,500);}};
    private void addOverlay() {
        if(!Settings.canDrawOverlays(this)){SessionState.log("悬浮窗未开启，可从主界面停止；允许通知后也可使用通知控制");return;}
        windows=(WindowManager)getSystemService(WINDOW_SERVICE);
        overlayLabel=new DragLabel(this);
        pauseButton=new Button(this);pauseButton.setText(inspecting?"取样":"暂停");pauseButton.setOnClickListener(v->onStartCommand(new Intent().setAction(inspecting?SAMPLE:PAUSE),0,0));
        Button stop=new Button(this);stop.setText("停止");stop.setOnClickListener(v->{SessionState.status("已手动停止");stopRun();});
        TaskPanelView box=TaskPanelView.create(this,overlayLabel,pauseButton,stop);
        android.graphics.Point size=overlaySize();
        WindowManager.LayoutParams params=new WindowManager.LayoutParams(size.x*265/1280,WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.START;params.x=8;params.y=8;
        overlayLabel.setOnClickListener(v->{box.toggleCollapsed();updateOverlay();});
        overlayLabel.setOnTouchListener(new View.OnTouchListener(){float startX,startY;int x,y;boolean dragged;
            public boolean onTouch(View v,android.view.MotionEvent e){
                if(e.getAction()==MotionEvent.ACTION_DOWN){startX=e.getRawX();startY=e.getRawY();x=params.x;y=params.y;dragged=false;return true;}
                if(e.getAction()==MotionEvent.ACTION_MOVE){float dx=e.getRawX()-startX,dy=e.getRawY()-startY;if(Math.abs(dx)+Math.abs(dy)>12)dragged=true;
                    if(dragged&&!resultDocked){params.x=x+(int)dx;params.y=y+(int)dy;clampOverlay();windows.updateViewLayout(box,params);}return true;}
                if(e.getAction()==MotionEvent.ACTION_UP){if(!dragged)v.performClick();else if(!resultDocked){android.graphics.Point current=overlaySize();params.x=params.x+box.getWidth()/2<current.x/2?8:Math.max(8,current.x-box.getWidth()-8);clampOverlay();windows.updateViewLayout(box,params);getSharedPreferences("overlay",0).edit().putFloat("x",params.x/(float)Math.max(1,current.x-box.getWidth())).putFloat("y",params.y/(float)Math.max(1,current.y-box.getHeight())).apply();}return true;}return true;
            }});
        overlay=box;overlayParams=params;SessionState.resultOverlay=false;SessionState.overlayMenuCompact=false;resultDocked=false;overlayPositioned=false;
        box.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            if(overlay==null)return;
            if(!overlayPositioned){android.graphics.Point current=overlaySize();android.content.SharedPreferences prefs=getSharedPreferences("overlay",0);params.x=(int)(prefs.getFloat("x",0)*Math.max(1,current.x-box.getWidth()));params.y=(int)(prefs.getFloat("y",1)*Math.max(1,current.y-box.getHeight()-8));overlayPositioned=true;clampOverlay();windows.updateViewLayout(box,params);}
            else{int x=params.x,y=params.y;clampOverlay();if(x!=params.x||y!=params.y)windows.updateViewLayout(box,params);}
            overlayBounds();
        });
        SessionState.overlayRefresh=this::updateOverlay;
        windows.addView(box,params);main.post(overlayTick);
    }
    private android.graphics.Point overlaySize(){android.graphics.Point p=new android.graphics.Point();windows.getDefaultDisplay().getRealSize(p);return p;}
    private void clampOverlay(){if(overlay==null||overlayParams==null)return;android.graphics.Point p=overlaySize();overlayParams.x=Math.max(0,Math.min(overlayParams.x,Math.max(0,p.x-overlay.getWidth())));overlayParams.y=Math.max(0,Math.min(overlayParams.y,Math.max(0,p.y-overlay.getHeight())));}
    private void overlayBounds(){if(overlay==null)return;int[] p=new int[2];overlay.getLocationOnScreen(p);SessionState.overlayBounds=new android.graphics.Rect(p[0],p[1],p[0]+overlay.getWidth(),p[1]+overlay.getHeight());}
    private void updateOverlay(){
        if(overlay==null||overlayParams==null)return;
        boolean dock=SessionState.resultOverlay||SessionState.overlayMenuCompact;
        if(resultDocked!=dock){
            if(dock){savedOverlayX=overlayParams.x;savedOverlayY=overlayParams.y;}
            else{overlayParams.x=savedOverlayX;overlayParams.y=savedOverlayY;}
            resultDocked=dock;
        }
        if(dock){overlayParams.x=8;overlayParams.y=SessionState.resultOverlay?overlaySize().y*180/720:Math.max(0,overlaySize().y-overlay.getHeight()-8);}
        // Results must leave both reward crops and the personal-rank badges unobstructed.
        overlayParams.width=overlaySize().x*(SessionState.overlayMenuCompact?200:265)/1280;
        overlay.update(SessionState.stats,SessionState.paused?"已暂停":SessionState.running?SessionState.hud:SessionState.status,inspecting,SessionState.hud,SessionState.resultOverlay||SessionState.overlayMenuCompact);
        pauseButton.setText(inspecting?"取样":paused?"继续":"暂停");clampOverlay();windows.updateViewLayout(overlay,overlayParams);overlayBounds();
    }
    private void removeOverlay(){SessionState.overlayRefresh=null;SessionState.resultOverlay=false;SessionState.overlayMenuCompact=false;overlayParams=null;SessionState.overlayBounds=null;main.removeCallbacks(overlayTick);if(overlay!=null){try{windows.removeView(overlay);}catch(Exception ignored){}overlay=null;}}
    @Override public void onDestroy(){stopRun();super.onDestroy();}
}
