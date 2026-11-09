package dev.funlee.a9;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import dev.funlee.a9.core.TaskStats;

/** Compact dashboard, expandable diagnostics and an edge-docked single-line state. */
@android.annotation.SuppressLint("ViewConstructor")
final class TaskPanelView extends LinearLayout {
    private final TextView title,body;
    private final Button detail;
    private final ScrollView scroll;
    private final LinearLayout actions;
    private boolean collapsed,expanded,temporarilyCompact;
    private TaskPanelView(Context context,TextView heading,Button pause,Button stop){
        super(context);setOrientation(VERTICAL);setPadding(dp(10),dp(6),dp(10),dp(6));
        GradientDrawable background=new GradientDrawable();background.setColor(0xe6222e3a);background.setCornerRadius(dp(10));setBackground(background);
        LinearLayout header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);title=heading;title.setTextColor(0xff73e0c1);title.setTextSize(12);title.setTypeface(Typeface.DEFAULT_BOLD);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);title.setMinHeight(dp(36));header.addView(title,new LayoutParams(0,-2,1));
        detail=new Button(context);style(detail,false);detail.setTextSize(10);detail.setMinWidth(dp(42));detail.setMinimumWidth(dp(42));detail.setText("详情");detail.setOnClickListener(v->{if(collapsed)collapsed=false;else expanded=!expanded;apply();});header.addView(detail,new LayoutParams(-2,dp(36)));addView(header);
        body=new TextView(context);body.setTextSize(10);body.setTextColor(0xfff0f4f8);body.setLineSpacing(dp(3),1);body.setPadding(0,dp(2),0,dp(5));
        scroll=new ScrollView(context);scroll.setVerticalScrollBarEnabled(false);scroll.addView(body);addView(scroll,new LayoutParams(-1,-2));
        actions=new LinearLayout(context);style(pause,false);style(stop,true);LayoutParams left=new LayoutParams(0,dp(40),1),right=new LayoutParams(0,dp(40),1);right.leftMargin=dp(8);actions.addView(pause,left);actions.addView(stop,right);addView(actions);
    }
    static TaskPanelView create(Context context,TextView heading,Button pause,Button stop){return new TaskPanelView(context,heading,pause,stop);}
    private int dp(int value){return (int)(getResources().getDisplayMetrics().density*value+.5f);}
    private void style(Button button,boolean danger){
        button.setTextSize(11);button.setTextColor(danger?0xffffb7b0:0xffe8f8f3);button.setAllCaps(false);button.setMinWidth(0);button.setMinimumWidth(0);button.setMinHeight(0);button.setMinimumHeight(0);button.setPadding(dp(6),0,dp(6),0);button.setElevation(0);button.setStateListAnimator(null);
        GradientDrawable bg=new GradientDrawable();bg.setColor(danger?0xff52383d:0xff354b52);bg.setCornerRadius(dp(6));button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x3073e0c1),bg,null));
    }
    private TaskStats stats;private String phase="",diagnostic="";private boolean inspect;
    void update(TaskStats stats,String phase,boolean inspect,String diagnostic,boolean temporarilyCompact){this.stats=stats;this.phase=phase;this.inspect=inspect;this.diagnostic=diagnostic;this.temporarilyCompact=temporarilyCompact;apply();}
    void toggleCollapsed(){if(!temporarilyCompact){collapsed=!collapsed;apply();}}
    boolean collapsed(){return collapsed||temporarilyCompact;}
    private void apply(){
        if(stats==null)return;boolean small=collapsed(),details=expanded&&!small;
        String state=stats.phase(phase);title.setText(inspect?"● 屏幕诊断":"● "+state);title.setTextColor(state.equals("任务停止")?0xffffb7b0:state.equals("已暂停")||state.equals("恢复游戏")?0xffffd28a:0xff73e0c1);detail.setEnabled(!temporarilyCompact);detail.setText(temporarilyCompact?"避让":small?"展开":details?"精简":"详情");
        scroll.setVisibility(small?GONE:VISIBLE);actions.setVisibility(small&&!temporarilyCompact?GONE:VISIBLE);
        String text=inspect?diagnostic:details?stats.panel(phase):stats.compact();if(!text.contentEquals(body.getText())){
            android.text.SpannableString styled=new android.text.SpannableString(text);
            for(String marker:new String[]{"左划兜底","收益未识别"}){int start=text.indexOf(marker);if(start>=0){int end=text.indexOf('\n',start);styled.setSpan(new android.text.style.ForegroundColorSpan(0xffffd28a),start,end<0?text.length():end,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}}
            body.setText(styled);
        }
        scroll.setVerticalScrollBarEnabled(details);LayoutParams p=(LayoutParams)scroll.getLayoutParams();p.height=details?dp(190):-2;scroll.setLayoutParams(p);
    }
}
