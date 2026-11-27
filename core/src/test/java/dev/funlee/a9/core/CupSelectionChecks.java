package dev.funlee.a9.core;

import java.nio.file.*;
import java.util.*;

/** Exercises the real cup selector with stale on-disk identity and menu movement. */
final class CupSelectionChecks {
    private static PixelFrame scrollFrame(int offset) {
        int[] pixels=new int[720*1280];
        for(int row=0;row<5;row++)for(int x=0;x<1280;x++){
            long hash=(x+offset+row*10007L)*2654435761L;
            pixels[x*720+719-(545+row*20)]=(int)(hash^(hash>>>17));
        }
        return new PixelFrame(720,1280,pixels);
    }
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
        if(CupCarousel.listVisible(new PixelFrame(720,1280,pixels)))throw new AssertionError("Cards without Back must not identify a list");
        for(int x=29;x<=53;x++)for(int y=43;y<=76;y++)pixels[x*720+719-y]=0xffffffff;
        if(!CupCarousel.listVisible(new PixelFrame(720,1280,pixels)))throw new AssertionError("Standard carousel must not require a wide viewport");
        int[] noSelection=pixels.clone();
        for(int edge:new int[]{238,435})for(int x=99;x<=199;x++)noSelection[edge*720+x]=0xff00335d;
        if(CupCarousel.listVisible(new PixelFrame(720,1280,noSelection)))throw new AssertionError("List requires a selected event");
        int[] widePixels=new int[720*1280];
        widePixels[50*720+680]=widePixels[70*720+660]=0xffffffff;
        if(!RaceHud.visible(new PixelFrame(720,1280,widePixels)))throw new AssertionError("Pause-button samples stopped matching");
        for(int x=29;x<=53;x++)for(int y=43;y<=76;y++)widePixels[x*720+719-y]=0xffffffff;
        for(int edge:new int[]{80,238,248,387,397,537})for(int x=99;x<=199;x++)
            widePixels[edge*720+x]=edge<240?0xffe0dde2:0xff00335d;
        PixelFrame wideFrame=new PixelFrame(720,1280,widePixels);
        if(RaceHud.visible(wideFrame)||!RaceHud.backWedge(wideFrame))throw new AssertionError("Wide Back wedge looks like a race");
        if(!RaceHud.raceEvidence(wideFrame,false))throw new AssertionError("Pause dots still prove a live race under the Back wedge");
        int[] banner=new int[720*1280];
        for(int x=29;x<=53;x++)for(int y=43;y<=76;y++)banner[x*720+719-y]=0xffffffff;
        PixelFrame bannerFrame=new PixelFrame(720,1280,banner);
        if(!RaceHud.backWedge(bannerFrame)||RaceHud.raceEvidence(bannerFrame,false))
            throw new AssertionError("A white banner arrow alone is not a running race");
        for(int center:new int[]{1206,1224}){
            int[] homePixels=widePixels.clone();
            for(int[] point:new int[][]{{0,-13},{-13,2},{11,2},{-12,19},{10,19}})
                homePixels[(center+point[0])*720+719-(34+point[1])]=0xffeeeeee;
            PixelFrame home=new PixelFrame(720,1280,homePixels);
            Map<String,Integer> button=RaceHud.homeButton(home,true);
            if(!button.equals(Map.of("x",center,"y",34))||!RaceHud.homeButton(home,false).isEmpty())
                throw new AssertionError("Find the shifted Home icon only on the supported menu layout");
        }
        int[] standardPixels=widePixels.clone();
        for(int[] point:new int[][]{{0,-13},{-13,2},{11,2},{-12,19},{10,19}})
            standardPixels[(1250+point[0])*720+719-(27+point[1])]=0xffeeeeee;
        if(!RaceHud.homeButton(new PixelFrame(720,1280,standardPixels),false).equals(Map.of("x",1250,"y",27)))
            throw new AssertionError("Find the Home tile on the standard club subpage");
        // Xiaomi 15 screenshot: the notice obscures the C card's sides but leaves
        // the entry underline, upper white corners and seven ticket badges visible.
        int[] phone=new int[720*1280];
        for(int center:new int[]{82,230,380,530,847,997,1147}){
            for(int x=center-64;x<center+33;x++)for(int y:new int[]{484,507})phone[x*720+719-y]=0xffed095a;
            for(int x=center+33;x<center+65;x++)phone[x*720+719-503]=0xff06345b;
        }
        for(int x=610;x<=768;x++)for(int row=14;row<=18;row++)phone[x*720+row]=0xffc4ed22;
        for(int x:new int[]{611,767})for(int y=451;y<=515;y++)phone[x*720+719-y]=0xffefe3e9;
        var phoneCards=CupCarousel.cards(new PixelFrame(720,1280,phone),true);
        if(phoneCards.size()!=8||!Boolean.TRUE.equals(phoneCards.get(4).get("selected"))||
                Math.abs((int)phoneCards.get(2).get("center")-380)>3)
            throw new AssertionError("Count six from the right despite the notice covering the selected C card");
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
        int[] spotlight=new int[720*1280];
        for(int y=394;y<=438;y++){
            for(int x=390;x<=398;x++)spotlight[x*720+719-y]=0xffff0054;
            for(int x=409;x<=870;x++)spotlight[x*720+719-y]=0xffffffff;
            for(int x=881;x<=889;x++)spotlight[x*720+719-y]=0xffff0054;
        }
        for(int x=562;x<=717;x++)for(int y=654;y<=684;y++)spotlight[x*720+719-y]=0xffffe923;
        PixelFrame unlabeled=new PixelFrame(720,1280,spotlight);
        if(CupCarousel.claimSpotlight(unlabeled)||CupCarousel.claimButtonAt(unlabeled,639))
            throw new AssertionError("A gold card without a claim label cannot identify a reward");
        for(int x=626;x<=651;x++)for(int y=664;y<=678;y++)
            if(x%5<=2)spotlight[x*720+719-y]=0xffa47823;
        PixelFrame claim=new PixelFrame(720,1280,spotlight);
        if(!CupCarousel.claimSpotlight(claim)||!CupCarousel.claimButtonAt(claim,639)||CupCarousel.claimButtonAt(claim,803))
            throw new AssertionError("Claim spotlight applies only to the highlighted card");
        for(int x=881;x<=889;x++)for(int y=394;y<=438;y++)spotlight[x*720+719-y]=0;
        if(CupCarousel.claimSpotlight(new PixelFrame(720,1280,spotlight)))
            throw new AssertionError("An unrelated single pink border is not a claim spotlight");
        String fixture="realFindCup=utils.findCup;data={};run={index=3,data={['杯赛位置']='3'}};LOCAL_ROOT='/tmp/a9-cup-check';"+
            "cached=true;stale=true;selected=9;frameSelected=9;removed=0;finds=0;taps={};snapshots={};"+
            "function keepCapture()frameSelected=selected end;function releaseCapture()end;"+
            "function swipe(...)end;function print(...)end;function toast(...)end;"+
            "function tap(x,y)assert(x>=0 and x<720 and y>=0 and y<1280,'tap out of bounds');taps[#taps+1]={x,y};selected=math.floor((y-120)/180+1.5) end;"+
            "function snapShot(...)assert(frameSelected==selected,'stale frame cached');snapshots[#snapshots+1]=frameSelected;cached=true end;"+
            "os.remove=function(...)removed=removed+1;cached=false;stale=false;return true end;"+
            "utils.fileExist=function(...)return cached end;"+
            "function _cupCards()local cards={};for i=1,8 do cards[i]={center=120+180*(i-1),selected=i==selected} end;return cards end;"+
            "utils.mycolor=function(x,y,color)if color~='12FBC3-101010' then return 0 end;"+
            "for _,c in ipairs(_cupCards())do if math.abs(c.center-y)<=5 then return c.selected and 1 or 0 end end;return 0 end;"+
            "utils.mySwipe=function(...)end;"+
            "function _findColorTriple(x,y,x2,y2,color)if color=='12FBC3-101010' then "+
            "for _,c in ipairs(_cupCards())do if c.selected then return 0,16,c.center end end end return -1,-1,-1 end;"+
            "utils.findCup=function(...)finds=finds+1;keepCapture();if cached and (not stale or selected==2) then return {16,120+180*(selected-1),350,212} end;return {-1,-1,-1,-1} end;";
        List<String> cases=List.of(
            "local entered=false;local offset=0;local delta=0;local picked=1;utf8.left=function(s)return s end;"+
                "function swipe(x,a,y,b) local old=offset;offset=math.max(0,math.min(4*186,offset+a-b));delta=offset-old end;"+
                "function _cupScrollDelta(d)return (not d or d==0) and 0 or delta end;"+
                "function _cupCards()local t={};for i=1,10 do local x=151+(i-1)*186-offset;"+
                "if x>70 and x<1200 then t[#t+1]={center=x,selected=i==picked} end end;return t end;"+
                "function _soloEventUi()return {scene=entered and 'event' or 'cupList'} end;"+
                "function tap(x,y)assert(not entered);for _,c in ipairs(_cupCards()) do if c.center==y then "+
                "local i=1+(y+offset-151)/186;if x==65 then assert(i==10 and picked==10);entered=true else picked=i end;return end end;error('not a card') end;"+
                "assert(utils.chooseCup('10')==1 and entered,'wrong card at the right stop')",
            "assert(utils.chooseCup('3')==1);assert(selected==3,'stale identity overrides configured third cup');assert(removed==1 and #snapshots==1 and snapshots[1]==3);assert(finds==0,'new task searched old cache')",
            "utils.chooseCup('3');utils.chooseCup('3');assert(removed==1 and #snapshots==1 and selected==3,'same task lost its selected event')",
            "utils.chooseCup('3');utils.chooseCup('4');assert(removed==2 and #snapshots==2 and snapshots[2]==4 and selected==4,'position change reused wrong cached event')",
            "utils.chooseCup('1');assert(taps[#taps][1]==100 and taps[#taps][2]==150,'first cup clicks zero coordinates')",
            "local hint=true;local swipes=0;local picked=1;local entered=0;"+
                "function _cupClaimState()return hint and 'spotlight' or '' end;"+
                "function swipe(x,y,x2,y2)assert(x==115 and x2==115);swipes=swipes+1;hint=false end;"+
                "function _cupCards()return {{center=155,selected=picked==1},{center=310,selected=picked==2},{center=465,selected=picked==3}}end;"+
                "function tap(x,y)assert(not hint and y==465 and x==115);if picked==3 then entered=entered+1 end;picked=3 end;"+
                "assert(utils.chooseCup('3',true)==1 and entered==1 and swipes>=7,'reward hint changed configured event number')",
            "assert(utils.chooseCup('0')=='cupPositionInvalid' and #taps==0 and removed==0,'zero position emits input')",
            "assert(utils.chooseCup('no')=='cupPositionInvalid' and #taps==0 and removed==0,'invalid text silently chooses position two')",
            "assert(utils.chooseCup('2.5')=='cupPositionInvalid' and #taps==0,'fractional position emits input')",
            "utils.chooseCup('3');local count=0;utils.findCup=function(...)count=count+1;if count==3 then return {16,300,350,212} end;return {-1,-1,-1,-1} end;utils.chooseCup('3');assert(run.data['杯赛位置']=='2','rediscovered event saves zero-based position')",
            "utils.chooseCup('3');utils.findCup=function(...)return {-1,-1,-1,-1} end;utils.chooseCup('3');assert(selected==3,'exhausted search retries default position two')",
            "selected=1;utils.mycolor=function(x,y,color)return selected==math.floor((y-120)/180+1.5) and color=='12FBC3-101010' and 1 or 0 end;local originalTap=tap;tap=function(x,y)assert(not (y==150 and selected==1),'entered first event while resetting carousel');originalTap(x,y)end;utils.chooseCup('3');assert(selected==3 and snapshots[1]==3)",
            "selected=1;utils.mycolor=function(x,y,color)return color=='12FBC3-101010' and 1 or 0 end;utils.chooseCup('1');assert(#taps==1 and #snapshots==1,'already selected first event entered twice')",
            "_findColorTriple=function(x,y,x2,y2,color)if color=='12FBC3-101010' then return 0,16,435 end;return -1,-1,-1 end;_findPicTriple=function(...)return 0,350,212 end;local r=realFindCup('cache');assert(r[1]==16 and r[2]==435 and r[3]==350,'lime cached event not recognized')",
            "_findPicTriple=function(...)return 0,350,212 end;local r=realFindCup('cache');assert(r[3]==-1,'matched image without selected card would click invalid coordinates')",
            "function _cupCards()return {{center=120},{center=300},{center=480}}end;utils.mycolor=function(...)return 0 end;"+
                "assert(utils.chooseCup('3')=='cupPositionUnavailable' and #snapshots==0 and #taps==1,'unconfirmed selection entered an event')",
            "selected=1;function _cupCards() return {{center=159,selected=selected==1},{center=317,selected=false},{center=467,selected=selected==3}} end;"+
                "function tap(x,y)assert(y==467);selected=3;taps[#taps+1]=y end;assert(utils.chooseCup('3',true)==1 and #taps==2)",
            "function _cupCards()return {{center=159,selected=true},{center=467}}end;"+
                "assert(utils.chooseCup('2',true)=='cupPositionUnavailable' and #taps==0,'a missing intermediate card changed the requested position')",
            "selected=1;local first=1;local entered=0;"+
                "function _cupCards()local cards={};for i=first,first+2 do cards[#cards+1]={center=159+(i-first)*150,selected=i==selected}end;return cards end;"+
                "function swipe(x,y,x2,y2)if y2<y then first=first+1 else first=1 end end;"+
                "function tap(x,y)local target=first+(y-159)/150;if target==selected then entered=entered+1 else selected=target end end;"+
                "assert(utils.chooseCup('5',true)==1 and selected==5 and entered==1,'wide scroll selected the wrong event')",
            "selected=5;local entered=0;local centers={82,230,380,530,689,847,997,1147};"+
                "function _cupCards()local cards={};for i,x in ipairs(centers)do cards[i]={center=x+(selected==3 and 9 or 0),selected=i==selected,artwork={75*i}}end;return cards end;"+
                "function tap(x,y)local c=_cupCards()[3];assert(y==c.center,'must select sixth card from right');if selected==3 then entered=entered+1 else selected=3 end end;"+
                "assert(utils.chooseCup('-6')==1 and selected==3 and entered==1,'legacy entry also verifies the shifted Xiaomi wide card')",
            "selected=3;local reads,entered=0,0;"+
                "function _cupCards()reads=reads+1;local cards={{center=170,selected=selected==1}};"+
                "if reads~=3 then cards[#cards+1]={center=320,selected=selected==2} end;"+
                "cards[#cards+1]={center=470,selected=selected==3};return cards end;"+
                "function tap(x,y)assert(y==320,'must wait for the missing second card');"+
                "if selected==2 then entered=entered+1 else selected=2 end end;"+
                "assert(utils.chooseCup('-2',true)==1 and entered==1,'transient missing card prevented a verified selection')",
            // 复现「配置 13 实际停在 11」：滑动力度只有半张卡时，逐张校验仍须走到第 13 张。
            "offset=0;selected=1;entered=0;"+
                "function visible()local cards={};for id=1,20 do local c=(id-0.5)*185-offset;"+
                "if c>=92 and c<=908 then cards[#cards+1]={center=math.floor(c+0.5),selected=id==selected} end end;return cards end;"+
                "function _cupCards()return visible() end;"+
                "function swipe(x,y,x2,y2)if y2>y then offset=0 else offset=math.min(800,offset+120) end end;"+
                "utils.mySwipe=function(x,y,x2,y2)offset=math.min(3400,offset+120) end;"+
                "function tap(x,y)for _,c in ipairs(visible())do if math.abs(c.center-y)<3 then "+
                "local id=math.floor((c.center+offset)/185+0.5);if id==selected then entered=entered+1 else selected=id end;return end end;"+
                "error('tap outside real cards')end;"+
                "assert(utils.chooseCup('13')==1 and selected==13 and entered==1,'short swipes shifted the configured thirteenth event')"
        );
        CupCarousel.ScrollTracker tracker=new CupCarousel.ScrollTracker();
        tracker.observe(scrollFrame(0),0);
        if(!Integer.valueOf(528).equals(tracker.observe(scrollFrame(528),1))
                || !Integer.valueOf(317).equals(tracker.observe(scrollFrame(845),1))
                || !Integer.valueOf(0).equals(tracker.observe(scrollFrame(845),1))
                || !Integer.valueOf(-520).equals(tracker.observe(scrollFrame(325),-1)))
            throw new AssertionError("Cup scroll distance must follow the artwork, including a clamped last drag");
        if(!Integer.valueOf(102).equals(tracker.shiftSinceLastDrag(scrollFrame(427)))
                || !Integer.valueOf(-95).equals(tracker.shiftSinceLastDrag(scrollFrame(230))))
            throw new AssertionError("Selection must use the pre-tap reference across reads in either direction");
        tracker.observe(new PixelFrame(720,1280,new int[720*1280]),0);
        if(tracker.observe(new PixelFrame(720,1280,new int[720*1280]),1)!=null)
            throw new AssertionError("An ambiguous image cannot establish a card position");
        if(tracker.shiftSinceLastDrag(new PixelFrame(720,1280,new int[720*1280]))!=null)
            throw new AssertionError("An ambiguous selection shift must not authorize a tap");
        int checked=18;
        for(String script:cases){
            cleanCupCache();
            try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())){
                lua.load("utilities",Files.readString(assets.resolve("lua/utilities.lua")));lua.load("cup-fixture",fixture);
                lua.load("cup-case","function test()"+script+" end");lua.start("test");
                boolean alive=true;for(int n=0;n<500&&alive;n++)alive=lua.step(n*1000L);
                if(alive)throw new AssertionError("Cup selection did not terminate");
            }
            checked++;
        }
        String dailyEntry;
        try(var input=CupSelectionChecks.class.getResourceAsStream("/daily-event-entry.lua")){
            dailyEntry=new String(Objects.requireNonNull(input).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        }
        for(String scenario:List.of("delayed","retry","direct","covered-border","claim-button","missing","unknown","stale","wrong-card")){
            cleanCupCache();
            try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())){
                for(String module:List.of("bootstrap","utilities","cup","workflows"))
                    lua.load(module,Files.readString(assets.resolve("lua/"+module+".lua")));
                lua.set("LOCAL_ROOT","/tmp/a9-cup-check");
                lua.set("scenario",scenario);
                lua.load("daily-event-entry",dailyEntry);
                lua.start("test");
                boolean alive=true;
                for(int n=0;n<1200&&alive;n++)alive=lua.step(n*100L);
                if(alive)throw new AssertionError("Daily event entry timed out: "+scenario);
            }
            checked++;
        }
        // Five real cards fit before the garage-unlock footer. Negative positions
        // must count cards, including those requiring a scroll, not screen slots.
        for(int position:new int[]{-1,-2,-6,-8,-20,-21}){
            cleanCupCache();
            try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())){
                lua.load("utilities",Files.readString(assets.resolve("lua/utilities.lua")));lua.load("cup-fixture",fixture);
                lua.load("negative-carousel","offset=16;selected=18;entered=0;"+
                        "function _cupCards()local cards={};for id=offset,math.min(20,offset+4) do cards[#cards+1]={center=80+(id-offset)*185,selected=id==selected} end;return cards end;"+
                        "function swipe(x,y,x2,y2)assert(x>=80 and x2>=80,'carousel swipe crosses the task panel');if y2<y then offset=16 else offset=math.max(1,offset-2)end end;"+
                        "function tap(x,y)for _,card in ipairs(_cupCards())do if card.center==y then local id=offset+(y-80)/185;if id==selected then entered=entered+1 else selected=id end;return end end;error('tap outside real cards')end;"+
                        "utils.mycolor=function(x,y,color)return 0 end;"+
                        "function test()local ok,result=pcall(utils.chooseCup,'"+position+"');"+
                        (position==-21?"assert(ok and result=='cupPositionUnavailable' and entered==0,'out-of-range position entered an event')":
                        "assert(ok,result);assert(result==1 and selected=="+(21+position)+" and entered==1,'wrong negative event');assert(#snapshots==1 and snapshots[1]==selected,'wrong identity cached')")+" end");
                lua.start("test");boolean alive=true;for(int n=0;n<500&&alive;n++)alive=lua.step(n*1000L);
                if(alive)throw new AssertionError("Negative cup selector did not terminate");
            }
            checked++;
        }
        // 正数位置 1..15 全量回归：第 1 张中心 150、间距 180、每次滑动只推进一张卡，
        // 覆盖「配置 13 实际停在 11」这类滑动距离不足的情况。
        for(int position=1;position<=15;position++){
            cleanCupCache();
            try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())){
                lua.load("utilities",Files.readString(assets.resolve("lua/utilities.lua")));lua.load("cup-fixture",fixture);
                lua.load("positive-carousel","offset=0;selected=1;entered=0;"+
                        "function visible()local cards={};for id=1,20 do local c=150+(id-1)*180-offset;"+
                        "if c>=95 and c<=1260 then cards[#cards+1]={center=math.floor(c+0.5),selected=id==selected} end end;return cards end;"+
                        "function _cupCards()return visible() end;"+
                        "function swipe(x,y,x2,y2)if y2>y then offset=0 else offset=math.min(3000,offset+120) end end;"+
                        "utils.mySwipe=function(...)offset=math.min(3000,offset+180) end;"+
                        "function tap(x,y)for _,c in ipairs(visible())do if math.abs(c.center-y)<3 then "+
                        "local id=math.floor((c.center+offset-150)/180+1.5);if id==selected then entered=entered+1 else selected=id end;return end end;"+
                        "error('tap outside cards at '..tostring(y))end;"+
                        "function test()local ok,res=pcall(utils.chooseCup,'"+position+"',false,true);"+
                        "assert(ok,res);assert(res==1,'position "+position+" returned '..tostring(res));"+
                        "assert(selected=="+position+",'position "+position+" selected '..tostring(selected));"+
                        "assert(entered==1,'position "+position+" entered '..tostring(entered)..' times')end");
                lua.start("test");boolean alive=true;for(int n=0;n<800&&alive;n++)alive=lua.step(n*1000L);
                if(alive)throw new AssertionError("Positive cup selector hung at position "+position);
            }
            checked++;
        }
        // Regression for the short fixed-scroll bug: 25 cards in pixel space,
        // each rightward swipe flings 800px like a real gesture, reaching the
        // true right stop within the six-attempt cap.
        cleanCupCache();
        try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())){
            lua.load("utilities",Files.readString(assets.resolve("lua/utilities.lua")));lua.load("cup-fixture",fixture);
            lua.load("slow-carousel","offset=0;selected=0;entered=0;swipes=0;"+
                    "function visibleCards()local cards={};for id=1,25 do local c=(id-0.5)*185-offset;if c>=92 and c<=908 then cards[#cards+1]={center=math.floor(c+0.5),selected=id==selected} end end;return cards end;"+
                    "function _cupCards()return visibleCards() end;"+
                    "function swipe(x,y,x2,y2)swipes=swipes+1;if y2<y then offset=math.min(3625,offset+800) else offset=math.max(0,offset-320) end end;"+
                    "function tap(x,y)if not swipesAtAnchor then swipesAtAnchor=swipes end;for _,card in ipairs(visibleCards())do if math.abs(card.center-y)<3 then local id=math.floor((card.center+offset)/185+0.5);if id==selected then entered=entered+1 else selected=id end;return end end;error('tap outside real cards')end;"+
                    "utils.mycolor=function(x,y,color)return 0 end;"+
                    "function test()local ok,result=pcall(utils.chooseCup,'-6');"+
                    "assert(ok,result);assert(result==1 and selected==20 and entered==1,'short scroll anchored mid-list: selected '..tostring(selected));"+
                    "assert(swipesAtAnchor and swipesAtAnchor<=6,'endpoint detection must stay within the six-attempt cap') end");
            lua.start("test");boolean alive=true;for(int n=0;n<500&&alive;n++)alive=lua.step(n*1000L);
            if(alive)throw new AssertionError("Slow-scroll cup selector did not terminate");
        }
        checked++;
        // Inertia and edge bounce make consecutive reads differ by a couple of
        // pixels: stability must tolerate 3px instead of exact equality.
        cleanCupCache();
        try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())){
            lua.load("utilities",Files.readString(assets.resolve("lua/utilities.lua")));lua.load("cup-fixture",fixture);
            lua.load("jitter-carousel","wobble=0;selected=0;entered=0;"+
                    "function _cupCards()wobble=-wobble;local cards={};for id=1,5 do cards[#cards+1]={center=139+(id-1)*185+wobble*2,selected=id==selected} end;return cards end;"+
                    "function swipe(x,y,x2,y2)end;"+
                    "function tap(x,y)for i,card in ipairs(_cupCards())do if math.abs(card.center-y)<15 then if i==selected then entered=entered+1 else selected=i end;return end end;error('tap outside real cards')end;"+
                    "utils.mycolor=function(x,y,color)return 0 end;"+
                    "function test()local ok,result=pcall(utils.chooseCup,'-2');"+
                    "assert(ok,result);assert(result==1 and selected==4 and entered==1,'2px jitter blocked endpoint stability') end");
            lua.start("test");boolean alive=true;for(int n=0;n<500&&alive;n++)alive=lua.step(n*1000L);
            if(alive)throw new AssertionError("Jitter cup selector did not terminate");
        }
        checked++;
        // A list that never settles must still anchor on the current rightmost
        // card after six attempts instead of failing the whole task.
        cleanCupCache();
        try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())){
            lua.load("utilities",Files.readString(assets.resolve("lua/utilities.lua")));lua.load("cup-fixture",fixture);
            lua.load("restless-carousel","shift=0;selected=0;entered=0;reads=0;"+
                    "function _cupCards()reads=reads+1;shift=math.min(60,shift+10);return {{center=200+shift,selected=selected==1},{center=450+shift,selected=selected==2}} end;"+
                    "function swipe(x,y,x2,y2)end;"+
                    "function tap(x,y)for i,card in ipairs(_cupCards())do if math.abs(card.center-y)<15 then if i==selected then entered=entered+1 else selected=i end;return end end;error('tap outside real cards')end;"+
                    "utils.mycolor=function(x,y,color)return 0 end;"+
                    "function test()local ok,result=pcall(utils.chooseCup,'-1');"+
                    "assert(ok,result);assert(result==1 and entered==1,'restless list must anchor and enter: '..tostring(result)) end");
            lua.start("test");boolean alive=true;for(int n=0;n<500&&alive;n++)alive=lua.step(n*1000L);
            if(alive)throw new AssertionError("Restless cup selector did not terminate");
        }
        checked++;
        // An abnormal gap between the selected card and its left neighbour means
        // an intermediate card was missed: fail loudly instead of entering.
        cleanCupCache();
        try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())){
            lua.load("utilities",Files.readString(assets.resolve("lua/utilities.lua")));lua.load("cup-fixture",fixture);
            lua.load("gap-anomaly","selected=1;entered=0;"+
                    "function _cupCards()return {{center=200,selected=selected==1},{center=600,selected=selected==2}} end;"+
                    "function tap(x,y)if y==600 then selected=2 elseif y==200 then selected=1 else error('unexpected tap') end end;"+
                    "utils.mycolor=function(x,y,color)return 0 end;"+
                    "function test()local ok,result=pcall(utils.chooseCup,'-2');"+
                    "assert(ok and result=='cupPositionUnavailable' and entered==0,'abnormal card gap entered an event') end");
            lua.start("test");boolean alive=true;for(int n=0;n<500&&alive;n++)alive=lua.step(n*1000L);
            if(alive)throw new AssertionError("Gap-anomaly cup selector did not terminate");
        }
        checked++;
        return checked;
    }
    private static void cleanCupCache()throws Exception{
        Path dir=Paths.get("/tmp/a9-cup-check");
        Files.createDirectories(dir);
        try(var stream=Files.list(dir)){
            for(Path file:(Iterable<Path>)stream::iterator)Files.deleteIfExists(file);
        }
    }
}
