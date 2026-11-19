package dev.funlee.a9;

import android.app.Instrumentation;
import android.graphics.*;
import android.os.Bundle;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import dev.funlee.a9.core.PixelFrame;
import java.util.Map;

final class GarageDetailDeviceCheck {
    static void run(Instrumentation test,Bundle report)throws Exception{
        TextRecognizer ocr=TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
        try{
            Bitmap upright=BitmapFactory.decodeStream(test.getContext().getAssets().open("garage-detail-nissan.png"));
            Matrix rotation=new Matrix();rotation.postRotate(90);
            Bitmap side=Bitmap.createBitmap(upright,0,0,1280,720,rotation,false);
            int[] pixels=new int[720*1280];side.getPixels(pixels,0,720,0,0,720,1280);
            side.recycle();upright.recycle();
            PixelFrame frame=new PixelFrame(720,1280,pixels);
            Map<String,Object> detail=GarageOcr.detail(ocr,frame,false);
            if(!detail.get("name").toString().contains("370ZNISMO")||!detail.get("performance").equals("620/1337"))
                throw new AssertionError("Live Nissan detail: "+detail);
            report.putString("result","PASS: live Nissan screenshot identity "+detail+"; no game input");
        }finally{ocr.close();}
    }
}
