package dev.funlee.a9;

import android.content.*;
import android.graphics.*;
import android.os.*;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import dev.funlee.a9.core.*;
import party.iroiro.luajava.Lua;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Android implementation of the automation API used by the Lua workflows. */
public final class AndroidHost implements LuaRuntime.Host,AutoCloseable {
    public static final List<String> FUNCTIONS=List.of("keepCapture","releaseCapture","tap","swipe","touchDown","touchMove","touchUp",
            "cmpColorEx","cmpColor","getPixelColor","findColor","findMultiColor","findPic","snapShot","rotateImage",
            "getDisplaySize","getDisplayRotate","getWorkPath","getSdPath","getPackageName","getInstalledApk","frontAppName","runApp","stopApp","keyPress",
            "setScreenScale","setControlBarPos","rnd","toast","print","colorToRGB","_jsonEncode","_jsonDecode","_textOcr","_textWords","_digit","_mapMatch","_recoverEventStart","_requestGameRestart","_specialEventList","_recoveryUi","_popupUi","_legacyPopupProbe","_legacyPopupDismiss","_soloGarageScore","_soloDetailScore",
            "_validateRoute","_driverBegin","_driverStep","_driverEnd","_racePanel","_hud","_taskNotice","_finished","_multiplayerUi","_rewardUi","_fallbackBegin","_fallbackStep","_sessionCycle","_sessionProject","_sessionRaceBegin","_sessionRaceResult","_sessionRaceReturned","_sessionLegacyCompleted","_sessionLegacyPrepare","_sessionLegacyMap","_sessionOperation","_routeDisplay","_cupCards","_cupScrollDelta","_cupClaimState","_dailyEventUi","_soloEventUi","_soloGarageUi","_garageFilterState","_garageFilterPrepare","_dailyTaskStatus","_usedCarEntries","_selectCarEntry","_queueResume","_queueRewardsDone","_queueEnter","_queueDone","_queueBeginRewards","_queueHomeConfirmed","_requirePlayerAction");
    private final Context context;
    private final LocalStore store;
    private final CaptureSession capture;
    private final TouchService touch;
    private final TextRecognizer recognizer=TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
    private PixelFrame frame;
    private ModalPopup.Panel panelOnFrame;
    private boolean panelChecked;
    private RouteEngine driver;
    private final ProgressFilter progress=new ProgressFilter();
    private long driverStart,lastDigitTime,lastProgressTime,lastPulse,lastDriverResultCheck;
    private int lastProgress,lastLoggedProgress;
    private boolean nitroDrift,driverFallback;
    private String lastUiScene="";
    private long lastEmptyUiSnapshot,lastUnknownLog;
    private String lastUnknownText="";
    private final File root;
    private GameViewport viewport;
    public volatile boolean finished;
    public long clock;
    private final CupCarousel.ScrollTracker cupScroll = new CupCarousel.ScrollTracker();
    private final TaskCheckpoint checkpoint=new TaskCheckpoint();
    private boolean restoredQueue;
    private final MatchmakingWatchdog matching=new MatchmakingWatchdog();
    private long lastSavedClock;
    private long nextAccountCheck;
    private boolean accountDisplaced;
    private long lastPopupInput;
    private String backSkipReason="";
    private long backSkipAt;

    private void checkAccountText(String text){
        if(AccountLoginDialog.matches(text)){
            accountDisplaced=true;
            throw new IllegalStateException(AccountLoginDialog.STOP_REASON);
        }
    }

    void checkAccountLogin() throws Exception {
        if(accountDisplaced)throw new IllegalStateException(AccountLoginDialog.STOP_REASON);
        long now=SystemClock.elapsedRealtime();
        if(now<nextAccountCheck)return;
        nextAccountCheck=now+200;
        Bitmap candidate=capture.possibleAccountDialog();
        if(candidate==null)return;
        Task<Text> read=null;
        try{
            if(touch==null||!touch.gameInForeground())return;
            read=recognizer.process(InputImage.fromBitmap(candidate,0));
            checkAccountText(Tasks.await(read,12,TimeUnit.SECONDS).getText());
            nextAccountCheck=SystemClock.elapsedRealtime()+750;
        }finally{
            if(read!=null&&!read.isComplete())read.addOnCompleteListener(Runnable::run,done->candidate.recycle());
            else candidate.recycle();
        }
    }

    private boolean legacyPrepared,legacyRaceSeen,legacyInvalid;
    private String legacyMap="";
    public GameRecovery.Reason recoveryReason;
    private boolean runningQueueWork;
    private String hardStop;
    boolean needsHome(){return checkpoint.needsHome();}
    boolean runningQueueWork(){return runningQueueWork;}

