package dev.funlee.a9;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import dev.funlee.a9.core.TaskStats;
import java.io.*;

/** Offscreen production view rendering; no activity, overlay window or game input. */
final class PanelLayoutCheck {
    static void run(Instrumentation test,Bundle report)throws Exception {
        final Throwable[] error={null};
        test.runOnMainSync(()->{
            try {
                Context context=new ContextThemeWrapper(test.getTargetContext(),R.style.AppTheme);
                TaskStats stats=new TaskStats();stats.project("多人下");stats.cycle(1);stats.projectTarget(20);
                stats.begin("季风秘境幽林飞瀑",false);stats.result(2);stats.rewards(4800,189);stats.returned();
                for(String mode:new String[]{"杯赛","每日战利品","特殊赛事"}) {
                    stats.project(mode);stats.begin("示例地图",false);stats.legacyCompleted(3200,105);
                }
                stats.project("多人下");stats.begin("季风秘境幽林飞瀑",false);stats.progress(58,18,39);stats.operation("N · 氮气");stats.elapsed(17*60000L);
                TextView heading=new TextView(context);Button pause=new Button(context),stop=new Button(context);
                pause.setText("暂停");stop.setText("停止");
                TaskPanelView panel=TaskPanelView.create(context,heading,pause,stop);
                android.util.DisplayMetrics metrics=context.getResources().getDisplayMetrics();
                int width=Math.max(metrics.widthPixels,metrics.heightPixels)*TaskPanelView.WIDTH/1280;
                final int[] clicks={0,0};pause.setOnClickListener(v->clicks[0]++);stop.setOnClickListener(v->clicks[1]++);
                String[] phases={"正在准备","正在领取免费包","正在领取每日奖励","领奖：已确认领取每日目标奖励 2 项","领奖检查结束，继续任务","正在恢复","多人流程：cars","多人流程：car","等待匹配","比赛中","已暂停","结算中"};
                for(String phase:phases){
                    panel.setCompact(!phase.equals("比赛中")&&!phase.equals("已暂停"));
                    panel.update(stats,phase,false,"",phase.equals("结算中"));
                    if(heading.getVisibility()!=View.VISIBLE)throw new AssertionError("Current activity hidden in "+phase);
                    if(phase.startsWith("正在领取")&&!heading.getText().toString().contains(phase))throw new AssertionError("Reward activity not displayed: "+heading.getText());
                    pause.performClick();stop.performClick();
                    save(context,panel,pause,stop,width,"panel-"+phase.replace('：','-')+".png");
                }
                if(clicks[0]!=phases.length||clicks[1]!=phases.length)throw new AssertionError("Pause/stop callbacks unavailable in a phase");
                panel.setCompact(false);
                if(button(panel,"详情")!=null||button(panel,"精简")!=null||button(panel,"展开")!=null)
                    throw new AssertionError("Fold controls still present");
                panel.update(stats,"比赛中",false,"",false);save(context,panel,pause,stop,width,"panel-default.png");
            } catch(Throwable e){error[0]=e;}
        });
        if(error[0]!=null)throw new AssertionError("Panel render failed",error[0]);
        report.putString("result","PASS: full/compact panels rendered across startup/rewards/recovery/selection/matching/race/pause/result; pause/stop callbacks available; no game input");
    }
    private static Button button(View view,String label){
        if(view instanceof Button&&label.contentEquals(((Button)view).getText()))return (Button)view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){Button result=button(group.getChildAt(i),label);if(result!=null)return result;}}
        return null;
    }
    private static void checkTextFits(View view){
        if(view.getVisibility()!=View.VISIBLE)return;
        if(view instanceof TextView&&!(view instanceof Button)){
            TextView text=(TextView)view;
            if(text.getLayout()!=null&&text.getLayout().getLineCount()>0&&text.getLayout().getLineWidth(0)>text.getWidth()+1)
                throw new AssertionError("Text clipped: "+text.getText());
            if(text.getLayout()!=null&&text.getLayout().getHeight()>text.getHeight()-text.getPaddingTop()-text.getPaddingBottom())
                throw new AssertionError("Text vertically clipped: "+text.getText());
        }
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)checkTextFits(group.getChildAt(i));}
    }
    private static boolean visible(View view){
        while(view!=null){if(view.getVisibility()!=View.VISIBLE)return false;view=view.getParent() instanceof View?(View)view.getParent():null;}
        return true;
    }
    private static void save(Context context,TaskPanelView panel,Button pause,Button stop,int width,String name)throws Exception {
        panel.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(width*panel.panelHeight()/TaskPanelView.WIDTH,View.MeasureSpec.EXACTLY));
        panel.layout(0,0,width,panel.getMeasuredHeight());
        if(!visible(pause)||!visible(stop))throw new AssertionError("Controls hidden in "+name);
        Bitmap bitmap=Bitmap.createBitmap(width,panel.getMeasuredHeight(),Bitmap.Config.ARGB_8888);
        panel.draw(new Canvas(bitmap));
        checkTextFits(panel);

        try(FileOutputStream out=new FileOutputStream(new File(context.getFilesDir(),name))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}finally{bitmap.recycle();}
    }
}
