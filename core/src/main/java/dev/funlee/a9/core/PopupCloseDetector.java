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
    public static int[] find(PixelFrame frame) {
        if(frame.width!=720||frame.height!=1280)return null;
        for(boolean pink:new boolean[]{true,false})for(int radius:new int[]{11,8})for(int y=65;y<220;y++)for(int x=950;x<1190;x++) {
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
