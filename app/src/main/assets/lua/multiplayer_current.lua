-- Current CN series cards and vehicle layout. Coordinates below are upright 1280x720.
-- OCR guards each menu action; race commands still use the existing route engine.
currentMultiplayer={}
vehicleGrid={}
local function click(x,y) tap(719-math.floor(y),math.floor(x)) end
local function has(text,part)
  local normalized=string.gsub(string.gsub(text,'[ \t\r\n]',''),'賽','赛')
  normalized=string.gsub(normalized,'错矢机会','错失机会')
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
local function settlementStatus(screen)
  local rank=tonumber(screen.finalRank) or 0
  return rank>0 and ('开始结算 · 第 '..rank..' 名') or '开始结算'
end
local function worldRewardPosition(rules,before,after)
  if not before or not after then return nil end
  for _,rule in ipairs(rules) do
    if rule.score>before then
      if rule.score<=after then return rule.position,rule.score end
      return nil
    end
  end
end
local leagues={'青铜','白银','黄金','白金','翡翠','钻石','精英','宗师','传奇'}
local function blankCarList(list)
  return not list or #list==0 or (#list==1 and tonumber(list[1])==0)
end
local function lowerConfiguredLeague(current)
  for i,league in ipairs(leagues) do
    if league==current then
      for j=i-1,1,-1 do
        if not blankCarList(run.data[leagues[j]..'用车']) then return leagues[j] end
      end
      break
    end
  end
end
-- Track ordinal positions through overlapping identified cards after each short drag.
-- A gesture alone never advances the index, including when it is cancelled or overshoots.
local function sameCard(a,b,tolerance,minContrast)
  local first,second=a.signature,b.signature
  if not first or not second or #first~=#second then return false end
  local differences,contrast=0,0
  for i=1,#first do
    if first:byte(i)==49 then contrast=contrast+1 end
    if first:byte(i)~=second:byte(i) then differences=differences+1 end
    if differences>(tolerance or 6) then return false end
  end
  return contrast>=(minContrast or 8) and contrast<=#first-8
end
-- The game often reopens on the previous car. Only reuse that page if two artworks
-- agree on one column shift in the same league.
local function rememberedOffset(saved,garage)
  local offset=nil;local matches=0
  for _,old in ipairs(saved.cards) do
    for _,card in ipairs(garage.cards or {}) do
      if old.index%2==card.index%2 and sameCard(old,card) then
        local candidate=saved.offset+old.index-card.index
        if candidate<0 or (offset and offset~=candidate) then return nil end
        offset=candidate;matches=matches+1
      end
    end
  end
  if matches>=2 then return offset end
end
local function confirmCar(screen,card,index,position,readFull)
  local garage=screen.garage
  local anchor={offset=position.offset,cards=garage.cards}
  local lastSlot,lastLeft,lastOffset,lastFrame
  for retry=1,5 do
    local full=readFull and readFull() or _multiplayerUi(false,false)
    if full.scene~='cars' then error('目标车位已离开车库，停止选车') end
    if full.frameId~=screen.frameId and full.frameId~=lastFrame then
      local current=full.garage
      local left=current.left
      local samePage=math.abs(left-garage.left)<=3
      local offset=samePage and position.offset or rememberedOffset(anchor,current)
      local candidate
      for _,slot in ipairs(current.cards or {}) do
        if offset and slot.index+offset==index and not slot.clipped then
          if samePage and sameCard(card,slot,12) then return slot,full end
          if not samePage and lastSlot and offset==lastOffset and math.abs(left-lastLeft)<=3 and sameCard(lastSlot,slot,12) then
            position.offset=offset
            return slot,full
          end
          candidate=slot
          break
        end
      end
      lastSlot=candidate;lastLeft=left;lastOffset=offset;lastFrame=full.frameId
    end
    sleep(150)
  end
  error('目标车位仍在移动，未选择车辆')
end
local function awaitGarageOverlap(previous,position,direction,afterFrame,readGrid)
  local stableOffset,stableLeft,lastGarage
  local stableFrames=0
  for retry=1,18 do
    sleep(90)
    local nextScreen=readGrid and readGrid() or _multiplayerUi(false,false,true)
    if nextScreen.scene~='cars' then error('跨屏选车时已离开车库，已停止') end
    if nextScreen.frameId and nextScreen.frameId>afterFrame then
      afterFrame=nextScreen.frameId
      local nextGarage=nextScreen.garage or {};lastGarage=nextGarage
      local offset=nil
      if not readGrid and direction==-1 and nextGarage.start and nextGarage.rank==position.rank then offset=0
      else
        local candidates={}
        local moved=(tonumber(previous.left) or 0)-(tonumber(nextGarage.left) or 0)
        for _,old in ipairs(previous.cards or {}) do
          for _,new in ipairs(nextGarage.cards or {}) do
            local delta=old.index-new.index
            local shifted=delta*direction>0 or moved*direction>=20
            if position.offset+delta>=0 and delta*direction>=0 and delta%2==0 and shifted then
              local match=sameCard(old,new,12)
              local lowContrast=old.clipped or new.clipped or
                (old.availability=='locked' and new.availability=='locked')
              if match or (lowContrast and sameCard(old,new,8,5)) then
                local evidence=candidates[delta] or {matches=0,confirmed=false}
                evidence.matches=evidence.matches+(match and sameCard(old,new) and 2 or 1)
                evidence.confirmed=evidence.confirmed or match
                candidates[delta]=evidence
              end
            end
          end
        end
        for delta,evidence in pairs(candidates) do
          if evidence.confirmed and evidence.matches>=2 then
            if offset then offset=nil;break end
            offset=position.offset+delta
          end
        end
      end
      local left=tonumber(nextGarage.left) or 0
      if offset and offset==stableOffset and stableLeft and math.abs(left-stableLeft)<=3 then
        stableFrames=stableFrames+1
      else stableFrames=1 end
      stableOffset=offset;stableLeft=left
      if offset and stableFrames>=2 then return nextScreen,offset end
    end
  end
  return nil,nil,lastGarage,afterFrame
