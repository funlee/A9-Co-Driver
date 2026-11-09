package dev.funlee.a9.core;

import java.nio.file.*;
import java.time.*;
import java.util.*;

final class DailyTaskChecks {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static DailyTaskPolicy policy(String time,Map<String,String> saved){
        ZoneId zone=ZoneId.of("Asia/Shanghai");
        Clock clock=Clock.fixed(LocalDateTime.parse(time).atZone(zone).toInstant(),zone);
        return new DailyTaskPolicy(clock,new DailyTaskPolicy.CompletionStore(){
            public String read(String id){return saved.getOrDefault(id,"");}
            public void write(String id,String date){saved.put(id,date);}
        });
    }
    static int run(Path assets)throws Exception {
        Map<String,String> saved=new HashMap<>();
        for(String time:List.of("00:00:00","00:15:00","00:29:59"))
            check(policy("2026-11-09T"+time,saved).skipReason("a",false).equals("dailyQuietHours"),"Quiet window, checkbox off: "+time);
        for(String time:List.of("00:30:00","00:30:01","23:59:59"))
            check(policy("2026-11-09T"+time,saved).skipReason("a",true).isEmpty(),"Allowed boundary: "+time);
        DailyTaskPolicy today=policy("2026-11-09T12:00:00",saved);
        today.completed("a",true,4,10);
        check(today.skipReason("a",true).isEmpty(),"Partial/no-ticket/stop does not finish daily task");
        today.completed("a",true,0,0);
        check(saved.isEmpty(),"Zero target is not a completion");
        today.completed("a",true,10,10);
        check(today.skipReason("a",true).equals("dailyAlreadyDone"),"Complete configured quota skips next loop");
        check(today.skipReason("a",false).isEmpty(),"Unchecking overrides completion");
        check(today.skipReason("b",true).isEmpty(),"Tasks have independent completion identities");
        check(policy("2026-11-09T22:00:00",new HashMap<>(saved)).skipReason("a",true).equals("dailyAlreadyDone"),"Recreated policy loads saved completion");
        check(policy("2026-11-10T00:15:00",saved).skipReason("a",true).equals("dailyQuietHours"),"Next day still respects quiet hours");
        check(policy("2026-11-10T00:30:00",saved).skipReason("a",true).isEmpty(),"Next day resets eligibility at 00:30");
        today.completed("b",false,10,10);
        check(!saved.containsKey("b"),"Disabled daily-once has no completion write");
        for(String reason:List.of("dailyQuietHours","dailyAlreadyDone",""))queue(assets,reason);
        // Use the actual workflow to cross midnight between two races. No second preparation/input.
        try(LuaRuntime lua=new LuaRuntime((n,l)->0,List.of())) {
            lua.load("workflows",Files.readString(assets.resolve("lua/workflows.lua")));
            lua.load("daily",Files.readString(assets.resolve("lua/daily.lua")));
            lua.load("fixture","data={['项目']='每日战利品',['杯赛']=0};UIconfig={spPrize=''};run.data={['次数']='2'};"+
                "local done=0;function _dailyTaskStatus()return done==0 and '' or 'dailyQuietHours' end;"+
                "run.nitro=function()end;utils={freePkg=function()end,getToken=function()end,getCoupons=function()end};"+
                "function keepCapture()end;function cmpColorEx()return 1 end;function _sessionLegacyPrepare()assert(done==0)end;"+
                "function _sessionLegacyCompleted()done=done+1 end;run.list={['每日战利品']={{'race',function()return 0 end}}};"+
                "function test()assert(run.project()=='dailyQuietHours');assert(done==1);"+
                "utils.startRace=function()error('Must not start during quiet hours')end;assert(dailyCup.operation[5][2]()=='dailyQuietHours')end");
            execute(lua,"test");checks+=2;
        }
        return checks;
    }
    private static void queue(Path assets,String reason)throws Exception {
        try(LuaRuntime lua=new LuaRuntime((n,l)->0,List.of())) {
            lua.set("reason",reason);
            lua.load("bootstrap",Files.readString(assets.resolve("lua/bootstrap.lua")));
            lua.load("daily",Files.readString(assets.resolve("lua/daily.lua")));
            lua.load("fixture","data={};run={};local inputs=0;local multiplayer=0;local queueDone=0;local finished=false;"+
                "UIconfig={routeOnly='',maxCycles=2,ActiveProj={true,true},projects={{['项目']='每日战利品',data={['次数']='10'}},{['项目']='多人下',data={['次数']='1'}}}};"+
                "function localInit()end;function monitor()end;function _hud()end;function print()end;function _sessionCycle()end;"+
                "function _sessionProject()end;function _queueDone()queueDone=queueDone+1 end;function _finished()finished=true end;"+
                "function _dailyTaskStatus(index)assert(index==1);return reason end;"+
                "utils={backToMain=function()inputs=inputs+1 end,checkAuto=function()end,initCar=function()end};"+
                "run.project=function()inputs=inputs+1 end;currentMultiplayer={run=function()multiplayer=multiplayer+1 end};"+
                "function test()main();assert(finished and queueDone==4 and multiplayer==2);assert(inputs==(reason=='' and 4 or 0))end");
            execute(lua,"test");checks++;
        }
    }
    private static void execute(LuaRuntime lua,String method){
        lua.start(method);boolean alive=true;
        for(int i=0;i<100&&alive;i++)alive=lua.step(i*1000L);
        if(alive)throw new AssertionError("Daily schedule check did not terminate");
    }
}
