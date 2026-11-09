package dev.funlee.a9.core;

import java.util.*;
import java.nio.file.*;

final class RewardChecks {
    private static int count;
    private static void check(boolean ok,String why){count++;if(!ok)throw new AssertionError(why);}
    static int run(Path assets)throws Exception{
        count=0;
        check(RaceRewards.amount("4,800",false)==4800&&RaceRewards.amount("＋207",true)==207,"Separate scoped gold and paid fame amounts");
        for(String s:List.of("90%","+90%","+25%","18小时38分","+189 90%","+189 +207","+1O9","+999999999","+189\n+207"))check(RaceRewards.amount(s,true)==-1,"Ambiguous/boost fame rejected: "+s);
        for(String s:List.of("4,80","金币4800","4800\n4800","-1","+4800","88,888 金币"))check(RaceRewards.amount(s,false)==-1,"Invalid/merged coin crop rejected: "+s);
        check(RaceRewards.amount("0",false)==0&&RaceRewards.amount("+0",true)==0,"Confirmed zero is distinct from unknown");
        RaceRewards.Stable stable=new RaceRewards.Stable();RaceRewards r=new RaceRewards(4800,189);
        check(stable.observe(r,1,0).coins<0,"First reward frame cannot commit");stable.observe(r,2,500);
        check(stable.observe(r,2,10000).coins<0,"Old screenshot cannot become stable merely by waiting");
        check(stable.observe(r,3,2000).coins==4800,"Fresh stable frames commit after settling delay");
        check(stable.observe(new RaceRewards(5200,207),4,2100).coins<0,"Animated amount change resets stability");
        check(stable.observe(new RaceRewards(5200,207),5,3200).fame==207,"Paid fame stabilizes independently");
        stable.reset();stable.observe(new RaceRewards(2880,-1),10,0);RaceRewards partial=stable.observe(new RaceRewards(2880,-1),11,2000);
        check(partial.coins==2880&&partial.fame<0,"Unreadable field stays unknown without discarding valid other field");
        TaskStats s=new TaskStats();s.project("多人下");
        check(!s.rewards(4800,189),"Stale reward before own race ignored");s.begin("季风秘境幽林飞瀑",false);check(!s.rewards(4800,189),"Reward needs confirmed result");s.result(2);
        check(s.rewards(4800,-1)&&s.rewards(-1,189),"Partial reads can fill independent fields");
        check(!s.rewards(4800,189)&&!s.rewards(9600,378)&&s.coins()==4800&&s.fame()==189,"Repeated frames and callbacks count once");s.returned();
        check(!s.rewards(4800,189),"Late reward after leaving race ignored");
        TaskStats restore=TaskStats.restore(s.snapshot());check(restore.coins()==4800&&restore.fame()==189&&!restore.rewards(4800,189),"Resume preserves earnings and rejects prior-race callbacks");
        restore.recovered();restore.project("多人上");restore.begin("",true);restore.result(4);restore.rewards(2880,207);restore.returned();
        check(restore.coins()==7680&&restore.fame()==396,"New race after recovery accumulates once across modes");
        restore.begin("",true);restore.result();restore.returned();
        check(restore.compact().contains("金币 1 场 / 名人堂 1 场")&&restore.panel("").contains("最近一局：金币 未识别"),"Missing rewards remain visible, not fake zeros");
        Map<String,Object> old=new LinkedHashMap<>(s.snapshot());for(String k:List.of("coins","fame","coinRaces","fameRaces","lastCoins","lastFame","fallback","fallbackReason"))old.remove(k);
        check(TaskStats.restore(old).compact().contains("金币：未识别"),"Old resume files migrate without inventing historical rewards");
        Map<String,Object> invalid=new LinkedHashMap<>(s.snapshot());invalid.put("coinRaces",4294967296L);boolean rejected=false;try{TaskStats.restore(invalid);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"Corrupted coverage cannot overflow to zero");
        TaskStats zero=new TaskStats();zero.project("多人上");zero.begin("zero",false);zero.result(8);zero.rewards(0,0);zero.returned();check(zero.compact().contains("金币：0，名人堂：0")&&!zero.compact().contains("收益未识别"),"Confirmed zero retains full coverage instead of unknown");
        TaskStats legacy=new TaskStats();legacy.project("杯赛");legacy.begin("测试",false);legacy.legacyCompleted(2880,207);check(legacy.finished()==1&&legacy.returnedCount()==1&&legacy.coins()==2880&&legacy.rankedCount()==0,"Legacy reward commits with completed iteration, without entering multiplayer rate");
        TaskStats mixed=new TaskStats();long expectedCoins=0,expectedFame=0;int n=0;
        for(String mode:List.of("多人上","杯赛","特殊赛事","每日战利品","多人下")){
            mixed.project(mode);mixed.begin("test",false);mixed.result(++n==5?5:1);
            long gold=700+n*173,experience=20+n*11;expectedCoins+=gold;expectedFame+=experience;
            mixed.rewards(gold,experience);mixed.rewards(gold,experience);mixed.returned();
            check(mixed.coins()==expectedCoins&&mixed.fame()==expectedFame,"Every race uses its distinct observed amounts once: "+mode);
        }
        check(mixed.finished()==5&&mixed.rankedCount()==2&&mixed.topThreeCount()==1,"Only world/master races enter either top-three numerator or denominator");
        check(mixed.compact().contains("多人前三 50.0%（1/2）"),"Mixed-task panel explicitly labels multiplayer-only rate");
        TaskStats fallback=new TaskStats();fallback.begin("",true);check(fallback.phase("地图识别失败，左划兜底").equals("比赛中"),"Fallback isn't a stopped task");check(!fallback.compact().contains("unknown")&&fallback.compact().contains("左划兜底"),"Compact view uses human-readable route state");
        for(int scenario=0;scenario<5;scenario++)workflow(assets,scenario);
        legacyHelper(assets);
        return count;
    }
    private static void legacyHelper(Path assets)throws Exception{
        for(boolean partial:new boolean[]{false,true}){
            int[] reads={0},writes={0};boolean[] done={false};
            try(LuaRuntime lua=new LuaRuntime((name,l)->{
                if(name.equals("_legacyRewardSample")){int n=reads[0]++;LuaRuntime.push(l,Map.of("rewardPage",true,"rewardCanContinue",n>0,"rewardReady",n>0&&!partial,"rewardCoins",n>0?2880:-1,"rewardFame",n>0&&!partial?207:-1));return 1;}
                if(name.equals("_sessionLegacyRewards")){writes[0]++;check(l.toInteger(1)==2880&&l.toInteger(2)==(partial?-1:207),"Shared legacy collector forwards only stable fields");}
                if(name.equals("done"))done[0]=true;return 0;
            },List.of("_legacyRewardSample","_sessionLegacyRewards","done"))){
                lua.load("bootstrap",Files.readString(assets.resolve("lua/bootstrap.lua")));
                lua.load("fixture","LOCAL_ROOT='/synthetic';data={};ROUTES={};UIconfig={gamePkg='test'};utils={};run={};multiple1={};multiple2={};cup={};spCup={};dsCup={};career={};dailyCup={};function test()localInit();utils.collectRaceRewards();done()end");
                lua.start("test");for(int t=0;t<30&&!done[0];t++)lua.step(t*1000L);
            }
            check(done[0]&&writes[0]==1,"Legacy collector completes stable or bounded partial reads once");
        }
    }
    private static Map<String,Object> screen(String scene){return Map.of("scene",scene,"text",scene.equals("event")?"世界系列赛 开始":"","words",List.of());}
    private static void workflow(Path assets,int scenario)throws Exception{
        List<Object> frames=new ArrayList<>();frames.add(screen("race"));
        Map<String,Object> reward=new LinkedHashMap<>(screen("result"));reward.put("text","比赛奖励 你的用时 总金币 继续");reward.put("words",List.of(Map.of("text","继续","x",1100,"y",650)));reward.put("rewardPage",true);
        if(scenario==4){reward.put("rewardExit","错失机会");reward.put("words",List.of(Map.of("text","观看广告","x",1100,"y",540),Map.of("text","错失机会","x",1050,"y",650)));}
        // A tempting generic close must not bypass reward collection.
        reward.put("closeX",Map.of("x",1200,"y",100));
        Map<String,Object> animating=new LinkedHashMap<>(reward);animating.put("rewardCoins",100);animating.put("rewardFame",90);animating.put("rewardReady",false);animating.put("rewardCanContinue",false);frames.add(animating);
        Map<String,Object> finalFrame=new LinkedHashMap<>(reward);finalFrame.put("rewardCoins",4800);finalFrame.put("rewardFame",scenario==1?-1:189);finalFrame.put("rewardReady",scenario!=1);finalFrame.put("rewardCanContinue",scenario!=3);
        for(int i=0;i<(scenario==1?12:scenario==3?30:3);i++)frames.add(finalFrame);
        if(scenario==2)frames.add(0,new LinkedHashMap<>(reward));
        for(int i=0;i<8;i++)frames.add(screen("event"));
        boolean[] animation={false};
        int[] index={0},calls={0},beforeReadyTaps={0};boolean[] done={false};TaskStats stats=new TaskStats();boolean stopped=false;
        try(LuaRuntime lua=new LuaRuntime((name,l)->{
            switch(name){
                case "_multiplayerUi":Object current=frames.get(Math.min(index[0]++,frames.size()-1));animation[0]=current==animating;LuaRuntime.push(l,current);return 1;
                case "_sessionProject":stats.project(l.toString(1));return 0;
                case "_sessionRaceBegin":stats.begin("",true);return 0;
                case "_sessionRaceResult":stats.result();return 0;
                case "_sessionRaceRewards":calls[0]++;stats.rewards(l.toInteger(1),l.toInteger(2));return 0;
                case "_sessionRaceReturned":stats.returned();return 0;
                case "_fallbackStep":l.push(80);return 1;
                case "tap":if(animation[0])beforeReadyTaps[0]++;if(scenario==4&&l.toInteger(2)!=1050)throw new AssertionError("Wrong ad exit");return 0;
                case "done":done[0]=true;return 0;
                default:return 0;
            }
        },List.of("_multiplayerUi","_sessionProject","_sessionRaceBegin","_sessionRaceResult","_sessionRaceRewards","_sessionRaceReturned","_fallbackBegin","_fallbackStep","_sessionOperation","tap","swipe","_hud","print","done"))){
            lua.set("data",Map.of("项目","多人上","多人",0));lua.set("run",Map.of("data",Map.of("次数","1","多人选路","3")));
            lua.load("helpers","utils={defultCfg=function(v,d)return v or d end};run.nitro=function()end");lua.load("workflow",Files.readString(assets.resolve("lua/multiplayer_current.lua")));
            lua.load("entry","function test()currentMultiplayer.run();done()end");lua.start("test");
            try{for(int t=0;t<120&&!done[0];t++)lua.step(t*1000L);}catch(RuntimeException e){if(e.getMessage().contains("未完成结算"))stopped=true;else throw e;}
        }
        check(beforeReadyTaps[0]==0,"No Continue or generic close during reward animation: "+scenario);
        if(scenario==3)check(stopped&&calls[0]==0&&stats.coins()==0,"Unavailable Continue stops without crediting animated income");
        else check(done[0]&&calls[0]==1&&stats.finished()==1&&stats.coins()==4800&&stats.fame()==(scenario==1?0:189),"Lua records stable/partial earnings once before Continue: "+scenario);
    }
}
