package dev.funlee.a9.core;

import java.util.*;

/** Finds complete bright reward cards only after the caller confirms the free-choice reward scene. */
public final class DynamicRewards {
    public static List<int[]> cards(PixelFrame frame){
        List<int[]> out=new ArrayList<>();if(frame.width!=720||frame.height!=1280)return out;
        int start=-1,last=-1;
        for(int x=0;x<1280;x++){
            int bright=0;
            for(int y=210;y<=490;y+=2){
                int c=frame.pixel(719-y,x),r=c>>16&255,g=c>>8&255,b=c&255;
                int max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b));
                if((max>145&&max-min>35)||min>135)bright++;
            }
            if(bright>=34){if(start<0)start=x;last=x;}
            if(start>=0&&(x-last>8||x==1279)){
                int width=last-start+1;
                if(start>35&&last<1245&&width>=90&&width<=270)out.add(new int[]{(start+last)/2,350,start,last});
                start=-1;last=-1;
            }
        }
        return out;
    }
}
