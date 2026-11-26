package dev.funlee.a9;

import android.graphics.*;
import com.google.android.gms.tasks.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import dev.funlee.a9.core.*;
import java.util.concurrent.TimeUnit;

/** Upright 1280x720 reward-page exit controls. Coin/fame OCR was removed to speed up settlement. */
final class RewardOcr {
    /** Keep the recognized label and enabled button coordinates from the same frame. */
    static java.util.Map<String,Object> exitButton(Text full,PixelFrame frame){
        return exitButton(full,frame,0,0,1);
    }
    private static java.util.Map<String,Object> exitButton(Text full,PixelFrame frame,int left,int top,int scale){
        java.util.Map<String,Object> next=java.util.Map.of();
        for(Text.TextBlock block:full.getTextBlocks())for(Text.Line line:block.getLines()){
            Rect b=line.getBoundingBox();if(b==null)continue;
            int x=left+b.centerX()/scale,y=top+b.centerY()/scale;
            String label=RaceRewards.exitLabel(line.getText(),x,y);
            if(label.isEmpty()||!RaceRewards.enabledExit(frame,x,y))continue;
            java.util.Map<String,Object> button=java.util.Map.of("x",x,"y",y,"label",label);
            if(label.equals("错失机会"))return button;
            next=button;
        }
        return next;
    }
    static java.util.Map<String,Object> focusedExitButton(TextRecognizer recognizer,Bitmap upright,PixelFrame frame)throws Exception{
        boolean visible=false;
        for(int x:new int[]{960,1020,1080,1140,1200})for(int y:new int[]{618,646,674,690})
            if(RaceRewards.enabledExit(frame,x,y)){visible=true;break;}
        if(!visible)return java.util.Map.of();
        int left=850,top=585,scale=3;
        Bitmap crop=Bitmap.createBitmap(upright,left,top,430,135);
        Bitmap zoom=Bitmap.createScaledBitmap(crop,430*scale,135*scale,true);
        crop.recycle();
        return exitButton(recognize(recognizer,zoom),frame,left,top,scale);
    }
    private static Text recognize(TextRecognizer recognizer,Bitmap image)throws Exception{
        Task<Text> task;
        try{task=recognizer.process(InputImage.fromBitmap(image,0));}catch(Exception e){image.recycle();throw e;}
        task.addOnCompleteListener(Runnable::run,done->image.recycle());
        return Tasks.await(task,12,TimeUnit.SECONDS);
    }
}
