package dev.funlee.a9.core;

import java.nio.file.*;
import java.util.*;

final class StabilityChecks {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static int run(Path assets)throws Exception{
        check(MultiplayerUi.classify("动态奖励 您可以从这组奖励中选一个").equals("rewardChoice"),"Dynamic selection recognized before Confirm appears");
        check(MultiplayerUi.classify("你的俱乐部达成了该里程碑 万能图纸 稍后领取 前往领奖").equals("milestoneLater"),"Observed club milestone is distinct from claiming rewards");
        check(!MultiplayerUi.classify("俱乐部里程碑 前往领奖 立即购买").equals("milestoneLater"),"Do not treat other milestone or purchase views as safe Later");
        List<String> milestoneTaps=new ArrayList<>();
        try(LuaRuntime lua=new LuaRuntime((name,l)->{
            if(name.equals("_multiplayerUi")){LuaRuntime.push(l,Map.of("scene","milestoneLater","text","你的俱乐部达成了该里程碑 稍后领取 前往领奖",
                "words",List.of(Map.of("text","稍后领取","x",450,"y",598),Map.of("text","前往领奖","x",830,"y",598))));return 1;}
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
        int[] dialog=new int[720*1280];Arrays.fill(dialog,0xff102030);
        for(int d=-16;d<=16;d++)for(int b=-1;b<=1;b++){
            dialog[(1090+d)*720+719-(245+d+b)]=0xffffffff;
            dialog[(1090+d)*720+719-(245-d+b)]=0xffffffff;
        }
        check(PopupCloseDetector.find(new PixelFrame(720,1280,dialog))==null&&PopupCloseDetector.findDialog(new PixelFrame(720,1280,dialog))!=null,"Error dialog close uses its own lower region, preserving promotion detector");
        int[] pixels=new int[720*1280];Arrays.fill(pixels,0xff112030);
        for(int x=100;x<260;x++)for(int y=240;y<440;y++)pixels[x*720+719-y]=0xffffdd55;
        for(int x=400;x<560;x++)for(int y=240;y<440;y++)pixels[x*720+719-y]=0xffdd77ff;
        for(int x=1240;x<1280;x++)for(int y=240;y<440;y++)pixels[x*720+719-y]=0xffffdd55;
        check(DynamicRewards.cards(new PixelFrame(720,1280,pixels)).size()==2,"Complete cards found; partial edge card excluded");
        List<String> taps=new ArrayList<>();int[] phase={0};
        try(LuaRuntime lua=new LuaRuntime((name,l)->{
            if(name.equals("_multiplayerUi")){
                LuaRuntime.push(l,Map.of("scene","rewardChoice","text","动态奖励 从这组奖励中选一个","closeX",Map.of("x",1210,"y",30),
                        "rewardChoices",List.of(Map.of("x",180,"y",350),Map.of("x",480,"y",350)),
                        "words",phase[0]==0?List.of():List.of(Map.of("text","确认","x",640,"y",605))));return 1;
            }
            if(name.equals("rnd")){l.push(2);return 1;}
            if(name.equals("tap")){taps.add(l.toInteger(1)+","+l.toInteger(2));phase[0]++;}
            return 0;
        },List.of("_multiplayerUi","_sessionProject","_hud","print","tap","rnd"))){
            lua.set("data",Map.of("项目","多人上"));lua.set("run",Map.of("data",Map.of("次数",1)));
            lua.load("helpers","utils={defultCfg=function(v,d) return v or d end}");
            lua.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));lua.start("currentMultiplayer.run");
            for(int n=0;n<8&&taps.size()<2;n++)lua.step(n*2000L);
        }
        check(taps.equals(List.of("369,480","114,640")),"Random selected card then Confirm; never closes free choice popup");
        for(String errorScene:List.of("serverError","noOpponents","raceCancelled")){
        int maxRetries=errorScene.equals("serverError")?3:5;
        String expectedError=errorScene.equals("serverError")?"服务器连续返回错误":"连续未找到对手";
        int[] errors={0},results={0},reads={0};boolean limited=false;
        try(LuaRuntime lua=new LuaRuntime((name,l)->{
            if(name.equals("_multiplayerUi")){Map<String,Object> screen=new HashMap<>(Map.of("scene",errorScene,"text","服务器错误 无法在服务器上执行操作(3)","words",List.of(Map.of("text","继续","x",1100,"y",600))));if(reads[0]++>=3)screen.put("closeX",Map.of("x",1090,"y",245));LuaRuntime.push(l,screen);return 1;}
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
        carAlignment(assets);
        fuelSkip(assets);
        skippedQueue(assets);
        for(boolean skip:List.of(false,true))for(boolean crash:List.of(false,true))boundedCycles(assets,skip,crash);
        for(int failAt:List.of(0,119,200))boundedQueue(assets,failAt);
        return checks;
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
                    LuaRuntime.push(l,n==0?Map.of("scene","series","text","世界系列赛 大师系列赛","words",List.of(Map.of("text","大师系列赛","x",640,"y",450))):
                        n==1?Map.of("scene","event","text","大师系列赛 里程碑 开始","words",List.of(Map.of("text","开始","x",1100,"y",650))):
                        Map.of("scene","cars","text","车辆选择 选择参与此赛事的车辆",
                            "emptyFuelSlots",testCase==1?List.of(1,2,3,4,5):testCase==3?List.of():List.of(1,2,3,4,5,6),
                            "words",testCase==3?List.of(Map.of("text","0/3","x",240,"y",390)):List.of()));return 1;
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
                catch(RuntimeException e){if(!e.getMessage().contains("没有已配置且有燃油的车辆"))throw e;blocked=true;}
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
        for(int attempt=0;attempt<2&&!finished[0];attempt++){
            try(LuaRuntime lua=new LuaRuntime((name,l)->{
                switch(name){
                    case "_queueResume":LuaRuntime.push(l,checkpoint.resume());return 1;
                    case "_queueEnter":checkpoint.enter((int)l.toInteger(1),(int)l.toInteger(2),(int)l.toInteger(3));entered.add(l.toInteger(1)+":"+l.toInteger(2));l.push(checkpoint.completed());return 1;
                    case "_queueDone":checkpoint.projectDone();if(cycle[0]==4&&!crashed[0]){crashed[0]=true;throw new IllegalStateException("injected after skip");}break;
                    case "_sessionProject":l.push(checkpoint.completed());return 1;
                    case "_sessionLimitReached":l.push(total[0]>=200);return 1;
                    case "_sessionCycle":cycle[0]=(int)l.toInteger(1);break;
                    case "empty":boolean empty=cycle[0]==4;if(empty)skipped[0]++;l.push(empty);return 1;
                    case "race":total[0]++;if(l.toString(1).equals("多人上"))world[0]++;else master[0]++;checkpoint.completed(1);break;
                    case "_finished":finished[0]=true;break;
                    default:break;
                }return 0;
            },List.of("_queueResume","_queueEnter","_queueDone","_sessionProject","_sessionLimitReached","_sessionCycle","_finished","_hud","print","race","empty"))){
                lua.set("UIconfig",Map.of("routeOnly","","repeatProjects",true,"projects",List.of(Map.of("项目","多人上","data",Map.of("次数",10)),Map.of("项目","多人下","data",Map.of("次数",10))),"ActiveProj",List.of(true,true)));
                lua.load("bootstrap",Files.readString(assets.resolve("lua/bootstrap.lua")));
                lua.load("helpers","data={};run={};function localInit() end;function monitor() sleep(99999999) end;currentMultiplayer={run=function() local done=_sessionProject(data['项目'],10);if empty() then return 'fuelEmpty' end;for i=done+1,10 do race(data['项目']);if _sessionLimitReached() then return end;sleep(1) end end}");
                lua.start("main");try{for(int n=0;n<500&&!finished[0];n++)lua.step(n*2000L);}catch(RuntimeException e){if(!e.getMessage().contains("injected after skip"))throw e;}
            }
        }
        check(finished[0]&&total[0]==200&&world[0]==102&&master[0]==98,"Skipped quota never counts as finishes; existing 78 survive and actual finishes stop at 200");
        check(skipped[0]==1&&entered.subList(0,3).equals(List.of("4:2","5:1","5:2")),"Skip advances to next task even after recovery; next cycle retries skipped mode");
    }
    private static void carAlignment(Path assets)throws Exception{
        int[] frames={0},swipes={0};List<String> taps=new ArrayList<>();
        try(LuaRuntime lua=new LuaRuntime((name,l)->{
            if(name.equals("_multiplayerUi")){
                int n=frames[0]++;
                LuaRuntime.push(l,n==0?Map.of("scene","series","text","世界系列赛 大师系列赛","words",List.of(Map.of("text","大师系列赛","x",640,"y",450))):
                    n==1?Map.of("scene","event","text","大师系列赛 里程碑 开始","words",List.of(Map.of("text","开始","x",1100,"y",650))):
                    Map.of("scene","cars","text","车辆选择 选择参与此赛事的车辆","words",List.of(
                        Map.of("text","2/3","x",swipes[0]<2?240:704,"y",390))));return 1;
            }
            if(name.equals("swipe")){check(l.toInteger(1)==419&&l.toInteger(2)==300&&l.toInteger(4)==1100,"Garage recenter swipes right in card region");swipes[0]++;}
            if(name.equals("tap")&&frames[0]>2)taps.add(l.toInteger(1)+","+l.toInteger(2));
            return 0;
        },List.of("_multiplayerUi","_sessionProject","tap","swipe","_hud","print"))){
            lua.set("data",Map.of("项目","多人下"));lua.set("run",Map.of("data",Map.of("次数",10,"多人下用车",List.of("1","2","3","4"))));
            lua.load("helpers","utils={defultCfg=function(v,d)return v or d end}");
            lua.load("current",Files.readString(assets.resolve("lua/multiplayer_current.lua")));lua.start("currentMultiplayer.run");
            for(int i=0;i<20&&taps.isEmpty();i++)lua.step(i*2000L);
        }
        check(swipes[0]==2&&taps.equals(List.of("414,874")),"Re-read after recenter selects configured car 3, not stale shifted car 1");
    }
    private static void boundedCycles(Path assets,boolean skip,boolean crash)throws Exception{
        TaskCheckpoint checkpoint=new TaskCheckpoint();int[] cycle={0},project={0},races={0},entries={0};boolean[] finished={false},failed={false};
        for(int attempt=0;attempt<2&&!finished[0];attempt++){
            try(LuaRuntime lua=new LuaRuntime((name,l)->{
                switch(name){
                    case "_queueResume":LuaRuntime.push(l,checkpoint.resume());return 1;
                    case "_queueEnter":project[0]=(int)l.toInteger(2);checkpoint.enter((int)l.toInteger(1),project[0],(int)l.toInteger(3));entries[0]++;l.push(checkpoint.completed());return 1;
                    case "_queueDone":checkpoint.projectDone();if(crash&&cycle[0]==5&&project[0]==2&&!failed[0]){failed[0]=true;throw new IllegalStateException("injected final cycle recovery");}break;
                    case "_sessionProject":l.push(checkpoint.completed());return 1;
                    case "_sessionLimitReached":l.push(false);return 1;
                    case "_sessionCycle":cycle[0]=(int)l.toInteger(1);break;
                    case "empty":l.push(skip&&project[0]==2&&checkpoint.completed()>=8);return 1;
                    case "race":races[0]++;checkpoint.completed(1);break;
                    case "_finished":finished[0]=true;break;
                    default:break;
                }return 0;
            },List.of("_queueResume","_queueEnter","_queueDone","_sessionProject","_sessionLimitReached","_sessionCycle","_finished","_hud","print","race","empty"))){
                lua.set("UIconfig",Map.of("routeOnly","","repeatProjects",true,"maxCycles",5,"projects",List.of(Map.of("项目","多人上","data",Map.of("次数",30)),Map.of("项目","多人下","data",Map.of("次数",20))),"ActiveProj",List.of(true,true)));
                lua.load("bootstrap",Files.readString(assets.resolve("lua/bootstrap.lua")));
                lua.load("helpers","data={};run={};function localInit() end;function monitor() sleep(99999999) end;currentMultiplayer={run=function() local done=_sessionProject(data['项目'],run.data['次数']);for i=done+1,run.data['次数'] do if empty() then return 'fuelEmpty' end;race();sleep(1) end end}");
                lua.start("main");try{for(int n=0;n<600&&!finished[0];n++)lua.step(n*2000L);}catch(RuntimeException e){if(!e.getMessage().contains("injected final cycle recovery"))throw e;}
            }
        }
        check(finished[0]&&cycle[0]==5&&entries[0]==10,"Exactly five cycles with no sixth task entry, skip="+skip+", recovery="+crash);
        check(races[0]==(skip?190:250),"Cycle limit counts skipped tasks without invented finishes");
    }
    private static void boundedQueue(Path assets,int failAt)throws Exception{
        TaskCheckpoint checkpoint=new TaskCheckpoint();List<String> races=new ArrayList<>();boolean[] finished={false},failed={false};int[] cycle={0};
        for(int attempt=0;attempt<2&&!finished[0];attempt++){
            try(LuaRuntime lua=new LuaRuntime((name,l)->{
                switch(name){
                    case "_queueResume":LuaRuntime.push(l,checkpoint.resume());return 1;
                    case "_queueEnter":checkpoint.enter((int)l.toInteger(1),(int)l.toInteger(2),(int)l.toInteger(3));l.push(checkpoint.completed());return 1;
                    case "_queueDone":checkpoint.projectDone();break;
                    case "_sessionProject":l.push(checkpoint.completed());return 1;
                    case "_sessionLimitReached":l.push(races.size()>=200);return 1;
                    case "_sessionCycle":cycle[0]=(int)l.toInteger(1);break;
                    case "race":races.add(l.toString(1));checkpoint.completed(1);if(races.size()==failAt&&!failed[0]){failed[0]=true;throw new IllegalStateException("injected crash");}break;
                    case "_finished":finished[0]=true;break;
                    default:break;
                }return 0;
            },List.of("_queueResume","_queueEnter","_queueDone","_sessionProject","_sessionLimitReached","_sessionCycle","_finished","_hud","print","race"))){
                lua.set("UIconfig",Map.of("routeOnly","","repeatProjects",true,"projects",List.of(Map.of("项目","多人上","data",Map.of("次数",10)),Map.of("项目","多人下","data",Map.of("次数",10))),"ActiveProj",List.of(true,true)));
                lua.load("bootstrap",Files.readString(assets.resolve("lua/bootstrap.lua")));
                lua.load("helpers","data={};run={};function localInit() end;function monitor() sleep(99999999) end;currentMultiplayer={run=function() local done=_sessionProject(data['项目'],10);for i=done+1,10 do race(data['项目']);if _sessionLimitReached() then return end;sleep(1) end end}");
                lua.start("main");try{for(int n=0;n<500&&!finished[0];n++)lua.step(n*2000L);}catch(RuntimeException e){if(!e.getMessage().contains("injected crash"))throw e;}
            }
        }
        check(finished[0]&&races.size()==200&&cycle[0]==10,"10+10 stops at exactly 200 across recovery: "+failAt);
        for(int i=0;i<200;i++)if(!races.get(i).equals(i%20<10?"多人上":"多人下"))throw new AssertionError("Queue order at "+i);
        check(Collections.frequency(races,"多人上")==100&&Collections.frequency(races,"多人下")==100,"100 world and 100 master in alternating blocks");
    }
}
