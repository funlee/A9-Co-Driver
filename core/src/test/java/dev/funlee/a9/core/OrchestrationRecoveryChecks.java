package dev.funlee.a9.core;

import java.nio.file.*;
import java.util.*;

/** Replays the failed transitions observed during the two-hour queue run. */
final class OrchestrationRecoveryChecks {
    static int run(Path assets)throws Exception {
        if(!MultiplayerUi.classify("游戏选项 连接 客户服务 游戏设置 游戏信息 退出").equals("options"))throw new AssertionError("Options recovery");
        if(!MultiplayerUi.classify("每日目标 成就 赛季任务 连环目标 领取 世界系列赛 大师系列赛").equals("dailyGoals"))throw new AssertionError("Goals overlay takes priority over series labels");
        if(!DailyEventUi.classifySolo("AUDI R8 阶段1 俱乐部奖励 单人奖励 S+级 完成比赛 跳过30 继续").equals("missionRewards"))throw new AssertionError("Special reward is not a lobby");
        if(!DailyEventUi.classifySolo("阶段1 俱乐部奖励 单人奖励 完成比赛 比赛").equals("event"))throw new AssertionError("Special lobby");
        if(!DailyEventUi.classifySolo("门票不足 获取更多门票").equals("noTicket"))throw new AssertionError("Semantic ticket guard");
        if(!DailyEventUi.classifySolo("降低您的危险等级 任务需要一定数量的空危险槽 您要立刻清空所有槽位吗 清空所有槽位 300 观看广告 15/15").equals("noTicket"))throw new AssertionError("Club hazard dialog reads as noTicket");
        if(DailyEventUi.classifySolo("铬蛇竞速2 任务 1983/7888 特别任务 0/60 危险等级 2 危险等级 3").equals("noTicket"))throw new AssertionError("A club task panel is not the hazard dialog");
        if(!DailyEventUi.classifySolo("极速回溯 LAMBORGHINI TERZO MILLENNIO 单人奖励 排名奖励 前30% 顶尖排名 100% 比賽").equals("event"))throw new AssertionError("Ranked cup reward tab is a returned lobby");
        if(DailyEventUi.classifySolo("单人奖励 排名奖励 前30% 顶尖排名 100%").equals("event"))throw new AssertionError("Reward preview without Race is not a lobby");
        if(!DailyEventUi.classifySolo("金币争夺战：沙漠高速 单人奖励 40,000 金币 1/1 使用最低性能分为2700的车辆打破01:38秒的纪录 最佳用时纪录 01:37.846 比赛").equals("event"))throw new AssertionError("Credit cup lobby has no club or ranking tab");
        if(!DailyEventUi.classifySolo("寻车赛事:Aston Martin V12 Speedster 是佳用时纪录 单人奖励 寻车賽事包 打破比赛用时纪录:44秒 比賽").equals("event"))throw new AssertionError("Car hunt lobby remains identifiable when OCR misreads the best time label");
        if(DailyEventUi.classifySolo("寻车赛事 单人奖励 打破比赛用时纪录:44秒 继续").equals("event"))throw new AssertionError("Car hunt result overlay is not a returned lobby");
        if(!DailyEventUi.classifySolo("车辅选择 选择参与此賽事的车辆。 PORSCHE 911 Targa 4S").equals("cars"))throw new AssertionError("Car hunt garage remains identifiable after an unavailable car selection");
        if(!DailyEventUi.classifySolo("车辆选擇 选择参与此赛事的车辆。 LANCER EVOLUTION").equals("cars"))throw new AssertionError("Class D Cup garage remains identifiable with mixed OCR variants");
        if(!DailyEventUi.classifySolo("性能分 1,567/1,920 B 立刻获取! EXOTIC RIDES W70 23/35 發得该车網的图纸以解因取图纸").equals("lockedCar"))throw new AssertionError("Locked vehicle remains distinct from unavailable fuel");
        if(!DailyEventUi.classifySolo("DEVEL SIXTEEN 性能分 4,071/4,185 获取这把车钥匙以解锁它! 获取钥匙!").equals("lockedCar"))throw new AssertionError("Key-locked vehicle remains distinct from unavailable fuel");
        if(!DailyEventUi.classifySolo("RAESR TARTARUS 性能分 4,097/4,260 获得该车辆的图紙以解锁 但获取图纸").equals("lockedCar"))throw new AssertionError("Mixed OCR script must still identify a blueprint-locked vehicle");
        if(DailyEventUi.classifySolo("金币争夺战 单人奖励 最佳用时纪录 01:37.846").equals("event"))throw new AssertionError("Solo reward preview needs a Race control");
        if(DailyEventUi.classifySolo("单人奖励 最佳用时纪录 完成比赛 继续").equals("event"))throw new AssertionError("Continue overlay is not a returned lobby");
        if(!DailyEventUi.classifySolo("AUDI R8 LMP1 2000 性能分 4889 S 免费试驾 TOUCHDRIVE 0/3 跳过").equals("car"))throw new AssertionError("Special may return directly to an empty-fuel vehicle");
        if(!DailyEventUi.classifySolo("每日车辆战利品 俱乐部奖励 单人奖励 10/10 已领取 必须有足够的活跃俱乐部成员完成条件").equals("clubRewards"))throw new AssertionError("Claimed club tab has no Race button");
        script(assets,List.of("daily"),"data={};local reads=0;local taps=0;function tap(x,y)assert(x==519 and y==1080);taps=taps+1 end;"+
            "function _soloEventUi()reads=reads+1;return {scene=reads<3 and 'clubRewards' or 'event',words={{text='单人奖励',x=1080,y=200}}}end;"+
            "function test()assert(dailyCup.returnToSolo(_soloEventUi()) and taps==1,'Restore solo tab without repeated taps');assert(not dailyCup.returnToSolo({scene='missionRewards'}));done=true end");
        script(assets,List.of("daily"),"data={};local taps=0;function tap()taps=taps+1 end;function _soloEventUi()return {scene='clubRewards',words={{text='单人奖励',x=1080,y=200}}}end;"+
            "function test()assert(not pcall(function()dailyCup.returnToSolo(_soloEventUi())end));assert(taps==3,'Unchanged tab stops after bounded retries');done=true end");
        script(assets,List.of("daily"),"data={};function tap()error('Must not tap a label outside the tab strip')end;function _soloEventUi()return {scene='clubRewards',words={{text='单人奖励',x=1080,y=650}}}end;"+
            "function test()assert(not pcall(function()dailyCup.returnToSolo(_soloEventUi())end));done=true end");
        script(assets,List.of("workflows","daily"),"data={['项目']='每日战利品',['杯赛']=0};run.data={['次数']='1'};local restored=false;local reads=0;utils={tipsHandle=function()end};run.rebootHandle=function()end;"+
            "function _soloEventUi()reads=reads+1;return {scene=restored and (reads==2 and 'event' or 'car') or 'clubRewards',text='每日车辆战利品',words={{text='单人奖励',x=1080,y=200}}}end;function tap()restored=true end;function keepCapture()error('Club tab must precede legacy color dispatch')end;"+
            "run.carDetails={['每日战利品']=function()return 'fuelEmpty' end};function test()assert(run.project()=='fuelEmpty' and restored and data['杯赛']==0);done=true end");
        script(assets,List.of("workflows","daily","cup"),"data={['项目']='杯赛'};run.data={['次数']='1'};local reads,starts,confirmations,tips=0,0,0,0;"+
            "utils={tipsHandle=function()tips=tips+1 end};run.nitro=function()end;run.rebootHandle=function()end;function print()end;"+
            "function _soloEventUi()reads=reads+1;if reads<=2 then return {scene='car'} elseif reads<=4 then return {scene='unknown',words={{text='仍然挑战',x=920,y=530}}} elseif reads<=6 then return {scene='unknown'} else return {scene='race'} end end;"+
            "function tap(y,x)assert(y==189 and x==920);confirmations=confirmations+1 end;function keepCapture()end;function cmpColorEx()return reads==7 and 1 or 0 end;"+
            "run.carDetails={['杯赛']=function()starts=starts+1;utils.soloStartAt=utils.soloStartAt or tickCount()end};"+
            "run.list={['杯赛']={{'race',function()assert(reads==7);return 'raceSeen'end}}};"+
            "function test()assert(run.project()=='raceSeen' and starts==2 and confirmations==1 and tips==6);done=true end");
        script(assets,List.of("special"),"data={};local taps=0;local routes=0;utils={prepareTaskRoute=function()routes=routes+1 end};"+
            "function print()end;function tap()taps=taps+1 end;function _soloEventUi()error('Teaching must return to page dispatch, not block in an observer')end;"+
            "function test()spCup.operation[1][2]();assert(taps==1 and routes==1 and utils.soloStartAt~=nil);done=true end");
        script(assets,List.of("workflows","daily","special"),String.join("\n",
            "data={['项目']='特殊赛事',['特赛']=0};run.data={['次数']='1'};local tips=0;local completed=0",
            "utils={prepareTaskRoute=function()end,taskRouteAuto=function()return false end,specialStageList=function()return false end,tipsHandle=function()tips=tips+1 end};function print()end;function tap()end",
            "run.rebootHandle=function()end;run.carDetails={['特殊赛事']=function()end};run.list={['特殊赛事']={}}",
            "function _soloEventUi()return {scene='unknown',text='新游戏机制 氮气爆发或360度旋转击破赛道上的物品',words={{text='开始',x=1157,y=646}}}end",
            "function _sessionLegacyCompleted()completed=completed+1 end",
            "function test()local ok,reason=pcall(run.project);assert(not ok and reason:find('未确认进入比赛'));assert(tips>2 and completed==0 and data['特赛']==0,'Repeated tutorial must stay bounded without consuming race quota');done=true end"));
        script(assets,List.of("special"),"local start={text='开始',x=1128,y=652};local ignore={text='不再提示',x=120,y=652};"+
            "local screen={scene='unknown',text='新游戏机制 年破障碍 进行氮气爆发或360度旋转击破賽道上的物品',words={ignore,start}};"+
            "function test()assert(spCup.tutorialButton(screen)==start);screen.text='360度旋转';assert(not spCup.tutorialButton(screen));done=true end");
        script(assets,List.of("workflows","daily","special"),String.join("\n",
            "data={['项目']='特殊赛事',['特赛']=0};run.data={['次数']='1'};local phase='tutorial';local taps,tips=0,0",
            "utils={prepareTaskRoute=function()end,taskRouteAuto=function()return false end,specialStageList=function()return false end,tipsHandle=function()tips=tips+1 end};function print()end;run.rebootHandle=function()end",
            "run.carDetails={['特殊赛事']=function()error('Loading must not execute car actions without recognized car')end}",
            "function _soloEventUi()return phase=='tutorial' and {scene='unknown',text='新游戏机制 360度旋转击破赛道上的物品',words={{text='开始',x=1157,y=646}}} or {scene='unknown'} end",
            "function tap()taps=taps+1;if taps==2 then phase='loading' end end;function keepCapture()end",
            "function cmpColorEx(pattern)return phase=='loading' and pattern=='680|50|FFFFFF-101010,660|70|FFFFFF-101010' and 1 or 0 end",
            "run.list={['特殊赛事']={{'680|50|FFFFFF-101010,660|70|FFFFFF-101010',function()assert(not utils.soloStartAt and taps==2 and tips==2);return 'raceSeen'end}}}",
            "function test()assert(run.project()=='raceSeen' and data['特赛']==0,'Color race fallback must remain live after unchanged teaching');done=true end"));
        script(assets,List.of("workflows"),"data={['项目']='特殊赛事',['特赛']=0};run.data={['次数']='1'};utils={};local completed=0;"+
            "function keepCapture()end;function cmpColorEx()return 1 end;function _sessionLegacyCompleted()completed=completed+1 end;run.list={['特殊赛事']={{'screen',function()return -1 end}}};"+
            "function test()local ok=pcall(run.project);assert(not ok and completed==0 and data['特赛']==0);done=true end");
        script(assets,List.of("daily","cup","special"),"data={['项目']='特殊赛事'};local reads=0;local taps=0;function print()end;function tap(x,y)assert(x==69 and y==1100,'Only Continue may be tapped');taps=taps+1 end;"+
            "function _soloEventUi()reads=reads+1;local scene=reads<5 and 'missionRewards' or 'event';return {scene=scene,frameId=1000+reads*1000,words={{text='继续',x=1100,y=650},{text='跳过30',x=900,y=550}}} end;"+
            "function test()assert(spCup.operation[8][2]()==0);assert(taps==1,'Stale reward frames must not repeat taps');done=true end");
        script(assets,List.of("cup","special"),"data={};local backs=0;function tap()error('A no-ticket dialog must not be tapped')end;"+
            "function keyPress(k)assert(k=='back');backs=backs+1 end;function toast()end;function sleep()end;function _soloEventUi()return {scene='unknown'} end;"+
            "function test()assert(cup.operation[6][2]()=='noTicket' and spCup.operation[6][2]()=='noTicket');assert(backs==2,'A colour-verified no-ticket dialog must Back out even when OCR reads no keyword');done=true end");
        script(assets,List.of("daily","special"),"data={['项目']='特殊赛事'};local reads=0;local taps=0;function print()end;function tap()taps=taps+1 end;"+
            "function _soloEventUi()reads=reads+1;return {scene=reads==1 and 'missionRewards' or 'car',frameId=reads*2000,words={{text=reads==1 and '继续' or '跳过',x=1100,y=650}}}end;"+
            "function test()assert(spCup.operation[8][2]()==0 and taps==1,'Finalize on returned car without pressing Start/Skip');done=true end");
        script(assets,List.of("daily","cup","special"),"data={['项目']='特殊赛事'};local reads,taps=0,0;"+
            "utils={prepareTaskRoute=function()end,newAutoDriver=function()return 1 end};function keepCapture()end;function cmpColorEx()return 0 end;"+
            "function _soloEventUi()reads=reads+1;return {scene=reads<3 and 'unknown' or reads<5 and 'results' or 'specialRanking',words={{text='继续',x=1100,y=650}}}end;"+
            "function tap(x,y)assert(x==69 and y==1100);taps=taps+1 end;function test()assert(spCup.operation[7][2]()==0 and taps==1);done=true end");
        script(assets,List.of("daily"),"data={};function print()end;function tap()error('Unconfirmed car page clicked')end;"+
            "function _soloEventUi()return {scene='car',words={{text='开始',x=1100,y=650}}}end;"+
            "function test()assert(not pcall(function()dailyCup.settle('特殊赛事',_soloEventUi)end),'Car without result must not consume quota');done=true end");
        script(assets,List.of("daily"),"data={};local reads=0;function print()end;function tap()error('Ranking lobby must not start another race')end;"+
            "function _soloEventUi()reads=reads+1;return {scene=reads==1 and 'results' or 'specialRanking',words={{text='开始40',x=1100,y=665}}}end;"+
            "function test()assert(dailyCup.settle('特殊赛事',_soloEventUi)==0);done=true end");
        script(assets,List.of("multiplayer_current"),"data={['项目']='多人上'};run={data={['次数']='1'}};utils={defultCfg=function(v,d)return v or d end};local reads=0;local taps=0;function _sessionProject()return 0 end;function _hud()end;function print()end;"+
            "function _multiplayerUi()reads=reads+1;if reads>1 then done=true;sleep(999999)end;return {scene='options',text='',words={}} end;"+
            "function tap(x,y)assert(x==690 and y==38,'Options must use Back, never Exit');taps=taps+1;assert(taps==1) end;function test()currentMultiplayer.run()end");
        script(assets,List.of("multiplayer_current"),"data={['项目']='多人上'};run={data={['次数']='1'}};utils={defultCfg=function(v,d)return v or d end};local reads,backs,taps=0,0,0;function _sessionProject()return 0 end;function _hud()end;function print()end;"+
            "function _multiplayerUi()reads=reads+1;if reads>5 then assert(backs==3 and taps==1,'Stale car frame must step back then fall back to Home once');done=true;sleep(999999)end;return {scene='car',text='',words={}} end;"+
            "function tap(x,y)if x==690 and y==38 then backs=backs+1;assert(backs<=3,'Step back is bounded');return end;assert(x==689 and y==1250 and backs==3,'Car page must step back to the series list before Home');taps=taps+1 end;function test()currentMultiplayer.run()end");
        // The game restores the last multiplayer sub-page. Landing on the other series'
        // event page must step back to the series list instead of bouncing home forever.
        script(assets,List.of("multiplayer_current"),"data={['项目']='多人下'};run={data={['次数']='1'}};utils={defultCfg=function(v,d)return v or d end};local reads,backs=0,0;function _sessionProject()return 0 end;function _hud()end;function print()end;"+
            "function _multiplayerUi()reads=reads+1;if reads>1 then assert(backs==1,'Wrong series event must step back once');done=true;sleep(999999)end;"+
            "return {scene='event',text='幽灵滑流赛 世界系列赛 开始 排行榜 里程碑',words={{text='世界系列赛',x=300,y=400},{text='开始',x=1100,y=650}}} end;"+
            "function tap(x,y)assert(x==690 and y==38,'Wrong series event must use the in-page back arrow');backs=backs+1 end;function test()currentMultiplayer.run()end");
        // A pending season settlement blocks the series entry and must be claimed first.
        script(assets,List.of("multiplayer_current"),"data={['项目']='多人下'};run={data={['次数']='1'}};utils={defultCfg=function(v,d)return v or d end};local reads,claims=0,0;function _sessionProject()return 0 end;function _hud()end;function print()end;"+
            "function _multiplayerUi()reads=reads+1;if reads>1 then assert(claims==1,'Settlement reward must be claimed before entering the series');done=true;sleep(999999)end;"+
            "return {scene='series',text='幽灵滑流赛 世界系列赛 赛季结算 菜鸟 奖励已就绪 大师系列赛',words={{text='世界系列赛',x=300,y=400},{text='赛季结算',x=590,y=430},{text='奖励已就绪',x=590,y=470},{text='领取',x=590,y=526},{text='大师系列赛',x=590,y=300}}} end;"+
            "function tap(x,y)assert(x==193 and y==590,'Only the settlement claim may be tapped');claims=claims+1 end;function test()currentMultiplayer.run()end");
        // Settlement detail page: claim bottom-right, then dismiss the reward display.
        script(assets,List.of("multiplayer_current"),"data={['项目']='多人下'};run={data={['次数']='1'}};utils={defultCfg=function(v,d)return v or d end};local reads,claims,skips=0,0,0;function _sessionProject()return 0 end;function _hud()end;function print()end;"+
            "function _multiplayerUi()reads=reads+1;"+
            "if reads==1 then return {scene='unknown',text='联赛奖励 菜鸟联赛 联赛最终排名的奖励 88,688',words={{text='领取',x=1100,y=650}}} end;"+
            "if reads==2 then return {scene='unknown',text='恭喜 你获得了这些奖励 点击任意位置跳过',words={}} end;"+
            "assert(claims==1 and skips==1,'Settlement page must claim then dismiss');done=true;sleep(999999)end;"+
            "function tap(x,y)if x==69 and y==1100 then claims=claims+1;return end;assert(x==169 and y==640,'Dismiss by tapping the page');skips=skips+1 end;function test()currentMultiplayer.run()end");
        script(assets,List.of("bootstrap","special","multiplayer_current"),String.join("\n",
            "data={};run={};local reads,taps=0,0;function localInit()end;function monitor()end;function _hud()end;function print()end",
            "UIconfig={routeOnly='',ActiveProj={true},projects={{['项目']='多人上',data={['次数']='1'}}}}",
            "utils={defultCfg=function(v,d)return v or d end,backToMain=function()error('Resumed tutorial must stay in the current race')end}",
            "local tutorial={scene='unknown',text='不廿落后？ 获得氮气加速 滑流',words={{text='开始',x=1157,y=646},{text='不再提示',x=150,y=640}}}",
            "function _queueResume()return {cycle=1,index=1,completed=0,restored=true}end",
            "function _sessionCycle()end;function _sessionProject()return 0 end;function _soloEventUi()return tutorial end",
            "function _multiplayerUi(_,raceExpected)assert(raceExpected);reads=reads+1",
            "if reads==4 then assert(taps==1);done=true;sleep(999999)end;return tutorial end",
            "function tap(x,y)assert(x==73 and y==1157);taps=taps+1 end;function test()main()end"));
        script(assets,List.of("multiplayer_current"),"data={['项目']='多人上'};run={data={['次数']='1'}};utils={defultCfg=function(v,d)return v or d end};local reads=0;local taps=0;function _sessionProject()return 0 end;function _hud()end;function print()end;"+
            "function _multiplayerUi()reads=reads+1;if reads>1 then assert(taps==1);done=true;sleep(999999)end;return {scene='dailyGoals',text='',words={{text='领取',x=1100,y=220},{text='观看广告',x=1150,y=610},{text='X',x=1209,y=110}}} end;"+
            "function tap(x,y)assert(x==609 and y==1209,'Only goals Close may be used');taps=taps+1 end;function test()currentMultiplayer.run()end");
        script(assets,List.of("multiplayer_current"),"data={['项目']='多人上'};run={data={['次数']='3'}};utils={defultCfg=function(v,d)return v or d end};local reads,taps=0,0;function _sessionProject()return 2 end;function _hud()end;function print()end;"+
            "function _multiplayerUi()reads=reads+1;if reads>3 then assert(taps==1);done=true;sleep(999999)end;return {scene='leagueDemotion',text='',words={},demotionConfirm={x=633,y=528}} end;"+
            "function tap(x,y)assert(x==191 and y==633,'Only confirm league demotion');taps=taps+1 end;function test()currentMultiplayer.run()end");
        for(int fuel:List.of(0,1,3)){
            if(FuelAvailability.detail(List.of(Map.of("text","肉"+fuel+"/3","x",541,"y",665)))!=fuel)throw new AssertionError("Detail fuel counter");
        }
        for(int fuel:List.of(0,9,10)){
            if(FuelAvailability.detail(List.of(Map.of("text",fuel+" / 10","x",562,"y",665)))!=fuel)
                throw new AssertionError("Ten-slot detail fuel counter");
        }
        if(FuelAvailability.focusedDetail("1 0／\n1 0")!=10||FuelAvailability.focusedDetail("99/10")!=-1)
            throw new AssertionError("Isolated fuel counter may split digits around the slash");
        for(Map<String,Object> w:List.of(Map.<String,Object>of("text","0/10","x",1150,"y",183),Map.<String,Object>of("text","0/3","x",300,"y",300),Map.<String,Object>of("text","0/30","x",540,"y",665),Map.<String,Object>of("text","4/3","x",540,"y",665))){
            if(FuelAvailability.detail(List.of(w))!=-1)throw new AssertionError("Non-fuel ratio accepted");
        }
        script(assets,List.of("workflows"),"data={['项目']='特殊赛事',['特赛']=0};run.data={['次数']='3'};utils={};local completed=0;"+
            "function keepCapture()end;function cmpColorEx()return 1 end;function _sessionLegacyCompleted()completed=completed+1 end;run.list={['特殊赛事']={{'screen',function()return 'fuelEmpty' end}}};"+
            "function test()assert(run.project()=='fuelEmpty' and completed==0 and data['特赛']==0,'Empty skips without consuming quota');done=true end");
        script(assets,List.of("daily","cup","special","club"),"data={};utils={taskCars=function()return {1,2}end,chooseCarCup=function()return 'fuelEmpty' end,prepareTaskRoute=function()end,startConfiguredRace=function()return 'fuelEmpty' end};"+
            "function _dailyTaskStatus()return '' end;run={index=1};function test()"+
            "for _,flow in ipairs({dailyCup,cup,spCup})do assert(flow.operation[4][2]()=='fuelEmpty');assert(flow.operation[5][2]()=='fuelEmpty')end;"+
            "assert(dsCup.operation[3][2]()=='fuelEmpty' and dsCup.operation[4][2]()=='fuelEmpty');done=true end");
        script(assets,List.of("workflows"),"data={['项目']='特殊赛事',['特赛']=0};run.data={['次数']='1'};utils={tipsHandle=function()end};run.carDetails={['特殊赛事']=function()return 'fuelEmpty' end};"+
            "function _soloEventUi()return {scene='car'}end;function keepCapture()end;function cmpColorEx()return 1 end;run.list={['特殊赛事']={{'wrong lobby',function()error('Car misrouted into lobby')end}}};"+
            "function test()assert(run.project()=='fuelEmpty' and data['特赛']==0);done=true end");
        script(assets,List.of("special"),"data={};utils={prepareTaskRoute=function()end};local taps=0;function print()end;function tap()taps=taps+1 end;"+
            "function _soloEventUi()error('No nested polling after start')end;"+
            "function test()spCup.operation[1][2]();local started=utils.soloStartAt;spCup.operation[1][2]();assert(taps==2 and utils.soloStartAt==started,'An unchanged tutorial can be handled again without extending timeout');done=true end");
        script(assets,List.of("special"),"data={};local auto=false;utils={prepareTaskRoute=function()end,startConfiguredRace=function()return 1 end,taskRouteAuto=function()return auto end};"+
            "function _soloEventUi()error('Car start must return to the main loop')end;"+
            "function test()local before=tickCount();assert(spCup.operation[5][2]()==nil and tickCount()-before>=3000);auto=true;before=tickCount();assert(spCup.operation[5][2]()==nil and tickCount()==before,'Automatic route sampling must not wait past a short loading title');done=true end");
        script(assets,List.of("special"),"data={['线路']=''};local reads=0;function print()end;"+
            "utils={soloStartAt=1,getMapName=function(key)assert(reads==4 and key=='target');data['多人地图']=key;data['线路']='0,21' end};"+
            "function _soloEventUi()error('Map observation must use the main-loop frame')end;"+
            "function test()for i=1,4 do reads=i;spCup.observeStart({specialRoute=i==1 and 'other' or 'target',frameId=i==3 and 2 or i})end;assert(data['线路']=='0,21');utils.soloStartAt=nil;spCup.observeStart({});data['线路']='';utils.soloStartAt=2;reads=1;spCup.observeStart({specialRoute='target',frameId=5});assert(data['线路']=='','Do not reuse the previous race candidate');done=true end");
        script(assets,List.of("utilities"),"run={data={['特殊赛事阶段']='2'}};local screen={scene='missionRewards',text='1阶段21',stageLabel='阶段2',words={{text='继续',x=1100,y=650}}};"+
            "function test()assert(utils.specialMissionContinue(screen));screen.stageLabel='阶段21';assert(not utils.specialMissionContinue(screen));done=true end");
        if(!DailyEventUi.classifySolo("1阶段21 具乐部奖励 单人笑励 继续").equals("missionRewards"))
            throw new AssertionError("Recognize the special stage reward page with observed OCR variants");
        script(assets,List.of("utilities"),"run={data={['特殊赛事阶段']='2'}};local badge={text='1阶段21',x=140,y=280};"+
            "local screen={scene='missionRewards',words={badge,{text='继续',x=1060,y=655}}};"+
            "function test()assert(utils.specialMissionContinue(screen));badge.text='阶段21';assert(not utils.specialMissionContinue(screen));"+
            "badge.text='1阶段21';badge.x=800;assert(not utils.specialMissionContinue(screen));done=true end");
        if(!DailyEventUi.classifySolo("N VISION 74 |阶段1 进入前30%即有领笑资 排行榜 T尖排名 开始令0").equals("specialRanking"))
            throw new AssertionError("Ranking stage has a separate Start action");
        script(assets,List.of("utilities"),"run={data={['特殊赛事阶段']='1'}};local badge={text='|阶段1',x=195,y=112};"+
            "local start={text='开始令0',x=1100,y=650};local screen={scene='specialRanking',words={badge,start}};"+
            "function test()assert(utils.specialMissionContinue(screen)==start);badge.text='|阶段2';assert(not utils.specialMissionContinue(screen));done=true end");
        script(assets,List.of("utilities"),"data={};run={data={['特殊赛事阶段']='1'}};local reads,taps=0,0;function print()end;"+
            "local screen={scene='specialRanking',words={{text='阶段1',x=195,y=112},{text='开始',x=1100,y=650}}};"+
            "function tap(y,x)assert(y==69 and x==1100);taps=taps+1 end;"+
            "function _soloEventUi()reads=reads+1;return reads==1 and screen or {scene='car'}end;"+
            "function test()local started=tickCount();assert(utils.continueSpecialMission(screen));assert(taps==1 and reads==2 and tickCount()-started<2500);done=true end");
        script(assets,List.of("utilities"),"data={};local frame,taps=0,0;local fuel=-1;local star,zeroTicket=false,false;utils.mycolor=function(x,y,color)assert(color=='12FBC3-303030');return 1 end;function print()end;"+
            "function _soloEventUi()frame=frame+1;return {scene='car',text=zeroTicket and '' or '免费试驾',zeroTicketStart=zeroTicket,fuel=fuel,starUpPending=star,frameId=frame,words={{text='开始0',x=1100,y=650}}}end;"+
            "function tap(x,y)assert(x==69 and y==1100);taps=taps+1 end;function test()"+
            "for _,mode in ipairs({'杯赛','特殊赛事'}) do data['项目']=mode;assert(utils.startRace()==1) end;"+
            "zeroTicket=true;assert(utils.startRace()==1);data['项目']='杯赛';assert(utils.startRace()==1,'A gaugeless car with a colored start button starts on the pixel verdict');zeroTicket=false;"+
            "for _,mode in ipairs({'每日战利品','车联'}) do data['项目']=mode;assert(utils.startRace()==1) end;data['项目']='杯赛';fuel=0;assert(utils.startRace()=='fuelEmpty');"+
            "star=true;fuel=6;for _,mode in ipairs({'杯赛','特殊赛事','每日战利品','车联'}) do data['项目']=mode;assert(utils.startRace()=='fuelEmpty') end;"+
            "assert(taps==12);done=true end");
        script(assets,List.of("utilities"),String.join("\n",
            "data={['项目']='杯赛'};local taps=0;local ready=true;function print()end;function keepCapture()end",
            "utils.carFuel=function()return 6 end;utils.mycolor=function()return 1 end",
            "function _soloEventUi()return {scene='unknown',words={}}end",
            "function cmpColorEx(pattern)assert(pattern:find('236|142',1,true));return ready and 1 or 0 end;function cmpColor()return 0 end",
            "function tap(y,x)assert(y==80 and x==1080);taps=taps+1 end",
            "function test()local before=tickCount();assert(utils.startRace()==1 and taps==2 and tickCount()-before>=2000,'Color gate starts without button OCR');local started=utils.soloStartAt;assert(utils.startRace()==1 and taps==4 and utils.soloStartAt==started);ready=false;assert(not pcall(utils.startRace) and taps==4,'Unknown car colors must never use fixed Start coordinates');done=true end"));
        script(assets,List.of("workflows","daily","cup"),String.join("\n",
            "data={['项目']='杯赛',['杯赛']=0};run.data={['次数']='1'};local starts,tips=0,0;local phase='car'",
            "utils={tipsHandle=function()tips=tips+1;if phase=='loading' then phase='race' end end};run.rebootHandle=function()end",
            "run.carDetails={['杯赛']=function()starts=starts+1;utils.soloStartAt=tickCount();phase='loading' end}",
            "function _soloEventUi()return {scene=phase}end;function keepCapture()end;function cmpColorEx()return phase=='race' and 1 or 0 end",
            "run.list={['杯赛']={{'680|50|FFFFFF-101010,660|70|FFFFFF-101010',function()assert(starts==1 and tips==1 and not utils.soloStartAt);return 'raceSeen' end}}}",
            "function test()assert(run.project()=='raceSeen' and data['杯赛']==0,'A popup after Start must not be starved by the loading guard');done=true end"));
        script(assets,List.of("utilities"),String.join("\n",
            "data={['项目']='特殊赛事'};local reads,taps=0,0;function print()end;utils.mycolor=function()return 1 end",
            "function _soloEventUi()reads=reads+1;local start={text='开始0',x=1100,y=650}",
            "return {scene=reads==2 and 'unknown' or 'car',text='',fuel=-1,zeroTicketStart=reads~=4,frameId=reads,words={start}} end",
            "function tap(x,y)assert(reads==5 and y==1100);taps=taps+1 end",
            "function test()assert(utils.startRace()==1 and taps==2,'A single missed trial frame must not skip the event');done=true end"));
        script(assets,List.of("utilities"),String.join("\n",
            "data={};local taps=0;function print()end;function sleep()end",
            "function tap(x,y)taps=taps+1;atX,atY=x,y end",
            "function test()",
            "local tip={scene='unknown',frameId=7,text='新游戏机制 冲破障碍 不再提示',words={{text='开始',x=1180,y=640}}}",
            "assert(utils.proceedTipPage(tip) and taps==1 and atX==719-640 and atY==1180,'A mechanic tip page continues through its bottom-right Start button')",
            "assert(not utils.proceedTipPage(tip),'The same projection frame is never tapped twice')",
            "tip={scene='car',frameId=8,text='新游戏机制'};assert(not utils.proceedTipPage(tip),'A recognized car page is left alone')",
            "tip={scene='unknown',frameId=9,text='新游戏机制 不再提示',words={{text='开始',x=200,y=100}}};assert(not utils.proceedTipPage(tip),'Start outside the bottom-right control never gets a tap')",
            "tip={scene='unknown',frameId=10,text='加载中'};assert(not utils.proceedTipPage(tip),'Loading frames are not tips')",
            "tip={scene='loading',frameId=11,text='限时赛事 更新提醒 购买至尊通行证后即可自动领取 不再提示',words={{text='明白了！',x=1115,y=627}}}",
            "assert(utils.proceedTipPage(tip) and atX==719-627 and atY==1115,'An event ad covering the loading page closes through the confirm button')",
            "tip={scene='loading',frameId=12,text='加载中'};assert(not utils.proceedTipPage(tip),'A plain loading page still gets no tap')",
            "done=true end"));
        script(assets,List.of("utilities"),String.join("\n",
            "data={};run={data={['分支选择']='1',['危险等级']='0',['难度不足']='0'}};utils.carClubAtOrigin=true",
            "function print()end;function sleep()end;function toast()end;function keepCapture()end",
            "local taps,reads=0,0",
            "function tap()taps=taps+1 end;function swipe()end;function touchDown()end;function touchMove()end;function touchUp()end",
            "local cards={scene='unknown',words={{text='铬蛇竞速1',x=200,y=240},{text='任务',x=200,y=280},",
            "{text='7888/7888',x=200,y=350},{text='特别任务',x=200,y=420},{text='0/60',x=200,y=470}}}",
            "function _soloEventUi()reads=reads+1;return cards end",
            "function test()assert(utils.spChoose()==-1,'A finished main line is not reopened by the special progress');",
            "assert(taps==0 and reads==2,'A card holding only the special task is skipped without a tap');done=true end"));
        script(assets,List.of("utilities"),String.join("\n",
            "data={};run={data={['分支选择']='0',['危险等级']='0',['难度不足']='0'}};utils.carClubAtOrigin=true",
            "function print()end;function sleep()end;function toast()end;function keepCapture()end",
            "local taps,tx,ty,reads=0,0,0,0",
            "function tap(x,y)taps=taps+1;tx,ty=x,y end;function swipe()end;function touchDown()end;function touchMove()end;function touchUp()end",
            "local cards={scene='unknown',words={{text='速度纪元3',x=200,y=240},{text='任务',x=200,y=280},",
            "{text='1983/7888',x=200,y=350},{text='特别任务',x=200,y=420},{text='0/60',x=200,y=470}}}",
            "local panel={scene='unknown',words={{text='赢取额外车联包',x=800,y=150},",
            "{text='危险等级',x=520,y=654},{text='开始任务',x=1043,y=604}}}",
            "function _soloEventUi()reads=reads+1;if reads==1 then return cards end;return panel end",
            "function _textWords()return {{text='开始任务',x=1043,y=604}}end",
            "function test()assert(utils.spChoose()==1,'The single special branch starts the special task');",
            "assert(taps==4 and tx==1043 and ty==604,'Card tap then the special Start button');done=true end"));
        script(assets,List.of("utilities"),String.join("\n",
            "data={};run={data={['分支选择']='1',['危险等级']='0',['难度不足']='0'}};utils.carClubAtOrigin=true",
            "function print()end;function sleep()end;function toast()end;function keepCapture()end",
            "local taps={};local reads=0",
            "function tap(x,y)taps[#taps+1]={x,y} end;function swipe()end;function touchDown()end;function touchMove()end;function touchUp()end",
            "local cards={scene='unknown',words={{text='铬蛇竞速2',x=200,y=240},{text='任务',x=200,y=280},",
            "{text='1983/7888',x=200,y=350},{text='特别任务',x=200,y=420},{text='0/60',x=200,y=470}}}",
            "local panel={scene='unknown',words={{text='危险等级',x=575,y=393},{text='危险等级',x=575,y=571}}}",
            "function _soloEventUi()reads=reads+1;if reads==1 then return cards end;return panel end",
            "function _textWords()return {{text='开始任务',x=1043,y=604}}end",
            "function test()assert(utils.spChoose()==1,'An unfinished main line starts');",
            "assert(#taps==5 and taps[2][1]==719-393 and taps[2][2]==575,'The top difficulty row is picked');",
            "for i=3,5 do assert(taps[i][1]==116 and taps[i][2]==1100,'The main branch never presses the special task label') end;done=true end"));
        script(assets,List.of("utilities"),String.join("\n",
            "data={};run={data={['分支选择']='1',['危险等级']='2',['难度不足']='0'}};utils.carClubAtOrigin=true",
            "function print()end;function sleep()end;function toast()end;function keepCapture()end",
            "local taps={};local reads=0",
            "function tap(x,y)taps[#taps+1]={x,y} end;function swipe()end;function touchDown()end;function touchMove()end;function touchUp()end",
            "local cards={scene='unknown',words={{text='铬蛇竞速2',x=200,y=240},{text='任务',x=200,y=280},",
            "{text='1983/7888',x=200,y=350},{text='特别任务',x=200,y=420},{text='0/60',x=200,y=470}}}",
            "local panel={scene='unknown',words={{text='危险等级',x=575,y=393},{text='2',x=690,y=393},",
            "{text='危险等级',x=575,y=571},{text='3',x=690,y=571}}}",
            "function _soloEventUi()reads=reads+1;if reads==1 then return cards end;return panel end",
            "function test()assert(utils.spChoose()==1,'Two rows can still start the configured third level');",
            "assert(taps[2][1]==719-571 and taps[2][2]==575,'The row labelled with the configured level wins over row order');done=true end"));
        script(assets,List.of("utilities"),String.join("\n",
            "data={};run={data={['分支选择']='1',['危险等级']='2',['难度不足']='1'}};utils.carClubAtOrigin=true",
            "function print()end;function sleep()end;function toast()end;function keepCapture()end",
            "local taps={};local reads=0",
            "function tap(x,y)taps[#taps+1]={x,y} end;function swipe()end;function touchDown()end;function touchMove()end;function touchUp()end",
            "local cards={scene='unknown',words={{text='速度纪元3',x=200,y=240},{text='任务',x=200,y=280},",
            "{text='1983/7888',x=200,y=350},{text='特别任务',x=200,y=420},{text='0/60',x=200,y=470}}}",
            "local panel={scene='unknown',words={{text='危险等级',x=575,y=393},{text='1',x=690,y=393},",
            "{text='危险等级',x=575,y=571},{text='2',x=690,y=571}}}",
            "function _soloEventUi()reads=reads+1;if reads==1 then return cards end;return panel end",
            "function test()assert(utils.spChoose()==1,'A short chapter still starts');",
            "assert(taps[2][1]==719-571,'Without the configured level the highest available one is used');done=true end"));
        script(assets,List.of("utilities"),String.join("\n",
            "data={};run={data={['分支选择']='1',['危险等级']='2',['难度不足']='0'}};utils.carClubAtOrigin=true",
            "function print()end;function sleep()end;function toast()end;function keepCapture()end",
            "local taps={};local reads=0",
            "function tap(x,y)taps[#taps+1]={x,y} end;function swipe()end;function touchDown()end;function touchMove()end;function touchUp()end",
            "local cards={scene='unknown',words={{text='铬蛇竞速2',x=200,y=240},{text='任务',x=200,y=280},",
            "{text='1983/7888',x=200,y=350},{text='特别任务',x=200,y=420},{text='0/60',x=200,y=470}}}",
            "local panel={scene='unknown',words={{text='危险等级',x=575,y=393},{text='2',x=690,y=393},",
            "{text='危险等级',x=575,y=571},{text='3',x=690,y=571}}}",
            "function _soloEventUi()reads=reads+1;if reads==1 then return cards end;return panel end",
            "function test()assert(utils.spChoose(2)==1,'A capped run still starts a race');",
            "assert(taps[2][1]==719-393 and taps[2][2]==575,'The cap keeps the run on the lower level');",
            "assert(data['车联等级']==2,'The level actually chosen is remembered for the next drop');done=true end"));
        script(assets,List.of("utilities"),String.join("\n",
            "data={};run={data={}}",
            "function test()",
            "assert(utils.carScoreShort({text='AELLA-60 性能分不足，无法参加当前赛事'}),'A car below the race requirement is recognized');",
            "assert(not utils.carScoreShort({text='性能分 2,233/2,233 开始'}),'A healthy car page is not a score problem');",
            "assert(not utils.carScoreShort(nil),'A missing frame is not a score problem');",
            "done=true end"));
        script(assets,List.of("club"),String.join("\n",
            "data={['车联等级']='3'};local backs,cap,looks=0,0,0",
            "utils={carScoreShort=function()return false end,spChoose=function(c)cap=c;return 1 end,",
            "carClubCards=function()looks=looks+1;if looks>1 then return {{name='铬蛇竞速2',x=200,y=280}} end;return {} end,",
            "startConfiguredRace=function()error('未确认车辆开始按钮，保留当前任务') end}",
            "function _soloEventUi()return {text=''} end",
            "function keyPress()backs=backs+1 end;function toast()end;function sleep()end;function tap()end;function print()end",
            "function test()local ret=dsCup.operation[4][2]()",
            "assert(ret==nil and cap==2,'A car that cannot enter the configured level drops one level and retries');",
            "assert(backs==1,'Back stops as soon as the chapter panel is visible instead of leaving the club');",
            "assert(data['信息']=='车辆性能分不足，降到难度2','The drop is reported on the bottom banner');",
            "done=true end"));
        script(assets,List.of("utilities"),String.join("\n",
            "data={};run={data={['分支选择']='1',['危险等级']='0',['难度不足']='0'}};utils.carClubAtOrigin=true",
            "function print()end;function sleep()end;function toast()end;function keepCapture()end",
            "local taps,reads=0,0",
            "function tap()taps=taps+1 end;function swipe()end;function keyPress()end;function touchDown()end;function touchMove()end;function touchUp()end",
            "local cards={scene='unknown',words={{text='速度纪元4',x=200,y=240},{text='任务',x=200,y=280},",
            "{text='1983/7888',x=200,y=350},{text='特别任务',x=200,y=420},{text='0/60',x=200,y=470}}}",
            "local panel={scene='unknown',words={{text='危险等级',x=520,y=654},{text='车联包',x=900,y=560}}}",
            "function _soloEventUi()reads=reads+1;if reads==2 then return panel end;return cards end",
            "function test()assert(utils.spChoose()==-1,'A panel whose Start button was missed is not a main panel');",
            "assert(taps==1,'Only the card is tapped, never the bottom bar');done=true end"));
        script(assets,List.of("utilities"),String.join("\n",
            "data={};run={data={['分支选择']='2',['危险等级']='0',['难度不足']='0'}};utils.carClubAtOrigin=true",
            "function print()end;function sleep()end;function toast()end;function keepCapture()end",
            "local taps,tx,reads=0,0,0",
            "function tap(x,y)taps=taps+1;tx=x end;function swipe()end;function keyPress()end;function touchDown()end;function touchMove()end;function touchUp()end",
            "local cards={scene='unknown',words={{text='速度纪元2',x=200,y=240},{text='任务',x=200,y=280},",
            "{text='2388/2388',x=200,y=350},{text='0/54',x=200,y=470}}}",
            "local panel={scene='unknown',words={{text='赢取额外车联包',x=800,y=150},{text='开始任务',x=1043,y=604}}}",
            "function _soloEventUi()reads=reads+1;if reads==1 then return cards end;return panel end",
            "function _textWords()return {{text='开始任务',x=1043,y=604}}end",
            "function test()assert(utils.spChoose()==1,'A card without the special-task label still opens its special branch');",
            "assert(taps==4 and tx==1043,'The unfinished special task wins over the finished main line');done=true end"));
        script(assets,List.of("utilities"),String.join("\n",
            "data={};run={data={['分支选择']='2',['危险等级']='0',['难度不足']='0'}};utils.carClubAtOrigin=true",
            "function print()end;function sleep()end;function toast()end;function keepCapture()end",
            "local taps,reads=0,0",
            "function tap()taps=taps+1 end;function swipe()end;function keyPress()end;function touchDown()end;function touchMove()end;function touchUp()end",
            "local cards={scene='unknown',words={{text='速度纪元2',x=200,y=260},{text='任务',x=200,y=300},",
            "{text='SP2388/2388',x=272,y=367},{text='特別任务',x=169,y=501},{text='尽0/54',x=218,y=535}}}",
            "local panel={scene='unknown',words={{text='赢取比赛',x=200,y=270},{text='危险等级',x=95,y=430},{text='继续',x=1156,y=640}}}",
            "function _soloEventUi()reads=reads+1;if reads==1 then return cards end;return panel end",
            "function _textWords()return {{text='继续',x=1156,y=640}}end",
            "function test()assert(utils.spChoose()==1,'Progress digits read together with icons still open the chapter');",
            "assert(taps==4,'The special panel starts even without the Start-task label');done=true end"));
        script(assets,List.of("utilities"),String.join("\n",
            "data={};run={data={['分支选择']='2',['危险等级']='0',['难度不足']='0'}};utils.carClubAtOrigin=true",
            "function print()end;function sleep()end;function toast()end;function keepCapture()end",
            "local taps,reads=0,0",
            "function tap()taps=taps+1 end;function swipe()end;function keyPress()end;function touchDown()end;function touchMove()end;function touchUp()end",
            "utils.carClubToOrigin=function() utils.carClubAtOrigin=true end",
            "local a={scene='unknown',words={{text='速度纪元2',x=200,y=240},{text='任务',x=200,y=280},",
            "{text='7000/7888',x=200,y=350},{text='特别任务',x=200,y=420},{text='60/60',x=200,y=470}}}",
            "local b={scene='unknown',words={{text='速度纪元3',x=600,y=240},{text='任务',x=600,y=280},",
            "{text='7000/7888',x=600,y=350},{text='特别任务',x=600,y=420},{text='60/60',x=600,y=470}}}",
            "local panel={scene='unknown',words={{text='危险等级',x=575,y=393}}}",
            "function _soloEventUi()reads=reads+1;if reads<=3 then return reads==1 and a or b end;",
            "if reads==4 then return a end;return panel end",
            "function test()assert(utils.spChoose()==1,'The main line only runs after the strip shows no special task left');",
            "assert(data['车联等级']==1 and taps==5,'Card, difficulty row and the three start taps');done=true end"));
        script(assets,List.of("utilities"),String.join("\n",
            "data={};run={data={['分支选择']='2',['危险等级']='0',['难度不足']='0'}};utils.carClubAtOrigin=true",
            "function print()end;function sleep()end;function toast()end;function keepCapture()end",
            "local taps=0",
            "function tap()taps=taps+1 end;function swipe()end;function keyPress()end;function touchDown()end;function touchMove()end;function touchUp()end",
            "utils.carClubToOrigin=function() utils.carClubAtOrigin=true end",
            "local cards={scene='unknown',words={{text='速度纪元1',x=200,y=240},{text='任务',x=200,y=280}}}",
            "function _soloEventUi()return cards end",
            "function test()assert(utils.spChoose()==-1,'A fully cleared chapter card is never opened');",
            "assert(taps==0,'A cleared card has no progress left, so it is not even tapped');done=true end"));
        script(assets,List.of("utilities"),String.join("\n",
            "data={};run={data={['分支选择']='1',['危险等级']='0',['难度不足']='0'}};utils.carClubAtOrigin=true",
            "function print()end;function sleep()end;function toast()end;function keepCapture()end",
            "local taps,reads=0,0",
            "function tap()taps=taps+1 end;function swipe()end;function touchDown()end;function touchMove()end;function touchUp()end",
            "local cards={scene='unknown',words={{text='铬蛇竞速2',x=200,y=240},{text='任务',x=200,y=280},",
            "{text='1983/7888',x=200,y=350},{text='特别任务',x=200,y=420},{text='0/60',x=200,y=470}}}",
            "local hazard={scene='noTicket',words={{text='降低您的危险等级',x=400,y=300},",
            "{text='任务需要一定数量的空危险槽',x=400,y=360},{text='您要立刻清空所有槽位吗',x=400,y=420}}}",
            "function _soloEventUi()reads=reads+1;if reads==1 then return cards end;return hazard end",
            "function test()assert(utils.spChoose()=='noTicket','A hazard dialog found while reading the panel is a ticket problem');",
            "assert(taps==1,'The ticket problem is reported without pressing Start');done=true end"));
        script(assets,List.of("utilities"),String.join("\n",
            "data={};run={data={['分支选择']='2',['危险等级']='2',['难度不足']='1'}};utils.carClubAtOrigin=true",
            "function print()end;function sleep()end;function toast()end;function keepCapture()end",
            "function tap()error('A score-short race page must not be tapped')end;function swipe()end;function keyPress()end;function touchDown()end;function touchMove()end;function touchUp()end",
            "function _soloEventUi()return {scene='unknown',text='AELLA-60 性能分不足，无法参加当前赛事',words={}}end",
            "function test()assert(utils.spChoose()=='noScore','A score-short race page is handed back for a level drop');",
            "assert(data['信息']=='车辆性能分不足，按难度不足配置降档','The drop is announced to the user');done=true end"));
        script(assets,List.of("utilities"),String.join("\n",
            "data={['车联等级']=3};run={data={['分支选择']='2',['危险等级']='2',['难度不足']='1'}};utils.carClubAtOrigin=false",
            "function print()end;function sleep()end;function toast()end;function keepCapture()end",
            "local taps={}",
            "function tap(x,y)taps[#taps+1]={x,y} end;function swipe()end;function keyPress()end;function touchDown()end;function touchMove()end;function touchUp()end",
            "utils.carClubToOrigin=function() error('A dropped level must be re-picked on the open panel') end",
            "local panel={scene='unknown',words={{text='危险等级',x=575,y=393},{text='2',x=690,y=393},",
            "{text='危险等级',x=575,y=571},{text='3',x=690,y=571}}}",
            "function _soloEventUi()return panel end",
            "function _textWords()return {{text='继续',x=1156,y=640}}end",
            "function test()assert(utils.spChoose(2)==1,'A dropped level is re-picked on the open panel');",
            "assert(taps[1][1]==719-393 and taps[1][2]==575,'The lower allowed row is tapped right away');",
            "assert(data['车联等级']==2,'The re-picked level is remembered');",
            "assert(data['信息']=='性能分不足，刷主线（难度2）','The drop reason is shown to the user');done=true end"));
        script(assets,List.of("utilities"),String.join("\n",
            "data={};run={data={['分支选择']='1',['危险等级']='0',['难度不足']='0'}};utils.carClubAtOrigin=true",
            "function print()end;function sleep()end;function toast()end;function keepCapture()end",
            "function tap()end;function swipe()end;function touchDown()end;function touchMove()end;function touchUp()end",
            "function _soloEventUi()return {scene='unknown',words={{text='空危险槽',x=300,y=400}}} end",
            "function test()assert(utils.spChoose()=='noTicket','A hazard dialog hiding the chapter strip is a ticket problem, not a lost list');done=true end"));
        script(assets,List.of("club"),String.join("\n",
            "data={};utils={spChoose=function()return 'noTicket' end}",
            "local backs=0",
            "function keyPress()backs=backs+1 end;function toast()end;function sleep()end;function tap()end;function print()end",
            "function test()local ret=dsCup.operation[2][2]()",
            "assert(ret=='clubNoTicket','A hazard dialog found while choosing a task skips the club project')",
            "assert(backs==1 and data['信息']=='车联危险值不足，已跳过','The dialog is closed and the run keeps going')",
            "done=true end"));
        script(assets,List.of("club"),String.join("\n",
            "data={}",
            "local backs=0",
            "function keyPress()backs=backs+1 end;function toast()end;function sleep()end;function tap()end;function print()end",
            "function test()local ret=dsCup.operation[6][2]()",
            "assert(ret=='clubNoTicket','A short hazard value skips the club project instead of retrying it')",
            "assert(backs==1 and data['信息']=='车联危险值不足，已跳过','The dialog is closed and the run keeps going')",
            "done=true end"));
        script(assets,List.of("daily"),"data={};local reads,taps=0,0;function print()end;"+
            "function tap(x,y)assert(x==109 and y==450);taps=taps+1 end;"+
            "function _soloEventUi()reads=reads+1;if reads==1 then return {scene='results'} end;"+
            "if reads<=3 then return {scene='unknown',frameId=5000,text='你的俱乐部达成了该里程碑!',words={{text='前往领奖',x=830,y=610},{text='稍后领取',x=450,y=610}}} end;return {scene='car'} end;"+
            "function test()assert(dailyCup.settle('特殊赛事',_soloEventUi)==0 and taps==1);done=true end");
        script(assets,List.of("utilities"),"data={};function _soloEventUi()return {scene='unknown',text='世界系列賽 試驾挑战 大师系列赛 对决'} end;"+
                "function toast()end;function tap()error('Series selector already on home')end;function test()utils.backToMain();done=true end");
        script(assets,List.of("utilities"),String.join("\n",
                "data={};local taps=0;function toast()end;function keepCapture()end;function releaseCapture()end",
                "function _soloEventUi() if taps==1 then return {scene='home'} end",
                "return {scene='unknown',text='俱乐部里程碑 领取',homeButton={x=1249,y=26}} end",
                "function cmpColorEx(pattern)return pattern:find('690|1240',1,true) and 1 or 0 end",
                "function tap(y,x)assert(y==693 and x==1249);taps=taps+1 end",
                "function test()utils.backToMain();assert(taps==1);done=true end"));
        script(assets,List.of("utilities"),String.join("\n",
            "data={};run={data={['称霸模式']='0'}};function test()",
            "assert(utils.specialStageList({scene='unknown',words={{text='章节1',x=165,y=250}}}))",
            "assert(not utils.specialStageList({scene='home',words={{text='章节1',x=165,y=250}}}))",
            "assert(not utils.specialStageList({scene='unknown',words={{text='章节1',x=800,y=250}}}))",
            "run.data['称霸模式']='1';assert(not utils.specialStageList({scene='unknown',words={{text='章节1',x=165,y=250}}}));done=true end"));
        script(assets,List.of("workflows","utilities"),String.join("\n",
            "data={['项目']='特殊赛事',['特赛']=0};run.data={['次数']='3',['特殊赛事阶段']='5',['称霸模式']='0'}",
            "local phase='missionRewards';local completed=0;local continued=0;local selected=0",
            "function print()end;utils.tipsHandle=function()end;run.rebootHandle=function()end",
            "utils.backToMain=function()error('Consecutive special races must not go home')end",
            "utils.selectSpecialStage=function(screen)assert(screen.scene=='missionRewards' and screen.stageLabel=='阶段6');selected=selected+1;phase='missionRewards' end",
            "function _soloEventUi()return {scene=phase,stageLabel=phase=='missionRewards' and (completed==2 and selected==0 and '阶段6' or '阶段5') or nil,words={{text='继续',x=1100,y=650}}}end",
            "function tap()continued=continued+1;phase='car' end",
            "run.carDetails={['特殊赛事']=function()return 0 end}",
            "function _sessionLegacyCompleted()completed=completed+1;phase='missionRewards' end",
            "function keepCapture()error('Special reward navigation must precede legacy actions')end",
            "function test()assert(run.project()==0 and completed==3 and data['特赛']==3 and continued==3 and selected==1);done=true end"));
        script(assets,List.of("utilities"),String.join("\n",
            "data={};local backs=0;local last=-4000;local taps=0;function toast()end",
            "function _soloEventUi()return {scene=backs>=3 and 'home' or 'unknown'}end",
            "function keepCapture()end;function releaseCapture()end;function cmpColorEx()return 0 end",
            "function tap()taps=taps+1 end",
            "function keyPress(key)assert(key=='back' and tickCount()-last>=4000);last=tickCount();backs=backs+1 end",
            "function test()utils.backToMain();assert(backs==3 and taps==1);done=true end"));
        script(assets,List.of("workflows","daily"),String.join("\n",
            "data={['项目']='每日战利品'};run.data={['次数']='1',['杯赛位置']='5'};local phase='cupList';local chosen=false",
            "utils={tipsHandle=function()end,chooseCup=function(position,wide,configuredOnly)assert(position=='5' and not wide and configuredOnly);chosen=true;phase='event' end}",
            "run.rebootHandle=function()end;function keepCapture()error('Standard list must precede legacy colors')end",
            "function _soloEventUi()return {scene=phase,text='每日车辆战利品'}end",
            "run.carDetails={['每日战利品']=function()return 'fuelEmpty' end};dailyCup.openRaceGarage=function()phase='car';return true end",
            "function test()assert(run.project()=='fuelEmpty' and chosen);done=true end"));
        for(String project:List.of("每日战利品","杯赛"))for(String outcome:List.of("cupListObscured","cupPositionUnavailable","cupPositionInvalid","cupClaimOnly","garageEmpty","noTicket","-1")){
            script(assets,List.of("bootstrap"),String.join("\n",
                "data={};local notices,finished=0,0;function localInit()end;function monitor()end;function print()end;function _hud()end",
                "UIconfig={routeOnly='',ActiveProj={true,true},projects={{['项目']='"+project+"',data={['次数']='3',['杯赛位置']='5'}},{['项目']='多人下',data={['次数']='1'}}}}",
                "function _queueResume()return {cycle=1,index=1,completed=0}end;function _queueEnter()return 0 end;function _sessionCycle()end;function _sessionProject()end",
                "function _queueDone()finished=finished+1 end;function _taskNotice(message,ms)assert(message:find('本轮跳过项目 1') and ms==4000);"+
                    (outcome.equals("garageEmpty")?"assert(message=='本轮跳过项目 1 · "+project+"\\n没有可用车辆' and not message:find('异常'),message);":"")+"notices=notices+1 end",
                "utils={backToMain=function()end,checkAuto=function()end,initCar=function()end};dailyCup={skipReason=function()return '' end}",
                "run={project=function()return "+(outcome.equals("-1")?"-1":"'"+outcome+"'")+" end};function _requirePlayerAction()error('Configuration outcomes must not halt the queue')end",
                "currentMultiplayer={run=function()assert(notices==1 and finished==1 and UIconfig.projects[1].data['次数']=='3');done=true;sleep(999999)end}",
                "function test()main()end"));
        }
        for(String project:List.of("每日战利品","杯赛")){
            script(assets,List.of("workflows","daily"),String.join("\n",
                "data={['项目']='"+project+"',['杯赛']=0};run.data={['次数']='3'};local completed=0;utils={}",
                "run.nitro=function()end",
                "function keepCapture()end;function cmpColorEx()return 1 end;function _sessionLegacyCompleted()completed=completed+1 end",
                "run.list={['"+project+"']={{'screen',function()return -1 end}}}",
                "function test()assert(run.project()==-1 and completed==0 and data['杯赛']==0,'Failed cup must skip without consuming race quota');done=true end"));
        }
        script(assets,List.of("daily","cup"),String.join("\n",
            "data={};local taps=0;function print()end;function keepCapture()end;function cmpColorEx()return 0 end",
            "function _soloEventUi()return {scene='cars'}end;function tap(y,x)assert(y==69 and x==1100);taps=taps+1 end",
            "function test()assert(dailyCup.openRaceGarage({scene='event',text='巅峰对决',words={{text='比赛',x=1100,y=650}}}) and taps==1);done=true end"));
        for(String project:List.of("每日战利品","杯赛"))for(String scene:List.of("unknown","event")){
            script(assets,List.of("workflows","daily","cup"),String.join("\n",
                "data={['项目']='"+project+"',['杯赛']=0};run.data={['次数']='3',['杯赛位置']='5'};local phase='"+scene+"';local taps,selected,reads=0,0,0",
                "local operations="+(project.equals("每日战利品")?"dailyCup":"cup")+".operation;run.list={['"+project+"']=operations}",
                "utils={tipsHandle=function()end,dailyPrize=function()end,taskCars=function()return {1} end,chooseCarCup=function(cars)assert(phase=='cars' and cars[1]==1);selected=selected+1;phase='car' end}",
                "run.rebootHandle=function()end;run.carDetails={['"+project+"']=function()assert(phase=='car');return 'noTicket' end}",
                "function _soloEventUi()reads=reads+1;assert(reads<=8,'OCR blocked the cup color dispatch');return {scene=phase,text='巅峰对决 单人奖励 是佳用时录 比赛',words={}}end",
                "function keepCapture()end;function cmpColorEx(pattern)if phase=='"+scene+"' then return pattern==operations[3][1] and 1 or 0 end;return phase=='cars' and pattern==operations[4][1] and 1 or 0 end",
                "function tap(y,x)assert(y==24 and x==1200 and phase=='"+scene+"');taps=taps+1;phase='cars' end",
                "function _sessionLegacyCompleted()error('A garage entry must not consume race quota')end",
                "function test()assert(run.project()=='noTicket' and taps==1 and selected==1 and data['杯赛']==0);done=true end"));
        }
        for(String project:List.of("每日战利品","杯赛")){
            script(assets,List.of("workflows","daily"),String.join("\n",
                "data={['项目']='"+project+"',['杯赛']=0,['超时状态']=true};run.data={['次数']='1'};utils={}",
                "function keepCapture()end;function cmpColorEx()return 1 end;run.list={['"+project+"']={{'race',function()return -1 end}}}",
                "function _sessionLegacyCompleted()error('Timeout must not complete a race')end",
                "function test()local ok,reason=pcall(run.project);assert(not ok and tostring(reason):find('保留当前任务') and data['杯赛']==0);done=true end"));
        }
        int fuelScenarios=0;
        for(String project:List.of("多人上","多人下"))for(String scenario:List.of("mixed","allEmpty","detailEmpty","starCard","starDetail","lockedCard","lockedDetail","trialCard","rotate","detailOcr","wrongCar","lateRewards","blockedRewards","longRewards")){
            String series=project.equals("多人上")?"世界系列赛":"大师系列赛";
            script(assets,List.of("multiplayer_current"),
                "local scenario='"+scenario+"';local series='"+series+"';local count=scenario=='rotate' and 2 or 1;"+
                "data={['项目']='"+project+"',['多人']=0};run={data={['次数']=count,['用车模式']=(scenario=='rotate' or scenario=='detailOcr') and '1' or '0',['青铜用车']={1,2},['多人下用车']={1,2}}};"+
                "utils={defultCfg=function(v,d)return v or d end};local phase='series';local frame=0;local selected=0;local choices={};local notices=0;local finished=0;local rewardSince=nil;local exitAt=nil;local rewardTaps=0;local bonusAt=nil;local bonusTaps=0;"+
                "function print()end;function _hud()end;function swipe()end;function cmpColor()return 1 end;function _sessionProject()return 0 end;function _sessionRaceBegin()end;function _sessionRaceResult()end;function _fallbackBegin()end;function _fallbackStep()return 99 end;function _sessionOperation()end;"+
                "function _sessionRaceReturned()finished=finished+1;phase='series' end;"+
                "function tap(y,x)y=719-y;if x==350 and y==450 then phase='event' elseif x==1100 and y==650 then if phase=='event' then phase='cars' elseif phase=='car' then phase='race' elseif phase=='result' then phase=(scenario=='lateRewards' or scenario=='blockedRewards' or scenario=='longRewards') and 'reward' or 'returned' elseif phase=='reward' then phase='rewardLeaving';exitAt=tickCount();rewardTaps=rewardTaps+1 elseif phase=='bonus' then bonusTaps=bonusTaps+1;bonusAt=tickCount();if bonusTaps==3 then phase='returned' end end elseif x==410 and (y==305 or y==542)then selected=y==305 and 1 or 2;choices[#choices+1]=selected;phase='car' elseif x==38 and y==29 then phase='cars' end end;"+
                "function _taskNotice(msg,duration)local league=(series=='世界系列赛' and '青铜' or scenario=='rotate' and '铂金' or '无段位');if msg==league then assert(phase=='event' and duration>0);notices=notices+1 else assert(tostring(msg):find('^关闭') and duration>0,'unexpected notice: '..tostring(msg)) end end;"+
                "function _multiplayerUi()frame=frame+2000;local s={scene=phase,text=series,frameId=frame,words={},worldLeague='青铜',masterLeague=scenario=='rotate' and '白金' or nil};"+
                "if phase=='series' then s.text='世界系列赛 大师系列赛';s.words={{text=series,x=350,y=450}} " +
                "elseif phase=='event' or phase=='car' then s.words={{text='开始',x=1100,y=650}};s.carDetail={name='LANCER',performance=((scenario=='detailOcr' and #choices==1) or scenario=='wrongCar') and '1/9' or '1/2'};s.fuel=scenario=='detailEmpty' and selected==1 and 0 or 3;s.starUpPending=scenario=='starDetail' and selected==1;if phase=='car' and selected==1 and scenario=='lockedDetail' then s.scene='lockedCar' end " +
                "elseif phase=='cars' then s.garage={start=true,key='origin',rank='青铜',cards={}};for i=1,2 do s.garage.cards[i]={index=i,x=410,y=i==1 and 305 or 542,availability=(scenario=='allEmpty' or scenario=='mixed' and i==1)and 'empty' or scenario=='starCard' and i==1 and 'starUp' or scenario=='lockedCard' and i==1 and 'locked' or scenario=='trialCard' and i==1 and 'trial' or 'ready'}end " +
                "elseif phase=='race' then phase='result' elseif phase=='result' then s.text='你的排名';s.words={{text='继续',x=1100,y=650}} elseif phase=='reward' or phase=='rewardLeaving' then rewardSince=rewardSince or tickCount();s.scene='result';s.rewardPage=true;s.rewardCanContinue=phase=='reward' and ((scenario=='lateRewards' and tickCount()-rewardSince>=14000) or (scenario=='longRewards' and tickCount()-rewardSince>=28000));s.rewardReady=true;s.words={{text='继续',x=1100,y=650}};if phase=='rewardLeaving' and tickCount()-exitAt>=10000 then phase=scenario=='longRewards' and 'bonus' or 'returned';bonusAt=tickCount() end elseif phase=='bonus' then s.scene='unknown';s.text=({'名人堂奖励','新赛季奖励','你获得了卡包奖励'})[bonusTaps+1];if tickCount()-bonusAt>=10000 then s.words={{text='继续',x=1100,y=650}}end elseif phase=='returned' then s.scene='event' end;return s end;"+
                "function test()local ok,result=pcall(currentMultiplayer.run);assert(notices==count);if scenario=='blockedRewards' then assert(not ok and finished==0 and rewardTaps==0,'Unusable reward exit still times out');done=true;return end;assert(ok,result);if scenario=='longRewards' then assert(finished==1 and rewardTaps==1 and bonusTaps==3,'Wait past 20 seconds and advance each distinct reward page') elseif scenario=='lateRewards' then assert(finished==1 and rewardTaps==1,'Late-ready reward gets a separate exit transition window');elseif scenario=='trialCard' or scenario=='detailOcr' or scenario=='wrongCar' then assert(finished==1 and table.concat(choices,',')=='1','Start the unlocked fueled slot without reading detail text');elseif scenario=='allEmpty' then assert(result=='fuelEmpty' and finished==0 and #choices==0) elseif scenario=='rotate' then assert(finished==2 and table.concat(choices,',')=='1,2') elseif scenario=='detailEmpty' or scenario=='starDetail' or scenario=='lockedDetail' then assert(finished==1 and table.concat(choices,',')=='1,2') else assert(finished==1 and table.concat(choices,',')=='2')end;done=true end");
            fuelScenarios++;
        }
        return 74+fuelScenarios+wideHome(assets)+strictHome(assets);
    }
    private static int wideHome(Path assets)throws Exception{
        List<Object> tabs=List.of(Map.of("text","每日","x",593,"y",626),Map.of("text","多人","x",787,"y",626),
                Map.of("text","俱乐部","x",960,"y",659),Map.of("text","单人","x",1173,"y",626));
        if(!MainMenuUi.classify("周五 挑战限时 赛事 探索赛事",tabs).equals("home"))throw new AssertionError("Xiaomi home with split tab labels");
        if(!MainMenuUi.classify("确定退出游戏吗？ 来九游领取更多福利 再玩一会 退出游戏",tabs).equals("exitGame"))
            throw new AssertionError("Exit prompt takes priority over visible background tabs");
        String seriesMenu="幽灵滑流赛 世界系列賽 试驾挑战 大师系列赛 对决";
        if(!MultiplayerUi.classify(seriesMenu,tabs).equals("series")||
                !MultiplayerUi.classify("确定退出游戏吗？ "+seriesMenu,tabs).equals("exitGame"))
            throw new AssertionError("Series cards take priority over shared bottom tabs while Exit stays protected");
        script(assets,List.of("utilities"),"data={};local phase=0;"+
                "function _soloEventUi()if phase==0 then return {scene='exitGame',words={{text='退出游戏',x=779,y=518},{text='再玩一会',x=568,y=532}}} end;"+
                "return {scene='home',text=phase>=2 and '每周五挑战限时赛事' or '赛季赛事',words={{text='每日',x=593,y=626},{text='採索赛事',x=1024,y=510}}} end;"+
                "function tap(x,y)phase=phase+1;if phase==1 then assert(x==187 and y==568,'Only Stay is allowed')"+
                "elseif phase==2 then assert(x==93 and y==593,'Click the observed Daily tab')"+
                "elseif phase==3 then assert(x==209 and y==1024,'Click the observed Explore button')else error('Unexpected tap')end end;"+
                "function keyPress()error('No Back on a confirmed home or exit prompt')end;function toast()end;"+
                "function test()utils.backToMain();local read=_soloEventUi;_soloEventUi=function()if phase>=3 then return {scene='unknown'}end;return read()end;"+
                "utils.openHomeTab(_soloEventUi(),'daily');assert(phase==3);done=true end");
        script(assets,List.of("utilities"),"data={};local backs=0;local taps=0;"+
                "function _soloEventUi()return {scene='unknown'}end;function tap()taps=taps+1 end;"+
                "function keyPress(k)assert(k=='back');backs=backs+1 end;function keepCapture()end;function releaseCapture()end;function cmpColorEx()return 0 end;"+
                "function test()assert(not pcall(utils.backToMain));assert(backs>1 and backs<=25 and taps==1,'Unknown page may retry Back but must stop within the navigation budget');done=true end");
        script(assets,List.of("utilities"),"data={};local phase=1;local actedAt=nil;local pages={'garageFilter','cars','event','home'};"+
                "function keepCapture()end;function releaseCapture()end;function toast()end;function cmpColorEx()return 0 end;"+
                "function _soloEventUi()if actedAt and tickCount()-actedAt>=1500 then phase=phase+1;actedAt=nil end;return {scene=pages[phase]}end;"+
                "function keyPress(key)assert(phase==1 and key=='back' and not actedAt);actedAt=tickCount()end;"+
                "function tap(y,x)assert(y==690 and (phase==2 and x==38 or phase==3 and x==1244) and not actedAt);actedAt=tickCount()end;"+
                "function test()utils.backToMain();assert(phase==4);done=true end");
        script(assets,List.of("utilities"),String.join("\n",
                "data={};local tappedAt",
                "function _soloEventUi()",
                "  return {scene=tappedAt and tickCount()-tappedAt>=1500 and 'home' or 'unknown'}",
                "end",
                "function tap()assert(not tappedAt);tappedAt=tickCount()end",
                "function keyPress()error('Home transition must not trigger Back')end",
                "function keepCapture()end;function releaseCapture()end;function cmpColorEx()return 0 end",
                "function toast()end",
                "function test()",
                "  utils.backToMain()",
                "  assert(tickCount()-tappedAt<3000,'Confirmed home should return before the retry deadline')",
                "  done=true",
                "end"));
        script(assets,List.of("utilities"),"data={};local taps=0;local tappedAt;function toast()end;"+
                "function _soloEventUi()return {scene=tappedAt and tickCount()-tappedAt>=5000 and 'home' or 'rewards',rewardPage=true,rewardCanContinue=true,frameId=123,rewardExitButton={x=1044,y=638,label='错失机会'}}end;"+
                "function tap(x,y)assert(x==81 and y==1044);taps=taps+1;tappedAt=tappedAt or tickCount()end;"+
                "function keyPress()error('Do not leave the verified reward page with Back')end;"+
                "function test()utils.backToMain();assert(taps==1,'Old reward frame cannot trigger another tap');done=true end");
        script(assets,List.of("utilities"),"data={};function _soloEventUi()return {scene='rewards',rewardPage=true,rewardCanContinue=false,rewardExitButton={x=1044,y=638,label='错失机会'}}end;"+
                "function tap()error('Disabled footer must not be clicked')end;function test()assert(not pcall(utils.backToMain));done=true end");
        int[] pixels=new int[720*1280];
        for(int y=464;y<553;y++)for(int x=858;x<1191;x++){
            if(y<493||y>524||x<970||x>1069)pixels[x*720+719-y]=0xffc3fb12;
        }
        PixelFrame image=new PixelFrame(720,1280,pixels);
        var explore=MainMenuUi.dailyExplore("周末 挑战限时 赛事 探索骞事",image);
        if(Math.abs(explore.getOrDefault("x",0)-1024)>3||Math.abs(explore.getOrDefault("y",0)-509)>3)
            throw new AssertionError("Lime Daily action remains recognizable when its label is misread");
        if(!MainMenuUi.dailyExplore("赛季活动",image).isEmpty())throw new AssertionError("Do not use the color outside Daily home");
        script(assets,List.of("utilities"),"data={};local taps=0;"+
                "function _soloEventUi()return {scene='unknown'}end;"+
                "function tap(x,y)taps=taps+1;assert(x==210 and y==1025,'Open the selected Daily hero directly')end;function print()end;"+
                "function test()utils.openHomeTab({scene='home',text='挑战限时',dailyExplore={x=1025,y=509},words={{text='每日',x=592,y=628},{text='探索骞事',x=1025,y=509}}},'daily');assert(taps==1);done=true end");
        script(assets,List.of("utilities"),String.join("\n",
            "data={};local taps={};local reads=0;function print()end",
            "local home={scene='home',dailyExplore={x=1025,y=509}}",
            "function _soloEventUi()reads=reads+1;if reads==1 then return {scene='exitGame',words={{text='再玩一会',x=570,y=510}}}end",
            "return reads==2 and home or {scene='unknown'}end",
            "function tap(x,y)taps[#taps+1]=y end",
            "function test()utils.openHomeTab(home,'daily');assert(table.concat(taps,',')=='1025,570,1025');done=true end"));
        script(assets,List.of("utilities"),String.join("\n",
            "data={};local taps={};local reads=0;function print()end;function toast()end",
            "function _soloEventUi()reads=reads+1;if reads==1 then return {scene='rewards',rewardPage=true,rewardCanContinue=true,",
            "frameId=1,rewardExitButton={x=1044,y=640,label='继续'}} end",
            "return {scene=reads<6 and 'unknown' or reads==6 and 'specialRanking' or 'home'}end",
            "function keepCapture()error('Wait for the verified rankings page instead of probing the transition')end",
            "function tap(x,y)taps[#taps+1]=y end",
            "function test()utils.backToMain();assert(table.concat(taps,',')=='1044,1244');done=true end"));
        script(assets,List.of("utilities"),String.join("\n",
            "data={};local reads=0;local taps={};function print()end;utils.carFuel=function()return 6 end;utils.mycolor=function()return 1 end",
            "function _soloEventUi()reads=reads+1;if reads<=2 then return {scene='unknown',text='大师操作模式 |不再堤示',",
            "words={{text=reads==1 and '继续' or '确定',x=1088,y=650},{text='进行教学',x=720,y=650}}}end",
            "return {scene='car',fuel=6,words={{text='开始',x=1110,y=650}}}end",
            "function tap(x,y)taps[#taps+1]=y end",
            "function test()assert(utils.startRace()==1);assert(table.concat(taps,',')=='1088,1088,1110,1110',table.concat(taps,','));done=true end"));
        return 16;
    }
    private static int strictHome(Path assets) throws Exception {
        String fixture;
        try (var input=OrchestrationRecoveryChecks.class.getResourceAsStream("/strict-home.lua")) {
            fixture=new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        List<String> scenarios=List.of("sameRule","masked","exit","originalActions","popup","nestedBack",
                "unknownTimeout","stale","unconfirmedHome","rewardChoice","loginHistory");
        for (String scenario:scenarios)
            script(assets,List.of("utilities"),"scenario='"+scenario+"'\n"+fixture);
        int[] pixels=new int[720*1280];
        PixelFrame frame=new PixelFrame(720,1280,pixels);
        List<Object> tabs=List.of(Map.of("text","每日","x",586,"y",630),Map.of("text","多人","x",771,"y",630),
                Map.of("text","俱乐部","x",989,"y",656));
        if (MainMenuUi.homeEvidence(frame,"",tabs,"home",false)!=2) throw new AssertionError("Observed bottom navigation");
        pixels[690+1240*720]=0xffffffff;
        pixels[690+1252*720]=pixels[670+1270*720]=0xff12233b;
        frame=new PixelFrame(720,1280,pixels);
        if (MainMenuUi.homeEvidence(frame,"",tabs,"home",false)!=3 ||
                MainMenuUi.homeEvidence(frame,"确定退出游戏",tabs,"home",false)!=0 ||
                MainMenuUi.homeEvidence(frame,"",tabs,"home",true)!=0 ||
                MainMenuUi.homeEvidence(frame,"",tabs,"cars",false)!=0)
            throw new AssertionError("Home pixels and tab evidence must use the unobstructed frame");
        pixels[493+416*720]=0xff5678ff; pixels[493+425*720]=0xff5679ff; pixels[506+441*720]=0xff57a1ff;
        if (!MainMenuUi.returnOverlay(new PixelFrame(720,1280,pixels)).equals("back")) throw new AssertionError("Expected blue overlay signature");
        return scenarios.size()+2;
    }
    private static void script(Path assets,List<String> modules,String fixture)throws Exception {
        try(LuaRuntime lua=new LuaRuntime((n,l)->0,List.of())) {
            for(String module:modules)lua.load(module,Files.readString(assets.resolve("lua/"+module+".lua")));
            lua.load("fixture",fixture);lua.start("test");
            for(int i=0;i<180;i++)if(!lua.step(i*1000L))break;
            lua.load("verify","assert(done,'Recovery scenario did not finish')");
        }
    }
}
