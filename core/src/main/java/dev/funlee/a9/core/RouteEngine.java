package dev.funlee.a9.core;

import java.util.*;

/** Deterministic scheduler. All methods run on the automation worker. Time is monotonic. */
public final class RouteEngine {
    public interface Touch {
        void tap(int finger, int x, int y);
        void down(int finger, int x, int y);
        void up(int finger);
    }
    private static final class Job {
        final long due, serial; final int kind, x, y, finger; final String choice;
        Job(long t,long serial,int kind,int finger,int x,int y,String choice) {
            due=t; this.serial=serial; this.kind=kind; this.finger=finger; this.x=x; this.y=y; this.choice=choice;
        }
    }
    private final Route route;
    private final Touch touch;
    private final PriorityQueue<Job> jobs=new PriorityQueue<>(Comparator.<Job>comparingLong(j->j.due).thenComparingLong(j->j.serial));
    private final int delay;
    private final boolean twoLaps, threaded, nitroDrift;
    private String currentOperation="等待路线指令";
    public String currentOperation(){return currentOperation;}
    private long serial; private int index, lap=1; private boolean stopped, drifting;
    public RouteEngine(Route route,Touch touch,int delay,boolean twoLaps,boolean threaded) {
        this(route,touch,delay,twoLaps,threaded,false);
    }
    public RouteEngine(Route route,Touch touch,int delay,boolean twoLaps,boolean threaded,boolean nitroDrift) {
        if(!route.valid()) throw new IllegalArgumentException(String.join("；",route.errors));
        if(delay<0 || delay>5000) throw new IllegalArgumentException("延迟必须为 0–5000ms");
        this.route=route; this.touch=touch; this.delay=delay; this.twoLaps=twoLaps; this.threaded=threaded;this.nitroDrift=nitroDrift;
    }
    public int nextEvent() { return index; }
    public int eventCount() { return route.events.size(); }
    public void update(int progress,long now,boolean choiceVisible) {
        if(stopped) return;
        if(progress<0 || progress>100) { tick(now,choiceVisible); return; }
        if(progress>98) { stop(); return; }
        int p=progress;
        if(twoLaps) {
            if(p>50 && lap==1) { index=0; lap=2; clear(); }
            p=lap==1?p*2:(p-50)*2;
        }
        // One source event per observation preserves the ordering of simultaneous commands.
        if(index<route.events.size() && p>=route.events.get(index).progress) send(route.events.get(index++),now);
        tick(now,choiceVisible);
    }
    private void send(Route.Event e,long t) {
        switch(e.op) {
            case "N":
                jobs.removeIf(j->j.kind==2 || j.kind==5);
                int interval=nitroDrift?100:e.interval;
                for(int k=1;k<=e.count;k++) add(t+delay+(threaded?e.a+(long)(k-1)*interval:(long)k*interval),1,1,280,1100,null);
                break;
            case "D":
                jobs.removeIf(j->j.kind==1);
                if(threaded) add(t+delay,5,2,250,150,null); else { currentOperation="D · 漂移 "+e.a+"ms";touch.down(2,250,150); drifting=true; }
                add(t+delay+e.a,2,2,0,0,null); break;
            case "S":
                currentOperation="S · 切换驾驶模式";touch.tap(4,606,218); add(t+delay+e.a,4,4,606,218,null); break;
            case "C": add(t+delay,3,3,0,0,e.choice); break;
            default: throw new IllegalArgumentException(e.op);
        }
    }
    private void add(long due,int kind,int finger,int x,int y,String choice) { jobs.add(new Job(due,serial++,kind,finger,x,y,choice)); }
    public void tick(long now,boolean icon) {
        if(stopped) return;
        List<Job> deferred=new ArrayList<>();
        while(!jobs.isEmpty() && jobs.peek().due<=now) {
            Job j=jobs.remove();
            if(j.kind==3 && !"360".equals(j.choice) && !icon) { if(now-j.due<=3000) deferred.add(j); continue; }
            if(j.kind==1) { currentOperation="N · 氮气";releaseDrift(); touch.tap(1,j.x,j.y); }
            else if(j.kind==2) {currentOperation="结束漂移";releaseDrift();}
            else if(j.kind==3) choose(j.choice,now);
            else if(j.kind==5) { currentOperation="D · 漂移";touch.down(2,j.x,j.y); drifting=true; }
            else touch.tap(j.finger,j.x,j.y);
        }
        jobs.addAll(deferred);
    }
    private void choose(String c,long t) {
        currentOperation=c.equals("360")?"360 · 旋转":c+" · 选路";
        if(c.equals("360")) {
            touch.tap(3,250,151); add(t+10,4,3,250,151,null);
            for(int k=0;k<4;k++) add(t+210+k*100,4,1,236,1000,null);
            return;
        }
        int y;
        switch(c) {
            case "21": case "42": y=600; break;
            case "22": case "43": y=680; break;
            case "31": y=564; break;
            case "32": y=644; break;
            case "33": y=720; break;
            case "41": y=520; break;
            case "44": y=760; break;
            default: throw new IllegalArgumentException(c);
        }
        touch.tap(3,590,y);
    }
    private void releaseDrift() { if(drifting) { try{touch.up(2);}finally{drifting=false;} } }
    public void clear() { jobs.clear(); releaseDrift(); }
    public boolean drifting(){return drifting;}
    public void nitroPulse(long now){add(now,1,1,236,1000,null);add(now+100,1,1,236,1000,null);}
    public void stop() { clear(); stopped=true; }
    public boolean stopped() { return stopped; }
}
