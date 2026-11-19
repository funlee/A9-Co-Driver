package dev.funlee.a9.core;

/** Detects the pink/white X on the upper-right of current 16:9 game promotions. */
public final class PopupCloseDetector {
    private static boolean ink(int rgb,boolean pink) {
        int r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;
        return pink?r>=220&&g<=65&&b>=65&&b<=190:r>=235&&g>=235&&b>=235;
    }
    private static boolean at(PixelFrame frame,int x,int y,boolean pink) {
        return ink(frame.pixel(719-y,x),pink);
    }
    private static boolean whiteNear(PixelFrame frame,int x,int y){
        for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)if(at(frame,x+dx,y+dy,false))return true;
        return false;
    }
    public static int[] find(PixelFrame frame) {
        return find(frame,65,220);
    }
    public static int[] findSeasonPassUnlock(PixelFrame frame) {
        if(frame.width!=720||frame.height!=1280)return null;
        for(int y=55;y<135;y++)for(int x=950;x<1190;x++){
            if(!at(frame,x,y,false)||!at(frame,x-18,y,true)||!at(frame,x+18,y,true)||
                    !at(frame,x,y-21,true)||!at(frame,x,y+21,true))continue;
            for(int rise:new int[]{5,7}){
                if(whiteNear(frame,x-5,y-rise)&&whiteNear(frame,x+5,y+rise)&&
                        whiteNear(frame,x-5,y+rise)&&whiteNear(frame,x+5,y-rise))return new int[]{x,y};
            }
        }
        return null;
    }
    public static int[] findLimitedSale(PixelFrame frame) {
        if(frame.width!=720||frame.height!=1280)return null;
        for(int y=65;y<170;y++)for(int x=950;x<1150;x++){
            if(!at(frame,x,y,true))continue;
            boolean cross=true;
            for(int dx:new int[]{-1,1})for(int dy:new int[]{-1,1})
                cross&=at(frame,x+dx*3,y+dy*3,true)&&at(frame,x+dx*5,y+dy*5,true);
            if(cross&&!at(frame,x-5,y,true)&&!at(frame,x+5,y,true)&&
                    !at(frame,x,y-5,true)&&!at(frame,x,y+5,true))return new int[]{x,y};
        }
        return find(frame,65,170);
    }
    public static int[] findDialog(PixelFrame frame){return find(frame,100,350);}
    public static int[] findDialog(PixelFrame frame,boolean wide){
        if(wide&&frame.width==720&&frame.height==1280){
            for(int y=175;y<265;y++)for(int x=1010;x<1160;x++){
                if(!at(frame,x,y,false))continue;
                boolean cross=true;
                for(int dx:new int[]{-1,1})for(int dy:new int[]{-1,1})
                    cross&=whiteNear(frame,x+dx*5,y+dy*8)&&whiteNear(frame,x+dx*9,y+dy*14);
                if(cross&&at(frame,x+18,y,true)&&at(frame,x,y-18,true)&&at(frame,x,y+18,true))return new int[]{x,y};
            }
        }
        return findDialog(frame);
    }
    private static int[] find(PixelFrame frame,int top,int bottom) {
        if(frame.width!=720||frame.height!=1280)return null;
        for(boolean pink:new boolean[]{true,false})for(int radius:new int[]{11,8})for(int y=top;y<bottom;y++)for(int x=950;x<1190;x++) {
            if(!at(frame,x,y,pink))continue;
            boolean cross=true;
            for(int d:new int[]{4,radius})for(int dx:new int[]{-1,1})for(int dy:new int[]{-1,1})
                cross&=at(frame,x+dx*d,y+dy*d,pink);
            if(!cross)continue;
            if(at(frame,x-radius,y,pink)||at(frame,x+radius,y,pink)||at(frame,x,y-radius,pink)||at(frame,x,y+radius,pink))continue;
            return new int[]{x,y};
        }
        return null;
    }
}
