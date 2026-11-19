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
    static void run(Instrumentation test,Bundle report,boolean detail)throws Exception {run(test,report,detail?"detail":"page");}
    static void run(Instrumentation test,Bundle report,String mode)throws Exception {
        boolean detail=mode.equals("detail"),finish=mode.equals("finish"),reward=mode.equals("reward"),score=mode.equals("score");
        File source=new File(test.getTargetContext().getFilesDir(),score?"runtime/multiplayer-event.png":reward?"runtime/daily-rewards.png":finish?"runtime/diagnostic.png":detail?"runtime/multiplayer-car.png":"runtime/multiplayer-garage-unconfirmed.png");
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
            if(score){
                Bitmap crop=Bitmap.createBitmap(upright,315,525,145,53);
                Bitmap enlarged=Bitmap.createScaledBitmap(crop,435,159,true);crop.recycle();
                try{
                    String value=Tasks.await(recognizer.process(InputImage.fromBitmap(enlarged,0)),12,TimeUnit.SECONDS).getText();
                    report.putString("score",value);
                    if(dev.funlee.a9.core.RaceRewards.amount(value,false)!=1053)throw new AssertionError("Wide score crop clips the digits or includes the label");
                    report.putString("result","PASS: live wide World Series score recognized");return;
                }finally{enlarged.recycle();}
            }
            if(reward){
                report.putString("text",read.getText());
                List<Object> footer=new ArrayList<>();
                for(Text.TextBlock block:read.getTextBlocks())for(Text.Line line:block.getLines()){
                    Rect b=line.getBoundingBox();if(b!=null&&b.centerY()>590)footer.add(Map.of("text",line.getText(),"bounds",b.toString(),"label",dev.funlee.a9.core.RaceRewards.exitLabel(line.getText(),b.centerX(),b.centerY()),"color",Integer.toHexString(frame.pixel(719-b.centerY()+25,b.centerX()))));
                }
                report.putString("footer",footer.toString());report.putBoolean("page",RewardOcr.page(read.getText()));
                var amounts=RewardOcr.read(recognizer,upright,read,false,true);
                report.putString("amounts",amounts.coins+"/"+amounts.fame);
                if(amounts.coins!=1200||amounts.fame!=80)throw new AssertionError("Wide reward crop includes a currency icon or clips the fame digits");
                var exit=RewardOcr.exitButton(read,frame);report.putString("exit",exit.toString());
                if(!RewardOcr.page(read.getText())||!"错失机会".equals(exit.get("label")))throw new AssertionError("Observed traditional skip label was not recognized");
                report.putString("result","PASS: observed reward skip button recognized without selecting the ad");return;
            }
            if(finish){
                String scene=dev.funlee.a9.core.MultiplayerUi.classify(read.getText());
                report.putString("scene",scene);report.putString("text",read.getText());
                if(!dev.funlee.a9.core.MultiplayerUi.raceEnded(scene))throw new AssertionError("Missed terminal multiplayer screen");
                report.putString("result","PASS: multiplayer result without a progress meter is recognized");return;
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
