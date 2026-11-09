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

/** Android implementations of the small automation API used by recovered state machines. */
public final class AndroidHost implements LuaRuntime.Host,AutoCloseable {
    public static final List<String> FUNCTIONS=List.of("keepCapture","releaseCapture","tap","swipe","touchDown","touchMove","touchUp",
            "cmpColorEx","cmpColor","getPixelColor","findColor","findMultiColor","findPic","snapShot","rotateImage",
            "getDisplaySize","getDisplayRotate","getWorkPath","getSdPath","getPackageName","getInstalledApk","frontAppName","runApp","stopApp","keyPress",
            "setScreenScale","setControlBarPos","rnd","toast","print","colorToRGB","_jsonEncode","_jsonDecode","_textOcr","_digit","_mapMatch",
            "_validateRoute","_driverBegin","_driverStep","_driverEnd","_hud","_finished","_multiplayerUi","_fallbackBegin","_fallbackStep","_sessionCycle","_sessionProject","_sessionRaceBegin","_sessionRaceResult","_sessionRaceReturned","_sessionLegacyCompleted","_sessionRaceRewards","_sessionLegacyPrepare","_sessionLegacyMap","_legacyRewardSample","_sessionLegacyRewards","_sessionOperation","_routeDisplay","_dailyEventUi","_garageFilterState","_garageFilterPrepare","_dailyTaskStatus","_queueResume","_queueEnter","_queueDone","_sessionLimitReached");
    private final Context context;
    private final LocalStore store;
    private final CaptureSession capture;
    private final TouchService touch;
    private final TextRecognizer recognizer=TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
    private PixelFrame frame;
    private RouteEngine driver;
    private final ProgressFilter progress=new ProgressFilter();
    private long driverStart,lastDigitTime,lastProgressTime,lastPulse;
    private int lastProgress,lastLoggedProgress;
    private boolean nitroDrift,driverFallback;
    private String lastUiScene="";
    private long lastEmptyUiSnapshot,lastUnknownLog;
    private String lastUnknownText="";
    private final File root;
    public volatile boolean finished;
    public long clock;
    private final TaskCheckpoint checkpoint=new TaskCheckpoint();
    private final MatchmakingWatchdog matching=new MatchmakingWatchdog();
    private long lastSavedClock;
    private final RaceRewards.Stable rewardStable=new RaceRewards.Stable();
    private boolean legacyPrepared,legacyRaceSeen,legacyRewardDone,legacyInvalid;
    private String legacyMap="";
    private long legacyCoins=-1,legacyFame=-1;
    public GameRecovery.Reason recoveryReason;
    public void recovered() throws InterruptedException{
        driver=null;driverFallback=false;frame=null;progress.reset();matching.reset();lastUiScene="";finished=false;recoveryReason=null;
        SessionState.overlayMode(false,false);SessionState.stats.recovered();rewardStable.reset();legacyPrepared=legacyRaceSeen=legacyRewardDone=false;
    }
    public void restoreQueue(JSONObject saved)throws Exception{
        JSONObject q=saved.getJSONObject("queue");checkpoint.restore(q.getInt("cycle"),q.getInt("index"),q.getInt("completed"));
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
        return task==null?"":store.dailyTaskPolicy().skipReason(task.optString("taskId","legacy-"+index),
                "0".equals(task.getJSONObject("data").optString("每日只执行一次")));
    }
    private void recordDailyCompletion() throws JSONException {
        int index=(Integer)checkpoint.resume().get("index");JSONObject task=dailyTask(index);
        if(task!=null) {
            JSONObject settings=task.getJSONObject("data");
            store.dailyTaskPolicy().completed(task.optString("taskId","legacy-"+index),"0".equals(settings.optString("每日只执行一次")),
                    checkpoint.completed(),settings.optInt("次数",1));
        }
    }
    public String workPath(){return root.getAbsolutePath();}
    private int n(Lua l,int i) {return (int)l.toInteger(i);}
    private double ratio(Lua l,int i) {return l.isNoneOrNil(i)?1:l.toNumber(i);}
    private String s(Lua l,int i) {return l.toString(i);}
    private int result(Lua l,Object... values) {for(Object value:values)LuaRuntime.push(l,value);return values.length;}
    private void capture() {
        if(touch!=null)touch.requireGameForeground();
        Bitmap b=capture.frame(store.config.optInt("rotation",90));
        int[] p=new int[720*1280];b.getPixels(p,0,720,0,0,720,1280);b.recycle();frame=new PixelFrame(720,1280,p);
        if(touch!=null)touch.configure(capture.width,capture.height,store.config.optInt("rotation",90));
        if(legacyPrepared&&!legacyRaceSeen&&ColorPattern.matches(frame,"680|50|FFFFFF-101010,660|70|FFFFFF-101010",1)){
            legacyRaceSeen=true;SessionState.stats.begin(legacyMap,legacyInvalid);
        }
    }
    private PixelFrame frame() {if(frame==null)capture();return frame;}
    private Map<String,Object> multiplayerUi() throws Exception {
        capture();
        Map<String,Object> out=new LinkedHashMap<>();
        if((touch==null||!touch.unlockGuard.active())&&ColorPattern.matches(frame,"680|50|FFFFFF-101010,660|70|FFFFFF-101010",1)) {
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
        List<Object> words=new ArrayList<>();StringBuilder visible=new StringBuilder(),mapTitle=new StringBuilder();
        int[] laterBounds=null;
        Rect overlay=SessionState.overlayBounds;
        Rect excluded=overlay==null?null:new Rect(overlay.left*1280/capture.width,overlay.top*720/capture.height,
                overlay.right*1280/capture.width,overlay.bottom*720/capture.height);
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
        String scene=MultiplayerUi.classify(text);
        boolean isMatching=scene.equals("matching");
        boolean covered=excluded!=null&&Rect.intersects(excluded,new Rect(0,0,205,55));
        MatchmakingWatchdog.Back back=MatchmakingWatchdog.backButton(frame,covered);
        boolean restart=matching.observe(isMatching,back,clock,capture.frameId());
        SessionState.stats.matching(isMatching?matching.elapsed(clock):-1);
        if(isMatching){out.put("matchingSeconds",matching.elapsed(clock)/1000);out.put("backButton",back.name());}
        if(restart){recoveryReason=GameRecovery.Reason.MATCH_TIMEOUT;throw new IllegalStateException("匹配超过 90 秒且连续确认返回按钮消失");}
        if(touch!=null){
            touch.unlockGuard.observe(VehicleUnlockGuard.prompt(text),laterBounds,!scene.equals("unknown")&&!scene.equals("vehicleUnlock"));
            if(touch.unlockGuard.active()){
                out.put("scene","vehicleUnlock");int[] later=touch.unlockGuard.laterCenter();
                if(later!=null)out.put("unlockLater",Map.of("x",later[0],"y",later[1]));
                return out;
            }
        }
        out.put("scene",scene);
        if(scene.equals("loginHistory")&&touch!=null){
            int[] recent=touch.recentLogin();if(recent!=null)out.put("recentAccount",Map.of("x",recent[0],"y",recent[1]));
        }
        if(scene.equals("rewardChoice")){
            List<Object> cards=new ArrayList<>();
            for(int[] card:DynamicRewards.cards(frame)){
                if(excluded!=null&&Rect.intersects(excluded,new Rect(card[2],210,card[3]+1,491)))continue;
                cards.add(Map.of("x",card[0],"y",card[1]));
            }
            out.put("rewardChoices",cards);
        }
        boolean resultScene=scene.equals("result"),menuScene=scene.equals("cars")||scene.equals("car")||scene.equals("rewardChoice");
        if(resultScene!=SessionState.resultOverlay||menuScene!=SessionState.overlayMenuCompact){
            SessionState.overlayMode(resultScene,menuScene);
            capture.awaitFrameAfter(SystemClock.elapsedRealtime()+180);
            return multiplayerUi(); // Re-read uncovered fields before input.
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
        if(scene.equals("unknown")&&!text.isEmpty()&&!text.equals(lastUnknownText)&&clock-lastUnknownLog>=5000){
            lastUnknownLog=clock;lastUnknownText=text;SessionState.log("待识别界面文字："+text);
        }
        int[] cross=(scene.equals("serverError")||scene.equals("noOpponents"))?PopupCloseDetector.findDialog(frame):PopupCloseDetector.find(frame);
        if(cross!=null)out.put("closeX",Map.of("x",cross[0],"y",cross[1]));
        if(!scene.equals(lastUiScene)) {
            lastUiScene=scene;
            snapshot(new File(root,"multiplayer-"+scene+".png").getAbsolutePath(),0,0,719,1279);
        }
        String title=mapTitle.toString();
        // A dedicated crop keeps small Chinese map labels from being merged with the roster.
        if(scene.equals("unknown"))title=text(529,80,684,550,2);
        if(scene.equals("cars")){out.put("rankText",text(250,90,335,200,3));out.put("emptyFuelSlots",FuelAvailability.emptySlots(frame));}
        out.put("mapText",title);
        // The loading title includes the lap count. Cinematic player names do not.
        MapMatcher.Match match=store.matcher.match(title.contains("圈")?title:"");
        if(match.unique()){
            String key=match.candidates.get(0).internal;Route route=store.routes.get(key);
            out.put("route",key);out.put("routeAvailable",route!=null&&route.valid());
        }
        return out;
    }
    private Map<String,Object> dailyEventUi()throws Exception {
        capture();
        if(ColorPattern.matches(frame,"680|50|FFFFFF-101010,660|70|FFFFFF-101010",1))return Map.of("scene","race","words",List.of());
        if(!SessionState.resultOverlay||SessionState.overlayMenuCompact){
            SessionState.overlayMode(true,false);capture.awaitFrameAfter(SystemClock.elapsedRealtime()+180);capture();
        }
        Bitmap side=Bitmap.createBitmap(frame.copyPixels(),720,1280,Bitmap.Config.ARGB_8888);Matrix m=new Matrix();m.postRotate(270);
        Bitmap upright=Bitmap.createBitmap(side,0,0,720,1280,m,false);
        Task<Text> task=null;Runnable cleanup=()->{upright.recycle();side.recycle();};
        try {
            task=recognizer.process(InputImage.fromBitmap(upright,0));Text full=Tasks.await(task,12,TimeUnit.SECONDS);
            String scene=DailyEventUi.classify(full.getText());
            if(scene.equals("unknown"))android.util.Log.d("A9DailyOcr",full.getText());
            Map<String,Object> out=new LinkedHashMap<>();
            out.put("scene",scene);out.put("text",full.getText());List<Object> words=new ArrayList<>();int[] later=null;
            for(Text.TextBlock block:full.getTextBlocks())for(Text.Line line:block.getLines()){
                Rect b=line.getBoundingBox();if(b==null)continue;
                words.add(Map.of("text",line.getText(),"x",b.centerX(),"y",b.centerY()));
                if(VehicleUnlockGuard.laterLabel(line.getText(),b.left,b.top,b.right,b.bottom))later=new int[]{b.left,b.top,b.right,b.bottom};
            }
            out.put("words",words);
            if(touch!=null){
                touch.unlockGuard.observe(VehicleUnlockGuard.prompt(full.getText()),later,!scene.equals("unknown")&&!scene.equals("vehicleUnlock"));
                if(touch.unlockGuard.active()){
                    out.put("scene","vehicleUnlock");int[] center=touch.unlockGuard.laterCenter();
                    if(center!=null)out.put("unlockLater",Map.of("x",center[0],"y",center[1]));return out;
                }
            }
            if(scene.equals("loading")){
                // Daily loot places the track BELOW the event name, unlike multiplayer's upper-left title.
                String title=text(379,405,439,850,3);out.put("mapText",title);
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
                lastUiScene="daily-"+scene;SessionState.log("每日战利品界面："+scene);
                snapshot(new File(root,"daily-"+scene+".png").getAbsolutePath(),0,0,719,1279);
            }
            return out;
        }finally{if(task!=null&&!task.isComplete())task.addOnCompleteListener(Runnable::run,done->cleanup.run());else cleanup.run();}
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
        if(!SessionState.resultOverlay){SessionState.resultOverlay(true);capture.awaitFrameAfter(SystemClock.elapsedRealtime()+180);}
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
            switch(name) {
                case "_dailyEventUi":return result(l,dailyEventUi());
                case "_garageFilterPrepare":
                    SessionState.overlayMode(false,true);capture.awaitFrameAfter(SystemClock.elapsedRealtime());return 0;
                case "_garageFilterState":return result(l,GarageFilter.read(frame()));
                case "keepCapture": capture();return 0;
                case "releaseCapture": return 0;
                case "tap": touch.tap(8,n(l,1),n(l,2));return 0;
                case "touchDown": touch.down(n(l,1),n(l,2),n(l,3));return 0;
                case "touchMove": touch.move(n(l,1),n(l,2),n(l,3));return 0;
                case "touchUp": touch.up(n(l,1));return 0;
                case "swipe": touch.swipe(n(l,1),n(l,2),n(l,3),n(l,4),n(l,5));return 0;
                case "cmpColorEx":return result(l,ColorPattern.matches(frame(),s(l,1),ratio(l,2))?1:0);
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
                case "_hud":SessionState.hud=s(l,1);SessionState.changed();return 0;
                case "_finished":finished=true;persist(true);return 0;
                case "_jsonEncode":return result(l,toJson(l.toObject(1)).toString());
                case "_jsonDecode":return result(l,LocalStore.javaValue(new JSONTokener(s(l,1)).nextValue()));
                case "_textOcr":return result(l,text(n(l,1),n(l,2),n(l,3),n(l,4)));
                case "_multiplayerUi":return result(l,multiplayerUi());
                case "_routeDisplay":return result(l,store.matcher.displayName(s(l,1)));
                case "_sessionCycle":SessionState.stats.cycle(n(l,1));return 0;
                case "_dailyTaskStatus":return result(l,dailySkipReason(n(l,1)));
                case "_queueResume":return result(l,checkpoint.resume());
                case "_queueEnter":checkpoint.enter(n(l,1),n(l,2),n(l,3));persist(false);return result(l,checkpoint.completed());
                case "_queueDone":
                    legacyPrepared=legacyRaceSeen=legacyRewardDone=false;legacyCoins=legacyFame=-1;rewardStable.reset();
                    checkpoint.projectDone();persist(false);return 0;
                case "_sessionLimitReached":return result(l,store.config.optInt("maxRaces",0)>0&&SessionState.stats.finished()>=store.config.optInt("maxRaces",0));
                case "_sessionProject":SessionState.stats.project(s(l,1));SessionState.stats.resumeProject(l.getTop()>1?n(l,2):0,checkpoint.completed());return result(l,checkpoint.completed());
                case "_sessionRaceBegin":{
                    String key=s(l,1);Route r=store.routes.get(key);SessionState.stats.begin(store.matcher.displayName(key),l.toBoolean(2));
                    if(l.toBoolean(2))SessionState.stats.routeReason(key.isEmpty()?"地图未识别":r==null||r.raw.isEmpty()?"暂无跑法":"跑法无效");
                    rewardStable.reset();persist(false);return 0;
                }
                case "_sessionRaceRewards":
                    if(SessionState.stats.rewards(l.toInteger(1),l.toInteger(2))){SessionState.log("本局收益：金币 "+l.toInteger(1)+" · 名人堂 "+l.toInteger(2)+"（-1 为未识别）");persist(false);}return 0;
                case "_sessionLegacyPrepare":
                    legacyPrepared=true;legacyRaceSeen=legacyRewardDone=legacyInvalid=false;legacyMap="";legacyCoins=legacyFame=-1;rewardStable.reset();SessionState.overlayMode(false,false);return 0;
                case "_sessionLegacyMap":
                    if(legacyPrepared){legacyMap=s(l,1);legacyInvalid=l.toBoolean(2);SessionState.stats.routeMap(legacyMap,legacyInvalid);}return 0;
                case "_legacyRewardSample":return result(l,legacyRewardSample());
                case "_sessionLegacyRewards":
                    if(legacyPrepared&&legacyRaceSeen&&!legacyRewardDone){legacyCoins=l.toInteger(1);legacyFame=l.toInteger(2);legacyRewardDone=true;SessionState.log("本局收益识别：金币 "+(legacyCoins<0?"未识别":legacyCoins)+" / 名人堂 "+(legacyFame<0?"未识别":legacyFame));}return 0;
                case "_sessionRaceResult":{
                    int before=SessionState.stats.finished(),beforeRanked=SessionState.stats.rankedCount();SessionState.stats.result(l.getTop()>0?n(l,1):0);
                    checkpoint.completed(SessionState.stats.finished()-before);
                    if(SessionState.stats.finished()>before)SessionState.log("任务累计完赛 "+SessionState.stats.finished()+" / "+store.config.optInt("maxRaces",0)+" · 世界 "+SessionState.stats.count("多人上")+" / 大师 "+SessionState.stats.count("多人下")+" · 自动重启 "+SessionState.stats.restartCount());
                    if(SessionState.stats.finished()!=before||SessionState.stats.rankedCount()!=beforeRanked)persist(false);
                    return 0;
                }
                case "_sessionLegacyCompleted":SessionState.stats.legacyCompleted(legacyCoins,legacyFame);legacyPrepared=legacyRaceSeen=false;legacyCoins=legacyFame=-1;SessionState.overlayMode(false,false);checkpoint.completed(1);recordDailyCompletion();persist(false);return 0;
                case "_sessionOperation":SessionState.stats.operation(s(l,1));return 0;
                case "_sessionRaceReturned":SessionState.stats.returned();persist(false);return 0;
                case "_fallbackBegin":
                    driverStart=clock;lastProgressTime=clock;lastProgress=-1;lastLoggedProgress=-10;progress.reset();return 0;
                case "_fallbackStep": {
                    int p=progress.accept(store.digits.progress(frame(),store.config.optInt("accuracy",80)/100.0));
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
                    driverStart=clock;lastDigitTime=clock;lastProgressTime=clock;lastProgress=-1;lastLoggedProgress=-10;lastPulse=clock-300;progress.reset();return 0;
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
    private int step(Lua l) {
        if(driver==null&&!driverFallback)throw new IllegalStateException("跑图尚未初始化");capture();
        boolean end=ColorPattern.color(frame.pixel(68,812),"5400FF-050505",1)||ColorPattern.color(frame.pixel(708,288),"5400FF",1);
        if(end){if(driver!=null)driver.stop();return result(l,true,-1,"比赛结束");}
        int rawProgress=store.digits.progress(frame,store.config.optInt("accuracy",80)/100.0);
        int p=progress.accept(rawProgress);
        // Readable backtracking after a crash is not an OCR outage. Keep route progress monotonic.
        if(rawProgress>=0)lastDigitTime=clock;
        if(p>lastProgress){lastProgress=p;lastProgressTime=clock;}
        if(!driverFallback&&clock-lastDigitTime>12_000)throw failure("12 秒未能确认进度，请在诊断页检查截图方向和进度字库");
        if(clock-lastProgressTime>90_000)throw failure("比赛进度 90 秒未前进，请检查游戏是否卡住或暂停");
        if(clock-driverStart>1000L*store.config.optInt("raceTimeout",900))throw failure("单局跑图超时，可在设置中调整单局最长运行时间");
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
        try{snapshot(new File(root,"diagnostic.png").getAbsolutePath(),0,0,719,1279);SessionState.log("已保存停止时画面，可在诊断页查看");}
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
        int percent=store.digits.progress(frame,.8);
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
