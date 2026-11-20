package dev.funlee.a9.core;
import java.nio.file.*;
import java.util.*;

final class DailyEventChecks {
    static int run(Path assets)throws Exception {
        String[][] scenes={{"比赛结果 你的用时 00:29:145 排名 玩家 车名 时间 继续","results"},
            {"限时 赛事 每日车辆战利品 喧闹铁路 打破你的最佳用时纪录","loading"},
            {"每日车辆战利品 俱乐部都奖励 单人奖励 比賽 最佳用时纪录","event"},
            {"每日车辆战利品 俱乐部奖励 单人奖励 使用S级车辆打破25秒纪录 比賽","event"},
            {"恭喜！你获得了这些奖励 特殊卡牌 继续","cards"},
            {"恭喜！你已解锁新的档案头像！档案头像 重型座驾 荣誉称号 稍后查看 前往","profileUnlock"},
            {"恭喜！你已解鎖新的檔案頭像！稍後查看 前往","profileUnlock"},
            {"恭喜! 你己解锁新的档案头像! 稍后查査看 重型座驾 荣誉称祢号 档案头像 前往","profileUnlock"},
            {"恭喜! 你巴解锁新的挡案头像! 稍后查着 重型座驾 荣誉祢号 档案头像 前往","profileUnlock"},
            {"档案头像 已解锁 前往","unknown"},
            {"名人堂 新人组 特殊卡牌 进口零件 升级 氮气推进 继续","cards"},
            {"名人堂 新入組 金币 24,888 继续","cards"},
            {"比赛奖励 你的用时 名人堂 继续","unknown"},
            {"名人堂 特殊卡牌 立即购买 继续","unknown"},
            {"红币 多余的图纸或奖励将被兑换为红币 确定","conversion"},
            {"你有定够的图纸来提升车库了! 恭喜! 升星就绪! 新车辆性能分上限 稍后査看 升星","vehicleUnlock"},
            {"你有足够的图纸来提升车库了! 恭喜! 升呈就绪! 新车辆性能分上限 稍后查看 升星","vehicleUnlock"},
            {"车库 车辆升星 稍后查看","unknown"},
            {"比赛奖励 你的用时 总金币 3200 +100 继续","rewards"},
            {"比賽奖励 你的用时00:32:881 总全币 G4,200 +165 继续","rewards"},
            {"你的用时 继续","unknown"},{"限时赛事 购买优惠 立即购买","unknown"}};
        for(String[] c:scenes)if(!DailyEventUi.classify(c[0]).equals(c[1]))throw new AssertionError(c[0]);
        if(!RaceRewards.totalCoinsLabel("总全币")||RaceRewards.totalCoinsLabel("总全币 4,200")||
                RaceRewards.page("总全币 4,200 继续"))
            throw new AssertionError("Accept the observed OCR error in the total label without weakening its boundaries");
        String attributes="性能分 最大速度 加速 操控性 氮气";
        if(!DailyEventUi.classifySolo(attributes+" 最高阶段 仓库 你已达到最高等级").equals("vehicleUpgrade")||
                !DailyEventUi.classifySolo(attributes+" TOUCHDRIVE 升级 开始 7/8").equals("car"))
            throw new AssertionError("Vehicle attributes upgrade and the racing car detail are distinct");
        if(!DailyEventUi.classifySolo("筛进条件 已拥有 排序方式 性能分 完成").equals("garageFilter"))
            throw new AssertionError("Garage filter is distinguishable from the car list during task resume");
        String avatar="恭喜！你已解锁新的档案头像！稍后查看 前往";
        if(!MultiplayerUi.classify(avatar).equals("profileUnlock")||!RecoveryReadiness.ready(avatar)||VehicleUnlockGuard.prompt(avatar))
            throw new AssertionError("Profile popup is actionable after recovery without conflating vehicle unlocks");
        if(!MultiplayerUi.classify("升星就绪 新车辆性能分上限 稍后査看 升星").equals("vehicleUnlock")||
                !MultiplayerUi.classify("车辆选择 选择参与此赛事的车辆 升星就绪 30/30").equals("cars")||
                !VehicleUnlockGuard.laterLabel("稍后査看",380,625,500,665)||
                VehicleUnlockGuard.laterLabel("升星",800,625,880,665))
            throw new AssertionError("Star-up prompt must share the vehicle protection and authorize only Later");
        String setup="data={};run={data={}};utils={getMapName=function(k)data['多人地图']=k;data['线路']='route' end};"+
            "function keepCapture()end;function cmpColorEx(s)assert(s~=nil,'nil color pattern');return 0 end;function print()end;"+
            "taps=0;rewards=0;function tap(x,y)assert(x>=0 and y>=0);taps=taps+1 end;"+
            "function _sessionLegacyRewards(c,f)rewards=rewards+1;coins=c;fame=f end;"+
            "local i=0;function _dailyEventUi()i=i+1;return screens[math.min(i,#screens)] end;";
        String continueButton="words={{text='继续',x=1100,y=650}}";
        List<String> cases=List.of(
            "screens={{scene='results',"+continueButton+"},{scene='profileUnlock',words={{text='稍后查看',x=473,y=659},{text='前往',x=808,y=659}}},{scene='event'}};tap=function(x,y)assert(y==1100 or (x==60 and y==473),'Wrong profile action');taps=taps+1 end;assert(dailyCup.settle()==0 and taps==2)",
            "local screen={scene='profileUnlock',words={{text='稍 后 查 看',x=473,y=659},{text='前往',x=808,y=659}}};local reads=0;tap=function(x,y)assert(x==60 and y==473);taps=taps+1 end;assert(dailyCup.dismissProfile(screen,function()reads=reads+1;return {scene='event'}end));assert(taps==1 and reads==1)",
            "local screen={scene='profileUnlock',words={{text='稍后查看',x=808,y=659},{text='前往',x=473,y=659}}};assert(not pcall(dailyCup.dismissProfile,screen,function()return screen end));assert(taps==0)",
            "local screen={scene='profileUnlock',words={{text='稍后查看',x=473,y=659}}};assert(not pcall(dailyCup.dismissProfile,screen,function()return screen end));assert(taps==3,'Stuck popup must have bounded retries')",
            "assert(not dailyCup.dismissProfile({scene='unknown',words={{text='稍后查看',x=473,y=659}}}));assert(taps==0)",
            "for _,label in ipairs({'稍后查査看','稍后查着'}) do assert(dailyCup.dismissProfile({scene='profileUnlock',words={{text=label,x=473,y=659},{text='前往',x=808,y=659}}},function()return {scene='event'}end)) end;assert(taps==2)",
            "local reads=0;_dailyEventUi=function()reads=reads+1;assert(reads<=9,'alternating lobby OCR timed out');if reads==1 then return {scene='cards',words={}} end;return {scene=reads%3==0 and 'event' or 'unknown'} end;assert(dailyCup.settle()==0 and taps==0)",
            "screens={{scene='cards',words={}},{scene='event'},{scene='unknown'},{scene='event'},{scene='event'}};local reads=0;_dailyEventUi=function()reads=reads+1;assert(reads<=5,'brief OCR miss restarted full grace period');return screens[reads] end;assert(dailyCup.settle()==0)",
            "screens={{scene='cards',words={}},{scene='unknown',closeX={x=1115,y=140},words={{text='购买赛季通行证',x=900,y=550}}},{scene='event'}};tap=function(x,y)assert(x==579 and y==1115);taps=taps+1 end;assert(dailyCup.settle()==0 and taps==1)",
            "screens={{scene='vehicleUnlock',closeX={x=1115,y=140},words={}}};assert(not pcall(dailyCup.settle));assert(taps==0)",
            "screens={{scene='vehicleUnlock',unlockLater={x=438,y=645},words={{text='升星',x=841,y=645}}},{scene='event'}};tap=function(x,y)assert(x==74 and y==438,'Wrong star-up action');taps=taps+1 end;assert(dailyCup.settle()==0 and taps==1)",
            "UIconfig={cupTicketAD=''};utils.defultCfg=function(v,d)return v or d end;function keyPress()end;function toast()end;assert(dailyCup.operation[6][2]()=='noTicket')",
            "screens={{scene='results',"+continueButton+"},{scene='cards',"+continueButton+"},{scene='conversion',words={{text='确定',x=1100,y=650}}},{scene='event'},{scene='event'}};assert(dailyCup.operation[8][2]()==0);assert(taps==3 and rewards==0)",
            "screens={{scene='rewards',rewardCanContinue=true,rewardReady=true,rewardCoins=3200,rewardFame=100,"+continueButton+"},{scene='rewards',rewardCanContinue=true,"+continueButton+"},{scene='event'},{scene='event'}};assert(dailyCup.settle()==0);assert(rewards==1 and coins==3200 and fame==100)",
            "screens={{scene='rewards',rewardCanContinue=true,rewardReady=true,rewardCoins=3200,rewardFame=105,words={{text='观看广告',x=1100,y=650},{text='错失机会',x=1050,y=660}}},{scene='event'},{scene='event'}};tap=function(x,y)assert(y==1050,'ad clicked');taps=taps+1 end;assert(dailyCup.settle()==0);assert(taps==1 and rewards==1 and coins==3200)",
            "screens={};for i=1,12 do screens[#screens+1]={scene='rewards',rewardCanContinue=false,rewardReady=false} end;for i=1,3 do screens[#screens+1]={scene='rewards',rewardCanContinue=true,rewardReady=false,words={}} end;screens[#screens+1]={scene='rewards',rewardCanContinue=true,rewardReady=true,rewardCoins=4173,rewardFame=128,"+continueButton+"};screens[#screens+1]={scene='event'};screens[#screens+1]={scene='event'};assert(dailyCup.settle()==0);assert(taps==1 and rewards==1 and coins==4173 and fame==128)",
            "screens={{scene='cards',"+continueButton+"},{scene='event'},{scene='unknown'},{scene='event'},{scene='event'}};assert(dailyCup.settle()==0);assert(taps==1)",
            "screens={{scene='event'}};assert(not pcall(dailyCup.settle));assert(taps==0)",
            "screens={{scene='unknown',"+continueButton+"}};assert(not pcall(dailyCup.settle));assert(taps==0)",
            "screens={{scene='loading',route='旧金山喧闹铁路'},{scene='loading',route='旧金山喧闹铁路'},{scene='race'}};dailyCup.readMap();assert(data['多人地图']=='旧金山喧闹铁路' and data['线路']=='route')",
            "local prompt={scene='car',words={{text='取消',x=510,y=523},{text='仍 然 挑 战',x=915,y=529}}};screens={prompt,prompt,{scene='loading',route='旧金山喧闹铁路'},{scene='loading',route='旧金山喧闹铁路'},{scene='race'}};function tap(y,x)assert(x==915 and y==190);taps=taps+1 end;dailyCup.readMap();assert(taps==1 and data['多人地图']=='旧金山喧闹铁路')",
            "local prompt={scene='unknown',words={{text='任然挑战',x=915,y=529}}};data['线路']='configured';screens={prompt,prompt,{scene='loading'}};dailyCup.readMap(false);assert(taps==1 and data['线路']=='configured')",
            "data['线路']='stale';screens={{scene='loading',route='old'},{scene='race'}};dailyCup.readMap();assert(data['线路']=='' and data['多人地图']=='')"
        );
        for(String script:cases)try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())){
            lua.load("setup",setup);lua.load("daily",Files.readString(assets.resolve("lua/daily.lua")));
            lua.load("case","function test()"+script+" end");lua.start("test");boolean alive=true;
            for(int i=0;i<400&&alive;i++)alive=lua.step(i*1000L);
            if(alive)throw new AssertionError("Daily flow did not terminate");
        }
        if(DailyEventUi.rewardExit("观看广告",1100,650)||DailyEventUi.rewardExit("错失机会",1100,400)||!DailyEventUi.rewardExit("错失机会",1100,650))throw new AssertionError("Ad exit guard");
        String vehicle="TOUCHDRIVE 升级 加速";
        int[] button=new int[720*1280];
        for(int x=980;x<=1235;x++)for(int y=610;y<=687;y++)button[x*720+719-y]=0xffc3fb12;
        PixelFrame trial=new PixelFrame(720,1280,button);
        if(!DailyEventUi.zeroTicketStart("TOUCHDRIVE 升級 加速",List.of(Map.of("text","开始 A0","x",1100,"y",650)),trial)||
                DailyEventUi.zeroTicketStart(vehicle,List.of(Map.of("text","开始10","x",1100,"y",650)),trial)||
                DailyEventUi.zeroTicketStart(vehicle,List.of(Map.of("text","开始0","x",1100,"y",550)),trial))
            throw new AssertionError("Zero-ticket Start requires the exact cost on the enabled vehicle button");
        List<?> mixedIcon=List.of(Map.of("text","开始40","x",1105,"y",650));
        for(int x=1149;x<=1161;x++)for(int y=638;y<=658;y++)
            if(x<=1151||x>=1159||y<=640||y>=656)button[x*720+719-y]=0xff263e45;
        if(!DailyEventUi.zeroTicketStart(vehicle,mixedIcon,new PixelFrame(720,1280,button)))
            throw new AssertionError("Isolate the zero when OCR mistakes the ticket icon for 4");
        for(int y=639;y<=657;y++)button[1170*720+719-y]=0xff263e45;
        if(DailyEventUi.zeroTicketStart(vehicle,mixedIcon,new PixelFrame(720,1280,button)))
            throw new AssertionError("An additional digit cannot pass the zero-cost check");
        return scenes.length+cases.size()+9;
    }
}
