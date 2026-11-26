package dev.funlee.a9.core;

import java.text.Normalizer;
import java.util.regex.Pattern;

/** Strict numeric reads and reward-page controls. -1 means unknown, never zero. */
public final class RaceRewards {
    private static final Pattern RACE_TIME=Pattern.compile("你的用时\\s*[0-9]{1,2}[:：][0-9]{2}[:：.．][0-9]{3}");
    private static String normalizeTotalCoins(String text) {
        return MapMatcher.normalize(text).replace('總','总').replace('幣','币').replace("总全币","总金币");
    }
    public static boolean page(String text) {
        if(!ResultRank.personalReward(text))return false;
        String normalized=normalizeTotalCoins(text).replace('樂','乐').replace('經','经').replace('驗','验');
        return normalized.contains("总金币")||normalized.contains("俱乐部经验")||
                normalized.contains("具乐部经验")||RACE_TIME.matcher(text).find();
    }
    public static String exitLabel(String text,int x,int y) {
        if(x<=850||y<=590)return "";
        String t=MapMatcher.normalize(text).replace('錯','错').replace('機','机').replace('會','会').replace('繼','继').replace('續','续');
        if(t.equals("继续")||t.equals("继绩")||t.equals("架续"))return "继续";
        if(t.equals("错失机会")||t.equals("错矢机会"))return "错失机会";
        return "";
    }
    /** Verify the fill beside the caption. Compact buttons can be shorter than 50 pixels. */
    public static boolean enabledExit(PixelFrame frame,int x,int y) {
        int white=0,lime=0;
        for(int dy:new int[]{-13,0,13})for(int dx:new int[]{-34,34}){
            int pixel=frame.pixel(719-y-dy,x+dx),r=pixel>>16&255,g=pixel>>8&255,b=pixel&255;
            if(r>=223&&g>=223&&b>=223)white++;
            if(r>=145&&g>=207&&g>=r-12&&b<=116)lime++;
        }
        return white>=4||lime>=4;
    }
    /** Strict numeric read, also used for the world-series score crop. */
    public static long amount(String text,boolean increment){
        String normalized=Normalizer.normalize(text==null?"":text,Normalizer.Form.NFKC).trim();
        if(normalized.split("[\\r\\n]+",-1).length>1&&!normalized.matches("\\+\\s*[0-9,]+"))return -1;
        String s=normalized.replaceAll("\\s+","").replace('，',',');
        if(increment){if(!s.startsWith("+"))return -1;s=s.substring(1);}
        if(!s.matches("(?:0|[1-9][0-9]{0,7}|[1-9][0-9]{0,2}(?:,[0-9]{3}){1,2})"))return -1;
        try{long n=Long.parseLong(s.replace(",",""));return n<=99_999_999?n:-1;}catch(NumberFormatException e){return -1;}
    }
}
