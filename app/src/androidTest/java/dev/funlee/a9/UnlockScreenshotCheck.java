package dev.funlee.a9;

import android.app.Instrumentation;
import android.os.Bundle;
import android.graphics.*;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import dev.funlee.a9.core.*;
import java.util.concurrent.TimeUnit;

/** Uses the production offline OCR model on the user's screenshot; never dispatches input. */
public final class UnlockScreenshotCheck extends Instrumentation {
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    @Override public void onStart(){
        Bundle report=new Bundle();TextRecognizer recognizer=null;Bitmap source=null,frame=null;
        try {
            source=BitmapFactory.decodeStream(getContext().getAssets().open("new-car-unlock.jpg"));
            frame=Bitmap.createScaledBitmap(source,1280,720,true);
            recognizer=TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
            Text text=Tasks.await(recognizer.process(InputImage.fromBitmap(frame,0)),15,TimeUnit.SECONDS);
            int[] later=null;Rect unlock=null;
            for(Text.TextBlock block:text.getTextBlocks())for(Text.Line line:block.getLines()){
                Rect b=line.getBoundingBox();if(b==null)continue;
                if(VehicleUnlockGuard.laterLabel(line.getText(),b.left,b.top,b.right,b.bottom))later=new int[]{b.left,b.top,b.right,b.bottom};
                if(VehicleUnlockGuard.normalize(line.getText()).equals("解锁车辆"))unlock=b;
            }
            String scene=MultiplayerUi.classify(text.getText());
            if(!scene.equals("vehicleUnlock")||later==null||unlock==null)throw new AssertionError("Missing prompt or button OCR: "+text.getText());
            VehicleUnlockGuard guard=new VehicleUnlockGuard();guard.observe(true,later,false);
            int[] center=guard.laterCenter();guard.requireTap(center[0],center[1]);boolean denied=false;
            try{guard.requireTap(unlock.centerX(),unlock.centerY());}catch(IllegalStateException expected){denied=true;}
            if(!denied)throw new AssertionError("Unlock click was not rejected");
            report.putString("result","PASS: screenshot recognized; Later allowed; Unlock rejected; no input dispatched");
            report.putString("later",center[0]+","+center[1]);report.putString("unlock",unlock.centerX()+","+unlock.centerY());
            report.putString("ocr",text.getText());finish(0,report);
        } catch(Throwable e){report.putString("result","FAIL: "+e);finish(1,report);}
        finally{if(recognizer!=null)recognizer.close();if(frame!=null&&frame!=source)frame.recycle();if(source!=null)source.recycle();}
    }
}
