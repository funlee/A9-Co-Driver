package dev.funlee.a9.core;

import java.nio.file.*;
import java.util.*;

final class RewardMenuChecks {
    static int run(Path assets)throws Exception {
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
        run(source,common+"UIconfig={};function _rewardUi()error('disabled rewards accessed game')end;function test()currentRewards.collect();done=true end");
        run(source,common+"UIconfig={freePkg='0',autoToken='0'};home.words[1]=w('全新促销！',1105,30);"+
            "home.dailyGoalsHighlighted=false;home.words[#home.words+1]=w('免费包',800,400);"+
            "function _rewardUi()return home end;function tap()error('No home reward indicator')end;"+
            "function test()currentRewards.collect();assert(logged('主界面未显示免费包') and logged('图标未标黄'));done=true end");
        for(boolean paid:List.of(false,true)){
            String fixture=common+"UIconfig={freePkg='0'};state='home';opens=0;claims=0;"+
                "shop={text='卡牌包 卡牌包详情 经典包',words={w('免费',760,594),w('开始',470,594)}};"+
                "detail={text='经典包 此包内可能包含的物品 掉落几率',words={w('"+(paid?"75":"免费")+"',620,648)}};"+
                "empty={text='卡牌包 卡牌包详情 即将可用',words={w('75',760,594),w('开始',470,594)}};"+
                "reward={text='恭喜！ 你获得了该卡牌',words={w('|继续',1150,664)}};"+
                "function _rewardUi()if state=='home' and claims>0 then home.words[1]=w('商店',1105,30) end;return ({home=home,shop=shop,detail=detail,reward=reward,empty=empty})[state]end;"+
                "function tap(y,x)if x==1250 then state='home' elseif state=='home' then assert(x==1105);state='shop' elseif state=='shop' then assert(x==760);state='detail';opens=opens+1 elseif state=='detail' then assert('"+paid+"'=='false','Priced purchase clicked');assert(x==620);claims=claims+1;state='reward' elseif state=='reward' then assert(x==1150);state='empty' else error('Unexpected tap')end end;"+
                "function keyPress(k)state='home'end;function swipe()assert(state=='empty')end;"+
                "function test()currentRewards.collect();assert(huds['正在领取免费包'] and lastHud=='领奖检查结束，继续任务');assert(opens==1 and claims=="+(paid?0:1)+",'opens='..opens..' claims='..claims..table.concat(logs,'|'));assert(state=='home','state='..state);assert(logged('"+(paid?"不是免费领取":"已确认领取免费包 1 个")+"'),table.concat(logs,'|'));done=true end";
            run(source,fixture);
        }
        for(String location:List.of("visible","below","above","missing","wrongPage")) {
            boolean free=location.equals("visible");
            String fixture=common+"UIconfig={freePkg='0'};state='home';claims=0;sidebarSwipes=0;tabClicks=0;"+
                "classic={text='卡牌包 卡牌包详情 经典包 即将可用',words={w('"+(free?"免费":"65")+"',760,594),w('开始',470,594)}};"+
                "empty={text='卡牌包 卡牌包详情 经典包 即将可用',words={w('65',760,594),w('开始',470,594)}};"+
                "reward={text='恭喜！你获得了该卡牌',words={w('继续',1150,664)}};"+
                "function _rewardUi()if state=='shop' then local words={w('充值活动',130,100),w('限时商品包',130,400),w('免费',636,355)};"+
                "local visible=('"+location+"'=='visible' or '"+location+"'=='wrongPage' or ('"+location+"'=='below' and sidebarSwipes>=1) or ('"+location+"'=='above' and sidebarSwipes>=5));"+
                "if visible then words[#words+1]=w('卡牌包',116,600) end;return {text='充值活动 限时商品包 活动免费福利',words=words} end;"+
                "if state=='home' and claims>0 then home.words[1]=w('商店',1105,30) end;return ({home=home,classic=classic,empty=empty,reward=reward})[state]end;"+
                "function tap(y,x)if x==1250 then state='home' elseif state=='home' then assert(x==1105);state='shop' " +
                "elseif state=='shop' then assert(x==116 and y==119,'Must select Packs before inspecting featured giveaways');tabClicks=tabClicks+1;if '"+location+"'~='wrongPage' then state='classic' end " +
                "elseif state=='classic' then assert('"+free+"'=='true' and x==760,'Timed pack or ad clicked');claims=claims+1;state='reward' " +
                "elseif state=='reward' then assert(x==1150);state='empty' else error('Unexpected input') end end;"+
                "function swipe(x1,y1,x2,y2)if state=='shop' then assert(y1==150 and y2==150,'Scrolled featured merchandise instead of sidebar');sidebarSwipes=sidebarSwipes+1;"+
                "assert((sidebarSwipes<=4 and x1==119 and x2==479) or (sidebarSwipes>4 and x1==479 and x2==119)) " +
                "else assert(state=='empty' or state=='classic');assert(tabClicks==1) end end;"+
                "function keyPress()state='home'end;function test()currentRewards.collect();assert(state=='home');"+
                "assert(claims=="+(free?1:0)+");assert(sidebarSwipes=="+(location.equals("below")?1:location.equals("above")?5:location.equals("missing")?8:0)+");"+
                "assert(logged('"+(location.equals("missing")||location.equals("wrongPage")?"未确认卡牌包选项":free?"已确认领取免费包 1 个":"当前没有可领取的免费卡牌包")+"'),table.concat(logs,'|'));done=true end";
            run(source,fixture);
        }
        run(source,common+"UIconfig={freePkg='0'};state='home';rewinds=0;claims=0;"+
            "tail={text='卡牌包 卡牌包详情 高级图纸包',words={w('100',760,594)}};"+
            "classic={text='卡牌包 卡牌包详情 经 其他标签 典 包 广 告',words={w('免费',760,594)}};"+
            "reward={text='恭喜！你获得了该卡牌',words={w('继续',1150,664)}};"+
            "empty={text='卡牌包 卡牌包详情 经典包 即将可用',words={w('65',760,594)}};"+
            "function _rewardUi()if state=='home' and claims>0 then home.words[1]=w('商店',1105,30) end;return ({home=home,tail=tail,classic=classic,reward=reward,empty=empty})[state]end;"+
            "function tap(y,x)if x==1250 then state='home' elseif state=='home' then state='tail' elseif state=='classic' then assert(x==760);claims=claims+1;state='reward' elseif state=='reward' then state='empty' else error('Wrong card or price clicked')end end;"+
            "function swipe(x1,y1,x2,y2)if state=='tail' then assert(x1==299 and y1==450 and x2==299 and y2==1130);rewinds=rewinds+1;if rewinds==2 then state='classic' end else assert(state=='empty')end end;"+
            "function keyPress()state='home'end;function test()currentRewards.collect();assert(rewinds==2 and claims==1 and state=='home');done=true end");
        for(boolean stale:List.of(false,true)){
            String fixture=common+"UIconfig={autoToken='0'};home.dailyGoalsHighlighted=true;state='home';claims=0;reads=0;"+
                "goals={text='每日目标 参加任意比赛 3/3 领取 前往 观看广告',words={w('每日目标',310,110),w('参加任意比赛',520,440),w('领取',1150,460),w('前往',1020,580),w('观看广告',1150,580)}};"+
                "empty={text='每日目标 升级车辆 0/1 前往 观看广告',words={w('每日目标',310,110),w('前往',1020,580),w('观看广告',1150,580)}};"+
                "function _rewardUi()reads=reads+1;if state=='home' then return home elseif state=='empty' then return empty else if '"+stale+"'=='true' then goals.text='每日目标 参加任意比赛 3/3 领取 前往 观看广告 clock'..reads end;return goals end end;"+
                "function tap(y,x)if state=='home' then assert(x==440);state='goals' elseif x==310 then assert(state=='goals') elseif x==1150 and y==259 then claims=claims+1;if '"+stale+"'=='false' then state='empty'end else error('Unexpected ad/go action')end end;"+
                "function keyPress(k)state='home'end;function swipe()assert(state=='empty')end;"+
                "function test()currentRewards.collect();assert(huds['正在领取每日奖励'] and lastHud=='领奖检查结束，继续任务');assert(claims==1);assert(logged('"+(stale?"领取结果未确认":"已确认领取每日目标奖励 1 项")+"'));assert(state=='home');done=true end";
            run(source,fixture);
        }
        run(source,common+String.join("\n",
            "UIconfig={freePkg='0'};state='home';claims=0;entries=0",
            "local reward={text='恭喜！你获得了以下奖励',words={w('继续',1150,665)}}",
            "function _rewardUi()",
            "  if state=='home' then",
            "    home.words[1]=w(claims<2 and '免费包' or '商店',1105,30)",
            "    return home",
            "  end",
            "  if state=='reward' then return reward end",
            "  local title=entries==1 and '特殊赛事包' or '每周补给包'",
            "  return {text='卡牌包 特殊赛事 '..title,words={w('卡牌包',124,510),",
            "    w('特殊赛事',124,655),w(title,550,200),w('免费',634,352),",
            "    w(title,550,475),w('观看广告',634,610),w('免费',950,352),w('75',1100,405)}}",
            "end",
            "function tap(y,x)",
            "  if x==1250 then state='home'",
            "  elseif state=='home' then assert(x==1105);entries=entries+1;state='featured'",
            "  elseif state=='featured' then assert(x==634 and y==367);claims=claims+1;state='reward'",
            "  elseif state=='reward' then assert(x==1150);state='featured'",
            "  else error('Unexpected click') end",
            "end",
            "function test()",
            "  currentRewards.collect()",
            "  assert(state=='home' and claims==2 and entries==2)",
            "  assert(logged('已确认领取特殊赛事免费包'))",
            "  done=true",
            "end"));
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
            "    {['项目']='多人上',data={['次数']='3'}},",
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
        run(bootstrap,queueFixture+"expected='领奖1,保存1,多人1,特赛1,领奖2,保存2,多人2,特赛2'");
        run(bootstrap,queueFixture+"expected='特赛1,领奖2,保存2,多人2,特赛2';"+
            "function _queueResume() return {cycle=1,index=3,completed=1,rewardCycle=1} end");
        return 17;
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
