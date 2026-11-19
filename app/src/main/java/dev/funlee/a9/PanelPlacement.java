package dev.funlee.a9;

import android.graphics.Point;
import android.os.Build;
import android.view.*;

/** Anchor the race panel to the physical bottom-left, rather than the cutout's inset frame. */
final class PanelPlacement {
    static void apply(WindowManager.LayoutParams params,Point screen){
        apply(params,screen,TaskPanelView.HEIGHT);
    }
    static void apply(WindowManager.LayoutParams params,Point screen,int height){
        apply(params,screen,TaskPanelView.WIDTH,height);
    }
    static void apply(WindowManager.LayoutParams params,Point screen,int width,int height){
        int scale=Math.max(screen.x,screen.y);
        params.width=scale*width/1280;
        params.height=scale*height/1280;
        params.gravity=Gravity.BOTTOM|Gravity.LEFT;
        params.x=8;params.y=8;
        params.flags|=WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        if(Build.VERSION.SDK_INT>=30){
            params.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            // Keep visible navigation controls clear; a side cutout must not shift the whole panel.
            params.setFitInsetsTypes(WindowInsets.Type.navigationBars());
        }else if(Build.VERSION.SDK_INT>=28){
            params.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
    }
}
