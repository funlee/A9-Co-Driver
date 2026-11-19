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
            "setScreenScale","setControlBarPos","rnd","toast","print","colorToRGB","_jsonEncode","_jsonDecode","_textOcr","_digit","_mapMatch","_recoverEventStart",
            "_validateRoute","_driverBegin","_driverStep","_driverEnd","_racePanel","_hud","_finished","_multiplayerUi","_rewardUi","_fallbackBegin","_fallbackStep","_sessionCycle","_sessionProject","_sessionRaceBegin","_sessionRaceResult","_sessionRaceReturned","_sessionLegacyCompleted","_sessionRaceRewards","_sessionLegacyPrepare","_sessionLegacyMap","_legacyRewardSample","_sessionLegacyRewards","_sessionOperation","_routeDisplay","_cupCards","_dailyEventUi","_soloEventUi","_garageFilterState","_garageFilterPrepare","_dailyTaskStatus","_queueResume","_queueRewardsDone","_queueEnter","_queueDone");
    private final Context context;
    private final LocalStore store;
    private final CaptureSession capture;
    private final TouchService touch;
    private final TextRecognizer recognizer=TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
    private PixelFrame frame;
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
    private final TaskCheckpoint checkpoint=new TaskCheckpoint();
    private boolean restoredQueue;
    private final MatchmakingWatchdog matching=new MatchmakingWatchdog();
    private long lastSavedClock;
    private long nextAccountCheck;
    private boolean accountDisplaced;

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

    private final RaceRewards.Stable rewardStable=new RaceRewards.Stable();
    private boolean legacyPrepared,legacyRaceSeen,legacyRewardDone,legacyInvalid;
    private String legacyMap="";
    private long legacyCoins=-1,legacyFame=-1;
    public GameRecovery.Reason recoveryReason;
    public void recovered() throws InterruptedException{
        driver=null;driverFallback=false;frame=null;progress.reset();matching.reset();lastUiScene="";finished=false;recoveryReason=null;
        SessionState.racePanel(false);SessionState.overlayMode(false,false);SessionState.stats.recovered();rewardStable.reset();legacyPrepared=legacyRaceSeen=legacyRewardDone=false;
    }
    public void restoreQueue(JSONObject saved)throws Exception{
        JSONObject q=saved.getJSONObject("queue");
        int cycle=q.getInt("cycle");
        // Sessions from before round-based rewards resume at their existing task.
        checkpoint.restore(cycle,q.getInt("index"),q.getInt("completed"),q.optInt("rewardCycle",cycle));
        restoredQueue=true;
    }
    public void persist(boolean complete)throws Exception{SessionRecord.write(context,store.config,checkpoint,SessionState.stats,complete);lastSavedClock=clock;}
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
        if(touch!=null)touch.requireGameForeground();
        CaptureSession.Snapshot snapshot=capture.frame(store.config.optInt("rotation",90),
                touch==null?null:touch::foregroundGameBounds);
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
        if(legacyPrepared){
            boolean racing=RaceHud.visible(frame,wideViewport());
            if(racing&&!legacyRaceSeen){legacyRaceSeen=true;SessionState.stats.begin(legacyMap,legacyInvalid);}
            if(legacyRaceSeen)SessionState.racePanel(racing);
        }
    }
    private PixelFrame frame() {if(frame==null)capture();return frame;}
    private double viewportAspect(){return viewport.width/(double)viewport.height;}
    private boolean wideViewport(){return viewportAspect()>=2.0;}
    private boolean ultrawideViewport(){return viewportAspect()>2.07;}
    private void addWideHomeButton(Map<String,Object> out){
        // The ultrawide HUD moves this icon inward. Verify both the roof and doorway.
        if(ultrawideViewport()&&RaceHud.backWedge(frame)&&ColorPattern.matches(frame,
                "698|1206|FFFFFF-555555,683|1193|FFFFFF-555555,683|1217|FFFFFF-555555,666|1194|FFFFFF-555555,666|1216|FFFFFF-555555,666|1206|45331F-353535",1))
            out.put("homeButton",Map.of("x",1206,"y",34));
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
        Map<String,Object> screen=rewardUi();
        String text=(String)screen.get("text");
        if(RecoveryReadiness.incompatibleDevice(text))throw new GameRecovery.IncompatibleStartup();
        if(MultiplayerUi.limitedSalePromo(text))return PopupCloseDetector.findLimitedSale(frame)!=null;
        return RecoveryReadiness.ready(text,(String)screen.get("scene"));
    }
    private Map<String,Object> rewardUi() throws Exception {
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
            if(scene.equals("home"))screen.put("dailyGoalsHighlighted",MainMenuUi.dailyGoalsHighlighted(frame));
            addWideHomeButton(screen);
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
    private Map<String,Object> multiplayerUi(boolean readSeriesScore, boolean raceExpected) throws Exception {
        capture();
        Map<String,Object> out=new LinkedHashMap<>();out.put("frameId",capture.frameId());
        // Menu transitions may look like the wide race HUD before verified vehicle Start.
        if(raceExpected&&(touch==null||!touch.unlockGuard.active())&&RaceHud.visible(frame,wideViewport())) {
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
        String menu=MainMenuUi.classify(text,words);
        String scene=menu.isEmpty()?MultiplayerUi.classify(text):menu;
        if(touch!=null){
            touch.unlockGuard.observe(VehicleUnlockGuard.prompt(text),laterBounds,!scene.equals("unknown")&&!scene.equals("vehicleUnlock"));
            if(touch.unlockGuard.active()){
                matching.reset();SessionState.stats.matching(-1);
                out.put("scene","vehicleUnlock");int[] later=touch.unlockGuard.laterCenter();
                if(later!=null)out.put("unlockLater",Map.of("x",later[0],"y",later[1]));
                return out;
            }
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
            return multiplayerUi(readSeriesScore,raceExpected); // Re-read uncovered fields before input.
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
                addSeriesScore(out,excluded,303,550,520,595);
            }else if(Boolean.TRUE.equals(out.get("rewardPage"))&&normalized.contains("系列赛分数")&&normalized.contains("总计")){
                addSeriesScore(out,excluded,543,505,690,550);
            }
        }
        if(scene.equals("unknown")&&!text.isEmpty()&&!text.equals(lastUnknownText)&&clock-lastUnknownLog>=5000){
            lastUnknownLog=clock;lastUnknownText=text;SessionState.log("待识别界面文字："+text);
        }
        int[] cross=MultiplayerUi.limitedSalePromo(text)?PopupCloseDetector.findLimitedSale(frame):
                (scene.equals("serverError")||scene.equals("noOpponents"))?PopupCloseDetector.findDialog(frame,ultrawideViewport()):PopupCloseDetector.find(frame);
        if(cross!=null)out.put("closeX",Map.of("x",cross[0],"y",cross[1]));
        addWideHomeButton(out);
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
            String rank=wide?text(262,126,305,185,4):text(250,90,335,200,3);out.put("rankText",rank);
            out.put("emptyFuelSlots",wideViewport()?List.of():FuelAvailability.emptySlots(frame));
            Map<String,Object> garage=GarageOcr.read(recognizer,frame,words,rank,viewportAspect());out.put("garage",garage);
            out.put("leagueTabs",MultiplayerGarage.leagueTabs(frame));
            if(!Boolean.TRUE.equals(garage.get("start"))){
                SessionState.log("选车起点尚未稳定："+garage);
                snapshot(new File(root,"multiplayer-garage-unconfirmed.png").getAbsolutePath(),0,0,719,1279);
            }
        }
        if(scene.equals("car")){
            Map<String,Object> detail=MultiplayerGarage.detail(words);
            if(excluded==null||!Rect.intersects(excluded,new Rect(150,75,1061,206))){
                Map<String,Object> focused=GarageOcr.detail(recognizer,frame,ultrawideViewport());
                String performance=focused.get("performance").toString();
                detail=Map.of("name",detail.get("name")+" "+focused.get("name"),
                        "performance",performance.matches("[0-9]{2,}/[0-9]{3,}")?performance:detail.get("performance"));
            }
            out.put("carDetail",detail);out.put("fuel",FuelAvailability.detail(words));
            android.util.Log.d("A9CarDetail",detail.toString());
        }
        if(loadingCandidate){
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
    private Map<String,Object> dailyEventUi()throws Exception { return dailyEventUi(false); }
    private Map<String,Object> dailyEventUi(boolean solo)throws Exception { return dailyEventUi(solo,false); }
    private Map<String,Object> dailyEventUi(boolean solo,boolean specialLoading)throws Exception {
        capture();
        if(RaceHud.visible(frame,wideViewport()))return Map.of("scene","race","words",List.of());
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
            if(DailyEventUi.seasonPassUnlock(full.getText())){
                int[] close=PopupCloseDetector.findSeasonPassUnlock(frame);
                if(close!=null)return Map.of("scene","unknown","words",words,"text",full.getText(),
                        "closeX",Map.of("x",close[0],"y",close[1]));
            }
            String menu=MainMenuUi.classify(full.getText(),words);
            if(solo&&(menu.equals("exitGame")||scene.equals("unknown")&&!menu.isEmpty()))scene=menu;
            if(solo&&MultiplayerUi.limitedSalePromo(full.getText()))scene="limitedSalePromo";
            if(solo&&scene.equals("unknown")&&DailyEventUi.rewardOnly(full.getText(),words,frame))scene="dailyRewardOnly";
            if(solo&&scene.equals("unknown")&&wideViewport()&&RaceHud.backWedge(frame)&&
                    CupCarousel.cards(frame,true).size()>=3)scene="wideCupList";
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
            if(solo)addWideHomeButton(out);
            out.put("words",words);
            if(scene.equals("car"))out.put("fuel",FuelAvailability.detail(words));
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
            if(solo&&scene.equals("missionRewards"))out.put("stageLabel",specialStageLabel());
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
            if(scene.equals("rewards")&&legacyPrepared&&legacyRaceSeen){
                if(!legacyRewardDone)out.putAll(rewardRead(upright,full,true));
                // Solo rewards can offer an ad multiplier; only the lower exit is safe to advance.
                for(Object word:words){Map<?,?> w=(Map<?,?>)word;
                    if(DailyEventUi.rewardExit((String)w.get("text"),(Integer)w.get("x"),(Integer)w.get("y"))){
                        int x=(Integer)w.get("x"),y=(Integer)w.get("y");
                        int color=frame.pixel(719-y+25,x);
                        if(ColorPattern.color(color,"FFFFFF-202020",1)||ColorPattern.color(color,"12FBC3-303030",1))out.put("rewardCanContinue",true);
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
        int left=335,top=25,width=160,height=230;
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
            Task<Text> read=recognizer.process(InputImage.fromBitmap(zoom,0));
            read.addOnCompleteListener(Runnable::run,done->zoom.recycle());
            String label=Tasks.await(read,12,TimeUnit.SECONDS).getText();
            android.util.Log.d("A9SpecialStage",label);
            return label;
        }catch(RuntimeException e){if(!zoom.isRecycled())zoom.recycle();throw e;}
    }
    private Map<String,Object> rewardRead(Bitmap upright,Text full)throws Exception{
        return rewardRead(upright,full,false);
    }
    private Map<String,Object> rewardRead(Bitmap upright,Text full,boolean soloLayout)throws Exception{
        if(!RewardOcr.page(full.getText())){rewardStable.reset();return Map.of("rewardPage",false);}
        RaceRewards value=RewardOcr.read(recognizer,upright,full,soloLayout);
        RaceRewards stable=rewardStable.observe(value,capture.frameId(),SystemClock.elapsedRealtime());
        android.util.Log.d("A9RewardOcr","observed="+value.coins+"/"+value.fame+" stable="+stable.coins+"/"+stable.fame);
        String exit="";boolean canContinue=false;
        for(Text.TextBlock block:full.getTextBlocks())for(Text.Line line:block.getLines()){
            Rect bounds=line.getBoundingBox();if(bounds==null)continue;
            String label=RaceRewards.exitLabel(line.getText(),bounds.centerX(),bounds.centerY());
            if(label.isEmpty())continue;
            int color=frame.pixel(719-bounds.centerY()+25,bounds.centerX());
            if(ColorPattern.color(color,"FFFFFF-202020",1)||ColorPattern.color(color,"12FBC3-303030",1)){
                exit=label;canContinue=true;
            }
        }
        return Map.of("rewardPage",true,"rewardCoins",stable.coins,"rewardFame",stable.fame,"rewardCanContinue",canContinue,"rewardExit",exit,"rewardReady",stable.coins>=0&&stable.fame>=0);
    }
    private Map<String,Object> legacyRewardSample()throws Exception{
        if(!legacyPrepared||!legacyRaceSeen||legacyRewardDone)return Map.of("rewardPage",false);
        if(!SessionState.resultOverlay){prepareOverlay(true,SessionState.overlayMenuCompact);}
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
        PixelFrame f=frame(); int x=Math.max(0,x1),y=Math.max(0,y1),w=Math.min(720,x2+1)-x,h=Math.min(1280,y2+1)-y;
        if(w<=0||h<=0) return "";
        Bitmap all=Bitmap.createBitmap(f.copyPixels(),720,1280,Bitmap.Config.ARGB_8888);
        Bitmap crop=Bitmap.createBitmap(all,x,y,w,h);Matrix m=new Matrix();m.postRotate(270);
        Bitmap rotated=Bitmap.createBitmap(crop,0,0,w,h,m,true);
        Bitmap upright=scale==1?rotated:Bitmap.createScaledBitmap(rotated,rotated.getWidth()*scale,rotated.getHeight()*scale,true);
        Runnable cleanup=()->{if(upright!=rotated)upright.recycle();if(rotated!=crop&&rotated!=all)rotated.recycle();if(crop!=all)crop.recycle();all.recycle();};
        Task<Text> task;
        try {task=recognizer.process(InputImage.fromBitmap(upright,0));}catch(Exception e){cleanup.run();throw e;}
        task.addOnCompleteListener(Runnable::run,done->cleanup.run());
        return Tasks.await(task,12,TimeUnit.SECONDS).getText().replace("\n","");
    }
    @Override public int call(String name,Lua l) {
        try {
            checkAccountLogin();
            switch(name) {
                case "_cupCards":return result(l,CupCarousel.cards(frame(),wideViewport()));
                case "_dailyEventUi":return result(l,dailyEventUi());
                case "_soloEventUi":return result(l,dailyEventUi(true,l.toBoolean(1)));
                case "_garageFilterPrepare":
                    prepareOverlay(false,true);return 0;
                case "_garageFilterState":return result(l,GarageFilter.read(frame(),viewportAspect()));
                case "keepCapture": capture();return 0;
                case "releaseCapture": return 0;
                case "tap": touch.tap(8,n(l,1),n(l,2));return 0;
                case "touchDown": touch.down(n(l,1),n(l,2),n(l,3));return 0;
                case "touchMove": touch.move(n(l,1),n(l,2),n(l,3));return 0;
                case "touchUp": touch.up(n(l,1));return 0;
                case "swipe": touch.swipe(n(l,1),n(l,2),n(l,3),n(l,4),n(l,5));return 0;
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
                case "keyPress":touch.key(s(l,1));return 0;
                case "setScreenScale":case "setControlBarPos":return 0;
                case "rnd": {int a=n(l,1),b=n(l,2);return result(l,a+new Random().nextInt(Math.max(1,b-a+1)));}
                case "toast":case "print": {
                    List<String> parts=new ArrayList<>();for(int i=1;i<=l.getTop();i++)parts.add(l.toString(i));SessionState.log(String.join(" ",parts));return 0;
                }
                case "_racePanel":
                    if(l.toBoolean(1))SessionState.overlayMode(false,false);
                    SessionState.racePanel(l.toBoolean(1));return 0;
                case "_hud":SessionState.hud(s(l,1));return 0;
                case "_finished":finished=true;persist(true);return 0;
                case "_jsonEncode":return result(l,toJson(l.toObject(1)).toString());
                case "_jsonDecode":return result(l,LocalStore.javaValue(new JSONTokener(s(l,1)).nextValue()));
                case "_textOcr":return result(l,text(n(l,1),n(l,2),n(l,3),n(l,4)));
                case "_multiplayerUi":return result(l,multiplayerUi(l.toBoolean(1),l.toBoolean(2)));
                case "_recoverEventStart":
                    recoveryReason=GameRecovery.Reason.EVENT_START_TIMEOUT;
                    throw new IllegalStateException("点击赛事开始后未确认进入选车页面");
                case "_rewardUi":return result(l,rewardUi());
                case "_routeDisplay":return result(l,store.matcher.displayName(s(l,1)));
                case "_sessionCycle":SessionState.stats.cycle(n(l,1));return 0;
                case "_dailyTaskStatus":return result(l,dailySkipReason(n(l,1)));
                case "_queueResume":{
                    Map<String,Object> saved=new LinkedHashMap<>(checkpoint.resume());
                    saved.put("restored",restoredQueue);
                    restoredQueue=false;
                    return result(l,saved);
                }
                case "_queueRewardsDone":checkpoint.rewardsDone(n(l,1));persist(false);return 0;
                case "_queueEnter":checkpoint.enter(n(l,1),n(l,2),n(l,3));persist(false);return result(l,checkpoint.completed());
                case "_queueDone":
                    legacyPrepared=legacyRaceSeen=legacyRewardDone=false;legacyCoins=legacyFame=-1;rewardStable.reset();
                    checkpoint.projectDone();persist(false);return 0;
                case "_sessionProject":SessionState.stats.project(s(l,1));SessionState.stats.resumeProject(l.getTop()>1?n(l,2):0,checkpoint.completed());return result(l,checkpoint.completed());
                case "_sessionRaceBegin":{
                    String key=s(l,1);Route r=store.routes.get(key);SessionState.stats.begin(store.matcher.displayName(key),l.toBoolean(2));
                    if(l.toBoolean(2))SessionState.stats.routeReason(key.isEmpty()?"地图未识别":r==null||r.raw.isEmpty()?"暂无跑法":"跑法无效");
                    rewardStable.reset();persist(false);return 0;
                }
                case "_sessionRaceRewards":
                    if(SessionState.stats.rewards(l.toInteger(1),l.toInteger(2))){SessionState.log("本局收益：金币 "+l.toInteger(1)+" · 名人堂 "+l.toInteger(2)+"（-1 为未识别）");persist(false);}return 0;
                case "_sessionLegacyPrepare":
                    SessionState.racePanel(false);legacyPrepared=true;legacyRaceSeen=legacyRewardDone=legacyInvalid=false;legacyMap="";legacyCoins=legacyFame=-1;rewardStable.reset();SessionState.overlayMode(false,false);return 0;
                case "_sessionLegacyMap":
                    if(legacyPrepared){legacyMap=s(l,1);legacyInvalid=l.toBoolean(2);SessionState.stats.routeMap(legacyMap,legacyInvalid);}return 0;
                case "_legacyRewardSample":return result(l,legacyRewardSample());
                case "_sessionLegacyRewards":
                    if(legacyPrepared&&legacyRaceSeen&&!legacyRewardDone){legacyCoins=l.toInteger(1);legacyFame=l.toInteger(2);legacyRewardDone=true;SessionState.log("本局收益识别：金币 "+(legacyCoins<0?"未识别":legacyCoins)+" / 名人堂 "+(legacyFame<0?"未识别":legacyFame));}return 0;
                case "_sessionRaceResult":{
                    int before=SessionState.stats.finished(),beforeRanked=SessionState.stats.rankedCount();SessionState.stats.result(l.getTop()>0?n(l,1):0);
                    checkpoint.completed(SessionState.stats.finished()-before);
                    if(SessionState.stats.finished()>before)SessionState.log("任务累计完赛 "+SessionState.stats.finished()+" · 世界 "+SessionState.stats.count("多人上")+" / 大师 "+SessionState.stats.count("多人下")+" · 自动重启 "+SessionState.stats.restartCount());
                    if(SessionState.stats.finished()!=before||SessionState.stats.rankedCount()!=beforeRanked)persist(false);
                    return 0;
                }
                case "_sessionLegacyCompleted":
                    if(!legacyPrepared||!legacyRaceSeen)throw new IllegalStateException("未确认本任务实际比赛，拒绝累计完赛或切换任务");
                    SessionState.stats.legacyCompleted(legacyCoins,legacyFame);legacyPrepared=legacyRaceSeen=false;legacyCoins=legacyFame=-1;SessionState.overlayMode(false,false);checkpoint.completed(1);persist(false);return 0;
                case "_sessionOperation":SessionState.stats.operation(s(l,1));return 0;
                case "_sessionRaceReturned":SessionState.racePanel(false);SessionState.stats.returned();persist(false);return 0;
                case "_fallbackBegin":
                    driverStart=clock;lastProgressTime=clock;lastProgress=-1;lastLoggedProgress=-10;progress.reset();return 0;
                case "_fallbackStep": {
                    int p=progress.accept(store.digits.progress(frame(),store.config.optInt("accuracy",80)/100.0,viewportAspect()));
                    if(p>lastProgress){lastProgress=p;lastProgressTime=clock;}
                    if(clock-lastProgressTime>90_000)throw failure("划动选路进度 90 秒未前进，请检查游戏画面");
                    if(clock-driverStart>1000L*store.config.optInt("raceTimeout",900))throw failure("划动选路超过单局时限");
                    if(p>=0&&p>=lastLoggedProgress+10){lastLoggedProgress=p;SessionState.log("划动选路进度 "+p+"%");}
                    SessionState.stats.progress(p,-1,0);return result(l,p);
                }
                case "_digit":return result(l,store.digits.read(frame(),n(l,1),n(l,2),n(l,3),n(l,4),store.config.optInt("accuracy",80)/100.0).digit);
                case "_mapMatch": {
                    MapMatcher.Match m=store.matcher.match(s(l,1));
                    if(!m.unique()) {SessionState.log("地图匹配候选 "+m.candidates.size()+"："+s(l,1));return result(l,null,null);}
                    return result(l,m.candidates.get(0).internal,m.candidates.get(0).display);
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
                    driverStart=clock;lastDigitTime=clock;lastProgressTime=clock;lastProgress=-1;lastLoggedProgress=-10;lastPulse=clock-300;lastDriverResultCheck=clock;progress.reset();return 0;
                }
                case "_driverStep":return step(l);
                case "_driverEnd":if(driver!=null)driver.stop();driverFallback=false;return 0;
                default:throw new UnsupportedOperationException("尚未实现的运行接口："+name);
            }
        } catch(Exception e) {
            if(e instanceof TouchService.ForegroundLost)recoveryReason=GameRecovery.Reason.FOREGROUND_LOST;
            throw new IllegalStateException(name+"："+e.getMessage(),e);
        }
    }
    private int step(Lua l) throws Exception {
        if(driver==null&&!driverFallback)throw new IllegalStateException("跑图尚未初始化");capture();
        boolean end=ColorPattern.color(frame.pixel(68,812),"5400FF-050505",1)||ColorPattern.color(frame.pixel(708,288),"5400FF",1);
        if(end){if(driver!=null)driver.stop();return result(l,true,-1,"比赛结束");}
        int rawProgress=store.digits.progress(frame,store.config.optInt("accuracy",80)/100.0,viewportAspect());
        int p=progress.accept(rawProgress);
        // Readable backtracking after a crash is not an OCR outage. Keep route progress monotonic.
        if(rawProgress>=0)lastDigitTime=clock;
        if(p>lastProgress){lastProgress=p;lastProgressTime=clock;}
        if(legacyRaceSeen&&rawProgress<0&&clock-lastDigitTime>=2500&&clock-lastDriverResultCheck>=2500){
            lastDriverResultCheck=clock;
            String scene=(String)dailyEventUi(true).get("scene");
            if(scene.equals("results")||scene.equals("rewards")){
                if(driver!=null)driver.stop();
                SessionState.racePanel(false);
                SessionState.log("单人比赛已进入结果页，转入结算");
                return result(l,true,-1,"比赛结束",true);
            }
        }
        if(!driverFallback&&clock-lastDigitTime>12_000)throw failure("12 秒未能确认进度，请检查游戏画面是否被遮挡");
        if(clock-lastProgressTime>90_000)throw failure("比赛进度 90 秒未前进，请检查游戏是否卡住或暂停");
        if(clock-driverStart>1000L*store.config.optInt("raceTimeout",900))throw failure("单局跑图超时，可在任务偏好中调整单局时间上限");
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
