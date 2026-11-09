package dev.funlee.a9.core;
import java.nio.file.*;
import java.util.*;

/** Repeated calls must consume configured entries, including deliberate duplicate car numbers. */
final class CupCarRotationChecks {
    static int run(Path assets)throws Exception {
        String setup="data={['用车次数']={}};run={data={['选车方式']='1',['用车模式']='1'}};"+
            "calls={};selected=0;empty={};ads=0;"+
            "function tap()end;function keepCapture()end;function swipe()end;function toast()end;"+
            "function cmpColorEx()return 1 end;function cmpColor(x,y)if x==75 then return empty[selected] and 1 or 0 end;return 1 end;"+
            "utils.clickChoose=function(car)selected=car;calls[#calls+1]=car end;utils.carAd=function()ads=ads+1;return 1 end;";
        List<String> cases=List.of(
            "local cars={1,1,2};for i=1,4 do assert(utils.chooseCarCup(cars))end;assert(table.concat(calls,',')=='1,1,2,1')",
            "local cars={1,2,3};empty[1]=true;assert(utils.chooseCarCup(cars));assert(utils.chooseCarCup(cars));assert(table.concat(calls,',')=='1,2,1,3')",
            "local cars={1,2};empty[1]=true;empty[2]=true;assert(utils.chooseCarCup(cars)==false);assert(table.concat(calls,',')=='1,2')",
            "run.data['用车模式']='0';local cars={1,2};assert(utils.chooseCarCup(cars));assert(utils.chooseCarCup(cars));assert(table.concat(calls,',')=='1,1')",
            "run.data['广告修车']='0';empty[1]=true;local cars={1,2};assert(utils.chooseCarCup(cars));assert(utils.chooseCarCup(cars));assert(table.concat(calls,',')=='1,2' and ads==1)"
        );
        for(String script:cases)try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())){
            lua.load("utilities",Files.readString(assets.resolve("lua/utilities.lua")));lua.load("fixture",setup);
            lua.load("case","function test()"+script+" end");lua.start("test");boolean alive=true;
            for(int i=0;i<400&&alive;i++)alive=lua.step(i*1000L);
            if(alive)throw new AssertionError("Car rotation did not terminate");
        }
        return cases.size();
    }
}
