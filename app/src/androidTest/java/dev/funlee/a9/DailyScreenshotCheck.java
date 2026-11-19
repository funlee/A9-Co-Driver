package dev.funlee.a9;
import android.app.Instrumentation;
import android.graphics.*;
import android.os.Bundle;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import dev.funlee.a9.core.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

final class DailyScreenshotCheck {
    static void run(Instrumentation test,Bundle report)throws Exception {
        TextRecognizer recognizer=TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
        try{
            LocalStore store=new LocalStore(test.getTargetContext());
            for(String scene:List.of("loading","results","cards","conversion","event","rewards","rewards-live")){
                Bitmap b=BitmapFactory.decodeStream(test.getContext().getAssets().open("daily-"+scene+".png"));
                try{
                    String text=Tasks.await(recognizer.process(InputImage.fromBitmap(b,0)),12,TimeUnit.SECONDS).getText();
                    if(!scene.replace("-live","").equals(DailyEventUi.classify(text)))throw new AssertionError(scene+": "+text);
                    if(scene.startsWith("rewards")){
                        Text full=Tasks.await(recognizer.process(InputImage.fromBitmap(b,0)),12,TimeUnit.SECONDS);
                        RaceRewards rewards=RewardOcr.read(recognizer,b,full,true);
                        if(rewards.coins!=3200||rewards.fame!=105)throw new AssertionError("Daily rewards: "+rewards.coins+"/"+rewards.fame+" scene="+MultiplayerUi.classify(full.getText())+" text="+full.getText());
                        if(scene.equals("rewards-live")){
                            // The same solo totals appear before the ad is offered. Hide its whole
                            // panel to prove layout selection does not depend on marketing text.
                            Bitmap noAd=b.copy(Bitmap.Config.ARGB_8888,true);
                            try{
                                Paint paint=new Paint();paint.setColor(Color.BLACK);
                                new Canvas(noAd).drawRect(470,160,1280,720,paint);
                                Text noAdText=Tasks.await(recognizer.process(InputImage.fromBitmap(noAd,0)),12,TimeUnit.SECONDS);
                                RaceRewards solo=RewardOcr.read(recognizer,noAd,noAdText,true);
                                if(solo.coins!=3200||solo.fame!=105)throw new AssertionError("Solo layout without ad: "+solo.coins+"/"+solo.fame);
                            }finally{noAd.recycle();}
                        }
                        boolean exit=false;for(Text.TextBlock block:full.getTextBlocks())for(Text.Line line:block.getLines()){
                            Rect bounds=line.getBoundingBox();if(bounds!=null&&DailyEventUi.rewardExit(line.getText(),bounds.centerX(),bounds.centerY()))exit=true;
                        }
                        if(!exit)throw new AssertionError("Missing safe ad exit");report.putString("rewards","3200 coins / 105 fame; ad offer 9600 excluded");
                    }
                    if(scene.equals("loading")){
                        Bitmap crop=Bitmap.createBitmap(b,405,280,446,61),scaled=Bitmap.createScaledBitmap(crop,1338,183,true);
                        String title=Tasks.await(recognizer.process(InputImage.fromBitmap(scaled,0)),12,TimeUnit.SECONDS).getText();scaled.recycle();crop.recycle();
                        MapMatcher.Match match=store.matcher.match(title);
                        if(!match.unique()||!match.candidates.get(0).internal.equals("旧金山喧闹铁路"))throw new AssertionError("Map crop: "+title);
                        report.putString("map",match.candidates.get(0).display);
                    }
                }finally{b.recycle();}
            }
            report.putString("result","PASS: real OCR loading/map, solo results, blueprint cards, red-token conversion and event return; no input dispatched");
        }finally{recognizer.close();}
    }
}
