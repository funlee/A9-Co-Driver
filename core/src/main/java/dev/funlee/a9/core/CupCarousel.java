package dev.funlee.a9.core;

import java.util.*;

/** Full event cards in the normalized 720 x 1280 capture; excludes the unlock footer. */
public final class CupCarousel {
    private CupCarousel(){}
    /** Measures actual horizontal travel from overlapping card artwork, including partial or zero travel at an end. */
    public static final class ScrollTracker {
        private int[][] previous;

        private static int[][] sample(PixelFrame frame) {
            int[][] rows = new int[5][1280];
            for (int row = 0; row < rows.length; row++) {
                for (int x = 0; x < 1280; x++) rows[row][x] = frame.pixel(719 - (545 + row * 20), x);
            }
            return rows;
        }

        public Integer observe(PixelFrame frame, int direction) {
            int[][] before = previous;
            previous = sample(frame);
            if (direction == 0 || before == null) return 0;
            return match(before, previous, direction > 0 ? 0 : -800, direction > 0 ? 800 : 0, 25, 5);
        }

        /** Card selection can recenter in either direction. Keep the pre-tap reference across reads. */
        public Integer shiftSinceLastDrag(PixelFrame frame) {
            // Two cards change size/art on selection. Allow more changed pixels, but require
            // a clearer margin over competing translations before accepting the shift.
            return previous == null ? null : match(previous, sample(frame), -800, 800, 48, 8);
        }

