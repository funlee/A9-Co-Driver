package dev.funlee.a9;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.*;
import android.widget.*;
import java.util.function.BiConsumer;

/** Native task dragging with an insertion marker and scrolling at viewport edges. */
final class TaskQueueView extends LinearLayout {
    private final ScrollView scroll;
    private final BiConsumer<Integer,Integer> moved;
    private final Paint marker=new Paint(Paint.ANTI_ALIAS_FLAG);
    private View dragged;
    private int source=-1,slot=-1,direction;
    private float viewportY;
    private final Runnable autoScroll=new Runnable(){
        @Override public void run(){
            if(dragged==null||direction==0)return;
            scroll.scrollBy(0,direction*dp(10));
            int[] listPos=new int[2],scrollPos=new int[2];
            getLocationOnScreen(listPos);scroll.getLocationOnScreen(scrollPos);
            locate(scrollPos[1]+viewportY-listPos[1]);
            postOnAnimation(this);
        }
    };
    TaskQueueView(Context context,ScrollView scroll,BiConsumer<Integer,Integer> moved){
        super(context);this.scroll=scroll;this.moved=moved;setOrientation(VERTICAL);
        marker.setColor(0xff087f73);marker.setStrokeWidth(dp(3));
        setOnDragListener((view,event)->onTaskDrag(event));
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    void addTask(View row,View handle,View details){
        addView(row);
        View.OnLongClickListener start=view->startDrag(row,view);
        handle.setOnLongClickListener(start);details.setOnLongClickListener(start);row.setOnLongClickListener(start);
        // The dedicated handle owns its gesture from DOWN, before ScrollView can
        // intercept it. A short click does nothing; movement starts a native drag.
        handle.setOnTouchListener(new View.OnTouchListener(){
            private float downX,downY;
            @Override public boolean onTouch(View view,MotionEvent event){
                switch(event.getActionMasked()){
                    case MotionEvent.ACTION_DOWN:
                        downX=event.getX();downY=event.getY();view.setPressed(true);
                        view.getParent().requestDisallowInterceptTouchEvent(true);return true;
                    case MotionEvent.ACTION_MOVE:
                        int slop=ViewConfiguration.get(getContext()).getScaledTouchSlop();
                        if(dragged==null&&Math.hypot(event.getX()-downX,event.getY()-downY)>slop){
                            view.setPressed(false);startDrag(row,view);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:
                        view.setPressed(false);view.getParent().requestDisallowInterceptTouchEvent(false);return true;
                    default:return true;
                }
            }
        });
    }
    private boolean startDrag(View row,View sourceView){
        dragged=row;source=indexOfChild(row);slot=source;
        boolean started=sourceView.startDragAndDrop(ClipData.newPlainText("task",""),new View.DragShadowBuilder(row),this,0);
        if(started)row.setAlpha(.35f);else clearDrag();
        return started;
    }
    private void locate(float y){
        slot=getChildCount();
        for(int i=0;i<getChildCount();i++){
            View child=getChildAt(i);
            if(y<child.getTop()+child.getHeight()/2f){slot=i;break;}
        }
        invalidate();
    }
    private boolean onTaskDrag(DragEvent event){
        if(event.getLocalState()!=this||dragged==null)return false;
        switch(event.getAction()){
            case DragEvent.ACTION_DRAG_STARTED:return true;
            case DragEvent.ACTION_DRAG_ENTERED:return true;
            case DragEvent.ACTION_DRAG_LOCATION:{
                locate(event.getY());
                int[] listPos=new int[2],scrollPos=new int[2];
                getLocationOnScreen(listPos);scroll.getLocationOnScreen(scrollPos);
                viewportY=listPos[1]+event.getY()-scrollPos[1];
                direction=viewportY<dp(56)?-1:viewportY>scroll.getHeight()-dp(56)?1:0;
                removeCallbacks(autoScroll);if(direction!=0)postOnAnimation(autoScroll);
                return true;
            }
            case DragEvent.ACTION_DRAG_EXITED:
                direction=0;removeCallbacks(autoScroll);slot=-1;invalidate();return true;
            case DragEvent.ACTION_DROP:{
                locate(event.getY());
                int from=source,to=slot>source?slot-1:slot;
                clearDrag();
                if(from!=to)moved.accept(from,to);
                return true;
            }
            case DragEvent.ACTION_DRAG_ENDED:clearDrag();return true;
            default:return true;
        }
    }
    private void clearDrag(){
        removeCallbacks(autoScroll);direction=0;slot=-1;source=-1;
        scroll.requestDisallowInterceptTouchEvent(false);
        if(dragged!=null)dragged.setAlpha(1);dragged=null;invalidate();
    }
    @Override protected void dispatchDraw(Canvas canvas){
        super.dispatchDraw(canvas);
        if(dragged==null||slot<0||getChildCount()==0)return;
        float y=slot==getChildCount()?getChildAt(slot-1).getBottom():getChildAt(slot).getTop();
        y=Math.max(dp(2),Math.min(getHeight()-dp(2),y));
        canvas.drawLine(dp(4),y,getWidth()-dp(4),y,marker);
    }
    @Override protected void onDetachedFromWindow(){clearDrag();super.onDetachedFromWindow();}
}
