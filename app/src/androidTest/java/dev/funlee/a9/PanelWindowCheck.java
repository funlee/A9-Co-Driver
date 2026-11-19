package dev.funlee.a9;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

/** Real overlay placement, including cutout insets; no automation service or race inputs. */
final class PanelWindowCheck {
    static void run(Instrumentation test,Bundle report)throws Exception {
        WindowManager wm=(WindowManager)test.getTargetContext().getSystemService(Context.WINDOW_SERVICE);
        Point screen=new Point();wm.getDefaultDisplay().getRealSize(screen);
        TaskPanelView[] panel={null};WindowManager.LayoutParams params=new WindowManager.LayoutParams();
        params.type=WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        params.flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        params.format=PixelFormat.TRANSLUCENT;StringBuilder positions=new StringBuilder();
        try{
            test.runOnMainSync(()->{
                Context c=new ContextThemeWrapper(test.getTargetContext(),R.style.AppTheme);
                Button pause=new Button(c),stop=new Button(c);pause.setText("暂停");stop.setText("停止");
                panel[0]=TaskPanelView.create(c,new TextView(c),pause,stop);
                PanelPlacement.apply(params,screen);wm.addView(panel[0],params);
            });
            for(boolean compact:new boolean[]{false,true,false}){
                test.runOnMainSync(()->{panel[0].setCompact(compact);PanelPlacement.apply(params,screen,panel[0].panelHeight());wm.updateViewLayout(panel[0],params);});
                Thread.sleep(350);int[] xy=new int[2];int[] size=new int[2];
                test.runOnMainSync(()->{panel[0].getLocationOnScreen(xy);size[0]=panel[0].getWidth();size[1]=panel[0].getHeight();});
                if(Math.abs(xy[0]-8)>2)throw new AssertionError("Cutout shifted panel: x="+xy[0]);
                int bottomGap=screen.y-xy[1]-size[1];
                if(bottomGap<6||bottomGap>80)throw new AssertionError("Panel not at bottom: gap="+bottomGap);
                if(size[1]!=params.height)throw new AssertionError("Wrong panel height");
                positions.append(compact?"compact":"race").append(": ").append(xy[0]).append(',').append(xy[1]).append(' ').append(size[0]).append('x').append(size[1]).append(';');
            }
        }finally{if(panel[0]!=null)test.runOnMainSync(()->wm.removeViewImmediate(panel[0]));}
        report.putString("result","PASS: "+positions);
    }
}