    void checkRecoveryAvailable() throws Exception {
        if(hardStop!=null)throw new IllegalStateException(hardStop);
        CaptureSession.requireSupportedDisplay(context);
        if(capture.authorizationEnded())throw new IllegalStateException("截屏授权已结束");
        if(touch==null||touch!=TouchService.instance)throw new IllegalStateException("无障碍服务已断开，请重新连接");
        if(context.getSystemService(android.app.KeyguardManager.class).isKeyguardLocked())
            throw new IllegalStateException("设备已锁屏，无法继续操作游戏");
        checkAccountLogin();
    }
    void recordQueueFailure(String reason)throws Exception{
        checkpoint.failure(reason);
        runningQueueWork=false;
        persist(false);
        SessionState.status(checkpoint.failureSummary());
        saveRecoveryFrame(reason);
    }
    void reserveHomeRestart()throws Exception{
        if(!checkpoint.beginHomeRestart())throw new IllegalStateException("重启后仍未能确认游戏主界面，任务进度已保留");
        persist(false);
    }
    void saveRecoveryFrame(String reason){
        try{FailureScreenshot.save(context,capture.copyLatestFrame(),reason,null);}
        catch(RuntimeException e){FailureScreenshot.save(context,null,reason,null);SessionState.log("无法保存恢复前画面："+e.getMessage());}
    }
    public void resetWorkflow(boolean gameRestarted) throws InterruptedException{
        runningQueueWork=false;
        touch.releaseAll();
        driver=null;driverFallback=false;frame=null;progress.reset();matching.reset();lastUiScene="";finished=false;recoveryReason=null;
        SessionState.racePanel(false);SessionState.overlayMode(false,false);
        if(gameRestarted)SessionState.stats.recovered();else SessionState.stats.interrupted();
        legacyPrepared=legacyRaceSeen=false;
    }
    public void restoreQueue(JSONObject saved)throws Exception{
        JSONObject q=saved.getJSONObject("queue");
        int cycle=q.getInt("cycle");
        // Sessions from before round-based rewards resume at their existing task.
        checkpoint.restore(cycle,q.getInt("index"),q.getInt("completed"),q.optInt("rewardCycle",cycle));
        checkpoint.restoreRecovery(q.optInt("failures"),q.optBoolean("needsHome"),q.optBoolean("homeRestarted"),
                q.optBoolean("rewardWork"),q.optString("firstFailure"),q.optString("lastFailure"));
        JSONArray used=q.optJSONArray("usedCarEntries");
        if(used!=null){
            List<Integer> entries=new ArrayList<>();
            for(int i=0;i<used.length();i++)entries.add(used.getInt(i));
            checkpoint.restoreCarEntries(entries);
        }
        restoredQueue=true;
    }
    public void persist(boolean complete)throws Exception{
        try{SessionRecord.write(context,store.config,checkpoint,SessionState.stats,complete);lastSavedClock=clock;}
        catch(Exception e){hardStop="无法保存任务进度："+e.getMessage();throw e;}
    }
    public void periodicSave()throws Exception{if(clock-lastSavedClock>=5000)persist(finished);}
    public AndroidHost(Context context,LocalStore store,CaptureSession capture,TouchService touch) {
        this.context=context;this.store=store;this.capture=capture;this.touch=touch;
        root=new File(context.getFilesDir(),"runtime");root.mkdirs();
    }
    private JSONObject dailyTask(int index) throws JSONException {
        JSONObject task=store.config.getJSONArray("projects").getJSONObject(index-1);
        return "每日战利品".equals(task.optString("项目"))?task:null;
    }
    private String dailySkipReason(int index) throws JSONException {
        JSONObject task=dailyTask(index);
        return task==null?"":store.dailyTaskPolicy().skipReason();
    }
    public String workPath(){return root.getAbsolutePath();}
    private int n(Lua l,int i) {return (int)l.toInteger(i);}
    private double ratio(Lua l,int i) {return l.isNoneOrNil(i)?1:l.toNumber(i);}
    private String s(Lua l,int i) {return l.toString(i);}
    private int result(Lua l,Object... values) {for(Object value:values)LuaRuntime.push(l,value);return values.length;}
    private void capture() {
        CaptureSession.requireSupportedDisplay(context);
        if(touch!=null)touch.requireGameForeground();
        CaptureSession.Snapshot snapshot=capture.frame(store.config.optInt("rotation",90));
        Bitmap b=snapshot.bitmap;
        int[] p=new int[720*1280];b.getPixels(p,0,720,0,0,720,1280);b.recycle();
        viewport=snapshot.viewport;
        if(touch!=null){
            touch.configure(viewport);
            Rect hint=uprightBounds(touch.activityHintBounds(capture.frameId()));
            if(hint!=null&&hint.intersect(0,0,1280,720)){
                // The normalized pixel frame is the upright landscape view rotated clockwise.
                for(int x=hint.left;x<hint.right;x++)
                    java.util.Arrays.fill(p,x*720+720-hint.bottom,x*720+720-hint.top,0xff222e3a);
            }
        }
        frame=new PixelFrame(720,1280,p);
        panelChecked=false;
        if(legacyPrepared){
            boolean racing=RaceHud.visible(frame,wideViewport())&&popupPanel()==null;
            if(racing&&!legacyRaceSeen){
                legacyRaceSeen=true;SessionState.stats.begin(legacyMap,legacyInvalid);
                if(checkpoint.carRaceStarted()){
                    try{persist(false);}catch(Exception e){throw new IllegalStateException(hardStop,e);}
                }
                SessionState.activity("比赛开始，辅助驾驶中");
            }
            if(legacyRaceSeen)SessionState.racePanel(racing);
        }
    }
    private PixelFrame frame() {if(frame==null)capture();return frame;}
    private ModalPopup.Panel popupPanel(){
        PixelFrame current=frame();
        if(!panelChecked){
            panelOnFrame=ModalPopup.locate(current);
            panelChecked=true;
        }
        return panelOnFrame;
    }
    // All production recognition and task coordinates use the fixed 16:9 layout.
    private double viewportAspect(){return GameViewport.LANDSCAPE_ASPECT;}
    private boolean wideViewport(){return false;}
    private boolean ultrawideViewport(){return false;}
    private void addHomeButton(Map<String,Object> out){
        Map<String,Integer> button=RaceHud.homeButton(frame,ultrawideViewport());
        if(!button.isEmpty())out.put("homeButton",button);
    }
    private Rect excludedOverlay() {
        return uprightBounds(SessionState.overlayBounds);
    }
    private Rect uprightBounds(Rect overlay) {
        if(overlay==null)return null;
        float[] start=viewport.toUpright(overlay.left,overlay.top);
        float[] end=viewport.toUpright(overlay.right,overlay.bottom);
        return new Rect((int)Math.floor(Math.min(start[0],end[0])),(int)Math.floor(Math.min(start[1],end[1])),
                (int)Math.ceil(Math.max(start[0],end[0])),(int)Math.ceil(Math.max(start[1],end[1])));
    }
    /** Reward menus use fresh OCR, without multiplayer scene transitions or watchdog state. */
    public boolean recoveryReady()throws Exception{
        Map<String,Object> screen=rewardUi(false);
        String text=(String)screen.get("text");
        if(needsHome()&&"loginHistory".equals(screen.get("scene")))throw new IllegalStateException("登录历史需要玩家确认，请检查登录状态");
        if(needsHome()&&"rewardChoice".equals(screen.get("scene")))throw new IllegalStateException("请先手动选择画面上的动态奖励，再续跑");
        if(MaintenanceUi.detected(text,frame))throw new IllegalStateException("游戏维护中，任务暂停");
        if(RecoveryReadiness.restartScreen(frame))throw new GameRecovery.RestartScreen();
        if(RecoveryReadiness.incompatibleDevice(text))throw new GameRecovery.IncompatibleStartup();
        if("interruptingPopup".equals(screen.get("scene")))return screen.containsKey("dismissPopup");
        if(MultiplayerUi.limitedSalePromo(text))return PopupCloseDetector.findLimitedSale(frame)!=null;
        boolean homeButton=screen.containsKey("homeButton");ModalPopup.Panel panel=popupPanel();
        return RecoveryReadiness.ready(text,(String)screen.get("scene"),homeButton&&panel==null);
    }
    /**
     * One bounded Back press for a readable page the menu rules do not recognize, so a run that
     * starts on an unnamed subpage can navigate out instead of waiting for the restart timeout.
     */
    public boolean recoveryBack()throws Exception{
        if(touch==null||SessionState.inputPaused()||!touch.gameInForeground())return false;
        Map<String,Object> screen=rewardUi(false);
        String text=(String)screen.get("text"),scene=(String)screen.get("scene");
        if("exitGame".equals(scene)){
            // Back at Home opened the exit dialog: cancel it instead of leaving the game.
            Map<?,?> stay=uiWord(screen,"再玩一会");
            if(stay==null)stay=uiWord(screen,"再玩一会儿");
            if(stay==null)return false;
            touch.tap(8,719-((Number)stay.get("y")).intValue(),((Number)stay.get("x")).intValue());
            return true;
        }
        // A club page draws its own bright emblem where the race Back wedge sits, and shop
        // banners draw white shapes in the same corner. Only a real race HUD defers Back.
        if(!ModalPopup.clubPage(MapMatcher.normalize(text))&&RaceHud.backWedge(frame)
                &&RaceHud.raceEvidence(frame,wideViewport())){
            logBackSkip("画面仍是比赛提示");
            return false;
        }
        // A panel this build cannot act on leaves Back as the only control; the game
        // closes its own dialog on Back, and the exit dialog is handled above.
        boolean undismissable="interruptingPopup".equals(scene)&&!screen.containsKey("dismissPopup");
        if(!undismissable&&!RecoveryReadiness.navigable(text,scene)){
            logBackSkip("画面不属于可用返回键离开的页面（"+scene+"）");
            return false;
        }
        touch.key("back");
        return true;
    }
    private void logBackSkip(String reason){
        long now=SystemClock.elapsedRealtime();
        if(reason.equals(backSkipReason)&&now-backSkipAt<6000)return;
        backSkipReason=reason;backSkipAt=now;
        SessionState.log("返回键恢复暂缓："+reason);
    }
    private Map<?,?> uiWord(Map<String,Object> screen,String label){
        Object words=screen.get("words");if(!(words instanceof List))return null;
        for(Object line:(List<?>)words){
            Map<?,?> word=(Map<?,?>)line;Object value=word.get("text");
            if(value!=null&&MapMatcher.normalize(value.toString()).equals(label))return word;
        }
        return null;
    }
    /** The game's own start prompt after a cold launch; false when the frame is something else. */
    public boolean tapStartPrompt()throws Exception{
        String text=MapMatcher.normalize((String)rewardUi(false).get("text"));
        if(!text.contains("适龄提示")&&!text.contains("著作权登记")&&!text.contains("点击开始")&&!text.contains("taptostart"))return false;
        if(touch==null||SessionState.inputPaused()||!touch.gameInForeground())return false;
        touch.tap(0,359,640);
        return true;
    }
    private Map<String,Object> rewardUi(boolean keepDailyGoals) throws Exception {
        capture();
        Bitmap side=Bitmap.createBitmap(frame.copyPixels(),720,1280,Bitmap.Config.ARGB_8888);
        Matrix rotation=new Matrix();rotation.postRotate(270);
        Bitmap upright=Bitmap.createBitmap(side,0,0,720,1280,rotation,false);
        Task<Text> task=null;
        try {
            task=recognizer.process(InputImage.fromBitmap(upright,0));
            Text read=Tasks.await(task,12,TimeUnit.SECONDS);
            checkAccountText(read.getText());
            List<Object> words=new ArrayList<>();StringBuilder text=new StringBuilder();
            Rect excluded=excludedOverlay();
            for(Text.TextBlock block:read.getTextBlocks())for(Text.Line line:block.getLines()){
                Rect b=line.getBoundingBox();if(b==null||(excluded!=null&&Rect.intersects(excluded,b)))continue;
                words.add(Map.of("text",line.getText(),"x",b.centerX(),"y",b.centerY()));
                text.append(line.getText()).append('\n');
            }
            String menu=MainMenuUi.classify(text.toString(),words);
            String scene=menu.isEmpty()?MultiplayerUi.classify(text.toString()):menu;
            Map<String,Object> screen=new LinkedHashMap<>(Map.of("text",text.toString(),"words",words,
                    "frameId",capture.frameId(),"scene",scene));
            if(addLeagueDemotion(screen,words))screen.put("scene","leagueDemotion");
            if(addInterruptingPopup(screen,popupPanel(),words,keepDailyGoals))return screen;
            if(scene.equals("home"))screen.put("dailyGoalsHighlighted",MainMenuUi.dailyGoalsHighlighted(frame));
            addHomeButton(screen);
            return screen;
        } finally {
            Runnable cleanup=()->{upright.recycle();side.recycle();};
            if(task!=null&&!task.isComplete())task.addOnCompleteListener(Runnable::run,done->cleanup.run());else cleanup.run();
        }
    }
    private void prepareOverlay(boolean result,boolean menu)throws InterruptedException {
        long before=SystemClock.elapsedRealtime();
        if(SessionState.overlayMode(result,menu))capture.awaitFrameAfter(before);
    }
    private boolean addInterruptingPopup(Map<String,Object> screen,ModalPopup.Panel panel,List<?> words)throws Exception {
        return addInterruptingPopup(screen,panel,words,false);
    }
    private boolean addInterruptingPopup(Map<String,Object> screen,ModalPopup.Panel panel,List<?> words,boolean keepDailyGoals)throws Exception {
        if(screen.containsKey("demotionConfirm")||touch!=null&&touch.unlockGuard.active())return false;
        Map<String,Object> action=ModalPopup.dismiss(frame,panel,words,viewportAspect(),keepDailyGoals);
        if(action==null&&ModalPopup.possibleClubMilestone(frame)){
            List<Object> focused=new ArrayList<>(words);
            focused.add(Map.of("text",text(494,300,689,980,2),"x",640,"y",130));
            action=ModalPopup.dismiss(frame,panel,focused,viewportAspect(),keepDailyGoals);
        }
        if(action==null)return false;
        screen.put("scene","interruptingPopup");
        if(!action.isEmpty())screen.put("dismissPopup",action);
        return true;
    }
    private Map<String,Object> popupOverPixelMenu()throws Exception{
        if(popupPanel()==null&&!ModalPopup.possibleClubMilestone(frame)&&!ModalPopup.possibleRedCoinConversion(frame)&&
                !ModalPopup.possibleNewcomerGift(frame)&&!ModalPopup.possibleBlueprintGift(frame)&&PopupCloseDetector.findDailyGoals(frame)==null)return Map.of();
        Map<String,Object> screen=dailyEventUi(true);
        return "interruptingPopup".equals(screen.get("scene"))?screen:Map.of();
    }
    private CaptureSession.PopupSample legacyPopup(boolean backgroundOnly) {
        if(touch==null||SessionState.inputPaused()||!touch.gameInForeground())return null;
        CaptureSession.PopupSample sample=capture.peekLegacyPopup(viewport,backgroundOnly);
        if(sample!=null&&sample.rule==LegacyPopup.ACCOUNT){
            accountDisplaced=true;
            throw new IllegalStateException(AccountLoginDialog.STOP_REASON);
        }
        return touch.unlockGuard.active()?null:sample;
    }
    private boolean dismissLegacyPopup(String name,boolean continuation)throws Exception{
        CaptureSession.PopupSample sample=legacyPopup(false);
        if(sample==null||!sample.rule.name().equals(name))return false;
        LegacyPopup rule=sample.rule;
        if(continuation&&rule!=LegacyPopup.TIP_SEQUENCE)return false;
        Bitmap bitmap=capture.copyPopupFrame(viewport);
        if(bitmap==null)return false;
        Task<Text> task=null;
        try{
            task=recognizer.process(InputImage.fromBitmap(bitmap,270));
            String words=Tasks.await(task,12,TimeUnit.SECONDS).getText();
            checkAccountText(words);
            if(!rule.permitsInput(words))return false;
        }finally{
            if(task!=null&&!task.isComplete())task.addOnCompleteListener(Runnable::run,done->bitmap.recycle());
            else bitmap.recycle();
        }
        // OCR may span a page transition. Recheck the live game immediately before dispatch.
        sample=legacyPopup(false);
        if(sample==null||sample.rule!=rule)return false;
        if(driver!=null)driver.clear();
        touch.releaseAll();
        if(rule==LegacyPopup.AD_BACK)touch.key("back");
        else touch.tap(8,continuation?30:rule.tapX,continuation?1240:rule.tapY);
        lastPopupInput=SystemClock.elapsedRealtime();
        frame=null;
        SessionState.log("已处理游戏弹窗："+rule.name());
        return true;
    }
    private boolean freshAfterPopup(){
        // A sleeping task may still hold a button or swipe from the covered page.
        // Only a later observation by that task can authorize more input or a completion.
        return lastPopupInput==0||frame!=null&&capture.frameId()>lastPopupInput;
    }
    private Map<String,Object> multiplayerUi(boolean readSeriesScore,boolean raceExpected,boolean sampleGrid) throws Exception {
        boolean master=SessionState.stats.masterSeries();
        capture();
        ModalPopup.Panel panel=popupPanel();
        boolean unobstructed=panel==null&&!ModalPopup.possibleClubMilestone(frame)&&!ModalPopup.possibleBlueprintGift(frame);
        if(unobstructed&&sampleGrid&&!master&&lastUiScene.equals("cars")){
            Map<String,Object> garage=MultiplayerGarage.slots(frame,List.of(),"",viewportAspect());
            if(!((List<?>)garage.get("cards")).isEmpty()&&!MultiplayerGarage.leagueTabs(frame).isEmpty())
                return Map.of("frameId",capture.frameId(),"scene","cars","garage",garage,"gridOnly",true);
        }
        Map<String,Object> out=new LinkedHashMap<>();out.put("frameId",capture.frameId());
        // Menu transitions may look like the wide race HUD before verified vehicle Start.
        if(unobstructed&&raceExpected&&(touch==null||!touch.unlockGuard.active())&&RaceHud.visible(frame,wideViewport())) {
            matching.reset();SessionState.stats.matching(-1);
            SessionState.overlayMode(false,false);out.put("scene","race");out.put("text","");out.put("words",List.of());return out;
        }
        Bitmap sideways=Bitmap.createBitmap(frame.copyPixels(),720,1280,Bitmap.Config.ARGB_8888);
        Matrix matrix=new Matrix();matrix.postRotate(270);
        Bitmap upright=Bitmap.createBitmap(sideways,0,0,720,1280,matrix,false);
        Task<Text> task;
        try{task=recognizer.process(InputImage.fromBitmap(upright,0));}
        catch(Exception e){upright.recycle();sideways.recycle();throw e;}
        task.addOnCompleteListener(Runnable::run,done->{upright.recycle();sideways.recycle();});
        Text result=Tasks.await(task,12,TimeUnit.SECONDS);
        checkAccountText(result.getText());
        List<Object> words=new ArrayList<>();StringBuilder visible=new StringBuilder(),mapTitle=new StringBuilder();
        int[] laterBounds=null;
        Rect excluded=excludedOverlay();
        for(Text.TextBlock block:result.getTextBlocks())for(Text.Line line:block.getLines()) {
            Rect b=line.getBoundingBox();if(b==null)continue;
            if(excluded!=null&&Rect.intersects(excluded,b))continue;
            if(VehicleUnlockGuard.laterLabel(line.getText(),b.left,b.top,b.right,b.bottom))laterBounds=new int[]{b.left,b.top,b.right,b.bottom};
            Map<String,Object> word=new LinkedHashMap<>();word.put("text",line.getText());
            word.put("x",b.centerX());word.put("y",b.centerY());words.add(word);
            visible.append(line.getText()).append('\n');
            // Loading-screen map title is above the player roster; player names are not map evidence.
            if(b.centerX()>80&&b.centerX()<550&&b.centerY()>35&&b.centerY()<190)mapTitle.append(line.getText()).append('\n');
        }
        String text=visible.toString();out.put("text",text);out.put("words",words);
        if(text.isEmpty()&&clock-lastEmptyUiSnapshot>5000) {
            lastEmptyUiSnapshot=clock;
            snapshot(new File(root,"multiplayer-empty.png").getAbsolutePath(),0,0,719,1279);
            SessionState.log("界面文字为空，已保存当前截屏用于排查");
        }
        String scene=MultiplayerUi.classify(text,words);
        boolean trial=VehicleUnlockGuard.trialStart(text,words);
        if(trial&&scene.equals("lockedCar"))scene="car";
        if(addLeagueDemotion(out,words))scene="leagueDemotion";
        if(touch!=null){
            touch.unlockGuard.observe(VehicleUnlockGuard.prompt(text),laterBounds,!scene.equals("unknown")&&!scene.equals("vehicleUnlock"));
            if(touch.unlockGuard.active()){
                matching.reset();SessionState.stats.matching(-1);
                out.put("scene","vehicleUnlock");int[] later=touch.unlockGuard.laterCenter();
                if(later!=null)out.put("unlockLater",Map.of("x",later[0],"y",later[1]));
                return out;
            }
        }
        // A filtered two-column garage (e.g. owned cars sorted by descending rank)
        // can satisfy the popup panel geometry when fewer than five cars remain.
        // A readable card grid proves the "panel" is the garage itself, not a modal.
        if(panel!=null&&scene.equals("cars")){
            Map<String,Object> garageGrid=MultiplayerGarage.slots(frame,List.of(),"",viewportAspect());
            if(!((List<?>)garageGrid.get("cards")).isEmpty())panel=null;
        }
        if(addInterruptingPopup(out,panel,words)){
            matching.uncertain(clock);
            SessionState.stats.matching(-1);
            return out;
        }
        boolean restart=false;
        if(scene.equals("unknown"))matching.uncertain(clock);
        else restart=matching.observe(scene.equals("matching"),clock,capture.frameId());
        SessionState.stats.matching(matching.active()?matching.elapsed(clock):-1);
        if(restart){
            recoveryReason=GameRecovery.Reason.MATCH_TIMEOUT;
            throw new IllegalStateException("匹配超过 "+MatchmakingWatchdog.TIMEOUT_SECONDS+" 秒");
        }
        out.put("scene",scene);
        if(scene.equals("loginHistory")&&touch!=null){
            int[] recent=touch.recentLogin();if(recent!=null)out.put("recentAccount",Map.of("x",recent[0],"y",recent[1]));
        }
        if(scene.equals("rewardChoice")){
            List<Object> cards=new ArrayList<>();
            List<int[]> detected=DynamicRewards.cards(frame);
            boolean completePrefix=true;
            int previous=0;
            for(int[] card:detected){
                // A clipped, skipped or covered card must not shift subsequent ordinal positions.
                if((previous==0&&card[0]>280)||(previous>0&&(card[0]-previous<145||card[0]-previous>280)))completePrefix=false;
                boolean coveredCard=excluded!=null&&Rect.intersects(excluded,new Rect(card[2],210,card[3]+1,525));
                DynamicRewards.TradePrice price=coveredCard?DynamicRewards.TradePrice.UNKNOWN:DynamicRewards.tradePrice(frame,card,words);
                cards.add(Map.of("x",card[0],"y",card[1],"visible",!coveredCard,"price",price.name()));
                previous=card[0];
            }
            out.put("rewardChoices",cards);
            out.put("rewardOrderKnown",completePrefix);
        }
        boolean resultScene=scene.equals("result")||scene.equals("rewardChoice")||(scene.equals("unknown")&&SessionState.resultOverlay),menuScene=scene.equals("cars")||scene.equals("car")||scene.equals("rewardChoice");
        if(resultScene!=SessionState.resultOverlay||menuScene!=SessionState.overlayMenuCompact){
            prepareOverlay(resultScene,menuScene);
            return multiplayerUi(readSeriesScore,raceExpected,false); // Re-read uncovered fields before input.
        }
        if(scene.equals("result")){
            Bitmap rankSide=Bitmap.createBitmap(frame.copyPixels(),720,1280,Bitmap.Config.ARGB_8888);
            Matrix rankMatrix=new Matrix();rankMatrix.postRotate(270);
            Bitmap rankFrame=Bitmap.createBitmap(rankSide,0,0,720,1280,rankMatrix,false);
            try {
                int rank=RankOcr.read(recognizer,rankFrame,result,true);
                if(rank==0)rank=RankOcr.read(recognizer,rankFrame,result,false);
                out.put("finalRank",rank);out.putAll(rewardRead(rankFrame,result));
            }finally{rankFrame.recycle();rankSide.recycle();}
        }
        if(readSeriesScore){
            String normalized=MapMatcher.normalize(text).replace('賽','赛').replace('總','总').replace('計','计');
            if(scene.equals("event")&&normalized.contains("世界系列赛")){
                if(wideViewport())addSeriesScore(out,excluded,315,525,460,578);
                else addSeriesScore(out,excluded,303,550,520,595);
            }else if(Boolean.TRUE.equals(out.get("rewardPage"))&&normalized.contains("系列赛分数")&&normalized.contains("总计")){
                addSeriesScore(out,excluded,543,505,690,550);
            }
        }
        if(!master&&scene.equals("series")){
            String league=MultiplayerGarage.worldEntryLeague(words);
            if(!league.isEmpty())out.put("entryLeague",league);
        }
        if(scene.equals("event")&&MapMatcher.normalize(text).replace('賽','赛')
                .contains(master?"大师系列赛":"世界系列赛")){
            String league=eventLeague(words,master);
            if(!league.isEmpty())out.put(master?"masterLeague":"worldLeague",league);
        }
        if(scene.equals("unknown")&&!text.isEmpty()&&!text.equals(lastUnknownText)&&clock-lastUnknownLog>=5000){
            lastUnknownLog=clock;lastUnknownText=text;SessionState.log("待识别界面文字："+text);
        }
        int[] cross=MultiplayerUi.limitedSalePromo(text)?PopupCloseDetector.findLimitedSale(frame):
                (scene.equals("serverError")||scene.equals("noOpponents"))?PopupCloseDetector.findDialog(frame,ultrawideViewport()):PopupCloseDetector.find(frame);
        if(cross!=null)out.put("closeX",Map.of("x",cross[0],"y",cross[1]));
        if(ultrawideViewport())addHomeButton(out);
        if(!scene.equals(lastUiScene)) {
            lastUiScene=scene;
            snapshot(new File(root,"multiplayer-"+scene+".png").getAbsolutePath(),0,0,719,1279);
        }
        String header=mapTitle.toString();
        // A dedicated crop keeps small Chinese map labels from being merged with the roster.
        boolean loadingCandidate=scene.equals("unknown")&&raceExpected;
        String focusedTitle=loadingCandidate?text(529,80,684,550,2):"";
        if(scene.equals("cars")){
            boolean wide=ultrawideViewport();
            double aspect=viewportAspect();
            Map<String,Object> garage=MultiplayerGarage.slots(frame,words,"",aspect,master);
            int left=(int)garage.get("left");
            if(!master&&garage.get("rank").equals("")&&left>=(wide?215:202)&&left<=(wide?233:220)){
                String rank=wide?text(262,126,305,185,4):text(250,90,335,200,3);
                garage=MultiplayerGarage.slots(frame,words,rank,aspect);
            }
            out.put("garage",garage);
            out.put("leagueTabs",master?Map.of():MultiplayerGarage.leagueTabs(frame));
            if(((List<?>)garage.get("cards")).isEmpty())
                snapshot(new File(root,"multiplayer-garage-unconfirmed.png").getAbsolutePath(),0,0,719,1279);
        }
        if(scene.equals("car")){
            out.put("fuel",FuelAvailability.detail(words));
            out.put("starUpPending",!trial&&VehicleUnlockGuard.pendingDetail(text));
        }
        if(loadingCandidate){
            out.put("matchingRoster",ColorPattern.matches(frame,"600|125|FFFFFF-101010,602|125|FFFFFF-101010",1));
            out.put("mapText",MapMatcher.loadingTitle(header,focusedTitle));
            MapMatcher.Match match=store.matcher.matchLoadingTitle(header,focusedTitle);
            if(match.unique()){
                String key=match.candidates.get(0).internal;Route route=store.routes.get(key);
                out.put("route",key);out.put("routeAvailable",route!=null&&route.valid());
            }
        }
        return out;
    }
    private void addSeriesScore(Map<String,Object> out,Rect overlay,int left,int top,int right,int bottom)throws Exception {
        if(overlay!=null&&Rect.intersects(overlay,new Rect(left,top,right+1,bottom+1)))return;
        long score=RaceRewards.amount(text(719-bottom,left,719-top,right,2),false);
        if(score>=0)out.put("seriesScore",score);
    }
    private String eventLeague(List<?> words,boolean master)throws Exception {
        boolean wide=wideViewport();
        if(master&&!MultiplayerGarage.hasEventRating(words))return "";
        String color=MultiplayerGarage.eventLeagueIcon(frame,words,wide);
        int[] badge=MultiplayerGarage.eventLeagueBadgeBounds(words,wide);
        String label=MultiplayerGarage.leagueBadge(text(719-badge[3],badge[0],719-badge[1],badge[2],5));
        if(color.isEmpty())return label;
        return label.isEmpty()||label.equals(color)?color:"";
    }
    private boolean addLeagueDemotion(Map<String,Object> out,List<?> words)throws Exception {
        if(!MultiplayerUi.leagueDemotionLayout(frame))return false;
        Map<String,Integer> button=MultiplayerUi.leagueDemotionButton(frame,words);
        if(button.isEmpty()){
            List<Object> focused=new ArrayList<>(words);
            focused.add(Map.of("text",text(510,535,564,728,3),"x",633,"y",182));
            focused.add(Map.of("text",text(171,568,211,700,3),"x",633,"y",528));
            button=MultiplayerUi.leagueDemotionButton(frame,focused);
        }
        if(button.isEmpty())return false;
        out.put("demotionConfirm",button);
        return true;
    }
    private Map<String,Object> recoveryUi() throws Exception {
        Map<String,Object> out=new LinkedHashMap<>(dailyEventUi(true));
        // All detectors below use the frame that produced this OCR, including early scene returns.
        out.put("frameId",capture.frameId());
        String text=(String)out.getOrDefault("text","");
        List<?> words=(List<?>)out.getOrDefault("words",List.of());
        String scene=(String)out.get("scene");
        String menu=MultiplayerUi.classify(text);
        if (menu.equals("exitGame") || menu.equals("loginHistory") || menu.equals("rewardChoice"))
            out.put("scene",scene=menu);
        // Full-screen confirmations and game subpages have their own actions. Their
        // ordinary buttons can share the legacy overlay's colors.
        boolean canShowOverlay=scene.equals("unknown") || scene.equals("home") || scene.equals("interruptingPopup");
        String overlay=canShowOverlay ? MainMenuUi.returnOverlay(frame) : "";
        if (!overlay.isEmpty()) out.put("returnOverlay",overlay);
        LegacyPopup legacy=LegacyPopup.match(frame);
        if (legacy==LegacyPopup.ACCOUNT) {
            accountDisplaced=true;
            throw new IllegalStateException(AccountLoginDialog.STOP_REASON);
        }
        if (legacy!=null && !legacy.permitsInput(text)) legacy=null;
        if (legacy!=null) out.put("legacyPopup",legacy.name());
        boolean panelVisible=popupPanel()!=null;
        if (panelVisible) out.put("panelVisible",true);
        boolean obstructed=legacy!=null || !overlay.isEmpty() || out.containsKey("closeX")
                || out.containsKey("homeButton") || panelVisible
                || !(menu.equals("unknown") || menu.equals("home") || menu.equals("series"));
        out.put("restartScreen",RecoveryReadiness.restartScreen(frame));
        out.put("homeEvidence",MainMenuUi.homeEvidence(frame,text,words,scene,obstructed));
        return out;
    }

