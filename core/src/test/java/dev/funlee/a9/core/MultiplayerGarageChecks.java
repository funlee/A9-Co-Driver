package dev.funlee.a9.core;

import java.nio.file.*;
import java.util.*;

final class MultiplayerGarageChecks {
    private static long serial;
    /** Existing workflow fixtures model an already rendered garage; negative cases below do not. */
    static Map<String,Object> fixture(Object source){
        Map<String,Object> s=new HashMap<>((Map<String,Object>)source);s.put("frameId",(++serial)*2000);
        if("cars".equals(s.get("scene"))){
            List<Object> cards=new ArrayList<>();List<?> words=(List<?>)s.getOrDefault("words",List.of());
            for(int i=1;i<=6;i++){
                int left=210+(i-1)/2*464,top=195+(i-1)%2*237,fuel=-1,capacity=0;
                for(Object o:words){Map<?,?> w=(Map<?,?>)o;int x=((Number)w.get("x")).intValue(),y=((Number)w.get("y")).intValue();
                    if(x>left&&x<left+120&&y>top+170&&y<top+225){String t=w.get("text").toString();if(t.matches("[0-9]+/[1-9]")){String[] p=t.split("/");int n=Integer.parseInt(p[0]),d=Integer.parseInt(p[1]);if(n<=d){fuel=n;capacity=d;}}}
                }
                if(((List<?>)s.getOrDefault("emptyFuelSlots",List.of())).contains(i)){fuel=0;capacity=6;}
                cards.add(Map.of("index",i,"x",Math.min(left+200,1240),"y",top+110,"availability",fuel<0?"unknown":fuel==0?"empty":"ready"));
            }
            s.put("garage",Map.of("start",true,"rank","青铜","key","青铜:467/1094:LANCER","cards",cards));
        }
        if("car".equals(s.get("scene")))s.put("carDetail",Map.of("name","MITSUBISHILANCEREVOLUTION","performance","467/1094"));
        return s;
    }
    private static Map<String,Object> word(String t,int x,int y){return Map.of("text",t,"x",x,"y",y);}
    private static Map<String,Object> screen(String scene){return Map.of("scene",scene,"text",scene.equals("series")?"世界系列赛 大师系列赛":scene.equals("event")?"世界系列赛 排行榜 里程碑 开始":"车辆选择 青铜","words",scene.equals("series")?List.of(word("世界系列赛",350,450)):scene.equals("event")||scene.equals("car")?List.of(word("开始",1100,650)):List.of(word("6/6",240,390)));}
    static int run(Path assets)throws Exception{
        int checks=0;int[] pixels=new int[720*1280];
        for(int top:new int[]{195,432})for(int y=top;y<top+225;y++)pixels[210*720+719-y]=0xff00ccff;
        List<Object> words=List.of(word("467/1,094",270,225),word("6/6",240,390),word("LANCER",580,340),word("EVOLUTION",580,360));
        Map<String,Object> good=MultiplayerGarage.read(new PixelFrame(720,1280,pixels),words,"青铜");
        for(String league:List.of("青铜","白银","黄金","白金","翡翠","钻石","精英","宗师","传奇")) {
            List<Object> railWords=new ArrayList<>(words);railWords.add(word(league,138,421));
            Map<String,Object> recognized=MultiplayerGarage.read(new PixelFrame(720,1280,pixels),railWords,"");
            require(recognized.get("start").equals(true)&&recognized.get("rank").equals(league),league+": recognize league on garage rail");checks++;
        }
        require(MultiplayerGarage.read(new PixelFrame(720,1280,pixels),words,"铂金").get("rank").equals("白金"),"Legacy platinum OCR uses the current white-gold config key");checks++;
        require(MultiplayerGarage.validPerformance("1,868/1,868")&&!MultiplayerGarage.validPerformance("1,868/1,368"),
                "Reject impossible OCR performance before replacing a valid card reading");checks++;
        var shortModel=MultiplayerGarage.read(new PixelFrame(720,1280,pixels),List.of(
                word("1,868/1,868",270,225),word("5/6",240,390),word("R1",580,350)),"青铜");
        require(shortModel.get("start").equals(true)&&((List<?>)((Map<?,?>)((List<?>)shortModel.get("cards")).get(0)).get("tokens")).contains("R1"),
                "Short alphanumeric model names retain their identity token");checks++;
        for(double aspect:new double[]{16.0/9,2.0,20.0/9}){
            boolean wide=aspect>2.07,medium=aspect>=2.0&&!wide;
            int x=wide?222:210,top=wide?215:medium?206:195;
            int pitch=wide?359:medium?405:464,rowPitch=wide?231:medium?233:237;
            int[] grid=new int[720*1280];
            for(int slot=0;slot<4;slot++){
                int left=x+slot/2*pitch,y=top+slot%2*rowPitch;
                for(int dx=0;dx<=100;dx++)grid[(left+dx)*720+719-y]=0xff0079cd;
                for(int dy=0;dy<=210;dy++)grid[left*720+719-y-dy]=0xff0079cd;
                int badge=new int[]{0xff0e1829,0xffc0fa14,0xffff0054,0xff6a787f}[slot];
                for(int dx=10;dx<=50;dx++)for(int dy=190;dy<=209;dy++)grid[(left+dx)*720+719-y-dy]=badge;
            }
            // A clipped card drawing can resemble two corners, but has artwork in its left gutter.
            for(int y:new int[]{top,top+rowPitch}){
                for(int dx=0;dx<=90;dx++)grid[(60+dx)*720+719-y]=0xff0079cd;
                for(int dy=1;dy<210;dy++){
                    grid[60*720+719-y-dy]=0xff0079cd;
                    grid[55*720+719-y-dy]=0xff99aacc;
                }
            }
            var page=MultiplayerGarage.slots(new PixelFrame(720,1280,grid),List.of(),"青铜",aspect);
            require(Boolean.TRUE.equals(page.get("start")),"Paired column borders and league rail verify the origin");
            List<?> cards=(List<?>)page.get("cards");
            require(cards.size()==4,"Count all slots without model or performance text");
            List<String> states=List.of("locked","ready","empty","unknown");
            for(int j=0;j<cards.size();j++){
                Map<?,?> slot=(Map<?,?>)cards.get(j);
                require(slot.get("index").equals(j+1)&&slot.get("availability").equals(states.get(j)),"Preserve column order and fuel badge state");
            }
            for(int dx=12;dx<=154;dx++)for(int dy=5;dy<=22;dy++)grid[(x+dx)*720+719-top-dy]=0xfff3bb05;
            var pending=MultiplayerGarage.slots(new PixelFrame(720,1280,grid),List.of(),"青铜",aspect);
            require(((Map<?,?>)((List<?>)pending.get("cards")).get(0)).get("availability").equals("starUp"),
                    "Star-up ribbon is recognized without OCR even on a fuelled card");
            checks+=7;
        }
        // Xiaomi 15: the first column is fully upgraded and uses a pale gold border.
        int[] goldGrid=new int[720*1280];
        for(int i=0;i<4;i++){
            int x=223+i/2*359,y=217+i%2*231;
            int border=i<2?0xfff6edcb:0xff0079cd;
            for(int dx=0;dx<=160;dx++)goldGrid[(x+dx)*720+719-y]=border;
            for(int dy=1;dy<=210;dy++)goldGrid[x*720+719-y-dy]=border;
            if(i!=2)for(int dx=12;dx<=154;dx++)for(int dy=4;dy<=22;dy++)
                goldGrid[(x+dx)*720+719-y-dy]=i<2?0xffe29512:0xfff3bb05;
            for(int dx=10;dx<=50;dx++)for(int dy=190;dy<=204;dy++)goldGrid[(x+dx)*720+719-y-dy]=0xffc0fa14;
        }
        PixelFrame goldFrame=new PixelFrame(720,1280,goldGrid);
        var goldPage=MultiplayerGarage.slots(goldFrame,List.of(word("升星就绪",670,237)),"青铜",2670.0/1200);
        require(Boolean.TRUE.equals(goldPage.get("start"))&&goldPage.get("left").equals(223),
                "A pale gold column on the Xiaomi 15 establishes the bronze origin");checks++;
        List<?> goldCards=(List<?>)goldPage.get("cards");
        require(goldCards.size()==4,"Gold and blue cards retain all four indices");checks++;
        for(int i=0;i<goldCards.size();i++){
            Map<?,?> slot=(Map<?,?>)goldCards.get(i);
            require(slot.get("index").equals(i+1)&&slot.get("availability").equals(i<2?"ready":"starUp"),
                    "Fuelled maxed cars remain selectable and both star-up signals are skipped");checks++;
        }
        require(Boolean.FALSE.equals(MultiplayerGarage.slots(goldFrame,List.of(),"",2670.0/1200).get("start")),
                "Gold borders alone cannot establish the origin without a league");checks++;
        int[] clippedGrid=new int[720*1280];
        for(int left:new int[]{391,855})for(int top:new int[]{195,432}){
            for(int dx=0;dx<=100;dx++)clippedGrid[(left+dx)*720+719-top]=0xff0079cd;
            for(int dy=0;dy<=210;dy++)clippedGrid[left*720+719-top-dy]=0xff0079cd;
        }
        var clippedPage=MultiplayerGarage.slots(new PixelFrame(720,1280,clippedGrid),List.of(),"",16.0/9);
        var clippedCards=(List<?>)clippedPage.get("cards");
        require(clippedCards.size()==4&&Boolean.TRUE.equals(((Map<?,?>)clippedCards.get(2)).get("clipped")),
                "Right-edge cards retain tracking signatures while remaining ineligible for taps");checks++;
        int[] masterGrid=new int[720*1280];
        Arrays.fill(masterGrid,0xff233853);
        for(int left:new int[]{211,675})for(int top:new int[]{195,432}){
            for(int dx=0;dx<=100;dx++)masterGrid[(left+dx)*720+719-top]=0xfffbf8f7;
            for(int dy=0;dy<=210;dy++)masterGrid[left*720+719-top-dy]=0xfffbf8f7;
            int badge=top==195?0xfffe1f68:0xffc0fa14;
            for(int dx=10;dx<=50;dx++)for(int dy=190;dy<=209;dy++)masterGrid[(left+dx)*720+719-top-dy]=badge;
        }
        var masterFrame=new PixelFrame(720,1280,masterGrid);
        var masterPage=MultiplayerGarage.slots(masterFrame,List.of(word("小白",141,409)),"",16.0/9,true);
        List<?> masterCards=(List<?>)masterPage.get("cards");
        require(Boolean.TRUE.equals(masterPage.get("start"))&&masterCards.size()==4,
                "Master rank rail and white trial-card borders on the blue gutter establish the first car");checks++;
        require(((Map<?,?>)masterCards.get(0)).get("availability").equals("empty")&&
                ((Map<?,?>)masterCards.get(1)).get("availability").equals("ready"),
                "Master empty car 1 and fueled car 2 keep their configured ordinals");checks++;
        require(Boolean.FALSE.equals(MultiplayerGarage.slots(masterFrame,List.of(),"",16.0/9,true).get("start")),
                "White borders alone cannot establish Master garage origin");checks++;
        var bronzePage=MultiplayerGarage.slots(masterFrame,List.of(word("青铜",141,421)),"",16.0/9,false);
        require(Boolean.TRUE.equals(bronzePage.get("start"))&&bronzePage.get("left").equals(211)&&
                ((List<?>)bronzePage.get("cards")).size()==4,
                "Standard World garage can also have white borders on the blue gutter");checks++;
        masterFuelFallback(assets,masterPage);checks++;
        checks+=leagueSelection(assets);
        String fallbackCases;
        try(var input=MultiplayerGarageChecks.class.getResourceAsStream("/world-league-fallback.lua")){
            fallbackCases=new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        }
        for(String scenario:List.of("silverEmpty","silverFueled","skipBlank","cascade","detailEmpty","unknown","allEmpty","stuckTab","missingTab")){
            try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())){
                lua.set("scenario",scenario);
                lua.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));
                lua.load("fallback-"+scenario,fallbackCases);lua.start("test");
                for(int i=0;i<180;i++)if(!lua.step(i*1000L))break;
                lua.load("verify","assert(done,scenario)");
            }
            checks++;
        }
        String paginationCases;
        try(var input=MultiplayerGarageChecks.class.getResourceAsStream("/world-car-pagination.lua")){
            paginationCases=new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        }
        for(String scenario:List.of("car27","empty27","locked27","remembered","reversePage","differentPage","emptyBoth","delayedPage","missingBottom","slowPagination","fuzzyName","sameColumnShift","clippedOverlap","lockedOverlap","overshoot","targetMoved","targetSettled","stuckPage","wideCar10","wideCar10Overscroll")){
            try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())){
                lua.set("scenario",scenario);
                lua.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));
                lua.load("pagination-"+scenario,paginationCases);lua.start("test");
                for(int i=0;i<600;i++)if(!lua.step(i*(scenario.equals("slowPagination")?4000L:1000L)))break;
                lua.load("verify","assert(done,scenario)");
            }
            checks++;
        }
        require(good.get("start").equals(true),"visible rank rail, paired borders and identified first card establish origin");checks++;
        require(MultiplayerGarage.read(new PixelFrame(720,1280,pixels),words,"").get("start").equals(false),"no league origin => no origin");checks++;
        require(MultiplayerGarage.read(new PixelFrame(720,1280,new int[720*1280]),words,"青铜").get("start").equals(false),"OCR alone cannot authorize selection");checks++;
        require(MultiplayerGarage.read(new PixelFrame(720,1280,pixels),List.of(word("6/6",240,390)),"青铜").get("start").equals(false),"fuel alone is not vehicle identity");checks++;
        require(MultiplayerGarage.detail(List.of(word("LANCER EVOLUTION",300,160),word("467/1,094",1000,150))).get("performance").equals("467/1094"),"detail rank ignores separators");checks++;
        // Measured screenshot border RGB and OCR centers; keeps this guard tied to device evidence.
        Arrays.fill(pixels,0);
        for(int y=207;y<400;y++)pixels[211*720+719-y]=0xff0079cd;
        for(int y=444;y<637;y++)pixels[211*720+719-y]=0xffe5fe94;
        List<Object> observed=List.of(word("青洞",138,421),word("團467/1,094|",291,246),word("國6/6",251,396),word("LANCER",553,352),word("EVOLUTION",601,352));
        require(MultiplayerGarage.read(new PixelFrame(720,1280,pixels),observed,"").get("start").equals(true),"observed border and bronze OCR substitution authorize origin");checks++;
        int[] pixels18By9=new int[720*1280];
        for(int top:new int[]{206,439})for(int y=top;y<top+225;y++)pixels18By9[211*720+719-y]=0xff00ccff;
        List<Object> words18By9=List.of(word("吉铜",141,421),word("467/1,094",276,253),
                word("6/6",248,402),word("LANCER",526,353),word("620/1,337",673,484),
                word("6/6",647,635),word("370Z NISMO",926,580));
        var garage18By9=MultiplayerGarage.read(new PixelFrame(720,1280,pixels18By9),words18By9,"",2.0);
        require(garage18By9.get("start").equals(true)&&((List<?>)garage18By9.get("cards")).size()==2,
                "18:9 verifies the origin and the second column using its own card pitch");
        require(MultiplayerUi.classify("车輛选择 选择参与此賽事的车辆").equals("cars"),
                "Mixed-script OCR still classifies the 18:9 garage");
        checks+=2;
        List<Object> changedBrand=new ArrayList<>(observed);changedBrand.add(word("MSU8ISHI",555,333));
        require(MultiplayerGarage.read(new PixelFrame(720,1280,pixels),observed,"").get("key").equals(MultiplayerGarage.read(new PixelFrame(720,1280,pixels),changedBrand,"").get("key")),"changing OCR of tiny brand text cannot reset an otherwise verified origin");checks++;
        int[] shifted=new int[720*1280];for(int x=0;x<1200;x++)for(int y=0;y<720;y++)shifted[(x+50)*720+y]=pixels[x*720+y];
        require(MultiplayerGarage.read(new PixelFrame(720,1280,shifted),observed,"").get("start").equals(false),"partial swipe with stale OCR cannot authorize a shifted first card");checks++;
        for(String mode:List.of("slow","ocrGlitch","originLost","noOrigin","stale","wrongDetail","stuckEvent","stuckCars")){
            int[] reads={0},events={0},cardTaps={0},startTaps={0},detailReads={0};boolean[] finished={false};String[] failure={""};
            try(LuaRuntime lua=new LuaRuntime((name,l)->{
                if(name.equals("_multiplayerUi")){
                    int n=reads[0]++;Map<String,Object> s;
                    if(n==0)s=fixture(screen("series"));
                    else if(n<9||mode.equals("stuckEvent"))s=fixture(screen("event"));
                    else if(cardTaps[0]==0||mode.equals("stuckCars")||detailReads[0]++<3)s=fixture(screen("cars"));
                    else s=fixture(screen("car"));
                    if(mode.equals("noOrigin")||(mode.equals("ocrGlitch")&&n==13)||(mode.equals("originLost")&&n>=13)){Map<String,Object> g=new HashMap<>((Map<String,Object>)s.getOrDefault("garage",Map.of()));g.put("start",false);s.put("garage",g);}
                    if(mode.equals("stale"))s.put("frameId",1);
                    if(mode.equals("wrongDetail"))s.put("carDetail",Map.of("name","NISSAN370ZNISMO","performance","620/991"));
                    LuaRuntime.push(l,s);return 1;
                }
                if(name.equals("tap")){int x=(int)l.toInteger(2),y=719-(int)l.toInteger(1);if(x==1100&&y==650){if(cardTaps[0]==0)events[0]++;else{startTaps[0]++;finished[0]=true;}}if(x==410&&y==305)cardTaps[0]++;}
                if(name.equals("cmpColor")){l.push(1);return 1;}return 0;
            },List.of("_multiplayerUi","_sessionProject","tap","swipe","print","_hud","cmpColor"))){
                lua.set("data",Map.of("项目","多人上"));lua.set("run",Map.of("data",Map.of("次数",100,"青铜用车",List.of("1"))));
                lua.load("helpers","utils={defultCfg=function(v,d)return v or d end}");lua.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));lua.start("currentMultiplayer.run");
                try{for(int i=0;i<160&&!finished[0];i++)lua.step(i*1000L);}catch(RuntimeException e){failure[0]=e.getMessage();}
            }
            require(events[0]==1,mode+": event Start must never be retried into new page");checks++;
            require(cardTaps[0]<=1,mode+": vehicle card must never be tapped twice");checks++;
            require(startTaps[0]==((mode.equals("slow")||mode.equals("ocrGlitch")||mode.equals("wrongDetail"))?1:0),mode+": verified fueled slot starts regardless of detail text "+failure[0]);checks++;
            if(mode.equals("noOrigin")||mode.equals("stale")||mode.equals("originLost")){require(cardTaps[0]==0,mode+": no ordinal selection without origin evidence");checks++;}
            if(mode.equals("stuckEvent")||mode.equals("stuckCars")){require(!failure[0].isEmpty(),mode+": fail closed instead of retrying click");checks++;}
        }
        return checks;
    }
    /** A non-default slot proves each recognized league reads its own vehicle preference. */
    private static void masterFuelFallback(Path assets,Map<String,Object> garage)throws Exception{
        int[] frames={0};List<String> taps=new ArrayList<>();
        try(LuaRuntime lua=new LuaRuntime((name,l)->{
            if(name.equals("_multiplayerUi")){
                int n=frames[0]++;
                LuaRuntime.push(l,n==0?Map.of("scene","event","text","大师系列赛","words",List.of(word("开始",1100,650))):
                    Map.of("scene","cars","text","车辆选择","frameId",n*1000,"garage",garage));
                return 1;
            }
            if(name.equals("tap"))taps.add(l.toInteger(1)+","+l.toInteger(2));
            if(name.equals("swipe"))throw new AssertionError("Verified master origin needs no drag");
            return 0;
        },List.of("_multiplayerUi","_sessionProject","tap","swipe","print","_hud"))){
            lua.set("data",Map.of("项目","多人下"));
            lua.set("run",Map.of("data",Map.of("次数",1,"多人下用车",List.of(1,2,3,4))));
            lua.load("helpers","utils={defultCfg=function(v,d)return v or d end}");
            lua.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));
            lua.start("currentMultiplayer.run");
            for(int i=0;i<15&&taps.size()<2;i++)lua.step(i*1000L);
        }
        require(taps.equals(List.of("69,1100","177,411")),"Skip empty first Master car and directly tap the fueled second car");
    }

    private static int leagueSelection(Path assets)throws Exception {
        int checks=0;
        for(String league:List.of("青铜","白银","黄金","白金","翡翠","钻石","精英","宗师","传奇")) {
            int[] reads={0},selected={0},started={0};
            try(LuaRuntime lua=new LuaRuntime((name,l)->{
                if(name.equals("_multiplayerUi")) {
                    int n=reads[0]++;Map<String,Object> screen=fixture(screen(n==0?"series":n<9?"event":selected[0]==0?"cars":"car"));
                    if("cars".equals(screen.get("scene"))) {
                        Map<String,Object> garage=new HashMap<>((Map<String,Object>)screen.get("garage"));
                        garage.put("rank",league);garage.put("key",league+":467/1094");
                        List<Object> cards=new ArrayList<>();
                        for(Object item:(List<?>)garage.get("cards")) {
                            Map<String,Object> card=new HashMap<>((Map<String,Object>)item);
                            card.put("availability","ready");cards.add(card);
                        }
                        garage.put("cards",cards);screen.put("garage",garage);
                    }
                    LuaRuntime.push(l,screen);return 1;
                }
                if(name.equals("tap")) {
                    int x=(int)l.toInteger(2),y=719-(int)l.toInteger(1);
                    if(x==410&&(y==305||y==542))selected[0]=y==542?2:1;
                    if(x==1100&&y==650&&selected[0]>0)started[0]++;
                }
                if(name.equals("cmpColor")){l.push(1);return 1;}return 0;
            },List.of("_multiplayerUi","_sessionProject","tap","swipe","print","_hud","cmpColor"))) {
                lua.set("data",Map.of("项目","多人上"));lua.set("run",Map.of("data",Map.of("次数",1,league+"用车",List.of("2"))));
                lua.load("helpers","utils={defultCfg=function(v,d)return v or d end}");
                lua.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));lua.start("currentMultiplayer.run");
                for(int i=0;i<100&&started[0]==0;i++)lua.step(i*1000L);
            }
            require(selected[0]==2&&started[0]==1,league+": select configured second car, not default first car");checks++;
        }
        return checks;
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
