package dev.funlee.a9;

import android.graphics.*;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognizer;
import dev.funlee.a9.core.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Targeted OCR for fuel badges and car identity omitted by full-screen recognition. */
final class GarageOcr {
    static boolean soloHeading(TextRecognizer ocr,PixelFrame frame)throws Exception{
        String heading=MapMatcher.normalize(region(ocr,frame,40,65,365,155,3))
                .replace('車','车').replace('輛','辆').replace('擇','择');
        return heading.contains("车辆选择")||heading.contains("车辅选择");
    }

    /** Read identity from separate crops of the same frame, away from the busy garage background. */
    static Map<String,Object> detail(TextRecognizer ocr,PixelFrame frame,boolean wide)throws Exception{
        String name=region(ocr,frame,160,100,670,180,4);
        String performance=wide?region(ocr,frame,780,128,1060,172,4):region(ocr,frame,780,125,1060,160,1);
        return MultiplayerGarage.detail(List.of(Map.of("text",name,"x",300,"y",140),
                Map.of("text",performance,"x",950,"y",140)));
    }
    private static String region(TextRecognizer ocr,PixelFrame frame,int left,int top,int right,int bottom,int scale)throws Exception{
        Bitmap all=Bitmap.createBitmap(frame.copyPixels(),720,1280,Bitmap.Config.ARGB_8888);
        Bitmap crop=Bitmap.createBitmap(all,719-bottom,left,bottom-top+1,right-left+1);
        Matrix rotation=new Matrix();rotation.postRotate(270);
        Bitmap upright=Bitmap.createBitmap(crop,0,0,crop.getWidth(),crop.getHeight(),rotation,true);
        Bitmap scaled=Bitmap.createScaledBitmap(upright,upright.getWidth()*scale,upright.getHeight()*scale,true);
        Runnable cleanup=()->{if(scaled!=upright)scaled.recycle();upright.recycle();crop.recycle();all.recycle();};
        com.google.android.gms.tasks.Task<com.google.mlkit.vision.text.Text> task;
        try{task=ocr.process(InputImage.fromBitmap(scaled,0));}catch(Exception e){cleanup.run();throw e;}
        task.addOnCompleteListener(Runnable::run,done->cleanup.run());
        return Tasks.await(task,12,TimeUnit.SECONDS).getText();
    }
    static Map<String,Object> read(TextRecognizer ocr,PixelFrame frame,List<Object> words,String rank,boolean wide)throws Exception{
        return read(ocr,frame,words,rank,wide?20.0/9:16.0/9);
    }
    static Map<String,Object> read(TextRecognizer ocr,PixelFrame frame,List<Object> words,String rank,double aspect)throws Exception{
        Map<String,Object> garage=MultiplayerGarage.read(frame,words,rank,aspect);
        List<Object> supplemented=new ArrayList<>(words);
        for(Object value:(List<?>)garage.get("cards")){
            Map<?,?> card=(Map<?,?>)value;
            int left=((Number)card.get("x")).intValue()-MultiplayerGarage.cardCenterOffset(aspect),top=((Number)card.get("y")).intValue()-110;
            if(aspect>=2.0){
                int right=150;
                String performance=region(ocr,frame,left+10,top+28,left+right,top+65,4);
                String originalPerformance=card.get("performance").toString();
                boolean complete=performance.replaceAll("[^0-9]","").length()>=originalPerformance.replaceAll("[^0-9]","").length();
                if(complete&&MultiplayerGarage.validPerformance(performance)&&performance.matches("(?s).*\\d{2,}\\s*/\\s*[0-9,]{3,}.*"))
                    supplemented.add(Map.of("text",performance,"x",left+73,"y",top+46));
            }
            if(aspect>2.07){
                String name=region(ocr,frame,left+235,top+110,left+345,top+177,4);
                supplemented.add(Map.of("text",name,"x",left+290,"y",top+145));
            }
            if(((Number)card.get("fuel")).intValue()>=0)continue;
            int green=0;
            for(int x=left+10;x<=left+70;x+=5)for(int y=top+190;y<=top+210;y+=5){
                int c=frame.pixel(719-y,x),r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;
                if(r>90&&g>160&&b<130)green++;
            }
            if(green<15)continue; // Locked cards have no fuel badge here.
            Bitmap all=Bitmap.createBitmap(frame.copyPixels(),720,1280,Bitmap.Config.ARGB_8888);
            Bitmap crop=Bitmap.createBitmap(all,719-(top+218),left+22,38,65);
            Matrix rotation=new Matrix();rotation.postRotate(270);
            Bitmap upright=Bitmap.createBitmap(crop,0,0,crop.getWidth(),crop.getHeight(),rotation,true);
            Bitmap scaled=Bitmap.createScaledBitmap(upright,upright.getWidth()*4,upright.getHeight()*4,true);
            var task=ocr.process(InputImage.fromBitmap(scaled,0));
            task.addOnCompleteListener(Runnable::run,done->{scaled.recycle();upright.recycle();crop.recycle();all.recycle();});
            String text=Tasks.await(task,12,TimeUnit.SECONDS).getText();
            supplemented.add(Map.of("text",text,"x",left+54,"y",top+199));
        }
        return MultiplayerGarage.read(frame,supplemented,rank,aspect);
    }
}
