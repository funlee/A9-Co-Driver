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
                        // Earnings OCR was removed: the replay only needs the scene label and the safe ad exit.
                        Text full=Tasks.await(recognizer.process(InputImage.fromBitmap(b,0)),12,TimeUnit.SECONDS);
                        boolean exit=false;for(Text.TextBlock block:full.getTextBlocks())for(Text.Line line:block.getLines()){
                            Rect bounds=line.getBoundingBox();if(bounds!=null&&DailyEventUi.rewardExit(line.getText(),bounds.centerX(),bounds.centerY()))exit=true;
                        }
                        if(!exit)throw new AssertionError("Missing safe ad exit");report.putString("rewards","ad offer 9600 excluded");
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
