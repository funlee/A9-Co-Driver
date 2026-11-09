package dev.funlee.a9.core;

import java.nio.file.*;
import java.util.*;

/** Exercises the real cup selector with stale on-disk identity and menu movement. */
final class CupSelectionChecks {
    static int run(Path assets)throws Exception{
        String fixture="data={};run={index=3,data={['杯赛位置']='3'}};LOCAL_ROOT='/tmp/a9-cup-check';"+
            "cached=true;stale=true;selected=9;frameSelected=9;removed=0;finds=0;taps={};snapshots={};"+
            "function keepCapture()frameSelected=selected end;function releaseCapture()end;"+
            "function swipe(...)end;function print(...)end;function toast(...)end;"+
            "function tap(x,y)assert(x>=0 and x<720 and y>=0 and y<1280,'tap out of bounds');taps[#taps+1]={x,y};selected=math.floor((y-120)/180+1.5) end;"+
            "function snapShot(...)assert(frameSelected==selected,'stale frame cached');snapshots[#snapshots+1]=frameSelected;cached=true end;"+
            "os.remove=function(...)removed=removed+1;cached=false;stale=false;return true end;"+
            "utils.fileExist=function(...)return cached end;utils.mycolor=function(...)return 1 end;"+
            "utils.mySwipe=function(...)end;_findColorTriple=function(...)return -1,-1,-1 end;"+
            "utils.findCup=function(...)finds=finds+1;keepCapture();if cached and (not stale or selected==2) then return {16,120+180*(selected-1),350,212} end;return {-1,-1,-1,-1} end;";
        List<String> cases=List.of(
            "assert(utils.chooseCup('3')==1);assert(selected==3,'stale identity overrides configured third cup');assert(removed==1 and #snapshots==1 and snapshots[1]==3);assert(finds==0,'new task searched old cache')",
            "utils.chooseCup('3');utils.chooseCup('3');assert(removed==1 and #snapshots==1 and selected==3,'same task lost its selected event')",
            "utils.chooseCup('3');utils.chooseCup('4');assert(removed==2 and #snapshots==2 and snapshots[2]==4 and selected==4,'position change reused wrong cached event')",
            "utils.chooseCup('1');assert(taps[#taps][1]==100 and taps[#taps][2]==150,'first cup clicks zero coordinates')",
            "local ok=pcall(utils.chooseCup,'0');assert(not ok and #taps==0 and removed==0,'zero position emits input')",
            "local ok=pcall(utils.chooseCup,'no');assert(not ok and #taps==0 and removed==0,'invalid text silently chooses position two')",
            "local ok=pcall(utils.chooseCup,'2.5');assert(not ok and #taps==0,'fractional position emits input')",
            "utils.chooseCup('3');local count=0;utils.findCup=function(...)count=count+1;if count==3 then return {16,300,350,212} end;return {-1,-1,-1,-1} end;utils.chooseCup('3');assert(run.data['杯赛位置']=='2','rediscovered event saves zero-based position')",
            "utils.chooseCup('3');utils.findCup=function(...)return {-1,-1,-1,-1} end;utils.chooseCup('3');assert(selected==3,'exhausted search retries default position two')"
        );
        int checked=0;
        for(String script:cases){
            try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())){
                lua.load("utilities",Files.readString(assets.resolve("lua/utilities.lua")));lua.load("cup-fixture",fixture);
                lua.load("cup-case","function test()"+script+" end");lua.start("test");
                boolean alive=true;for(int n=0;n<500&&alive;n++)alive=lua.step(n*1000L);
                if(alive)throw new AssertionError("Cup selection did not terminate");
            }
            checked++;
        }
        return checked;
    }
}
