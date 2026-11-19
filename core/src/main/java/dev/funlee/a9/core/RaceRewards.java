package dev.funlee.a9.core;

import java.text.Normalizer;

/** Strict numeric reads from two scoped reward crops. -1 means unknown, never zero. */
public final class RaceRewards {
    public static String exitLabel(String text,int x,int y) {
        if(x<=850||y<=590)return "";
        String t=MapMatcher.normalize(text);
        if(t.equals("继续")||t.equals("继绩"))return "继续";
        if(t.equals("错失机会")||t.equals("错矢机会"))return "错失机会";
        return "";
    }
    public final long coins,fame;
    public RaceRewards(long coins,long fame){this.coins=coins;this.fame=fame;}
    public static long amount(String text,boolean increment){
        String normalized=Normalizer.normalize(text==null?"":text,Normalizer.Form.NFKC).trim();
        if(normalized.split("[\\r\\n]+",-1).length>1&&!normalized.matches("\\+\\s*[0-9,]+"))return -1;
        String s=normalized.replaceAll("\\s+","").replace('，',',');
        if(increment){if(!s.startsWith("+"))return -1;s=s.substring(1);}
        if(!s.matches("(?:0|[1-9][0-9]{0,7}|[1-9][0-9]{0,2}(?:,[0-9]{3}){1,2})"))return -1;
        try{long n=Long.parseLong(s.replace(",",""));return n<=99_999_999?n:-1;}catch(NumberFormatException e){return -1;}
    }
    /** Independent fields survive a missing crop; samples must be fresh and stable over time. */
    public static final class Stable {
        private long started=-1,lastFrame=-1,coinSince=-1,fameSince=-1,coin=-1,fame=-1,sampleAt=-1;
        private int coinSamples,fameSamples;
        public void reset(){started=lastFrame=coinSince=fameSince=coin=fame=sampleAt=-1;coinSamples=fameSamples=0;}
        public RaceRewards observe(RaceRewards r,long frame,long now){
            if(started<0)started=now;
            if(frame>lastFrame){
                sampleAt=now;
                if(r.coins>=0){if(coin!=r.coins){coin=r.coins;coinSince=now;coinSamples=0;}coinSamples++;}else{coin=-1;coinSince=-1;coinSamples=0;}
                if(r.fame>=0){if(fame!=r.fame){fame=r.fame;fameSince=now;fameSamples=0;}fameSamples++;}else{fame=-1;fameSince=-1;fameSamples=0;}
                lastFrame=frame;
            }
            boolean ready=sampleAt-started>=1800;
            return new RaceRewards(ready&&coinSamples>=2&&sampleAt-coinSince>=1000?coin:-1,ready&&fameSamples>=2&&sampleAt-fameSince>=1000?fame:-1);
        }
    }
}
