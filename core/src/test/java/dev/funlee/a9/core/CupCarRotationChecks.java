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
            "for _,mode in ipairs({'每日战利品','杯赛','特殊赛事','车联'}) do data['项目']=mode;data['用车次数']={};calls={};local frame=0;_soloEventUi=function()frame=frame+1;return {scene=selected==1 and 'lockedCar' or 'car',fuel=2,frameId=frame}end;assert(utils.chooseCarCup({1,2})==true and table.concat(calls,',')=='1,2') end",
            "local frame,taps=0,0;function _soloEventUi()frame=frame+1;return frame==1 and {scene='vehicleUnlock',unlockLater={x=410,y=640}} or {scene='cars'} end;function tap(y,x)assert(x==410 and y==79);taps=taps+1 end;local status,detail=utils.carFuel(true);assert(status=='vehicleLocked' and detail.scene=='cars' and taps==1)",
            "local cars={1,2};empty[1]=true;empty[2]=true;assert(utils.chooseCarCup(cars)=='fuelEmpty');assert(table.concat(calls,',')=='1,2')",
            "run.data['用车模式']='0';local cars={1,2};assert(utils.chooseCarCup(cars));assert(utils.chooseCarCup(cars));assert(table.concat(calls,',')=='1,1')",
            "run.data['选车方式']='0';function findMultiColor()return -1,-1 end;utils.carSwipe=function()end;function tap()selected=selected+1;calls[#calls+1]=selected end;utils.backToGarage=function()end;empty[1]=true;assert(utils.chooseCarCup({1,2})==true and table.concat(calls,',')=='1,2')",
            "run.data['选车方式']='0';data['用车次数']['car1']=1;local frame=0;local columns={};function findMultiColor()return -1,-1 end;utils.carSwipe=function(col)columns[#columns+1]=col end;function _soloEventUi()frame=frame+1;if columns[#columns]==6 then return {scene='cars',frameId=frame} end;return {scene='car',frameId=frame,fuel=2}end;assert(utils.chooseCarCup({1,11}));assert(table.concat(columns,',')=='6,1','Unavailable grid slot should rotate to the next configured car')",
            "function _soloEventUi()return {scene='car',frameId=1,fuel=0}end;assert(not pcall(function()utils.chooseCarCup({1})end),'Stale frame cannot prove empty')",
            "function _soloEventUi()return {scene='unknown',frameId=1}end;assert(not pcall(function()utils.chooseCarCup({1})end),'Unknown is not empty')",
            "local reads,taps=0,0;function tap(y,x)assert(y==690 and x==38);taps=taps+1 end;function _soloEventUi()reads=reads+1;if reads<4 then return {scene='vehicleUpgrade',frameId=reads} end;return {scene='car',frameId=reads,fuel=7}end;assert(utils.carFuel()==7 and taps==1 and reads==5)",
            "local taps=0;function tap()taps=taps+1 end;function _soloEventUi()return {scene='vehicleUpgrade',frameId=1}end;assert(not pcall(utils.carFuel) and taps==1)",
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
        return cases.size()+1+dailyGrid(assets);
    }
    private static int dailyGrid(Path assets)throws Exception {
        String fixture;
        try(var input=CupCarRotationChecks.class.getResourceAsStream("/daily-car-grid.lua")){
            fixture=new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        }
        var scenarios=List.of("at-start","filtered-click","filtered-slide","confirm-click","confirm-slide","confirm-obscured","confirm-home","confirm-car-click","confirm-moving","confirm-stale","ordered","stuck","mismatch","model","large","locked","locked-only","locked-detail","star-card","star-detail","star-only");
        for(String scenario:scenarios)
                try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())){
            lua.set("scenario",scenario);
            lua.load("utilities",Files.readString(assets.resolve("lua/utilities.lua")));
            lua.load("grid",Files.readString(assets.resolve("lua/multiplayer_current.lua")));
            lua.load("fixture",fixture);
            lua.start("test");
            for(int t=0;t<850;t++)if(!lua.step(t*500L))break;
            lua.load("verify","assert(done,scenario)");
        }
        int[] pixels=new int[720*1280];
        for(int column=0;column<2;column++){
            int left=97+column*474;
            for(int top:new int[]{195,439}){
                int color=column==0?0xffb40b1c:0xffefdfab;
                for(int y=top+170;y<=top+217;y++)for(int x=left+10;x<=left+215;x++)
                    pixels[x*720+719-y]=column==0?0xff101829:0xff286fbe;
                for(int y=top;y<top+220;y++)pixels[left*720+719-y]=color;
                for(int x=left;x<left+120;x++)pixels[x*720+719-top]=color;
            }
        }
        var page=MultiplayerGarage.soloSlots(new PixelFrame(720,1280,pixels),List.of(
                Map.of("x",650,"y",480,"text","4,998"),
                Map.of("x",720,"y",578,"text","99,999")),16.0/9);
        var cards=(List<?>)page.get("cards");
        if(!Boolean.TRUE.equals(page.get("start"))||cards.size()!=4||
                !"4998".equals(((Map<?,?>)cards.get(3)).get("score"))||
                !"locked".equals(((Map<?,?>)cards.get(0)).get("availability"))||
                "locked".equals(((Map<?,?>)cards.get(2)).get("availability")))
            throw new AssertionError("Daily garage counts red and gold tiles and reads the expected lower card");
        for(int x=46;x<87;x++)for(int y=195;y<670;y++)pixels[x*720+719-y]=0xff963815;
        if(Boolean.TRUE.equals(MultiplayerGarage.soloSlots(new PixelFrame(720,1280,pixels),List.of(),16.0/9).get("start")))
            throw new AssertionError("A clipped preceding card cannot be counted as the origin");
        return scenarios.size()+2;
    }
}
