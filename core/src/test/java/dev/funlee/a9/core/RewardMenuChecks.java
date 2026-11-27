package dev.funlee.a9.core;

import java.nio.file.*;
import java.util.*;

final class RewardMenuChecks {
    static int run(Path assets)throws Exception {
        int checks=0;
        int[] header=new int[720*1280];
        Arrays.fill(header,0xff102c4e);
        // A yellow badge and the neighboring tile must not count as a highlighted clipboard.
        for(int x=384;x<414;x++)Arrays.fill(header,x*720+660,x*720+710,0xffffd10a);
        for(int x=453;x<463;x++)Arrays.fill(header,x*720+703,x*720+715,0xffffd10a);
        if(MainMenuUi.dailyGoalsHighlighted(new PixelFrame(720,1280,header)))
            throw new AssertionError("Blue daily-goals tile matched a nearby badge");
        for(int x=424;x<466;x++)Arrays.fill(header,x*720+660,x*720+710,0xffffd10a);
        if(!MainMenuUi.dailyGoalsHighlighted(new PixelFrame(720,1280,header)))
            throw new AssertionError("Yellow daily-goals tile was not detected");
        String source=Files.readString(assets.resolve("lua/rewards.lua"));
        String common="data={};logs={};huds={};function _hud(s)huds[s]=true;lastHud=s end;function _racePanel()end;function print(s)logs[#logs+1]=s end;"+
            "function w(text,x,y)return {text=text,x=x,y=y}end;"+
            "home={text='每日赛事 多人游戏 我的俱乐部',words={w('兔费贵包！',1105,30),w('每日',570,650),w('游戏',800,680),w('俱乐部',975,680)}};"+
            "function logged(s)for _,v in ipairs(logs)do if v:find(s,1,true)then return true end end return false end;";
        run(source,common+"UIconfig={};function _rewardUi()error('disabled rewards accessed game')end;function test()currentRewards.collect();done=true end");checks++;
        run(source,common+"UIconfig={freePkg='0',autoToken='0'};home.words[1]=w('全新促销！',1105,30);"+
            "home.dailyGoalsHighlighted=false;home.words[#home.words+1]=w('免费包',800,400);"+
            "function _rewardUi()return home end;function tap()error('No home reward indicator')end;"+
            "function swipe()error('Unexpected swipe')end;function keyPress()error('Unexpected back')end;"+
            "function test()currentRewards.collect();assert(logged('多帧复核后未确认免费包入口') and logged('图标未标黄'));done=true end");checks++;
        for(String scenario:List.of("delayed","stale","missing","body","offpage","wrongX","wrongY")) {
            boolean opens=scenario.equals("delayed")||scenario.equals("stale");
            run(source,common+String.join("\n",
                "UIconfig={freePkg='0'};state='home';reads=0;entries=0;shopReads=0",
                "local scenario='"+scenario+"'",
                "local shop={text='卡牌包 卡牌包详情 经典包 即将可用',words={w('75',760,594)}}",
                "function _rewardUi()",
                "  if state=='shop' then shopReads=shopReads+1;return shop end",
                "  reads=reads+1",
                "  local label='商店'",
                "  if entries==0 and ((scenario=='delayed' or scenario=='stale') and reads>=5 or scenario=='stale' and reads==3) then label='兔费包!' end",
                "  local page={text=home.text,frameId=reads,words={w(label,1105,30),w('每日',570,650),w('游戏',800,680),w('俱乐部',975,680)}}",
                "  if scenario=='stale' and reads==3 then page.frameId=2 end",
                "  if scenario=='body' then page.words[#page.words+1]=w('免费包',800,400) end",
                "  if scenario=='wrongX' then page.words[1]=w('免费包',949,30) end",
                "  if scenario=='wrongY' then page.words[1]=w('免费包',1105,81) end",
                "  if scenario=='offpage' and reads>=3 then page.scene='series';page.words={w('免费包',1105,30)} end",
                "  return page",
                "end",
                "function tap(y,x)",
                "  if state=='home' then assert(reads>=6 and x==1105 and y==689,'Stale or unverified entry clicked');entries=entries+1;state='shop'",
                "  else assert(state=='shop' and x==1250 and y==689,'Price, ad or sidebar clicked');state='home' end",
                "end",
                "function swipe()error('Auto-positioned shop must never swipe')end",
                "function keyPress()error('Unexpected back')end",
                "function test()",
                "  currentRewards.collect()",
                "  assert(entries=="+(opens?1:0)+" and state=='home',table.concat(logs,'|'))",
                "  assert(logged('免费包入口复核'))",
                opens?"  assert(shopReads>=5 and logged('检查商店免费包与免费福利') and logged('当前定位页面未确认免费领取按钮，不切换商店分类'))":
                    "  assert(logged('多帧复核后未确认免费包入口') and reads=="+(scenario.equals("offpage")?3:6)+")",
                "  done=true",
                "end"));checks++;
        }
        // Every shop fixture forbids category taps and swipes, including failed claims.
        for(String scenario:List.of("card","cardDelayed","cardDetail","cardPaidDetail","welfare","delayed",
                "titleAbove","decoratedTitle","unreadableTitle","detail","paidDetail","traditionalDetail",
                "traditionalPaidDetail","dropRateDetail","detailDelayed","detailChangedPrice","changedPrice",
                "noWelfare","remoteWelfare","noTitle","multiplayer","adOnly","headerOnly","bodyOnly",
                "noFree","leftFree","rightFree","topFree","bottomFree","minBounds","maxBounds",
                "homeLanding","partialReward","shopDelayed","obtainFollowing","noContinue")) {
            boolean claims=!List.of("cardPaidDetail","paidDetail","traditionalPaidDetail","detailChangedPrice",
                "changedPrice","adOnly","headerOnly","bodyOnly","noFree","leftFree","rightFree",
                "topFree","bottomFree","homeLanding","partialReward").contains(scenario);
            boolean opens=claims||List.of("cardPaidDetail","paidDetail","traditionalPaidDetail",
                "detailChangedPrice","partialReward").contains(scenario);
            run(source,common+autoPositionedFixture(scenario,opens,claims,1));checks++;
        }
        for(String scenario:List.of("cardThenWelfare","welfareThenCard","fiveEntries")) {
            run(source,common+autoPositionedFixture(scenario,true,true,scenario.equals("fiveEntries")?5:2));checks++;
        }
        for(boolean stale:List.of(false,true)){
            String fixture=common+"UIconfig={autoToken='0'};home.dailyGoalsHighlighted=true;state='home';claims=0;reads=0;"+
                "goals={text='每日目标 参加任意比赛 3/3 领取 前往 观看广告',words={w('每日目标',310,110),w('参加任意比赛',520,440),w('领取',1150,460),w('前往',1020,580),w('观看广告',1150,580)}};"+
                "empty={text='每日目标 升级车辆 0/1 前往 观看广告',words={w('每日目标',310,110),w('前往',1020,580),w('观看广告',1150,580)}};"+
                "function _rewardUi()reads=reads+1;if state=='home' then return home elseif state=='empty' then return empty else if '"+stale+"'=='true' then goals.text='每日目标 参加任意比赛 3/3 领取 前往 观看广告 clock'..reads end;return goals end end;"+
                "function tap(y,x)if state=='home' then assert(x==440);state='goals' elseif x==310 then assert(state=='goals') elseif x==1150 and y==259 then claims=claims+1;if '"+stale+"'=='false' then state='empty'end else error('Unexpected ad/go action')end end;"+
                "function keyPress(k)state='home'end;function swipe()assert(state=='empty')end;"+
                "function test()currentRewards.collect();assert(huds['正在领取每日奖励'] and lastHud=='领奖检查结束，继续任务');assert(claims==1);assert(logged('"+(stale?"领取结果未确认":"已确认领取每日目标奖励 1 项")+"'));assert(state=='home');done=true end";
            run(source,fixture);checks++;
        }
        String bootstrap=Files.readString(assets.resolve("lua/bootstrap.lua"));
        String queueFixture=String.join("\n",
            "data={}",
            "function localInit() end",
            "function monitor() end",
            "function print() end",
            "function _hud() end",
            "function _sessionProject() end",
            "utils={backToMain=function() end, checkAuto=function() end, initCar=function() end}",
            "UIconfig={routeOnly='',ActiveProj={true,false,true},projects={",
            "    {['项目']='世界系列赛',data={['次数']='3'}},",
            "    {['项目']='杯赛',data={['次数']='5'}},",
            "    {['项目']='特殊赛事',data={['次数']='2'}}",
            "}}",
            "local events={}",
            "local cycle",
            "local function record(event) events[#events+1]=event end",
            "currentRewards={enabled=function() return true end,collect=function() record('领奖'..cycle) end}",
            "function _queueRewardsDone(round) assert(round==cycle);record('保存'..round) end",
            "currentMultiplayer={run=function() record('多人'..cycle) end}",
            "run={project=function() record('特赛'..cycle) end}",
            "function _sessionCycle(round)",
            "    cycle=round",
            "    if round==3 then",
            "        assert(table.concat(events,',')==expected, table.concat(events,','))",
            "        done=true",
            "        error('QUEUE_VERIFIED')",
            "    end",
            "end",
            "function test()",
            "    local ok,err=pcall(main)",
            "    assert(not ok and done and err:find('QUEUE_VERIFIED',1,true),err)",
            "end",
            "");
        run(bootstrap,queueFixture+"expected='领奖1,保存1,多人1,特赛1,领奖2,保存2,多人2,特赛2'");checks++;
        run(bootstrap,queueFixture+"expected='特赛1,领奖2,保存2,多人2,特赛2';"+
            "function _queueResume() return {cycle=1,index=3,completed=1,rewardCycle=1} end");checks++;
        return checks;
    }
    private static String autoPositionedFixture(String scenario,boolean opens,boolean claims,int expectedEntries){
        return String.join("\n",
            "UIconfig={freePkg='0'};state='home';entries=0;claims=0;opens=0;reads=0;detailReads=0;rewardReads=0;homeClicks=0;continues=0;detailClicks=0",
            "local scenario='"+scenario+"'",
            "local expectedEntries="+expectedEntries,
            "local isDelayed=scenario=='delayed' or scenario=='cardDelayed'",
            "local isDetail=scenario=='detail' or scenario=='paidDetail' or scenario=='traditionalDetail' or scenario=='traditionalPaidDetail' or scenario=='cardDetail' or scenario=='cardPaidDetail' or scenario=='dropRateDetail' or scenario=='detailDelayed' or scenario=='detailChangedPrice'",
            "local isPaid=scenario=='paidDetail' or scenario=='traditionalPaidDetail' or scenario=='cardPaidDetail'",
            "local noControl=scenario=='adOnly' or scenario=='headerOnly' or scenario=='bodyOnly' or scenario=='noFree' or scenario=='leftFree' or scenario=='rightFree' or scenario=='topFree' or scenario=='bottomFree' or scenario=='homeLanding'",
            "local function cardPage()",
            "  return scenario:sub(1,4)=='card' and (scenario~='cardThenWelfare' or entries==1) or scenario=='welfareThenCard' and entries==2",
            "end",
            "local function freePosition()",
            "  if cardPage() then return 760,594 end",
            "  if scenario=='multiplayer' then return 639,567 end",
            "  if scenario=='leftFree' then return 299,355 end",
            "  if scenario=='rightFree' then return 1221,355 end",
            "  if scenario=='topFree' then return 637,169 end",
            "  if scenario=='bottomFree' then return 637,681 end",
            "  if scenario=='minBounds' then return 300,170 end",
            "  if scenario=='maxBounds' then return 1220,680 end",
            "  if scenario=='headerOnly' then return 1107,31 end",
            "  return 637,355",
            "end",
            "local function shop()",
            "  reads=reads+1",
            "  local words={w('高级通行证',138,85),w('首充优惠',127,451),w('限时商品包',138,524),w('卡牌包',117,597),w('免费包!',1107,31)}",
            "  local text=cardPage() and '卡牌包 卡牌包详情 经典包' or '高级通行证 首充优惠 限时商品包'",
            "  if scenario=='homeLanding' and reads<=5 then text=home.text;for i=2,#home.words do words[#words+1]=home.words[i] end end",
            "  if not isDelayed or reads>=5 then",
            "    if scenario~='noTitle' then",
            "      local title=scenario=='decoratedTitle' and '多人游戏包IMOMIm' or scenario=='unreadableTitle' and 'InOMIm' or '赏金礼包'",
            "      local tx,ty=546,440",
            "      if scenario=='titleAbove' then ty=230 end",
            "      if scenario=='multiplayer' then title='多人游戏包NIUIm';tx=624;ty=230 end",
            "      words[#words+1]=w(title,tx,ty)",
            "    end",
            "    if scenario~='noWelfare' and scenario~='noTitle' then words[#words+1]=w('·:免费福利··',scenario=='remoteWelfare' and 1050 or scenario=='multiplayer' and 461 or 460,scenario=='multiplayer' and 448 or 382) end",
            "    local label=scenario=='adOnly' and '观看广告' or scenario=='bodyOnly' and '免费福利' or scenario=='noFree' and '88' or scenario=='changedPrice' and reads>=2 and '88' or claims>=entries and '88' or '免费'",
            "    local x,y=freePosition();words[#words+1]=w(label,x,y)",
            "    words[#words+1]=w('观看广告',636,565)",
            "    words[#words+1]=w('赏金徽章专属包',944,244);words[#words+1]=w('精选商品',1036,527);words[#words+1]=w('88',1035,566)",
            "  end",
            "  return {text=text,words=words}",
            "end",
            "function _rewardUi()",
            "  if state=='home' then home.words[1]=w((scenario=='fiveEntries' or claims<expectedEntries) and '免费贵包!' or '商店',1105,30);return home end",
            "  if state=='detail' then",
            "    detailReads=detailReads+1",
            "    local text=scenario:find('traditional',1,true) and '称霸礼包 |此包內可能包含的物品 掉落几奉' or scenario=='dropRateDetail' and '赏金礼包 掉落几率' or '赏金礼包 此包内可能包含的物品'",
            "    local label=isPaid and '88' or scenario=='detailDelayed' and detailReads<5 and '加载中' or scenario=='detailChangedPrice' and detailReads>=3 and '88' or '免费'",
            "    return {text=text,words={w(label,620,648)}}",
            "  end",
            "  if state=='reward' then",
            "    rewardReads=rewardReads+1",
            "    if scenario=='noContinue' and rewardReads>=3 then state='shop';return shop() end",
            "    return {text=scenario=='obtainFollowing' and '恭喜！获得以下奖励' or '恭喜！你获得了以下奖励',words=scenario=='noContinue' and {} or {w('|继续',1150,665)}}",
            "  end",
            "  if state=='unconfirmed' then return {text='高级通行证 首充优惠 你获得了以下奖励',words={w('高级通行证',138,85),w('首充优惠',127,451)}} end",
            "  assert(state=='shop')",
            "  if scenario=='shopDelayed' and reads<3 then reads=reads+1;return {text='加载中',words={w('免费',637,355)}} end",
            "  return shop()",
            "end",
            "function tap(y,x)",
            "  if state=='home' then assert(x==1105 and y==689 and entries<expectedEntries,'Unverified header or sixth entry clicked');entries=entries+1;reads=0;state='shop'",
            "  elseif x==1250 then assert(y==689 and (state=='shop' or state=='unconfirmed'),'Unexpected home input');homeClicks=homeClicks+1;state='home'",
            "  elseif state=='shop' then",
            "    local fx,fy=freePosition();assert("+opens+" and not noControl and x==fx and y==719-fy,'Clicked sidebar, ad, price, header or out-of-bounds Free')",
            "    if isDelayed then assert(reads>=6,'Clicked before auto-positioned control stabilized') end",
            "    if scenario=='shopDelayed' then assert(reads>=5,'Clicked Free on an unconfirmed loading page') end",
            "    assert(opens==entries-1,'More than one pack opened in one entry');opens=opens+1",
            "    if isDetail then state='detail' elseif scenario=='partialReward' then state='unconfirmed' else claims=claims+1;state='reward';rewardReads=0 end",
            "  elseif state=='detail' then",
            "    assert(not isPaid and scenario~='detailChangedPrice' and x==620 and y==71,'Clicked paid or changed detail price')",
            "    if scenario=='detailDelayed' then assert(detailReads>=6,'Detail Free was not stable') end",
            "    detailClicks=detailClicks+1;claims=claims+1;state='reward';rewardReads=0",
            "  elseif state=='reward' then assert(scenario~='noContinue' and x==1150 and y==54,'Unexpected reward input');continues=continues+1;state='shop'",
            "  else error('Unexpected click') end",
            "end",
            "function swipe()error('Auto-positioned reward flow must not swipe')end",
            "function keyPress(k)assert(k=='back' and state=='detail' and (isPaid or scenario=='detailChangedPrice'),'Unexpected back');state='shop'end",
            "function test()",
            "  currentRewards.collect()",
            "  assert(state=='home' and entries==expectedEntries and opens=="+(opens?expectedEntries:0)+" and claims=="+(claims?expectedEntries:0)+",scenario..':'..table.concat(logs,'|'))",
            "  assert(homeClicks==expectedEntries,'Did not return home after each auto-positioned entry')",
            "  assert(logged('检查商店免费包与免费福利') and lastHud=='领奖检查结束，继续任务')",
            "  if noControl then assert(logged('当前定位页面未确认免费领取按钮，不切换商店分类') and reads>=5) end",
            "  if isPaid or scenario=='partialReward' or scenario=='detailChangedPrice' then assert(logged('领取结果未确认')) end",
            claims?"  assert(logged('已确认领取'));assert(continues==(scenario=='noContinue' and 0 or expectedEntries));assert(detailClicks==(isDetail and expectedEntries or 0))":"  assert(not logged('已确认领取'))",
            "  if scenario=='card' or scenario=='cardDetail' then assert(logged('已确认领取免费卡牌包')) end",
            "  if scenario=='decoratedTitle' then assert(logged('已确认领取多人游戏包')) end",
            "  if scenario=='unreadableTitle' or scenario=='noTitle' or scenario=='noWelfare' or scenario=='remoteWelfare' then assert(logged('已确认领取商店免费福利包')) end",
            "  if scenario=='welfare' or scenario=='titleAbove' then assert(logged('已确认领取赏金礼包')) end",
            "  if scenario=='cardThenWelfare' or scenario=='welfareThenCard' then assert(logged('已确认领取免费卡牌包') and logged('已确认领取赏金礼包')) end",
            "  if scenario=='fiveEntries' then assert(logged('商店仍提示有免费包，尚未确认全部领取')) end",
            "  done=true",
            "end");
    }
    private static void run(String source,String fixture){
        try(LuaRuntime lua=new LuaRuntime((n,l)->0,List.of())){
            lua.load("rewards",source);lua.load("fixture",fixture);lua.start("test");
            boolean alive=true;for(int i=0;alive&&i<4000;i++)alive=lua.step(i*250L);
            if(alive)throw new AssertionError("Reward workflow did not finish");
            lua.load("verify","assert(done)");
        }
    }
}
