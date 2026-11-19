package dev.funlee.a9.core;
import java.nio.file.*;
import java.util.*;

/** Repeated calls must consume configured entries, including deliberate duplicate car numbers. */
final class CupCarRotationChecks {
    static int run(Path assets)throws Exception {
        String setup="data={['用车次数']={}};run={data={['选车方式']='1',['用车模式']='1'}};"+
            "calls={};selected=0;empty={};ads=0;local frame=0;"+
            "function print()end;function _soloEventUi()frame=frame+1;return {scene='car',frameId=frame,fuel=empty[selected] and 0 or 2}end;function tap()end;function keepCapture()end;function swipe()end;function toast()end;"+
            "function cmpColorEx()return 1 end;function cmpColor(x,y)if x==75 then return empty[selected] and 1 or 0 end;return 1 end;"+
            "utils.clickChoose=function(car)selected=car;calls[#calls+1]=car end;utils.carAd=function()ads=ads+1;return 1 end;";
        List<String> cases=List.of(
            "dailyCup={};run.data['杯赛用车']={1,1,1,2,3,4,5};utils.mycolor=function()return 1 end;utils.dailyPrize();assert(table.concat(dailyCup.carList,',')=='1,1,1,2,3,4,5');for i=1,8 do assert(utils.chooseCarCup(dailyCup.carList))end;assert(table.concat(calls,',')=='1,1,1,2,3,4,5,1');assert(#run.data['杯赛用车']==7)",
            "local cars={1,1,2};for i=1,4 do assert(utils.chooseCarCup(cars))end;assert(table.concat(calls,',')=='1,1,2,1')",
            "local cars={1,2,3};empty[1]=true;assert(utils.chooseCarCup(cars));assert(utils.chooseCarCup(cars));assert(table.concat(calls,',')=='1,2,1,3')",
            "local cars={1,2};empty[1]=true;empty[2]=true;assert(utils.chooseCarCup(cars)=='fuelEmpty');assert(table.concat(calls,',')=='1,2')",
            "run.data['用车模式']='0';local cars={1,2};assert(utils.chooseCarCup(cars));assert(utils.chooseCarCup(cars));assert(table.concat(calls,',')=='1,1')",
            "run.data['选车方式']='0';function findMultiColor()return -1,-1 end;local next=0;utils.carSwipe=function()end;function tap()next=next+1;selected=math.ceil(next/2);calls[#calls+1]=selected end;utils.backToGarage=function()end;empty[1]=true;assert(utils.chooseCarCup({1,2})==true and selected==2)",
            "run.data['选车方式']='0';data['用车次数']['car1']=1;local frame=0;local columns={};function findMultiColor()return -1,-1 end;utils.carSwipe=function(col)columns[#columns+1]=col end;function _soloEventUi()frame=frame+1;if columns[#columns]==6 then return {scene='cars',frameId=frame} end;return {scene='car',frameId=frame,fuel=2}end;assert(utils.chooseCarCup({1,11}));assert(table.concat(columns,',')=='6,1','Unavailable grid slot should rotate to the next configured car')",
            "function _soloEventUi()return {scene='car',frameId=1,fuel=0}end;assert(not pcall(function()utils.chooseCarCup({1})end),'Stale frame cannot prove empty')",
            "function _soloEventUi()return {scene='unknown',frameId=1}end;assert(not pcall(function()utils.chooseCarCup({1})end),'Unknown is not empty')",
            "empty[1]=true;selected=1;run.data['杯赛用车']={1};function tap()error('Must not click refuel/Skip/Start')end;assert(utils.startConfiguredRace()=='fuelEmpty')",
            "run.data['广告修车']='0';empty[1]=true;local cars={1,2};assert(utils.chooseCarCup(cars));assert(table.concat(calls,',')=='1,2' and ads==0)"
        );
        for(String script:cases)try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())){
            lua.load("utilities",Files.readString(assets.resolve("lua/utilities.lua")));lua.load("fixture",setup);
            lua.load("case","function test()"+script+" end");lua.start("test");boolean alive=true;
            for(int i=0;i<400&&alive;i++)alive=lua.step(i*1000L);
            if(alive)throw new AssertionError("Car rotation did not terminate");
        }
        // Reproduce a full lower-left panel obscuring the old nitro-bar sample.
        try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())){
            lua.load("utilities",Files.readString(assets.resolve("lua/utilities.lua")));
            lua.load("start-fixture","data={};run={data={}};local taps=0;local shown=false;"+
                "function keepCapture()end;function tap(x,y)assert(x==80 and y==1080);taps=taps+1 end;"+
                "function cmpColor()return 0 end;function cmpColorEx(pattern)"+
                "if string.find(pattern,'236|142',1,true)then return 0 end;"+
                "return string.find(pattern,'302|142',1,true)and 1 or 0 end;"+
                "utils.mycolor=function(x,y)return x==50 and y==830 and 1 or 0 end;"+
                "utils.carFuel=function()return 2 end;function _soloEventUi()return {scene='car',fuel=2,words={{text='开始A2',x=1080,y=639},{text='跳过',x=1100,y=650}}}end;function _racePanel(visible)shown=visible end;"+
                "function test()assert(utils.startRace()==1 and taps==1 and not shown,'Panel must stay hidden while loading')end");
            lua.start("test");boolean alive=true;for(int i=0;i<30&&alive;i++)alive=lua.step(i*1000L);
            if(alive)throw new AssertionError("Obscured nitro start did not terminate");
        }
        return cases.size()+1;
    }
}
