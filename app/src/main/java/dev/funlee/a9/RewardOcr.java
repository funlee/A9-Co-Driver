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
        if(!page(full.getText()))return new RaceRewards(-1,-1);
        boolean total=false;
        for(Text.TextBlock block:full.getTextBlocks())for(Text.Line line:block.getLines()){
            Rect b=line.getBoundingBox();if(b!=null&&b.centerX()<450&&b.centerY()>380&&b.centerY()<550&&MapMatcher.normalize(line.getText()).equals("总金币"))total=true;
        }
        // Coins exclude the currency icon, rank reward and +10% boost. Fame excludes the VIP 90% badge.
        // The caller selects the layout; ad invitations do not change the totals font.
        String goldText=total?crop(recognizer,upright,soloLayout?new Rect(110,538,270,597):new Rect(95,530,260,585)):"";
        String fameText=crop(recognizer,upright,soloLayout?new Rect(1095,65,1190,123):new Rect(1090,65,1195,123));
        long coins=RaceRewards.amount(goldText,false),fame=RaceRewards.amount(fameText,true);
        if(coins<0||fame<0)android.util.Log.d("A9RewardOcr","total="+total+" soloLayout="+soloLayout+" gold="+goldText+" fame="+fameText);
        return new RaceRewards(coins,fame);
    }
    private static String crop(TextRecognizer recognizer,Bitmap frame,Rect b)throws Exception{
        Bitmap small=Bitmap.createBitmap(frame,b.left,b.top,b.width(),b.height());Bitmap large=Bitmap.createScaledBitmap(small,b.width()*3,b.height()*3,true);small.recycle();
        Task<Text> task;try{task=recognizer.process(InputImage.fromBitmap(large,0));}catch(Exception e){large.recycle();throw e;}
        task.addOnCompleteListener(Runnable::run,done->large.recycle());return Tasks.await(task,12,TimeUnit.SECONDS).getText();
    }
}
