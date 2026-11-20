package dev.funlee.a9.core;

/** Locates a close glyph within a detected panel; older full-screen layouts have separate searches. */
public final class PopupCloseDetector {
    static int[] findInPanel(PixelFrame frame,ModalPopup.Panel panel,double aspect) {
        double rise=Math.max(.9,Math.min(1.4,aspect*9/16));
        int left=1280,right=-1,top=720,bottom=-1;
        for(int y=panel.top+18;y<=panel.top+79;y++)for(int x=panel.right-80;x<=panel.right-17;x++){
            int color=ModalPopup.pixel(frame,x,y);
            if(!panelCross(frame,x,y,color,rise))continue;
            left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
            if(right-left>8||bottom-top>8)return null;
        }
        return right<0?null:new int[]{(left+right)/2,(top+bottom)/2};
    }

    private static boolean panelCross(PixelFrame frame,int x,int y,int color,double rise) {
        for(int radius=7;radius<=15;radius+=2){
            int outerY=(int)Math.round(radius*rise);
            if(similar(frame,x-radius,y,color)||similar(frame,x+radius,y,color)||
                    similar(frame,x,y-outerY,color)||similar(frame,x,y+outerY,color))continue;
            boolean arms=true;
            for(int d=3;d<=radius&&arms;d+=2){
                int dy=(int)Math.round(d*rise);
                arms=stroke(frame,x-d,y-dy,color)&&stroke(frame,x+d,y-dy,color)&&
                        stroke(frame,x-d,y+dy,color)&&stroke(frame,x+d,y+dy,color);
            }
            if(arms)return true;
        }
        return false;
    }

    private static boolean similar(PixelFrame frame,int x,int y,int color) {
        return ModalPopup.distance(ModalPopup.pixel(frame,x,y),color)<=80;
    }

    private static boolean stroke(PixelFrame frame,int x,int y,int color) {
        return similar(frame,x,y,color)||similar(frame,x,y-1,color)||similar(frame,x,y+1,color);
    }

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
        return findPinkSquare(frame,950,1190,55,135);
    }
    public static int[] findDailyGoals(PixelFrame frame) {
        return findPinkSquare(frame,1110,1246,70,165);
    }
    private static int[] findPinkSquare(PixelFrame frame,int left,int right,int top,int bottom) {
        if(frame.width!=720||frame.height!=1280)return null;
        for(int y=top;y<bottom;y++)for(int x=left;x<right;x++){
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
