package dev.funlee.a9.core;

import java.nio.file.*;
import java.io.*;
import java.util.*;

/** SDK-independent acceptance checks; failures throw and make the script exit nonzero. */
public final class CoreChecks {
    private static int checks;
    static void check(boolean value,String message) { checks++; if(!value) throw new AssertionError(message); }
    static final class TouchLog implements RouteEngine.Touch {
        final List<String> events=new ArrayList<>();
        public void tap(int id,int x,int y) { events.add("tap:"+id+":"+x+":"+y); }
        public void down(int id,int x,int y) { events.add("down:"+id); }
        public void up(int id) { events.add("up:"+id); }
    }
    public static void main(String[] args) throws Exception {
        Path assets=Paths.get(args[0]);
        for(int rank=1;rank<=8;rank++)check(ResultRank.parse("比赛奖励 你的用时", "第 "+rank)==rank,"Personal rank "+rank);
        check(ResultRank.parse("获胜者! 你的用时 排行榜","第1")==0,"Winner animation is not personal rank evidence");
        check(ResultRank.parse("比赛奖励 你的用时","第6 1200")==0&&ResultRank.parse("比赛奖励 你的用时","第9")==0,"Ambiguous and invalid badge stays unknown");
        check(ResultRank.fromLines("你的排名 你的用时 获胜者",List.of("你的排名","第1"),true)==1,"Read own footer rank, not winner banner");
        check(ResultRank.fromLines("你的排名 你的用时",List.of("第2","第6"),true)==0,"Conflicting crop ranks rejected");
        check(ResultRank.fromLines("获胜者 你的用时",List.of("第1"),true)==0,"Footer requires own rank label");
        TaskStats rankStats=new TaskStats();rankStats.project("多人上");rankStats.cycle(1);rankStats.projectTarget(30);
        rankStats.result(1);check(rankStats.finished()==0,"Old result before race is ignored");
        rankStats.begin("测试地图",false);rankStats.result();rankStats.result(2);rankStats.result(2);rankStats.returned();
        check(rankStats.finished()==1&&rankStats.rankedCount()==1&&rankStats.topThreeCount()==1,"Late reward rank upgrades an unranked result exactly once");
        rankStats.begin("",true);rankStats.result(6);rankStats.returned();
        rankStats.begin("",true);rankStats.result();rankStats.returned();
        check(rankStats.panel("").contains("50.0%（1/2）")&&rankStats.panel("").contains("名次待确认 1"),"Unknown ranks excluded with visible coverage");
        rankStats.project("多人下");rankStats.projectTarget(20);rankStats.begin("",false);rankStats.result(1);rankStats.result(6);rankStats.result(1);rankStats.returned();
        check(rankStats.rankedCount()==2&&rankStats.topThreeCount()==1&&rankStats.panel("").contains("名次待确认 2"),"Conflicting ranks invalidate result, never inflate top-three rate");
        rankStats.cycle(2);check(rankStats.count("多人上")==3&&rankStats.count("多人下")==1,"Counters survive project and cycle transitions");
        rankStats.project("杯赛");rankStats.legacyCompleted();rankStats.project("生涯");rankStats.legacyCompleted();
        check(rankStats.count("杯赛")==1&&rankStats.count("生涯")==1&&rankStats.rankedCount()==2,"Other workflows do not enter multiplayer denominator");
        check(new TaskStats().panel("").contains("前三名率 —（0/0）"),"Fresh task has no invented rate");

        // Recorded Android RGB values from the game's top-right menu at 1280x720.
        int[] home=new int[720*1280];home[1240*720+690]=0xffffffff;home[1252*720+690]=0xff13233c;home[1270*720+670]=0xff111f36;
        check(ColorPattern.matches(new PixelFrame(720,1280,home),"690|1240|FFFFFF-101010,690|1252|3B2312-101010,670|1270|3B2312-101010",1),"Real menu pixels match recovered BGR signatures");
        check(ColorPattern.color(0xffff0054,"5400FF",1)&&!ColorPattern.color(0xff5400ff,"5400FF",1),"BGR red/blue channels remain distinct");
        check(MultiplayerUi.classify("幽灵系列赛 世界系列赛 试驾挑战 大师系列赛 多人游戏 单人模式").equals("series"),"Series cards take priority over bottom navigation");
        check(MultiplayerUi.classify("世界系列赛 大师系列賽 多人游戏 单人模式").equals("series"),"OCR traditional sai does not hide the series selector");
        check(MultiplayerUi.classify("世界系列赛 排行榜 里程碑 奖励 开始").equals("event"),"Current multiplayer event lobby");
        check(MultiplayerUi.classify("世界系列赛 里程碑 奖励 开始").equals("event"),"Event lobby remains recognizable when the overlay covers Ranking");
        check(MultiplayerUi.classify("幽灵系列赛 顶级玩家 我的俱乐部 我附近的 俱乐部名称").equals("leaderboard"),"Leaderboard can return to the correct series selection");
        check(MultiplayerUi.classify("车辆选择 选择参与比赛的车辆 6/6 已可解锁").equals("cars"),"Current multiplayer vehicle list");
        check(MultiplayerUi.classify("TOUCHDRIVE 升级 开 始").equals("car"),"OCR whitespace does not hide Start");
        check(MultiplayerUi.classify("TOUCHDRIVE 性能分 467/1094 开始").equals("car"),"Car is recognized when the overlay covers Upgrade");
        check(MultiplayerUi.classify("晋级奖励 小白联赛 在系列赛中晋级新联赛的奖励 领取").equals("rewardClaim"),"Master series promotion has a distinct free reward claim");
        check(!MultiplayerUi.classify("领取礼包 购买").equals("rewardClaim"),"Promotion claim requires league reward context");
        check(MultiplayerUi.classify("动态奖励 从这组奖励中选一个 确认").equals("rewardChoice"),"Free choice rewards require both the selection prompt and confirmation");
        check(!MultiplayerUi.classify("奖励 购买 确认").equals("rewardChoice"),"Purchase confirmation is never treated as a free choice reward");
        check(MultiplayerUi.classify("你的用时 01:38.532 排行榜 继续").equals("result"),"Result remains recognizable when rank is obscured");
        check(MultiplayerUi.classify("每日超能礼包 立即购买 多人游戏 单人模式").equals("promo"),"Promotion takes priority over navigation visible behind it");
        check(MultiplayerUi.classify("为赛道而生 BAILEY BLADE GT1 专属特效限时特惠").equals("promo"),"Limited-time effect promotion is dismissible before its button finishes appearing");
        check(MultiplayerUi.classify("TouchDrive 开始").equals("unknown"),"Partial OCR cannot authorize Start");
        check(MultiplayerUi.classify("比赛奖励 你的用时 01:57.098 总金币 继续").equals("result"),"Race reward is evidence of a completed race");
        check(MultiplayerUi.displayMode("多人上").equals("世界系列赛（原多人上）")&&MultiplayerUi.displayMode("多人下").equals("大师系列赛（原多人下）"),"Legacy multiplayer modes retain their explicit series mapping");
        if(args.length>1){
        for(String fixture:List.of("promo-pink-close.png","promo-white-close.png","reward-small-close.png","event-no-close.png")) {
            java.awt.image.BufferedImage crop=javax.imageio.ImageIO.read(Paths.get(args[1]).resolve(fixture).toFile());
            int[] p=new int[720*1280];
            for(int y=0;y<crop.getHeight();y++)for(int x=0;x<crop.getWidth();x++)p[(940+x)*720+719-(50+y)]=crop.getRGB(x,y);
            int[] cross=PopupCloseDetector.find(new PixelFrame(720,1280,p));
            check(fixture.startsWith("event")?cross==null:cross!=null&&Math.abs(cross[0]-(fixture.startsWith("reward")?1115:1087))<5&&Math.abs(cross[1]-(fixture.startsWith("reward")?141:110))<5,"Actual MuMu close-X fixture: "+fixture);
        }
        {
            java.awt.image.BufferedImage crop=javax.imageio.ImageIO.read(Paths.get(args[1]).resolve("server-error-close.png").toFile());
            int[] p=new int[720*1280];
            for(int y=0;y<crop.getHeight();y++)for(int x=0;x<crop.getWidth();x++)p[(940+x)*720+719-(50+y)]=crop.getRGB(x,y);
            int[] close=PopupCloseDetector.findDialog(new PixelFrame(720,1280,p));
            check(close!=null&&Math.abs(close[0]-1091)<5&&Math.abs(close[1]-244)<5,"Actual server error close-X is detected below the promotion region");
        }
        } else System.out.println("SKIP: optional local screenshot samples (not included in the repository)");
        check(!ColorPattern.color(0xff14233b,"3B2312-0A0201",1),"BGR per-channel tolerances are converted too");
        Route route=Route.parse("fixture","1,31,3,N:100:2:750,6,D:100|N:100:2:750,26,360");
        check(route.valid()&&route.events.size()==5,"Compound route parsing");
        check(!Route.parse("bad","878,21,2,22").valid(),"Reject recovered invalid progress");
        check(!Route.parse("bad","0,N:0:999999:0").valid(),"Bound resource use");
        check(!Route.parse("bad","1,99").valid(),"Unknown command cannot execute");
        TouchLog touch=new TouchLog(); RouteEngine engine=new RouteEngine(route,touch,0,false,false);
        engine.update(1,0,false); check(touch.events.isEmpty(),"Choice waits for route icon");
        engine.tick(1,true); check(touch.events.get(0).equals("tap:3:590:564"),"31 selects original coordinates");
        engine.update(3,1000,true); engine.tick(1749,true); check(touch.events.size()==1,"ZX does not fire early");
        engine.tick(1750,true); check(touch.events.size()==2,"ZX kth interval");
        engine.update(6,1800,true); check(touch.events.get(2).equals("down:2"),"Drift down immediate");
        engine.update(6,1810,true); engine.tick(1900,true); check(!touch.events.contains("up:2"),"Nitro cancels pending drift release");
        engine.tick(2560,true); check(touch.events.contains("up:2"),"Nitro releases drift");
        engine.clear(); int before=touch.events.size(); engine.update(26,3000,false); engine.tick(3010,false); engine.tick(3510,false);
        check(touch.events.size()==before+6,"360 sequence: two drift taps and four nitro taps");
        engine.stop(); int size=touch.events.size(); engine.update(40,9000,true); check(touch.events.size()==size,"Stop clears queued input");
        TouchLog t2=new TouchLog(); RouteEngine e2=new RouteEngine(Route.parse("lap","2,21"),t2,0,true,false);
        e2.update(1,0,true); e2.update(51,10,true); check(t2.events.size()==2,"Two-lap route restarts once");
        TouchLog fast=new TouchLog();RouteEngine fastroute=new RouteEngine(Route.parse("nitro-drift","1,N:0:2:750"),fast,0,false,false,true);
        fastroute.update(1,0,true);fastroute.tick(200,true);check(fast.events.size()==2,"Nitro-drift option uses 100ms nitro interval");
        ProgressFilter filter=new ProgressFilter();
        check(filter.accept(2)==2&&filter.accept(29)==-1&&filter.accept(3)==3,"Reject transient combined-digit jump");
        check(filter.accept(20)==-1&&filter.accept(21)==21,"Accept confirmed catch-up");
        check(filter.accept(19)==-1,"Monotonic progress");
        MapMatcher matcher=new MapMatcher(Files.newBufferedReader(assets.resolve("data/map-rules.tsv")));
        TaskStats stats=new TaskStats();stats.target(5);stats.project("多人上");stats.begin(matcher.displayName("喜马拉雅山冰封路径"),false);
        stats.progress(50,12,39);stats.progress(50,12,39);stats.result();stats.result();
        check(stats.finished()==1&&stats.returnedCount()==0&&stats.totalActions()==12,"Results count once even while a reward popup blocks returning");
        stats.returned();stats.returned();stats.begin("",true);stats.elapsed(65000);
        check(stats.returnedCount()==1&&stats.fallbackCount()==1&&stats.panel("跑图中").contains("完赛 1/5")&&stats.panel("跑图中").contains("00:01:05"),"Task dashboard preserves totals across races");
        check(new TaskStats().finished()==0&&matcher.displayName("喜马拉雅山冰封路径").equals("神山垭口冰封路径"),"New task clears counters and route key displays CN map name");
        check(matcher.rules.size()==191,"All original mapping branches retained");
        check(matcher.match("神山垭口冰封路径").unique(),"Chinese-server name maps");
        check(matcher.match("比赛地图 神山垭口冰封路径 推荐路线").unique(),"Full map title found among surrounding OCR text");
        check(matcher.match("神山垭口冰封路径").candidates.get(0).internal.equals("喜马拉雅山冰封路径"),"Display and internal keys distinct");
        check(matcher.match("樱花港难波公园").candidates.get(0).internal.equals("大阪难波公园"),"Exact names take priority over broad keywords");
        check(!matcher.match("沙漠迷雾高塔赛道").unique(),"Conflicting original aliases require explicit selection");
        check(!matcher.match("").unique(),"Empty OCR cannot choose a route");
        DigitOcr ocr=new DigitOcr(Files.newBufferedReader(assets.resolve("data/progress.lib")));
        check(ocr.templates.size()==58,"All progress templates loaded");
        Set<Integer> seen=new HashSet<>();
        for(DigitOcr.Glyph g:ocr.templates) {
            if(!seen.add(g.digit)) continue;
            int[] pixels=new int[40*40]; Arrays.fill(pixels,0xff000000);
            for(int x=0;x<g.w;x++) for(int y=0;y<g.h;y++) if(g.bits.charAt(x*g.h+y)=='1') pixels[(y+3)*40+x+2]=0xffffffff;
            DigitOcr.Result result=ocr.read(new PixelFrame(40,40,pixels),0,0,39,39,.80);
            check(result.digit==g.digit,"Dictionary digit recognition "+g.digit);
        }
        check(seen.size()==10,"Digit dictionary covers all ten digits");
        check(!ocr.read(new PixelFrame(40,40,new int[1600]),0,0,39,39,.8).valid(),"Blank image is unknown, not stale digit");
        if(args.length>1) for(String line:Files.readAllLines(Paths.get(args[1]).resolve("progress-single-digit.tsv"))) {
            if(line.startsWith("#")||line.isEmpty())continue;
            String[] sample=line.split("\\|");int expected=Integer.parseInt(sample[0]);String bits=sample[1];
            int[] pixels=new int[720*1280];pixels[50*720+680]=0xffffffff;pixels[70*720+660]=0xffffffff;
            for(int x=0;x<29;x++)for(int y=0;y<35;y++)if(bits.charAt(x*35+y)=='1')pixels[(190+y)*720+634+x]=0xffffffff;
            check(ocr.progress(new PixelFrame(720,1280,pixels),.8)==expected,"Live single-digit HUD "+expected+"% without a leading zero");
            pixels[50*720+680]=0;
            check(ocr.progress(new PixelFrame(720,1280,pixels),.8)==-1,"Single digit outside race HUD stays unknown");
            pixels[50*720+680]=0xffffffff;pixels[200*720+640]=0xffffffff;
            check(ocr.progress(new PixelFrame(720,1280,pixels),.8)==-1,"Unreadable nonblank tens glyph is not assumed zero");
        }
        List<String> log=new ArrayList<>();
        try(LuaRuntime runtime=new LuaRuntime((name,l)->{log.add(l.toString(1));return 0;},List.of("record"))) {
            runtime.load("cooperative-check","function main() beginThread(function() sleep(5); record('background') end); record('start'); sleep(10); record('done') end");
            runtime.start("main"); runtime.step(0); runtime.step(1); runtime.step(6); runtime.step(10);
            check(log.equals(List.of("start","background","done")),"Lua background jobs run during main sleep");
            // Compilation is checked against the actual native Lua 5.3 parser, not a regex.
            for(Path p:(Iterable<Path>)Files.list(assets.resolve("lua")).filter(x->x.toString().endsWith(".lua"))::iterator) {
                String escaped=p.toAbsolutePath().toString().replace("\\","\\\\").replace("'","\\'");
                runtime.load(p.getFileName().toString(),"assert(loadfile('"+escaped+"'))"); checks++;
            }
        }
        int valid=0, invalid=0; List<String> lines=Files.readAllLines(assets.resolve("data/routes.txt"));
        for(int i=1;i+1<lines.size();i+=2) { Route r=Route.parse(lines.get(i),lines.get(i+1)); if(r.valid())valid++;else invalid++; }
        check(valid+invalid==202,"All 202 resource routes accounted for");
        workflowSmoke(assets);
        multiplayerSeriesGuard(assets);
        multiplayerCompletionGuard(assets);
        forcedMapFallback(assets);
        multiplayerFuelSelection(assets);
        unlockProtection(assets);
        queueLoopOrder(assets);
        System.out.println("PASS "+checks+" checks; routes: "+valid+" executable / "+invalid+" flagged; Lua source compiled with native Lua 5.3");
    }
    private static void multiplayerSeriesGuard(Path assets) throws Exception {
        for(String mode:List.of("多人上","多人下")) {
            String target=mode.equals("多人上")?"世界系列赛":"大师系列赛";
            String wrong=mode.equals("多人上")?"大师系列赛":"世界系列赛";
            List<String> taps=new ArrayList<>();int[] frames={0};boolean rejected=false;
            try(LuaRuntime r=new LuaRuntime((name,l)->{
                if(name.equals("_multiplayerUi")) {
                    Object screen=frames[0]++==0?Map.of("scene","series","text","世界系列赛 大师系列赛","words",List.of(
                        Map.of("text","世界系列赛","x",350,"y",450),Map.of("text","大师系列赛","x",640,"y",450))):
                        Map.of("scene","event","text",wrong+" 排行榜 里程碑 开始","words",List.of(Map.of("text","开始","x",1100,"y",650)));
                    LuaRuntime.push(l,screen);return 1;
                }
                if(name.equals("tap"))taps.add(l.toInteger(1)+","+l.toInteger(2));
                return 0;
            },List.of("_sessionProject","_multiplayerUi","tap","_hud","print"))) {
                r.set("data",Map.of("项目",mode));r.set("run",Map.of("data",Map.of("次数","1")));
                r.load("helpers","utils={defultCfg=function(v,d) return v or d end}");
                r.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));
                r.start("currentMultiplayer.run");
                try{for(int i=0;i<20;i++)r.step(i*1000L);}catch(RuntimeException e){rejected=e.getMessage().contains("赛事校验失败");}
            }
            check(taps.equals(List.of("269,"+(mode.equals("多人上")?350:640))),mode+" taps only the matching series card, never wrong-event Start");
            check(rejected,target+" rejects the other event before selecting cars");
        }
    }
    private static void multiplayerCompletionGuard(Path assets) throws Exception {
        for(boolean includeResult:List.of(true,false)) {
            List<Object> screens=new ArrayList<>(List.of(
                Map.of("scene","series","text","世界系列赛 大师系列赛","words",List.of(Map.of("text","世界系列赛","x",350,"y",450))),
                Map.of("scene","event","text","世界系列赛 排行榜 里程碑 开始","words",List.of(Map.of("text","开始","x",1100,"y",650))),
                Map.of("scene","cars","text","车辆选择 青铜","words",List.of(Map.of("text","青铜","x",100,"y",300),Map.of("text","6/6","x",240,"y",390))),
                Map.of("scene","car","text","TOUCHDRIVE 升级 开始","words",List.of(Map.of("text","开始","x",1100,"y",650))),
                Map.of("scene","race","text","","words",List.of())));
            if(includeResult)screens.add(Map.of("scene","unknown","finalRank",3,"text","你的排名 3/3 你的用时 继续","words",List.of(Map.of("text","继续","x",1100,"y",650))));
            for(int stable=0;stable<5;stable++)screens.add(Map.of("scene","event","text","世界系列赛 排行榜 里程碑 开始","words",List.of()));
            if(includeResult) {
                List<Object> next=new ArrayList<>(screens.subList(0,4));
                Object title=Map.of("scene","unknown","text","地图 1圈","route","known-map","words",List.of(),"closeX",Map.of("x",1100,"y",100));
                next.add(title);next.add(title);next.addAll(screens.subList(4,screens.size()));screens.addAll(next);
            }
            screens.add(0,Map.of("scene","unknown","text","参与寻宝 稍后查看 立即寻宝","words",List.of(
                    Map.of("text","稍后查看","x",700,"y",645),Map.of("text","立即寻宝","x",1080,"y",645))));
            TaskStats session=new TaskStats();
            screens.add(1,Map.of("scene","rewardChoice","text","动态奖励 从这组奖励中选一个 确认","rewardChoices",List.of(Map.of("x",130,"y",350)),"words",List.of(Map.of("text","确认","x",640,"y",605))));
            screens.add(2,Map.of("scene","result","text","你的用时 排行榜 继续","words",List.of(Map.of("text","继续","x",1050,"y",640))));
            screens.add(3,Map.of("scene","rewardClaim","text","晋级奖励 小白联赛 领取","words",List.of(Map.of("text","领取","x",1130,"y",650))));
            screens.add(4,Map.of("scene","unknown","text","恭喜 特殊卡牌 继续","words",List.of(Map.of("text","继续","x",1150,"y",660))));
            screens.add(5,Map.of("scene","unknown","text","恭喜! 你获得了这些奖励 架续","words",List.of(Map.of("text","架续","x",1155,"y",665))));
            screens.add(2,screens.get(1));
            duplicateCarFrames(screens);
            int[] frame={0},routeRuns={0};boolean[] done={false};boolean rejected=false;List<String> swipes=new ArrayList<>(),taps=new ArrayList<>();
            try(LuaRuntime r=new LuaRuntime((name,l)->{
                if(name.equals("_multiplayerUi")){LuaRuntime.push(l,screens.get(Math.min(frame[0]++,screens.size()-1)));return 1;}
                if(name.equals("cmpColor")){l.push(1);return 1;}
                if(name.equals("_fallbackStep")){l.push(42);return 1;}
                if(name.equals("swipe")&&l.toInteger(1)==288)swipes.add(l.toInteger(1)+","+l.toInteger(2)+","+l.toInteger(3)+","+l.toInteger(4));
                if(name.equals("tap"))taps.add(l.toInteger(1)+","+l.toInteger(2));
                if(name.equals("record"))done[0]=true;
                if(name.equals("markRoute"))routeRuns[0]++;
                if(name.equals("_sessionProject"))session.project(l.toString(1));
                if(name.equals("_sessionRaceBegin"))session.begin(l.toString(1),l.toBoolean(2));
                if(name.equals("_sessionRaceResult"))session.result(l.getTop()>0?(int)l.toInteger(1):0);
                if(name.equals("_sessionRaceReturned"))session.returned();
                if(name.equals("_routeDisplay")){l.push(l.toString(1));return 1;}
                return 0;
            },List.of("_multiplayerUi","tap","swipe","_hud","print","cmpColor","record","markRoute","_sessionOperation","_fallbackBegin","_fallbackStep","_sessionProject","_sessionRaceBegin","_sessionRaceResult","_sessionRaceReturned","_routeDisplay"))) {
                r.set("data",Map.of("项目","多人上","多人",0));r.set("run",Map.of("data",Map.of("次数",includeResult?"2":"1","多人选路","3")));
                r.load("helpers","function rnd(a,b) return a end;utils={defultCfg=function(v,d) return v or d end,getMapName=function() end,newAutoDriver=function() markRoute() end};run.nitro=function() end");
                r.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));
                r.load("entry","function testRun() currentMultiplayer.run();record() end");r.start("testRun");
                try{for(int i=0;i<100&&!done[0];i++)r.step(i*1000L);}catch(RuntimeException e){rejected=e.getMessage().contains("未确认比赛结算");if(!rejected)throw e;}
            }
            check(session.finished()==(includeResult?2:0)&&session.returnedCount()==(includeResult?2:0)&&session.fallbackCount()==1,"Lua workflow records only confirmed finishes and counts fallback once");
            check(done[0]==includeResult,"Completion requires race result followed by event lobby: "+includeResult);
            check(session.rankedCount()==(includeResult?2:0)&&session.topThreeCount()==(includeResult?2:0),"Lua forwards personal ranks exactly once across two races");
            check(rejected!=includeResult,"Returning from a race without result is rejected: "+includeResult);
            check(swipes.equals(List.of("288,840,288,440")),"Unknown map falls back to a left swipe even with smart route configured");
            check(!taps.contains("619,1100"),"Loading animation cross-like graphics cannot trigger promotion dismissal");
            check(taps.contains("54,1155"),"Observed misread Continue is handled only on confirmed card-pack reward page");
            check(taps.contains("59,1150"),"Card-pack congratulations continues even when reward text is incomplete");
            check(taps.contains("69,1130"),"League promotion claims the labelled free reward");
            check(taps.contains("79,1050"),"Restart on previous results dismisses Continue without counting that old race");
            check(taps.contains("369,130")&&taps.contains("114,640"),"Dynamic free reward selects a detected card then confirms");
            check(taps.contains("74,700")&&!taps.contains("74,1080"),"Treasure promotion dismisses Later instead of entering Treasure mode");
            if(includeResult)check(routeRuns[0]==1,"Next race returns to smart routing after a fallback race");
        }
    }
    private static void forcedMapFallback(Path assets) throws Exception {
        MapMatcher matcher=new MapMatcher(Files.newBufferedReader(assets.resolve("data/map-rules.tsv")));
        List<String> names=List.of("热带天堂环岛赛车","凌空之巅极速眩晕","狮城音速狂飙","摩登城市量子跃迁","神山垭口冰封路径");
        for(String mode:List.of("多人上","多人下")) {
            String series=mode.equals("多人上")?"世界系列赛":"大师系列赛";
            List<Object> screens=new ArrayList<>();List<String> keys=new ArrayList<>();
            for(String name:names){
                MapMatcher.Match match=matcher.match(name+" 1圈");check(match.unique(),"Fallback scenario matches actual CN title: "+name);
                String key=match.candidates.get(0).internal;keys.add(key);
                screens.add(Map.of("scene","series","text","世界系列赛 大师系列赛","words",List.of(Map.of("text",series,"x",350,"y",450))));
                screens.add(Map.of("scene","event","text",series+" 排行榜 里程碑 开始","words",List.of(Map.of("text","开始","x",1100,"y",650))));
                screens.add(Map.of("scene","cars","text","车辆选择 青铜","words",List.of(Map.of("text","青铜","x",100,"y",300),Map.of("text","6/6","x",240,"y",390))));
                screens.add(Map.of("scene","car","text","TOUCHDRIVE 升级 开始","words",List.of(Map.of("text","开始","x",1100,"y",650))));
                Object title=Map.of("scene","unknown","text",name+" 1圈","route",key,"words",List.of());screens.add(title);screens.add(title);
                screens.add(Map.of("scene","race","text","","words",List.of()));
                screens.add(Map.of("scene","result","text","你的用时 比赛奖励 继续","words",List.of(Map.of("text","继续","x",1100,"y",650))));
                for(int n=0;n<5;n++)screens.add(Map.of("scene","event","text",series+" 开始","words",List.of()));
            }
            duplicateCarFrames(screens);
            int[] frame={0},left={0},nitro={0};boolean[] done={false};List<String> validated=new ArrayList<>(),hud=new ArrayList<>();TaskStats stats=new TaskStats();
            try(LuaRuntime r=new LuaRuntime((name,l)->{
                if(name.equals("_multiplayerUi")){LuaRuntime.push(l,screens.get(Math.min(frame[0]++,screens.size()-1)));return 1;}
                if(name.equals("cmpColor")){l.push(1);return 1;}
                if(name.equals("_fallbackStep")){l.push(42);return 1;}
                if(name.equals("tap")&&l.toInteger(1)==236&&l.toInteger(2)==1100)nitro[0]++;
                if(name.equals("swipe")&&l.toInteger(1)==288){check(l.toInteger(2)==840&&l.toInteger(4)==440,"Forced map sends left swipe, never right");left[0]++;}
                if(name.equals("_sessionProject"))stats.project(l.toString(1));
                if(name.equals("_sessionRaceBegin"))stats.begin(matcher.displayName(l.toString(1)),l.toBoolean(2));
                if(name.equals("_sessionRaceResult"))stats.result();
                if(name.equals("_sessionRaceReturned"))stats.returned();
                if(name.equals("_routeDisplay")){l.push(matcher.displayName(l.toString(1)));return 1;}
                if(name.equals("validate"))validated.add(l.toString(1));
                if(name.equals("_hud"))hud.add(l.toString(1));
                if(name.equals("done"))done[0]=true;
                return 0;
            },List.of("_multiplayerUi","cmpColor","tap","swipe","print","_hud","_sessionOperation","_sessionProject","_sessionRaceBegin","_sessionRaceResult","_sessionRaceReturned","_routeDisplay","_fallbackBegin","_fallbackStep","validate","done"))){
                r.set("data",Map.of("项目",mode,"多人",0));r.set("run",Map.of("data",Map.of("次数","5","多人选路","3")));
                r.load("helpers","utils={defultCfg=function(v,d)return v or d end,getMapName=function(key)validate(key)end,newAutoDriver=function()end};run.nitro=function()end");
                r.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));r.load("test","function test()currentMultiplayer.run();done()end");r.start("test");
                for(int i=0;i<250&&!done[0];i++)r.step(i*2000L);
            }
            check(done[0]&&stats.finished()==5&&stats.returnedCount()==5,"Four fallback maps and next smart route complete: "+mode);
            check(left[0]==4&&nitro[0]>=4&&stats.fallbackCount()==4,"Each explicit map runs left+nitro and counts fallback once: "+mode);
            check(validated.equals(List.of(keys.get(4))),"Bad/missing routes bypass validation; next ordinary map resumes smart route");
            for(String name:names.subList(0,4))check(hud.stream().anyMatch(h->h.contains(name)&&h.contains("固定左划")),"Fallback retains recognized CN map in status: "+name);
        }
    }
    private static void duplicateCarFrames(List<Object> screens){
        for(int i=screens.size()-1;i>=0;i--)if("cars".equals(((Map<?,?>)screens.get(i)).get("scene")))screens.add(i,screens.get(i));
    }
    private static void multiplayerFuelSelection(Path assets) throws Exception {
        String[][] fuelCases={{"4/4","3/3","3/3","3/3"},{"0/4","3/3","3/3","3/3"},
            {"0/4","0/3","2/3","3/3"},{"回油中","3/3","3/3","3/3"},
            {"5/4","3/3","3/3","3/3"},{"0/4","0/3","0/3","0/3","0/3","0/3"},
            {"0/4","0/3","0/3","0/3","3/3","3/3"},{"0/4","0/3","0/3","0/3","0/3","2/3"}};
        int[] expected={1,2,3,2,2,0,5,6};
        for(int c=0;c<fuelCases.length;c++) {
            List<Object> words=new ArrayList<>();
            for(int i=0;i<fuelCases[c].length;i++)words.add(Map.of("text",fuelCases[c][i],"x",240+(i/2)*464,"y",390+(i%2)*237));
            int[] frame={0},chosen={0};boolean[] stopped={false};
            try(LuaRuntime r=new LuaRuntime((name,l)->{
                if(name.equals("_multiplayerUi")) {
                    int n=frame[0]++;
                    LuaRuntime.push(l,n==0?Map.of("scene","series","text","世界系列赛 大师系列赛","words",List.of(Map.of("text","大师系列赛","x",640,"y",450))):
                        n==1?Map.of("scene","event","text","大师系列赛 里程碑 开始","words",List.of(Map.of("text","开始","x",1100,"y",650))):
                        Map.of("scene","cars","text","车辆选择 选择参与比赛的车辆","words",words));return 1;
                }
                if(name.equals("tap")&&frame[0]>=3){long x=l.toInteger(2),y=719-l.toInteger(1);chosen[0]=(x>1150?4:x>600?2:0)+(y>400?2:1);}
                return 0;
            },List.of("_multiplayerUi","_sessionProject","tap","swipe","_hud","print"))) {
                r.set("data",Map.of("项目","多人下","多人",0));r.set("run",Map.of("data",Map.of("次数","10","多人下用车",List.of("1","2","3","4","5","6"))));
                r.load("helpers","utils={defultCfg=function(v,d) return v or d end}");
                r.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));r.start("currentMultiplayer.run");
                try{for(int i=0;i<30&&chosen[0]==0;i++)r.step(i*2000L);}
                catch(RuntimeException e){if(!e.getMessage().contains("没有已配置且有燃油的车辆"))throw e;stopped[0]=true;}
            }
            check(chosen[0]==expected[c]&&!stopped[0],"Master series chooses next fuelled configured car, case "+c);
        }
    }
    private static void unlockProtection(Path assets) throws Exception {
        String prompt="恭喜！你得到了钥匙，可以解锁新车啦！解锁就绪！稍后查看 解锁车辆";
        check(MultiplayerUi.classify(prompt).equals("vehicleUnlock"),"User screenshot new-car prompt has highest-priority protection");
        check(MultiplayerUi.classify(prompt+" 你的用时 比赛奖励 继续").equals("vehicleUnlock"),"Unlock guard overrides reward/result controls behind the popup");
        check(VehicleUnlockGuard.prompt("解鎖車輛 稍後查看")&&!VehicleUnlockGuard.prompt("车辆选择 已可解锁 30/5"),"Traditional unlock label protected without mistaking garage cards for popup");
        check(VehicleUnlockGuard.laterLabel("稍 后 查 看",400,590,500,640)&&!VehicleUnlockGuard.laterLabel("稍后查看 解锁车辆",400,590,900,640)&&!VehicleUnlockGuard.laterLabel("稍后查看",800,590,900,640),"Only exact left-side Later label can authorize a tap");
        VehicleUnlockGuard guard=new VehicleUnlockGuard();guard.observe(true,new int[]{400,590,500,640},false);guard.requireTap(450,615);
        boolean denied=false;try{guard.requireTap(850,615);}catch(IllegalStateException e){denied=true;}check(denied,"Native touch gate blocks Unlock button");
        denied=false;try{guard.requireOtherGesture();}catch(IllegalStateException e){denied=true;}check(denied,"Native touch gate blocks swipes and Back");
        guard.observe(false,null,false);denied=false;try{guard.requireTap(450,615);}catch(IllegalStateException e){denied=true;}check(denied&&guard.active(),"Unreadable subsequent frame cannot reuse old Later coordinates or clear protection");
        guard.observe(false,null,true);check(guard.active(),"One safe frame does not clear unlock latch");guard.observe(false,null,true);check(!guard.active(),"Two known-safe frames clear protection after dismissal");
        for(boolean later:List.of(true,false)) {
            Map<String,Object> screen=new HashMap<>();screen.put("scene","vehicleUnlock");screen.put("text",prompt);screen.put("closeX",Map.of("x",1100,"y",100));
            screen.put("words",List.of(Map.of("text","解锁车辆","x",850,"y",620),Map.of("text","继续","x",1100,"y",650)));
            if(later)screen.put("unlockLater",Map.of("x",440,"y",620));
            List<String> taps=new ArrayList<>();boolean stopped=false;
            try(LuaRuntime r=new LuaRuntime((name,l)->{
                if(name.equals("_multiplayerUi")){LuaRuntime.push(l,screen);return 1;}
                if(name.equals("tap"))taps.add(l.toInteger(1)+","+l.toInteger(2));
                return 0;
            },List.of("_multiplayerUi","_sessionProject","tap","swipe","_hud","print"))) {
                r.set("data",Map.of("项目","多人上","多人",0));r.set("run",Map.of("data",Map.of("次数","30")));
                r.load("helpers","utils={defultCfg=function(v,d) return v or d end}");r.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));r.start("currentMultiplayer.run");
                try{for(int n=0;n<8&&taps.isEmpty();n++)r.step(n*2000L);}catch(RuntimeException e){if(!e.getMessage().contains("绝不自动解锁车辆"))throw e;stopped=true;}
            }
            check(later?taps.equals(List.of("99,440"))&&!stopped:taps.isEmpty()&&stopped,"Unlock prompt allows only Later; missing Later stops before any fallback: "+later);
        }
    }
    private static void queueLoopOrder(Path assets) throws Exception {
        for(boolean repeat:List.of(true,false)) {
            List<String> runs=new ArrayList<>();List<Long> cycles=new ArrayList<>();boolean[] finished={false};
            try(LuaRuntime r=new LuaRuntime((name,l)->{
                if(name.equals("recordProject"))runs.add(l.toString(1)+":"+l.toString(2));
                if(name.equals("_sessionCycle"))cycles.add(l.toInteger(1));
                if(name.equals("_finished"))finished[0]=true;
                return 0;
            },List.of("recordProject","_sessionCycle","_finished","_hud","print"))) {
                r.set("UIconfig",Map.of("routeOnly","","repeatProjects",repeat,"projects",List.of(
                    Map.of("项目","多人上","data",Map.of("次数","30")),Map.of("项目","多人下","data",Map.of("次数","20")),Map.of("项目","杯赛","data",Map.of("次数","99"))),"ActiveProj",List.of(true,true,false)));
                r.load("bootstrap",Files.readString(assets.resolve("lua/bootstrap.lua")));
                r.load("stubs","data={};run={};function localInit() end;function monitor() sleep(999999) end;currentMultiplayer={run=function() recordProject(data['项目'],run.data['次数']);sleep(5) end}");
                r.start("main");for(int n=0;n<30&&runs.size()<4&&!finished[0];n++)r.step(n*1000L);
            }
            check(runs.equals(repeat?List.of("多人上:30","多人下:20","多人上:30","多人下:20"):List.of("多人上:30","多人下:20")),"Queue preserves full 30+20 order and skips disabled project, repeat="+repeat);
            check(repeat?cycles.equals(List.of(1L,2L))&&!finished[0]:cycles.equals(List.of(1L))&&finished[0],"Cycle restarts only after both projects finish, repeat="+repeat);
        }
    }
    private static void workflowSmoke(Path assets) throws Exception {
        String[] modules={"bootstrap","shared","utilities","workflows","multiplayer_up","multiplayer_down","cup","special","club","career","daily"};
        String[] modes={"multiple1","multiple2","cup","spCup","dsCup","career","dailyCup"};
        List<String> apis=List.of("getDisplaySize","getWorkPath","keepCapture","releaseCapture","tap","swipe","touchDown","touchMove","touchUp","cmpColorEx","cmpColor","findColor","findMultiColor","getPixelColor","getDisplayRotate","keyPress","print","toast","_hud","record");
        Map<String,Object> cfg=new HashMap<>();
        for(String k:List.of("clearPrize","checkAuto","freePkg","coinPkg","autoToken","Coupons","spPrize","onceAD","coinAD","cupTicketAD","spCupTicketAD","dsTicketAD","dsCupAD","starWay"))cfg.put(k,"");
        cfg.put("gamePkg","test.game");cfg.put("AdTime",40);cfg.put("delay",0);cfg.put("coinADNum",1);
        for(String mode:modes) {
            List<String> taps=new ArrayList<>(); boolean[] done={false};
            try(LuaRuntime r=new LuaRuntime((name,l)->{
                switch(name) {
                    case "getDisplaySize":l.push(720);l.push(1280);return 2;
                    case "getDisplayRotate":l.push(1);return 1;
                    case "getWorkPath":l.push("/tmp/a9-local-check");return 1;
                    case "getPixelColor":l.push("000000");return 1;
                    case "cmpColor":case "cmpColorEx":l.push(0);return 1;
                    case "findColor":case "findMultiColor":l.push(-1);l.push(-1);return 2;
                    case "tap":taps.add(l.toInteger(1)+","+l.toInteger(2));return 0;
                    case "record":done[0]=true;return 0;
                    default:return 0;
                }
            },apis)) {
                r.set("LOCAL_ROOT","/tmp/a9-local-check");r.set("UIconfig",cfg);r.set("ROUTES",Map.of());
                for(String module:modules)r.load(module,Files.readString(assets.resolve("lua/"+module+".lua")));
                r.load("menu-smoke","function smoke() localInit();run.data={};"+mode+".operation[1][2]();record('done') end");
                r.start("smoke");for(int i=0;i<1000;i++)if(!r.step(i*1000L))break;
                check(done[0],mode+" menu state can finish under mocked nonmatching frames");
                check(!taps.isEmpty(),mode+" menu state emits native input calls");
            }
        }
        try(LuaRuntime r=new LuaRuntime((name,l)->0,List.of())) {
            r.load("cancel","function busy() while true do local a=1+1 end end");r.start("busy");
            Thread stop=new Thread(()->{try{Thread.sleep(30);}catch(InterruptedException ignored){}r.requestStop();});stop.start();
            boolean cancelled=false;try{r.step(0);}catch(RuntimeException e){cancelled=e.toString().contains("运行已停止");}stop.join();
            check(cancelled,"Infinite Lua computation is interruptible by stop control");
        }
    }
}
