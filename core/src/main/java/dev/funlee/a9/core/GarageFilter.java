package dev.funlee.a9.core;

import java.util.Map;

/** Current CN filter panel. Coordinates below are upright 1280 x 720; input is rotated 720 x 1280. */
public final class GarageFilter {
    private GarageFilter() {}
    private static double fraction(PixelFrame f,int x1,int y1,int x2,int y2,int kind) {
        int count=0,total=0;
        for(int y=y1;y<=y2;y++)for(int x=x1;x<=x2;x++){
            int c=f.pixel(719-y,x),r=(c>>16)&255,g=(c>>8)&255,b=c&255;
            boolean match=kind==0?r>150&&g>200&&b<85:kind==1?r>220&&g<60&&b>50:kind==2?r>215&&g>215&&b>215:r<30&&g<35&&b<65;
            if(match)count++;total++;
        }
        return count/(double)total;
    }
    private static int box(PixelFrame f,int y) {
        if(fraction(f,324,y,353,y+20,0)>.45)return 1;
        if(fraction(f,324,y,353,y+20,3)>.85)return 0;
        return -1;
    }
    public static Map<String,Object> read(PixelFrame f) {
        boolean panel=f.width==720&&f.height==1280&&fraction(f,90,570,280,595,0)>.85
            &&fraction(f,1190,55,1260,110,1)>.2;
        if(!panel)return Map.of("present",false);
        double down=fraction(f,320,265,338,298,2),up=fraction(f,343,265,361,298,2);
        int descending=down>.10&&up<.06?1:up>.10&&down<.06?0:-1;
        return Map.of("present",true,"owned",box(f,197),"rank",box(f,404),"descending",descending);
    }
}
