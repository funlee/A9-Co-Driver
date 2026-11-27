package dev.funlee.a9.core;

import java.util.*;
import java.nio.file.*;

final class RewardChecks {
    private static int count;
    private static void check(boolean ok,String why){count++;if(!ok)throw new AssertionError(why);}
    static int run(Path assets)throws Exception{
        count=0;
        for(String label:List.of("錯失机会","錯失機會","错矢机会"))check(RaceRewards.exitLabel(label,1044,638).equals("错失机会"),"Canonical reward skip: "+label);
        check(RaceRewards.exitLabel("繼續",1044,638).equals("继续"),"Traditional continue");
        int[] compactButton=new int[720*1280];
        for(int x=1000;x<=1100;x++)for(int y=622;y<=654;y++)compactButton[x*720+719-y]=0xffb8ec27;
        check(RaceRewards.enabledExit(new PixelFrame(720,1280,compactButton),1050,638),"Compact enabled button does not require a pixel 25px above its caption");
        check(!RaceRewards.enabledExit(new PixelFrame(720,1280,compactButton),1050,610),"An isolated stripe outside the button cannot authorize Continue");
        check(DailyEventUi.classifySolo("比赛奖励 你的用时 01:03:566 名人堂与俱乐部经验").equals("rewards"),"A missed coin label does not hide the personal rewards page");
        check(RaceRewards.exitLabel("观看广告",1044,638).isEmpty()&&RaceRewards.exitLabel("錯失机会",1044,480).isEmpty(),"Exclude ad and non-footer labels");
        check(ResultRank.personalReward("比赛賽奖励 你的用时 总金币"),"Live doubled race glyph remains a personal reward page");
        check(RaceRewards.amount("4,800",false)==4800&&RaceRewards.amount("＋207",true)==207,"World-series score crops keep the strict numeric read");
        for(String s:List.of("90%","+90%","+25%","18小时38分","+189 90%","+189 +207","+1O9","+999999999","+189\n+207"))check(RaceRewards.amount(s,true)==-1,"Ambiguous/boost fame rejected: "+s);
        for(String s:List.of("4,80","金币4800","4800\n4800","-1","+4800","88,888 金币"))check(RaceRewards.amount(s,false)==-1,"Invalid/merged coin crop rejected: "+s);
        check(RaceRewards.amount("0",false)==0&&RaceRewards.amount("+0",true)==0,"Confirmed zero is distinct from unknown");
        check(RaceRewards.amount("100,000",false)==100000,"Only settlement values have per-race caps");
        TaskStats s=new TaskStats();s.project("多人下");
        s.begin("季风秘境幽林飞瀑",false);s.result(2);s.returned();
        check(s.snapshot().isEmpty()||!s.snapshot().containsKey("coins"),"Earnings no longer persist after settlement");
        TaskStats restore=TaskStats.restore(s.snapshot());
        check(!restore.compact().contains("收益")&&!restore.panel("").contains("收益"),"Panels omit earnings rows after the removal");
        TaskStats legacy=new TaskStats();legacy.project("杯赛");legacy.begin("测试",false);legacy.legacyCompleted();check(legacy.finished()==1&&legacy.returnedCount()==1&&legacy.rankedCount()==0,"Legacy completion commits the finished iteration, without entering multiplayer rate");
        TaskStats mixed=new TaskStats();int n=0;
        for(String mode:List.of("多人上","杯赛","特殊赛事","每日战利品","多人下")){
            mixed.project(mode);mixed.begin("test",false);mixed.result(++n==5?5:1);mixed.returned();
        }
        check(mixed.finished()==5&&mixed.rankedCount()==2&&mixed.topThreeCount()==1,"Only world/master races enter either top-three numerator or denominator");
        check(mixed.compact().contains("多人前三 50.0%（1/2）"),"Mixed-task panel explicitly labels multiplayer-only rate");
        check(mixed.overlayText("比赛中").contains("多人：2，杯赛：2，特赛：1")&&mixed.overlayFullText().contains("多人：2，杯赛：2，特赛：1"),"Overlays merge world/master, group daily with cups and retain special events separately");
        check(mixed.overlayText("比赛中").contains("多人前三率：50.0%")&&mixed.overlayText("已暂停").startsWith("当前操作：已暂停"),"Reference-style overlay keeps multiplayer-only rate and pause state");
        check(mixed.panel("").contains("杯赛 2 · 特赛 1 · 其他 0")&&!mixed.overlayText("").contains("生涯"),"Detailed and compact panels keep the same separate cup/special totals");
        mixed.cycle(3);
        check(!mixed.overlayFullText().contains("轮")&&!mixed.compact().contains("轮")&&!mixed.panel("比赛中").contains("轮"),"User-facing stats omit queue cycle numbers");
        check(mixed.overlayFullText().split("\n").length==5,"Race overlay no longer carries an earnings row");
        check(mixed.overlayFullText().startsWith("地图：")&&!mixed.overlayFullText().contains("操作："),"Race statistics omit transient activity");
        check(!mixed.overlayFullText().contains("本局：")&&!mixed.overlayFullText().contains("收益覆盖")&&!mixed.overlayFullText().contains("上局")&&!mixed.overlayFullText().contains("累计指令"),"Race overlay omits removed detail rows");
        check(!mixed.overlayFullText().contains("金币")&&!mixed.overlayFullText().contains("经验值"),"Race overlay omits earnings entirely");
        check(!mixed.compact().contains("金币")&&!mixed.panel("").contains("金币"),"All panels omit coin earnings");
        // A pre-removal checkpoint with earnings still restores: legacy keys are ignored.
        Map<String,Object> oldSaved=new LinkedHashMap<>(s.snapshot());oldSaved.put("coins",4800L);oldSaved.put("fame",189L);oldSaved.put("coinRaces",1);oldSaved.put("fameRaces",1);oldSaved.put("lastCoins",4800L);oldSaved.put("lastFame",189L);
        TaskStats migrated=TaskStats.restore(oldSaved);
        check(migrated.compact().contains("完赛 1")&&!migrated.compact().contains("金币"),"Old resume files migrate without inventing or showing historical rewards");
        TaskStats zero=new TaskStats();zero.project("多人上");zero.begin("zero",false);zero.result(8);zero.returned();check(!zero.compact().contains("收益"),"Settlement no longer reports reward coverage");
        TaskStats fallback=new TaskStats();fallback.begin("",true);check(fallback.phase("地图识别失败，左划兜底").equals("比赛中"),"Fallback isn't a stopped task");check(!fallback.compact().contains("unknown")&&fallback.compact().contains("左划兜底"),"Compact view uses human-readable route state");
        check(fallback.currentActivity("正在领取免费包").equals("正在领取免费包"),"Live reward activity overrides stale race stats in compact panel");
        check(fallback.currentActivity("正在领取每日奖励\nold map 58%").equals("正在领取每日奖励"),"Compact activity uses the latest workflow line only");
        check(fallback.currentActivity("领奖：已确认领取每日目标奖励 2 项").equals("已确认领取每日目标奖励 2 项"),"Confirmed rewards remain visible as results, not race settlement");
        check(fallback.currentActivity("已暂停").equals("已暂停")&&fallback.currentActivity("任务停止：错误").equals("任务停止"),"Pause and stop override current reward activity");
        TaskStats menus=new TaskStats();check(menus.currentActivity("多人流程：cars · debug").equals("选择车辆"),"Compact menu title hides internal scene names");
        for(int scenario=0;scenario<7;scenario++)workflow(assets,scenario);
        try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())){
            lua.load("workflows",Files.readString(assets.resolve("lua/workflows.lua")));
            lua.load("counter-check","data={['杯赛']=0,['特赛']=0,['车联']=0};function test()run.dataList['杯赛']();run.dataList['特殊赛事']();run.dataList['车联']();assert(data['杯赛']==1 and run.dataList['每日战利品']==nil and data['特赛']==1 and data['车联']==1 and run.dataList['生涯']==nil)end");
            lua.start("test");lua.step(0);
        }
        count++;
        legacyHelper(assets);
        return count;
    }
    private static void legacyHelper(Path assets)throws Exception{
        // collectRaceRewards was removed: the legacy collector helper must no longer exist.
        try(LuaRuntime lua=new LuaRuntime((name,l)->{
            if(name.equals("_legacyRewardSample")){throw new AssertionError("Reward sampling must not be called after earnings removal");}
            return 0;
        },List.of("_legacyRewardSample","_sessionLegacyRewards","done"))){
            lua.load("bootstrap",Files.readString(assets.resolve("lua/bootstrap.lua")));
            lua.load("fixture","LOCAL_ROOT='/synthetic';data={};ROUTES={};UIconfig={gamePkg='test'};utils={};run={};multiple1={};multiple2={};cup={};spCup={};dsCup={};dailyCup={};function test()localInit();assert(utils.collectRaceRewards==nil);done()end");
            lua.start("test");lua.step(0);
        }
    }
    private static Map<String,Object> screen(String scene){return Map.of("scene",scene,"text",scene.equals("event")?"世界系列赛 开始":"","words",List.of());}
    private static void workflow(Path assets,int scenario)throws Exception{
        List<Object> frames=new ArrayList<>();frames.add(screen("race"));
        Map<String,Object> reward=new LinkedHashMap<>(screen("result"));reward.put("text","比赛奖励 你的用时 总金币 继续");reward.put("words",List.of(Map.of("text","继续","x",1100,"y",650)));reward.put("rewardPage",true);
        if(scenario==4){reward.put("rewardExit","错失机会");reward.put("words",List.of(Map.of("text","观看广告","x",1100,"y",540),Map.of("text","错失机会","x",1050,"y",650)));}
        if(scenario==6){
            reward.put("rewardExit","继续");reward.put("words",List.of(Map.of("text","继绩","x",1100,"y",650)));
            reward.put("rewardExitButton",Map.of("x",1100,"y",650,"label","继续"));
        }
        reward.put("closeX",Map.of("x",1200,"y",100));
        Map<String,Object> animating=new LinkedHashMap<>(reward);animating.put("rewardCanContinue",false);frames.add(animating);
        if(scenario==5){Map<String,Object> missing=new LinkedHashMap<>(reward);missing.put("scene","unknown");missing.put("text","比赛賽奖励 总金币 名人堂 继续");missing.put("rewardPage",false);frames.add(missing);}
        Map<String,Object> finalFrame=new LinkedHashMap<>(reward);finalFrame.put("rewardCanContinue",scenario!=3);
        for(int i=0;i<(scenario==1?12:scenario==3?85:4);i++)frames.add(finalFrame);
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
                case "_sessionRaceReturned":stats.returned();return 0;
                case "_fallbackStep":l.push(80);return 1;
                case "tap":if(animation[0])beforeReadyTaps[0]++;if(scenario==4&&l.toInteger(2)!=1050)throw new AssertionError("Wrong ad exit");return 0;
                case "done":done[0]=true;return 0;
                default:return 0;
            }
        },List.of("_multiplayerUi","_sessionProject","_sessionRaceBegin","_sessionRaceResult","_sessionRaceReturned","_fallbackBegin","_fallbackStep","_sessionOperation","tap","swipe","_hud","print","done"))){
            lua.set("data",Map.of("项目","多人上","多人",0));lua.set("run",Map.of("data",Map.of("次数","1","多人选路","3")));
            lua.load("helpers","utils={defultCfg=function(v,d)return v or d end};run.nitro=function()end");lua.load("workflow",Files.readString(assets.resolve("lua/multiplayer_current.lua")));
            lua.load("entry","function test()currentMultiplayer.run();done()end");lua.start("test");
            try{for(int t=0;t<120&&!done[0];t++)lua.step(t*1000L);}catch(RuntimeException e){if(e.getMessage().contains("未完成结算"))stopped=true;else throw e;}
        }
        check(beforeReadyTaps[0]==0,"No Continue or generic close during reward animation: "+scenario);
        if(scenario==3)check(stopped&&calls[0]==0,"Unavailable Continue stops without advancing the reward page");
        else check(done[0]&&stats.finished()==1,"Lua advances the reward page once Continue is enabled, without reward callbacks: "+scenario);
    }
}
