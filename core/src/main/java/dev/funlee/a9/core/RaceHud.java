package dev.funlee.a9.core;

import java.util.Map;

/** The menu's solid white Back wedge can cover both legacy pause-button samples. */
public final class RaceHud {
    public static final String PATTERN="680|50|FFFFFF-101010,660|70|FFFFFF-101010";
    private static final int[][] HOME_WHITE_POINTS={{0,-13},{-13,2},{11,2},{-12,19},{10,19}};
    private RaceHud(){}

    public static boolean visible(PixelFrame frame){
        return matches(frame,PATTERN,1);
    }

    public static boolean visible(PixelFrame frame,boolean wide){
        return matches(frame,PATTERN,1,wide);
    }

    public static boolean matches(PixelFrame frame,String pattern,double similarity){
        return matches(frame,pattern,similarity,false);
    }

    public static boolean matches(PixelFrame frame,String pattern,double similarity,boolean wide){
        return !backWedge(frame)&&(widePause(frame,wide)||ColorPattern.matches(frame,pattern,similarity));
    }

    public static Map<String,Integer> homeButton(PixelFrame frame,boolean wide){
        if(!wide||!backWedge(frame))return Map.of();
        int sumX=0,sumY=0,count=0;
        for(int x=1170;x<=1245;x++)for(int y=25;y<=45;y++){
            boolean match=true;
            for(int[] point:HOME_WHITE_POINTS){
                int color=frame.pixel(719-(y+point[1]),x+point[0]);
                if(((color>>16)&255)<170||((color>>8)&255)<170||(color&255)<170){match=false;break;}
            }
            int door=frame.pixel(719-(y+19),x);
            if(match&&((door>>16)&255)<120&&((door>>8)&255)<120&&(door&255)<140){sumX+=x;sumY+=y;count++;}
        }
        return count==0?Map.of():Map.of("x",sumX/count,"y",sumY/count);
    }
    static boolean widePause(PixelFrame frame,boolean wide){
        if(!wide)return false;
        for(int y=50;y<=70;y+=10){
            if(!ColorPattern.color(frame.pixel(719-y,78),"FFFFFF-101010",1)||
                    !ColorPattern.color(frame.pixel(719-y,97),"FFFFFF-101010",1)||
                    ColorPattern.color(frame.pixel(719-y,88),"FFFFFF-252525",1))return false;
        }
        return true;
    }

    public static boolean backWedge(PixelFrame frame){
        int white=0;
        for(int x=29;x<=53;x+=3)for(int y=43;y<=76;y+=3){
            int rgb=frame.pixel(719-y,x);
            if(((rgb>>16)&255)>=236&&((rgb>>8)&255)>=236&&(rgb&255)>=236)white++;
        }
        return white>=82; // At least 82 of 108 samples are solid white.
    }
}
