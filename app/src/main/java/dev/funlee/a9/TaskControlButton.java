package dev.funlee.a9;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ImageButton;
import java.util.Collections;
import java.util.List;

/** Small game overlay. A tap opens the task menu; a drag docks it on the nearest edge. */
final class TaskControlButton extends ImageButton {
    private final WindowManager windows;
    private final WindowManager.LayoutParams params;
    private final SharedPreferences placement;
    private final int inset,slop;
    private final Point travel=new Point();
    private final Rect margins=new Rect();
    private List<Rect> cutouts=Collections.emptyList();
    private float downX,downY;
    private int startX,startY;
    private boolean dragging;

    TaskControlButton(Context context){
        super(context);
        windows=context.getSystemService(WindowManager.class);
        placement=context.getSharedPreferences("task-control-position",Context.MODE_PRIVATE);
        float density=getResources().getDisplayMetrics().density;
        int hitPadding=Math.round(2*density);
        int size=Math.round(26*density)+2*hitPadding;
        inset=Math.round(3*density);
        slop=ViewConfiguration.get(context).getScaledTouchSlop();
        setImageResource(R.drawable.ic_steering_mark);
        setScaleType(ScaleType.FIT_CENTER);
        setContentDescription("A9 任务控制");
        GradientDrawable background=new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(0xeeecf6f3);
        background.setStroke(Math.max(1,Math.round(density)),0xff087f73);
        setBackground(new InsetDrawable(background,hitPadding));
        setPadding(inset,inset,inset,inset);

        params=new WindowManager.LayoutParams(size,size,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.LEFT;
        if(Build.VERSION.SDK_INT>=30){
            params.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            params.setFitInsetsTypes(WindowInsets.Type.navigationBars());
        }else if(Build.VERSION.SDK_INT>=29){
            params.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        params.setTitle("A9 游戏控制按钮");
    }

    void show(){
        margins.set(inset,inset,inset,inset);
        if(Build.VERSION.SDK_INT>=30){
            var metrics=windows.getCurrentWindowMetrics();
            var nav=metrics.getWindowInsets().getInsets(WindowInsets.Type.navigationBars());
            travel.set(metrics.getBounds().width()-nav.left-nav.right,metrics.getBounds().height()-nav.top-nav.bottom);
            var gestures=metrics.getWindowInsets().getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemGestures()|WindowInsets.Type.mandatorySystemGestures());
            // Keep the center grabbable even when Android claims an inward swipe
            // to reveal system bars or navigate back in immersive mode.
            keepCenterOutsideGestures(gestures.left-nav.left,gestures.top-nav.top,
                    gestures.right-nav.right,gestures.bottom-nav.bottom);
            readCutouts(metrics.getWindowInsets().getDisplayCutout(),nav.left,nav.top);
        }else{
            windows.getDefaultDisplay().getRealSize(travel);
            if(Build.VERSION.SDK_INT==29){
                // Android 10 exposes gesture insets only after a window is attached.
                // Start far enough in for a center grab on gesture-navigation devices.
                int edge=Math.round(32*getResources().getDisplayMetrics().density);
                keepCenterOutsideGestures(edge,edge,edge,edge);
                readCutouts(windows.getDefaultDisplay().getCutout(),0,0);
            }
        }
        travel.x=Math.max(0,travel.x-params.width-margins.left-margins.right);
        travel.y=Math.max(0,travel.y-params.height-margins.top-margins.bottom);
        restorePosition();
        windows.addView(this,params);
    }
    private void keepCenterOutsideGestures(int left,int top,int right,int bottom){
        int radius=params.width/2;
        margins.set(inset+Math.max(0,left-radius),inset+Math.max(0,top-radius),
                inset+Math.max(0,right-radius),inset+Math.max(0,bottom-radius));
    }
    private void readCutouts(DisplayCutout cutout,int left,int top){
        if(cutout==null)return;
        cutouts=cutout.getBoundingRects();
        for(Rect rect:cutouts)rect.offset(-left,-top);
    }
    private void avoidCutout(boolean sideEdge){
        for(Rect hole:cutouts){
            if(!hole.intersects(params.x,params.y,params.x+params.width,params.y+params.height))continue;
            // Slide along the chosen edge so the whole touch target stays reachable.
            if(sideEdge)params.y=clearPosition(params.y,params.height,hole.top,hole.bottom,margins.top,travel.y);
            else params.x=clearPosition(params.x,params.width,hole.left,hole.right,margins.left,travel.x);
        }
    }
    private int clearPosition(int position,int size,int from,int to,int min,int span){
        int before=from-size-inset,after=to+inset;
        if(before>=min&&(after>min+span||position-before<=after-position))return before;
        return Math.min(min+span,after);
    }
    private void restorePosition(){
        params.x=margins.left+Math.round(travel.x*placement.getFloat("x",1f));
        params.y=margins.top+Math.round(travel.y*placement.getFloat("y",.25f));
        avoidCutout(params.x==margins.left||params.x==margins.left+travel.x);
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);
        if(Build.VERSION.SDK_INT>=29)setSystemGestureExclusionRects(Collections.singletonList(new Rect(0,0,w,h)));
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                downX=event.getRawX();downY=event.getRawY();
                startX=params.x;startY=params.y;dragging=false;
                setPressed(true);
                return true;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                float dx=event.getRawX()-downX,dy=event.getRawY()-downY;
                if(!dragging&&dx*dx+dy*dy>slop*slop){dragging=true;setPressed(false);}
                boolean released=event.getActionMasked()==MotionEvent.ACTION_UP;
                if(dragging){
                    int x=Math.max(0,Math.min(travel.x,startX-margins.left+Math.round(dx)));
                    int y=Math.max(0,Math.min(travel.y,startY-margins.top+Math.round(dy)));
                    params.x=margins.left+x;params.y=margins.top+y;
                    if(released){
                        boolean sideEdge=Math.min(x,travel.x-x)<=Math.min(y,travel.y-y);
                        if(sideEdge)params.x=margins.left+(x<=travel.x/2?0:travel.x);
                        else params.y=margins.top+(y<=travel.y/2?0:travel.y);
                        avoidCutout(sideEdge);
                        placement.edit().putFloat("x",(float)(params.x-margins.left)/Math.max(1,travel.x))
                                .putFloat("y",(float)(params.y-margins.top)/Math.max(1,travel.y)).apply();
                    }
                    windows.updateViewLayout(this,params);
                }else if(released){
                    setPressed(false);
                    performClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                setPressed(false);
                if(dragging){restorePosition();windows.updateViewLayout(this,params);}
                return true;
            default:
                return true;
        }
    }
    @Override public boolean performClick(){return super.performClick();}
}
