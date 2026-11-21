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
        Map<String,Object> result=new java.util.LinkedHashMap<>();
        result.put("present",true);
        if(wide)result.put("wide",true);
        if(layout18By9)result.put("layout18By9",true);
        result.put("descending",descending);
        locateBox(f,result,"owned",wide?236:layout18By9?225:207);
        locateBox(f,result,"rank",wide?491:layout18By9?457:414);
        return result;
    }

    private static void locateBox(PixelFrame frame,Map<String,Object> result,String key,int expectedY){
        // Find the contiguous interior of the checkbox. The panel background is blue.
        int best=0,bestX=0,bestY=0,state=-1;
        for(int x=324;x<=354;x+=5){
            int top=-1;
            for(int y=expectedY-48;y<=expectedY+48;y++){
                boolean inside=y<expectedY+48 && (fraction(frame,x-3,y,x+3,y,0)>.55 ||
                        fraction(frame,x-3,y,x+3,y,3)>.8);
                if(inside){if(top<0)top=y;continue;}
                int height=top<0?0:y-top;
                if(height>=14 && height<=54 && height>best){
                    int cy=(top+y)/2;
                    double on=fraction(frame,x-5,cy-5,x+5,cy+5,0);
                    double off=fraction(frame,x-5,cy-5,x+5,cy+5,3);
                    if(on>.45||off>.85){
                        best=height;bestX=x;bestY=cy;state=on>.45?1:0;
                    }
                }
                top=-1;
            }
        }
        result.put(key,best==0?box(frame,expectedY-10):state);
        if(best>0)result.put(key+"Center",Map.of("x",bestX,"y",bestY));
    }
}
