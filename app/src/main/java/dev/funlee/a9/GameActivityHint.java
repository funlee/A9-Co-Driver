package dev.funlee.a9;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import java.util.function.BooleanSupplier;
import dev.funlee.a9.core.ActivityHintPolicy;

/** Non-touchable operation hints and immediate task notices share one game overlay. */
final class GameActivityHint {
    private final ActivityHintPolicy policy=new ActivityHintPolicy();
    private final Context context;
    private final Handler main;
    private final WindowManager windows;
    private final BooleanSupplier canShow;
    private TextView view;
    private boolean waiting;
    private final Runnable showPending=this::showPending;
    private final Runnable dismiss=this::hide;
    private static final class FrameArea {
        final Rect bounds;
        final long until;
        FrameArea(Rect bounds,long until){this.bounds=bounds;this.until=until;}
    }
    private volatile FrameArea frameArea;

    GameActivityHint(Context context,Handler main,BooleanSupplier canShow){
        this.context=context;this.main=main;this.canShow=canShow;
        windows=context.getSystemService(WindowManager.class);
    }

    void offer(String message){
        if(waiting)return;
        policy.offer(message);
        main.removeCallbacks(showPending);
        long delay=policy.delay(SystemClock.elapsedRealtime());
        if(delay==0)showPending();else main.postDelayed(showPending,delay);
    }

    private void showPending(){
        if(!canShow.getAsBoolean())return;
        long now=SystemClock.elapsedRealtime();
        long delay=policy.delay(now);
        if(delay>0){main.postDelayed(showPending,delay);return;}
        String message=policy.take(now);
        if(!message.isEmpty())show(message,ActivityHintPolicy.SHOW_MS,2);
    }

    boolean showNow(String message,long duration){
        clear();
        policy.reset(SystemClock.elapsedRealtime()+duration);
        show(message,duration,4);
        return view!=null;
    }

    void updateWait(String message){
        if(message==null||!canShow.getAsBoolean()){
            if(waiting)clear();
            return;
        }
        if(waiting&&view!=null){view.setText(message);return;}
        clear();
        waiting=true;
        show(message,0,2);
    }

    private void show(String message,long duration,int maxLines){
        main.removeCallbacks(dismiss);
        hide();
        TextView label=new TextView(context);
        label.setText(message);
        label.setTextColor(0xfff5fafb);
        label.setTextSize(15);
        label.setMaxLines(maxLines);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setMaxWidth(dp(280));
        label.setGravity(Gravity.CENTER);
        label.setPadding(dp(12),dp(6),dp(12),dp(6));
        label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        GradientDrawable background=new GradientDrawable();
        background.setColor(0xe6222e3a);
        background.setCornerRadius(dp(14));
        label.setBackground(background);
        WindowManager.LayoutParams params=new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity=(waiting?Gravity.TOP:Gravity.BOTTOM)|Gravity.CENTER_HORIZONTAL;
        params.y=dp(waiting?40:32);
        params.setTitle(waiting?"A9 游戏恢复等待":"A9 当前操作提示");
        label.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            int[] location=new int[2];
            label.getLocationOnScreen(location);
            frameArea=new FrameArea(new Rect(location[0],location[1],
                    location[0]+label.getWidth(),location[1]+label.getHeight()),Long.MAX_VALUE);
        });
        try{
            windows.addView(label,params);
            view=label;
            if(duration>0)main.postDelayed(dismiss,duration);
        }catch(WindowManager.BadTokenException|SecurityException e){
            SessionState.log("无法显示当前操作提示："+e.getMessage());
        }
    }

    private void hide(){
        if(view==null)return;
        windows.removeViewImmediate(view);
        view=null;
        FrameArea area=frameArea;
        // The projection can still hand out the most recent frame while the window is removed.
        if(area!=null)frameArea=new FrameArea(area.bounds,SystemClock.elapsedRealtime()+100);
    }

    void clear(){
        waiting=false;
        main.removeCallbacks(showPending);
        main.removeCallbacks(dismiss);
        hide();
        policy.reset(0);
    }

    Rect boundsAt(long frameTime){
        FrameArea area=frameArea;
        return area!=null&&frameTime<=area.until?area.bounds:null;
    }

    private int dp(int value){return Math.round(context.getResources().getDisplayMetrics().density*value);}
}
