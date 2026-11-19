package dev.funlee.a9.core;

import java.util.*;
import java.text.Normalizer;

/** Finds complete bright reward cards only after the caller confirms the free-choice reward scene. */
public final class DynamicRewards {
    private DynamicRewards() {}

    public static boolean prompt(String text) {
        String normalized=MapMatcher.normalize(text).replace("動態獎勵","动态奖励").replace("從這組獎勵中選","从这组奖励中选");
        return normalized.contains("动态奖励")&&normalized.contains("从这组奖励中选");
    }

    /** Keys are sorted so the first configured threshold above the opening score wins. */
    public static NavigableMap<Integer,Integer> rules(String input) {
        String raw=Normalizer.normalize(input,Normalizer.Form.NFKC).trim();
        NavigableMap<Integer,Integer> rules=new TreeMap<>();
        if(raw.isEmpty())return rules;
        for(String entry:raw.split(",",-1)){
            String[] pair=entry.trim().split("\\s*:\\s*",-1);
            if(pair.length!=2||!pair[0].matches("[1-9][0-9]{0,7}")||!pair[1].matches("[1-9][0-9]{0,2}"))
                throw new IllegalArgumentException("自选奖励请填写“里程碑分数:卡片序号”，例如 1000:4,31700:2");
            if(rules.putIfAbsent(Integer.parseInt(pair[0]),Integer.parseInt(pair[1]))!=null)
                throw new IllegalArgumentException("自选奖励的里程碑分数重复："+pair[0]);
        }
        return rules;
    }

    public enum TradePrice { PRESENT, ABSENT, UNKNOWN }

    /** Inspect the strip beneath the card, where the separate pink trade-price label appears. */
    public static TradePrice tradePrice(PixelFrame frame,int[] card,List<?> words) {
        int bottom=-1;
        for(int y=490;y>=300;y--){
            int lit=0;
            for(int x=card[2]+8;x<=card[3]-8;x+=2){
                int c=frame.pixel(719-y,x);
                if(Math.max(c>>16&255,Math.max(c>>8&255,c&255))>125)lit++;
            }
            if(lit>=(card[3]-card[2])*0.28){bottom=y;break;}
        }
        if(bottom<0||bottom+34>=530)return TradePrice.UNKNOWN;
        int left=card[0]-36,right=card[0]+36;
        for(Object item:words){
            if(!(item instanceof Map<?,?>))continue;
            Map<?,?> word=(Map<?,?>)item;
            if(!(word.get("x") instanceof Number)||!(word.get("y") instanceof Number))continue;
            Number x=(Number)word.get("x"),y=(Number)word.get("y");
            if(x.intValue()<left||x.intValue()>right||y.intValue()<bottom-2||y.intValue()>bottom+34)continue;
            String label=MapMatcher.normalize(String.valueOf(word.get("text"))).replace("交易價格","交易价格").replace("紅幣","红币");
            if(label.contains("交易价格")||label.equals("红币"))return TradePrice.PRESENT;
        }
        int pinkRows=0,bright=0;
        for(int y=bottom+1;y<=bottom+34;y++){
            int pink=0;
            for(int x=left;x<=right;x++){
                int c=frame.pixel(719-y,x),r=c>>16&255,g=c>>8&255,b=c&255;
                if(r>165&&g<115&&r-g>80&&b>=45&&b<175)pink++;
                if(Math.max(r,Math.max(g,b))>115)bright++;
            }
            if(pink>=14)pinkRows++;
        }
        if(pinkRows>=4)return TradePrice.PRESENT;
        return bright<90?TradePrice.ABSENT:TradePrice.UNKNOWN;
    }

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