    private Map<String,Object> dailyEventUi()throws Exception { return dailyEventUi(false); }
    private Map<String,Object> dailyEventUi(boolean solo)throws Exception { return dailyEventUi(solo,false); }
    private Map<String,Object> dailyEventUi(boolean solo,boolean specialLoading)throws Exception {
        capture();
        if(solo&&CupCarousel.claimSpotlight(frame))
            return Map.of("scene","cupClaimSpotlight","wide",wideViewport(),"frameId",capture.frameId(),"words",List.of());
        ModalPopup.Panel panel=popupPanel();
        if(panel==null&&!ModalPopup.possibleClubMilestone(frame)&&!ModalPopup.possibleBlueprintGift(frame)&&RaceHud.visible(frame,wideViewport()))return Map.of("scene","race","words",List.of());
        Bitmap side=Bitmap.createBitmap(frame.copyPixels(),720,1280,Bitmap.Config.ARGB_8888);Matrix m=new Matrix();m.postRotate(270);
        Bitmap upright=Bitmap.createBitmap(side,0,0,720,1280,m,false);
        Task<Text> task=null;Runnable cleanup=()->{upright.recycle();side.recycle();};
        try {
            task=recognizer.process(InputImage.fromBitmap(upright,0));Text full=Tasks.await(task,12,TimeUnit.SECONDS);
            checkAccountText(full.getText());
            String scene=solo?DailyEventUi.classifySolo(full.getText()):DailyEventUi.classify(full.getText());
            Map<String,Object> out=new LinkedHashMap<>();
            out.put("frameId",capture.frameId());out.put("text",full.getText());List<Object> words=new ArrayList<>();int[] later=null;
            for(Text.TextBlock block:full.getTextBlocks())for(Text.Line line:block.getLines()){
                Rect b=line.getBoundingBox();if(b==null)continue;
                words.add(Map.of("text",line.getText(),"x",b.centerX(),"y",b.centerY()));
                if(VehicleUnlockGuard.laterLabel(line.getText(),b.left,b.top,b.right,b.bottom))later=new int[]{b.left,b.top,b.right,b.bottom};
            }
            boolean trial=VehicleUnlockGuard.trialStart(full.getText(),words);
            if(trial&&scene.equals("lockedCar"))scene="car";
            if(DailyEventUi.seasonPassUnlock(full.getText())){
                int[] close=PopupCloseDetector.findSeasonPassUnlock(frame);
                if(close!=null)return Map.of("scene","unknown","words",words,"text",full.getText(),
                        "closeX",Map.of("x",close[0],"y",close[1]));
            }
            String menu=MainMenuUi.classify(full.getText(),words);
            if(solo&&(menu.equals("exitGame")||scene.equals("unknown")&&!menu.isEmpty()))scene=menu;
            if(solo&&MultiplayerUi.limitedSalePromo(full.getText()))scene="limitedSalePromo";
            if(solo&&addLeagueDemotion(out,words))scene="leagueDemotion";
            if(solo&&(scene.equals("unknown")||scene.equals("car"))&&DailyEventUi.zeroTicketStart(full.getText(),words,frame)){
                scene="car";out.put("zeroTicketStart",true);
            }
            if(solo&&scene.equals("unknown")&&DailyEventUi.rewardOnly(full.getText(),words,frame))scene="dailyRewardOnly";
            if(solo&&scene.equals("unknown")&&CupCarousel.listVisible(frame))scene="cupList";
            boolean settlement=scene.equals("missionRewards")||scene.equals("results")||scene.equals("rewards")||scene.equals("cards")||scene.equals("conversion")||ResultRank.personalReward(full.getText())||(scene.equals("unknown")&&SessionState.resultOverlay);
            if(settlement!=SessionState.resultOverlay){
                prepareOverlay(settlement,false);
                return dailyEventUi(solo,specialLoading);
            }
            if(scene.equals("unknown"))android.util.Log.d("A9DailyOcr",full.getText());
            out.put("scene",scene);
            if(scene.equals("home")){
                Map<String,Integer> explore=MainMenuUi.dailyExplore(full.getText(),frame);
                if(!explore.isEmpty())out.put("dailyExplore",explore);
            }
            if(solo&&SpecialEventList.activeTab(frame)){
                out.put("specialSeasonTab",true);
                Map<String,Integer> button=SpecialEventList.entryButton(frame);
                if(!button.isEmpty())out.put("specialEntryButton",button);
            }
            if(solo&&(scene.equals("unknown")||ultrawideViewport()))addHomeButton(out);
            out.put("words",words);
            if(solo)out.put("restartScreen",RecoveryReadiness.restartScreen(frame));
            if(scene.equals("car")){
                int fuel=FuelAvailability.detail(words);
                if(fuel<0&&!wideViewport()){
                    String counter=text(32,510,79,649,4);
                    fuel=FuelAvailability.focusedDetail(counter);
                    if(fuel<0)android.util.Log.d("A9FuelOcr","Unrecognized detail counter: "+counter);
                }
                out.put("fuel",fuel);
                out.put("starUpPending",!trial&&VehicleUnlockGuard.pendingDetail(full.getText()));
                String detailScore=MultiplayerGarage.soloDetailScore(words);
                if(SessionState.stats.dailyLoot()&&detailScore.isEmpty())
                    detailScore=MultiplayerGarage.soloScore(text(557,830,625,1040,3));
                out.put("soloScore",detailScore);
                out.put("soloName",MultiplayerGarage.soloDetailName(words));
            }
            if(scene.equals("unknown")||scene.equals("limitedSalePromo")){
                int[] close=scene.equals("limitedSalePromo")?PopupCloseDetector.findLimitedSale(frame):PopupCloseDetector.find(frame);
                if(close!=null)out.put("closeX",Map.of("x",close[0],"y",close[1]));
            }
            if(touch!=null){
                touch.unlockGuard.observe(VehicleUnlockGuard.prompt(full.getText()),later,!scene.equals("unknown")&&!scene.equals("vehicleUnlock"));
                if(touch.unlockGuard.active()){
                    out.put("scene","vehicleUnlock");int[] center=touch.unlockGuard.laterCenter();
                    if(center!=null)out.put("unlockLater",Map.of("x",center[0],"y",center[1]));return out;
                }
            }
            if(addInterruptingPopup(out,panel,words))return out;
            if(scene.equals("results")||scene.equals("rewards"))recordLegacyResult();
            if(solo&&(scene.equals("missionRewards")||scene.equals("specialRanking")))out.put("stageLabel",specialStageLabel());
            if(specialLoading&&(scene.equals("unknown")||scene.equals("loading"))){
                String header=text(445,0,675,430,3);
                if(header.replaceAll("\\s","").matches("(?s).*阶段[0-9]+.*")){
                    MapMatcher.Match match=store.matcher.matchLoadingTitle(header,header);
                    if(match.unique()){
                        String key=match.candidates.get(0).internal;
                        Route route=store.routes.get(key);
                        if(route!=null&&route.valid())out.put("specialRoute",key);
                    }
                }
            }
            if(scene.equals("loading")){
                // Daily loot places the track BELOW the event name. Some titles (东方巴黎)
                // wrap onto two lines; include both instead of clipping the lower line.
                String title=text(350,405,439,850,3);out.put("mapText",title);
                android.util.Log.d("A9DailyMap",title);
                MapMatcher.Match match=store.matcher.match(title);
                if(match.unique())out.put("route",match.candidates.get(0).internal);
            }
            if(scene.equals("rewards")){
                Map<String,Object> exit=RewardOcr.exitButton(full,frame);
                if(exit.isEmpty()&&(!legacyPrepared||!legacyRaceSeen))
                    exit=RewardOcr.focusedExitButton(recognizer,upright,frame);
                out.put("rewardPage",true);out.put("rewardCanContinue",!exit.isEmpty());out.put("rewardExitButton",exit);
            }
            if(scene.equals("rewards")&&legacyPrepared&&legacyRaceSeen){
                out.putAll(rewardRead(upright,full));
                // Solo rewards can offer an ad multiplier; only the lower exit is safe to advance.
                for(Object word:words){Map<?,?> w=(Map<?,?>)word;
                    if(DailyEventUi.rewardExit((String)w.get("text"),(Integer)w.get("x"),(Integer)w.get("y"))){
                        int x=(Integer)w.get("x"),y=(Integer)w.get("y");
                        if(RaceRewards.enabledExit(frame,x,y))out.put("rewardCanContinue",true);
                    }
                }
            }
            if(!("daily-"+scene).equals(lastUiScene)){
                lastUiScene="daily-"+scene;SessionState.log((solo?"单人赛事":"每日战利品")+"界面："+scene);
                snapshot(new File(root,"daily-"+scene+".png").getAbsolutePath(),0,0,719,1279);
            }
            return out;
        }finally{if(task!=null&&!task.isComplete())task.addOnCompleteListener(Runnable::run,done->cleanup.run());else cleanup.run();}
    }
    private String specialStageLabel()throws Exception {
        // This badge is vertical in landscape. The rotated capture already has its text upright.
        return redLabel(285,12,260,230,0);
    }
    private String redLabel(int left,int top,int width,int height,int rotation)throws Exception {
        int[] pixels=new int[width*height];
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            int color=frame.pixel(left+x,top+y);
            int red=(color>>16)&255,green=(color>>8)&255,blue=color&255;
            pixels[y*width+x]=red>145&&red>green*1.35&&red>blue*1.1?0xff000000:0xffffffff;
        }
        Bitmap badge=Bitmap.createBitmap(pixels,width,height,Bitmap.Config.ARGB_8888);
        Bitmap zoom=Bitmap.createScaledBitmap(badge,width*3,height*3,true);
        badge.recycle();
        try{
            Task<Text> read=recognizer.process(InputImage.fromBitmap(zoom,rotation));
            read.addOnCompleteListener(Runnable::run,done->zoom.recycle());
            String label=Tasks.await(read,12,TimeUnit.SECONDS).getText();
            android.util.Log.d("A9SpecialStage",label);
            return label;
        }catch(RuntimeException e){if(!zoom.isRecycled())zoom.recycle();throw e;}
    }
    private Map<String,Object> rewardRead(Bitmap upright,Text full)throws Exception{
        if(!RaceRewards.page(full.getText()))return Map.of("rewardPage",false);
        Map<String,Object> exitButton=RewardOcr.exitButton(full,frame);
        if(exitButton.isEmpty())exitButton=RewardOcr.focusedExitButton(recognizer,upright,frame);
        boolean canContinue=!exitButton.isEmpty();
        String exit=(String)exitButton.getOrDefault("label","");
        return Map.of("rewardPage",true,"rewardCanContinue",canContinue,"rewardExit",exit,"rewardExitButton",exitButton);
    }
    // After an observed race, the results screen settles its quota even if returning to the event fails.
    private void recordLegacyResult()throws Exception{
        if(!legacyPrepared||!legacyRaceSeen||SessionState.stats.awaitingRewards())return;
        int before=SessionState.stats.finished();
        checkpoint.recordResult(SessionState.stats,0);
        if(SessionState.stats.finished()>before){
            if(SessionState.stats.rankUnconfirmed())notifyRankSkipped("结算中途被打断，本局不计入统计");
            SessionState.activity("正在结算本局比赛");persist(false);
        }
    }
    private void notifyRankSkipped(String message){
        SessionState.log(message);
        if(touch!=null)touch.showTaskNotice(message,2500);
    }
    private Map<String,Object> legacyRewardSample()throws Exception{
        if(!legacyPrepared||!legacyRaceSeen)return Map.of("rewardPage",false);
        capture();Bitmap side=Bitmap.createBitmap(frame.copyPixels(),720,1280,Bitmap.Config.ARGB_8888);Matrix m=new Matrix();m.postRotate(270);Bitmap upright=Bitmap.createBitmap(side,0,0,720,1280,m,false);
        Task<Text> task=null;Runnable cleanup=()->{upright.recycle();side.recycle();};
        try{
            task=recognizer.process(InputImage.fromBitmap(upright,0));Text full=Tasks.await(task,12,TimeUnit.SECONDS);return rewardRead(upright,full);
        }finally{if(task!=null&&!task.isComplete())task.addOnCompleteListener(Runnable::run,done->cleanup.run());else cleanup.run();}
    }
    public String text(int x1,int y1,int x2,int y2) throws Exception {
        return text(x1,y1,x2,y2,1);
    }
    private String text(int x1,int y1,int x2,int y2,int scale) throws Exception {
        Text result=readText(x1,y1,x2,y2,scale);
        return result==null?"":result.getText().replace("\n","");
    }
    /**
     * Recognized word boxes inside an automation-frame region, mapped back to frame coordinates.
     * Screens are identified by their wording instead of their palette, so restyling a menu keeps
     * the same anchor.
     */
    public List<Object> textWords(int x1,int y1,int x2,int y2) throws Exception {
        List<Object> words=new ArrayList<>();
        Text read=readText(x1,y1,x2,y2,1);
        if(read==null)return words;
        int x=Math.max(0,x1),y=Math.max(0,y1),w=Math.min(720,x2+1)-x,h=Math.min(1280,y2+1)-y;
        if(w<=0||h<=0)return words;
        for(Text.TextBlock block:read.getTextBlocks())for(Text.Line line:block.getLines()){
            Rect b=line.getBoundingBox();if(b==null)continue;
            int cx=x+w-1-b.centerY(),cy=y+b.centerX();
            if(cx<0||cy<0||cx>=720||cy>=1280)continue;
            words.add(new LinkedHashMap<>(Map.of("text",line.getText(),"x",cx,"y",cy)));
        }
        return words;
    }
    private Text readText(int x1,int y1,int x2,int y2,int scale) throws Exception {
        PixelFrame f=frame(); int x=Math.max(0,x1),y=Math.max(0,y1),w=Math.min(720,x2+1)-x,h=Math.min(1280,y2+1)-y;
        if(w<=0||h<=0) return null;
        Bitmap all=Bitmap.createBitmap(f.copyPixels(),720,1280,Bitmap.Config.ARGB_8888);
        Bitmap crop=Bitmap.createBitmap(all,x,y,w,h);Matrix m=new Matrix();m.postRotate(270);
        Bitmap rotated=Bitmap.createBitmap(crop,0,0,w,h,m,true);
        Bitmap upright=scale==1?rotated:Bitmap.createScaledBitmap(rotated,rotated.getWidth()*scale,rotated.getHeight()*scale,true);
        Runnable cleanup=()->{if(upright!=rotated)upright.recycle();if(rotated!=crop&&rotated!=all)rotated.recycle();if(crop!=all)crop.recycle();all.recycle();};
        Task<Text> task;
        try {task=recognizer.process(InputImage.fromBitmap(upright,0));}catch(Exception e){cleanup.run();throw e;}
        task.addOnCompleteListener(Runnable::run,done->cleanup.run());
        return Tasks.await(task,12,TimeUnit.SECONDS);
    }
    @Override public int call(String name,Lua l) {
        try {
            checkAccountLogin();
            switch(name) {
                case "_cupClaimState":{
                    PixelFrame current=frame();
                    if(CupCarousel.claimSpotlight(current))return result(l,"spotlight");
                    return result(l,!l.isNoneOrNil(1)&&CupCarousel.claimOnlyAt(current,n(l,1))?"claimOnly":"");
                }
                case "_cupScrollDelta":return result(l,cupScroll.observe(frame(),n(l,1)));
                case "_cupCards":{
                    Map<String,Object> popup=popupOverPixelMenu();
                    return popup.isEmpty()?result(l,CupCarousel.cards(frame(),wideViewport())):result(l,popup);
                }
                case "_legacyPopupProbe":{
                    CaptureSession.PopupSample sample=legacyPopup(l.toBoolean(1));
                    return sample==null?result(l,(Object)null):result(l,Map.of("rule",sample.rule.name(),"frameId",sample.frameId));
                }
                case "_legacyPopupDismiss":return result(l,dismissLegacyPopup(s(l,1),l.toBoolean(2)));
                case "_popupUi":return result(l,popupOverPixelMenu());
                case "_specialEventList":{
                    capture();
                    Map<String,Object> popup=popupOverPixelMenu();
                    if(!popup.isEmpty())return result(l,popup);
                    Map<String,Object> list=new LinkedHashMap<>(SpecialEventList.read(frame,wideViewport()));
                    list.put("frameId",capture.frameId());
                    return result(l,list);
                }
                case "_recoveryUi":return result(l,recoveryUi());
                case "_dailyEventUi":return result(l,dailyEventUi());
                case "_soloEventUi":{
                    Map<String,Object> ui=dailyEventUi(true,l.toBoolean(1));
                    if(!l.toBoolean(2)||!"unknown".equals(ui.get("scene")))return result(l,ui);
                    Map<String,Object> rail=SpecialEventList.stageRail(frame);
                    if(rail.isEmpty())return result(l,ui);
                    Map<String,Object> stage=new LinkedHashMap<>(ui);
                    stage.put("stageRail",rail);
                    if(Boolean.TRUE.equals(rail.get("clear"))){
                        int x=(int)rail.get("x"),y=(int)rail.get("y");
                        List<?> words=(List<?>)ui.get("words");
                        boolean badge=words.stream().map(value->(Map<?,?>)value).anyMatch(word->{
                            int wx=(int)word.get("x"),wy=(int)word.get("y");
                            return wx>=x+32&&wx<=x+114&&wy>=185&&wy<=300&&
                                    MapMatcher.normalize(word.get("text").toString()).replace("|","").equals("阶段1");
                        });
                        if(!badge){
                            // Crop inside the badge so the red card artwork stays outside the mask.
                            int badgeBottom=y-7;
                            stage.put("stageFirstLabel",redLabel(719-badgeBottom,x+39,26,60,270));
                        }
                    }
                    return result(l,stage);
                }
                case "_soloGarageScore":{
                    int left=n(l,1),top=n(l,2);
                    return result(l,MultiplayerGarage.soloScore(text(719-top-77,left+11,719-top-29,left+171,3)));
                }
                case "_soloDetailScore":return result(l,MultiplayerGarage.soloScore(text(565,880,600,1038,4)));
                case "_soloGarageUi":{
                    if(!l.toBoolean(1)){
                        Map<String,Object> ui=dailyEventUi(true);
                        if(l.toBoolean(2)){
                            ui=new LinkedHashMap<>(ui);
                            ui.putIfAbsent("frameId",capture.frameId());
                            boolean obscured=popupPanel()!=null || ui.containsKey("closeX") ||
                                    Boolean.TRUE.equals(GarageFilter.read(frame,viewportAspect()).get("present"));
                            if(obscured && "cars".equals(ui.get("scene")))ui.put("scene","unknown");
                            if(!obscured && "unknown".equals(ui.get("scene")) && RaceHud.backWedge(frame)
                                    && GarageOcr.soloHeading(recognizer,frame))ui.put("focusedGarage",true);
                        }
                        boolean obscured=popupPanel()!=null||ui.containsKey("closeX")||
                                Boolean.TRUE.equals(GarageFilter.read(frame,viewportAspect()).get("present"));
                        List<?> words=(List<?>)ui.getOrDefault("words",List.of());
                        boolean garageScene="cars".equals(ui.get("scene"))||Boolean.TRUE.equals(ui.get("focusedGarage"));
                        boolean empty=!obscured&&(garageScene||"unknown".equals(ui.get("scene")))&&
                                GarageOcr.soloEmpty(recognizer,frame,words,viewportAspect());
                        if(garageScene||empty){
                            ui=new LinkedHashMap<>(ui);
                            ui.put("garage",MultiplayerGarage.soloSlots(frame,words,viewportAspect()));
                            ui.put("garageEmpty",empty);
                            if(empty)ui.put("focusedGarage",true);
                        }
                        return result(l,ui);
                    }
                    capture();
                    Map<String,Object> popup=popupOverPixelMenu();
                    if(!popup.isEmpty())return result(l,popup);
                    return result(l,Map.of("scene","cars","frameId",capture.frameId(),"gridOnly",true,
                            "garage",MultiplayerGarage.soloSlots(frame,List.of(),viewportAspect())));
                }
                case "_garageFilterPrepare":
                    prepareOverlay(false,true);return 0;
                case "_garageFilterState":{
                    Map<String,Object> popup=popupOverPixelMenu();
                    return popup.isEmpty()?result(l,GarageFilter.read(frame(),viewportAspect())):result(l,popup);
                }
                case "keepCapture": capture();return result(l,ModalPopup.possibleClubMilestone(frame)||popupPanel()!=null);
                case "releaseCapture": return 0;
                case "tap":if(!freshAfterPopup())return 0; touch.tap(8,n(l,1),n(l,2));return 0;
                case "touchDown":if(!freshAfterPopup())return 0; touch.down(n(l,1),n(l,2),n(l,3));return 0;
                case "touchMove":if(!freshAfterPopup())return 0; touch.move(n(l,1),n(l,2),n(l,3));return 0;
                case "touchUp": touch.up(n(l,1));return 0;
                case "swipe":
                    if(!freshAfterPopup())return 0;
                    if(!l.isNoneOrNil(6))touch.dragColumn(n(l,1),n(l,2),n(l,3),n(l,4),n(l,5),n(l,6));
                    else touch.swipe(n(l,1),n(l,2),n(l,3),n(l,4),n(l,5));
                    return 0;
                case "cmpColorEx":{
                    String pattern=s(l,1);
                    boolean raceCheck=RaceHud.PATTERN.equals(pattern)||"680|50|FFFFFF,660|70|FFFFFF".equals(pattern);
                    return result(l,(raceCheck?RaceHud.matches(frame(),pattern,ratio(l,2),wideViewport()):
                            ColorPattern.matches(frame(),pattern,ratio(l,2)))?1:0);
                }
                case "cmpColor":return result(l,frame().inBounds(n(l,1),n(l,2))&&ColorPattern.color(frame().pixel(n(l,1),n(l,2)),s(l,3),ratio(l,4))?1:0);
                case "getPixelColor":return result(l,String.format(Locale.ROOT,"%06X",ColorPattern.swapRedBlue(frame().pixel(n(l,1),n(l,2)))&0xffffff));
                case "colorToRGB":{
                    int c=ColorPattern.swapRedBlue((int)Long.parseLong(s(l,1).replace("0x","").replace("#",""),16));return result(l,(c>>16)&255,(c>>8)&255,c&255);
                }
                case "findColor":return find(l,false);
                case "findMultiColor":return find(l,true);
                case "findPic":return picture(l);
                case "snapShot": snapshot(s(l,1),l.getTop()>1?n(l,2):0,l.getTop()>1?n(l,3):0,l.getTop()>1?n(l,4):719,l.getTop()>1?n(l,5):1279);return result(l,true);
                case "rotateImage": rotate(s(l,1),n(l,2));return result(l,true);
                case "getDisplaySize":return result(l,720,1280);
                case "getDisplayRotate":return result(l,capture.width>capture.height?1:0);
                case "getWorkPath":case "getSdPath":return result(l,workPath());
                case "getPackageName":return result(l,context.getPackageName());
                case "getInstalledApk":return result(l,List.of(store.config.getString("gamePkg")));
                case "frontAppName":return result(l,TouchService.foreground);
                case "runApp":launch(s(l,1));return 0;
                case "stopApp":throw new IllegalStateException("请手动关闭或重启游戏");
                case "keyPress":if(!freshAfterPopup())return 0;touch.key(s(l,1));return 0;
                case "setScreenScale":case "setControlBarPos":return 0;
                case "rnd": {int a=n(l,1),b=n(l,2);return result(l,a+new Random().nextInt(Math.max(1,b-a+1)));}
                case "toast":case "print": {
                    List<String> parts=new ArrayList<>();for(int i=1;i<=l.getTop();i++)parts.add(l.toString(i));SessionState.log(String.join(" ",parts));return 0;
                }
                case "_racePanel":
                    if(l.toBoolean(1))SessionState.overlayMode(false,false);
                    SessionState.racePanel(l.toBoolean(1));return 0;
                case "_hud":SessionState.hud(s(l,1));return 0;
                case "_taskNotice":{
                    String message=s(l,1);
                    SessionState.log(message);
                    if(touch!=null)touch.showTaskNotice(message,n(l,2));
                    return 0;
                }
                case "_finished":finished=true;persist(true);return 0;
                case "_jsonEncode":return result(l,toJson(l.toObject(1)).toString());
                case "_jsonDecode":return result(l,LocalStore.javaValue(new JSONTokener(s(l,1)).nextValue()));
                case "_textOcr":return result(l,text(n(l,1),n(l,2),n(l,3),n(l,4)));
                case "_textWords":return result(l,textWords(n(l,1),n(l,2),n(l,3),n(l,4)));
                case "_multiplayerUi":return result(l,multiplayerUi(l.toBoolean(1),l.toBoolean(2),l.toBoolean(3)));
                case "_requestGameRestart":
                    switch(s(l,1)){
                        case "heartbeat": recoveryReason=GameRecovery.Reason.HEARTBEAT_TIMEOUT;break;
                        case "matching": recoveryReason=GameRecovery.Reason.MATCH_TIMEOUT;break;
                        case "returnScreen": recoveryReason=GameRecovery.Reason.RESTART_SCREEN;break;
                        default: throw new IllegalArgumentException("未知游戏重启原因");
                    }
                    throw new IllegalStateException(recoveryReason.detail);
                case "_recoverEventStart":
                    recoveryReason=GameRecovery.Reason.EVENT_START_TIMEOUT;
                    throw new IllegalStateException("点击赛事开始后未确认进入选车页面");
                case "_rewardUi":return result(l,rewardUi(l.toBoolean(1)));
                case "_routeDisplay":return result(l,store.matcher.displayName(s(l,1)));
                case "_sessionCycle":SessionState.stats.cycle(n(l,1));return 0;
                case "_dailyTaskStatus":return result(l,dailySkipReason(n(l,1)));
                case "_usedCarEntries":return result(l,checkpoint.usedCarEntries());
                case "_selectCarEntry":checkpoint.selectCarEntry(n(l,1),l.toBoolean(2));return 0;
                case "_queueResume":{
                    Map<String,Object> saved=new LinkedHashMap<>(checkpoint.resume());
                    saved.put("restored",restoredQueue);
                    restoredQueue=false;
                    return result(l,saved);
                }
                case "_requirePlayerAction":
                    hardStop=s(l,1);throw new IllegalStateException(hardStop);
                case "_queueHomeConfirmed":
                    SessionState.log("已确认游戏主界面。"+checkpoint.failureSummary());
                    checkpoint.homeConfirmed();persist(false);return result(l,checkpoint.resume());
                case "_queueBeginRewards":
                    checkpoint.beginRewards(n(l,1));runningQueueWork=true;SessionState.activity("正在检查本轮奖励");persist(false);return 0;
                case "_queueRewardsDone":runningQueueWork=false;checkpoint.rewardsDone(n(l,1));persist(false);return 0;
                case "_queueEnter":checkpoint.enter(n(l,1),n(l,2),n(l,3));runningQueueWork=true;persist(false);return result(l,checkpoint.completed());
                case "_queueDone":
                    runningQueueWork=false;
                    legacyPrepared=legacyRaceSeen=false;
                    checkpoint.projectDone();persist(false);return 0;
                case "_sessionProject":SessionState.stats.project(s(l,1));SessionState.stats.resumeProject(l.getTop()>1?n(l,2):0,checkpoint.completed());return result(l,checkpoint.completed());
                case "_sessionRaceBegin":{
                    String key=s(l,1);Route r=store.routes.get(key);SessionState.stats.begin(store.matcher.displayName(key),l.toBoolean(2));
                    SessionState.activity("比赛开始，辅助驾驶中");
                    if(l.toBoolean(2))SessionState.stats.routeReason(key.isEmpty()?"地图未识别":r==null||r.raw.isEmpty()?"暂无跑法":"跑法无效");
                    persist(false);return 0;
                }
                case "_sessionLegacyPrepare":
                    SessionState.racePanel(false);legacyPrepared=true;legacyRaceSeen=false;legacyInvalid=false;legacyMap="";SessionState.overlayMode(false,false);return 0;
                case "_sessionLegacyMap":
                    if(legacyPrepared){legacyMap=s(l,1);legacyInvalid=l.toBoolean(2);SessionState.stats.routeMap(legacyMap,legacyInvalid);}return 0;
                case "_sessionRaceResult":{
                    int before=SessionState.stats.finished(),beforeRanked=SessionState.stats.rankedCount();
                    boolean conflict=checkpoint.recordResult(SessionState.stats,l.getTop()>0?n(l,1):0);
                    if(conflict)notifyRankSkipped("名次读数前后不一致，本局不计入统计");
                    else if(SessionState.stats.finished()>before&&SessionState.stats.rankUnconfirmed())
                        notifyRankSkipped("结算中途被打断，本局不计入统计");
                    if(SessionState.stats.finished()>before)SessionState.activity("正在结算本局比赛");
                    if(SessionState.stats.finished()>before)SessionState.log("任务累计完赛 "+SessionState.stats.finished()+" · 世界 "+SessionState.stats.count("多人上")+" / 大师 "+SessionState.stats.count("多人下")+" · 自动重启 "+SessionState.stats.restartCount());
                    if(SessionState.stats.finished()!=before||SessionState.stats.rankedCount()!=beforeRanked)persist(false);
                    return 0;
                }
                case "_sessionLegacyCompleted":
                    if(!freshAfterPopup())return result(l,false);
                    if(!legacyPrepared||!legacyRaceSeen)throw new IllegalStateException("未确认本任务实际比赛，拒绝累计完赛或切换任务");
                    recordLegacyResult();
                    SessionState.stats.returned();
                    legacyPrepared=legacyRaceSeen=false;SessionState.overlayMode(false,false);
                    persist(false);return 0;
                case "_sessionOperation":SessionState.stats.operation(s(l,1));return 0;
                case "_sessionRaceReturned":SessionState.racePanel(false);SessionState.stats.returned();persist(false);return 0;
                case "_fallbackBegin":
                    driverStart=clock;lastProgressTime=clock;lastProgress=-1;lastLoggedProgress=-10;progress.reset();return 0;
                case "_fallbackStep": {
                    int p=progress.accept(store.digits.progress(frame(),store.config.optInt("accuracy",80)/100.0,viewportAspect()));
                    if(p>lastProgress){lastProgress=p;lastProgressTime=clock;}
                    if(clock-lastProgressTime>90_000)throw failure("划动选路进度 90 秒未前进，请检查游戏画面");
                    if(clock-driverStart>1000L*store.config.optInt("raceTimeout",LocalStore.DEFAULT_RACE_TIMEOUT_SECONDS))throw failure("划动选路超过单局时限");
                    if(p>=0&&p>=lastLoggedProgress+10){lastLoggedProgress=p;SessionState.log("划动选路进度 "+p+"%");}
                    SessionState.stats.progress(p,-1,0);return result(l,p);
                }
                case "_digit":return result(l,store.digits.read(frame(),n(l,1),n(l,2),n(l,3),n(l,4),store.config.optInt("accuracy",80)/100.0).digit);
                case "_mapMatch": {
                    MapMatcher.Match m=store.matcher.match(s(l,1));
                    if(!m.unique()) {SessionState.log("地图匹配候选 "+m.candidates.size()+"："+s(l,1));return result(l,null,null);}
                    return result(l,m.candidates.get(0).internal,store.matcher.displayName(m.candidates.get(0).internal));
                }
                case "_validateRoute": {
                    return result(l,Route.parse(s(l,1),s(l,2)).valid());
                }
                case "_driverBegin": {
                    SessionState.overlayMode(false,false);
                    if(driver!=null)driver.stop();
                    nitroDrift=l.toBoolean(3);
                    Route route=Route.parse("当前路线",s(l,1));driverFallback=!route.valid();
                    driver=driverFallback?null:new RouteEngine(route,touch,store.config.optInt("delay",0),l.toBoolean(2),store.config.optInt("driverMode",1)==2,nitroDrift);
                    if(driverFallback)SessionState.log("暂无有效跑法，使用左划兜底");
                    if(!store.config.optString("routeOnly").isEmpty())SessionState.activity("比赛开始，辅助驾驶中");
                    driverStart=clock;lastDigitTime=clock;lastProgressTime=clock;lastProgress=-1;lastLoggedProgress=-10;lastPulse=clock-300;lastDriverResultCheck=clock;progress.reset();return 0;
                }
                case "_driverStep":return step(l);
                case "_driverEnd":if(driver!=null)driver.stop();driverFallback=false;return 0;
                default:throw new UnsupportedOperationException("尚未实现的运行接口："+name);
            }
        } catch(Exception e) {
            if(e instanceof CaptureSession.Unavailable||e instanceof SecurityException)hardStop=e.getMessage();
            if(e instanceof CaptureSession.OrientationPending&&GameRecovery.portrait(context))recoveryReason=GameRecovery.Reason.PORTRAIT;
            if(e instanceof TouchService.ForegroundLost)recoveryReason=GameRecovery.Reason.FOREGROUND_LOST;
            throw new IllegalStateException(name+"："+e.getMessage(),e);
        }
    }
    private int step(Lua l) throws Exception {
        if(driver==null&&!driverFallback)throw new IllegalStateException("跑图尚未初始化");capture();
        if((ModalPopup.possibleClubMilestone(frame)||ModalPopup.possibleBlueprintGift(frame)||popupPanel()!=null)
                &&!popupOverPixelMenu().isEmpty())return interruptDriver(l);
        boolean end=ColorPattern.color(frame.pixel(68,812),"5400FF-050505",1)||ColorPattern.color(frame.pixel(708,288),"5400FF",1);
        if(end){if(driver!=null)driver.stop();return result(l,true,-1,"比赛结束");}
        int rawProgress=store.digits.progress(frame,store.config.optInt("accuracy",80)/100.0,viewportAspect());
        int p=progress.accept(rawProgress);
        // Readable backtracking after a crash is not an OCR outage. Keep route progress monotonic.
        if(rawProgress>=0)lastDigitTime=clock;
        if(p>lastProgress){lastProgress=p;lastProgressTime=clock;}
        if(rawProgress<0&&clock-lastDigitTime>=2500&&clock-lastDriverResultCheck>=2500){
            lastDriverResultCheck=clock;
            prepareOverlay(true,false);
            Map<String,Object> observed=legacyRaceSeen?dailyEventUi(true):multiplayerUi(false,false,false);
            String scene=(String)observed.get("scene");
            if(scene.equals("interruptingPopup"))return interruptDriver(l);
            boolean settlement=scene.equals("results")||scene.equals("rewards");
            if(legacyRaceSeen?settlement:MultiplayerUi.raceEnded(scene)){
                if(driver!=null)driver.stop();
                SessionState.racePanel(false);
                SessionState.log("比赛已进入赛后页面（"+scene+"），交由赛事流程处理");
                return result(l,true,-1,"比赛结束",legacyRaceSeen&&settlement);
            }
            prepareOverlay(false,false);
        }
        if(!driverFallback&&clock-lastDigitTime>12_000)throw failure("12 秒未能确认进度，请检查游戏画面是否被遮挡");
        if(clock-lastProgressTime>90_000)throw failure("比赛进度 90 秒未前进，请检查游戏是否卡住或暂停");
        if(clock-driverStart>1000L*store.config.optInt("raceTimeout",LocalStore.DEFAULT_RACE_TIMEOUT_SECONDS))throw failure("单局跑图超时，可在任务偏好中调整单局时间上限");
        if(driverFallback){
            if(p<99&&clock-lastPulse>=700){touch.tap(1,236,1100);touch.swipe(288,840,288,440,50);lastPulse=clock;}
            SessionState.stats.progress(p,-1,0);SessionState.stats.operation("暂无有效跑法，左划兜底");
            return result(l,p>=99,p,"暂无有效跑法，左划兜底"+(p>=0?" · "+p+"%":""));
        }
        boolean icon=false;
        for(int y=450;y<=800;y++) if(ColorPattern.color(frame.pixel(600,y),"E7911F-0F191F",1)||ColorPattern.color(frame.pixel(600,y),"2020CB-1F1F1F",1)){icon=true;break;}
        if(nitroDrift&&!driver.drifting()&&clock-lastPulse>=300&&ColorPattern.matches(frame,"680|50|FFFFFF-050505,678|840|C3C9EE-191919",1)){
            driver.nitroPulse(clock);lastPulse=clock;
        }
        try{driver.update(p,clock,icon);SessionState.stats.progress(p,driver.nextEvent(),driver.eventCount());SessionState.stats.operation(driver.currentOperation());}
        catch(RuntimeException e){
            if(e instanceof TouchService.ForegroundLost)throw e;
            throw failure("进度 "+p+"%，动作 "+driver.nextEvent()+"/"+driver.eventCount()+"："+e.getMessage());
        }
        if(p>=0&&(p>=lastLoggedProgress+10||driver.stopped())) {
            lastLoggedProgress=p;SessionState.log("跑图进度 "+p+"% · 已执行动作 "+driver.nextEvent()+"/"+driver.eventCount());
        }
        return result(l,driver.stopped(),p,p<0?"等待可靠的进度画面":"智能选路 "+p+"% · "+driver.nextEvent()+"/"+driver.eventCount());
    }
    private int interruptDriver(Lua l) {
        if(driver!=null)driver.clear();
        lastDigitTime=clock;lastProgressTime=clock;
        return result(l,false,-1,"正在处理游戏弹窗",false,true);
    }
    private IllegalStateException failure(String reason) {
        try{snapshot(new File(root,"diagnostic.png").getAbsolutePath(),0,0,719,1279);SessionState.log("已保存停止时画面");}
        catch(IOException e){SessionState.log("停止画面保存失败："+e.getMessage());}
        return new IllegalStateException(reason);
    }
    private int find(Lua l,boolean multi) {
        int x1=Math.max(0,n(l,1)),y1=Math.max(0,n(l,2)),x2=Math.min(719,n(l,3)),y2=Math.min(1279,n(l,4));
        String base=s(l,5);String[] points=multi?s(l,6).split("\\|"):new String[0];
        double similarity=ratio(l,multi?8:7);int direction=n(l,multi?7:6);
        ColorPattern.Test baseTest=new ColorPattern.Test(base,similarity);
        int count=points.length/3;int[] dx=new int[count],dy=new int[count];ColorPattern.Test[] tests=new ColorPattern.Test[count];
        for(int i=0;i<count;i++){dx[i]=Integer.parseInt(points[i*3]);dy[i]=Integer.parseInt(points[i*3+1]);tests[i]=new ColorPattern.Test(points[i*3+2],similarity);}
        PixelFrame f=frame();
        // Native direction 2 scans from the right; direction 0/1 scans in reading order.
        for(int yi=y1;yi<=y2;yi++) for(int xi=x1;xi<=x2;xi++) {
            int x=direction==2?x2-(xi-x1):xi,y=yi;
            if(!baseTest.matches(f.pixel(x,y)))continue;
            boolean yes=true;
            for(int i=0;i<count;i++) {
                int px=x+dx[i],py=y+dy[i];
                if(!f.inBounds(px,py)||!tests[i].matches(f.pixel(px,py))){yes=false;break;}
            }
            if(yes)return result(l,x,y);
        }
        return result(l,-1,-1);
    }
    private File file(String name) throws IOException {
        File f=new File(name);if(!f.isAbsolute())f=new File(root,name);
        String path=f.getCanonicalPath();if(!path.startsWith(root.getCanonicalPath()+File.separator))throw new IOException("文件不在本地工作目录");
        File parent=f.getParentFile();if(parent!=null)parent.mkdirs();return f;
    }
    private void snapshot(String name,int x1,int y1,int x2,int y2) throws IOException {
        PixelFrame f=frame();Bitmap b=Bitmap.createBitmap(f.copyPixels(),720,1280,Bitmap.Config.ARGB_8888);
        int x=Math.max(0,x1),y=Math.max(0,y1),w=Math.min(720,x2+1)-x,h=Math.min(1280,y2+1)-y;
        Bitmap crop=Bitmap.createBitmap(b,x,y,w,h);
        try(FileOutputStream out=new FileOutputStream(file(name))){crop.compress(Bitmap.CompressFormat.PNG,100,out);}
        finally {if(crop!=b)crop.recycle();b.recycle();}
    }
    private void rotate(String name,int angle) throws IOException {
        File f=file(name);Bitmap b=BitmapFactory.decodeFile(f.getAbsolutePath());if(b==null)throw new IOException("图片无法打开");
        Matrix m=new Matrix();m.postRotate(angle);Bitmap rotated=Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight(),m,false);
        try(FileOutputStream out=new FileOutputStream(f)){rotated.compress(Bitmap.CompressFormat.PNG,100,out);}finally{if(rotated!=b)rotated.recycle();b.recycle();}
    }
    private int picture(Lua l) throws IOException {
        Bitmap b=BitmapFactory.decodeFile(file(s(l,5)).getAbsolutePath());if(b==null)return result(l,-1,-1);
        try {
            int x1=Math.max(0,n(l,1)),y1=Math.max(0,n(l,2)),x2=Math.min(719,n(l,3)),y2=Math.min(1279,n(l,4));
            double threshold=ratio(l,8);
            int w=b.getWidth(),h=b.getHeight();PixelFrame f=frame();
            for(int y=y1;y+h-1<=y2;y++)for(int x=x1;x+w-1<=x2;x++) {
                int yes=0,total=0;
                for(int py=0;py<h;py+=2)for(int px=0;px<w;px+=2) {
                    String desired=String.format(Locale.ROOT,"%06X-",ColorPattern.swapRedBlue(b.getPixel(px,py))&0xffffff)+s(l,6);
                    if(ColorPattern.color(f.pixel(x+px,y+py),desired,1))yes++;total++;
                }
                if(total>0&&yes/(double)total>=threshold)return result(l,x,y);
            }
            return result(l,-1,-1);
        }finally{b.recycle();}
    }
    private Object toJson(Object o) throws JSONException {
        if(o==null)return JSONObject.NULL;
        if(o instanceof Map){JSONObject j=new JSONObject();for(Map.Entry<?,?>e:((Map<?,?>)o).entrySet())j.put(String.valueOf(e.getKey()),toJson(e.getValue()));return j;}
        if(o instanceof Collection){JSONArray a=new JSONArray();for(Object e:(Collection<?>)o)a.put(toJson(e));return a;}return o;
    }
    public void launch(String pkg) {
        Intent i=context.getPackageManager().getLaunchIntentForPackage(pkg);
        if(i==null)throw new IllegalStateException("未找到游戏："+pkg);
        context.startActivity(new Intent().setComponent(i.getComponent()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
    public void diagnostic() throws Exception {
        capture();snapshot(new File(root,"diagnostic.png").getAbsolutePath(),0,0,719,1279);
        String text=text(520,110,680,350);MapMatcher.Match m=store.matcher.match(text);
        DigitOcr.Result a=store.digits.read(frame,634,190,660,208,.8),b=store.digits.read(frame,634,209,662,224,.8);
        SessionState.log("画面："+capture.width+"×"+capture.height+"，归一化 720×1280；地图 OCR："+text+"；匹配候选："+m.candidates.size());
        int percent=store.digits.progress(frame,.8,viewportAspect());
        SessionState.log("进度诊断："+(percent<0?"未知":percent+"%")+"（字形："+a.digit+" / "+b.digit+"）；截图："+new File(root,"diagnostic.png"));
        SessionState.hud="地图："+(text.isEmpty()?"未识别":text)+"\n进度："+(percent<0?"未知":percent+"%");SessionState.changed();
        Bitmap image=Bitmap.createBitmap(frame.copyPixels(),720,1280,Bitmap.Config.ARGB_8888);
        File exported=new File(context.getExternalFilesDir(null),"diagnostic.png");
        try(FileOutputStream out=new FileOutputStream(exported)){image.compress(Bitmap.CompressFormat.PNG,100,out);}finally{image.recycle();}
        SessionState.log("可导出的诊断截图："+exported);
    }
    public void pause(){
        try{if(driver!=null)driver.clear();}catch(TouchService.ForegroundLost ignored){}
        finally{touch.releaseAll();}
    }
    @Override public void close(){try{if(driver!=null)driver.stop();}catch(Exception ignored){}finally{recognizer.close();if(touch!=null)touch.releaseAll();}}
}
