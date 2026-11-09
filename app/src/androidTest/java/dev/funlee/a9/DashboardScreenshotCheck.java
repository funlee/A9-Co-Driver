package dev.funlee.a9;

import android.app.Instrumentation;
import android.os.Bundle;
import android.graphics.*;
import android.content.Context;
import android.view.*;
import android.widget.*;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import dev.funlee.a9.core.*;
import java.util.concurrent.TimeUnit;

/** Replays saved screenshots without game input; optional production-layout preview uses sample data. */
public final class DashboardScreenshotCheck extends Instrumentation {
    private boolean preview;
    @Override public void onCreate(Bundle args){super.onCreate(args);preview="true".equals(args.getString("preview"));start();}
    @Override public void onStart(){
        Bundle report=new Bundle();TextRecognizer recognizer=TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
        final View[] view={null};final WindowManager[] wm={null};
        try {
            String[] names={"personal-footer-first.jpg","personal-reward-sixth.jpg","winner-not-personal.png"};
            int[] expected={1,6,0};
            for(int i=0;i<names.length;i++){
                Bitmap source=BitmapFactory.decodeStream(getContext().getAssets().open(names[i]));
                Bitmap frame=Bitmap.createScaledBitmap(source,1280,720,true);
                Text full=Tasks.await(recognizer.process(InputImage.fromBitmap(frame,0)),15,TimeUnit.SECONDS);
                for(Text.TextBlock bl:full.getTextBlocks())for(Text.Line li:bl.getLines())if(li.getText().contains("第"))android.util.Log.i("A9RankCheck",names[i]+" "+li.getText()+" "+li.getBoundingBox());
                int rank=RankOcr.read(recognizer,frame,full,true);
                if(rank==0)rank=RankOcr.read(recognizer,frame,full,false);
                report.putString(names[i],"rank="+rank+" expected="+expected[i]);
                if(rank!=expected[i])throw new AssertionError(names[i]+" rank="+rank+" OCR="+full.getText());
                if(frame!=source)frame.recycle();source.recycle();
            }
            if(preview){
                TaskStats stats=new TaskStats();stats.cycle(1);stats.project("多人上");stats.projectTarget(30);
                for(int n:new int[]{1,4,6,7,8}){stats.begin("神山垭口冰封路径",false);stats.result(n);stats.returned();}
                stats.begin("神山垭口冰封路径",false);stats.progress(45,18,39);stats.operation("D · 漂移 3000ms");stats.elapsed(89*60_000);
                runOnMainSync(()->{
                    Context context=new ContextThemeWrapper(getTargetContext(),dev.funlee.a9.R.style.AppTheme);
                    TextView label=new TextView(context);label.setText(stats.panel("示例预览 · 未运行比赛"));
                    Button pause=new Button(context);pause.setText("暂停");Button stop=new Button(context);stop.setText("停止");
                    TaskPanelView panel=TaskPanelView.create(context,label,pause,stop);panel.update(stats,"示例预览",false,"",false);view[0]=panel;wm[0]=(WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
                    Point size=new Point();wm[0].getDefaultDisplay().getRealSize(size);
                    WindowManager.LayoutParams p=new WindowManager.LayoutParams(size.x*265/1280,-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,PixelFormat.TRANSLUCENT);
                    p.gravity=Gravity.BOTTOM|Gravity.LEFT;p.x=8;p.y=8;wm[0].addView(view[0],p);
                });
                Thread.sleep(15000);
            }
            report.putString("result","PASS: personal footer 1; reward 6; winner excluded; no game input dispatched");finish(0,report);
        }catch(Throwable e){report.putString("result","FAIL: "+e);finish(1,report);}
        finally{recognizer.close();if(view[0]!=null)runOnMainSync(()->wm[0].removeView(view[0]));}
    }
}
