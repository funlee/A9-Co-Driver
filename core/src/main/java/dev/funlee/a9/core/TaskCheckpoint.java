package dev.funlee.a9.core;

import java.util.LinkedHashMap;
import java.util.Map;

/** Lives with the assistant service, outside the replaceable game/Lua runtime. */
public final class TaskCheckpoint {
    private int cycle=1,index=1,completed,target,rewardCycle;
    private boolean projectDone,rewardWork,needsHome,homeRestarted;
    private int failures;
    private String firstFailure="",lastFailure="";

    public void enter(int round,int project,int count){
        if(needsHome)throw new IllegalStateException("项目开始前尚未确认主界面");
        if(round!=cycle||project!=index||projectDone){completed=0;projectDone=false;clearFailures();}
        cycle=round;index=project;target=Math.max(0,count);
        if(completed>target)throw new IllegalArgumentException("Saved project progress exceeds its target");
    }
    public void beginRewards(int round){
        if(needsHome)throw new IllegalStateException("领奖前尚未确认主界面");
        if(cycle!=round){cycle=round;index=1;completed=0;target=0;projectDone=false;}
        if(!rewardWork)clearFailures();
        rewardWork=true;
    }
    public void completed(int newlyFinished){completed=Math.min(target,completed+Math.max(0,newlyFinished));}
    public void recordResult(TaskStats stats,int position){
        int before=stats.finished();
        stats.result(position);
        completed(stats.finished()-before);
    }
    public int completed(){return completed;}
    public void projectDone(){projectDone=true;clearFailures();}
    public void rewardsDone(int round){rewardCycle=round;rewardWork=false;clearFailures();}

    private void clearFailures(){
        failures=0;firstFailure="";lastFailure="";needsHome=false;homeRestarted=false;
    }
    public void failure(String reason){
        if(needsHome||failures>=2)throw new IllegalStateException("重复记录项目异常");
        if(failures++==0)firstFailure=reason;
        lastFailure=reason;
        needsHome=true;homeRestarted=false;
    }
    public boolean needsHome(){return needsHome;}
    public boolean beginHomeRestart(){
        if(!needsHome||homeRestarted)return false;
        homeRestarted=true;
        return true;
    }
    public void homeConfirmed(){
        if(!needsHome)throw new IllegalStateException("当前没有待恢复的任务");
        needsHome=false;homeRestarted=false;
        if(failures==2){
            if(rewardWork)rewardsDone(cycle);
            else projectDone();
        }
    }
    public String failureSummary(){
        String scope="第 "+cycle+" 轮"+(rewardWork?"领奖":"项目 "+index);
        return failures==1?scope+"首次异常："+firstFailure+"；回主后重试":
                scope+"再次异常，回主后跳过本轮剩余部分。首次："+firstFailure+"；本次："+lastFailure;
    }
    public void restore(int round,int project,int done){restore(round,project,done,0);}
    public void restore(int round,int project,int done,int lastRewardCycle){
        if(round<1||project<1||done<0)throw new IllegalArgumentException("Invalid saved queue position");
        cycle=round;index=project;completed=done;target=done;projectDone=false;
        rewardCycle=lastRewardCycle;rewardWork=false;clearFailures();
    }
    public void restoreRecovery(int count,boolean pending,boolean restarted,boolean rewards,String first,String last){
        if(count<0||count>2||(count==2&&!pending)||(count==0&&pending)||(restarted&&!pending))
            throw new IllegalArgumentException("无效的任务恢复记录");
        failures=count;needsHome=pending;homeRestarted=restarted;rewardWork=rewards;
        firstFailure=first;lastFailure=last;
    }
    public Map<String,Object> resume(){
        Map<String,Object> state=new LinkedHashMap<>();
        state.put("cycle",cycle);state.put("index",projectDone?index+1:index);
        state.put("completed",projectDone?0:completed);state.put("rewardCycle",rewardCycle);
        state.put("failures",failures);state.put("needsHome",needsHome);state.put("homeRestarted",homeRestarted);
        state.put("rewardWork",rewardWork);state.put("firstFailure",firstFailure);state.put("lastFailure",lastFailure);
        return state;
    }
}
