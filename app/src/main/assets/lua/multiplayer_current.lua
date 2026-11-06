-- Current CN series cards and vehicle layout. Coordinates below are upright 1280x720.
-- OCR guards each menu action; race commands still use the existing route engine.
currentMultiplayer={}
-- Explicit user-selected left-swipe policy, keyed by the same internal IDs as map OCR.
local leftSwipeMaps={
  ['加勒比海环岛赛车']=true, -- 热带天堂·环岛赛车: missing route
  ['芝加哥极速眩晕']=true, -- 凌空之巅·极速眩晕: invalid progress
  ['新加坡浪花骑士']=true, -- 狮城·音速狂飙: invalid command
  ['纽约量子跃迁']=true -- 摩登城市·量子跃迁: backwards progress
}
local function click(x,y) tap(719-math.floor(y),math.floor(x)) end
local function has(text,part)
  local normalized=string.gsub(string.gsub(text,'[ \t\r\n]',''),'賽','赛')
  return string.find(normalized,part,1,true)~=nil
end
local function word(screen,label,minY)
  for _,w in ipairs(screen.words or {}) do
    if has(w.text,label) and w.y>=(minY or 0) then return w end
  end
end
local function press(screen,label,minY)
  local w=word(screen,label,minY)
  if not w then return false end
  click(w.x,w.y);sleep(1200);return true
end
local function status(message)
  data['信息']=message;_hud(message);print(message)
