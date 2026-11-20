package dev.funlee.a9;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Intent;
import android.os.SystemClock;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.*;
import android.os.Bundle;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import dev.funlee.a9.core.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import party.iroiro.luajava.Lua;

/** Replays two private screenshots with the installed APK's native Lua, OCR and file checkpoints. */
final class QueueRetryDeviceCheck {
    private TaskCheckpoint queue = new TaskCheckpoint();
    private int project, failures, completions, confirmations;
    private boolean finished;
    private final List<Integer> quotas = new ArrayList<>();
    private final List<String> notices = new ArrayList<>();
    private static final List<String> API = List.of("_queueResume", "_queueEnter", "_queueHomeConfirmed",
            "_queueDone", "_sessionProject", "_sessionCycle", "finishRace", "_hud", "_taskNotice", "print", "toast", "remainingQuota");

    private TouchService overlay;
    private UiAutomation ui;

    static void run(Instrumentation test, Bundle report,boolean checkWindow) throws Exception {
        QueueRetryDeviceCheck replay=new QueueRetryDeviceCheck();
        if(!checkWindow){replay.play(test,report);return;}
        replay.ui=test.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
        var activity=test.startActivitySync(new Intent(test.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try{
            long deadline=SystemClock.elapsedRealtime()+10000;
            while(TouchService.instance==null&&SystemClock.elapsedRealtime()<deadline)SystemClock.sleep(100);
            replay.overlay=TouchService.instance;
            if(replay.overlay==null||SessionState.running)throw new AssertionError("An idle accessibility service is required");
            SessionState.running=true;
            try{
                replay.overlay.arm(test.getTargetContext().getPackageName());
                replay.play(test,report);
                report.putString("window","All four notices appeared and dismissed on Android");
            }finally{
                replay.overlay.disarm();
                SessionState.running=false;
            }
        }finally{test.runOnMainSync(activity::finish);}
    }

    private void checkWindow(String message,int duration){
        overlay.showTaskNotice(message,duration);
        long start=SystemClock.elapsedRealtime();
        while(overlay.activityHintBounds(SystemClock.elapsedRealtime())==null&&SystemClock.elapsedRealtime()-start<2000)
            SystemClock.sleep(50);
        Rect bounds=overlay.activityHintBounds(SystemClock.elapsedRealtime());
        if(bounds==null||bounds.isEmpty())throw new AssertionError("Notice window missing: "+message);
        Bitmap screenshot=ui.takeScreenshot();
        try{
            if(screenshot==null||bounds.right>screenshot.getWidth()||bounds.bottom>screenshot.getHeight())
                throw new AssertionError("Notice window outside screenshot");
        }finally{if(screenshot!=null)screenshot.recycle();}
        while(overlay.activityHintBounds(SystemClock.elapsedRealtime())!=null&&SystemClock.elapsedRealtime()-start<6000)
            SystemClock.sleep(100);
        long elapsed=SystemClock.elapsedRealtime()-start;
        if(elapsed<duration-150||elapsed>=6000)throw new AssertionError("Unexpected notice duration: "+elapsed);
    }

    private static Map<String,Object> readPage(Context context, String filename, TextRecognizer recognizer) throws Exception {
        Bitmap raw = BitmapFactory.decodeFile(new File(context.getFilesDir(),filename).getPath());
        if (raw == null) throw new IllegalStateException("Missing screenshot fixture " + filename);
        Bitmap frame = CaptureSession.normalizeFrame(raw,90,raw.getWidth(),raw.getHeight());
        Matrix rotation = new Matrix();
        rotation.postRotate(270);
        Bitmap upright = Bitmap.createBitmap(frame,0,0,720,1280,rotation,false);
        try {
            Text text = Tasks.await(recognizer.process(InputImage.fromBitmap(upright,0)),40,TimeUnit.SECONDS);
            List<Map<String,Object>> words = new ArrayList<>();
            for (Text.TextBlock block : text.getTextBlocks()) for (Text.Line line : block.getLines()) {
                Rect b = line.getBoundingBox();
                if (b != null) words.add(Map.of("text",line.getText(),"x",b.centerX(),"y",b.centerY()));
            }
            int[] colors = new int[720*1280];
            frame.getPixels(colors,0,720,0,0,720,1280);
            PixelFrame pixels = new PixelFrame(720,1280,colors);
            ModalPopup.Panel panel = ModalPopup.locate(pixels);
            Map<String,Object> dismiss = ModalPopup.dismiss(pixels,panel,words,(double)raw.getWidth()/raw.getHeight());
            String scene = dismiss != null ? "interruptingPopup" : MainMenuUi.classify(text.getText(),words);
            Map<String,Object> page = new HashMap<>();
            page.put("scene",scene);
            page.put("text",text.getText());
            if (dismiss != null && !dismiss.isEmpty()) page.put("dismissPopup",dismiss);
            boolean obstructed = panel != null || dismiss != null || !MainMenuUi.returnOverlay(pixels).isEmpty();
            page.put("homeEvidence",MainMenuUi.homeEvidence(pixels,text.getText(),words,scene,obstructed));
            return page;
        } finally {
            upright.recycle();
            frame.recycle();
            raw.recycle();
        }
    }

    private void play(Instrumentation test, Bundle report) throws Exception {
        Context target = test.getTargetContext();
        Context isolated = new ContextWrapper(target) {
            @Override public File getFilesDir() { return new File(target.getCacheDir(),"queue-retry-check"); }
        };
        Map<String,Object> home, modal;
        try (TextRecognizer reader = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build())) {
            home = readPage(target,"queue-home.jpg",reader);
            modal = readPage(target,"queue-modal.png",reader);
        }
        Map<?,?> dismiss = (Map<?,?>) modal.get("dismissPopup");
        if ((int)home.get("homeEvidence")==0 || (int)modal.get("homeEvidence")!=0 || dismiss==null ||
                !dismiss.get("label").toString().contains("稍后领取")) {
            throw new AssertionError("Unexpected ML Kit page or action. home=" + home + " modal=" + modal);
        }
        String fixture;
        try (var in = test.getContext().getAssets().open("queue-device-retry.lua")) {
            fixture = new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
        LocalStore store = new LocalStore(target);
        for (int vm = 0; vm < 3; vm++) {
            try (LuaRuntime lua = new LuaRuntime(this::call,API)) {
                lua.set("home",home);
                lua.set("modal",modal);
                lua.set("UIconfig", Map.of("routeOnly","", "ActiveProj",List.of(true,true),
                        "projects",List.of(Map.of("项目","特殊赛事","data",Map.of("次数","3")),
                                Map.of("项目","多人上","data",Map.of("次数","1")))));
                lua.load("bootstrap",store.asset("lua/bootstrap.lua"));
                lua.load("utilities",store.asset("lua/utilities.lua"));
                lua.load("fixture",fixture);
                lua.start("main");
                for (int step=0; step<120 && !finished; step++) lua.step(step*300L);
            } catch (RuntimeException fault) {
                if (!fault.toString().contains("fixture: business failed") || vm==2) throw fault;
                failures++;
                queue.failure("fixture-"+failures);
                SessionRecord.write(isolated,new JSONObject(),queue,new TaskStats(),false);
                JSONObject saved = SessionRecord.read(isolated).getJSONObject("queue");
                queue = new TaskCheckpoint();
                queue.restore(saved.getInt("cycle"),saved.getInt("index"),saved.getInt("completed"),saved.getInt("rewardCycle"));
                queue.restoreRecovery(saved.getInt("failures"),saved.getBoolean("needsHome"),saved.getBoolean("homeRestarted"),
                        saved.getBoolean("rewardWork"),saved.getString("firstFailure"),saved.getString("lastFailure"));
            }
        }
        if (!finished || failures!=2 || confirmations!=2 || completions!=3 || !quotas.equals(List.of(3,2,1))) {
            throw new AssertionError("Queue terminated early or replayed a settled race: "+quotas);
        }
        if(notices.size()!=4 || !notices.get(0).contains("异常，正在回主") || !notices.get(0).contains("fixture-1") ||
                !notices.get(1).contains("正在重试") || !notices.get(1).contains("已结算 1 局") ||
                !notices.get(2).contains("再次异常") || !notices.get(2).contains("fixture-2") ||
                !notices.get(3).contains("跳过项目 1 · 特殊赛事"))
            throw new AssertionError("Unexpected queue notices: "+notices);
        report.putString("notices",notices.toString());
        report.putString("homeRuleBits",home.get("homeEvidence").toString());
        report.putString("popupAction",dismiss.toString());
        report.putString("result","PASS: ML Kit, two strict home confirmations, 3/2/1 quotas, second failure skipped");
    }

    private int call(String name,Lua lua) {
        switch (name) {
            case "_taskNotice":
                notices.add(lua.toString(1));
                if(overlay!=null)checkWindow(lua.toString(1),(int)lua.toInteger(2));
                break;
            case "_queueResume": LuaRuntime.push(lua,queue.resume()); return 1;
            case "_queueEnter":
                project=(int)lua.toInteger(2);
                queue.enter((int)lua.toInteger(1),project,(int)lua.toInteger(3));
                lua.push(queue.completed()); return 1;
            case "_sessionProject": lua.push(queue.completed()); return 1;
            case "_queueHomeConfirmed":
                queue.homeConfirmed();
                confirmations++;
                LuaRuntime.push(lua,queue.resume()); return 1;
            case "_queueDone": queue.projectDone(); finished=project==2; break;
            case "finishRace": queue.completed(1); completions++; break;
            case "remainingQuota": quotas.add((int)lua.toInteger(1)); break;
            default: break;
        }
        return 0;
    }
}