end
local function locateCar(screen,index,position,readGrid,readFull)
  local garage=screen.garage
  local attempts=0
  while true do
    for _,card in ipairs(garage.cards or {}) do
      if card.index+position.offset==index and not card.clipped then
        if screen.gridOnly then
          -- Only the destination needs full menu OCR; the intervening columns use pixels.
          card,screen=confirmCar(screen,card,index,position,readFull)
        end
        local result={}
        for k,v in pairs(card) do result[k]=v end
        result.index=index
        return result,screen
      end
    end
    attempts=attempts+1
    if attempts>60 then error('无法连续核验配置车辆 '..index..' 的跨屏位置，已停止选车') end
    local direction=index<=position.offset and -1 or 1
    local pitch=math.min(tonumber(garage.pitch) or 359,readGrid and 480 or 464)
    -- Ultrawide layouts can show only two complete columns. Leave most of a column
    -- visible across the drag, and give the carousel time to settle before release.
    local wide=pitch==359 or readGrid~=nil
    local distance=wide and math.floor(pitch*0.7) or pitch
    local duration,hold=wide and 600 or 300,wide and 180 or 100
    local from=direction==1 and 1100 or 300
    swipe(419,from,419,from-direction*distance,duration,hold)
    local nextScreen,offset,lastGarage,lastFrame=awaitGarageOverlap(garage,position,direction,screen.frameId,readGrid)
    if not nextScreen and wide and lastGarage then
      -- A short reverse drag can bring the overlap back. Keep the same verified anchor
      -- and direction until the corrected position is confirmed in consecutive frames.
      print('宽屏选车翻页重叠不足，回拖核验上一页车辆')
      local backFrom=direction==1 and 300 or 1100
      swipe(419,backFrom,419,backFrom+direction*distance,duration,hold)
      nextScreen,offset,lastGarage=awaitGarageOverlap(garage,position,direction,lastFrame,readGrid)
    end
    if not nextScreen then
      if _jsonEncode then print('翻页定位失败，起始偏移 '..position.offset..'：'.._jsonEncode(garage)..' → '.._jsonEncode(lastGarage or {})) end
      error('车辆 '..index..' 翻页后无法核对重叠车辆，已停止，未改选其他车辆')
    end
    position.offset=offset;screen=nextScreen;garage=nextScreen.garage
  end
end

