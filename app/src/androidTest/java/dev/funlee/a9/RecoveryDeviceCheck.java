package dev.funlee.a9;

import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.*;
import android.os.*;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import dev.funlee.a9.core.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Screenshot replay by default. Explicit restart/crash modes exercise recovery, never start races. */
public final class RecoveryDeviceCheck extends Instrumentation {
    private String mode;
    private int cupPosition;
    private String cupTitle;
    @Override public void onCreate(Bundle args){super.onCreate(args);mode=args.getString("mode","screenshots");cupPosition=Integer.parseInt(args.getString("cupPosition","3"));cupTitle=args.getString("cupTitle","");start();}
    @Override public void onStart(){
        Bundle report=new Bundle();
        try{
            if(mode.equals("panel-window"))PanelWindowCheck.run(this,report);
            else if(mode.equals("panel-visibility"))PanelVisibilityDeviceCheck.run(this,report);
            else if(mode.equals("reward-menus"))RewardMenuCheck.run(this,report);
            else if(mode.equals("panel-layout"))PanelLayoutCheck.run(this,report);
            else if(mode.equals("custom-maps"))CustomMapDeviceCheck.run(this,report);
            else if(mode.equals("route-reset"))RouteResetDeviceCheck.run(this,report);
            else if(mode.equals("task-queue"))TaskQueueDeviceCheck.run(this,report);
            else if(mode.equals("task-editing"))TaskEditingDeviceCheck.run(this,report);
            else if(mode.equals("screenshots"))screenshots(report);
            else if(mode.equals("daily-schedule"))DailyScheduleDeviceCheck.run(this,report);
            else if(mode.equals("daily-screenshots"))DailyScreenshotCheck.run(this,report);
            else if(mode.equals("garage-screenshots"))new GarageFilterDeviceCheck(this).screenshots(report);
            else if(mode.equals("rotation-recovery"))RotationRecoveryDeviceCheck.run(this,report);
            else if(mode.equals("home-replay")||mode.equals("home-clean"))homeReplay(report,mode.equals("home-clean"));
            else if(mode.equals("capture-resources"))CaptureResourceDeviceCheck.run(this,report);
            else if(mode.equals("garage-detail"))GarageDetailDeviceCheck.run(this,report);
            else if(mode.equals("garage-filter"))new GarageFilterDeviceCheck(this).run(report);
            else if(mode.equals("rewards"))RewardScreenshotCheck.run(this,report);
            else if(mode.equals("cup-selection"))new CupSelectionDeviceCheck(this).run(cupPosition,cupTitle,report);
            else if(mode.equals("restart")||mode.equals("crash")){
                if(SessionState.running)throw new IllegalStateException("Stop the current task before a device recovery check");
                long until=SystemClock.elapsedRealtime()+15000;
                while(TouchService.instance==null&&SystemClock.elapsedRealtime()<until)Thread.sleep(200);
                TouchService touch=TouchService.instance;if(touch==null)throw new IllegalStateException("Accessibility service unavailable");
                String game=new LocalStore(getTargetContext()).config.getString("gamePkg");touch.arm(game);
                Intent launch=getTargetContext().getPackageManager().getLaunchIntentForPackage(game);
                getTargetContext().startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Thread.sleep(4000);
                if(!touch.gameInForeground())throw new IllegalStateException("Game is not foreground");
                if(mode.equals("crash")){
                    android.util.Log.i("A9RecoveryCheck","READY_FOR_EXTERNAL_CRASH");
                    until=SystemClock.elapsedRealtime()+30000;
                    while(touch.gameInForeground()&&SystemClock.elapsedRealtime()<until)Thread.sleep(200);
                    if(touch.gameInForeground())throw new IllegalStateException("No external crash was injected");
                }
                GameRecovery recovery=new GameRecovery(getTargetContext(),touch,game,()->false,()->false);
                if(!recovery.recover(mode.equals("restart")?GameRecovery.Reason.MATCH_TIMEOUT:GameRecovery.Reason.FOREGROUND_LOST))throw new AssertionError("No recovery performed");
                touch.disarm();report.putString("result","PASS: production "+mode+" recovery returned game to foreground; no race started");
            }else throw new IllegalArgumentException("Unknown mode");
            finish(0,report);
        }catch(Throwable e){report.putString("result","FAIL: "+e);finish(1,report);}
    }
    private void homeReplay(Bundle report,boolean cleanHome)throws Exception{
        File image=new File(getTargetContext().getFilesDir(),"home-replay.jpg");
        Bitmap raw=BitmapFactory.decodeFile(image.getAbsolutePath());
        if(raw==null)throw new IllegalStateException("Missing private home-replay.jpg fixture");
        Bitmap frame=null,upright=null;
        TextRecognizer recognizer=TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
        try{
            frame=CaptureSession.normalizeFrame(raw,90,raw.getWidth(),raw.getHeight());
            Matrix matrix=new Matrix();matrix.postRotate(270);
            upright=Bitmap.createBitmap(frame,0,0,720,1280,matrix,false);
            Text result=Tasks.await(recognizer.process(InputImage.fromBitmap(upright,0)),20,TimeUnit.SECONDS);
            List<Map<String,Object>> words=new ArrayList<>();
            boolean stay=false,daily=false,explore=false;
            for(Text.TextBlock block:result.getTextBlocks())for(Text.Line line:block.getLines()){
                Rect bounds=line.getBoundingBox();if(bounds==null)continue;
                int x=bounds.centerX(),y=bounds.centerY();String text=line.getText();
                words.add(Map.of("text",text,"x",x,"y",y));
                String label=MapMatcher.normalize(text).replace('賽','赛');
                stay|=(label.equals("再玩一会")||label.equals("再玩一会儿"))&&x>=300&&x<=710&&y>=370&&y<=640;
                daily|=(label.equals("每日")||label.equals("每日赛事"))&&x>=420&&x<=650&&y>=580&&y<=715;
                explore|=(label.equals("探索赛事")||label.equals("採索赛事")||label.equals("采索赛事"))&&
                        x>=720&&x<=1265&&y>=330&&y<=585;
            }
            if(cleanHome){
                int[] pixels=new int[720*1280];frame.getPixels(pixels,0,720,0,0,720,1280);
                var button=MainMenuUi.dailyExplore(result.getText(),new PixelFrame(720,1280,pixels));
                if(!MainMenuUi.classify(result.getText(),words).equals("home")||
                        Math.abs(button.getOrDefault("x",0)-1025)>12||Math.abs(button.getOrDefault("y",0)-509)>12)
                    throw new AssertionError("Daily home visual action not confirmed: "+button);
                report.putString("result","PASS: selected Daily home and Explore button located despite OCR misreading the label");
                report.putString("dailyExplore",button.toString());
                return;
            }
            if(!MainMenuUi.classify(result.getText(),words).equals("exitGame")||!stay||!daily||!explore||
                    !MainMenuUi.classify("",words).equals("home"))
                throw new AssertionError("The normalized screenshot did not identify the exit dialog, safe button and menu controls: "+words);
            report.putString("result","PASS: real wide screenshot recognized the exit modal and Stay button, with Daily and Explore coordinates");
        }finally{
            recognizer.close();
            if(upright!=null)upright.recycle();
            if(frame!=null)frame.recycle();
            raw.recycle();
        }
    }
    private void screenshots(Bundle report)throws Exception{
        Bitmap upright=BitmapFactory.decodeStream(getContext().getAssets().open("matching-back-present.png"));
        TextRecognizer recognizer=TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
        try{
            Text ocr=Tasks.await(recognizer.process(InputImage.fromBitmap(upright,0)),15,TimeUnit.SECONDS);
            if(!MultiplayerUi.classify(ocr.getText()).equals("matching"))throw new AssertionError("Matching OCR: "+ocr.getText());
            Matrix m=new Matrix();m.postRotate(90);Bitmap side=Bitmap.createBitmap(upright,0,0,1280,720,m,false);
            int[] p=new int[720*1280];side.getPixels(p,0,720,0,0,720,1280);side.recycle();
            var back=MatchmakingWatchdog.backButton(new PixelFrame(720,1280,p),false);
            if(back!=MatchmakingWatchdog.Back.PRESENT)throw new AssertionError("Visible Back classified as "+back);
            MatchmakingWatchdog watchdog=new MatchmakingWatchdog();watchdog.observe(true,back,0,1);
            if(watchdog.observe(true,back,180000,2))throw new AssertionError("Visible Back must prevent restart");
            Bitmap rewards=BitmapFactory.decodeStream(getContext().getAssets().open("dynamic-reward-choice.png"));
            try{
                Text rewardText=Tasks.await(recognizer.process(InputImage.fromBitmap(rewards,0)),15,TimeUnit.SECONDS);
                if(!MultiplayerUi.classify(rewardText.getText()).equals("rewardChoice"))throw new AssertionError("Dynamic reward OCR: "+rewardText.getText());
                Bitmap rewardSide=Bitmap.createBitmap(rewards,0,0,1280,720,m,false);
                rewardSide.getPixels(p,0,720,0,0,720,1280);rewardSide.recycle();
                int cards=DynamicRewards.cards(new PixelFrame(720,1280,p)).size();
                if(cards!=5)throw new AssertionError("Expected five complete reward cards, got "+cards);
                report.putInt("rewardCards",cards);
            }finally{rewards.recycle();}
            report.putString("result","PASS: matching OCR and visible Back veto; dynamic reward OCR before Confirm and five cards; no input dispatched");
        }finally{recognizer.close();upright.recycle();}
    }
}
