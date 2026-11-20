package dev.funlee.a9;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import dev.funlee.a9.core.TaskStats;

/** Full race statistics with an always-available compact activity and pause/stop bar outside races. */
@android.annotation.SuppressLint("ViewConstructor")
final class TaskPanelView extends LinearLayout {
    static final int WIDTH=200,HEIGHT=161,COMPACT_HEIGHT=64;
    private static final int RACE_STATS_WIDTH=232,RACE_STATS_HEIGHT=164;
    private static final int MATCHING_HEIGHT=33;
    private final TextView title;
    private final LinearLayout information;
    private final LinearLayout actions;
    private boolean compact;
    private final TextView[] rows=new TextView[6];
    private final float scale;
    private TaskPanelView(Context context,TextView heading,Button pause,Button stop){
        super(context);
        android.util.DisplayMetrics metrics=getResources().getDisplayMetrics();
        scale=Math.max(metrics.widthPixels,metrics.heightPixels)/1280f;
        setOrientation(VERTICAL);setPadding(px(7),px(4),px(7),px(4));
        GradientDrawable background=new GradientDrawable();background.setColor(0xe6222e3a);background.setCornerRadius(px(10));setBackground(background);
        title=heading;
        title.setTextColor(0xff73e0c1);
        title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        title.setSingleLine(true);
        title.setHorizontallyScrolling(false);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setAutoSizeTextTypeUniformWithConfiguration(px(9),px(12),1,TypedValue.COMPLEX_UNIT_PX);
        title.setIncludeFontPadding(false);
        title.setGravity(Gravity.CENTER_VERTICAL);
        addView(title,new LayoutParams(-1,px(18)));
        information=new LinearLayout(context);information.setOrientation(VERTICAL);
        for(int i=0;i<rows.length;i++){
            TextView row=new TextView(context);row.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));row.setTextColor(0xfff0f4f8);row.setSingleLine(true);row.setHorizontallyScrolling(false);row.setIncludeFontPadding(false);row.setGravity(Gravity.CENTER_VERTICAL);
            row.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.setAutoSizeTextTypeUniformWithConfiguration(px(8),px(11),1,TypedValue.COMPLEX_UNIT_PX);
            information.addView(row,new LayoutParams(-1,0,1));rows[i]=row;
        }
        LayoutParams info=new LayoutParams(-1,0,1);addView(information,info);
        actions=new LinearLayout(context);style(pause,false);style(stop,true);
        LayoutParams left=new LayoutParams(0,-1,1),right=new LayoutParams(0,-1,1);right.leftMargin=px(8);
        actions.addView(pause,left);actions.addView(stop,right);
        LayoutParams controls=new LayoutParams(-1,px(34));
        controls.topMargin=px(4);
        addView(actions,controls);
    }
    static TaskPanelView create(Context context,TextView heading,Button pause,Button stop){return new TaskPanelView(context,heading,pause,stop);}
    void setCompact(boolean value){
        compact=value;title.setVisibility(VISIBLE);information.setVisibility(value?GONE:VISIBLE);
    }
    void showRaceStatsOnly(){
        actions.setVisibility(GONE);
        title.getLayoutParams().height=px(25);
        title.setAutoSizeTextTypeUniformWithConfiguration(px(13),px(18),1,TypedValue.COMPLEX_UNIT_PX);
        for(TextView row:rows)
            row.setAutoSizeTextTypeUniformWithConfiguration(px(11),px(17),1,TypedValue.COMPLEX_UNIT_PX);
    }
    int panelWidth(){return actions.getVisibility()==GONE?RACE_STATS_WIDTH:WIDTH;}
    int panelHeight(){
        if(actions.getVisibility()==GONE)return compact?MATCHING_HEIGHT:RACE_STATS_HEIGHT;
        return compact?COMPACT_HEIGHT:HEIGHT;
    }
    private int px(float value){return Math.max(1,Math.round(scale*value));}
    private void style(Button button,boolean danger){
        button.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX,12*scale);button.setIncludeFontPadding(false);button.setTextColor(danger?0xffffb7b0:0xffe8f8f3);button.setAllCaps(false);button.setMinWidth(0);button.setMinimumWidth(0);button.setMinHeight(0);button.setMinimumHeight(0);button.setPadding(px(4),0,px(4),0);button.setElevation(0);button.setStateListAnimator(null);
        GradientDrawable bg=new GradientDrawable();bg.setColor(danger?0xff52383d:0xff354b52);bg.setCornerRadius(px(6));button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x3073e0c1),bg,null));
    }
    void update(TaskStats stats,String phase,boolean inspect,String diagnostic,boolean avoiding){
        title.setText(inspect?"● 屏幕诊断":compact?"● "+stats.currentActivity(phase):"● 比赛统计");
        title.setContentDescription(title.getText());
        String[] lines=(inspect?diagnostic:stats.overlayFullText()).split("\n",-1);
        for(int i=0;i<rows.length;i++){
            String line=i<lines.length?lines[i]:"";rows[i].setVisibility(i<lines.length?VISIBLE:GONE);rows[i].setText(line);
            rows[i].setTextColor(line.contains("兜底")&&stats.fallbackCount()>0||line.contains("未识别")?0xffffd28a:0xfff0f4f8);
        }
    }
}
