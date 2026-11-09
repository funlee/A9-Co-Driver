package dev.funlee.a9.core;
import java.nio.file.*;
import java.util.*;

final class DailyEventChecks {
    static int run(Path assets)throws Exception {
        String[][] scenes={{"比赛结果 你的用时 00:29:145 排名 玩家 车名 时间 继续","results"},
            {"限时 赛事 每日车辆战利品 喧闹铁路 打破你的最佳用时纪录","loading"},
            {"每日车辆战利品 俱乐部都奖励 单人奖励 比賽 最佳用时纪录","event"},
            {"恭喜！你获得了这些奖励 特殊卡牌 继续","cards"},
            {"红币 多余的图纸或奖励将被兑换为红币 确定","conversion"},
            {"比赛奖励 你的用时 总金币 3200 +100 继续","rewards"},
            {"你的用时 继续","unknown"},{"限时赛事 购买优惠 立即购买","unknown"}};
        for(String[] c:scenes)if(!DailyEventUi.classify(c[0]).equals(c[1]))throw new AssertionError(c[0]);
        String setup="data={};run={data={}};utils={getMapName=function(k)data['多人地图']=k;data['线路']='route' end};"+
            "function keepCapture()end;function cmpColorEx(s)assert(s~=nil,'nil color pattern');return 0 end;function print()end;"+
            "taps=0;rewards=0;function tap(x,y)assert(x>=0 and y>=0);taps=taps+1 end;"+
            "function _sessionLegacyRewards(c,f)rewards=rewards+1;coins=c;fame=f end;"+
            "local i=0;function _dailyEventUi()i=i+1;return screens[math.min(i,#screens)] end;";
        String continueButton="words={{text='继续',x=1100,y=650}}";
        List<String> cases=List.of(
            "UIconfig={cupTicketAD=''};utils.defultCfg=function(v,d)return v or d end;function keyPress()end;function toast()end;assert(dailyCup.operation[6][2]()=='noTicket')",
            "screens={{scene='results',"+continueButton+"},{scene='cards',"+continueButton+"},{scene='conversion',words={{text='确定',x=1100,y=650}}},{scene='event'},{scene='event'}};assert(dailyCup.operation[8][2]()==0);assert(taps==3 and rewards==0)",
            "screens={{scene='rewards',rewardCanContinue=true,rewardReady=true,rewardCoins=3200,rewardFame=100,"+continueButton+"},{scene='rewards',rewardCanContinue=true,"+continueButton+"},{scene='event'},{scene='event'}};assert(dailyCup.settle()==0);assert(rewards==1 and coins==3200 and fame==100)",
            "screens={{scene='rewards',rewardCanContinue=true,rewardReady=true,rewardCoins=3200,rewardFame=105,words={{text='观看广告',x=1100,y=650},{text='错失机会',x=1050,y=660}}},{scene='event'},{scene='event'}};tap=function(x,y)assert(y==1050,'ad clicked');taps=taps+1 end;assert(dailyCup.settle()==0);assert(taps==1 and rewards==1 and coins==3200)",
            "screens={};for i=1,12 do screens[#screens+1]={scene='rewards',rewardCanContinue=false,rewardReady=false} end;for i=1,3 do screens[#screens+1]={scene='rewards',rewardCanContinue=true,rewardReady=false,words={}} end;screens[#screens+1]={scene='rewards',rewardCanContinue=true,rewardReady=true,rewardCoins=4173,rewardFame=128,"+continueButton+"};screens[#screens+1]={scene='event'};screens[#screens+1]={scene='event'};assert(dailyCup.settle()==0);assert(taps==1 and rewards==1 and coins==4173 and fame==128)",
            "screens={{scene='cards',"+continueButton+"},{scene='event'},{scene='unknown'},{scene='event'},{scene='event'}};assert(dailyCup.settle()==0);assert(taps==1)",
            "screens={{scene='event'}};assert(not pcall(dailyCup.settle));assert(taps==0)",
            "screens={{scene='unknown',"+continueButton+"}};assert(not pcall(dailyCup.settle));assert(taps==0)",
            "screens={{scene='loading',route='旧金山喧闹铁路'},{scene='loading',route='旧金山喧闹铁路'},{scene='race'}};dailyCup.readMap();assert(data['多人地图']=='旧金山喧闹铁路' and data['线路']=='route')",
            "data['线路']='stale';screens={{scene='loading',route='old'},{scene='race'}};dailyCup.readMap();assert(data['线路']=='' and data['多人地图']=='')"
        );
        for(String script:cases)try(LuaRuntime lua=new LuaRuntime((name,l)->0,List.of())){
            lua.load("setup",setup);lua.load("daily",Files.readString(assets.resolve("lua/daily.lua")));
            lua.load("case","function test()"+script+" end");lua.start("test");boolean alive=true;
            for(int i=0;i<400&&alive;i++)alive=lua.step(i*1000L);
            if(alive)throw new AssertionError("Daily flow did not terminate");
        }
        if(DailyEventUi.rewardExit("观看广告",1100,650)||DailyEventUi.rewardExit("错失机会",1100,400)||!DailyEventUi.rewardExit("错失机会",1100,650))throw new AssertionError("Ad exit guard");
        return scenes.length+cases.size()+1;
    }
}
