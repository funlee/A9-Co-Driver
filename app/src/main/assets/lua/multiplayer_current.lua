-- Current CN series cards and vehicle layout. Coordinates below are upright 1280x720.
-- OCR guards each menu action; race commands still use the existing route engine.
currentMultiplayer={}
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
function currentMultiplayer.run()
  local count=tonumber(run.data['次数']) or 1
  local completed=_sessionProject(data['项目'],count) or 0
  for _,key in ipairs({'选车筛选'}) do
    if utils.defultCfg(run.data[key],'')~='' then error('新版多人菜单尚未适配“'..key..'”，请关闭此选项后运行') end
  end
  local rotate=tostring(utils.defultCfg(run.data['用车模式'],'0'))=='1'
  local usedCars={}
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
    local lastScene='';local changed=tickCount();local started=false;local seriesSelected=false
    local lastPulse=0;local carRetries=0;local carListAligned=false
    local identityRetries=0;local selectedEntry=nil;local selectedRank=nil
    local eventPressedAt=nil;local carPressedAt=nil;local selectedCar=nil;local detailFrames=0;local detailFrame=nil
    local returnHomeAt=nil;local emptyCars={};local emptyDetailFrames=0;local emptyDetailFrame=nil
    local originRetries=0;local alignAttempts=0;local alignKey=nil;local alignFrames=0;local alignFrame=nil;local alignSwipeFrame=nil;local verifiedKey=nil
    local rewardChoice=nil;local rewardAt=0;local rewardTries=0;local dialogMisses=0
    local scoreBefore,scoreAfter,scoreSample,scoreFrame,scoreSince
    local rewardSeenAt,rewardConfirmedAt,rewardObservedCard,rewardObservedAt
    local rewardHandled=false
    local returnFrame=nil;local returnFrames=0
    local postActionKey=nil;local postActionFrame=nil;local postActionTries=0
    local function postPress(screen,label,minY,phase)
      if not raced then return press(screen,label,minY) end
      local button=word(screen,label,minY)
      if not button then return false end
      local frame=screen.frameId or tickCount()
      local key=phase..':'..label
      -- OCR and tap dispatch can outlive a page animation. Never repeatedly tap
      -- Continue through a stale reward image into the next menu's Start button.
      if postActionFrame and (frame-postActionFrame<3000 or
          (key==postActionKey and frame-postActionFrame<8000)) then
        data['信息']='已点击结算按钮，等待切页';sleep(500);return true
      end
      if key~=postActionKey then postActionTries=0 end
      if postActionTries>=3 then error('结算按钮多次点击后页面仍未切换，已停止避免误点') end
      postActionKey=key;postActionFrame=frame;postActionTries=postActionTries+1
      click(button.x,button.y);sleep(1200);return true
    end
    while true do
      local screen=_multiplayerUi(readSeriesScore,started or raced)
      local scene=screen.scene
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
        if not resultSeen then resultSeen=true;status(settlementStatus(screen)) end
      end
      if started and not raced and scene=='unknown' then
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
        status('多人流程：'..scene..' · '..(total+1)..'/'..count)
        print('界面文字：'..screen.text)
        lastScene=scene;changed=tickCount()
      end
      -- The host handles the timeout while matching is confirmed.
      if scene~='matching' and tickCount()-changed>120000 then error('多人界面等待超时：'..scene..'，请查看诊断截图') end
      if scene=='profileUnlock' then
        dailyCup.dismissProfile(screen,_multiplayerUi);changed=tickCount()
      elseif scene=='vehicleUnlock' then
        -- Highest priority: never dispatch a close-X, Back, Continue or unlock action here.
        if screen.unlockLater and screen.unlockLater.x<640 and screen.unlockLater.y>=500 then
          unlockMisses=0;status('发现新车可解锁，仅点击稍后查看')
          click(screen.unlockLater.x,screen.unlockLater.y);sleep(1200);changed=tickCount()
        else
          unlockMisses=unlockMisses+1;status('新车解锁保护：等待确认稍后查看')
          if unlockMisses>=3 then error('新车提示未能确认稍后查看；已停止，请由玩家处理，绝不自动解锁车辆') end
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
          status('比赛收益已记录，返回赛事')
          if postPress(screen,screen.rewardExit=='错失机会' and '错失机会' or '继续',590,'raceRewards') and not rewardExitAt then rewardExitAt=tickCount() end
          changed=tickCount()
        elseif tickCount()-(rewardExitAt or rewardsSince)>=20000 then
          error('比赛奖励页面未完成结算或继续按钮不可用，请检查画面')
        else status('等待结算完成并识别本局收益');sleep(400) end
      elseif raced and not rewardsDone and (has(screen.text,'总金币') or (rewardsSince and (scene=='unknown' or scene=='result'))) then
        rewardsSince=rewardsSince or tickCount()
        if tickCount()-rewardsSince>=20000 then error('比赛奖励页面未完成结算或继续按钮不可用，请检查画面') end
        status('等待奖励文字稳定，暂不退出结算');sleep(400)
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
        seriesSelected=press(screen,series)
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
            seriesSelected=true
            scoreBefore=screen.seriesScore
            if readSeriesScore then print('世界赛赛前系列赛分数：'..tostring(scoreBefore or '未识别')) end
            eventPressedAt=tickCount()
            click(start.x,start.y);sleep(1200)
            status('已确认赛事：'..series..'，开始已点击一次，等待切页')
          end
        end
      elseif scene=='cars' then
        local garage=screen.garage or {}
        if carPressedAt then
          if tickCount()-carPressedAt>30000 then error('选车后未能确认车辆详情，已停止避免重复点击') end
          data['信息']='等待所选车辆详情';sleep(500)
        elseif not carListAligned then
          if alignAttempts>=12 then error('无法确认选车列表起点，已停止，未选择车辆') end
          if screen.frameId and screen.frameId~=alignFrame then
            alignFrame=screen.frameId
            -- Use capture time, not the Lua scheduler clock: OCR/gesture work can consume a sleep.
            -- Never start another swipe while the previous gesture's bounce animation is settling.
            if not alignSwipeFrame or (not garage.start and screen.frameId-alignSwipeFrame>=6000) then
              alignAttempts=alignAttempts+1;alignSwipeFrame=screen.frameId;alignFrames=0;alignKey=nil
              status('校验选车列表起点 '..alignAttempts..'/12 · '..(garage.key or '未确认'))
              swipe(419,300,419,1100,600);sleep(1800)
            elseif screen.frameId-alignSwipeFrame>=3000 then
              if garage.start and garage.key~='' then
                if garage.key==alignKey then alignFrames=alignFrames+1 else alignFrames=1 end
                alignKey=garage.key
                if alignFrames>=2 then
                  carListAligned=true;verifiedKey=garage.key;status('已连续确认列表起点：'..garage.key)
                end
              else alignFrames=0;alignKey=nil end
              sleep(500)
            else sleep(500) end
          else sleep(500) end
        elseif not garage.start or garage.key~=verifiedKey then
          originRetries=originRetries+1
          if originRetries>3 then error('选车列表起点反复无法确认，已停止，未选择车辆') end
          -- A missing league OCR result is not proof of movement. Discard authorization,
          -- then require two fresh origin observations again before using any ordinal.
          carListAligned=false;verifiedKey=nil;alignKey=nil;alignFrames=0
          alignFrame=screen.frameId;alignSwipeFrame=screen.frameId
          status('选车起点暂未确认，重新核验 '..originRetries..'/3');sleep(500)
        else
          local list=data['项目']=='多人下' and (run.data['多人下用车'] or {'1'}) or run.data[(garage.rank or '')..'用车'] or {'1'}
          local configured=0;local exhausted=0;local chosen=false
          local rankKey=garage.rank or series
          usedCars[rankKey]=usedCars[rankKey] or {}
          for pass=1,(rotate and 2 or 1) do
          configured=0;exhausted=0
          for entry,raw in ipairs(list) do
            configured=configured+1;local index=tonumber(raw)
            for _,card in ipairs(garage.cards or {}) do
              if card.index==index then
                if card.fuel==0 or emptyCars[index] then exhausted=exhausted+1
                elseif card.fuel and card.fuel>0 and card.capacity>=card.fuel and (not rotate or not usedCars[rankKey][entry]) then
                  usedCars[rankKey][entry]=true
                  selectedEntry=entry;selectedRank=rankKey
                  selectedCar=card;carPressedAt=tickCount();chosen=true
                  status('选择车辆序号 '..index..'：'..table.concat(card.tokens,' ')..'，性能 '..card.performance..'，燃油 '..card.fuel..'/'..card.capacity)
                  click(card.x,card.y);sleep(1800);break
                end
              end
            end
            if chosen then break end
          end
          if chosen or configured==exhausted then break end
          if rotate then usedCars[rankKey]={} end
          end
          if not chosen then
            carRetries=carRetries+1
            if configured>0 and configured==exhausted then
              status('已配置车辆全部无油，跳过本轮'..series..'；保留已完成局数');return 'fuelEmpty'
            elseif carRetries>=6 then error('当前可见车辆中没有已配置且有燃油的车辆；请核对车辆序号') end
            sleep(500)
          end
        end
      elseif scene=='car' and not started then
        if not selectedCar then error('未经过列表起点及序号校验，禁止直接开始比赛') end
        local detail=screen.carDetail or {};local sameName=false
        for _,token in ipairs(selectedCar.tokens) do
          if string.find(detail.name or '',token,1,true) then sameName=true end
        end
        if screen.fuel==0 and sameName and detail.performance==selectedCar.performance then
          if screen.frameId~=emptyDetailFrame then emptyDetailFrames=emptyDetailFrames+1;emptyDetailFrame=screen.frameId end
          if emptyDetailFrames>=2 then
            emptyCars[selectedCar.index]=true
            status('车辆 '..selectedCar.index..' 详情确认无油，返回列表尝试下一辆')
            if screen.homeButton then keyPress('back') else click(38,29) end
            sleep(2000)
            carPressedAt=nil;selectedCar=nil;detailFrames=0;detailFrame=nil
            emptyDetailFrames=0;emptyDetailFrame=nil;carListAligned=false;alignAttempts=0
            alignSwipeFrame=nil;alignFrame=nil;alignFrames=0;verifiedKey=nil
          else sleep(500) end
        elseif sameName and detail.performance==selectedCar.performance then
          emptyDetailFrames=0;emptyDetailFrame=nil
          if screen.frameId and screen.frameId~=detailFrame then detailFrames=detailFrames+1;detailFrame=screen.frameId end
          if detailFrames>=2 then
            if cmpColor(60,830,'12FBC3-303030',1)~=1 then
              status('开启 TouchDrive');click(853,670);sleep(1200)
            elseif press(screen,'开始',550) then started=true;changed=tickCount();status('车辆身份已核对，等待匹配与地图识别') end
          end
        else
          detailFrames=0
          if tickCount()-carPressedAt>15000 then
            if identityRetries>=2 then error('车辆详情重新识别后仍与所选车辆不一致或无法确认，已停止，未开始比赛') end
            identityRetries=identityRetries+1
            status('车辆详情文字未确认，返回列表重新核验 '..identityRetries..'/2')
            -- Re-open the same configured entry rather than relaxing identity checks
            -- or consuming the once-per-car rotation before a race has started.
            if selectedRank and selectedEntry then usedCars[selectedRank][selectedEntry]=nil end
            if screen.homeButton then keyPress('back') else click(38,29) end
            sleep(2000)
            carPressedAt=nil;selectedCar=nil;selectedEntry=nil;selectedRank=nil
            detailFrames=0;detailFrame=nil;emptyDetailFrames=0;emptyDetailFrame=nil
            carListAligned=false;alignAttempts=0;alignSwipeFrame=nil;alignFrame=nil
            alignFrames=0;alignKey=nil;verifiedKey=nil;changed=tickCount()
          else data['信息']='正在核对所选车型与性能分';sleep(500) end
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
