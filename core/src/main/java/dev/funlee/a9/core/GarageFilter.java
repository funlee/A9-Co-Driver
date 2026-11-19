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
        return read(f, false);
    }
    public static Map<String,Object> read(PixelFrame f, boolean wide) {
        return read(f,wide?20.0/9:16.0/9);
    }
    public static Map<String,Object> read(PixelFrame f,double aspect) {
        boolean wide=aspect>2.07,layout18By9=aspect>=2.0&&!wide;
        boolean panel=f.width==720&&f.height==1280&&fraction(f,290,570,315,595,0)>.85
            &&fraction(f,1190,55,1260,110,1)>.2;
        if(!panel)return wide ? Map.of("present",false,"wide",true) : Map.of("present",false);
        int arrowTop=wide?317:layout18By9?290:265;
        double down=fraction(f,320,arrowTop,338,arrowTop+33,2),up=fraction(f,343,arrowTop,361,arrowTop+33,2);
        int descending=down>.10&&up<.06?1:up>.10&&down<.06?0:-1;
        if(wide)return Map.of("present",true,"wide",true,"owned",box(f,226),"rank",box(f,481),"descending",descending);
        if(layout18By9)return Map.of("present",true,"layout18By9",true,"owned",box(f,215),"rank",box(f,447),"descending",descending);
        return Map.of("present",true,"owned",box(f,197),"rank",box(f,404),"descending",descending);
    }
}
