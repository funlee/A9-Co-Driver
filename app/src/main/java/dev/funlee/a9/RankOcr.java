package dev.funlee.a9;

import android.graphics.*;
import com.google.android.gms.tasks.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import dev.funlee.a9.core.ResultRank;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Only spatially scoped personal badges are eligible; never the winner animation. */
final class RankOcr {
    static int read(TextRecognizer recognizer,Bitmap upright,Text full,boolean footer)throws Exception{
        String context=full.getText();
        if(!(footer?ResultRank.personalFooter(context):ResultRank.personalReward(context)))return 0;
        List<String> badges=new ArrayList<>();List<Rect> missingDigit=new ArrayList<>();
        for(Text.TextBlock block:full.getTextBlocks())for(Text.Line line:block.getLines()){
            Rect b=line.getBoundingBox();if(b==null||b.left<0||b.right>300||b.height()<35)continue;
            if(footer?(b.top<565||b.bottom>715):(b.top<25||b.bottom>180))continue;
            badges.add(line.getText());
            // ML Kit sometimes detects the whole two-character badge but emits only 第.
            // A one-character box is too narrow: do not guess a digit from its strokes.
            if(line.getText().trim().equals("第")&&b.width()>b.height()*1.15)missingDigit.add(b);
        }
        int rank=ResultRank.fromLines(context,badges,footer);
        if(rank==0&&missingDigit.size()==1){
            Rect b=missingDigit.get(0);int candidate=0;
            for(double height:new double[]{.76,.74}){
                int x=b.left+(int)(b.width()*.56),y=Math.max(0,b.top-2);
                int w=Math.min(upright.getWidth()-x,(int)(b.width()*.59)),h=(int)Math.round(b.height()*height);
                Bitmap crop=Bitmap.createBitmap(upright,x,y,w,h);
                Bitmap big=Bitmap.createScaledBitmap(crop,w*3,h*3,true);crop.recycle();
                int[] pixels=new int[big.getWidth()*big.getHeight()];big.getPixels(pixels,0,big.getWidth(),0,0,big.getWidth(),big.getHeight());
                for(int i=0;i<pixels.length;i++){int c=pixels[i];pixels[i]=Math.min(Color.red(c),Math.min(Color.green(c),Color.blue(c)))>205?Color.BLACK:Color.WHITE;}
                big.setPixels(pixels,0,big.getWidth(),0,0,big.getWidth(),big.getHeight());
                Task<Text> task;
                try{task=recognizer.process(InputImage.fromBitmap(big,0));}catch(Exception e){big.recycle();throw e;}
                task.addOnCompleteListener(Runnable::run,done->big.recycle());
                String digit=Tasks.await(task,12,TimeUnit.SECONDS).getText().trim();
                SessionState.log("个人名次数字补读："+digit);
                if(digit.matches("[1-8]")){int n=Integer.parseInt(digit);if(candidate>0&&candidate!=n){candidate=0;break;}candidate=n;}
            }
            rank=candidate;
        }
        SessionState.log((footer?"左下个人名次":"奖励个人名次")+" OCR："+badges+" → "+(rank==0?"待确认":rank));
        return rank;
    }
}
