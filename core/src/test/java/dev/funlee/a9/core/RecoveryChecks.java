package dev.funlee.a9.core;

import java.nio.file.*;
import java.util.*;

/** Recovery acceptance without Android, private resources or real game inputs. */
final class RecoveryChecks {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static int run(Path assets)throws Exception{
        check(!RecoveryReadiness.ready("狂野飙车 竞速传奇 登录中 1s 切换游戏号"),"Game splash is not a ready menu");
        check(!RecoveryReadiness.ready("登录历史 游戏号 最近登录"),"Account picker is not a loaded menu");
        check(!RecoveryReadiness.ready("VS 正在搜索其他玩家和地点"),"Stalled match is not startup completion");
        check(RecoveryReadiness.ready("单人模式 多人游戏 每日赛事 俱乐部"),"Loaded home authorizes recovery completion");
        check(RecoveryReadiness.ready("世界系列赛 大师系列赛"),"Series menu authorizes recovery completion");
        String sale="限时发售 开启赛道制霸征程 把握 传奇促销 入手 PORSCHE 718 CAYMAN 一举成名";
        check(MultiplayerUi.classify(sale).equals("promo")&&RecoveryReadiness.ready(sale),"Limited sale is a recognized recovery overlay");
        check(!MultiplayerUi.limitedSalePromo("PORSCHE 718 CAYMAN 传奇促销"),"Vehicle text alone cannot authorize a promotion close");
        MatchmakingWatchdog w=new MatchmakingWatchdog();
        check(!w.observe(true,0,1)&&!w.observe(true,90_001,2)&&!w.observe(true,120_000,3),
                "No recovery at or below 120 seconds");
        check(!w.observe(true,120_001,3)&&w.observe(true,120_002,4),
                "Recovery after 120 seconds requires a fresh matching frame");
        check(!w.observe(false,121_000,5)&&!w.active()&&w.elapsed(200_000)==0,
                "A recognized nonmatching page resets the timer");
        check(!w.observe(true,200_000,6),"Next match starts a new clock");
        check(MultiplayerUi.classify("正在搜索其他玩家和地点……").equals("matching"),"Matching text recognized");
        check(MultiplayerUi.classify("正在搜素其他玩家和地点.").equals("matching"),"Observed search OCR variant does not reset matching timer");
        check(MultiplayerUi.classify("正在授索其他玩家和地点...").equals("matching"),"Live search OCR substitution remains matching");
        check(MultiplayerUi.classify("正在授素其他玩家和地点..").equals("matching"),"Stable alternate search OCR still advances matching timer");
        check(MultiplayerUi.classify("正在援索其他玩家和地点...").equals("matching"),"Search verb OCR variation preserves the complete matching sentence");
        w.reset();w.observe(true,0,1);w.observe(true,118_000,2);
        w.uncertain(119_000);
        check(w.active()&&w.elapsed(121_000)==121_000,"Brief OCR gap preserves elapsed matching time");
        check(!w.observe(true,121_001,2)&&!w.observe(true,121_002,3)&&w.observe(true,121_003,4),
                "Recovery after an OCR gap requires two distinct matching frames");
        w.uncertain(132_000);
        check(!w.active()&&w.elapsed(132_000)==0,"Long unknown screen expires matching evidence");
        check(MultiplayerUi.classify("其他玩家 圈").equals("unknown"),"Loading roster is not matching");
        TaskStats stats=new TaskStats();stats.project("多人上");stats.matching(92_800);
        check(stats.currentActivity("多人流程：matching · 1/3").equals("匹配中 · 92 秒")&&
                stats.panel("").contains("匹配等待 92 秒 / 120 秒"),"Both matching displays show elapsed time and the current limit");
        check(stats.currentActivity("已暂停").equals("已暂停"),"A paused task keeps its paused status");
        stats.begin("sample",false);stats.result(2);
        stats.recovered();stats.resumeProject(30,1);stats.result(1);stats.returned();
        check(stats.finished()==1&&stats.returnedCount()==0&&stats.topThreeCount()==1&&stats.restartCount()==1,"Recovery preserves confirmed finish/rank; stale result ignored");
        check(stats.panel("").contains("1/30")&&stats.panel("").contains("自动重启 1 次"),"Dashboard keeps quota and restart count");
        stats.begin("unfinished",false);stats.recovered();stats.result(1);
        check(stats.finished()==1&&stats.topThreeCount()==1,"Interrupted unconfirmed race consumes no quota or rank");
        stats.elapsed(666000);TaskStats restored=TaskStats.restore(stats.snapshot());restored.result(1);
        check(restored.finished()==1&&restored.topThreeCount()==1&&restored.elapsedMillis()==666000&&restored.restartCount()==2,"Persisted counters and elapsed survive helper restart; stale callbacks ignored");
        restored.begin("next",false);restored.result(4);restored.returned();
        check(restored.finished()==2&&restored.rankedCount()==2&&restored.topThreeCount()==1,"Resumed race increments saved totals once");
        Map<String,Object> corrupt=new LinkedHashMap<>(stats.snapshot());corrupt.put("finished",999);boolean rejected=false;
        try{TaskStats.restore(corrupt);}catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,"Reject inconsistent saved counters instead of fabricating progress");
        TaskCheckpoint savedQueue=new TaskCheckpoint();savedQueue.restore(1,1,3);savedQueue.enter(1,1,10);
        check(savedQueue.completed()==3,"Saved queue retains three finishes and seven remaining");
        int[] reads={0},inputs={0};
        try(LuaRuntime lua=new LuaRuntime((name,l)->{
            if(name.equals("_multiplayerUi")){reads[0]++;LuaRuntime.push(l,Map.of("scene","matching","text","正在搜索其他玩家和地点"));return 1;}
            if(name.equals("tap"))inputs[0]++;
            return 0;
        },List.of("_multiplayerUi","_sessionProject","_hud","print","tap"))){
            lua.set("data",Map.of("项目","多人上"));lua.set("run",Map.of("data",Map.of("次数",1)));
            lua.load("stubs","utils={defultCfg=function(v,d) return v or d end}");
            lua.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));lua.start("currentMultiplayer.run");
            lua.step(0);lua.step(200000);lua.step(400000);
            check(reads[0]==3&&inputs[0]==0,"The Lua matching branch sends no touch input while the host handles the timer");
        }
        for(int failAt:List.of(2,30,50,57))queue(assets,failAt);
        return checks;
    }
    private static void queue(Path assets,int failAt)throws Exception{
        TaskCheckpoint checkpoint=new TaskCheckpoint();TaskStats stats=new TaskStats();
        List<String> races=new ArrayList<>();boolean[] injected={false},finished={false};
        for(int vm=0;vm<2;vm++){
            try(LuaRuntime lua=new LuaRuntime((name,l)->{
                switch(name){
                    case "_queueResume":LuaRuntime.push(l,checkpoint.resume());return 1;
                    case "_queueEnter":checkpoint.enter((int)l.toInteger(1),(int)l.toInteger(2),(int)l.toInteger(3));l.push(checkpoint.completed());return 1;
                    case "_queueDone":checkpoint.projectDone();break;
                    case "_sessionProject":stats.project(l.toString(1));l.push(checkpoint.completed());return 1;
                    case "race":stats.begin("synthetic",false);stats.result(2);stats.result(2);checkpoint.completed(1);races.add(l.toString(1));
                        if(races.size()==failAt&&!injected[0]){injected[0]=true;throw new IllegalStateException("simulated crash after confirmed result");}break;
                    case "_sessionCycle":if(l.toInteger(1)==3)finished[0]=true;break;
                    default:break;
                }return 0;
            },List.of("_queueResume","_queueEnter","_queueDone","_sessionProject","race","_sessionCycle","_finished","_hud","print"))){
                lua.set("UIconfig",Map.of("routeOnly","","projects",List.of(
                        Map.of("项目","多人上","data",Map.of("次数","30")),Map.of("项目","多人下","data",Map.of("次数","20"))),"ActiveProj",List.of(true,true)));
                lua.load("bootstrap",Files.readString(assets.resolve("lua/bootstrap.lua")));
                lua.load("stubs","data={};run={};utils={backToMain=function()end};function localInit() end;function monitor() sleep(99999999) end;currentMultiplayer={run=function() local done=_sessionProject(data['项目'],run.data['次数']); for i=done+1,tonumber(run.data['次数']) do race(data['项目']);sleep(1) end end}");
                lua.start("main");
                try{for(int t=0;t<150&&!finished[0];t++)lua.step(t*2000L);}
                catch(RuntimeException e){if(!e.getMessage().contains("simulated crash"))throw e;stats.recovered();}
            }
        }
        // Third-cycle callback runs in the same step as its first race; inspect the two complete cycles.
        check(races.size()>=100&&injected[0],"Resume reaches two complete 30+20 cycles after crash at "+failAt);
        for(int i=0;i<100;i++)if(!races.get(i).equals(i%50<30?"多人上":"多人下"))throw new AssertionError("Wrong project order after recovery at "+failAt+", race "+i);
        check(stats.finished()==races.size()&&stats.restartCount()==1,"Confirmed results neither reset nor repeated after crash at "+failAt);
    }
}
