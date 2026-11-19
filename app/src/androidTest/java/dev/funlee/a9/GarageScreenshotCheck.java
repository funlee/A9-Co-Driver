package dev.funlee.a9;

import android.app.Instrumentation;
import android.os.Bundle;
import android.graphics.*;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import dev.funlee.a9.core.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

/** Read-only regression over retained device frames, using the production OCR and garage detector. */
public final class GarageScreenshotCheck extends Instrumentation {
    private Bundle arguments;
    @Override public void onCreate(Bundle args){arguments=args;super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle report=new Bundle();TextRecognizer ocr=TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
        try{
            Map<String,Object> garage=null,detail=null;
            for(String scene:List.of("cars","car")){
                Bitmap side=BitmapFactory.decodeFile(new File(getTargetContext().getFilesDir(),"runtime/"+(scene.equals("cars")?arguments.getString("carsFrame","multiplayer-cars.png"):"multiplayer-car.png")).getPath());
                if(side==null)throw new AssertionError("Missing retained "+scene+" frame");
                if(side.getWidth()==1280){Matrix m=new Matrix();m.postRotate(90);Bitmap normalized=Bitmap.createBitmap(side,0,0,side.getWidth(),side.getHeight(),m,false);side.recycle();side=normalized;}
                Matrix rotation=new Matrix();rotation.postRotate(270);Bitmap image=Bitmap.createBitmap(side,0,0,720,1280,rotation,false);
                Text text=Tasks.await(ocr.process(InputImage.fromBitmap(image,0)),15,TimeUnit.SECONDS);
                List<Object> words=new ArrayList<>();
                for(Text.TextBlock block:text.getTextBlocks())for(Text.Line line:block.getLines()){
                    Rect b=line.getBoundingBox();if(b!=null)words.add(Map.of("text",line.getText(),"x",b.centerX(),"y",b.centerY()));
                }
                if(scene.equals("cars")){
                    Bitmap crop=Bitmap.createBitmap(side,250,90,86,111);Bitmap rot=Bitmap.createBitmap(crop,0,0,86,111,rotation,true);Bitmap scaled=Bitmap.createScaledBitmap(rot,rot.getWidth()*3,rot.getHeight()*3,true);
                    String rank=Tasks.await(ocr.process(InputImage.fromBitmap(scaled,0)),15,TimeUnit.SECONDS).getText();
                    int[] pixels=new int[720*1280];side.getPixels(pixels,0,720,0,0,720,1280);
                    // Reproduce the observed full-frame OCR omission; production crop must recover it.
                    words.removeIf(value->{Map<?,?> w=(Map<?,?>)value;int x=((Number)w.get("x")).intValue(),y=((Number)w.get("y")).intValue();return x>210&&x<340&&y>365&&y<420;});
                    garage=GarageOcr.read(ocr,new PixelFrame(720,1280,pixels),words,rank,false);
                    report.putString("garage",new JSONObject(garage).toString());report.putString("rank",rank);
                    scaled.recycle();rot.recycle();crop.recycle();
                    if(!Boolean.TRUE.equals(garage.get("start")))throw new AssertionError("Retained first-page screenshot was not confirmed: "+garage+" OCR="+words);
                }else{detail=MultiplayerGarage.detail(words);report.putString("detail",new JSONObject(detail).toString());}
                image.recycle();side.recycle();
            }
            Map<?,?> first=(Map<?,?>)((List<?>)garage.get("cards")).get(0);
            if(((Number)first.get("fuel")).intValue()<=0)throw new AssertionError("Retained first car fuel not recognized: "+first);
            if(!first.get("performance").equals(detail.get("performance")))throw new AssertionError("Performance mismatch: "+first+" vs "+detail);
            boolean match=false;for(Object token:(List<?>)first.get("tokens"))if(detail.get("name").toString().contains(token.toString()))match=true;
            if(!match)throw new AssertionError("Vehicle model mismatch");
            report.putString("result","PASS: origin, first card and selected vehicle agree; no input dispatched");finish(0,report);
        }catch(Throwable e){report.putString("result","FAIL: "+e);finish(1,report);}finally{ocr.close();}
    }
}