local function sameSoloPage(before,after)
  if not before or not after or before.start~=after.start or math.abs(before.left-after.left)>3 then return false end
  local matches=0
  for _,a in ipairs(before.cards) do
    for _,b in ipairs(after.cards) do
      if a.index==b.index and sameCard(a,b,12,5) then matches=matches+1 end
    end
  end
  return matches>=math.min(2,#after.cards) and matches>0
end

local function settledSoloGrid()
  local previous
  local stable=0
  for sample=1,14 do
    local page=_soloGarageUi(true)
    local garage=page.garage
    if garage and #garage.cards>0 then
      if not garage.leadingClear then return page end
      -- An empty leading gutter can grow during overscroll. Wait for it to
      -- return to the normal inset and verify the same leading cards in fresh frames.
      if not previous or page.frameId>previous.frameId then
        stable=garage.start and sameSoloPage(previous and previous.garage,garage) and stable+1 or 0
        if stable>=2 then return page end
        previous=page
      end
    else
      previous=nil;stable=0
    end
    sleep(250)
  end
end

function vehicleGrid.soloOrigin()
  local previous,stalled,drags=nil,0,0
  while stalled<5 and not data['超时状态'] do
    local current=settledSoloGrid()
    if not current then break end
    if current.garage.start then
      print('每日战利品车库已回到最左，确认第 1 列（回拖 '..drags..' 次）')
      return current
    end
    if sameSoloPage(previous,current.garage) then stalled=stalled+1 else stalled=0 end
    if stalled<5 then
      previous=current.garage
      -- Travel by momentum while an earlier column still occupies the leading gutter.
      swipe(414,255,414,1150,190)
      drags=drags+1
      sleep(550)
    end
  end
  error('未确认每日战利品车库最左起点，已保留任务进度')
end

function vehicleGrid.soloCar(origin,index)
  return locateCar(origin,index,{offset=0},
    function()return _soloGarageUi(true)end,function()return _soloGarageUi(false)end)
end

function currentMultiplayer.run(resumingTutorial)
  local count=tonumber(run.data['次数']) or 1
  local completed=_sessionProject(data['项目'],count) or 0
  for _,key in ipairs({'选车筛选'}) do
    if utils.defultCfg(run.data[key],'')~='' then error('新版多人菜单尚未适配“'..key..'”，请关闭此选项后运行') end
  end
  local rotate=tostring(utils.defultCfg(run.data['用车模式'],'0'))=='1'
  local usedCars={}
  local lastPage=nil
  local mode='3'
  local series=data['项目']=='多人上' and '世界系列赛' or '大师系列赛'
  local other=data['项目']=='多人上' and '大师系列赛' or '世界系列赛'
  local world=data['项目']=='多人上'
  local autoReward=world and run.data['自选奖励']=='0'
  local rewardRules=run.data.worldRewardRules or {}
  local readSeriesScore=autoReward and #rewardRules>0
  status('目标赛事：'..series..'（'..data['项目']..'）')
  local disconnects=0;local total=completed;local unlockMisses=0;local milestoneMisses=0;local loginAttempts=0;local loginMissing=0;local serverErrors=0;local noOpponents=0
  while total<count do
    local route=nil;local routeAvailable=true;local candidate=nil;local candidateFrames=0;local lastMapText=''
    local raceMode=mode;local fallback=false;local fallbackReason=''
    local raced=false;local resultSeen=false;local eventSince=nil;local rewardsSince=nil;local rewardsDone=false;local rewardExitAt=nil
    local lastScene='';local changed=tickCount();local started=resumingTutorial==true;local seriesSelected=started
    resumingTutorial=false
    local seriesEntry,seriesFrame,seriesPressedAt
    local tutorialPressedAt=nil
    local demotionPressedAt=nil
    local lastPulse=0;local carRetries=0;local carListAligned=false
    local eventPressedAt=nil;local carPressedAt=nil;local selectedCar=nil
    local unverifiedCarBackAt=nil;local unverifiedCarBacks=0
    local returnHomeAt=nil;local emptyCars={};local emptyDetailFrames=0;local emptyDetailFrame=nil
    local originRetries=0;local alignAttempts=0;local alignKey=nil;local alignFrames=0;local alignFrame=nil;local alignSwipeFrame=nil;local verifiedKey=nil
    local position={offset=0}
    local pageOffset,pageLeft,pageFrame
    local verifiedWorldLeague,targetLeague
    local ratingScore,ratingFrame
    local leagueWaitAt=nil
    local rewardChoice=nil;local rewardAt=0;local rewardTries=0;local dialogMisses=0
    local scoreBefore,scoreAfter,scoreSample,scoreFrame,scoreSince
    local rewardSeenAt,rewardConfirmedAt,rewardObservedCard,rewardObservedAt
    local rewardHandled=false
    local returnFrame=nil;local returnFrames=0
    local postActionKey,postActionFrame,postCandidate,settlementProgressAt
    local function postPress(screen,label,minY,phase)
      local button=word(screen,label,minY)
      local exit=screen.rewardExitButton
      if phase=='raceRewards' and exit and exit.label==label then button=exit end
      if not button then postCandidate=nil;return false end
      if not raced then click(button.x,button.y);sleep(1200);return true end
      local frame=screen.frameId or tickCount()
      local key=phase..':'..label
      if phase=='bonusReward' then
        -- Successive reward pages can use the same Continue label and heading.
        -- The reward text distinguishes batches after two matching fresh frames.
        local rewardText=(screen.text or ''):gsub('%s',''):gsub('賽','赛'):gsub('續','续')
        key=key..':'..rewardText
      end
      -- OCR and tap dispatch can outlive a page animation. Never repeatedly tap
      -- Continue through a stale reward image into the next menu's Start button.
      -- Different reward batches can share the same Continue label, so use it
      -- only for pacing taps across fresh frames.
      if postActionFrame and (frame-postActionFrame<2200 or
          (key==postActionKey and frame-postActionFrame<6500)) then
        data['信息']='已点击结算按钮，等待切页';sleep(500);return true
      end
      if not postCandidate or postCandidate.key~=key or math.abs(postCandidate.x-button.x)>8
          or math.abs(postCandidate.y-button.y)>8 then
        postCandidate={key=key,frame=frame,x=button.x,y=button.y}
        return true
      end
      if frame<=postCandidate.frame then return true end
      if key~=postActionKey then
        settlementProgressAt=tickCount()
        if phase=='raceRewards' then rewardExitAt=tickCount()
        else rewardsSince=nil;rewardExitAt=nil end
      end
      postActionKey=key;postActionFrame=frame
      postCandidate=nil
      print('多人结算：点击'..label)
      click(button.x,button.y);sleep(1200);return true,true
    end
    while true do
      local screen=_multiplayerUi(readSeriesScore,started or raced)
      local scene=screen.scene
      if world and scene=='event' and has(screen.text,series) then
        local league=screen.worldLeague
        if not league and screen.worldLeagueByRating and screen.worldRating and screen.frameId then
          if screen.worldRating==ratingScore and screen.frameId>ratingFrame then
            league=screen.worldLeagueByRating
            if league~=verifiedWorldLeague then print('按评级分 '..ratingScore..' 确认世界赛段位：'..league) end
          elseif screen.worldRating~=ratingScore then
            ratingScore=screen.worldRating;ratingFrame=screen.frameId
          end
        else ratingScore,ratingFrame=nil,nil end
        if league and verifiedWorldLeague~=league then
          verifiedWorldLeague=league
          print('赛事详情已确认世界赛当前段位：'..league)
        end
      else ratingScore,ratingFrame=nil,nil end
      if scene~='series' then seriesEntry=nil;seriesFrame=nil;seriesPressedAt=nil end
      local tutorial=started and not raced and spCup and spCup.tutorialButton(screen)
      if _racePanel then _racePanel(scene=='race') end
      if scene=='event' or scene=='home' or scene=='cars' or scene=='series' then rewardsSince=nil end
      if scene~='rewardChoice' then
        rewardChoice=nil;rewardTries=0;rewardSeenAt=nil;rewardConfirmedAt=nil
        rewardObservedCard=nil;rewardObservedAt=nil
      end
      local returnedMenu=scene=='event' or scene=='home' or scene=='series' or scene=='cars' or scene=='car'
      if not returnedMenu then eventSince=nil;returnFrame=nil;returnFrames=0 end
      if scene~='noOpponents' and scene~='serverError' then dialogMisses=0 end
      if raced and scene=='result' then
        _sessionRaceResult(screen.finalRank or 0)
        if not resultSeen then resultSeen=true;settlementProgressAt=tickCount();status(settlementStatus(screen)) end
      end
      if raced and resultSeen and not returnedMenu and scene~='race' and
          tickCount()-(settlementProgressAt or tickCount())>=90000 then
        error('多人结算长时间未能切页，已保留完赛进度')
      end
      if started and not raced and scene=='unknown' and not tutorial then
        if screen.mapText and screen.mapText~=lastMapText then
          lastMapText=screen.mapText;print('地图标题 OCR：'..lastMapText)
          snapShot(getWorkPath()..'/multiplayer-map.png')
        end
        if screen.route and screen.route==candidate then candidateFrames=candidateFrames+1
        else candidate=screen.route;candidateFrames=screen.route and 1 or 0 end
        if candidateFrames>=2 and route~=candidate then
          route=candidate;routeAvailable=screen.routeAvailable~=false;print('地图标题连续确认：'..route)
        end
      end
      if scene~=lastScene then
        print('多人流程：'..scene..' · '..(total+1)..'/'..count)
        print('界面文字：'..screen.text)
        lastScene=scene;changed=tickCount()
      end
      -- The host handles the timeout while matching is confirmed.
      if scene~='matching' and tickCount()-changed>120000 then error('多人界面等待超时：'..scene..'，请查看诊断截图') end
      if tutorial then
        if not tutorialPressedAt then
          tutorialPressedAt=tickCount()
          status('多人开赛教学：已确认并点击开始')
          click(tutorial.x,tutorial.y);sleep(1500)
        elseif tickCount()-tutorialPressedAt>30000 then error('多人开赛教学点击后未能确认切页，保留当前任务')
        else sleep(500) end
      elseif scene=='leagueDemotion' then
        local confirm=screen.demotionConfirm
        if confirm and not demotionPressedAt then
          verifiedWorldLeague=nil
          demotionPressedAt=tickCount()
          status('已确认世界赛降级提示，等待返回赛事')
          click(confirm.x,confirm.y);sleep(1500)
        elseif demotionPressedAt and tickCount()-demotionPressedAt>30000 then
          error('世界赛降级提示未能关闭，已保留任务进度')
        else sleep(500) end
      elseif scene=='profileUnlock' then
        dailyCup.dismissProfile(screen,_multiplayerUi);changed=tickCount()
      elseif scene=='vehicleUnlock' then
        -- Highest priority: never dispatch a close-X, Back, Continue or unlock action here.
        if screen.unlockLater and screen.unlockLater.x<640 and screen.unlockLater.y>=500 then
          unlockMisses=0;status('车辆提示：稍后查看')
          click(screen.unlockLater.x,screen.unlockLater.y);sleep(1200);changed=tickCount()
        else
          unlockMisses=unlockMisses+1;status('车辆提示保护：等待确认稍后查看')
          if unlockMisses>=3 then error('车辆提示未能确认稍后查看，已停止，请由玩家处理') end
        end
      elseif scene=='milestoneLater' then
        local later=word(screen,'稍后领取',530)
        if later and later.x<640 then
          milestoneMisses=0;status('俱乐部里程碑：稍后领取')
          click(later.x,later.y);sleep(1200);changed=tickCount()
        else
          milestoneMisses=milestoneMisses+1
          data['信息']='等待确认里程碑的稍后领取按钮';sleep(500)
          if milestoneMisses>=6 then error('里程碑页面无法确认稍后领取，已停止避免误点') end
        end
      elseif raced and returnedMenu then
        if not resultSeen then error('已返回比赛菜单，但未确认比赛结算；本局不计为完成') end
        local frame=screen.frameId or tickCount()
        if not eventSince then eventSince=frame end
        if frame~=returnFrame then returnFrames=returnFrames+1;returnFrame=frame end
        -- Finalize before any menu action, even when the event page was brief or missed.
        -- The outer loop discards the previous race's selectedCar/carPressedAt/route state.
        if frame-eventSince>=3000 and returnFrames>=2 then
          disconnects=0;total=total+1;data['多人']=data['多人']+1;_sessionRaceReturned()
          status('多人比赛已完成 '..total..'/'..count..'，已返回比赛菜单')
          break
        else data['信息']='等待赛后菜单稳定';sleep(500) end
      elseif scene=='raceDisconnected' then
        -- Disconnect results also contain Your time / Ranking. Never count them as finishes,
        -- even if the next OCR frame temporarily loses the disconnect label.
        raced=false;resultSeen=false
        local nextButton=word(screen,'继续',530)
        if nextButton then
          disconnects=disconnects+1
          if disconnects>3 then error('连续比赛断线，已停止并保留有效完赛局数') end
          status('比赛断开连接，不计入完赛；返回后重新匹配')
          click(nextButton.x,nextButton.y);sleep(5000)
          break
        else data['信息']='比赛断线，等待返回按钮';sleep(500) end
      elseif scene=='raceCancelled' then
        local nextButton=word(screen,'继续',530)
        if nextButton then
          noOpponents=noOpponents+1
          if noOpponents>5 then error('连续未找到对手，已停止并保存进度，请在玩家较多时继续任务') end
          status('比赛尚未开始且未找到车手，等待后重新匹配，不计入完赛')
          click(nextButton.x,nextButton.y);sleep(30000)
          break -- Reset started/route state after a cancelled loading sequence.
        end
      elseif scene=='noOpponents' or scene=='serverError' then
        if not screen.closeX then
          dialogMisses=dialogMisses+1
          if dialogMisses>=6 then error('错误提示弹窗多次无法确认关闭按钮，已停止并保留任务') end
          data['信息']='等待错误提示弹窗动画结束';sleep(500)
        else
          dialogMisses=0
          if scene=='noOpponents' then
            noOpponents=noOpponents+1
            if noOpponents>5 then error('连续未找到对手，已停止并保存进度，请在玩家较多时继续任务') end
            status('未找到对手，关闭提示并等待 30 秒后重新匹配')
          else
            serverErrors=serverErrors+1
            if serverErrors>3 then error('服务器连续返回错误，已停止并保留已完成局数，请稍后继续任务') end
            status('关闭服务器错误，等待后重试未完成的比赛')
          end
          click(screen.closeX.x,screen.closeX.y);sleep(scene=='noOpponents' and 30000 or 5000)
          if resultSeen then total=total+1 end
          break -- Restart navigation; an unconfirmed race never consumes quota.
        end
      elseif scene=='matching' then
        data['信息']='匹配中'
        data['心跳时间']=os.time();data['超时状态']=false
      elseif scene=='loginHistory' then
        local recent=screen.recentAccount
        if not recent then
          loginMissing=loginMissing+1
          if loginMissing>30 then error('登录历史无法唯一确认最近登录账号，已停止，请由玩家选择') end
          data['信息']='等待登录历史加载';sleep(500)
        else
          loginMissing=0;loginAttempts=loginAttempts+1
          if loginAttempts>3 then error('最近登录账号未能进入游戏，请由玩家检查登录状态') end
          status('恢复最近登录的游戏账号');click(recent.x,recent.y);sleep(5000);changed=tickCount()
        end
      elseif not raced and not started and screen.rewardPage and screen.rewardCanContinue then
        status('恢复赛后奖励页，返回赛事')
        postPress(screen,screen.rewardExit or '继续',590,'raceRewards');changed=tickCount()
      elseif raced and screen.rewardPage and scene~='rewardChoice' then
        if not rewardsSince then rewardsSince=tickCount() end
        if readSeriesScore and scoreBefore and screen.seriesScore and screen.seriesScore>=scoreBefore and screen.frameId~=scoreFrame then
          scoreFrame=screen.frameId
          if scoreSample~=screen.seriesScore then
            scoreSample=screen.seriesScore;scoreSince=tickCount();scoreAfter=nil
          elseif tickCount()-scoreSince>=750 then scoreAfter=scoreSample end
        end
        local scoreReady=not readSeriesScore or not scoreBefore or scoreAfter or tickCount()-rewardsSince>=5000
        if scoreReady and screen.rewardCanContinue and (rewardsDone or screen.rewardReady or tickCount()-rewardsSince>=8000) then
          if not rewardsDone then
            _sessionRaceRewards(screen.rewardCoins or -1,screen.rewardFame or -1);rewardsDone=true
          end
          local _,pressed=postPress(screen,screen.rewardExit=='错失机会' and '错失机会' or '继续',590,'raceRewards')
          if pressed then changed=tickCount() end
        else data['信息']='等待结算完成并识别本局收益';sleep(400) end
        if tickCount()-(rewardExitAt or rewardsSince)>=60000 then
          error('比赛奖励页面未完成结算或继续按钮不可用，已保留完赛进度')
        end
      elseif raced and not rewardsDone and
          (has(screen.text,'总金币') or has(screen.text,'总全币')) and
          (has(screen.text,'比赛奖励') or has(screen.text,'比赛赛奖励')) then
        rewardsSince=rewardsSince or tickCount()
        if tickCount()-rewardsSince>=60000 then
          error('比赛奖励页面未完成结算或继续按钮不可用，已保留完赛进度')
        end
        data['信息']='等待比赛奖励页稳定';sleep(400)
      elseif scene=='rewardChoice' then
        if world and not autoReward then error('世界赛动态奖励等待手动选择。选好后可续跑') end
        rewardSeenAt=rewardSeenAt or tickCount()
        if tickCount()-rewardSeenAt>=15000 then error('未能确认动态奖励的选卡或领取结果，请手动处理后续跑') end
        local cards=screen.rewardChoices or {}
        local confirm
        for _,button in ipairs(screen.words or {}) do
          if button.y>=530 and string.gsub(button.text,'%s','')=='确认' then confirm=button;break end
        end
        if not rewardChoice then
          local candidate,description
          if not world then
            if #cards>0 then candidate=cards[rnd(1,#cards)];description='随机选择一张可见奖励' end
          elseif readSeriesScore then
            local position,score=worldRewardPosition(rewardRules,scoreBefore,scoreAfter)
            if rewardHandled or not position then
              error('无法确认本局动态奖励对应已配置的里程碑（赛前 '..tostring(scoreBefore or '未知')..'，赛后 '..tostring(scoreAfter or '未知')..'），请手动选择后续跑')
            end
            if screen.rewardOrderKnown then candidate=cards[position] end
            description='里程碑 '..score..'，选择第 '..position..' 张奖励'
          elseif screen.rewardOrderKnown then
            for _,card in ipairs(cards) do
              if not card.visible or card.price=='UNKNOWN' then break end
              if card.price=='ABSENT' then candidate=card;description='选择最左边的非红币奖励';break end
              if card.price~='PRESENT' then break end
            end
          end
          if candidate and candidate.visible~=false then
            local key=candidate.x..':'..candidate.y..':'..tostring(candidate.price)
            if key~=rewardObservedCard then rewardObservedCard=key;rewardObservedAt=tickCount() end
            if not world or tickCount()-rewardObservedAt>=700 then
              rewardChoice=candidate;rewardAt=tickCount();rewardTries=1
              status('动态奖励：'..description);click(candidate.x,candidate.y);sleep(700)
            end
          else rewardObservedCard=nil end
        elseif confirm and (not rewardConfirmedAt or tickCount()-rewardConfirmedAt>=3000) then
          rewardConfirmedAt=tickCount();rewardHandled=true
          status('确认领取动态奖励');click(confirm.x,confirm.y);sleep(1200)
        elseif not rewardConfirmedAt and tickCount()-rewardAt>=3000 then
          rewardTries=rewardTries+1
          if rewardTries>3 then error('动态奖励选择后未出现确认按钮，请手动处理后续跑') end
          rewardAt=tickCount();click(rewardChoice.x,rewardChoice.y);sleep(700)
        end
      elseif word(screen,'稍后查看',530) then
        status('关闭广告：稍后查看');press(screen,'稍后查看',530);changed=tickCount()
      elseif screen.closeX and (not started or raced) then
        status('关闭广告：右上角 ×');click(screen.closeX.x,screen.closeX.y);sleep(1200)
      elseif scene=='unknown' and word(screen,'立即寻宝',530) and word(screen,'查看',530) then
        status('关闭寻宝广告：稍后查看');press(screen,'查看',530);changed=tickCount()
      elseif scene=='rewardClaim' then
        verifiedWorldLeague=nil
        status('领取联赛晋级奖励');if press(screen,'领取',530) then changed=tickCount() end
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
      elseif scene=='dailyGoals' then
        local closed=false
        for _,w in ipairs(screen.words or {}) do
          local mark=string.gsub(w.text,'%s','')
          if (mark=='X' or mark=='x' or mark=='×') and w.x>1100 and w.y<200 then
            status('关闭每日目标奖励页，继续目标赛事')
            click(w.x,w.y);sleep(1500);closed=true;break
          end
        end
        if closed and not raced then break end
        data['信息']='等待每日目标页面关闭按钮';sleep(500)
      elseif scene=='options' then
        status('退出游戏选项，重新进入目标赛事')
        click(38,29);sleep(1500)
        if not raced then break end -- Keep confirmed post-race accounting until the menu returns.
      elseif scene=='leaderboard' then
        status('退出排行榜，返回赛事入口');click(1250,30);seriesSelected=false;sleep(1500)
      elseif has(screen.text,'观看广告') and word(screen,'错失机会',530) then
        status('跳过广告加倍奖励');postPress(screen,'错失机会',530,'raceRewards');changed=tickCount()
      elseif scene=='unknown' and has(screen.text,'解锁') and word(screen,'带我去',530) and word(screen,'查看',530) then
        status('关闭新玩法解锁提示');press(screen,'查看',530);changed=tickCount()
      elseif scene=='unknown' and has(screen.text,'恭喜') and has(screen.text,'你获得了这些奖励') and word(screen,'架续',530) and word(screen,'架续',530).x>950 then
        -- Observed ML Kit confusion on the white card-pack Continue button; reward-only, bottom-right.
        status('关闭卡包奖励提示');postPress(screen,'架续',530,'bonusReward');changed=tickCount()
      elseif (scene=='unknown' or scene=='result') and (has(screen.text,'奖励') or has(screen.text,'礼物') or has(screen.text,'名人堂') or has(screen.text,'恭喜') or has(screen.text,'你获得了')) and word(screen,'继续',530) then
        status('关闭奖励提示');postPress(screen,'继续',530,'bonusReward');changed=tickCount()
      elseif (scene=='unknown' or scene=='result') and (scene=='result' or has(screen.text,'你的排名')) and word(screen,'继续',530) then
        resultSeen=raced;if raced then _sessionRaceResult(screen.finalRank or 0) end;status(settlementStatus(screen));postPress(screen,'继续',530,'rankResult')
      elseif not seriesSelected and (scene=='event' and not has(screen.text,series) or scene=='cars' or scene=='car') then
        if not returnHomeAt then
          returnHomeAt=tickCount()
          if screen.homeButton then click(screen.homeButton.x,screen.homeButton.y) else click(1250,30) end
          sleep(1500)
        elseif tickCount()-returnHomeAt>30000 then
          error('返回主界面后未确认切页，保留当前任务')
        else data['信息']='已点击返回主界面，等待切页';sleep(500) end
      elseif scene=='exitGame' then
        utils.backToMain()
      elseif scene=='home' then
        returnHomeAt=nil
        -- OCR can split 多人/游戏 into separate lines and omit 人. Both labels are in the same bottom tab.
        if not press(screen,'多人',580) and not press(screen,'多人游戏',580) then press(screen,'游戏',580) end
      elseif scene=='series' then
        if seriesPressedAt then
          if tickCount()-seriesPressedAt>30000 then error('点击赛事入口后未能确认切页，保留当前任务') end
        else
          -- The tabs can expose both series labels while the cards are still sliding.
          -- Only tap once the target label holds its position across fresh captures.
          local entry=word(screen,series)
          if entry and seriesEntry and screen.frameId and seriesFrame and screen.frameId>seriesFrame
              and math.abs(entry.x-seriesEntry.x)<=12 and math.abs(entry.y-seriesEntry.y)<=12 then
            seriesSelected=true;seriesPressedAt=tickCount()
            click(entry.x,entry.y);sleep(1200)
          else seriesEntry=entry;seriesFrame=screen.frameId end
        end
      elseif scene=='event' then
        if has(screen.text,other) then error('赛事校验失败：目标是'..series..'，当前是'..other..'；已停止，不会开始比赛') end
        local start=word(screen,'开始',550)
        if start and (start.x<900 or start.text:gsub('%s','')~='开始') then start=nil end
        if eventPressedAt then
          if tickCount()-eventPressedAt>45000 then
            if start and has(screen.text,series) and _recoverEventStart then _recoverEventStart() end
            error('点击赛事开始后未能确认选车页面，已停止避免重复点击')
          end
          data['信息']='已点击赛事开始，等待选车页面';sleep(500)
        elseif has(screen.text,series) then
          if start then
            if world and not verifiedWorldLeague then
              leagueWaitAt=leagueWaitAt or tickCount()
              if tickCount()-leagueWaitAt>=15000 then error('未确认世界系列赛当前段位，保留任务') end
              data['信息']='正在识别世界系列赛当前段位'
            else
              targetLeague=world and verifiedWorldLeague or nil
              seriesSelected=true
              scoreBefore=screen.seriesScore
              if readSeriesScore then print('世界赛赛前系列赛分数：'..tostring(scoreBefore or '未识别')) end
              eventPressedAt=tickCount()
              click(start.x,start.y);sleep(1200)
              print('已确认赛事：'..series..'，开始已点击一次，等待切页')
            end
          end
        end
      elseif scene=='cars' then
        unverifiedCarBackAt=nil
        local garage=screen.garage or {}
        local tabs=screen.leagueTabs or {}
        if world and not targetLeague then
          targetLeague=verifiedWorldLeague
          if not targetLeague then error('未确认世界系列赛当前段位，保留任务') end
        end
        local correctLeague=not targetLeague or (garage.rank==targetLeague and
          (not tabs.selected or tabs.selected==targetLeague))
        local reusePage,confirmPage=false,false
        if world and not carPressedAt and not carListAligned and alignAttempts==0 and lastPage and
            tabs.selected==lastPage.rank and targetLeague==lastPage.rank and not garage.start then
          local offset=rememberedOffset(lastPage,garage)
          if offset then
            reusePage=pageFrame~=nil and screen.frameId~=pageFrame and pageOffset==offset and math.abs(pageLeft-garage.left)<=3
            confirmPage=not reusePage
            pageOffset=offset;pageLeft=garage.left;pageFrame=screen.frameId
            if reusePage then
              position={offset=offset,rank=targetLeague}
              print('复用'..targetLeague..'上局第 '..lastPage.index..' 辆的位置，当前首列 '..(offset+1)..'、'..(offset+2))
            end
          end
        end
        if carPressedAt then
          if tickCount()-carPressedAt>30000 then error('选车后未能确认车辆详情，已停止避免重复点击') end
          data['信息']='等待所选车辆详情';sleep(500)
        elseif confirmPage then sleep(200)
        elseif not carListAligned and not reusePage then
          if alignAttempts>=12 then error('无法确认选车列表起点，已停止，未选择车辆') end
          if screen.frameId and screen.frameId~=alignFrame then
            alignFrame=screen.frameId
            -- Use capture time, not the Lua scheduler clock: OCR/gesture work can consume a sleep.
            -- Never start another swipe while the previous gesture's bounce animation is settling.
            if targetLeague and (not correctLeague or not garage.start) then
              alignFrames=0;alignKey=nil
              if not alignSwipeFrame or screen.frameId-alignSwipeFrame>=6000 then
                alignAttempts=alignAttempts+1;alignSwipeFrame=screen.frameId
                local tab=tabs[targetLeague]
                if tab then
                  print('切换并核验'..targetLeague..'车库');click(tab.x,tab.y);sleep(1200)
                else print('等待识别'..targetLeague..'段位标签');sleep(500) end
              else sleep(500) end
            elseif (not targetLeague or not tabs.selected) and ((not alignSwipeFrame and (world or garage.rank~=series)) or
                (not garage.start and (not alignSwipeFrame or screen.frameId-alignSwipeFrame>=6000))) then
              alignAttempts=alignAttempts+1;alignSwipeFrame=screen.frameId;alignFrames=0;alignKey=nil
              print('校验选车列表起点 '..alignAttempts..'/12 · '..(garage.key or '未确认'))
              swipe(419,300,419,1100,600);sleep(1800)
            elseif not alignSwipeFrame or screen.frameId-alignSwipeFrame>=3000 then
              if garage.start and garage.key~='' then
                if garage.key==alignKey then alignFrames=alignFrames+1 else alignFrames=1 end
                alignKey=garage.key
                if alignFrames>=2 then
                  carListAligned=true;verifiedKey=garage.key;position={offset=0,rank=garage.rank};print('已连续确认列表起点：'..garage.key)
                end
              else alignFrames=0;alignKey=nil end
              sleep(500)
            else sleep(500) end
          else sleep(500) end
        elseif not reusePage and (not correctLeague or not garage.start or garage.key~=verifiedKey) then
          originRetries=originRetries+1
          if originRetries>3 then error('选车列表起点反复无法确认，已停止，未选择车辆') end
          -- A missing league OCR result is not proof of movement. Discard authorization,
          -- then require two fresh origin observations again before using any ordinal.
          carListAligned=false;verifiedKey=nil;alignKey=nil;alignFrames=0
          alignFrame=screen.frameId;alignSwipeFrame=screen.frameId
          print('选车起点暂未确认，重新核验 '..originRetries..'/3');sleep(500)
        else
          local rankKey=reusePage and targetLeague or garage.rank or series
          local list=run.data['多人下用车'] or {'1'}
          if world then
            list=run.data[rankKey..'用车']
            if not list and rankKey=='青铜' then list={'1'} end
            if blankCarList(list) then list={} end
          end
          local configured=0;local exhausted=0;local chosen=false
          usedCars[rankKey]=usedCars[rankKey] or {}
          for pass=1,(rotate and 2 or 1) do
          configured=0;exhausted=0
          for entry,raw in ipairs(list) do
            configured=configured+1;local index=tonumber(raw)
            if not index or index<1 or index%1~=0 then error('车辆序号无效，请核对配置') end
            local card
            if not emptyCars[index] and (not rotate or not usedCars[rankKey][entry]) then
              data['信息']='正在定位配置车辆'
              card,screen=locateCar(screen,index,position)
              changed=tickCount()
              garage=screen.garage
            end
            if emptyCars[index] then exhausted=exhausted+1
            elseif card then
                if card.availability=='empty' or card.availability=='locked' or card.availability=='starUp' then
                  emptyCars[index]=true;exhausted=exhausted+1
                  if card.availability=='starUp' then print('车辆 '..index..' 待升星，尝试下一辆配置车辆')
                  elseif not world and card.availability=='empty' then print('车辆 '..index..' 无油，尝试下一辆配置车辆') end
                elseif card.availability=='ready' then
                  usedCars[rankKey][entry]=true
                  lastPage={rank=rankKey,index=index,offset=position.offset,cards=garage.cards}
                  selectedCar=card;carPressedAt=tickCount();chosen=true
                  if world then print('世界赛选车段位：'..rankKey) end
                  status('选择车辆序号 '..index..(world and '：已解锁且有油' or '：可出赛'))
                  click(card.x,card.y);sleep(1800);break
                end
            end
            if card and index>6 and card.availability=='unknown' then
              error('无法确认配置车辆 '..index..' 的燃油；请核对车辆序号')
            end
            if chosen then break end
          end
          if chosen or configured==exhausted then break end
          if rotate then usedCars[rankKey]={} end
          end
          if not chosen then
            carRetries=carRetries+1
            if configured==exhausted and (world or configured>0) then
              local lower=world and lowerConfiguredLeague(rankKey)
              if not lower then
                local reason=world and '当前及更低段位均无配置车可出赛' or '已配置车辆均不可出赛'
                status(reason..'，跳过本轮'..series..'；保留已完成局数');return 'fuelEmpty'
              end
              status(rankKey..(configured==0 and '未配置车辆' or '配置车辆均不可出赛')..'，改用'..lower..'配置')
              targetLeague=lower;emptyCars={};carRetries=0;originRetries=0
              carListAligned=false;verifiedKey=nil;alignKey=nil;alignFrames=0;alignAttempts=0
              alignFrame=screen.frameId;alignSwipeFrame=nil
            elseif carRetries>=6 then error('当前可见车辆中没有已配置且有燃油的车辆；请核对车辆序号') end
            sleep(500)
          end
        end
      elseif scene=='car' and not started and not selectedCar then
        if unverifiedCarBackAt then
          if tickCount()-unverifiedCarBackAt>10000 then error('未校验车辆详情无法返回选择列表，已停止') end
          sleep(500)
        else
          unverifiedCarBacks=unverifiedCarBacks+1
          if unverifiedCarBacks>3 then error('反复进入未校验车辆详情，已停止') end
          unverifiedCarBackAt=tickCount()
          carListAligned=false;alignAttempts=0;alignKey=nil;alignFrames=0
          alignSwipeFrame=nil;alignFrame=nil;verifiedKey=nil
          pageOffset=nil;pageLeft=nil;pageFrame=nil
          status('发现未校验车辆详情，返回列表重新核对车辆序号')
          if screen.homeButton then keyPress('back') else click(38,29) end
          sleep(1200)
        end
      elseif scene=='car' and not started then
        if screen.fuel==0 or screen.starUpPending then
          if screen.frameId~=emptyDetailFrame then emptyDetailFrames=emptyDetailFrames+1;emptyDetailFrame=screen.frameId end
          if emptyDetailFrames>=2 then
            emptyCars[selectedCar.index]=true
            status('车辆 '..selectedCar.index..(screen.starUpPending and ' 待升星' or ' 详情确认无油')..'，返回列表尝试下一辆')
            if screen.homeButton then keyPress('back') else click(38,29) end
            sleep(1200)
            carPressedAt=nil;selectedCar=nil
            emptyDetailFrames=0;emptyDetailFrame=nil;carListAligned=false;alignAttempts=0
            alignSwipeFrame=nil;alignFrame=nil;alignFrames=0;verifiedKey=nil
          else sleep(200) end
        else
          emptyDetailFrames=0;emptyDetailFrame=nil
          if cmpColor(60,830,'12FBC3-303030',1)~=1 then
            status('开启 TouchDrive');click(853,670);sleep(1200)
          elseif press(screen,'开始',550) then
            started=true;changed=tickCount();print('第 '..selectedCar.index..' 辆已点击开始，等待匹配')
          end
        end
      elseif scene=='race' then
        if not raced then
          serverErrors=0;noOpponents=0
          raced=true
          local missingRoute=route and not routeAvailable
          fallback=missingRoute or (mode=='3' and not route)
          _sessionRaceBegin(route or '',fallback)
          if fallback then
            raceMode='1'
            fallbackReason=missingRoute and ('已识别地图：'.._routeDisplay(route)..'，暂无有效跑法，左划兜底')
              or '地图识别失败，左划兜底'
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
        if postPress(screen,'继续',530,'bonusReward') or postPress(screen,'下一步',530,'bonusReward') then changed=tickCount()
        elseif has(screen.text,'点击任意位置') then click(640,550);sleep(1200) end
      elseif scene=='unknown' then
        -- Recover from a known non-racing detail page once, then let recognition settle.
        if not started and has(screen.text,'新手狂欢礼') then keyPress('back');sleep(1200) end
      end
      sleep(350)
    end
  end
end
