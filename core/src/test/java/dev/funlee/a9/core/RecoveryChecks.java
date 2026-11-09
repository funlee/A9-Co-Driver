package dev.funlee.a9.core;

import java.nio.file.*;
import java.util.*;

/** Recovery acceptance without Android, private resources or real game inputs. */
final class RecoveryChecks {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static int run(Path assets)throws Exception{
        MatchmakingWatchdog w=new MatchmakingWatchdog();
        var present=MatchmakingWatchdog.Back.PRESENT;var absent=MatchmakingWatchdog.Back.ABSENT;var unknown=MatchmakingWatchdog.Back.UNKNOWN;
        check(!w.observe(true,present,0,1)&&!w.observe(true,present,180_000,2),"Back visible: never restart even beyond old 120s limit");
        check(!w.observe(true,absent,180_500,3)&&!w.observe(true,absent,181_000,4)&&w.observe(true,absent,181_800,5),"After 90s require three missing frames over 1.2s");
        w.reset();w.observe(true,absent,0,1);w.observe(true,absent,1000,2);
        check(!w.observe(true,absent,90_000,3)&&w.observe(true,absent,90_001,4),"Strictly more than 90s");
        w.reset();w.observe(true,absent,0,1);w.observe(true,absent,1000,1);
        check(!w.observe(true,absent,100_000,1),"Repeated cached frame cannot confirm absence");
        w.observe(true,unknown,100_100,2);w.observe(true,absent,100_500,3);
        check(!w.observe(true,absent,101_500,4),"Unknown clears missing-button streak");
        check(!w.observe(false,absent,101_600,5)&&w.elapsed(200_000)==0,"Loading/race/unknown clears matching clock");
        check(!w.observe(true,absent,200_000,6),"Next match starts a new clock");
        check(MultiplayerUi.classify("正在搜索其他玩家和地点……").equals("matching"),"Matching text recognized");
        check(MultiplayerUi.classify("正在搜素其他玩家和地点.").equals("matching"),"Observed search OCR variant does not reset matching timer");
        check(MultiplayerUi.classify("其他玩家 圈").equals("unknown"),"Loading roster is not matching");
        int[] pixels=new int[720*1280];Arrays.fill(pixels,0xff142d50);
        check(MatchmakingWatchdog.backButton(new PixelFrame(720,1280,pixels),false)==absent,"Blue matching corner confirms absent");
        check(MatchmakingWatchdog.backButton(new PixelFrame(720,1280,pixels),true)==unknown,"Overlay excludes corner");
        for(int y=0;y<55;y++)for(int x=0;x<100;x++)pixels[x*720+719-y]=0xffffffff;
        check(MatchmakingWatchdog.backButton(new PixelFrame(720,1280,pixels),false)==present,"White back plate vetoes restart without OCR");
        Arrays.fill(pixels,0xff000000);
        check(MatchmakingWatchdog.backButton(new PixelFrame(720,1280,pixels),false)==unknown,"Black capture is unknown, not absence");
        TaskStats stats=new TaskStats();stats.project("多人上");stats.begin("sample",false);stats.result(2);
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
            if(name.equals("_multiplayerUi")){reads[0]++;LuaRuntime.push(l,Map.of("scene","matching","text","正在搜索其他玩家和地点","backButton","PRESENT","matchingSeconds",200));return 1;}
            if(name.equals("tap"))inputs[0]++;
            return 0;
        },List.of("_multiplayerUi","_sessionProject","_hud","print","tap"))){
            lua.set("data",Map.of("项目","多人上"));lua.set("run",Map.of("data",Map.of("次数",1)));
            lua.load("stubs","utils={defultCfg=function(v,d) return v or d end}");
            lua.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));lua.start("currentMultiplayer.run");
            lua.step(0);lua.step(200000);lua.step(400000);
            check(reads[0]==3&&inputs[0]==0,"Actual multiplayer loop keeps waiting with Back present beyond 120s, without input");
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
                lua.load("stubs","data={};run={};function localInit() end;function monitor() sleep(99999999) end;currentMultiplayer={run=function() local done=_sessionProject(data['项目'],run.data['次数']); for i=done+1,tonumber(run.data['次数']) do race(data['项目']);sleep(1) end end}");
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
