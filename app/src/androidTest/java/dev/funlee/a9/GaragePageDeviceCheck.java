package dev.funlee.a9;

import android.app.Instrumentation;
import android.graphics.*;
import android.os.Bundle;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import dev.funlee.a9.core.PixelFrame;
import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

final class GaragePageDeviceCheck {
    static void run(Instrumentation test,Bundle report)throws Exception {run(test,report,false);}
    static void run(Instrumentation test,Bundle report,boolean detail)throws Exception {
        File source=new File(test.getTargetContext().getFilesDir(),detail?"runtime/multiplayer-car.png":"runtime/multiplayer-garage-unconfirmed.png");
        Bitmap side=BitmapFactory.decodeFile(source.getAbsolutePath());
        int[] pixels=new int[720*1280];side.getPixels(pixels,0,720,0,0,720,1280);
        PixelFrame frame=new PixelFrame(720,1280,pixels);
        Matrix rotation=new Matrix();rotation.postRotate(270);
        Bitmap upright=Bitmap.createBitmap(side,0,0,720,1280,rotation,false);
        TextRecognizer recognizer=TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
        try {
            Text read=Tasks.await(recognizer.process(InputImage.fromBitmap(upright,0)),12,TimeUnit.SECONDS);
            List<Object> words=new ArrayList<>();
            for(Text.TextBlock block:read.getTextBlocks())for(Text.Line line:block.getLines()){
                Rect box=line.getBoundingBox();if(box!=null)words.add(Map.of("text",line.getText(),"x",box.centerX(),"y",box.centerY()));
            }
            if(detail){
                var full=dev.funlee.a9.core.MultiplayerGarage.detail(words);
                var focused=GarageOcr.detail(recognizer,frame,true);
                report.putString("full",full.toString());report.putString("focused",focused.toString());
                if(full.get("performance").equals("")||!full.get("performance").equals(focused.get("performance")))
                    throw new AssertionError("Full and focused vehicle performance disagree");
                report.putString("result","PASS: full and focused vehicle performance agree");return;
            }
            var garage=GarageOcr.read(recognizer,frame,words,"",20.0/9);
            report.putString("garage",garage.toString());report.putString("words",words.toString());
            if(((List<?>)garage.get("cards")).size()<2)throw new AssertionError("Could not identify visible garage page: "+garage);
            report.putString("result","PASS: scrolled garage cards recognized");
        }finally{recognizer.close();upright.recycle();side.recycle();}
    }
}
