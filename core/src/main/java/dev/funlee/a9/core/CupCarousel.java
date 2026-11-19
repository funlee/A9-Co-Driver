package dev.funlee.a9.core;

import java.util.*;

/** Full event cards in the normalized 720 x 1280 capture; excludes the unlock footer. */
public final class CupCarousel {
    private CupCarousel(){}
    public static List<Map<String,Object>> cards(PixelFrame frame){
        return cards(frame,false);
    }
    public static List<Map<String,Object>> cards(PixelFrame frame,boolean wide){
        List<Integer> edges=new ArrayList<>();int start=-1;
        for(int y=0;y<=1280;y++){
            int hits=0;
            if(y<1280){
                int tolerance=whiteTolerance(frame,y,wide);
                for(int x=99;x<=199;x+=4)if(border(frame.pixel(x,y),wide,tolerance))hits++;
            }
            if(hits>=(wide?22:24)&&(!wide||!whiteEdge(frame,y,true)||whiteEndpoints(frame,y))){if(start<0)start=y;}
            else if(start>=0){edges.add((start+y-1)/2);start=-1;}
        }
        List<Map<String,Object>> cards=new ArrayList<>();
        for(int i=0;i+1<edges.size();i++){
            int left=edges.get(i),right=edges.get(i+1),width=right-left;
            if(width<(wide?115:160)||width>(wide?183:210))continue;
            // The running task panel covers the bottom-left underline. The selected
            // card's white side borders stay visible above it (upright y=520..620).
            boolean selected=whiteEdge(frame,left,wide)&&whiteEdge(frame,right,wide);
            if(wide&&width>175&&!selected)continue;
            int center=(left+right)/2;
            if(wide) {
                List<Integer> artwork=new ArrayList<>();
                for(int offset=-40;offset<=40;offset+=20)for(int row=124;row<=184;row+=15) {
                    int color=frame.pixel(row,center+offset);
                    artwork.add(((color>>16)&255)+((color>>8)&255)+(color&255));
                }
                cards.add(Map.of("center",center,"selected",selected,"artwork",artwork));
            } else cards.add(Map.of("center",center,"selected",selected));
        }
        return cards;
    }
    private static boolean whiteEndpoints(PixelFrame frame,int y){
        int tolerance=whiteTolerance(frame,y,true);
        // A light shape in the card artwork can pass the count but ends
        // before the true border does, as on the C-class shield.
        return (near(frame.pixel(99,y),255,255,255,tolerance)||near(frame.pixel(103,y),255,255,255,tolerance))&&
                (near(frame.pixel(195,y),255,255,255,tolerance)||near(frame.pixel(199,y),255,255,255,tolerance));
    }
    private static boolean whiteEdge(PixelFrame frame,int y,boolean wide){
        int tolerance=whiteTolerance(frame,y,wide);
        int hits=0;for(int x=99;x<=199;x+=4)if(near(frame.pixel(x,y),255,255,255,tolerance))hits++;
        return hits>=(wide?22:24);
    }
    private static int whiteTolerance(PixelFrame frame,int y,boolean wide){
        if(!wide)return 10;
        // A scaled selected border may pick up the artwork's color. Only relax
        // near a selection underline endpoint, so artwork inside a card cannot split it.
        return underline(frame,y-8)!=underline(frame,y+8)?90:39;
    }
    private static boolean underline(PixelFrame frame,int y){
        int hits=0;
        for(int x=14;x<=18;x+=2)if(near(frame.pixel(x,y),195,251,18,16))hits++;
        return hits>=2;
    }
    private static boolean border(int color,boolean wide,int tolerance){
        if(!wide)return near(color,0,51,93,6)||near(color,255,255,255,10);
        int r=(color>>16)&255,g=(color>>8)&255,b=color&255;
        return (r<=20&&g>=33&&g<=67&&b>=80&&b<=107)||near(color,255,255,255,tolerance);
    }
    private static boolean near(int color,int r,int g,int b,int tolerance){
        return Math.abs((color>>16&255)-r)<=tolerance&&Math.abs((color>>8&255)-g)<=tolerance&&Math.abs((color&255)-b)<=tolerance;
    }
}
