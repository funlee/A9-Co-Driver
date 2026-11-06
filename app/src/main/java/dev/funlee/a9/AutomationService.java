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
    private volatile LuaRuntime runtime;
    private volatile boolean stopping,paused;
    private volatile boolean inspecting,sampleRequested;
    private WindowManager windows;
    private View overlay;
    private WindowManager.LayoutParams overlayParams;
    private boolean resultDocked;
    private int savedOverlayX,savedOverlayY;
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
            stopping=false;SessionState.running=true;SessionState.paused=false;SessionState.hud="";
            SessionState.stats=new TaskStats();SessionState.dashboardEnabled=!inspecting;SessionState.status("正在准备本地任务");
            worker=new Thread(()->execute(inspecting),"a9-automation");worker.start();
        }catch(Exception e){SessionState.status("启动失败："+e.getMessage());stopRun();}
        return START_NOT_STICKY;
    }
    private PendingIntent action(String value,int id) {
        return PendingIntent.getService(this,id,new Intent(this,AutomationService.class).setAction(value),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private Notification notification() {
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("A9 本地助手")
                .setContentText("截屏与任务运行中，可随时暂停或停止").setContentIntent(open).setOngoing(true)
                .addAction(new Notification.Action.Builder(null,inspecting?"取样":"暂停 / 继续",action(inspecting?SAMPLE:PAUSE,1)).build())
                .addAction(new Notification.Action.Builder(null,"停止",action(STOP,2)).build()).build();
    }
    private void execute(boolean inspect) {
        try {
            LocalStore store=new LocalStore(this);TouchService touch=TouchService.instance;
            int target=0;
            org.json.JSONArray projects=store.config.getJSONArray("projects");
            for(int i=0;i<projects.length();i++){org.json.JSONObject p=projects.getJSONObject(i);if(p.optBoolean("enabled")&&(p.optString("项目").equals("多人上")||p.optString("项目").equals("多人下")))target+=p.getJSONObject("data").optInt("次数",1);}
            SessionState.stats.target(store.config.optBoolean("repeat",false)?0:target);
            if(touch==null&&!inspect)throw new IllegalStateException("请先开启 A9 本地助手的无障碍服务");
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
                try(LuaRuntime lua=new LuaRuntime(host,AndroidHost.FUNCTIONS)) {
                    runtime=lua;lua.set("LOCAL_ROOT",host.workPath());lua.set("UIconfig",store.luaConfig());
                    Map<String,Object> routes=new LinkedHashMap<>();for(Route route:store.routes.values())routes.put(route.name,route.raw);lua.set("ROUTES",routes);
                    for(String name:List.of("bootstrap","shared","utilities","workflows","multiplayer_up","multiplayer_down","multiplayer_current","cup","special","club","career","daily"))
                        lua.load(name,store.asset("lua/"+name+".lua"));
                    lua.start("main");long origin=SystemClock.elapsedRealtime(),pauseAt=0,pausedTotal=0;
                    SessionState.status("任务已启动");
                    while(!stopping&&!host.finished) {
                        long now=SystemClock.elapsedRealtime();
                        if(paused){if(pauseAt==0){pauseAt=now;host.pause();SessionState.log("已暂停，触点与待执行动作已释放");}Thread.sleep(50);continue;}
                        if(pauseAt>0){pausedTotal+=now-pauseAt;pauseAt=0;lua.load("resume","data['心跳时间']=os.time()");}
                        host.clock=now-origin-pausedTotal;
                        SessionState.stats.elapsed(host.clock);
                        if(!lua.step(host.clock))break;
                        Thread.sleep(10);
                    }
                    if(!stopping)SessionState.status("本轮任务已完成");
                }finally{runtime=null;}
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
        LinearLayout box=TaskPanelView.create(this,overlayLabel,pauseButton,stop);
        android.graphics.Point displaySize=new android.graphics.Point();windows.getDefaultDisplay().getRealSize(displaySize);
        // Stay left of the first-column fuel labels (upright x=220) even with the full dashboard.
        WindowManager.LayoutParams params=new WindowManager.LayoutParams(displaySize.x*200/1280,WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
        // Keep the recovered route-choice coordinates near the top of the race unobstructed.
        // Keep bottom-centre "Later"/continue buttons visible to OCR and touch dispatch.
        params.gravity=Gravity.BOTTOM|Gravity.LEFT;params.x=8;params.y=8;
        overlayLabel.setOnTouchListener(new View.OnTouchListener(){float startX,startY;int x,y;
            public boolean onTouch(View v,android.view.MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){startX=e.getRawX();startY=e.getRawY();x=params.x;y=params.y;return true;}
                if(e.getAction()==MotionEvent.ACTION_MOVE){params.x=x+(int)(e.getRawX()-startX);params.y=y-(int)(e.getRawY()-startY);windows.updateViewLayout(box,params);return true;}
                if(e.getAction()==MotionEvent.ACTION_UP&&Math.abs(e.getRawX()-startX)<6&&Math.abs(e.getRawY()-startY)<6)v.performClick();return true;}});
        overlay=box;overlayParams=params;SessionState.resultOverlay=false;resultDocked=false;
        SessionState.overlayRefresh=this::updateOverlay;
        windows.addView(box,params);main.post(overlayTick);
    }
    private void updateOverlay(){
        if(overlay!=null&&overlayParams!=null&&resultDocked!=SessionState.resultOverlay){
            if(SessionState.resultOverlay){
                savedOverlayX=overlayParams.x;savedOverlayY=overlayParams.y;
                android.graphics.Point size=new android.graphics.Point();windows.getDefaultDisplay().getRealSize(size);
                overlayParams.gravity=Gravity.TOP|Gravity.LEFT;overlayParams.x=8;overlayParams.y=size.y*180/720;
            }else{overlayParams.gravity=Gravity.BOTTOM|Gravity.LEFT;overlayParams.x=savedOverlayX;overlayParams.y=savedOverlayY;}
            resultDocked=SessionState.resultOverlay;windows.updateViewLayout(overlay,overlayParams);
        }
        if(overlayLabel!=null){overlayLabel.setText(SessionState.panelText());pauseButton.setText(inspecting?"取样":paused?"继续":"暂停");if(overlay!=null){int[] p=new int[2];overlay.getLocationOnScreen(p);SessionState.overlayBounds=new android.graphics.Rect(p[0],p[1],p[0]+overlay.getWidth(),p[1]+overlay.getHeight());}}}
    private void removeOverlay(){SessionState.overlayRefresh=null;SessionState.resultOverlay=false;overlayParams=null;SessionState.overlayBounds=null;main.removeCallbacks(overlayTick);if(overlay!=null){try{windows.removeView(overlay);}catch(Exception ignored){}overlay=null;}}
    @Override public void onDestroy(){stopRun();super.onDestroy();}
}
