package dev.funlee.a9.core;

import java.nio.file.*;
import java.io.StringReader;
import java.util.*;

/** Self-contained synthetic checks; no reference APK, routes, dictionaries or screenshots. */
public final class PublicChecks {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    private static boolean rejected(Runnable action){try{action.run();return false;}catch(IllegalStateException expected){return true;}}
    public static void main(String[] args)throws Exception{
        Path assets=Path.of(args[0]);
        Route sample=Route.parse("synthetic","0,N:0:1:10,10,D:20,20,21,30,360");
        check(sample.valid()&&sample.events.size()==4,"Synthetic route syntax");
        for(String bad:List.of("10,21,1,22","101,21","0,unknown","0,N:0:999999:0"))check(!Route.parse("bad",bad).valid(),"Reject malformed route");
        List<String> events=new ArrayList<>();
        RouteEngine.Touch touch=new RouteEngine.Touch(){
            public void tap(int id,int x,int y){events.add("tap:"+id);}
            public void down(int id,int x,int y){events.add("down:"+id);}
            public void up(int id){events.add("up:"+id);}
        };
        RouteEngine engine=new RouteEngine(Route.parse("synthetic","0,D:100"),touch,0,false,false);
        engine.update(0,0,false);check(engine.drifting(),"Drift starts");engine.stop();
        check(events.equals(List.of("down:2","up:2")),"Stop releases held drift");engine.update(90,1000,true);check(events.size()==2,"Stopped engine emits no more input");
        ProgressFilter filter=new ProgressFilter();check(filter.accept(0)==0&&filter.accept(5)==5,"Normal progress");
        check(filter.accept(1)==-1&&filter.accept(90)==-1&&filter.accept(90)==90,"Backtracking and jump confirmation");
        filter.reset();check(filter.accept(0)==0,"Next race resets progress");
        MapMatcher matcher=new MapMatcher(new StringReader("Sample Park\tsample-route\tPark/Test\nOther Park\tother-route\tPark/Other\n"));
        check(matcher.match("Sample Park").unique(),"Exact synthetic map");check(!matcher.match("Park Test Other").unique(),"Ambiguous maps not guessed");check(!matcher.match("").unique(),"Empty map unknown");
        DigitOcr digits=new DigitOcr(new StringReader("1|2,2|1111\n"));
        check(digits.read(new PixelFrame(2,2,new int[]{-1,-1,-1,-1}),0,0,1,1,.99).digit==1,"Synthetic dictionary and pixels");
        check(!digits.read(new PixelFrame(2,2,new int[4]),0,0,1,1,.8).valid(),"Blank is not a digit");
        VehicleUnlockGuard guard=new VehicleUnlockGuard();
        check(VehicleUnlockGuard.prompt("你可以解锁新车啦 解锁车辆"),"Unlock prompt");
        check(!VehicleUnlockGuard.prompt("车库 已可解锁"),"Garage text not an unlock popup");
        guard.observe(true,new int[]{300,600,500,650},false);guard.requireTap(400,625);
        check(rejected(()->guard.requireTap(900,625)),"Unlock button rejected");check(rejected(guard::requireOtherGesture),"Other gestures rejected");
        guard.observe(false,null,false);check(rejected(()->guard.requireTap(400,625)),"No stale Later coordinates");
        guard.observe(false,null,true);check(guard.active(),"First safe frame retains guard");guard.observe(false,null,true);check(!guard.active(),"Second safe frame clears guard");
        for(int rank=1;rank<=8;rank++)check(ResultRank.fromLines("你的排名 你的用时",List.of("第"+rank),true)==rank,"Personal footer rank "+rank);
        check(ResultRank.fromLines("获胜者 你的用时",List.of("第1"),true)==0,"Winner is not personal rank");
        check(ResultRank.fromLines("你的排名 你的用时",List.of("第1","第6"),true)==0,"Conflicting badge stays unknown");
        TaskStats stats=new TaskStats();stats.project("多人上");stats.cycle(1);stats.projectTarget(30);stats.result(1);check(stats.finished()==0,"Ignore pre-race result");
        stats.begin("Synthetic Map",false);stats.result();stats.result(2);stats.result(2);stats.returned();stats.returned();
        check(stats.finished()==1&&stats.returnedCount()==1&&stats.topThreeCount()==1,"Results and ranks count once");
        stats.begin("Synthetic Map",true);stats.result(6);stats.returned();stats.begin("",true);stats.result();stats.returned();
        check(stats.panel("").contains("50.0%（1/2）")&&stats.panel("").contains("名次待确认 1"),"Unknown rank excluded with visible denominator");
        stats.project("多人下");stats.cycle(2);stats.begin("",false);stats.result(1);stats.result(6);stats.result(1);stats.returned();
        check(stats.rankedCount()==2&&stats.topThreeCount()==1&&stats.count("多人上")==3&&stats.count("多人下")==1,"Counters survive cycles; conflicting rank invalidated");
        stats.project("杯赛");stats.legacyCompleted();check(stats.count("杯赛")==1&&stats.rankedCount()==2,"Non-multiplayer excluded from rate");
        check(new TaskStats().panel("").contains("—（0/0）"),"Fresh task has no invented rate");
        List<String> order=new ArrayList<>();
        try(LuaRuntime lua=new LuaRuntime((name,l)->{order.add(l.toString(1));return 0;},List.of("record"))){
            lua.load("synthetic","function main() beginThread(function() sleep(5);record('background')end);record('start');sleep(10);record('end')end");
            lua.start("main");lua.step(0);lua.step(1);lua.step(6);lua.step(10);check(order.equals(List.of("start","background","end")),"Cooperative Lua scheduler");
            for(String name:List.of("bootstrap","multiplayer_current")){
                String source=Files.readString(assets.resolve("lua/"+name+".lua"));lua.set("source",source);lua.load("compile","assert(load(source))");check(true,"Compile self-written Lua "+name);
            }
        }
        System.out.println("PASS "+checks+" public checks; synthetic inputs only");
    }
}