end
function currentMultiplayer.run()
  local count=tonumber(run.data['次数']) or 1
  _sessionProject(data['项目'],count)
  for _,key in ipairs({'使用车组','广告修车','传奇停跑','多人上掉分','选车筛选'}) do
    if utils.defultCfg(run.data[key],'')~='' then error('新版多人菜单尚未适配“'..key..'”，请关闭此选项后运行') end
  end
  if tostring(utils.defultCfg(run.data['用车模式'],'0'))~='0' then error('新版多人菜单暂支持“每次用一辆”用车模式') end
  local mode=utils.defultCfg(run.data['多人选路'],'3')
  local series=data['项目']=='多人上' and '世界系列赛' or '大师系列赛'
  local other=data['项目']=='多人上' and '大师系列赛' or '世界系列赛'
  status('目标赛事：'..series..'（'..data['项目']..'）')
  local total=0;local unlockMisses=0
  while total<count do
    local route=nil;local candidate=nil;local candidateFrames=0;local lastMapText=''
    local raceMode=mode;local fallback=false;local fallbackReason=''
    local raced=false;local resultSeen=false;local eventSince=nil
    local lastScene='';local changed=tickCount();local started=false;local seriesSelected=false
    local lastPulse=0;local carRetries=0
    while true do
      local screen=_multiplayerUi()
      local scene=screen.scene
      if scene~='event' then eventSince=nil end
      if raced and scene=='result' then
        _sessionRaceResult(screen.finalRank or 0)
        if not resultSeen then resultSeen=true;status('已识别比赛结算：'..screen.text) end
      end
      if started and not raced and scene=='unknown' then
        if screen.mapText and screen.mapText~=lastMapText then
          lastMapText=screen.mapText;print('地图标题 OCR：'..lastMapText)
          snapShot(getWorkPath()..'/multiplayer-map.png')
        end
        if screen.route and screen.route==candidate then candidateFrames=candidateFrames+1
        else candidate=screen.route;candidateFrames=screen.route and 1 or 0 end
        if candidateFrames>=2 and route~=candidate then route=candidate;print('地图标题连续确认：'..route) end
      end
      if scene~=lastScene then
        status('多人流程：'..scene..' · '..(total+1)..'/'..count)
        print('界面文字：'..screen.text)
        lastScene=scene;changed=tickCount()
      end
      if tickCount()-changed>120000 then error('多人界面等待超时：'..scene..'，请查看诊断截图') end
      if scene=='vehicleUnlock' then
        -- Highest priority: never dispatch a close-X, Back, Continue or unlock action here.
        if screen.unlockLater and screen.unlockLater.x<640 and screen.unlockLater.y>=500 then
          unlockMisses=0;status('发现新车可解锁，仅点击稍后查看')
          click(screen.unlockLater.x,screen.unlockLater.y);sleep(1200);changed=tickCount()
        else
          unlockMisses=unlockMisses+1;status('新车解锁保护：等待确认稍后查看')
          if unlockMisses>=3 then error('新车提示未能确认稍后查看；已停止，请由玩家处理，绝不自动解锁车辆') end
        end
      elseif word(screen,'稍后查看',530) then
        status('关闭广告：稍后查看');press(screen,'稍后查看',530);changed=tickCount()
      elseif screen.closeX and (not started or raced) then
        status('关闭广告：右上角 ×');click(screen.closeX.x,screen.closeX.y);sleep(1200)
      elseif scene=='unknown' and word(screen,'立即寻宝',530) and word(screen,'查看',530) then
        status('关闭寻宝广告：稍后查看');press(screen,'查看',530);changed=tickCount()
      elseif scene=='rewardClaim' then
        status('领取联赛晋级奖励');if press(screen,'领取',530) then changed=tickCount() end
      elseif scene=='rewardChoice' then
        -- Free post-race blueprint selection: the first visible card, then the labelled confirmation.
        status('领取动态奖励：选择首张图纸');click(130,400);sleep(500)
        if press(screen,'确认',530) then changed=tickCount() end
      elseif scene=='promo' then
        local closed=false
        for _,w in ipairs(screen.words or {}) do
          local mark=string.gsub(w.text,'[ \t\r\n]','')
          if (mark=='X' or mark=='x' or mark=='×') and w.x>900 and w.y<300 then
            status('关闭礼包弹窗');click(w.x,w.y);sleep(1200);closed=true;break
          end
        end
        if not closed then keyPress('back');sleep(1200) end
      elseif scene=='tutorial' then
        if press(screen,'继续',530) or press(screen,'确定',530) then started=false;changed=tickCount() end
      elseif scene=='leaderboard' then
        status('退出排行榜，返回赛事入口');click(1250,30);seriesSelected=false;sleep(1500)
      elseif has(screen.text,'观看广告') and word(screen,'错失机会',530) then
        status('跳过广告加倍奖励');press(screen,'错失机会',530);changed=tickCount()
      elseif scene=='unknown' and has(screen.text,'解锁') and word(screen,'带我去',530) and word(screen,'查看',530) then
        status('关闭新玩法解锁提示');press(screen,'查看',530);changed=tickCount()
      elseif scene=='unknown' and has(screen.text,'恭喜') and has(screen.text,'你获得了这些奖励') and word(screen,'架续',530) and word(screen,'架续',530).x>950 then
        -- Observed ML Kit confusion on the white card-pack Continue button; reward-only, bottom-right.
        status('关闭卡包奖励提示');press(screen,'架续',530);changed=tickCount()
      elseif (scene=='unknown' or scene=='result') and (has(screen.text,'奖励') or has(screen.text,'礼物') or has(screen.text,'名人堂') or has(screen.text,'恭喜') or has(screen.text,'你获得了')) and word(screen,'继续',530) then
        status('关闭奖励提示');press(screen,'继续',530);changed=tickCount()
      elseif (scene=='unknown' or scene=='result') and (scene=='result' or has(screen.text,'你的排名')) and word(screen,'继续',530) then
        resultSeen=raced;if raced then _sessionRaceResult(screen.finalRank or 0) end;status('已识别比赛结算：'..screen.text);press(screen,'继续',530)
      elseif raced and scene=='event' then
        if not resultSeen then error('已返回赛事页，但未确认比赛结算；本局不计为完成') end
        if not eventSince then eventSince=tickCount() end
        -- Milestone/Hall-of-Fame rewards may open just after the event page reappears.
        if tickCount()-eventSince>=3000 then
          total=total+1;data['多人']=data['多人']+1;_sessionRaceReturned()
          status('多人比赛已完成 '..total..'/'..count..'，已返回赛事页');break
        end
      elseif not seriesSelected and (scene=='event' or scene=='cars' or scene=='car') then
        click(1250,30);sleep(1500)
      elseif scene=='home' then
        press(screen,'多人',580)
      elseif scene=='series' then
        seriesSelected=press(screen,series)
      elseif scene=='event' then
        if has(screen.text,other) then error('赛事校验失败：目标是'..series..'，当前是'..other..'；已停止，不会开始比赛') end
        if has(screen.text,series) then
          status('已确认赛事：'..series);press(screen,'开始',550)
        end
      elseif scene=='cars' then
        -- Select an owned, fuelled car in configured order. Unlockable cards have no fuel label.
        local list
        if data['项目']=='多人下' then list=run.data['多人下用车'] or {'1'}
        else
          local rank=nil
          for _,name in ipairs({'青铜','白银','黄金','铂金','钻石','传奇'}) do
            if has(screen.rankText or '',name) then rank=name end
          end
          for _,w in ipairs(screen.words or {}) do
            if w.x<200 then
              for _,name in ipairs({'青铜','白银','黄金','铂金','钻石','传奇'}) do
                if has(w.text,name) then rank=name end
              end
            end
          end
          if rank then list=run.data[rank..'用车'] or {'1'} end
        end
        local chosen=false
        for _,raw in ipairs(list or {}) do
          local index=tonumber(raw)
          if index and index>=1 and index<=4 then
            local col=math.floor((index-1)/2);local row=(index-1)%2
            local x=210+col*464;local y=195+row*237
            for _,w in ipairs(screen.words or {}) do
              local fuel,capacity=string.match(w.text,'(%d+)%s*/%s*(%d+)')
              if fuel and tonumber(capacity)>=1 and tonumber(capacity)<=9 and tonumber(fuel)<=tonumber(capacity) and w.x>x and w.x<x+120 and w.y>y+170 and w.y<y+225 then
                if tonumber(fuel)==0 then print('跳过无油车辆序号 '..index..'，燃油 '..fuel..'/'..capacity)
                else
                status('选择车辆序号 '..index..'，燃油 '..fuel..'/'..capacity)
                click(x+200,y+110);sleep(1800);chosen=true;break
                end
              end
            end
          end
          if chosen then break end
        end
        if chosen then carRetries=0
        else
          carRetries=carRetries+1
          if carRetries>=6 then error(list and '当前可见车辆中没有已配置且有燃油的车辆；请核对车辆序号' or '多次未识别联赛段位，请查看诊断截图') end
          sleep(400)
        end
      elseif scene=='car' and not started then
        -- The whole TD switch toggles, even when tapping its already-selected half.
        if cmpColor(60,830,'12FBC3-303030',1)~=1 then
          status('开启 TouchDrive');click(853,670);sleep(1200)
        elseif press(screen,'开始',550) then started=true;changed=tickCount();status('等待匹配与地图识别') end
      elseif scene=='race' then
        if not raced then
          raced=true
          local forcedLeft=route and leftSwipeMaps[route]
          fallback=forcedLeft or (mode=='3' and not route)
          _sessionRaceBegin(route or '',fallback)
          if fallback then
            raceMode='1'
            fallbackReason=forcedLeft and ('已识别地图：'.._routeDisplay(route)..'，固定左划兜底') or '地图识别失败，左划兜底'
            status(fallbackReason);_fallbackBegin()
          elseif mode=='3' then
            status('已识别地图：'.._routeDisplay(route));utils.getMapName(route);run.nitro()
            utils.newAutoDriver();status('跑图结束，等待结算');changed=tickCount()
          else _fallbackBegin()
          end
        end
        if raceMode~='3' then
          _sessionOperation(raceMode=='1' and '左划 + 氮气' or raceMode=='2' and '右划 + 氮气' or '氮气')
          local p=_fallbackStep()
          if p>=0 then data['路程']=p end
          data['信息']=fallback and fallbackReason or '多人选路执行中'
          changed=tickCount()
        end
        if tickCount()-lastPulse>700 then
          click(1100,483)
          if raceMode=='1' then swipe(288,840,288,440,50)
          elseif raceMode=='2' then swipe(288,440,288,840,50) end
          lastPulse=tickCount()
        end
      elseif raced then
        -- Only dismiss explicitly labelled result/continue controls. Never enter purchase flows.
        if press(screen,'继续',530) or press(screen,'下一步',530) then changed=tickCount()
        elseif has(screen.text,'点击任意位置') then click(640,550);sleep(1200) end
      elseif scene=='unknown' then
        -- Recover from a known non-racing detail page once, then let recognition settle.
        if not started and has(screen.text,'新手狂欢礼') then keyPress('back');sleep(1200) end
      end
      sleep(350)
    end
  end
end
