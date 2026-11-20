package dev.funlee.a9.core;

import java.nio.file.*;
import java.util.*;

final class StabilityChecks {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static int run(Path assets)throws Exception{
        check(MultiplayerUi.raceEnded("result")&&MultiplayerUi.raceEnded("raceDisconnected")&&
                MultiplayerUi.raceEnded("raceCancelled")&&!MultiplayerUi.raceEnded("race")&&
                !MultiplayerUi.raceEnded("unknown")&&!MultiplayerUi.raceEnded("event"),
                "Missing multiplayer progress hands verified results and interruptions back to the event flow");
        check(AccountLoginDialog.matches("检测到并行存取行为")&&AccountLoginDialog.matches("该账号从另一台设备登录。")&&
                AccountLoginDialog.matches("檢測到並行存取行為")&&AccountLoginDialog.matches("检测測到井行存取行为")&&!AccountLoginDialog.matches("登录历史 最近登录 游戏账号")&&
                !AccountLoginDialog.matches("断开连接，请重新登录")&&!AccountLoginDialog.matches("在其他设备上登录前请绑定账号"),
                "Concurrent account access is distinct from login and connection prompts");
        check(RunIssue.from(AccountLoginDialog.STOP_REASON).title.equals(AccountLoginDialog.STOP_REASON),
                "Concurrent access has an explicit terminal status");
        check(MultiplayerUi.classify("动态奖励 您可以从这组奖励中选一个").equals("rewardChoice"),"Dynamic selection recognized before Confirm appears");
        check(MultiplayerUi.classify("你的俱乐部达成了该里程碑 万能图纸 稍后领取 前往领奖").equals("milestoneLater"),"Observed club milestone is distinct from claiming rewards");
        check(!MultiplayerUi.classify("俱乐部里程碑 前往领奖 立即购买").equals("milestoneLater"),"Do not treat other milestone or purchase views as safe Later");
        List<String> milestoneTaps=new ArrayList<>();
        try(LuaRuntime lua=new LuaRuntime((name,l)->{
            if(name.equals("_multiplayerUi")){LuaRuntime.push(l,MultiplayerGarageChecks.fixture(Map.of("scene","milestoneLater","text","你的俱乐部达成了该里程碑 稍后领取 前往领奖",
                "words",List.of(Map.of("text","稍后领取","x",450,"y",598),Map.of("text","前往领奖","x",830,"y",598)))));return 1;}
            if(name.equals("tap"))milestoneTaps.add(l.toInteger(1)+","+l.toInteger(2));
            return 0;
        },List.of("_multiplayerUi","_sessionProject","_hud","print","tap"))){
            lua.set("data",Map.of("项目","多人下"));lua.set("run",Map.of("data",Map.of("次数",20)));
            lua.load("helpers","utils={defultCfg=function(v,d)return v or d end}");
            lua.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));lua.start("currentMultiplayer.run");
            for(int n=0;n<4&&milestoneTaps.isEmpty();n++)lua.step(n*1000L);
        }
        check(milestoneTaps.equals(List.of("121,450")),"Club milestone taps only the left Later button");
        check(MultiplayerUi.classify("动态奖励 您可以从这组奖励中选-").equals("rewardChoice"),"Observed OCR truncation of one-card suffix is supported");
        check(!MultiplayerUi.classify("奖励 立即购买 确认").equals("rewardChoice"),"Paid offers excluded");
        check(MultiplayerUi.classify("登录历史 游戏号 最近登录").equals("loginHistory"),"Recognize SDK recent-account picker");
        check(MultiplayerUi.classify("服务器错误 无法在服务器上执行操作(3) 正在搜索其他玩家和地点").equals("serverError"),"Server error takes precedence over matching behind it");
        check(MultiplayerUi.classify("未找到对手 未找到对手。请稍后重试。 正在搜索其他玩家和地点").equals("noOpponents"),"No opponents popup overrides matching behind it");
        check(MultiplayerUi.classify("末找到对手。请稍后重试。").equals("noOpponents"),"Observed OCR variant is recognized");
        check(MultiplayerUi.classify("比赛奖励 你的用时 比赛还未开始。未找到车手。没有可领取的比赛奖励 继续").equals("raceCancelled"),"Cancelled loading is not a finished race");
        check(MultiplayerUi.classify("|比赛奖励 6你的用时: 还未开始。 未找到车手。 没有可领取的比赛奖励 继续").equals("raceCancelled"),"Live cancellation OCR missing 比赛 still resets the matching attempt");
        check(!MultiplayerUi.classify("比赛奖励 你的用时 00:45.123 继续").equals("raceCancelled"),"A completed result never becomes cancellation");
        int[] dialog=new int[720*1280];Arrays.fill(dialog,0xff102030);
        for(int d=-16;d<=16;d++)for(int b=-1;b<=1;b++){
            dialog[(1090+d)*720+719-(245+d+b)]=0xffffffff;
            dialog[(1090+d)*720+719-(245-d+b)]=0xffffffff;
        }
        check(PopupCloseDetector.find(new PixelFrame(720,1280,dialog))==null&&PopupCloseDetector.findDialog(new PixelFrame(720,1280,dialog))!=null,"Error dialog close uses its own lower region, preserving promotion detector");
        check(DailyEventUi.seasonPassUnlock("領取 級别U2现已解锁 购买賽季通行证")&&
                !DailyEventUi.seasonPassUnlock("档案头像现已解锁 购买赛季通行证"),
                "Season pass unlock requires the level and purchase labels");
        int[] pass=new int[720*1280];Arrays.fill(pass,0xff102030);
        for(int x=1090;x<1140;x++)for(int y=55;y<119;y++)pass[x*720+719-y]=0xffff005c;
        check(PopupCloseDetector.findSeasonPassUnlock(new PixelFrame(720,1280,pass))==null,
                "A pink region without a white X cannot dismiss the season pass popup");
        for(int d=-12;d<=12;d++)for(int b=-1;b<=1;b++){
            pass[(1115+d)*720+719-(87+d*4/3+b)]=0xffffffff;
            pass[(1115+d)*720+719-(87-d*4/3+b)]=0xffffffff;
        }
        int[] passClose=PopupCloseDetector.findSeasonPassUnlock(new PixelFrame(720,1280,pass));
        check(passClose!=null&&Math.abs(passClose[0]-1115)<=2&&Math.abs(passClose[1]-87)<=2,
                "Wide season pass unlock uses the white X inside the pink close button");
        int[] pixels=new int[720*1280];Arrays.fill(pixels,0xff112030);
        for(int x=100;x<260;x++)for(int y=240;y<440;y++)pixels[x*720+719-y]=0xffffdd55;
        for(int x=400;x<560;x++)for(int y=240;y<440;y++)pixels[x*720+719-y]=0xffdd77ff;
        for(int x=1240;x<1280;x++)for(int y=240;y<440;y++)pixels[x*720+719-y]=0xffffdd55;
        check(DynamicRewards.cards(new PixelFrame(720,1280,pixels)).size()==2,"Complete cards found; partial edge card excluded");
        List<String> taps=new ArrayList<>();int[] phase={0};
        try(LuaRuntime lua=new LuaRuntime((name,l)->{
            if(name.equals("_multiplayerUi")){
                LuaRuntime.push(l,MultiplayerGarageChecks.fixture(Map.of("scene","rewardChoice","text","动态奖励 从这组奖励中选一个","closeX",Map.of("x",1210,"y",30),
                        "rewardChoices",List.of(Map.of("x",180,"y",350),Map.of("x",480,"y",350)),
                        "words",phase[0]==0?List.of():List.of(Map.of("text","确认","x",640,"y",605)))));return 1;
            }
            if(name.equals("rnd")){l.push(2);return 1;}
            if(name.equals("tap")){taps.add(l.toInteger(1)+","+l.toInteger(2));phase[0]++;}
            return 0;
        },List.of("_multiplayerUi","_sessionProject","_hud","print","tap","rnd"))){
            lua.set("data",Map.of("项目","多人下"));lua.set("run",Map.of("data",Map.of("次数",1)));
            lua.load("helpers","utils={defultCfg=function(v,d) return v or d end}");
            lua.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));lua.start("currentMultiplayer.run");
            for(int n=0;n<8&&taps.size()<2;n++)lua.step(n*2000L);
        }
        check(taps.equals(List.of("369,480","114,640")),"Random selected card then Confirm; never closes free choice popup");
        worldRewardChoices(assets);
        for(String errorScene:List.of("serverError","noOpponents","raceCancelled")){
        int maxRetries=errorScene.equals("serverError")?3:5;
        String expectedError=errorScene.equals("serverError")?"服务器连续返回错误":"连续未找到对手";
        int[] errors={0},results={0},reads={0};boolean limited=false;
        try(LuaRuntime lua=new LuaRuntime((name,l)->{
            if(name.equals("_multiplayerUi")){Map<String,Object> screen=new HashMap<>(Map.of("scene",errorScene,"text","服务器错误 无法在服务器上执行操作(3)","words",List.of(Map.of("text","继续","x",1100,"y",600))));if(reads[0]++>=3)screen.put("closeX",Map.of("x",1090,"y",245));LuaRuntime.push(l,MultiplayerGarageChecks.fixture(screen));return 1;}
            if(name.equals("tap"))errors[0]++;
            if(name.equals("_sessionRaceResult")||name.equals("_sessionRaceReturned"))results[0]++;
            return 0;
        },List.of("_multiplayerUi","_sessionProject","_hud","print","tap","_sessionRaceResult","_sessionRaceReturned"))){
            lua.set("data",Map.of("项目","多人上"));lua.set("run",Map.of("data",Map.of("次数",10)));
            lua.load("helpers","utils={defultCfg=function(v,d) return v or d end}");
            lua.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));lua.start("currentMultiplayer.run");
            try{for(int n=0;n<500;n++)lua.step(n*1000L);}catch(RuntimeException e){if(!e.getMessage().contains(expectedError))throw e;limited=true;}
        }
        check(limited&&errors[0]==maxRetries&&results[0]==0,"Animated close waits before bounded retries and never counts an unfinished race: "+errorScene);
        }
        postRaceTransitions(assets);
        postRaceRewardBatches(assets);
        disconnectedRace(assets);
        carAlignment(assets,false);
        carAlignment(assets,true);
        fuelSkip(assets);
        skippedQueue(assets);
        for(boolean skip:List.of(false,true))for(boolean crash:List.of(false,true))continuousCycles(assets,skip,crash);
        for(int failAt:List.of(0,119,200))continuousQueue(assets,failAt);
        return checks;
    }
    private static void worldRewardChoices(Path assets)throws Exception{
        NavigableMap<Integer,Integer> rules=DynamicRewards.rules("32700:1, 31700：4，30700:2");
        check(rules.higherKey(31680)==31700&&rules.get(31700)==4,"Reward rules normalize separators and sort by milestone");
        for(String invalid:List.of("31700:4,31700:2","31700:0","31700:4,")){
            boolean rejected=false;
            try{DynamicRewards.rules(invalid);}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"Reject ambiguous or invalid rule: "+invalid);
        }

        for(String mode:List.of("manual","firstNonRed","unknownPrice","configured","laterMilestone","noBefore","noAfter","offscreen")){
            List<Map<String,Object>> cards=new ArrayList<>();
            for(int x:List.of(222,428,633,839,1045)){
                String price=x<839?"PRESENT":"ABSENT";
                if(x==428&&mode.equals("unknownPrice"))price="UNKNOWN";
                cards.add(Map.of("x",x,"y",350,"visible",true,"price",price));
            }
            boolean mapped=List.of("configured","laterMilestone","noBefore","noAfter","offscreen").contains(mode);
            List<String> rewardTaps=new ArrayList<>();
            int[] reads={0};boolean[] onPopup={false},readScore={false};String error="";
            try(LuaRuntime lua=new LuaRuntime((name,l)->{
                if(name.equals("_multiplayerUi")){
                    readScore[0]=l.toBoolean(1);
                    int n=reads[0]++;
                    Map<String,Object> screen=new HashMap<>();
                    screen.put("frameId",n*1000L);
                    if(mapped&&n==0){
                        screen.put("scene","series");screen.put("text","世界系列赛 大师系列赛");
                        screen.put("words",List.of(Map.of("text","世界系列赛","x",350,"y",450)));
                    }else if(mapped&&n==1){
                        screen.put("scene","event");screen.put("text","世界系列赛 里程碑 开始");
                        screen.put("words",List.of(Map.of("text","开始","x",1100,"y",650)));
                        if(!mode.equals("noBefore"))screen.put("seriesScore",31680);
                    }else if(mapped&&n==2){
                        screen.put("scene","race");screen.put("text","");
                    }else if(mapped&&n<6){
                        screen.putAll(Map.of("scene","result","text","比赛奖励 系列赛分数 总计 继续","finalRank",2,
                                "rewardPage",true,"rewardReady",true,"rewardCanContinue",true));
                        if(!mode.equals("noAfter"))screen.put("seriesScore",31754);
                    }else{
                        onPopup[0]=true;
                        screen.putAll(Map.of("scene","rewardChoice","text","动态奖励 从这组奖励中选一个","rewardChoices",cards,
                                "closeX",Map.of("x",1210,"y",30),"rewardOrderKnown",true));
                        if(!rewardTaps.isEmpty())screen.put("words",List.of(Map.of("text","确认","x",640,"y",605)));
                    }
                    LuaRuntime.push(l,screen);return 1;
                }
                if(name.equals("_fallbackStep")){l.push(50);return 1;}
                if(name.equals("tap")&&onPopup[0])rewardTaps.add(l.toInteger(1)+","+l.toInteger(2));
                return 0;
            },List.of("_multiplayerUi","_sessionProject","_sessionRaceBegin","_fallbackBegin","_fallbackStep","_sessionOperation",
                    "_sessionRaceResult","_sessionRaceRewards","swipe","_hud","print","tap"))){
                Map<String,Object> config=new HashMap<>(Map.of("次数",1));
                if(!mode.equals("manual"))config.put("自选奖励","0");
                if(mapped)config.put("worldRewardRules",mode.equals("laterMilestone")?List.of(Map.of("score",32700,"position",1)):
                        List.of(Map.of("score",30700,"position",2),Map.of("score",31700,"position",mode.equals("offscreen")?6:4)));
                lua.set("data",Map.of("项目","多人上"));lua.set("run",Map.of("data",config));
                lua.load("helpers","utils={defultCfg=function(v,d) return v or d end}");
                lua.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));lua.start("currentMultiplayer.run");
                try{for(int n=0;n<40&&rewardTaps.size()<2;n++)lua.step(n*1000L);}
                catch(RuntimeException expected){error=expected.getMessage();}
            }
            check(readScore[0]==mapped,"Score OCR is requested only for configured milestones: "+mode);
            if(mode.equals("firstNonRed")||mode.equals("configured"))
                check(rewardTaps.equals(List.of("369,839","114,640"))&&error.isEmpty(),"Select fourth card and confirm: "+mode+" "+rewardTaps+" "+error);
            else check(rewardTaps.isEmpty()&&error.contains("手动"),"Inconclusive reward must be left open: "+mode+" "+rewardTaps+" "+error);
        }
    }
    private static Map<String,Object> transitionScreen(String phase){
        String scene=phase.equals("bonus")?"unknown":phase;
        String text="";
        switch(phase){
            case "series":text="世界系列赛 大师系列赛";break;
            case "event":text="世界系列赛 排行榜 里程碑 开始";break;
            case "cars":text="车辆选择 选择参与此赛事的车辆 青铜";break;
            case "car":text="TOUCHDRIVE 升级 开始";break;
            case "result":text="你的排名 你的用时 02:16.765 排行榜 继续";break;
            case "bonus":text="名人堂 新入组 稀有卡牌 进口零件 X1 继续";break;
            default:break;
        }
        List<Object> words=new ArrayList<>();
        if(phase.equals("series"))words.add(Map.of("text","世界系列赛","x",350,"y",450));
        if(phase.equals("event")||phase.equals("car"))words.add(Map.of("text","开始","x",1100,"y",650));
        if(phase.equals("cars"))words.add(Map.of("text","6/6","x",240,"y",390));
        if(phase.equals("result")||phase.equals("bonus"))words.add(Map.of("text","继续","x",1100,"y",650));
        return Map.of("scene",scene,"text",text,"words",words,"finalRank",2);
    }
    private static void postRaceTransitions(Path assets)throws Exception{
        for(String destination:List.of("event","cars","car","home","series"))for(boolean confirmed:List.of(true,false)){
            List<String> pages=new ArrayList<>(List.of("series","event"));
            pages.addAll(Collections.nCopies(6,"cars"));pages.addAll(Collections.nCopies(2,"car"));pages.add("race");
            if(confirmed){pages.addAll(Collections.nCopies(4,"result"));pages.addAll(Collections.nCopies(5,"bonus"));}
            // Reproduce a brief event lobby followed by a garage while the old selection timer exists.
            if(confirmed)pages.add("event");pages.addAll(Collections.nCopies(4,destination));
            pages.add("series");pages.add("event");pages.addAll(Collections.nCopies(6,"cars"));pages.addAll(Collections.nCopies(2,"car"));pages.add("race");
            int[] reads={0},begins={0},cards={0},swipes={0},bonusTaps={0},resultTaps={0};String[] current={""};
            TaskStats stats=new TaskStats();stats.project("多人上");boolean rejected=false;
            try(LuaRuntime lua=new LuaRuntime((name,l)->{
                if(name.equals("_multiplayerUi")){
                    int n=reads[0]++;current[0]=pages.get(Math.min(n,pages.size()-1));
                    Map<String,Object> screen=MultiplayerGarageChecks.fixture(transitionScreen(current[0]));
                    screen.put("frameId",n*1000L);LuaRuntime.push(l,screen);return 1;
                }
                if(name.equals("_sessionProject")){l.push(0);return 1;}
                if(name.equals("_sessionRaceBegin")){begins[0]++;stats.begin("test",false);}
                if(name.equals("_sessionRaceResult"))stats.result(2);
                if(name.equals("_sessionRaceReturned"))stats.returned();
                if(name.equals("_fallbackStep")){l.push(99);return 1;}
                if(name.equals("cmpColor")){l.push(1);return 1;}
                if(name.equals("swipe")&&l.toInteger(1)==419)swipes[0]++;
                if(name.equals("tap")){
                    int x=(int)l.toInteger(2),y=719-(int)l.toInteger(1);
                    if(x==410&&y==305)cards[0]++;
                    if(x==1100&&y==650){if(current[0].equals("bonus"))bonusTaps[0]++;if(current[0].equals("result"))resultTaps[0]++;}
                }
                return 0;
            },List.of("_multiplayerUi","_sessionProject","_sessionRaceBegin","_sessionRaceResult","_sessionRaceReturned","_sessionOperation","_fallbackBegin","_fallbackStep","cmpColor","tap","swipe","print","_hud"))){
                lua.set("data",Map.of("项目","多人上","多人",0));lua.set("run",Map.of("data",Map.of("次数",2,"多人选路","1","青铜用车",List.of("1"))));
                lua.load("helpers","utils={defultCfg=function(v,d)return v or d end}");lua.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));lua.start("currentMultiplayer.run");
                try{for(int i=0;i<180&&begins[0]<2;i++)lua.step(i*2000L);}catch(RuntimeException e){if(!e.getMessage().contains("未确认比赛结算"))throw e;rejected=true;}
            }
            check(confirmed?!rejected&&begins[0]==2&&stats.finished()==1&&stats.returnedCount()==1:rejected&&stats.finished()==0&&stats.returnedCount()==0,"Return via "+destination+" requires a result and resets the prior race before continuing: "+confirmed);
            if(confirmed){
                check(cards[0]==2&&swipes[0]==2,"Next race independently verifies garage origin and selects its car: "+destination);
                check(bonusTaps[0]==1&&resultTaps[0]==1,"Repeated captured reward pages must not send repeated Continue taps: "+destination);
            }
        }
    }
    private static void postRaceRewardBatches(Path assets)throws Exception{
        List<String> rewards=List.of("金币 18,888","蓝币 38","稀有卡牌 X1","进口零件 X2","联会币 388","万能图纸 X1");
        List<String> dismissed=new ArrayList<>();
        int race=-2, results=-1;
        int[] livePage={race}, capturedPage={race}, staleFrames={0}, returned={0};
        long[] frame={0};
        try(LuaRuntime lua=new LuaRuntime((name,l)->{
            if(name.equals("_multiplayerUi")){
                if(staleFrames[0]>0){
                    staleFrames[0]--;
                }else{
                    capturedPage[0]=livePage[0];
                    frame[0]+=1000;
                }
                int page=capturedPage[0];
                String phase=page==race?"race":page==results?"result":page<rewards.size()?"bonus":"event";
                Map<String,Object> screen=MultiplayerGarageChecks.fixture(transitionScreen(phase));
                if(phase.equals("bonus"))screen.put("text","恭喜！你获得了这些奖励 "+rewards.get(page)+" 继续");
                screen.put("frameId",frame[0]);
                if(page==race)livePage[0]=results;
                LuaRuntime.push(l,screen);return 1;
            }
            if(name.equals("_fallbackStep")){l.push(99);return 1;}
            if(name.equals("_sessionRaceReturned"))returned[0]++;
            if(name.equals("tap")&&l.toInteger(2)==1100&&719-l.toInteger(1)==650){
                int page=livePage[0];
                check(page==capturedPage[0]&&page>=results&&page<rewards.size(),"Only tap Continue on the current reward or result page");
                if(page>=0)dismissed.add(rewards.get(page));
                livePage[0]++;
                staleFrames[0]=2;
            }
            return 0;
        },List.of("_multiplayerUi","_sessionProject","_sessionRaceBegin","_sessionRaceResult","_sessionRaceReturned",
                "_sessionOperation","_fallbackBegin","_fallbackStep","tap","swipe","print","_hud"))){
            lua.set("data",Map.of("项目","多人下","多人",0));lua.set("run",Map.of("data",Map.of("次数",1)));
            lua.load("helpers","utils={defultCfg=function(v,d)return v or d end}");
            lua.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));
            lua.start("currentMultiplayer.run");
            for(int i=0;i<180&&returned[0]==0;i++)lua.step(i*1000L);
        }
        check(dismissed.equals(rewards)&&returned[0]==1,"Process more than three identical Continue labels and return without tapping Start");
    }
    private static void disconnectedRace(Path assets)throws Exception{
        String observed="你的排名 你已 断开连接 连接丟失 你的用时 00:00.000 排行榜 继续";
        check(MultiplayerUi.classify(observed).equals("raceDisconnected"),"Observed disconnected result overrides normal result labels");
        check(MultiplayerUi.classify("连接丢失").equals("raceDisconnected"),"Incomplete disconnect animation is recognized before result appears");
        int[] reads={0},begins={0};TaskStats stats=new TaskStats();stats.project("多人上");
        try(LuaRuntime lua=new LuaRuntime((name,l)->{
            if(name.equals("_multiplayerUi")){
                int n=reads[0]++;String scene=n==0||n==4?"race":n==1||n==3?"raceDisconnected":n==2||n==5?"result":"event";
                Map<String,Object> screen=new HashMap<>(Map.of("scene",scene,"text",scene.equals("raceDisconnected")?observed:"你的排名 你的用时 继续",
                    "words",n==1?List.of():List.of(Map.of("text","继续","x",1100,"y",620))));
                screen.put("finalRank",3);LuaRuntime.push(l,MultiplayerGarageChecks.fixture(screen));return 1;
            }
            if(name.equals("_sessionProject")){l.push(0);return 1;}
            if(name.equals("_sessionRaceBegin")){begins[0]++;stats.begin("test",false);}
            if(name.equals("_sessionRaceResult"))stats.result((int)l.toInteger(1));
            if(name.equals("_sessionRaceReturned"))stats.returned();
            if(name.equals("_fallbackStep")){l.push(20);return 1;}
            return 0;
        },List.of("_multiplayerUi","_sessionProject","_sessionRaceBegin","_sessionRaceResult","_sessionRaceReturned","_sessionOperation","_fallbackBegin","_fallbackStep","tap","swipe","print","_hud"))){
            lua.set("data",Map.of("项目","多人上","多人",0));lua.set("run",Map.of("data",Map.of("次数",1,"多人选路","1")));
            lua.load("helpers","utils={defultCfg=function(v,d)return v or d end}");lua.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));lua.start("currentMultiplayer.run");
            for(int i=0;i<50&&stats.returnedCount()==0;i++)lua.step(i*2000L);
        }
        check(begins[0]==2&&stats.finished()==1&&stats.returnedCount()==1,"Disconnect and partial subsequent result consume no quota; next successful race counts once");
    }
    private static void fuelSkip(Path assets)throws Exception{
        int[] pixels=new int[720*1280];Arrays.fill(pixels,0xff102030);
        for(int i=0;i<6;i++)for(int x=219+i/2*464;x<265+i/2*464;x++)for(int y=384+i%2*237;y<410+i%2*237;y++)pixels[x*720+719-y]=(x%10==0?0xffffffff:0xffff005b);
        check(FuelAvailability.emptySlots(new PixelFrame(720,1280,pixels)).equals(List.of(1,2,3,4,5,6)),"All six pink empty-fuel badges recognized");
        Arrays.fill(pixels,0xffccff00);check(FuelAvailability.emptySlots(new PixelFrame(720,1280,pixels)).isEmpty(),"Green fuel or unknown frames are not empty");
        // Skip only when every configured car is positively empty. Unknown and off-screen cars
        // must not be treated as empty; skipping never clicks a car or creates a race result.
        for(int scenario=0;scenario<4;scenario++){
            final int testCase=scenario;
            int[] reads={0},clock={0},completedAt={-1},results={0},taps={0};String[] outcome={""};boolean blocked=false;
            try(LuaRuntime lua=new LuaRuntime((name,l)->{
                if(name.equals("_multiplayerUi")){
                    int n=reads[0]++;
                    LuaRuntime.push(l,MultiplayerGarageChecks.fixture(n==0?Map.of("scene","series","text","世界系列赛 大师系列赛","words",List.of(Map.of("text","大师系列赛","x",640,"y",450))):
                        n==1?Map.of("scene","event","text","大师系列赛 里程碑 开始","words",List.of(Map.of("text","开始","x",1100,"y",650))):
                        Map.of("scene","cars","text","车辆选择 选择参与此赛事的车辆",
                            "emptyFuelSlots",testCase==1?List.of(1,2,3,4,5):testCase==3?List.of():List.of(1,2,3,4,5,6),
                            "words",testCase==3?List.of(Map.of("text","0/3","x",240,"y",390)):List.of())));return 1;
                }
                if(name.equals("_sessionProject")){l.push(8);return 1;}
                if(name.equals("tap"))taps[0]++;
                if(name.equals("_sessionRaceResult")||name.equals("_sessionRaceReturned"))results[0]++;
                if(name.equals("done")){outcome[0]=l.toString(1);completedAt[0]=clock[0];}
                return 0;
            },List.of("_multiplayerUi","_sessionProject","tap","swipe","_hud","print","_sessionRaceResult","_sessionRaceReturned","done"))){
                lua.set("data",Map.of("项目","多人下"));lua.set("run",Map.of("data",Map.of("次数",10,"多人下用车",
                    scenario==2?List.of("1","7"):scenario==3?List.of("1"):List.of("1","2","3","4","5","6"))));
                lua.load("helpers","utils={defultCfg=function(v,d)return v or d end};function test() done(currentMultiplayer.run()) end");
                lua.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));lua.start("test");
                try{for(int i=0;i<60&&completedAt[0]<0;i++){clock[0]=i*1000;lua.step(clock[0]);}}
                catch(RuntimeException e){if(!e.getMessage().contains("没有已配置且有燃油的车辆")&&!e.getMessage().contains("翻页后无法核对重叠车辆"))throw e;blocked=true;}
            }
            boolean knownEmpty=scenario==0||scenario==3;
            check(knownEmpty?"fuelEmpty".equals(outcome[0])&&completedAt[0]<60000&&!blocked:blocked&&completedAt[0]<0,
                "Only confirmed empty configured cars skip without waiting: "+scenario);
            check(taps[0]==2&&results[0]==0,"No car selection, purchase or false finish while skipping: "+scenario);
        }
    }
    private static void skippedQueue(Path assets)throws Exception{
        TaskCheckpoint checkpoint=new TaskCheckpoint();checkpoint.restore(4,2,8);
        int[] total={78},world={40},master={38},cycle={4},skipped={0};boolean[] finished={false},crashed={false};List<String> entered=new ArrayList<>();
        for(int attempt=0;attempt<2&&total[0]<200;attempt++){
            try(LuaRuntime lua=new LuaRuntime((name,l)->{
                switch(name){
                    case "_queueResume":LuaRuntime.push(l,checkpoint.resume());return 1;
                    case "_queueEnter":checkpoint.enter((int)l.toInteger(1),(int)l.toInteger(2),(int)l.toInteger(3));entered.add(l.toInteger(1)+":"+l.toInteger(2));l.push(checkpoint.completed());return 1;
                    case "_queueDone":checkpoint.projectDone();if(cycle[0]==4&&!crashed[0]){crashed[0]=true;throw new IllegalStateException("injected after skip");}break;
                    case "_sessionProject":l.push(checkpoint.completed());return 1;
                    case "_sessionCycle":cycle[0]=(int)l.toInteger(1);break;
                    case "empty":boolean empty=cycle[0]==4;if(empty)skipped[0]++;l.push(empty);return 1;
                    case "race":total[0]++;if(l.toString(1).equals("多人上"))world[0]++;else master[0]++;checkpoint.completed(1);break;
                    case "_finished":finished[0]=true;break;
                    default:break;
                }return 0;
            },List.of("_queueResume","_queueEnter","_queueDone","_sessionProject","_sessionCycle","_finished","_hud","print","race","empty"))){
                lua.set("UIconfig",Map.of("routeOnly","","projects",List.of(Map.of("项目","多人上","data",Map.of("次数",10)),Map.of("项目","多人下","data",Map.of("次数",10))),"ActiveProj",List.of(true,true)));
                lua.load("bootstrap",Files.readString(assets.resolve("lua/bootstrap.lua")));
                lua.load("helpers","data={};run={};utils={backToMain=function()end};function localInit() end;function monitor() sleep(99999999) end;currentMultiplayer={run=function() local done=_sessionProject(data['项目'],10);if empty() then return 'fuelEmpty' end;for i=done+1,10 do race(data['项目']);sleep(1) end end}");
                lua.start("main");try{for(int n=0;n<500&&!finished[0]&&total[0]<200;n++)lua.step(n*2000L);}catch(RuntimeException e){if(!e.getMessage().contains("injected after skip"))throw e;}
            }
        }
        check(!finished[0]&&total[0]==200&&world[0]==102&&master[0]==98,"Skipped quota never counts as finishes; existing 78 survive and 200 actual finishes observed without automatic stop");
        check(skipped[0]==1&&entered.subList(0,3).equals(List.of("4:2","5:1","5:2")),"Skip advances to next task even after recovery; next cycle retries skipped mode");
    }
    private static void carAlignment(Path assets,boolean unexpectedDetail)throws Exception{
        int[] frames={0},swipes={0},backs={0};List<String> taps=new ArrayList<>();
        try(LuaRuntime lua=new LuaRuntime((name,l)->{
            if(name.equals("_multiplayerUi")){
                int n=frames[0]++;
                LuaRuntime.push(l,MultiplayerGarageChecks.fixture(n==0?Map.of("scene","series","text","世界系列赛 大师系列赛","words",List.of(Map.of("text","大师系列赛","x",640,"y",450))):
                    n==1?Map.of("scene","event","text","大师系列赛 里程碑 开始","words",List.of(Map.of("text","开始","x",1100,"y",650))):
                    unexpectedDetail&&n<4?Map.of("scene","car","text","车辆详情 开始","words",List.of(Map.of("text","开始","x",1100,"y",650))):
                    Map.of("scene","cars","text","车辆选择 选择参与此赛事的车辆","words",List.of(
                        Map.of("text","2/3","x",swipes[0]<1?240:704,"y",390)))));return 1;
            }
            if(name.equals("swipe")){check(l.toInteger(1)==419&&l.toInteger(2)==300&&l.toInteger(4)==1100,"Garage recenter swipes right in card region");swipes[0]++;}
            if(name.equals("tap")&&frames[0]>2){
                if(unexpectedDetail&&frames[0]<=4&&l.toInteger(1)==690&&l.toInteger(2)==38)backs[0]++;
                else taps.add(l.toInteger(1)+","+l.toInteger(2));
            }
            return 0;
        },List.of("_multiplayerUi","_sessionProject","tap","swipe","_hud","print"))){
            lua.set("data",Map.of("项目","多人下"));lua.set("run",Map.of("data",Map.of("次数",10,"多人下用车",List.of("1","2","3","4"))));
            lua.load("helpers","utils={defultCfg=function(v,d)return v or d end}");
            lua.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));lua.start("currentMultiplayer.run");
            for(int i=0;i<20&&taps.isEmpty();i++)lua.step(i*2000L);
        }
        check(swipes[0]==1&&taps.equals(List.of("414,874"))&&backs[0]==(unexpectedDetail?1:0),
            "Re-read after recenter selects configured car 3, with one back tap from unexpected details when needed");
    }
    private static void continuousCycles(Path assets,boolean skip,boolean crash)throws Exception{
        TaskCheckpoint checkpoint=new TaskCheckpoint();int[] cycle={0},project={0},races={0},entries={0};boolean[] finished={false},failed={false};
        for(int attempt=0;attempt<2&&entries[0]<12;attempt++){
            try(LuaRuntime lua=new LuaRuntime((name,l)->{
                switch(name){
                    case "_queueResume":LuaRuntime.push(l,checkpoint.resume());return 1;
                    case "_queueEnter":project[0]=(int)l.toInteger(2);checkpoint.enter((int)l.toInteger(1),project[0],(int)l.toInteger(3));entries[0]++;l.push(checkpoint.completed());return 1;
                    case "_queueDone":checkpoint.projectDone();if(crash&&cycle[0]==5&&project[0]==2&&!failed[0]){failed[0]=true;throw new IllegalStateException("injected final cycle recovery");}break;
                    case "_sessionProject":l.push(checkpoint.completed());return 1;
                    case "_sessionCycle":cycle[0]=(int)l.toInteger(1);break;
                    case "empty":l.push(skip&&project[0]==2&&checkpoint.completed()>=8);return 1;
                    case "race":races[0]++;checkpoint.completed(1);break;
                    case "_finished":finished[0]=true;break;
                    default:break;
                }return 0;
            },List.of("_queueResume","_queueEnter","_queueDone","_sessionProject","_sessionCycle","_finished","_hud","print","race","empty"))){
                lua.set("UIconfig",Map.of("routeOnly","","maxCycles",5,"projects",List.of(Map.of("项目","多人上","data",Map.of("次数",30)),Map.of("项目","多人下","data",Map.of("次数",20))),"ActiveProj",List.of(true,true)));
                lua.load("bootstrap",Files.readString(assets.resolve("lua/bootstrap.lua")));
                lua.load("helpers","data={};run={};utils={backToMain=function()end};function localInit() end;function monitor() sleep(99999999) end;currentMultiplayer={run=function() local done=_sessionProject(data['项目'],run.data['次数']);for i=done+1,run.data['次数'] do if empty() then return 'fuelEmpty' end;race();sleep(1) end end}");
                lua.start("main");try{for(int n=0;n<600&&entries[0]<12;n++)lua.step(n*2000L);}catch(RuntimeException e){if(!e.getMessage().contains("injected final cycle recovery"))throw e;}
            }
        }
        check(!finished[0]&&cycle[0]==6&&entries[0]==12,"Legacy five-cycle limit is ignored, including after recovery, skip="+skip+", recovery="+crash);
        check(races[0]>=(skip?220:280),"Sixth cycle continues after full quotas or skipped tasks");
    }
    private static void continuousQueue(Path assets,int failAt)throws Exception{
        TaskCheckpoint checkpoint=new TaskCheckpoint();List<String> races=new ArrayList<>();boolean[] finished={false},failed={false};int[] cycle={0};
        for(int attempt=0;attempt<2&&races.size()<220;attempt++){
            try(LuaRuntime lua=new LuaRuntime((name,l)->{
                switch(name){
                    case "_queueResume":LuaRuntime.push(l,checkpoint.resume());return 1;
                    case "_queueEnter":checkpoint.enter((int)l.toInteger(1),(int)l.toInteger(2),(int)l.toInteger(3));l.push(checkpoint.completed());return 1;
                    case "_queueDone":checkpoint.projectDone();break;
                    case "_sessionProject":l.push(checkpoint.completed());return 1;
                    case "_sessionCycle":cycle[0]=(int)l.toInteger(1);break;
                    case "race":races.add(l.toString(1));checkpoint.completed(1);if(races.size()==failAt&&!failed[0]){failed[0]=true;throw new IllegalStateException("injected crash");}break;
                    case "_finished":finished[0]=true;break;
                    default:break;
                }return 0;
            },List.of("_queueResume","_queueEnter","_queueDone","_sessionProject","_sessionCycle","_finished","_hud","print","race"))){
                lua.set("UIconfig",Map.of("routeOnly","","maxRaces",200,"projects",List.of(Map.of("项目","多人上","data",Map.of("次数",10)),Map.of("项目","多人下","data",Map.of("次数",10))),"ActiveProj",List.of(true,true)));
                lua.load("bootstrap",Files.readString(assets.resolve("lua/bootstrap.lua")));
                lua.load("helpers","data={};run={};utils={backToMain=function()end};function localInit() end;function monitor() sleep(99999999) end;currentMultiplayer={run=function() local done=_sessionProject(data['项目'],10);for i=done+1,10 do race(data['项目']);sleep(1) end end}");
                lua.start("main");try{for(int n=0;n<500&&!finished[0]&&races.size()<220;n++)lua.step(n*2000L);}catch(RuntimeException e){if(!e.getMessage().contains("injected crash"))throw e;}
            }
        }
        check(!finished[0]&&races.size()==220&&cycle[0]==11,"10+10 passes the old 200-race limit without stopping across recovery: "+failAt);
        for(int i=0;i<220;i++)if(!races.get(i).equals(i%20<10?"多人上":"多人下"))throw new AssertionError("Queue order at "+i);
        check(Collections.frequency(races,"多人上")==110&&Collections.frequency(races,"多人下")==110,"110 world and 110 master in alternating blocks");
    }
}
