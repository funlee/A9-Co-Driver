package dev.funlee.a9.core;
import java.nio.file.*;
import java.util.*;

final class GarageFilterChecks {
    static int run(Path assets)throws Exception {
        String fixture="data={};state={present=true,owned=0,rank=0,descending=0};taps=0;closed=0;"+
            "function keepCapture()end;function _garageFilterState()return state end;"+
            "function tap(x,y)taps=taps+1;if x==509 then state.owned=1-state.owned elseif x==304 then state.rank=1-state.rank elseif x==436 then state.descending=1-state.descending elseif x==134 then closed=closed+1;state.present=false elseif x==600 then state.present=true else error('unexpected tap') end end;";
        List<String> cases=List.of(
            "assert(utils.configureGarageFilter('0,1,2'));assert(taps==4 and closed==1 and state.owned==1 and state.rank==1 and state.descending==1)",
            "state.owned=1;state.rank=1;state.descending=1;utils.configureGarageFilter('0,1,2');assert(taps==1,'already selected state toggled off')",
            "state.owned=1;state.rank=1;state.descending=1;utils.configureGarageFilter('0');assert(taps==3 and state.owned==1 and state.rank==0 and state.descending==0)",
            "state.present=false;utils.configureGarageFilter('0,1,2');assert(taps==5 and closed==1)",
            "state.rank=-1;local ok=pcall(utils.configureGarageFilter,'1');assert(not ok and taps==0 and closed==0)",
            "tap=function()taps=taps+1 end;local ok=pcall(utils.configureGarageFilter,'0');assert(not ok and taps==3 and closed==0,'failed click accepted')",
            "state.present=false;tap=function()taps=taps+1 end;local ok=pcall(utils.configureGarageFilter,'0');assert(not ok and taps==1 and closed==0)",
            "assert(utils.configureGarageFilter(''));assert(taps==0)",
            "utils.configureGarageFilter('0,1,2');utils.configureGarageFilter('0,1,2');assert(taps==6 and closed==2,'second visit changed selected state')"
        );
        for(String script:cases)try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())) {
            lua.load("utilities",Files.readString(assets.resolve("lua/utilities.lua")));lua.load("fixture",fixture);
            lua.load("case","function test()"+script+" end");lua.start("test");
            boolean alive=true;for(int i=0;i<300&&alive;i++)alive=lua.step(i*1000L);
            if(alive)throw new AssertionError("Filter did not terminate");
        }
        if(!GarageFilter.read(new PixelFrame(720,1280,new int[720*1280])).equals(Map.of("present",false)))throw new AssertionError("Blank frame accepted");
        return cases.size()+1;
    }
}