        private static Integer match(int[][] before, int[][] after, int min, int max, int maxError, int minMargin) {
            double[] scores = new double[max - min + 1];
            int best = 0;
            for (int distance = min; distance <= max; distance++) {
                int difference = 0, samples = 0;
                for (int x = 70; x + Math.abs(distance) <= 1210; x += 11) {
                    int from = x + Math.max(0, distance);
                    int to = x - Math.min(0, distance);
                    for (int row = 0; row < after.length; row++) {
                        int a = before[row][from], b = after[row][to];
                        difference += Math.abs((a >> 16 & 255) - (b >> 16 & 255))
                                + Math.abs((a >> 8 & 255) - (b >> 8 & 255)) + Math.abs((a & 255) - (b & 255));
                        samples += 3;
                    }
                }
                int index = distance - min;
                scores[index] = (double) difference / samples;
                if (scores[index] < scores[best]) best = index;
            }
            if (scores[best] > maxError) return null;
            for (int i = 0; i < scores.length; i++) {
                if (Math.abs(i - best) > 15 && scores[i] < scores[best] + minMargin) return null;
            }
            return best + min;
        }
    }
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
            cards.add(card(frame,center,selected,wide));
        }
        if(wide)addBadgeCards(frame,cards);
        return cards;
    }
    private static Map<String,Object> card(PixelFrame frame,int center,boolean selected,boolean wide){
        if(!wide)return Map.of("center",center,"selected",selected);
        List<Integer> artwork=new ArrayList<>();
        for(int offset=-40;offset<=40;offset+=20)for(int row=124;row<=184;row+=15){
            int color=frame.pixel(row,center+offset);
            artwork.add(((color>>16)&255)+((color>>8)&255)+(color&255));
        }
        return Map.of("center",center,"selected",selected,"artwork",artwork);
    }
    // Thin outlines can disappear after scaling. A full timer badge has two pink
    // rails and a blue ticket tile; the enlarged selected card instead has a lime
    // underline and white sides above the area covered by the task notice.
    private static void addBadgeCards(PixelFrame frame,List<Map<String,Object>> cards){
        int timer=-1,strip=-1;
        for(int x=0;x<=1280;x++){
            boolean pink=x<1280&&timerRail(frame,x,484)&&timerRail(frame,x,507);
            if(pink && timer<0)timer=x;
            if(!pink && timer>=0){
                int width=x-timer;
                if(width>=83&&width<=109&&x+30<1280){
                    int blue=0;
                    for(int dx=5;dx<=29;dx+=4)if(near(frame.pixel(719-503,x+dx),0,51,93,32))blue++;
                    if(blue>=3)mergeCard(frame,cards,(timer+x-1)/2+16,false);
                }
                timer=-1;
            }
            boolean lime=x<1280&&selectionStrip(frame,x);
            if(lime && strip<0)strip=x;
            if(!lime && strip>=0){
                int width=x-strip;
                if(width>=145&&width<=180&&strip>=3&&x+3<1280&&
                        upperWhiteSide(frame,strip)&&upperWhiteSide(frame,x-1))
                    mergeCard(frame,cards,(strip+x-1)/2,true);
                strip=-1;
            }
        }
        cards.sort(Comparator.comparingInt(c->(int)c.get("center")));
    }
    private static boolean timerRail(PixelFrame frame,int x,int y){
        for(int dy=-2;dy<=2;dy++){
            int c=frame.pixel(719-y-dy,x);
            if((c>>16&255)>178&&(c>>8&255)<62&&(c&255)>45&&(c&255)<158)return true;
        }
        return false;
    }
    private static boolean selectionStrip(PixelFrame frame,int x){
        int hits=0;
        for(int row=14;row<=18;row+=2){
            int color=frame.pixel(row,x);
            if(near(color,195,248,26,46))hits++;
        }
        return hits>=2;
    }
    private static boolean upperWhiteSide(PixelFrame frame,int edge){
        for(int x=edge-3;x<=edge+3;x++){
            int hits=0;
            for(int y=451;y<=515;y+=8)if(near(frame.pixel(719-y,x),255,255,255,58))hits++;
            if(hits>=7)return true;
        }
        return false;
    }
    private static void mergeCard(PixelFrame frame,List<Map<String,Object>> cards,int center,boolean selected){
        for(int i=0;i<cards.size();i++){
            Map<String,Object> old=cards.get(i);
            if(Math.abs((int)old.get("center")-center)>16)continue;
            if(selected && !Boolean.TRUE.equals(old.get("selected")))cards.set(i,card(frame,center,true,true));
            return;
        }
        cards.add(card(frame,center,selected,true));
    }
    /** Standard event list evidence, independent of device aspect-ratio branches. */
    public static boolean listVisible(PixelFrame frame){
        if(frame.width!=720||frame.height!=1280||!RaceHud.backWedge(frame))return false;
        List<Map<String,Object>> cards=cards(frame);
        return cards.size()>=3&&cards.stream().anyMatch(card->Boolean.TRUE.equals(card.get("selected")));
    }

    /** A white caption flanked by pink rails directly above the highlighted claim card. */
    public static boolean claimSpotlight(PixelFrame frame){
        if(frame.width!=720||frame.height!=1280)return false;
        int firstRail=-1;
        for(int x=0;x<1280;x+=3){
            if(!pinkRail(frame,x))continue;
            if(firstRail>=0){
                int width=x-firstRail;
                if(width<16)continue;
                int center=(firstRail+x)/2;
                if(width>=380&&width<=665&&claimOnlyAt(frame,center)
                        &&whiteCaption(frame,firstRail+32)&&whiteCaption(frame,x-32))return true;
            }
            firstRail=x;
        }
        return false;
    }

    private static boolean whiteCaption(PixelFrame frame,int x){
        return near(frame.pixel(719-403,x),255,255,255,20)&&near(frame.pixel(719-421,x),255,255,255,20);
    }

    private static boolean pinkRail(PixelFrame frame,int x){
        return near(frame.pixel(719-403,x),255,0,84,45)&&near(frame.pixel(719-421,x),255,0,84,45);
    }

    /** Bottom yellow strip with a centered two-character label, on an already selected event. */
    public static boolean claimOnlyAt(PixelFrame frame,int center){
        if(frame.width!=720||frame.height!=1280||center<65||center>=1215)return false;
        for(int x=center-54;x<=center+54;x+=108){
            if(!claimGold(frame.pixel(719-658,x))||!claimGold(frame.pixel(719-680,x))
                    ||claimGold(frame.pixel(719-691,x)))return false;
        }
        int ink=0;
        for(int x=center-20;x<=center+20;x+=2){
            for(int y=663;y<=677;y+=2){
                int color=frame.pixel(719-y,x);
                if((color>>16&255)<215&&(color>>8&255)<165&&(color&255)<108)ink++;
            }
        }
        return ink>=10;
    }

    private static boolean claimGold(int color){
        int r=color>>16&255,g=color>>8&255,b=color&255;
        return r>=227&&g>=190&&r>=g-7&&b<145;
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
