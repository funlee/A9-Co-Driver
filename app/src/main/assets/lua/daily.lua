dailyCup = {}
local config = {
  cupInfo = "612|80|4F00CF-201040,614|54|5F2200-501909,620|1242|000000-101010",
  noTicket = "538|1120|5400FF-290909,550|960|2C190C-101010,286|856|5400FF-101010",
  tips = "190|808|FFFFFF-101010,190|912|12FBC3-101010"
}
function dailyCup.dismissProfile(screen,readScreen)
  if screen.scene~='profileUnlock' then return false end
  readScreen=readScreen or _soloEventUi
  local since=tickCount();local lastTap=-10000;local tries=0
  while tickCount()-since<15000 do
    if screen.scene~='profileUnlock' then return true end
    if tickCount()-lastTap>=4000 and tries<3 then
      for _,w in ipairs(screen.words or {}) do
        local label=string.gsub(string.gsub(w.text,'%s',''),'稍後','稍后')
        if label=='稍后查査看' or label=='稍后查着' then label='稍后查看' end
        if label=='稍后查看' and w.x>=150 and w.x<640 and w.y>=500 and w.y<=720 then
          data['信息']='档案头像已解锁：稍后查看';print(data['信息'])
          tap(719-math.floor(w.y),math.floor(w.x));tries=tries+1;lastTap=tickCount();break
        end
      end
    end
    sleep(500);screen=readScreen()
  end
  error('档案头像弹窗未确认关闭，保留当前任务')
end
function dailyCup.skipReason()
  return _dailyTaskStatus and _dailyTaskStatus() or ''
end
-- Club rewards have no Race button. Restore the solo tab after claiming them,
-- including when a previous run stopped on the already-claimed club reward page.
function dailyCup.returnToSolo(screen)
  if screen.scene~='clubRewards' then return false end
  local since=tickCount();local lastTap=-10000;local tries=0
  local claimed=false
  local claim=run.data['是否领取俱乐部奖励']=='0'
  while tickCount()-since<15000 do
    if screen.scene=='event' or screen.scene=='dailyRewardOnly' then return true,screen end
    if screen.scene=='clubRewards' and tickCount()-lastTap>=4000 and tries<3 then
      if claim and not claimed then
        for _,w in ipairs(screen.words or {}) do
          if w.text:gsub('%s','')=='领取' and w.x>=200 and w.x<=610 and w.y>=420 and w.y<=625 then
            tap(719-w.y,w.x);claimed=true;lastTap=tickCount();break
          end
        end
      end
      for _,w in ipairs(screen.words or {}) do
        if tickCount()-lastTap>=4000 and string.gsub(w.text,'%s','')=='单人奖励' and w.x>640 and w.y>100 and w.y<260 then
          data['信息']='返回每日战利品单人奖励页'
          tap(719-math.floor(w.y),math.floor(w.x));tries=tries+1;lastTap=tickCount();break
        end
      end
    elseif screen.scene=='cards' and claimed then
      for _,w in ipairs(screen.words or {}) do
        local label=w.text:gsub('%s','')
        if w.y>=550 and ((label=='继续' and w.x>=900) or label=='点击任意位置跳过') then
          tap(719-w.y,w.x);break
        end
      end
    end
    sleep(500);screen=_soloEventUi()
  end
  error('每日战利品未确认返回单人奖励页，保留当前任务')
end
-- Loading and settlement differ from multiplayer; neither rank nor XP pages are mandatory.
function dailyCup.confirmChallenge(screen)
  if run.challengeConfirmed or not screen or
      (screen.scene~='unknown' and screen.scene~='car' and screen.scene~='event') then return end
  for _,word in ipairs(screen.words or {}) do
    local label=word.text:gsub('%s',''):gsub('戰','战')
    if (label=='仍然挑战' or label=='任然挑战') and word.x>=240 and word.x<=1160 and word.y>=390 then
      run.challengeConfirmed=true
      print('已确认仍然挑战')
      tap(719-math.floor(word.y),math.floor(word.x));sleep(1200)
      return
    end
  end
