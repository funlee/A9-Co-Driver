package dev.funlee.a9.core;

/** Only positively identified matching frames can request recovery. Times exclude pauses. */
public final class MatchmakingWatchdog {
    public enum Back { PRESENT, ABSENT, UNKNOWN }
    private long since=-1,absentSince=-1,lastFrame=-1;
    private int absentFrames;
    public void reset(){since=-1;absentSince=-1;absentFrames=0;lastFrame=-1;}
    public long elapsed(long now){return since<0?0:Math.max(0,now-since);}
    public boolean observe(boolean matching,Back back,long now,long frameId){
        if(!matching){reset();return false;}
        if(since<0)since=now;
        if(back!=Back.ABSENT){absentSince=-1;absentFrames=0;}
        else if(frameId!=lastFrame){
            if(absentSince<0)absentSince=now;
            absentFrames++;
        }
        lastFrame=frameId;
        return elapsed(now)>90_000&&back==Back.ABSENT&&absentFrames>=3&&now-absentSince>=1200;
    }
    /** Input is the host's sideways 720x1280 frame. A covered/dim/black corner is UNKNOWN. */
    public static Back backButton(PixelFrame frame,boolean covered){
        if(covered||frame.width!=720||frame.height!=1280)return Back.UNKNOWN;
        int light=0,blue=0,count=0,contextBlue=0,contextCount=0;
        for(int y=8;y<=48;y+=2)for(int x=12;x<=70;x+=2){
            int c=frame.pixel(719-y,x),r=(c>>16)&255,g=(c>>8)&255,b=c&255;
            count++;if(r>160&&g>160&&b>160)light++;
            if(b>=30&&b>r*1.25&&b>g*1.08)blue++;
        }
        // Any substantial white button background vetoes a restart, even if its arrow OCR fails.
        if(light>count*.35)return Back.PRESENT;
        for(int y=8;y<=48;y+=2)for(int x=130;x<=200;x+=2){
            int c=frame.pixel(719-y,x),r=(c>>16)&255,g=(c>>8)&255,b=c&255;
            contextCount++;if(b>=30&&b>r*1.25&&b>g*1.08)contextBlue++;
        }
        return blue>count*.92&&contextBlue>contextCount*.8?Back.ABSENT:Back.UNKNOWN;
    }
}
