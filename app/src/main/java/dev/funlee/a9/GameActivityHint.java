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

/** A non-touchable game message shown once when work continues on a still screen. */
final class GameActivityHint {
    static final long STILL_MS=8000;
    private static final long SHOW_MS=1000;
    private final Context context;
    private final Handler main;
    private final WindowManager windows;
    private final BooleanSupplier canShow;
    private TextView view;
    private String last="",pending="";
    private long pendingSince,shownForScreen=-1;
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
        if(message.isEmpty()||message.equals("nil")||message.equals(last)||!canShow.getAsBoolean())return;
        last=message;
        if(pending.isEmpty()){
            pendingSince=SystemClock.elapsedRealtime();
            main.postDelayed(showPending,STILL_MS);
        }
        pending=message;
    }

    private void showPending(){
        if(!canShow.getAsBoolean()||pending.isEmpty()){pending="";return;}
        long changed=SessionState.lastScreenChange;
        long wait=Math.max(changed,pendingSince)+STILL_MS-SystemClock.elapsedRealtime();
        if(wait>0){main.postDelayed(showPending,wait);return;}
        if(shownForScreen==changed){pending="";return;}
        shownForScreen=changed;
        TextView label=new TextView(context);
        label.setText(pending);
        pending="";
        label.setTextColor(0xfff5fafb);
        label.setTextSize(15);
        label.setMaxLines(2);
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
        params.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;
        params.y=dp(32);
        params.setTitle("A9 当前操作提示");
        label.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            int[] location=new int[2];
            label.getLocationOnScreen(location);
            frameArea=new FrameArea(new Rect(location[0],location[1],
                    location[0]+label.getWidth(),location[1]+label.getHeight()),Long.MAX_VALUE);
        });
        try{
            windows.addView(label,params);
            view=label;
            main.postDelayed(dismiss,SHOW_MS);
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
        main.removeCallbacks(showPending);
        main.removeCallbacks(dismiss);
        hide();
        pending=last="";
        pendingSince=0;shownForScreen=-1;
    }

    Rect boundsAt(long frameTime){
        FrameArea area=frameArea;
        return area!=null&&frameTime<=area.until?area.bounds:null;
    }

    private int dp(int value){return Math.round(context.getResources().getDisplayMetrics().density*value);}
}