end

function dailyCup.readMap(readRoute)
  readRoute=readRoute~=false
  if _racePanel then _racePanel(false) end
  if readRoute then data["多人地图"]="";data["线路"]="" end
  local candidate=nil;local frames=0;local chosen=nil
  local tipsFrames=0
  for i=1,60 do
    local screen=_soloEventUi()
    if screen.scene=="race" then break end
    if screen.scene=="loading" then
      if not readRoute then break end
      if screen.route and screen.route==candidate then frames=frames+1 else candidate=screen.route;frames=screen.route and 1 or 0 end
      if frames>=2 and chosen~=candidate then
        chosen=candidate;utils.getMapName(chosen)
        print("每日战利品地图连续确认："..data["多人地图"])
      end
    end
    dailyCup.confirmChallenge(screen)
    keepCapture()
    if cmpColorEx(config.tips,1)==1 and not run.challengeConfirmed then tipsFrames=tipsFrames+1 else tipsFrames=0 end
    if cmpColorEx(config.noTicket,1)==1 or tipsFrames>=3 then
      if _racePanel then _racePanel(false) end
      break
    end
    if data["超时状态"] then return -1 end
    sleep(200)
  end
  if readRoute and not chosen then print("每日战利品地图未确认，使用左划兜底") end
end
local function press(screen,label,delay)
  local exit=screen.rewardExitButton
  if exit and exit.label==label and exit.x>850 and exit.y>590 then
    tap(719-math.floor(exit.y),math.floor(exit.x))
    if label=="错失机会" then print("结算：已点击错失机会") end
    sleep(delay)
    return true
  end
  if screen.scene=='rewards' then return false end
  for _,w in ipairs(screen.words or {}) do
    local text=string.gsub(w.text,"%s","")
    if label=="继续" and text=="继绩" then text="继续" end
    if text==label and w.x>750 and w.y>540 then
      tap(719-math.floor(w.y),math.floor(w.x));sleep(delay);return true
    end
  end
  return false
end
local function packSkipHint(screen)
  for _,word in ipairs(screen.words or {}) do
    local text=word.text:gsub('[%s|]',''):gsub('　','')
    if word.y>=550 and (text=='点击任意区域可跳过' or text=='点击任意位置跳过') then
      return word
    end
  end
