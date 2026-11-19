package dev.funlee.a9;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.function.Consumer;

/** A separate window so game controls stay available when the race panel hides. */
final class TaskControlMenu {
    private final Context context;
    private final WindowManager windows;
    private final LinearLayout panel;

    TaskControlMenu(Context context,boolean inspecting,boolean paused,Consumer<String> action,Runnable dismiss){
        this.context=context;
        windows=context.getSystemService(WindowManager.class);
        panel=new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12),dp(10),dp(12),dp(8));
        GradientDrawable background=new GradientDrawable();
        background.setColor(0xff172536);
        background.setCornerRadius(dp(14));
        background.setStroke(dp(1),0xff4f687e);
        panel.setBackground(background);
        panel.setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        TextView title=new TextView(context);
        title.setText(inspecting?"屏幕诊断":"任务控制");
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        title.setPadding(dp(6),dp(4),dp(6),dp(4));
        panel.addView(title);
        addButton(inspecting?"取样":paused?"继续任务":"暂停任务",0xffa6ede5,
                ()->action.accept(inspecting?AutomationService.SAMPLE:AutomationService.PAUSE));
        addButton("终止任务",0xffffb6ae,()->action.accept(AutomationService.STOP));
        addButton("关闭菜单",0xffd9e3ec,dismiss);
    }

    private void addButton(String label,int color,Runnable action){
        Button button=new Button(context);
        button.setText(label);
        button.setTextColor(color);
        button.setBackgroundTintList(ColorStateList.valueOf(0xff34475b));
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setMinHeight(dp(48));
        button.setOnClickListener(v->action.run());
        panel.addView(button,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    void show(){
        WindowManager.LayoutParams params=new WindowManager.LayoutParams(dp(164),
                WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.RIGHT|Gravity.CENTER_VERTICAL;
        params.x=dp(64);
        params.setTitle("A9 任务控制");
        windows.addView(panel,params);
    }

    void hide(){
        if(panel.getParent()!=null)windows.removeViewImmediate(panel);
    }

    private int dp(int value){return Math.round(context.getResources().getDisplayMetrics().density*value);}
}
