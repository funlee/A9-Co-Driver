package dev.funlee.a9.core;

import java.util.Map;

/** Lives with the assistant service, outside the replaceable game/Lua runtime. */
public final class TaskCheckpoint {
    private int cycle=1,index=1,completed,target;
    private boolean projectDone;
    public void enter(int round,int project,int count){
        if(round!=cycle||project!=index){completed=0;projectDone=false;}
        cycle=round;index=project;target=Math.max(0,count);
        if(completed>target)throw new IllegalArgumentException("Saved project progress exceeds its target");
    }
    public void completed(int newlyFinished){completed=Math.min(target,completed+Math.max(0,newlyFinished));}
    public int completed(){return completed;}
    public void projectDone(){projectDone=true;}
    public void restore(int round,int project,int done){
        if(round<1||project<1||done<0)throw new IllegalArgumentException("Invalid saved queue position");
        cycle=round;index=project;completed=done;target=done;projectDone=false;
    }
    public Map<String,Object> resume(){return Map.of("cycle",cycle,"index",projectDone?index+1:index,"completed",projectDone?0:completed);}
}
