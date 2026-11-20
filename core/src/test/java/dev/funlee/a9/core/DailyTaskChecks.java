package dev.funlee.a9.core;

import java.nio.file.*;
import java.time.*;
import java.util.*;

final class DailyTaskChecks {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static DailyTaskPolicy policy(String time){
        ZoneId zone=ZoneId.of("Asia/Shanghai");
        Clock clock=Clock.fixed(LocalDateTime.parse(time).atZone(zone).toInstant(),zone);
        return new DailyTaskPolicy(clock);
    }
    static int run(Path assets)throws Exception {
        for(String time:List.of("00:00:00","00:15:00","00:29:59"))
            check(policy("2026-11-09T"+time).skipReason().equals("dailyQuietHours"),"Quiet window: "+time);
        for(String time:List.of("00:30:00","00:30:01","12:00:00","23:59:59"))
            check(policy("2026-11-09T"+time).skipReason().isEmpty(),"Allowed boundary: "+time);
        for(String reason:List.of("dailyQuietHours",""))queue(assets,reason);
        for(String scene:List.of("unknown","event","cars","car"))unmatchedQueue(assets,scene);
        try(LuaRuntime lua=new LuaRuntime((n,l)->0,List.of())) {
            lua.load("workflows",Files.readString(assets.resolve("lua/workflows.lua")));
            lua.load("retired daily options","utils={defultCfg=function(v,d)return v or d end};"+
                "data={['项目']='杯赛'};run.data={['杯赛漂流']='0'};run.nitro();assert(not data['氮气漂流']);"+
                "data['项目']='每日战利品';run.data={['杯赛漂流']='0',['杯赛两圈赛制']='0'};run.nitro();assert(not data['氮气漂流'])");checks+=2;
        }
        // Use the actual workflow to cross midnight between two races. No second preparation/input.
        try(LuaRuntime lua=new LuaRuntime((n,l)->0,List.of())) {
            lua.load("workflows",Files.readString(assets.resolve("lua/workflows.lua")));
            lua.load("daily",Files.readString(assets.resolve("lua/daily.lua")));
            lua.load("fixture","data={['项目']='每日战利品',['杯赛']=0};UIconfig={spPrize=''};run.data={['次数']='2'};"+
                "local done=0;function _dailyTaskStatus()return done==0 and '' or 'dailyQuietHours' end;"+
                "local lobby=true;function _soloEventUi()return {scene=lobby and 'event' or 'car',text='每日车辆战利品'}end;"+
                "run.carDetails={['每日战利品']=function()return 0 end};function dailyCup.openRaceGarage()lobby=false;return true end;"+
                "run.nitro=function()end;run.rebootHandle=function()end;utils={tipsHandle=function()end};"+
                "function keepCapture()end;function cmpColorEx()return 1 end;function _sessionLegacyPrepare()assert(done==0)end;"+
                "function _sessionLegacyCompleted()done=done+1 end;run.list={['每日战利品']={{'race',function()return 0 end}}};"+
                "function test()assert(run.project()=='dailyQuietHours');assert(done==1);"+
                "utils.startConfiguredRace=function()error('Must not start during quiet hours')end;assert(dailyCup.operation[5][2]()=='dailyQuietHours')end");
            execute(lua,"test");checks+=2;
        }
        return checks;
    }
    private static void unmatchedQueue(Path assets,String scene)throws Exception {
        try(LuaRuntime lua=new LuaRuntime((n,l)->0,List.of())) {
            lua.set("unmatchedScene",scene);
            for(String module:List.of("bootstrap","workflows","daily"))
                lua.load(module,Files.readString(assets.resolve("lua/"+module+".lua")));
            try(var input=DailyTaskChecks.class.getResourceAsStream("/daily-unmatched-queue.lua")){
                lua.load("fixture",new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
            }
            lua.start("test");for(int i=0;i<60;i++)lua.step(i*1000L);
            lua.load("verify","assert(verified)");checks++;
        }
    }
    private static void queue(Path assets,String reason)throws Exception {
        try(LuaRuntime lua=new LuaRuntime((n,l)->0,List.of())) {
            lua.set("reason",reason);
            lua.load("bootstrap",Files.readString(assets.resolve("lua/bootstrap.lua")));
            lua.load("daily",Files.readString(assets.resolve("lua/daily.lua")));
            lua.load("fixture","data={};run={};local inputs=0;local multiplayer=0;local queueDone=0;local finished=false;"+
                "UIconfig={routeOnly='',maxCycles=2,ActiveProj={true,true},projects={{['项目']='每日战利品',data={['次数']='10'}},{['项目']='多人下',data={['次数']='1'}}}};"+
                "function localInit()end;function monitor()end;function _hud()end;function print()end;function _sessionCycle(cycle)if cycle==3 then assert(not finished and queueDone==4 and multiplayer==2);assert(inputs==(reason=='' and 6 or 2));verified=true;sleep(999999)end end;"+
                "function _sessionProject()end;function _queueDone()queueDone=queueDone+1 end;function _finished()finished=true end;"+
                "function _dailyTaskStatus(index)assert(index==1);return reason end;"+
                "utils={backToMain=function()inputs=inputs+1 end,checkAuto=function()end,initCar=function()end};"+
                "run.project=function()inputs=inputs+1 end;currentMultiplayer={run=function()multiplayer=multiplayer+1 end};"+
                "function test()main()end");
            lua.start("test");for(int i=0;i<20;i++)lua.step(i*1000L);
            lua.load("verify","assert(verified, 'Two daily queue passes completed')");checks++;
        }
    }
    private static void execute(LuaRuntime lua,String method){
        lua.start(method);boolean alive=true;
        for(int i=0;i<100&&alive;i++)alive=lua.step(i*1000L);
        if(alive)throw new AssertionError("Daily schedule check did not terminate");
    }
}
