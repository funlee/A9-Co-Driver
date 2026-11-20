package dev.funlee.a9.core;

/** Only positively identified matching frames can request recovery. Times exclude pauses. */
public final class MatchmakingWatchdog {
    public static final int TIMEOUT_SECONDS=60;
    private long since=-1,lastFrame=-1,lastMatching=-1;
    private int matchingFrames;
    public void reset(){since=-1;matchingFrames=0;lastFrame=-1;lastMatching=-1;}
    /** Brief OCR gaps preserve the episode, but never authorize recovery themselves. */
    public void uncertain(long now){
        matchingFrames=0;
        if(lastMatching<0||now-lastMatching>10_000)reset();
    }
    public boolean active(){return since>=0;}
    public long elapsed(long now){return since<0?0:Math.max(0,now-since);}
    public boolean observe(boolean matching,long now,long frameId){
        if(!matching){reset();return false;}
        lastMatching=now;
        if(since<0)since=now;
        if(frameId<=lastFrame)return false;
        lastFrame=frameId;
        matchingFrames++;
        return elapsed(now)>TIMEOUT_SECONDS*1000L&&matchingFrames>=2;
    }
}
