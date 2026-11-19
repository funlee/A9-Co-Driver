package dev.funlee.a9.core;

import java.nio.file.*;
import java.util.*;

/** Exercises the real cup selector with stale on-disk identity and menu movement. */
final class CupSelectionChecks {
    static int run(Path assets)throws Exception{
        int[] pixels=new int[720*1280];
        // Border positions recorded at the right end: a clipped card, D/C/B/A/S,
        // then an unlock footer beginning at 1020 (not a sixth event).
        int[][] borders={{41,42},{53,53},{226,227},{238,238},{435,435},{446,447},{620,620},{631,632},{805,805},{816,818},{990,991}};
        for(int[] edge:borders)for(int y=edge[0];y<=edge[1];y++)for(int x=99;x<=199;x++)pixels[y*720+x]=edge[0]==238||edge[0]==435?0xffffffff:0xff00335d;
        for(int y=238;y<=435;y++)pixels[y*720+16]=0xffc3fb12;
        for(int x=99;x<=199;x+=12)pixels[1020*720+x]=0xffffffff;
        var cards=CupCarousel.cards(new PixelFrame(720,1280,pixels));
        if(cards.size()!=5||!cards.get(4).get("center").equals(903)||!cards.get(1).get("selected").equals(true))throw new AssertionError("Unlock footer or clipped card counted as a full event");
        // Even when the whole underline is covered, visible white borders identify
        // the selected card; a green overlay on an ordinary card must not select it.
        for(int y=0;y<1280;y++)for(int x=0;x<72;x++)pixels[y*720+x]=0xff222e3a;
        pixels[139*720+16]=0xffc3fb12;
        cards=CupCarousel.cards(new PixelFrame(720,1280,pixels));
        if(!cards.get(1).get("selected").equals(true)||!cards.get(0).get("selected").equals(false))throw new AssertionError("Task panel obscures card selection");
        if(!CupCarousel.cards(new PixelFrame(720,1280,new int[720*1280])).isEmpty())throw new AssertionError("Empty screen detected as carousel");
        int[] widePixels=new int[720*1280];
        widePixels[50*720+680]=widePixels[70*720+660]=0xffffffff;
        if(!RaceHud.visible(new PixelFrame(720,1280,widePixels)))throw new AssertionError("Pause-button samples stopped matching");
        for(int x=29;x<=53;x++)for(int y=43;y<=76;y++)widePixels[x*720+719-y]=0xffffffff;
        for(int edge:new int[]{80,238,248,387,397,537})for(int x=99;x<=199;x++)
            widePixels[edge*720+x]=edge<240?0xffe0dde2:0xff00335d;
        PixelFrame wideFrame=new PixelFrame(720,1280,widePixels);
        if(RaceHud.visible(wideFrame)||!RaceHud.backWedge(wideFrame))throw new AssertionError("Wide Back wedge looks like a race");
        var wideCards=CupCarousel.cards(wideFrame,true);
        if(wideCards.size()!=3||!wideCards.get(2).get("center").equals(467))throw new AssertionError("Wide card positions changed");
        for(int x=99;x<=199;x++){
            widePixels[80*720+x]=0xffb6aeba;
            widePixels[305*720+x]=0xffefe3d5;
        }
        for(int y=80;y<=238;y++)for(int x=14;x<=18;x++)widePixels[y*720+x]=0xffc3fb12;
        wideCards=CupCarousel.cards(new PixelFrame(720,1280,widePixels),true);
        if(wideCards.size()!=3||!wideCards.get(0).get("selected").equals(true))
            throw new AssertionError("Scaled 18:9 selection border must identify the card");
        for(int x=99;x<=191;x+=4)if(x!=135&&x!=139)widePixels[305*720+x]=0xffdddddd;
        wideCards=CupCarousel.cards(new PixelFrame(720,1280,widePixels),true);
        if(wideCards.size()!=3||!wideCards.get(1).get("center").equals(317))
            throw new AssertionError("A bright C-class shield must not split a card into two short edges");
        String fixture="realFindCup=utils.findCup;data={};run={index=3,data={['杯赛位置']='3'}};LOCAL_ROOT='/tmp/a9-cup-check';"+
            "cached=true;stale=true;selected=9;frameSelected=9;removed=0;finds=0;taps={};snapshots={};"+
            "function keepCapture()frameSelected=selected end;function releaseCapture()end;"+
            "function swipe(...)end;function print(...)end;function toast(...)end;"+
            "function tap(x,y)assert(x>=0 and x<720 and y>=0 and y<1280,'tap out of bounds');taps[#taps+1]={x,y};selected=math.floor((y-120)/180+1.5) end;"+
            "function snapShot(...)assert(frameSelected==selected,'stale frame cached');snapshots[#snapshots+1]=frameSelected;cached=true end;"+
            "os.remove=function(...)removed=removed+1;cached=false;stale=false;return true end;"+
            "utils.fileExist=function(...)return cached end;utils.mycolor=function(x,y,color)return selected==math.floor((y-120)/180+1.5) and color=='12FBC3-101010' and 1 or 0 end;"+
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
            "utils.chooseCup('3');utils.findCup=function(...)return {-1,-1,-1,-1} end;utils.chooseCup('3');assert(selected==3,'exhausted search retries default position two')",
            "selected=1;utils.mycolor=function(x,y,color)return selected==math.floor((y-120)/180+1.5) and color=='12FBC3-101010' and 1 or 0 end;local originalTap=tap;tap=function(x,y)assert(not (y==150 and selected==1),'entered first event while resetting carousel');originalTap(x,y)end;utils.chooseCup('3');assert(selected==3 and snapshots[1]==3)",
            "selected=1;utils.mycolor=function(x,y,color)return color=='12FBC3-101010' and 1 or 0 end;utils.chooseCup('1');assert(#taps==1 and #snapshots==1,'already selected first event entered twice')",
            "_findColorTriple=function(x,y,x2,y2,color)if color=='12FBC3-101010' then return 0,16,435 end;return -1,-1,-1 end;_findPicTriple=function(...)return 0,350,212 end;local r=realFindCup('cache');assert(r[1]==16 and r[2]==435 and r[3]==350,'lime cached event not recognized')",
            "_findPicTriple=function(...)return 0,350,212 end;local r=realFindCup('cache');assert(r[3]==-1,'matched image without selected card would click invalid coordinates')",
            "utils.mycolor=function(...)return 0 end;local ok=pcall(utils.chooseCup,'3');assert(not ok and #snapshots==0 and #taps==1,'unconfirmed selection entered an event')",
            "selected=1;function _cupCards() return {{center=159,selected=selected==1},{center=317,selected=false},{center=467,selected=selected==3}} end;"+
                "function tap(x,y)assert(y==467);selected=3;taps[#taps+1]=y end;assert(utils.chooseCup('3',true)==1 and #taps==2)",
            "function _cupCards()return {{center=159,selected=true},{center=467}}end;"+
                "assert(not pcall(utils.chooseCup,'2',true) and #taps==0,'a missing intermediate card changed the requested position')",
            "selected=1;local first=1;local entered=0;"+
                "function _cupCards()local cards={};for i=first,first+2 do cards[#cards+1]={center=159+(i-first)*150,selected=i==selected}end;return cards end;"+
                "function swipe(x,y,x2,y2)if y2<y then first=first+1 else first=1 end end;"+
                "function tap(x,y)local target=first+(y-159)/150;if target==selected then entered=entered+1 else selected=target end end;"+
                "assert(utils.chooseCup('5',true)==1 and selected==5 and entered==1,'wide scroll selected the wrong event')",
            "selected=3;local reads,entered=0,0;"+
                "function _cupCards()reads=reads+1;local cards={{center=170,selected=selected==1}};"+
                "if reads~=3 then cards[#cards+1]={center=320,selected=selected==2} end;"+
                "cards[#cards+1]={center=470,selected=selected==3};return cards end;"+
                "function tap(x,y)assert(y==320,'must wait for the missing second card');"+
                "if selected==2 then entered=entered+1 else selected=2 end end;"+
                "assert(utils.chooseCup('-2',true)==1 and entered==1,'transient missing card prevented a verified selection')"
        );
        int checked=5;
        for(String script:cases){
            try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())){
                lua.load("utilities",Files.readString(assets.resolve("lua/utilities.lua")));lua.load("cup-fixture",fixture);
                lua.load("cup-case","function test()"+script+" end");lua.start("test");
                boolean alive=true;for(int n=0;n<500&&alive;n++)alive=lua.step(n*1000L);
                if(alive)throw new AssertionError("Cup selection did not terminate");
            }
            checked++;
        }
        // Five real cards fit before the garage-unlock footer. Negative positions
        // must count cards, including those requiring a scroll, not screen slots.
        for(int position:new int[]{-1,-2,-6,-8,-20,-21}){
            try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())){
                lua.load("utilities",Files.readString(assets.resolve("lua/utilities.lua")));lua.load("cup-fixture",fixture);
                lua.load("negative-carousel","offset=16;selected=18;entered=0;"+
                        "function _cupCards()local cards={};for id=offset,math.min(20,offset+4) do cards[#cards+1]={center=80+(id-offset)*185,selected=id==selected} end;return cards end;"+
                        "function swipe(x,y,x2,y2)assert(x>=80 and x2>=80,'carousel swipe crosses the task panel');if y2<y then offset=16 else offset=math.max(1,offset-2)end end;"+
                        "function tap(x,y)for _,card in ipairs(_cupCards())do if card.center==y then local id=offset+(y-80)/185;if id==selected then entered=entered+1 else selected=id end;return end end;error('tap outside real cards')end;"+
                        "utils.mycolor=function(x,y,color)return 0 end;"+
                        "function test()local ok,result=pcall(utils.chooseCup,'"+position+"');"+
                        (position==-21?"assert(not ok and entered==0,'out-of-range position entered an event')":
                        "assert(ok,result);assert(result==1 and selected=="+(21+position)+" and entered==1,'wrong negative event');assert(#snapshots==1 and snapshots[1]==selected,'wrong identity cached')")+" end");
                lua.start("test");boolean alive=true;for(int n=0;n<500&&alive;n++)alive=lua.step(n*1000L);
                if(alive)throw new AssertionError("Negative cup selector did not terminate");
            }
            checked++;
        }
        return checked;
    }
}
