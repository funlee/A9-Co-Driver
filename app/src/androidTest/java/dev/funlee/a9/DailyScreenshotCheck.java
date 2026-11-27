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
                    Text full=Tasks.await(recognizer.process(InputImage.fromBitmap(b,0)),12,TimeUnit.SECONDS);
                    String text=full.getText();
                    if(!scene.replace("-live","").equals(DailyEventUi.classify(text)))throw new AssertionError(scene+": "+text);
                    if(scene.equals("event")&&!DailyEventUi.dailyLootLobby(text))
                        throw new AssertionError("Daily loot lobby not detected: "+text);
                    if(scene.startsWith("rewards")){
                        Matrix rotation=new Matrix();rotation.postRotate(90);
                        Bitmap side=Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight(),rotation,false);
                        int[] pixels=new int[side.getWidth()*side.getHeight()];
                        side.getPixels(pixels,0,side.getWidth(),0,0,side.getWidth(),side.getHeight());
                        PixelFrame frame=new PixelFrame(side.getWidth(),side.getHeight(),pixels);
                        side.recycle();
                        Map<String,Object> exit=RewardOcr.exitButton(full,frame);
                        if(!"错失机会".equals(exit.get("label")))throw new AssertionError("Missing enabled ad exit: "+exit);
                        report.putString("rewards","enabled 错失机会 recognized while 观看广告 was visible");
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