end
function dailyCup.settle(label,readScreen)
  label=label or '每日战利品'
  readScreen=readScreen or _soloEventUi
  if _racePanel then _racePanel(false) end
  data["信息"]=label.."结算中"
  local special=label=='特殊赛事'
  local daily=label=='每日战利品' or (label=='杯赛' and run and run.dailyLoot)
  local started=tickCount();local rewardSince=nil;local seen=false;local eventFrames=0;local eventSince=nil;local lastEvent=nil
  local returnedScene,returnedFrame
  local pressedFrame,pressedScene,pressedButton
  local pressTries=0
  local skipFrame,skipAt
  local skipTries=0
  local function advance(screen,button)
    local frame=screen.frameId
    local sameButton=screen.scene==pressedScene and button==pressedButton
    -- The ad offer replaces Continue on the same rewards page.
    local newSkip=screen.scene=='rewards' and button=='错失机会' and not sameButton
    if frame and pressedFrame then
      local elapsed=frame-pressedFrame
      if elapsed<=0 or (not newSkip and (elapsed<3000 or (screen.scene==pressedScene and elapsed<8000))) then
        return true
      end
    end
    if not sameButton then pressTries=0 end
    if pressTries>=3 then error(label..'结算按钮点击后未切页，保留当前任务') end
    if not press(screen,button,special and 350 or 800) then return false end
    pressedFrame,pressedScene,pressedButton=frame,screen.scene,button
    pressTries=pressTries+1
    if screen.scene=='cards' then skipFrame,skipAt,skipTries=nil,nil,0 end
    return true
  end
  while tickCount()-started<90000 do
    local screen=readScreen();local scene=screen.scene
    -- The same lobby label can be missed after settlement. Keep the color fallback.
    if (label=='每日战利品' or label=='杯赛') and scene=='unknown' and not screen.closeX then
      keepCapture()
      if cmpColorEx(config.cupInfo,1)==1 then scene='event' end
    end
    if scene=="event" or scene=="car" or (special and scene=='specialRanking')
        or (daily and scene=='dailyRewardOnly') then
      if special and scene~=returnedScene then eventFrames=0;eventSince=nil end
      if not screen.frameId or screen.frameId~=returnedFrame then eventFrames=eventFrames+1 end
      returnedScene=scene;returnedFrame=screen.frameId
      eventSince=eventSince or tickCount()
      lastEvent=tickCount()
      -- Hall-of-Fame cards may appear just after the event lobby returns.
      local grace=special and scene=='specialRanking' and screen.frameId and 1500 or 3000
      if seen and eventFrames>=2 and tickCount()-eventSince>=grace then print(label.."结算完成，已返回赛事");return 0 end
    else
      if scene~="unknown" or screen.closeX or not lastEvent then
        eventFrames=0;eventSince=nil
      end
    end
    if scene=="profileUnlock" then
      dailyCup.dismissProfile(screen,readScreen)
    elseif scene=="clubRewards" then
      dailyCup.returnToSolo(screen)
    elseif scene=="results" then
      seen=true;advance(screen,"继续")
    elseif scene=="rewards" then
      seen=true;rewardSince=rewardSince or tickCount()
      local exit=screen.rewardExitButton
      if screen.rewardCanContinue and exit and (exit.label=='错失机会' or exit.label=='继续') then
        advance(screen,exit.label)
      elseif tickCount()-rewardSince>=60000 then error(label.."收益结算超时") end
    elseif scene=="cards" or scene=="missionRewards" then
      seen=true
      local hint=label=='杯赛' and not daily and scene=='cards' and packSkipHint(screen)
      if hint then
        local frame=screen.frameId
        if frame and (not pressedFrame or frame>pressedFrame) and skipTries<2
            and (not skipFrame or frame>skipFrame and tickCount()-skipAt>=1000) then
          tap(719-math.floor(hint.y),math.floor(hint.x))
          skipFrame,skipAt=frame,tickCount()
          skipTries=skipTries+1
          print('杯赛奖励开包：已点击跳过提示 '..hint.text)
          sleep(800)
        end
      elseif not skipFrame or screen.frameId~=skipFrame then
        advance(screen,"继续")
      end
    elseif scene=="conversion" then
      seen=true;advance(screen,"确定")
    elseif scene=="vehicleUnlock" and screen.unlockLater then
      seen=true;print('车辆提示：稍后查看')
      tap(719-math.floor(screen.unlockLater.y),math.floor(screen.unlockLater.x));sleep(800)
    elseif seen and scene=="unknown" and (screen.text or ''):find('里程碑',1,true) then
      -- Club milestones interrupt settlement. Defer the claim to stay in the race flow.
      for _,word in ipairs(screen.words or {}) do
        local label=word.text:gsub('%s',''):gsub('領','领')
        if label=='稍后领取' and word.x>=230 and word.x<=630 and word.y>=495 and word.y<=690 then
          if not pressedFrame or not screen.frameId or screen.frameId-pressedFrame>=3000 then
            tap(719-word.y,word.x)
            print('俱乐部里程碑：已点击稍后领取')
            pressedFrame=screen.frameId
            sleep(800)
          end
          break
        end
      end
    elseif scene=="unknown" and screen.closeX then
      -- Optional promotions/season-pass dialogs after a race: close only the detected X.
      tap(719-math.floor(screen.closeX.y),math.floor(screen.closeX.x));sleep(800)
    end
    if data["超时状态"] then error(label.."结算超时") end
    sleep(300)
  end
  error(label.."未确认返回赛事，已停止")
end

return dailyCup
