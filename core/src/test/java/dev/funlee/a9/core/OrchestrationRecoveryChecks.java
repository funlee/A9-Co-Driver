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
        script(assets,List.of("workflows","daily","cup"),"data={['项目']='杯赛'};run.data={['次数']='1'};local reads,starts,confirmations=0,0,0;"+
            "utils={tipsHandle=function()error('Loading must not dispatch generic taps')end};run.nitro=function()end;run.rebootHandle=function()end;function print()end;"+
            "function _soloEventUi()reads=reads+1;if reads<=2 then return {scene='car'} elseif reads<=4 then return {scene='unknown',words={{text='仍然挑战',x=920,y=530}}} elseif reads<=6 then return {scene='unknown'} else return {scene='race'} end end;"+
            "function tap(y,x)assert(y==189 and x==920);confirmations=confirmations+1 end;function keepCapture()end;function cmpColorEx()return 1 end;"+
            "run.carDetails={['杯赛']=function()starts=starts+1;utils.soloStartAt=tickCount()end};"+
            "run.list={['杯赛']={{'race',function()assert(reads==7);return 'raceSeen'end}}};"+
            "function test()assert(run.project()=='raceSeen' and starts==1 and confirmations==1);done=true end");
        script(assets,List.of("special"),"data={};local reads=0;local taps=0;local routes=0;utils={taskRouteAuto=function()return false end,prepareTaskRoute=function()routes=routes+1 end};"+
            "function print()end;function tap()taps=taps+1 end;function _soloEventUi()reads=reads+1;return {scene=reads<5 and 'unknown' or 'race'} end;"+
            "function test()spCup.operation[1][2]();assert(reads==5 and taps==1 and routes==1,'Teaching must wait through loading');done=true end");
        script(assets,List.of("special"),String.join("\n",
            "data={};local reads=0;utils={taskRouteAuto=function()return false end};function print()end",
            "function _soloEventUi()reads=reads+1;return {scene='home',frameId=reads<3 and 1 or reads}end",
            "function test()assert(spCup.observeStart(false)==nil and reads==3);assert(spCup.observeStart(false)==nil)",
            "assert(not pcall(spCup.observeStart,false),'Stop after two automatic retries');done=true end"));
        script(assets,List.of("special"),"local start={text='开始',x=1128,y=652};local ignore={text='不再提示',x=120,y=652};"+
            "local screen={scene='unknown',text='新游戏机制 年破障碍 进行氮气爆发或360度旋转击破賽道上的物品',words={ignore,start}};"+
            "function test()assert(spCup.tutorialButton(screen)==start);screen.text='360度旋转';assert(not spCup.tutorialButton(screen));done=true end");
        script(assets,List.of("special"),"data={};utils={taskRouteAuto=function()return false end,prepareTaskRoute=function()end};function print()end;function tap()end;function _soloEventUi()return {scene='unknown'} end;"+
            "function test()local ok=pcall(spCup.operation[1][2]);assert(not ok,'Unconfirmed start cannot advance task');done=true end");
        script(assets,List.of("workflows"),"data={['项目']='特殊赛事',['特赛']=0};run.data={['次数']='1'};utils={};local completed=0;"+
            "function keepCapture()end;function cmpColorEx()return 1 end;function _sessionLegacyCompleted()completed=completed+1 end;run.list={['特殊赛事']={{'screen',function()return -1 end}}};"+
            "function test()local ok=pcall(run.project);assert(not ok and completed==0 and data['特赛']==0);done=true end");
        script(assets,List.of("daily","cup","special"),"data={['项目']='特殊赛事'};local reads=0;local taps=0;function print()end;function tap(x,y)assert(x==69 and y==1100,'Only Continue may be tapped');taps=taps+1 end;"+
            "function _soloEventUi()reads=reads+1;local scene=reads<5 and 'missionRewards' or 'event';return {scene=scene,frameId=1000+reads*1000,words={{text='继续',x=1100,y=650},{text='跳过30',x=900,y=550}}} end;"+
            "function test()assert(spCup.operation[8][2]()==0);assert(taps==1,'Stale reward frames must not repeat taps');done=true end");
        script(assets,List.of("cup","special"),"data={};local taps=0;function tap()taps=taps+1 end;function _soloEventUi()return {scene='unknown'} end;"+
            "function test()assert(cup.operation[6][2]()==nil and spCup.operation[6][2]()==nil);assert(taps==0);done=true end");
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
        script(assets,List.of("multiplayer_current"),"data={['项目']='多人上'};run={data={['次数']='1'}};utils={defultCfg=function(v,d)return v or d end};local reads=0;local taps=0;function _sessionProject()return 0 end;function _hud()end;function print()end;"+
            "function _multiplayerUi()reads=reads+1;if reads>4 then assert(taps==1,'Stale car frame repeated Home into Settings');done=true;sleep(999999)end;return {scene='car',text='',words={}} end;"+
            "function tap(x,y)assert(x==689 and y==1250);taps=taps+1 end;function test()currentMultiplayer.run()end");
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
        script(assets,List.of("special"),"data={};utils={taskRouteAuto=function()return false end,prepareTaskRoute=function()end};local reads=0;local taps=0;function print()end;function tap()taps=taps+1 end;"+
            "function _soloEventUi()reads=reads+1;return {scene=reads==2 and 'car' or reads>=8 and 'race' or 'unknown',fuel=3,frameId=reads*1000}end;"+
            "function test()spCup.operation[1][2]();assert(reads==8 and taps==1,'Transient car after tutorial must wait through loading');done=true end");
        script(assets,List.of("special"),"data={};utils={taskRouteAuto=function()return false end,prepareTaskRoute=function()end};local reads=0;function print()end;function tap()end;"+
            "function _soloEventUi()reads=reads+1;return {scene='car',fuel=0,frameId=reads*1000}end;"+
            "function test()spCup.operation[1][2]();assert(reads>=4,'Stable empty car returns to fuel handling');done=true end");
        script(assets,List.of("special"),"data={['线路']=''};local reads=0;function print()end;"+
            "utils={taskRouteAuto=function()return true end,getMapName=function(key)assert(reads==4 and key=='target');data['多人地图']=key;data['线路']='0,21' end};"+
            "function _soloEventUi(ocr)reads=reads+1;assert(ocr==(reads<=4));return {scene=reads==5 and 'race' or 'unknown',specialRoute=reads<=4 and (reads==1 and 'other' or 'target') or nil,frameId=reads==3 and 2 or reads}end;"+
            "function test()spCup.observeStart(false);assert(data['线路']=='0,21');done=true end");
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
            "zeroTicket=true;assert(utils.startRace()==1);data['项目']='杯赛';assert(not pcall(utils.startRace));zeroTicket=false;"+
            "for _,mode in ipairs({'每日战利品','车联'}) do data['项目']=mode;assert(utils.startRace()==1) end;data['项目']='杯赛';fuel=0;assert(utils.startRace()=='fuelEmpty');"+
            "star=true;fuel=6;for _,mode in ipairs({'杯赛','特殊赛事','每日战利品','车联'}) do data['项目']=mode;assert(utils.startRace()=='fuelEmpty') end;"+
            "assert(taps==5);done=true end");
        script(assets,List.of("utilities"),String.join("\n",
            "data={['项目']='特殊赛事'};local reads,taps=0,0;function print()end;utils.mycolor=function()return 1 end",
            "function _soloEventUi()reads=reads+1;local start={text='开始0',x=1100,y=650}",
            "return {scene=reads==2 and 'unknown' or 'car',text='',fuel=-1,zeroTicketStart=reads~=4,frameId=reads,words={start}} end",
            "function tap(x,y)assert(reads==5 and y==1100);taps=taps+1 end",
            "function test()assert(utils.startRace()==1 and taps==1,'A single missed trial frame must not skip the event');done=true end"));
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
        int fuelScenarios=0;
        for(String project:List.of("多人上","多人下"))for(String scenario:List.of("mixed","allEmpty","detailEmpty","starCard","starDetail","lockedCard","lockedDetail","trialCard","rotate","detailOcr","wrongCar","lateRewards","blockedRewards","longRewards")){
            String series=project.equals("多人上")?"世界系列赛":"大师系列赛";
            script(assets,List.of("multiplayer_current"),
                "local scenario='"+scenario+"';local series='"+series+"';local count=scenario=='rotate' and 2 or 1;"+
                "data={['项目']='"+project+"',['多人']=0};run={data={['次数']=count,['用车模式']=(scenario=='rotate' or scenario=='detailOcr') and '1' or '0',['青铜用车']={1,2},['多人下用车']={1,2}}};"+
                "utils={defultCfg=function(v,d)return v or d end};local phase='series';local frame=0;local selected=0;local choices={};local notices=0;local finished=0;local rewardSince=nil;local exitAt=nil;local rewardTaps=0;local bonusAt=nil;local bonusTaps=0;"+
                "function print()end;function _hud()end;function swipe()end;function cmpColor()return 1 end;function _sessionProject()return 0 end;function _sessionRaceBegin()end;function _sessionRaceResult()end;function _sessionRaceRewards()end;function _fallbackBegin()end;function _fallbackStep()return 99 end;function _sessionOperation()end;"+
                "function _sessionRaceReturned()finished=finished+1;phase='series' end;"+
                "function tap(y,x)y=719-y;if x==350 and y==450 then phase='event' elseif x==1100 and y==650 then if phase=='event' then phase='cars' elseif phase=='car' then phase='race' elseif phase=='result' then phase=(scenario=='lateRewards' or scenario=='blockedRewards' or scenario=='longRewards') and 'reward' or 'returned' elseif phase=='reward' then phase='rewardLeaving';exitAt=tickCount();rewardTaps=rewardTaps+1 elseif phase=='bonus' then bonusTaps=bonusTaps+1;bonusAt=tickCount();if bonusTaps==3 then phase='returned' end end elseif x==410 and (y==305 or y==542)then selected=y==305 and 1 or 2;choices[#choices+1]=selected;phase='car' elseif x==38 and y==29 then phase='cars' end end;"+
                "function _taskNotice(msg,duration)assert(phase=='event' and msg==(series=='世界系列赛' and '青铜' or scenario=='rotate' and '铂金' or '无段位') and duration>0);notices=notices+1 end;"+
                "function _multiplayerUi()frame=frame+2000;local s={scene=phase,text=series,frameId=frame,words={},worldLeague='青铜',masterLeague=scenario=='rotate' and '白金' or nil};"+
                "if phase=='series' then s.text='世界系列赛 大师系列赛';s.words={{text=series,x=350,y=450}} " +
                "elseif phase=='event' or phase=='car' then s.words={{text='开始',x=1100,y=650}};s.carDetail={name='LANCER',performance=((scenario=='detailOcr' and #choices==1) or scenario=='wrongCar') and '1/9' or '1/2'};s.fuel=scenario=='detailEmpty' and selected==1 and 0 or 3;s.starUpPending=scenario=='starDetail' and selected==1;if phase=='car' and selected==1 and scenario=='lockedDetail' then s.scene='lockedCar' end " +
                "elseif phase=='cars' then s.garage={start=true,key='origin',rank='青铜',cards={}};for i=1,2 do s.garage.cards[i]={index=i,x=410,y=i==1 and 305 or 542,availability=(scenario=='allEmpty' or scenario=='mixed' and i==1)and 'empty' or scenario=='starCard' and i==1 and 'starUp' or scenario=='lockedCard' and i==1 and 'locked' or scenario=='trialCard' and i==1 and 'trial' or 'ready'}end " +
                "elseif phase=='race' then phase='result' elseif phase=='result' then s.text='你的排名';s.words={{text='继续',x=1100,y=650}} elseif phase=='reward' or phase=='rewardLeaving' then rewardSince=rewardSince or tickCount();s.scene='result';s.rewardPage=true;s.rewardCanContinue=phase=='reward' and ((scenario=='lateRewards' and tickCount()-rewardSince>=14000) or (scenario=='longRewards' and tickCount()-rewardSince>=28000));s.rewardReady=true;s.words={{text='继续',x=1100,y=650}};if phase=='rewardLeaving' and tickCount()-exitAt>=10000 then phase=scenario=='longRewards' and 'bonus' or 'returned';bonusAt=tickCount() end elseif phase=='bonus' then s.scene='unknown';s.text=({'名人堂奖励','新赛季奖励','你获得了卡包奖励'})[bonusTaps+1];if tickCount()-bonusAt>=10000 then s.words={{text='继续',x=1100,y=650}}end elseif phase=='returned' then s.scene='event' end;return s end;"+
                "function test()local ok,result=pcall(currentMultiplayer.run);assert(notices==count);if scenario=='blockedRewards' then assert(not ok and finished==0 and rewardTaps==0,'Unusable reward exit still times out');done=true;return end;assert(ok,result);if scenario=='longRewards' then assert(finished==1 and rewardTaps==1 and bonusTaps==3,'Wait past 20 seconds and advance each distinct reward page') elseif scenario=='lateRewards' then assert(finished==1 and rewardTaps==1,'Late-ready reward gets a separate exit transition window');elseif scenario=='trialCard' or scenario=='detailOcr' or scenario=='wrongCar' then assert(finished==1 and table.concat(choices,',')=='1','Start the unlocked fueled slot without reading detail text');elseif scenario=='allEmpty' then assert(result=='fuelEmpty' and finished==0 and #choices==0) elseif scenario=='rotate' then assert(finished==2 and table.concat(choices,',')=='1,2') elseif scenario=='detailEmpty' or scenario=='starDetail' or scenario=='lockedDetail' then assert(finished==1 and table.concat(choices,',')=='1,2') else assert(finished==1 and table.concat(choices,',')=='2')end;done=true end");
            fuelScenarios++;
        }
        return 53+fuelScenarios+wideHome(assets)+strictHome(assets);
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
                "function test()assert(not pcall(utils.backToMain));assert(backs==1 and taps==1,'Unknown page must stop instead of looping Back');done=true end");
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
            "function test()assert(utils.startRace()==1);assert(table.concat(taps,',')=='1088,1088,1110',table.concat(taps,','));done=true end"));
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
        if (!MainMenuUi.returnOverlay(new PixelFrame(720,1280,pixels)).equals("back")) throw new AssertionError("Original blue overlay signature");
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
