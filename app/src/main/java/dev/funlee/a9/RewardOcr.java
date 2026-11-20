package dev.funlee.a9;

import android.graphics.*;
import com.google.android.gms.tasks.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import dev.funlee.a9.core.*;
import java.util.concurrent.TimeUnit;

/** Upright 1280x720 reward layout, including the paid-account boost badge. */
final class RewardOcr {
    static boolean page(String text){String s=MapMatcher.normalize(text);return ResultRank.personalReward(text)&&s.contains("总金币");}
    static RaceRewards read(TextRecognizer recognizer,Bitmap upright,Text full)throws Exception{
        return read(recognizer,upright,full,false);
    }
    static RaceRewards read(TextRecognizer recognizer,Bitmap upright,Text full,boolean soloLayout)throws Exception{
        return read(recognizer,upright,full,soloLayout,false);
    }
    static RaceRewards read(TextRecognizer recognizer,Bitmap upright,Text full,boolean soloLayout,boolean wide)throws Exception{
        if(!page(full.getText()))return new RaceRewards(-1,-1);
        boolean total=false;
        for(Text.TextBlock block:full.getTextBlocks())for(Text.Line line:block.getLines()){
            Rect b=line.getBoundingBox();if(b!=null&&b.centerX()<450&&b.centerY()>380&&b.centerY()<550&&MapMatcher.normalize(line.getText()).equals("总金币"))total=true;
        }
        // Coins exclude the currency icon, rank reward and +10% boost. Fame excludes the VIP 90% badge.
        // The caller selects the layout; ad invitations do not change the totals font.
        Rect goldBounds,fameBounds;
        if(wide){
            goldBounds=soloLayout?new Rect(121,512,315,581):new Rect(116,513,295,575);
            fameBounds=new Rect(1060,98,1175,145);
        }else{
            goldBounds=soloLayout?new Rect(110,538,307,597):new Rect(95,530,260,585);
            fameBounds=soloLayout?new Rect(1095,65,1190,123):new Rect(1090,65,1195,123);
        }
        String goldText=total?crop(recognizer,upright,goldBounds):"";
        String fameText=crop(recognizer,upright,fameBounds);
        long coins=RaceRewards.settlementAmount(goldText,false),fame=RaceRewards.settlementAmount(fameText,true);
        if(coins<0||fame<0)android.util.Log.d("A9RewardOcr","total="+total+" soloLayout="+soloLayout+" gold="+goldText+" fame="+fameText);
        return new RaceRewards(coins,fame);
    }
    /** Keep the recognized label and enabled button coordinates from the same frame. */
    static java.util.Map<String,Object> exitButton(Text full,PixelFrame frame){
        for(Text.TextBlock block:full.getTextBlocks())for(Text.Line line:block.getLines()){
            Rect b=line.getBoundingBox();if(b==null)continue;
            String label=RaceRewards.exitLabel(line.getText(),b.centerX(),b.centerY());
            if(label.isEmpty())continue;
            int color=frame.pixel(719-b.centerY()+25,b.centerX());
            if(ColorPattern.color(color,"FFFFFF-202020",1)||ColorPattern.color(color,"12FBC3-303030",1))
                return java.util.Map.of("x",b.centerX(),"y",b.centerY(),"label",label);
        }
        return java.util.Map.of();
    }
    private static String crop(TextRecognizer recognizer,Bitmap frame,Rect b)throws Exception{
        Bitmap small=Bitmap.createBitmap(frame,b.left,b.top,b.width(),b.height());
        Bitmap enlarged=Bitmap.createScaledBitmap(small,b.width()*3,b.height()*3,true);
        small.recycle();
        Text isolated=recognize(recognizer,enlarged);
        boolean upsideDown=false;
        for(Text.TextBlock block:isolated.getTextBlocks())for(Text.Line line:block.getLines()){
            if(Math.abs(line.getAngle())>90)upsideDown=true;
        }
        if(!upsideDown)return isolated.getText();
        // Digits alone (notably 99,999) can cause ML Kit to rotate a horizontal line 180°.
        // Put a clearly upright label beside the crop and read only the original number area.
        int scale=3, left=300, height=b.height()*scale;
        Bitmap sample=Bitmap.createBitmap(left+b.width()*scale,height,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(sample);
        canvas.drawColor(0xff152030);
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF,Typeface.BOLD));
        paint.setTextSize(66);
        canvas.drawText("Total",16,(height-paint.ascent()-paint.descent())/2,paint);
        canvas.drawBitmap(frame,b,new Rect(left,0,sample.getWidth(),height),paint);
        Text result=recognize(recognizer,sample);
        StringBuilder digits=new StringBuilder();
        for(Text.TextBlock block:result.getTextBlocks())for(Text.Line line:block.getLines()){
            if(Math.abs(line.getAngle())>90)return "";
            StringBuilder row=new StringBuilder();
            for(Text.Element word:line.getElements()){
                Rect box=word.getBoundingBox();
                if(box!=null&&box.centerX()>=left)row.append(word.getText());
            }
            if(row.length()>0){
                if(digits.length()>0)digits.append('\n');
                digits.append(row);
            }
        }
        return digits.toString();
    }
    private static Text recognize(TextRecognizer recognizer,Bitmap image)throws Exception{
        Task<Text> task;
        try{task=recognizer.process(InputImage.fromBitmap(image,0));}catch(Exception e){image.recycle();throw e;}
        task.addOnCompleteListener(Runnable::run,done->image.recycle());
        return Tasks.await(task,12,TimeUnit.SECONDS);
    }
}
