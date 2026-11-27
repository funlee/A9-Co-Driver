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
        for(String scenario:List.of("ordinary","daily-claim","daily-no-claim","quiet","midnight"))
            configuredCupQueue(assets,scenario);
        try(LuaRuntime lua=new LuaRuntime((n,l)->0,List.of())) {
            lua.load("workflows",Files.readString(assets.resolve("lua/workflows.lua")));
            lua.load("retired daily options","utils={defultCfg=function(v,d)return v or d end};"+
                "data={['项目']='杯赛'};run.data={['杯赛漂流']='0'};run.nitro();assert(not data['氮气漂流']);"+
                "assert(run.dataList['每日战利品']==nil)");checks+=2;
        }
        return checks;
    }
    private static void configuredCupQueue(Path assets,String scenario)throws Exception {
        try(LuaRuntime lua=new LuaRuntime((n,l)->0,List.of())) {
            lua.set("scenario",scenario);
            for(String module:List.of("bootstrap","workflows","daily","cup"))
                lua.load(module,Files.readString(assets.resolve("lua/"+module+".lua")));
            try(var input=DailyTaskChecks.class.getResourceAsStream("/cup-queue.lua")){
                lua.load("fixture",new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
            }
            lua.start("test");for(int i=0;i<100;i++)lua.step(i*1000L);
            lua.load("verify","assert(verified)");checks++;
        }
    }
}
