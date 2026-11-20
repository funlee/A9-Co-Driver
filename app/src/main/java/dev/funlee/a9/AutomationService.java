package dev.funlee.a9;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
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
    private static long runGeneration; // Accessed on the main looper, including screenshot callbacks.
    private Thread worker;
    private CaptureSession capture;
    private Intent captureConsent;
    private int captureResult;
    private volatile String gamePackage="";
    private org.json.JSONObject resumeState;
    private volatile LuaRuntime runtime;
    private volatile boolean stopping,paused;
    private volatile boolean inspecting,sampleRequested;
    private boolean waitForMenuOnStart;
    private WindowManager windows;
    private TaskPanelView overlay;
    private WindowManager.LayoutParams overlayParams;
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
        if(STOP.equals(intent.getAction())){SessionState.status("已手动终止任务");stopRun();return START_NOT_STICKY;}
        if(SAMPLE.equals(intent.getAction())){sampleRequested=true;closeTaskControls();return START_NOT_STICKY;}
        if(PAUSE.equals(intent.getAction())){
            paused=!paused;SessionState.paused=paused;
            TouchService touch=TouchService.instance;
            if(paused&&touch!=null){touch.releaseAll();touch.updateRecoveryWait(null);}
            closeTaskControls();
            SessionState.status(paused?"暂停任务":"继续运行");updateOverlay();return START_NOT_STICKY;
        }
        if(worker!=null){SessionState.log("已有运行中的任务");return START_NOT_STICKY;}
        try {
            runGeneration++;
            RunStatusRecord.clearFailure(this);
            waitForMenuOnStart=intent.getBooleanExtra("waitForMenuOnStart",false);
            inspecting=intent.getBooleanExtra("inspect",false);sampleRequested=false;
            SessionState.inspecting=inspecting;SessionState.clearIssue();
            TouchService touch=TouchService.instance;
            if(touch!=null)touch.dismissTaskFailure();
            NotificationManager notifications=getSystemService(NotificationManager.class);
            notifications.createNotificationChannel(new NotificationChannel(CHANNEL,"本地自动化",NotificationManager.IMPORTANCE_LOW));
            Notification notification=notification();
            if(Build.VERSION.SDK_INT>=29)startForeground(1,notification,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);else startForeground(1,notification);
            Intent consent=intent.getParcelableExtra("consent");
            if(consent==null)throw new IllegalStateException("缺少截屏授权，请重新启动");
            captureConsent=consent;captureResult=intent.getIntExtra("result",Activity.RESULT_CANCELED);
            stopping=false;paused=false;SessionState.running=true;SessionState.paused=false;SessionState.hud="";
            RunStatusRecord.save(this,true,"正在准备本地任务");
            resumeState=intent.getBooleanExtra("resume",false)?SessionRecord.read(this):null;
            if(intent.getBooleanExtra("resume",false)&&(resumeState==null||resumeState.optBoolean("complete")))throw new IllegalStateException("没有可继续的未完成任务");
            SessionState.stats=resumeState==null?new TaskStats():TaskStats.restore((Map<String,Object>)LocalStore.javaValue(resumeState.getJSONObject("stats")));
            SessionState.dashboardEnabled=!inspecting;SessionState.status(resumeState==null?"正在准备本地任务":"正在恢复上次任务，已完赛 "+SessionState.stats.finished()+" 局");
            worker=new Thread(()->execute(inspecting),"a9-automation");worker.start();
        }catch(Exception e){failRun("启动失败："+e.getMessage());}
        return START_NOT_STICKY;
    }
    private PendingIntent action(String value,int id) {
        return PendingIntent.getService(this,id,new Intent(this,AutomationService.class).setAction(value),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private Notification notification() {
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(getString(R.string.app_name))
                .setContentText("截屏与任务运行中，可随时暂停或停止").setContentIntent(open).setOngoing(true)
                .addAction(new Notification.Action.Builder(null,inspecting?"取样":"暂停 / 继续",action(inspecting?SAMPLE:PAUSE,1)).build())
                .addAction(new Notification.Action.Builder(null,"停止",action(STOP,2)).build()).build();
    }
    private void execute(boolean inspect) {
        try {
            LocalStore store=new LocalStore(this);TouchService touch=TouchService.instance;
            if(resumeState!=null){
                store.config=new org.json.JSONObject(resumeState.getJSONObject("config").toString());
                store.migrateConfig();
            }
            gamePackage=store.config.getString("gamePkg");
            // Old saved sessions may still carry a total-race target. Queue execution is unbounded.
            SessionState.stats.target(0);
            if(touch==null&&!inspect)throw new IllegalStateException(TouchService.unavailableReason(this));
            // MainActivity launches the game after receiving projection consent.
            Thread.sleep(3000);
            synchronized(this){
                if(stopping)return;
                capture=new CaptureSession(this,captureResult,captureConsent,()->main.post(()->{
                    failRun("截屏授权已结束");
                }));
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
                synchronized(this){
                    if(stopping)return;
                    touch.arm(gamePackage);
                }
                if(resumeState!=null)host.restoreQueue(resumeState);
                GameRecovery recovery=new GameRecovery(this,touch,gamePackage,()->stopping,SessionState::inputPaused,host::recoveryReady);
                recovery.awaitInitialForeground();
                if(waitForMenuOnStart)recovery.awaitReady();
                long origin=SystemClock.elapsedRealtime()-SessionState.stats.elapsedMillis(),pauseAt=0,pausedTotal=0;
                while(!stopping&&!host.finished){
                    boolean restarted=false;
                    boolean rebuildForQueue=false;
                    String homeFailure=null;
                    long portraitSince=-1,nextOrientationCheck=0;
                    GameRecovery.Reason interruptedAction=null;
                    try(LuaRuntime lua=new LuaRuntime(host,AndroidHost.FUNCTIONS)) {
                        runtime=lua;lua.set("LOCAL_ROOT",host.workPath());lua.set("UIconfig",store.luaConfig());
                        Map<String,Object> routes=new LinkedHashMap<>();for(Route route:store.routes.values())routes.put(route.name,route.raw);lua.set("ROUTES",routes);
                        for(String name:List.of("bootstrap","shared","utilities","rewards","workflows","multiplayer_up","multiplayer_down","multiplayer_current","cup","special","club","daily"))
                            lua.load(name,store.asset("lua/"+name+".lua"));
                        lua.start("main");
                        SessionState.status(SessionState.stats.restartCount()>0?"已恢复，继续剩余任务":"任务已启动");
                        while(!stopping&&!host.finished) {
                            long now=SystemClock.elapsedRealtime();
                            host.checkAccountLogin();
                            if(SessionState.inputPaused()){if(pauseAt==0){pauseAt=now;portraitSince=-1;host.pause();}Thread.sleep(50);continue;}
                            if(pauseAt>0){pausedTotal+=now-pauseAt;pauseAt=0;lua.load("resume","data['心跳时间']=os.time()");}
                            if(!touch.gameInForeground()){
                                portraitSince=-1;
                                long recoveryAt=SystemClock.elapsedRealtime();
                                restarted=recoverInterrupted(host,recovery,GameRecovery.Reason.FOREGROUND_LOST);
                                pausedTotal+=SystemClock.elapsedRealtime()-recoveryAt;
                                if(restarted)break;
                                host.pause();continue;
                            }
                            if(now>=nextOrientationCheck){
                                nextOrientationCheck=now+250;
                                if(GameRecovery.portrait(this)){
                                    if(portraitSince<0){portraitSince=now;host.pause();}
                                    if(now-portraitSince>=3000){interruptedAction=GameRecovery.Reason.PORTRAIT;break;}
                                }else portraitSince=-1;
                            }
                            if(portraitSince>=0){Thread.sleep(100);continue;}
                            host.clock=now-origin-pausedTotal;
                            SessionState.stats.elapsed(host.clock);
                            host.periodicSave();
                            try{
                                if(!lua.step(host.clock))break;
                            }catch(RuntimeException failed){
                                if(stopping)throw failed;
                                host.checkRecoveryAvailable();
                                boolean queueWork=host.runningQueueWork()||host.needsHome();
                                if(host.recoveryReason!=null&&(!queueWork||host.recoveryReason!=GameRecovery.Reason.EVENT_START_TIMEOUT)){
                                    interruptedAction=host.recoveryReason;
                                    break;
                                }
                                if(!queueWork)throw failed;
                                String message=businessReason(failed);
                                if(host.needsHome()){
                                    SessionState.log("回主未成功："+message);
                                    host.reserveHomeRestart();
                                    homeFailure=message;
                                }else{
                                    host.recordQueueFailure(message);
                                }
                                rebuildForQueue=true;
                                break;
                            }
                            Thread.sleep(10);
                        }
                    }finally{runtime=null;}
                    if(interruptedAction!=null){
                        long recoveryAt=SystemClock.elapsedRealtime();
                        restarted=recoverInterrupted(host,recovery,interruptedAction);
                        if(!restarted)restarted=recoverInterrupted(host,recovery,GameRecovery.Reason.INPUT_INTERRUPTED);
                        pausedTotal+=SystemClock.elapsedRealtime()-recoveryAt;
                    }
                    if(homeFailure!=null){
                        long recoveryAt=SystemClock.elapsedRealtime();
                        recovery.restartForHome(homeFailure);
                        pausedTotal+=SystemClock.elapsedRealtime()-recoveryAt;
                        restarted=true;
                    }
                    if(restarted||rebuildForQueue){host.resetWorkflow(restarted);continue;}
                    break;
                }
                if(!stopping){
                    if(!host.finished)throw new IllegalStateException("任务流程意外结束，已保留当前进度");
                    SessionState.status("本轮任务已完成");
                }
            }
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
            if(!stopping)main.post(()->failRun("任务执行意外中断，已保留当前进度"));
        }
        catch(Throwable e){
            String detail = e.getMessage();
            if (detail == null || detail.isEmpty()) detail = e.getClass().getSimpleName();
            String reason = detail;
            android.util.Log.w("A9Local", "任务异常", e);
            main.post(() -> failRun(reason));
        }
        finally{main.post(()->{if(!stopping)stopRun();});}
    }
    private boolean recoverInterrupted(AndroidHost host,GameRecovery recovery,GameRecovery.Reason reason)throws Exception{
        host.checkRecoveryAvailable();
        host.persist(false);
        return recovery.recover(reason,host.needsHome()?host::reserveHomeRestart:null);
    }
    private static String businessReason(RuntimeException failure){
        String message=failure.getMessage();
        if(message==null||message.isEmpty())return failure.getClass().getSimpleName();
        int trace=message.indexOf("\nstack traceback:");
        if(trace>=0)message=message.substring(0,trace);
        // The scheduler and the failing Lua chunk can both prefix their source locations.
        return message.replaceFirst("(?s)^.*?\\]:[0-9]+: (?:.*\\]:[0-9]+: )?","").trim();
    }

    private void failRun(String detail) {
        if (stopping) return;
        Bitmap frame = null;
        try {
            if (capture != null) frame = capture.copyLatestFrame();
        } catch (RuntimeException e) {
            SessionState.log("读取异常画面失败：" + e.getMessage());
        }
        long generation=runGeneration;
        boolean notifyUser=!inspecting;
        String game=gamePackage;
        FailureScreenshot.save(getApplicationContext(),frame,detail,saved->{
            if(!notifyUser||generation!=runGeneration)return;
            String message=RunIssue.stopMessage(detail,saved);
            RunStatusRecord.recordFailure(this,game,message);
            TouchService touch=TouchService.instance;
            if((touch==null||!touch.showPendingTaskFailure())&&SessionState.listener==null){
                NotificationManager manager=getSystemService(NotificationManager.class);
                if(manager.areNotificationsEnabled()){
                    String channel="task-failure";
                    manager.createNotificationChannel(new NotificationChannel(channel,"任务异常停止",NotificationManager.IMPORTANCE_HIGH));
                    PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),
                            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                    manager.notify(RunStatusRecord.FAILURE_NOTIFICATION,new Notification.Builder(this,channel)
                            .setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("任务已停止")
                            .setContentText(message.split("\n",2)[0]).setStyle(new Notification.BigTextStyle().bigText(message))
                            .setContentIntent(open).setAutoCancel(true).build());
                }
            }
            SessionState.changed();
        });
        SessionState.failure(detail);
        stopRun();
    }
    private synchronized void stopRun() {
        if(stopping)return;stopping=true;
        LuaRuntime lua=runtime;if(lua!=null)lua.requestStop();
        if(worker!=null)worker.interrupt();
        TouchService touch=TouchService.instance;if(touch!=null)touch.disarm();
        closeTaskControls();
        SessionState.running=false;SessionState.paused=false;SessionState.changed();
        RunStatusRecord.save(this,false,SessionState.status);
        if(capture!=null){capture.close();capture=null;}
        captureConsent=null;
        removeOverlay();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
    }
    private void closeTaskControls(){
        TouchService touch=TouchService.instance;
        if(touch!=null)touch.closeTaskControls();
    }
    private final Runnable overlayTick=new Runnable(){@Override public void run(){if(overlay==null)return;updateOverlay();main.postDelayed(this,500);}};
    private void addOverlay() {
        if(!Settings.canDrawOverlays(this)){SessionState.log("悬浮窗未开启，可从主界面停止；允许通知后也可使用通知控制");return;}
        windows=(WindowManager)getSystemService(WINDOW_SERVICE);
        overlayLabel=new DragLabel(this);
        pauseButton=new Button(this);pauseButton.setText(inspecting?"取样":"暂停");pauseButton.setOnClickListener(v->onStartCommand(new Intent().setAction(inspecting?SAMPLE:PAUSE),0,0));
        Button stop=new Button(this);stop.setText("停止");stop.setOnClickListener(v->onStartCommand(new Intent().setAction(STOP),0,0));
        TaskPanelView box=TaskPanelView.create(this,overlayLabel,pauseButton,stop);
        if(!inspecting)box.showRaceStatsOnly();
        android.graphics.Point size=overlaySize();
        WindowManager.LayoutParams params=new WindowManager.LayoutParams(size.x*TaskPanelView.WIDTH/1280,size.y*TaskPanelView.HEIGHT/720,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
        box.setCompact(!inspecting);box.setVisibility(View.GONE);
        PanelPlacement.apply(params,size,box.panelWidth(),box.panelHeight());
        overlay=box;overlayParams=params;SessionState.resultOverlay=false;SessionState.overlayMenuCompact=false;SessionState.racePanelVisible=false;
        box.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->overlayBounds());
        SessionState.overlayRefresh=this::updateOverlay;
        windows.addView(box,params);main.post(overlayTick);
    }
    private android.graphics.Point overlaySize(){android.graphics.Point p=new android.graphics.Point();windows.getDefaultDisplay().getRealSize(p);return p;}
    private void overlayBounds(){if(overlay==null||overlay.getVisibility()!=View.VISIBLE){SessionState.overlayBounds=null;return;}int[] p=new int[2];overlay.getLocationOnScreen(p);SessionState.overlayBounds=new android.graphics.Rect(p[0],p[1],p[0]+overlay.getWidth(),p[1]+overlay.getHeight());}
    private void updateOverlay(){
        if(overlay==null||overlayParams==null)return;
        android.graphics.Point size=overlaySize();
        boolean full=inspecting||(SessionState.racePanelVisible&&!SessionState.resultOverlay&&!SessionState.overlayMenuCompact);
        TouchService touch=TouchService.instance;
        boolean matching=!full&&SessionState.running&&SessionState.stats.isMatching()&&touch!=null&&touch.gameInForeground();
        if(!full&&!matching){
            overlay.setVisibility(View.GONE);overlayParams.flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            windows.updateViewLayout(overlay,overlayParams);overlayBounds();return;
        }
        overlay.setCompact(!full);
        PanelPlacement.apply(overlayParams,size,overlay.panelWidth(),overlay.panelHeight());
        overlay.setVisibility(View.VISIBLE);
        overlayParams.alpha=matching?.8f:1f;
        if(matching)overlayParams.flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        else overlayParams.flags&=~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        overlay.update(SessionState.stats,SessionState.paused?"已暂停":SessionState.running?SessionState.hud:SessionState.status,inspecting,SessionState.hud,SessionState.resultOverlay);
        pauseButton.setText(inspecting?"取样":paused?"继续":"暂停");windows.updateViewLayout(overlay,overlayParams);overlayBounds();
    }
    private void removeOverlay(){SessionState.racePanelVisible=false;SessionState.overlayRefresh=null;SessionState.resultOverlay=false;SessionState.overlayMenuCompact=false;overlayParams=null;SessionState.overlayBounds=null;main.removeCallbacks(overlayTick);if(overlay!=null){try{windows.removeView(overlay);}catch(Exception ignored){}overlay=null;}}
    @Override public void onDestroy(){
        if (!stopping && worker != null) failRun("任务服务意外停止");
        else stopRun();
        super.onDestroy();
    }
}
