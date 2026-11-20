package dev.funlee.a9;

import android.app.Instrumentation;
import android.graphics.*;
import android.os.Bundle;
import android.content.Context;
import android.view.*;
import android.widget.*;
import java.io.*;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import dev.funlee.a9.core.*;
import java.util.concurrent.TimeUnit;

/** Optional private screenshots. Exercises production OCR; never dispatches game input. */
final class RewardScreenshotCheck {
    static void run(Instrumentation test,Bundle report)throws Exception{
        TextRecognizer ocr=TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
        try{
            String[] files={"reward-standard.png","reward-paid.png","reward-multiplayer-ad.png"};long[][] expected={{4800,189},{2880,207},{4800,189}};
            for(int i=0;i<files.length;i++){
                Bitmap image=BitmapFactory.decodeStream(test.getContext().getAssets().open(files[i]));
                try{
                    Text full=Tasks.await(ocr.process(InputImage.fromBitmap(image,0)),15,TimeUnit.SECONDS);
                    RaceRewards rewards=RewardOcr.read(ocr,image,full);
                    String result="coins="+rewards.coins+", fame="+rewards.fame;
                    android.util.Log.i("A9RewardCheck",files[i]+" "+result+" OCR="+full.getText());
                    if(rewards.coins!=expected[i][0]||rewards.fame!=expected[i][1])throw new AssertionError(files[i]+" "+result);
                    report.putString(files[i],result);
                    if(i==0)checkMaximum(ocr,image,false,false);

                    Bitmap masked=image.copy(Bitmap.Config.ARGB_8888,true);Canvas canvas=new Canvas(masked);Paint paint=new Paint();paint.setColor(Color.BLACK);canvas.drawRect(95,530,260,585,paint);
                    RaceRewards missing=RewardOcr.read(ocr,masked,full);if(missing.coins!=-1||missing.fame!=expected[i][1])throw new AssertionError("Unreadable coins must not become zero or contaminate fame");masked.recycle();
                }finally{image.recycle();}
            }
            try(InputStream input=test.getContext().getAssets().open("daily-rewards-live.png")){
                Bitmap solo=BitmapFactory.decodeStream(input);
                try{checkMaximum(ocr,solo,false,true);}finally{solo.recycle();}
            }
            report.putString("maxima","99,999 coins / +999 fame in both standard layouts");
            preview(test,report);
            report.putString("result","PASS: production reward OCR, standard 4800/189 and paid 2880/207; no game input");
        }finally{ocr.close();}
    }
    /** Boundary replay on a receipt, retaining the real currency icon, boost and help badge. */
    static void checkMaximum(TextRecognizer ocr,Bitmap receipt,boolean wide,boolean solo)throws Exception{
        Bitmap sample=receipt.copy(Bitmap.Config.ARGB_8888,true);
        try{
            Canvas canvas=new Canvas(sample);
            Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));
            int goldX=wide?(solo?125:118):(solo?114:99);
            int goldY=wide?558:(solo?586:564);
            int xpRight=wide?1168:1187,xpY=wide?136:114;
            paint.setColor(0xff112233);
            canvas.drawRect(goldX-1,goldY-42,goldX+174,goldY+8,paint);
            canvas.drawRect(xpRight-92,xpY-35,xpRight+2,xpY+4,paint);
            paint.setColor(Color.WHITE);paint.setTextSize(solo?50:40);
            paint.setTextScaleX((solo?163f:136f)/paint.measureText("99,999"));
            canvas.drawText("99,999",goldX,goldY,paint);
            paint.setTextScaleX(1);paint.setTextSize(36);paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("+999",xpRight,xpY,paint);
            Text full=Tasks.await(ocr.process(InputImage.fromBitmap(sample,0)),12,TimeUnit.SECONDS);
            RaceRewards amounts=RewardOcr.read(ocr,sample,full,solo,wide);
            if(amounts.coins!=99999||amounts.fame!=999)
                throw new AssertionError("Boundary receipt "+(wide?"wide":"standard")+"/"+solo+": "+amounts.coins+"/"+amounts.fame);
        }finally{sample.recycle();}
    }
    private static void preview(Instrumentation test,Bundle report)throws Exception{
        final WindowManager[] window={null};final TaskPanelView[] panel={null};final TextView[] heading={null};
        try{
            test.runOnMainSync(()->{
                Context context=new ContextThemeWrapper(test.getTargetContext(),R.style.AppTheme);TaskStats stats=new TaskStats();stats.project("多人下");stats.cycle(1);stats.projectTarget(20);
                stats.begin("大桥湾云霄飞车",false);stats.result(2);stats.rewards(4800,189);stats.returned();stats.begin("季风秘境幽林飞瀑",false);stats.progress(58,18,39);
                heading[0]=new TextView(context);Button pause=new Button(context),stop=new Button(context);pause.setText("暂停");stop.setText("停止");panel[0]=TaskPanelView.create(context,heading[0],pause,stop);panel[0].update(stats,"示例预览",false,"",false);
                window[0]=(WindowManager)context.getSystemService(Context.WINDOW_SERVICE);Point size=new Point();window[0].getDefaultDisplay().getRealSize(size);
                WindowManager.LayoutParams params=new WindowManager.LayoutParams(size.x*TaskPanelView.WIDTH/1280,size.y*TaskPanelView.HEIGHT/720,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);params.gravity=Gravity.BOTTOM|Gravity.START;params.x=8;params.y=8;window[0].addView(panel[0],params);
            });
            Thread.sleep(350);save(test,panel[0],"overlay-compact.png");
            report.putString("preview","full-information panel PNG saved; no collapse controls");
        }finally{if(panel[0]!=null)test.runOnMainSync(()->window[0].removeView(panel[0]));}
    }
    private static Button findButton(View view,String text){if(view instanceof Button&&text.contentEquals(((Button)view).getText()))return (Button)view;if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++){Button found=findButton(group.getChildAt(i),text);if(found!=null)return found;}}return null;}
    private static void save(Instrumentation test,View view,String name)throws Exception{
        final Bitmap[] rendered={null};test.runOnMainSync(()->{rendered[0]=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);view.draw(new Canvas(rendered[0]));});
        File path=new File(test.getTargetContext().getExternalFilesDir(null),name);try(FileOutputStream out=new FileOutputStream(path)){rendered[0].compress(Bitmap.CompressFormat.PNG,100,out);}finally{rendered[0].recycle();}
    }

}
