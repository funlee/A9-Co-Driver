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
import java.util.concurrent.TimeUnit;

/** Screenshot replay by default. Explicit restart/crash modes exercise recovery, never start races. */
public final class RecoveryDeviceCheck extends Instrumentation {
    private String mode;
    @Override public void onCreate(Bundle args){super.onCreate(args);mode=args.getString("mode","screenshots");start();}
    @Override public void onStart(){
        Bundle report=new Bundle();
        try{
            if(mode.equals("screenshots"))screenshots(report);
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
