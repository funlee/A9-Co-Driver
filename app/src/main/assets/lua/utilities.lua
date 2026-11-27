utils = {}
function utils.getPkgName()
  local pkg = UIconfig.gamePkg
  local list
  if pkg == "" then
    pkg = "kybc"
    list = getInstalledApk()
  else
    return pkg
  end
  local i = 1
  while true do
    if string.find(list[i], pkg, 1) ~= nil then
      pkg = list[i]
      break
    elseif i == #list then
      toast("未找到游戏包名，将自动查找")
      sleep(2000)
      pkg = "kybc"
      i = 0
    end
    i = i + 1
  end
  return pkg
end
function utils.stopGame()
  data["信息"] = "关闭游戏"
  local pkg = UIconfig.gamePkg
  if pkg == "" then
    pkg = utils.getPkgName()
  end
  stopApp(pkg)
  toast(pkg)
  sleep(1000)
  return pkg
end
function utils.rebootGame()
  data.isAuto = false
  local pkg = utils.stopGame()
  data["信息"] = "重启游戏"
  runApp(pkg)
  local times = 10
  if string.find(pkg, "vivo", 1) ~= nil then
    times = 30
  end
  for t = 1, times do
    sleep(1000)
  end
  local _ue7bbb4e68aa4e4b8ad = "118|117|513512-050505,45|134|625858-050505,178|474|494141-050505,242|639|453D3D-050505"
  for i = 1, 100 do
    keepCapture()
    if cmpColorEx("702|1252|FFFFFF-101010,690|1252|3B2312-101010,670|1270|3B2312-101010", 1) == 1 then
      sleep(2000)
      utils.backToMain()
      data["超时状态"] = false
      return
    end
    if cmpColorEx("480|512|1A1007-101010,234|525|FFFFFF-101010,226|794|613111-101010", 1) == 1 then
      tap(240, 600)
      sleep(3000)
    end
    if cmpColorEx("552|836|5B5A58-090909,579|982|4D5054-090909,584|924|474344-090909", 0.9) == 1 then
      tap(640, 996)
    end
    if cmpColorEx("214|238|210900-101010,650|641|EDEDED-101010,697|597|4200CC-101010", 1) == 1 then
      tap(211, 234)
      sleep(2000)
      tap(56, 832)
      sleep(2000)
    end
    if string.find(pkg, "mi", 1) ~= nil and cmpColorEx("220|274|FFFFFF-090909,244|808|FFFFFF-090909,464|405|FFFFFF-090909", 1) == 1 then
      tap(172, 382)
      sleep(3000)
    end
    if string.find(pkg, "mz", 1) ~= nil and cmpColorEx("569|279|391F19-101010,576|867|381F19-101010,193|562|492D1D-101010", 1) == 1 then
      tap(182, 586)
      sleep(3000)
    end
    if string.find(pkg, "samsung", 1) ~= nil then
      local ret, x, y = _findColorTriple(10, 502, 614, 1076, "FE8103-101010", 0, 1.0)
      if x ~= -1 then
        tap(x, y)
        sleep(3000)
      end
    end
    if string.find(pkg, "huawei", 1) ~= nil and cmpColorEx("119|121|2C1E04-101010,563|959|AAAAAA-101010,560|1062|ACACAD-101010", 1) == 1 then
      tap(135, 956)
      sleep(300)
      tap(558, 1062)
      tap(558, 1062)
      sleep(3000)
    end
    if string.find(pkg, "bilibili", 1) ~= nil and cmpColorEx("282|710|EABD4F-090909,268|633|EABD4F-090909,330|645|EABD4F-090909", 1) == 1 then
      tap(282, 710)
      sleep(3000)
    end
    if string.find(pkg, "kybc.aligames", 1) ~= nil then
      if cmpColorEx("114|116|DB9714-090909,74|586|FC7519-090909,62|694|4788FB-090909", 1) == 1 then
        tap(176, 400)
        sleep(1000)
        tap(540, 600)
        sleep(3000)
      elseif cmpColorEx("114|116|DB9714-101010,214|370|FFFFFF-101010,481|717|3264F9-101010,512|369|FFFFFF-101010", 1) == 1 then
        tap(342, 608)
        sleep(3000)
      end
    end
    if string.find(pkg, "kybc.tap", 1) ~= nil and cmpColorEx("611|562|FF6A3F-101010,204|391|FF890F-101010,597|448|FFFFFF-101010", 1) == 1 then
      tap(100, 410)
      sleep(1000)
      tap(200, 516)
      sleep(3000)
    end
    if cmpColorEx("290|287|424242-101010,349|497|424242-101010,326|829|424242-101010", 1) == 1 then
      tap(316, 778)
      sleep(3000)
    end
    if cmpColorEx(_ue7bbb4e68aa4e4b8ad, 1) == 1 then
      (requirePlayerAction or error)('游戏维护中，任务暂停')
    end
    if cmpColorEx("689|1251|543E30-050505,678|1251|DAD5D5-050505,701|1252|FFFFFF-050505,690|1262|F4F4F4-101010", 1) == 1 then
      utils.stopGame()
      tap(1, 1)
      sleep(10000)
      utils.rebootGame()
      return
    end
    toast("启动游戏中")
    if 10 < i then
      if getDisplayRotate() == 1 then
        if cmpColorEx("340|591|4200CC-090909,318|610|FCFCFC-090909,167|571|BBBCBF-090909,101|608|4E00EE-090909", 1) == 0 then
          keyPress("back")
        end
      else
        keyPress("home")
      end
      runApp(pkg)
      sleep(3000)
    end
    sleep(3000)
  end
  releaseCapture()
end
function utils.findApkList(pkg)
  local ret = exec("pm list packages -e")
  ret = splitStr(ret, "\n")
  for i = 1, #ret do
    if string.find(ret[i], pkg, 1) ~= nil then
      return true
    end
  end
  return false
end
function utils.getInstallPkg()
  local ret = exec("pm list packages -e")
  ret = string.gsub(ret, "package:", "")
  ret = splitStr(ret, "\n")
  return ret
end
function utils.mycolor(px, py, color)
  local ret, x, y = -1, -1, -1
  keepCapture()
  ret, x, y = _findColorTriple(px - 2, py - 2, px + 2, py + 2, color, 1, 1)
  releaseCapture()
  if x ~= -1 then
    return 1
  else
    return 0
  end
end
function utils.findMultiStr(src, dst)
  local len = #dst
  local times = 0
  for i = 1, len do
    if string.find(src, dst[i], 1) ~= nil then
      times = times + 1
    end
  end
  if times == len then
    return 1
  else
    return 0
  end
end
function utils.transStr(str)
  if str == "" then
    return {0}
  end
  if string.find(str, "，", 1) ~= nil then
    str = string.gsub(str, "，", ",")
  end
  if utf8.right(str, 1) == "," then
    str = utf8.strCut(str, #str, 1)
  end
  if string.find(str, ",", 1) ~= nil then
    return splitStr(str, ",")
  else
    return {str}
  end
end
function utils.defaultConfig(config, default)
  if config == "" then
    return default
  end
  return config
end
function utils.defultCfg(cfg, def)
  return cfg or def
end
function utils.checkScreen()
  local w, h
  w, h = getDisplaySize()
  if w ~= 720 and h ~= 1280 then
    for i = 0, 6 do
      toast("设备分辨率不符合要求，看群文件说明，改分辨率")
      sleep(1000)
    end
    exitScript()
  end
end
local function findUiWord(screen,labels,minX,maxX,minY,maxY)
  for _,word in ipairs(screen.words or {}) do
    local label=word.text:gsub('[%s|]',''):gsub('賽','赛')
    if labels[label] and word.x>=minX and word.x<=maxX and word.y>=minY and word.y<=maxY then return word end
  end
end
local function tapUiWord(word)
  tap(719-math.floor(word.y),math.floor(word.x))
end
local function startButton(screen)
  for _,word in ipairs(screen.words or {}) do
    local label=word.text:gsub('%s','')
    if label:sub(1,#'开始')=='开始' and word.x>950 and word.y>590 then return word end
  end
end
function utils.specialMissionContinue(screen)
  local ranking=screen.scene=='specialRanking'
  if not ranking and screen.scene~='missionRewards' then return end
  local stage=tonumber(run.data['特殊赛事阶段']) or 1
  -- Keep OCR line boundaries: the next reward count can start with a digit.
  local digits=(screen.stageLabel or ''):match('阶%s*段%s*(%d+)')
  if not digits then
    for _,word in ipairs(screen.words or {}) do
      if word.x<=(ranking and 360 or 230) and word.y>=(ranking and 65 or 140) and word.y<=420 then
        local badge=word.text:gsub('[%s|]','')
        digits=badge:match('^阶段(%d+)$') or badge:match('^1阶段(%d+)1$')
        if digits then break end
      end
    end
  end
  if tonumber(digits)~=stage then return end
  if ranking then return startButton(screen) end
  return findUiWord(screen,{['继续']=true,['继绩']=true},900,1265,570,710)
end
function utils.specialStageList(screen)
  if run.data['称霸模式']~='0' or screen.scene~='unknown' then return false end
  for _,word in ipairs(screen.words or {}) do
    if word.x<=370 and word.text:gsub('[ \t\r\n]',''):match('^章节%d+$') then return true end
  end
  return false
end
function utils.continueSpecialMission(screen)
  local button=utils.specialMissionContinue(screen)
  if not button then return false end
  data['信息']='特殊赛事：继续进入配置阶段'
  print(data['信息']);tapUiWord(button)
  local deadline=tickCount()+15000
  repeat
    sleep(250)
    local fresh=_soloEventUi()
    if fresh.scene=='car' or fresh.scene=='cars' or fresh.scene=='noTicket' or fresh.scene=='race'
        or fresh.scene~=screen.scene and utils.specialMissionContinue(fresh)
        or spCup and spCup.tutorialButton(fresh) then return true end
  until tickCount()>=deadline
  error('特殊赛事点击阶段后未确认切页，保留当前任务')
end
function utils.dismissExitDialog(screen)
  local stay=findUiWord(screen,{['再玩一会']=true,['再玩一会儿']=true},300,710,370,640)
  if not stay then error('未确认退出提示上的「再玩一会」，请手动关闭后续跑') end
  if _taskNotice then _taskNotice('关闭退出提示：再玩一会',2500) end
  tapUiWord(stay)
  sleep(1);sleep(1000)
end
local function dismissDriveTutorial(screen)
  if screen.scene~='unknown' then return false end
  local text=(screen.text or ''):gsub('[ \t\r\n]',''):gsub('堤','提')
  if not text:find('大师操作模式',1,true) or not text:find('不再提示',1,true) then return false end
  local button=findUiWord(screen,{['继续']=true,['确定']=true},950,1265,580,710)
  if not button then return false end
  print('已识别驾驶模式说明，继续返回车辆页面')
  tapUiWord(button);sleep(1);sleep(1500)
  return true
end
function utils.openHomeTab(screen,name)
  local daily=name=='daily'
  local function dailyButton(s)
    if s.dailyExplore then return s.dailyExplore end
    local title=(s.text or ''):gsub('%s',''):gsub('賽','赛')
    if title:find('限时赛事',1,true) or title:find('挑战限时',1,true) then
      local explore=findUiWord(s,{['探索赛事']=true,['採索赛事']=true,['采索赛事']=true,['探索骞事']=true},720,1265,330,585)
      return explore or findUiWord(s,{['领取']=true},850,1265,420,580)
    end
  end
  local labels=daily and {['每日']=true,['每日赛事']=true} or {['赛季']=true,['赛季赛事']=true,['赛事']=true}
  local button=daily and dailyButton(screen)
  local opened=false
  if button then
    data['信息']='已点击每日赛事入口，等待赛事列表'
    print(data['信息'])
    tapUiWord(button)
    opened=true
  else
    local tab
    for attempt=1,5 do
      tab=findUiWord(screen,labels,daily and 420 or 180,daily and 650 or 465,580,715)
      if tab then break end
      if attempt<5 then sleep(1);sleep(450);screen=_soloEventUi() end
    end
    if not tab and not daily and screen.scene=='home' then
      local pass=findUiWord(screen,{['传奇']=true,['通行证']=true,['传奇通行证']=true},50,235,580,715)
      local dailyTab=findUiWord(screen,{['每日']=true,['每日赛事']=true},440,650,580,715)
      if pass and dailyTab then tab={x=340,y=640} end
    end
    if not tab then error('未识别要进入的赛事标签，请确认游戏处于主菜单后续跑') end
    data['信息']=daily and '正在打开每日赛事' or '正在打开赛季赛事'
    tapUiWord(tab)
  end
  sleep(1);sleep(1300)
  if not daily then return end

  local afterHome=0
  local dismissedExit=false
  for attempt=1,12 do
    local fresh=_soloEventUi()
    if fresh.scene=='exitGame' then
      afterHome=0
      if not dismissedExit then
        utils.dismissExitDialog(fresh)
        dismissedExit=true;opened=false
        print('每日赛事入口的退出提示已关闭，重新核对入口')
      end
    elseif fresh.scene=='home' then
      afterHome=0
      button=not opened and dailyButton(fresh)
      if button then
        data['信息']='已点击每日赛事入口，等待赛事列表'
        print(data['信息'])
        tapUiWord(button)
        opened=true
      end
    else
      afterHome=afterHome+1
      if afterHome>=2 then return end
    end
    sleep(1);sleep(600)
  end
  error('点击每日赛事后仍在主菜单，请确认赛事入口后续跑')
end
function utils.backToMain(strict,acceptCupList)
  data["信息"] = "返回游戏主界面"
  local dismissedExit=false
  local triedHome=false
  local closedPopup=false
  local driveTutorialPages=0
  local triedRankingHome=false
  local dismissedDemotion=false
  local garageBacks={}
  local lastFrame,previousEvidence=-1,0
  local previousPopup,popupClicks=nil,0
  local lastBack=-4000
  local attempts=0
  local navigationDeadline=0
  local rewardTapAt=-10000
  local rewardTaps=0
  local rewardFrame=nil
  local backs=0
  local deadline=tickCount()+90000
  while (strict or attempts<25 or backs<10) and tickCount()<deadline do
    local screen
    if strict then screen=_recoveryUi() else screen=_soloEventUi() end
    if screen.restartScreen then _requestGameRestart('returnScreen') end
    if acceptCupList and not strict and screen.scene=='cupClaimSpotlight' then return screen.scene end
    local text=(screen.text or ''):gsub('%s','')
    local loading=screen.scene=='unknown' and
      (text:find('登录中',1,true) or text:find('加载中',1,true) or text:find('检查游戏更新',1,true)
        or text:find('健康游戏忠告',1,true) or text:find('适龄提示',1,true))
    if strict and screen.scene=='loading' then loading=true end
    if not loading and tickCount()>=navigationDeadline then attempts=attempts+1 end
    local seriesMenu=text:gsub('賽','赛')
    local onMultiplayerTab=screen.scene=='unknown' and seriesMenu:find('世界系列赛',1,true)
      and seriesMenu:find('大师系列赛',1,true) and seriesMenu:find('对决',1,true)
    local fresh=screen.frameId and screen.frameId>lastFrame
    local evidence=screen.homeEvidence or 0
    if loading or screen.dismissPopup or screen.returnOverlay or screen.legacyPopup
        or screen.scene~='unknown' and screen.scene~='home' and screen.scene~='series' then evidence=0 end
    if strict and fresh then
      lastFrame=screen.frameId
      if (evidence & previousEvidence)~=0 then
        toast("到达游戏主界面")
        return true
      end
      previousEvidence=evidence
    elseif not strict and not screen.homeButton and (screen.scene=='home' or onMultiplayerTab) then
      toast("到达游戏主界面")
      return
    end
    if screen.scene=='rewardChoice' then (requirePlayerAction or error)('请先手动选择画面上的动态奖励，再续跑') end
    if strict and screen.scene=='loginHistory' then (requirePlayerAction or error)('登录历史需要玩家确认，请检查登录状态') end
    if screen.scene~='interruptingPopup' then previousPopup=nil end
    if strict and (not fresh or evidence~=0) then
      -- A candidate menu or repeated projection frame must not receive input.
      sleep(250)
    elseif loading then
      data['信息']='等待游戏登录与菜单加载'
      sleep(700)
    elseif screen.scene=='exitGame' then
      if not dismissedExit then
        utils.dismissExitDialog(screen)
        dismissedExit=true
      else sleep(500) end
    elseif strict and screen.legacyPopup and not screen.dismissPopup then
      if tickCount()>=navigationDeadline and _legacyPopupDismiss(screen.legacyPopup) then
        navigationDeadline=tickCount()+850
      end
      sleep(350)
    elseif strict and screen.returnOverlay and not screen.dismissPopup then
      local action=screen.returnOverlay
      if tickCount()>=navigationDeadline and (action~='back' or tickCount()-lastBack>=4000) then
        if action=='back' then keyPress('back');lastBack=tickCount()
        elseif action=='continue' then tap(33,1080)
        elseif action=='ad' then tap(600,1080)
        elseif action=='gray' then tap(317,774) end
        navigationDeadline=tickCount()+(action=='continue' and 300 or action=='back' and 4000 or 3000)
      end
      sleep(300)
    elseif strict and screen.scene=='interruptingPopup' then
      local action=screen.dismissPopup
      if tickCount()>=navigationDeadline then
        if action and popupClicks<5 then
          if previousPopup and action.label==previousPopup.label and math.abs(action.x-previousPopup.x)<=5
              and math.abs(action.y-previousPopup.y)<=5 then
            if _taskNotice then _taskNotice('关闭游戏弹窗：'..action.label,2500) end
            tapUiWord(action);popupClicks=popupClicks+1;previousPopup=nil
            navigationDeadline=tickCount()+1200
          else previousPopup=action end
        elseif tickCount()-lastBack>=4000 then
          previousPopup=nil
          keyPress('back');lastBack=tickCount();navigationDeadline=lastBack+4000
        end
      end
      sleep(300)
    elseif screen.scene=='rewards' and screen.rewardPage then
      local button=screen.rewardExitButton
      if screen.rewardCanContinue and button and (button.label=='继续' or button.label=='错失机会')
        and button.x>850 and button.y>590 and rewardTaps<3 and tickCount()-rewardTapAt>=4000
        and (not rewardFrame or screen.frameId~=rewardFrame) then
        data['信息']='恢复已完成比赛的奖励页'
        tapUiWord(button);rewardTapAt=tickCount();rewardFrame=screen.frameId;rewardTaps=rewardTaps+1
      end
      sleep(500)
    elseif screen.scene=='specialRanking' and rewardTaps>0 and not triedRankingHome then
      triedRankingHome=true;triedHome=true
      print('特殊赛事奖励已退出，从排行榜返回主界面')
      if screen.homeButton then tapUiWord(screen.homeButton) else tap(690,1244) end
      sleep(1);navigationDeadline=tickCount()+3000;sleep(400)
    elseif screen.scene=='unknown' and rewardTaps>0 and tickCount()-rewardTapAt<12000 then
      -- Do not spend the Home or Back attempt on a reward-page transition.
      sleep(500)
    elseif driveTutorialPages<2 and dismissDriveTutorial(screen) then
      driveTutorialPages=driveTutorialPages+1
    elseif screen.scene=='leagueDemotion' then
      if screen.demotionConfirm and not dismissedDemotion then
        dismissedDemotion=true
        print('已确认世界赛降级提示，继续恢复任务')
        tapUiWord(screen.demotionConfirm)
      end
      sleep(1200)
    elseif screen.scene=='profileUnlock' and dailyCup and dailyCup.dismissProfile then
      dailyCup.dismissProfile(screen)
    elseif screen.scene=='vehicleUnlock' then
      if screen.unlockLater then
        print('车辆提示：稍后查看')
        tapUiWord(screen.unlockLater)
      end
      sleep(800)
    elseif screen.scene=='limitedSalePromo' then
      if screen.closeX then tapUiWord(screen.closeX) end
      sleep(1200)
    else
      if not strict then keepCapture() end
      if not strict and not screen.homeButton and cmpColorEx("690|1240|FFFFFF-101010,690|1252|3B2312-101010,670|1270|3B2312-101010",1)==1 then
        releaseCapture()
        toast("到达游戏主界面")
        return
      elseif dismissedExit then
        -- A cancelled exit dialog must be followed by a confirmed home frame.
        sleep(600)
      elseif tickCount()<navigationDeadline then
        -- Look for Home while the page is moving. Keep the original window
        -- before sending another navigation gesture.
        sleep(400)
      elseif screen.scene=='garageFilter' or screen.scene=='vehicleUpgrade' or screen.scene=='cars'
          or (screen.scene=='car' or screen.scene=='lockedCar') and not screen.homeButton then
        if not garageBacks[screen.scene] then
          garageBacks[screen.scene]=true
          if screen.scene=='garageFilter' then keyPress('back');lastBack=tickCount() else tap(690,38) end
          sleep(1);navigationDeadline=tickCount()+2000
        end
        sleep(400)
      elseif screen.scene=='race' or not strict and cmpColorEx("680|50|FFFFFF,660|70|FFFFFF",1)==1 then
        tap(670, 60)
        sleep(2000)
        tap(80, 1100)
        sleep(3000)
      elseif screen.closeX and not closedPopup then
        closedPopup=true
        tapUiWord(screen.closeX)
        sleep(1200)
      elseif not triedHome and (not strict or not screen.panelVisible) then
        triedHome=true
        if screen.homeButton then tapUiWord(screen.homeButton) else tap(690,1244) end
        sleep(1)
        navigationDeadline=tickCount()+(strict and 4000 or 3000)
        sleep(400)
      elseif tickCount()-lastBack>=4000 then
        keyPress("back")
        backs=backs+1
        lastBack=tickCount()
        sleep(1)
        navigationDeadline=tickCount()+(strict and 4000 or 2000)
        sleep(400)
      else sleep(600) end
      if not strict then releaseCapture() end
    end
    if not strict and data["超时状态"] then break end
  end
  error('未能确认游戏主菜单，请打开游戏主菜单后续跑')
end
-- 说明类整页覆盖（如“新游戏机制”）：底部有「不再提示」且右下角有开始/继续时，点它继续。
function utils.proceedTipPage(screen)
  -- 活动广告（如「限时赛事 更新提醒」）会盖在加载中的赛事列表上，此时 scene 是 loading，
  -- 只认 unknown 会让任务在加载页空转到超时。
  if not screen or (screen.scene~='unknown' and screen.scene~='loading') or screen.closeX then return false end
  local text=(screen.text or ''):gsub('%s','')
  if not (text:find('新游戏机制',1,true) or text:find('新玩法机制',1,true)
      or text:find('不再提示',1,true) or text:find('不再顯示',1,true)
      or text:find('不再显示',1,true)) then return false end
  -- 确认按钮的文案不统一：说明书是「开始/继续/确定」，活动广告是「明白了！」。
  local labels={['开始']=true,['继续']=true,['确定']=true,['確定']=true,
    ['明白了']=true,['明白']=true,['知道了']=true,['我知道了']=true,['好的']=true}
  -- 全角标点是多字节，不能放进字符类（会按单字节误删汉字），只能整串替换。
  local function label(word) return (word.text:gsub('[%s!]',''):gsub('！',''):gsub('。','')) end
  local button
  for _,word in ipairs(screen.words or {}) do
    if word.x>950 and word.y>590 and labels[label(word)] then
      button=word
      break
    end
  end
  if not button then return false end
  if utils.tipPageFrame==screen.frameId then return false end
  utils.tipPageFrame=screen.frameId
  data['信息']='继续游戏说明页'
  tapUiWord(button)
  sleep(1200)
  return true
end
function utils.mySwipe(x1, y1, x2, y2)
  swipe(x1, y1, x2, y2, 300)
  touchDown(1, x2, y2)
  sleep(80)
  touchMove(1, x2, y2)
  sleep(10)
  touchUp(1)
  sleep(10)
end
function utils.mySwipe1(x1, y1, x2, y2, t)
  swipe(x1, y1, x2, y2, 200)
  touchDown(1, x2, y2)
  touchMove(1, x2, y2)
  sleep(t)
  touchUp(1)
end
function utils.sort(arr)
  local ret = 0
  for i = 1, #arr do
    if ret < arr[i] then
      ret = arr[i]
    end
  end
  return ret
end
function utils.checkGame()
  data["信息"] = "检查游戏是否启动"
  if getDisplayRotate() == 0 and string.find(frontAppName(), "kybc", 1) == nil then
    utils.rebootGame()
    data["心跳时间"] = os.time()
    data["超时状态"] = false
  end
end
function utils.fileExist(path)
  local file = io.open(path, "rb")
  if file then
    file:close()
  end
  return file ~= nil
end
function utils.file(path)
  local f = io.open(path, "r")
  local ff = f:read("*a")
  f:close()
  local ret = splitStr(ff, "\r\n")
  return ret
end
function utils.checkAuto()
  if data.isAuto or UIconfig.checkAuto == "" then
    return
  end
  data["信息"] = "检测游戏操作"
  tap(669, 1229)
  sleep(3000)
  tap(543, 634)
  sleep(3000)
  tap(289, 413)
  sleep(3000)
  tap(529, 758)
  sleep(3000)
  utils.backToMain()
  data.isAuto = true
end
function utils.checkJson(str)
  if type(str) ~= "string" then
    return false
  end
  str = str:match("^%s*(.-)%s*$")
  if str:sub(1, 1) == "{" and str:sub(-1) == "}" or str:sub(1, 1) == "[" and str:sub(-1) == "]" then
    return true
  end
  return false
end
function utils.wait(c)
  for ci = 1, 100 do
    keepCapture()
    if cmpColorEx(c, 1) == 1 then
      sleep(1000)
      break
    end
    if data["超时状态"] then
      return
    end
    sleep(500)
  end
end
function utils.getTicketNum()
  local ticket
  if cmpColor(618, 1120, "FFFFFF-101010", 1) == 1 then
    ticket = myocr.smartOcrEx(600, 1130, 623, 1150)
  else
    ticket = myocr.smartOcrEx(600, 1116, 623, 1150)
  end
  ticket = tonumber(ticket)
  if ticket == nil then
    ticket = 0
  end
  print(ticket)
  return ticket
end
function utils.checkRank()
  data["信息"] = "检查段位情况"
  local info = {
    "6085D8-101010",
    "D4B296-101010",
    "30CBF1-101010",
    "F86593-101010",
    "FFF66A-101010",
    "A4E2F5-101010"
  }
  local rank = {
    "青铜",
    "白银",
    "黄金",
    "铂金",
    "钻石",
    "传奇"
  }
  if data["项目"] == 2 then
    info = {
      "6085D8-101010",
      "D4B296-101010",
      "30CBF1-101010",
      "F86593-101010",
      "A4E2F5-101010"
    }
    rank = {
      "青铜",
      "白银",
      "黄金",
      "铂金",
      "传奇"
    }
  end
  data["段位"] = 1
  for i = 1, #rank do
    if utils.mycolor(376, 400, info[i]) == 1 then
      data["段位"] = i
      toast(rank[i])
      break
    elseif i == #rank then
      toast("无段位")
    end
  end
end
function utils.getRankNum()
  tap(350, 610)
  for j = 1, 30 do
    if cmpColor(208, 1010, "5400FF-101010", 1) == 1 then
      sleep(1000)
      break
    end
    sleep(100)
  end
  local rank = 0
  if data["项目"] == 1 then
    rank = myocr.smartOcrEx(260, 980, 291, 1064)
  elseif data["项目"] == 2 then
    rank = myocr.smartOcrEx(250, 930, 2801, 1034)
  end
  rank = string.gsub(rank, ",", "")
  rank = tonumber(rank)
  print(rank)
  tap(580, 1090)
  sleep(1000)
  return rank
end
function utils.freePkg(n)
  data["信息"] = "检查免费包"
  if UIconfig.freePkg ~= "0" or n <= 0 then
    return 0
  end
  sleep(500)
  data["信息"] = "检查免费包"
  if utils.mycolor(664, 1100, "4DF2FF-291909") == 1 then
    print("领取免费包")
    tap(708, 1100)
    sleep(5000)
    for i = 0, 50 do
      keepCapture()
      if cmpColorEx("551|370|5400FF-101010,549|354|5400FF-101010,565|353|5400FF-101010,552|379|361904-101010", 1) == 1 then
        sleep(3000)
        break
      elseif cmpColorEx("475|555|5400FF-101010,500|581|5400FF-101010,337|639|12FBC3-101010,111|644|12FBC3-101010", 1) == 1 then
        utils.backToMain()
        return 0
      end
      sleep(500)
    end
    keepCapture()
    local ret, x, y = _findColorTriple(300, 254, 600, 254, "00D8FF-101010", 0, 1.0)
    if x ~= -1 and y ~= -1 then
      tap(x - 20, y)
      sleep(1000)
    end
    keepCapture()
    local ret, x, y = _findColorTriple(120, 280, 120, 700, "FFFFFF-050505", 2, 1.0)
    if x ~= -1 and y ~= -1 and 400 < y then
      utils.mySwipe(510, y, 510, 300)
      sleep(1000)
    end
    keepCapture()
    x, y = findMultiColor(147, 342, 516, 1086, "260300-101010", "-136|261|8E5900-101010|-131|556|AF7100-101010|-89|568|704100-101010", 0, 1)
    if x ~= -1 and y ~= -1 then
      if UIconfig.coinPkg == "0" then
        keepCapture()
        ret, x, y = _findColorTriple(118, 960, 118, 1000, "12FBC3-050505", 2, 1.0)
        if x ~= -1 and y ~= -1 then
          tap(x, y)
          sleep(2000)
          keepCapture()
          if cmpColorEx("286|123|BA7A00-101010,423|157|502300-101010,284|161|985D00-101010,73|565|391D07-101010", 1) == 1 then
            toast("开金币零件包")
            for i = 1, 5 do
              tap(58, 627)
              sleep(500)
              tap(58, 627)
            end
            for i = 1, 15 do
              tap(30, 1240)
              sleep(500)
            end
          else
            tap(610, 1160)
            sleep(2000)
          end
        end
      end
      for i = 0, 4 do
        keepCapture()
        ret, x, y = _findColorTriple(110, 280, 110, 1200, "00D8FF-020903", 0, 1)
        if x ~= -1 and y ~= -1 then
          tap(x, y + 2)
          sleep(2000)
          keepCapture()
          if cmpColorEx("420|155|240300-101010,380|203|311200-101010,319|189|240700-101010,293|136|1E090B-101010", 1) == 1 or cmpColorEx("414|155|4D2200-101010,298|127|8E5D01-101010,305|183|CC8D00-101010,377|198|9C6400-101010", 1) == 1 then
            tap(58, 630)
            for i = 1, 15 do
              tap(50, 1074)
              sleep(500)
            end
          else
            tap(610, 1160)
            sleep(2000)
          end
          toast("开免费包完成")
        end
        sleep(1000)
      end
      releaseCapture()
      utils.backToMain()
      return 1
    end
    utils.backToMain()
  elseif utils.freePkg(n - 1) == 1 then
  end
  return 0
end
function utils.getToken()
  if UIconfig.autoToken == "" then
    return 0
  end
  data["信息"] = "检查每日任务代币"
  local ret = false
  keepCapture()
  if cmpColorEx("679|436|000000-101010,692|428|000000-101010,670|568|3C2313-101010", 1) == 1 then
    tap(692, 440)
    toast("自动领取代币")
    sleep(3000)
    if utils.mycolor(600, 320, "341904-101010") == 0 then
      tap(600, 320)
      sleep(1000)
    end
    swipe(50, 600, 380, 600, 100)
    swipe(50, 600, 380, 600, 100)
    sleep(1000)
    for ci = 1, 3 do
      for j = 0, 3 do
        keepCapture()
        local ret, x, y = _findColorTriple(50, 830, 400, 830, "00E0FD-101010", 0, 1.0)
        if x ~= -1 and utils.mycolor(608, 1226, "5400FF-101010") == 1 then
          tap(x + 5, y)
          sleep(1000)
          for c = 1, 15 do
            tap(50, 1087)
            sleep(200)
          end
          if ci == 1 then
            swipe(50, 600, 380, 600, 100)
            swipe(50, 600, 380, 600, 100)
          else
            swipe(380, 600, 50, 600, 100)
            swipe(380, 600, 50, 600, 100)
          end
          sleep(2000)
        end
      end
      swipe(380, 960, 50, 960, 100)
      sleep(2000)
    end
    if utils.mycolor(500, 1190, "561F04-101010") == 0 then
      tap(500, 1190)
      for i = 1, 15 do
        tap(52, 1230)
        sleep(500)
        if data["超时状态"] or utils.mycolor(608, 1226, "5400FF-101010") == 0 then
          releaseCapture()
          break
        end
      end
    end
    utils.backToMain()
    ret = true
  end
  releaseCapture()
  return ret
end
function utils.getCoupons()
  if UIconfig.Coupons == "" then
    return 0
  end
  data["信息"] = "检查免费点券"
  local canDo = false
  for i = 1, 5 do
    if utils.mycolor(664, 1100, "4DF2FF-291909") == 1 then
      canDo = true
      break
    end
    sleep(200)
  end
  if canDo then
    for i = 1, 8 do
      tap(690, 560)
      sleep(500)
    end
    for i = 0, 30 do
      keepCapture()
      if cmpColorEx("475|555|5400FF-101010,500|581|5400FF-101010,337|639|12FBC3-101010,111|644|12FBC3-101010", 1) == 1 then
        sleep(1000)
        break
      end
      sleep(500)
    end
    for i = 1, 8 do
      keepCapture()
      local x, y = findMultiColor(46, 262, 613, 698, "5400FF-090909", "30|-249|000000-090909|-352|-241|00D8FF-490900", 0, 1)
      if x ~= -1 then
        tap(100, y - 100)
        sleep(1000)
        utils.backToMain()
        return true
      end
      toast("检测点券")
      sleep(500)
      if data["超时状态"] then
        return false
      end
    end
  end
  releaseCapture()
  data["下次点券时间"] = os.time() + 3600
  utils.backToMain()
  return false
end
function utils.getSpPrize(spLocation)
  if UIconfig.spPrize == "" or os.time() < data["下次大奖赛时间"] then
    return 0
  end
  data["信息"] = "检查大奖赛包"
  keepCapture()
  if cmpColorEx("702|1252|FFFFFF-101010,690|1252|3B2312-101010,670|1270|3B2312-101010", 1) == 1 then
  else
    utils.backToMain()
  end
  for i = 1, 2 do
    keepCapture()
    if cmpColor(88, 300, "FFFFFF-101010", 1) == 0 then
      tap(88, 300)
      sleep(1000)
    end
  end
  for i = 1, 8 do
    utils.mySwipe(550, 406, 200, 406)
  end
  sleep(2000)
  local swipes = function(n)
    local dx = 0
    for i = 1, n do
      dx = 0
      keepCapture()
      local ret, x, y = _findColorTriple(330, 200, 560, 200, "360A17-090909", 0, 1.0)
      if x ~= -1 then
        dx = 425 - x
        print(dx, x)
      end
      utils.mySwipe1(180, 406, 430 + dx, 406, 200)
      sleep(500)
    end
  end
  local spPos = tonumber(spLocation) or 1
  if spPos == 1 then
    for i = 1, 3 do
      tap(500, 406)
      sleep(200)
    end
  elseif spPos == 2 then
    for i = 1, 3 do
      tap(364, 406)
      sleep(200)
    end
  else
    swipes(spPos - 2)
    keepCapture()
    local ret, x, y = _findColorTriple(180, 200, 400, 200, "360A17-090909", 0, 1.0)
    local dx = 320
    if 260 < x then
      dx = x - 10
    end
    for i = 1, 3 do
      tap(dx, 406)
      sleep(200)
    end
  end
  tap(60, 300)
  sleep(3000)
  keepCapture()
  if cmpColor(70, 406, "4F4F4F-050505", 1) == 1 or cmpColorEx("617|1155|FFFFFF-090909,600|1165|0FA1F4-090909,600|282|3B3337-090909", 1) == 0 then
    toast("无大奖赛包")
    utils.backToMain()
    return 0
  end
  tap(80, 530)
  sleep(1000)
  for i = 1, 20 do
    keepCapture()
    if cmpColor(44, 1238, "12FBC3-050505", 1) == 0 then
      tap(44, 1238)
    end
    sleep(500)
  end
  data["下次大奖赛时间"] = os.time() + 3600
  utils.backToMain()
  return 1
end
function utils.coinAd()
  data["信息"] = "看金币广告"
  while true do
    keepCapture()
    if cmpColorEx("207|976|FFFFFF-101010,169|69|00C6FF-101010,205|927|000000-101010", 1) == 1 then
      data["心跳时间"] = os.time()
      tap(170, 958)
      for i = UIconfig.AdTime, 1, -1 do
        keepCapture()
        if cmpColorEx("260|580|BABABA-101010,260|690|DB8E26-101010", 1) == 1 then
          tap(260, 690)
        elseif cmpColorEx("60|342|FFFFFF-101010,224|280|FFFFFF-101010,600|580|000000-101010", 1) == 1 then
          return 0
        end
        toast("看金币广告，等待" .. i .. "秒")
        sleep(1000)
      end
      releaseCapture()
    else
      toast("无金币广告")
      data["心跳时间"] = os.time()
      data["超时状态"] = false
      return 0
    end
    sleep(1000)
    for k = 1, 60 do
      local canBack = false
      local ret = utils.screenFreeze(false)
      sleep(1000)
      keepCapture()
      local r, x, y = _findColorTriple(113, 70, 395, 105, "00C6FF-101010", 0, 1.0)
      if x ~= -1 or cmpColorEx("169|68|00C6FF-101010,445|69|00C6FF-101010", 1) == 1 then
        return 0
      end
      if ret == 1 then
        ret = utils.screenFreeze(true)
        if ret == 0 then
          canBack = true
          print("可以返回关闭广告")
          sleep(3000)
        end
        sleep(3000)
        if canBack == false then
          print("尝试点击关闭广告")
          tap(656, 1214)
          sleep(500)
          tap(670, 1180)
          tap(644, 76)
          sleep(500)
          tap(644, 76)
          sleep(5000)
        end
        for n = 1, 10 do
          keepCapture()
          local r, x, y = _findColorTriple(113, 70, 395, 105, "00C6FF-101010", 0, 1.0)
          if x ~= -1 or cmpColorEx("169|68|00C6FF-101010,445|69|00C6FF-101010", 1) == 1 then
            print("广告结束")
            data["心跳时间"] = os.time()
            data["超时状态"] = false
            return 0
          elseif cmpColorEx("200|574|BABABA-101010,200|668|D98926-101010", 1) == 1 then
            tap(200, 668)
            sleep(1000)
          end
          sleep(500)
        end
      end
    end
  end
end
function utils.dangerAd()
  data["信息"] = "看危险度"
  while true do
    keepCapture()
    if cmpColorEx("216|346|3C00FF-101010,248|452|00BFFF-101010,230|780|FFFFFF", 1) == 1 then
      data["心跳时间"] = os.time()
      tap(220, 860)
      for i = 1, 5 do
        toast("如果看广告过程中出现异常，就不要勾选看广告")
        sleep(1000)
      end
      for i = UIconfig.AdTime, 1, -1 do
        keepCapture()
        if cmpColorEx("260|580|BABABA-101010,260|690|DB8E26-101010", 1) == 1 then
          tap(260, 690)
        elseif cmpColorEx("216|346|3C00FF-101010,248|452|00BFFF-101010,230|780|FFFFFF", 1) == 1 then
          tap(220, 860)
        end
        toast("看危险度，等待" .. i .. "秒")
        sleep(1000)
      end
      releaseCapture()
    elseif cmpColorEx("678|1245|010101-101010,690|1245|E9DC00-101010,690|1040|FFFFFF-101010", 1) == 1 then
      print("无广告")
      data["超时状态"] = false
      data["心跳时间"] = os.time()
      return 0
    elseif cmpColorEx("240|732|000000-101010,252|277|00BFFF-101010,237|779|00BFFF-101010", 1) == 1 then
      print("票满了")
      data["心跳时间"] = os.time()
      data["超时状态"] = false
      return 1
    end
    sleep(1000)
    for k = 1, 60 do
      local canBack = false
      local ret = utils.screenFreeze(false)
      sleep(1000)
      keepCapture()
      if cmpColorEx("678|1250|3C2313-101010,690|1250|E9E2DD-101010,670|1250|3C2313-101010", 1) == 1 then
        return 0
      end
      if ret == 1 then
        ret = utils.screenFreeze(true)
        if ret == 0 then
          canBack = true
          print("可以返回关闭广告")
          sleep(3000)
        end
        sleep(3000)
        if canBack == false then
          print("尝试点击关闭广告")
          tap(656, 1214)
          sleep(500)
          tap(670, 1180)
          tap(644, 76)
          sleep(500)
          tap(644, 76)
          sleep(5000)
        end
        for n = 1, 10 do
          keepCapture()
          if cmpColorEx("240|732|000000-101010,252|277|00BFFF-101010,237|779|00BFFF-101010", 1) == 1 then
            print("危险度满了")
            data["心跳时间"] = os.time()
            data["超时状态"] = false
            return 1
          elseif cmpColorEx("216|346|3C00FF-101010,248|452|00BFFF-101010,230|780|FFFFFF", 1) == 1 then
            print("可以看广告")
            break
          elseif cmpColorEx("678|1245|010101-101010,690|1245|E9DC00-101010,690|1040|FFFFFF-101010", 1) == 1 then
            print("没广告")
            return 0
          elseif cmpColorEx("200|574|BABABA-101010,200|668|D98926-101010", 1) == 1 then
            tap(200, 668)
            sleep(1000)
          end
          sleep(500)
        end
        keepCapture()
        if cmpColorEx("216|346|3C00FF-101010,248|452|00BFFF-101010,230|780|FFFFFF", 1) == 1 then
          print("可以看广告")
          break
        elseif cmpColorEx("240|732|000000-101010,252|277|00BFFF-101010,237|779|00BFFF-101010", 1) == 1 then
          print("票满了")
          data["心跳时间"] = os.time()
          data["超时状态"] = false
          return 1
        elseif cmpColorEx("678|1245|010101-101010,690|1245|E9DC00-101010,690|1040|FFFFFF-101010", 1) == 1 then
          print("不在广告界面")
          return 0
        end
      end
    end
  end
end
function utils.ticketAd()
  data["信息"] = "看广告票"
  while true do
    keepCapture()
    if cmpColorEx("546|1014|2B190C-101010,540|1184|5400FF-101010,180|860|FFFFFF-101010", 1) == 1 then
      data["心跳时间"] = os.time()
      tap(180, 860)
      for i = 1, 3 do
        toast("广告票每天只有10次，如果看广告过程中出现异常，就不要勾选看广告")
        sleep(1000)
      end
      for i = UIconfig.AdTime, 1, -1 do
        keepCapture()
        if cmpColorEx("260|580|BABABA-101010,260|690|DB8E26-101010", 1) == 1 then
          tap(260, 690)
        elseif cmpColorEx("546|1014|2B190C-101010,540|1184|5400FF-101010,180|860|FFFFFF-101010", 1) == 1 then
          tap(180, 860)
        elseif cmpColorEx("159|983|5400FF-101010,205|961|2F9FD4-101010,243|880|FFFFFF-101010", 1) == 1 then
          tap(180, 860)
          sleep(2000)
        elseif cmpColorEx("550|460|2B190C-101010,440|460|341C0C-101010", 1) == 1 then
          toast("票满了")
          data["心跳时间"] = os.time()
          data["超时状态"] = false
          return 1
        elseif cmpColorEx("546|1014|2B190C-101010,540|1184|5400FF-101010,180|860|5F3317-101010", 1) == 1 then
          toast("无广告")
          data["心跳时间"] = os.time()
          data["超时状态"] = false
          return 0
        end
        toast("看广告票，等待" .. i .. "秒")
        sleep(1000)
      end
      releaseCapture()
    elseif cmpColorEx("546|1014|2B190C-101010,540|1184|5400FF-101010,180|860|5F3317-101010", 1) == 1 then
      toast("无广告")
      data["心跳时间"] = os.time()
      data["超时状态"] = false
      return 0
    elseif cmpColorEx("550|460|2B190C-101010,440|460|341C0C-101010", 1) == 1 then
      toast("票满了")
      data["心跳时间"] = os.time()
      data["超时状态"] = false
      return 1
    end
    sleep(1000)
    for k = 1, 60 do
      local canBack = false
      local ret = utils.screenFreeze(false)
      sleep(1000)
      keepCapture()
      if cmpColorEx("678|1245|010101-101010,690|1245|E9DC00-101010,690|1040|FFFFFF-101010", 1) == 1 then
        return 0
      end
      if ret == 1 then
        ret = utils.screenFreeze(true)
        if ret == 0 then
          canBack = true
          print("可以返回关闭广告")
          sleep(3000)
        end
        sleep(3000)
        if canBack == false then
          print("尝试点击关闭广告")
          tap(656, 1214)
          sleep(500)
          tap(670, 1180)
          tap(644, 76)
          sleep(500)
          tap(644, 76)
          sleep(5000)
        end
        for n = 1, 10 do
          keepCapture()
          if cmpColorEx("550|460|2B190C-101010,440|460|341C0C-101010", 1) == 1 then
            print("票满了")
            data["心跳时间"] = os.time()
            data["超时状态"] = false
            return 1
          elseif cmpColorEx("546|1014|2B190C-101010,540|1184|5400FF-101010,180|860|FFFFFF-101010", 1) == 1 then
            print("可以看广告")
            break
          elseif cmpColorEx("546|1014|2B190C-101010,540|1184|5400FF-101010,180|860|5F3317-101010", 1) == 1 then
            print("没广告")
            return 0
          elseif cmpColorEx("200|574|BABABA-101010,200|668|D98926-101010", 1) == 1 then
            tap(200, 668)
            sleep(1000)
          end
          sleep(500)
        end
        keepCapture()
        if cmpColorEx("546|1014|2B190C-101010,540|1184|5400FF-101010,180|860|FFFFFF-101010", 1) == 1 then
          print("可以看广告")
          break
        elseif cmpColorEx("550|460|2B190C-101010,440|460|341C0C-101010", 1) == 1 then
          print("票满了")
          data["心跳时间"] = os.time()
          data["超时状态"] = false
          return 1
        elseif cmpColorEx("678|1245|010101-101010,690|1245|E9DC00-101010,690|1040|FFFFFF-101010", 1) == 1 then
          print("不在广告界面")
          return 0
        end
      end
    end
  end
end
function utils.carAd()
  data["信息"] = "看广告修车"
  if utils.mycolor(75, 1030, "FFFFFF-101010") == 1 then
    tap(80, 1200)
    for i = 1, 5 do
      toast("勾选修车广告后，只会使用第一辆车，如果想要按用车序列用车，就不要勾选看广告。")
      sleep(1000)
    end
    for i = 1, 60 do
      for j = UIconfig.AdTime, 1, -1 do
        keepCapture()
        if cmpColorEx("230|580|10FBC3-101010,230|700|FFFFFF-101010", 1) == 1 then
          data["心跳时间"] = os.time()
          tap(240, 714)
          sleep(2000)
        elseif UIconfig.onceAD ~= "" and cmpColorEx("476|1114|5400FF-101010,554|1040|1F0420-101010,479|948|2A190C-101010", 1) == 1 then
          tap(481, 1094)
          sleep(1500)
          tap(539, 1039)
          sleep(1500)
          return 1
        elseif cmpColorEx("676|1250|3C2313-030303,670|1244|3C2313-030303", 1) == 1 then
          data["心跳时间"] = os.time()
          if cmpColorEx("368|150|F9EF33,604|1170|FFFFFF-030303,100|270|FFFFFF-030303", 1) == 1 then
            data["超时状态"] = false
            data["心跳时间"] = os.time()
            return 1
          else
            data["超时状态"] = false
            data["心跳时间"] = os.time()
            return 0
          end
        elseif cmpColorEx("530|1060|5400FF-101010,514|824|2B190C-101010,238|712|733C19-101010", 1) == 1 then
          toast("没广告")
          tap(530, 1050)
          sleep(3000)
          data["心跳时间"] = os.time()
          data["超时状态"] = false
          return 0
        elseif cmpColorEx("476|1112|5400FF-101010,486|982|2B190C-101010", 1) == 1 then
          tap(476, 1112)
          sleep(3000)
          print("广告失败错误弹窗")
          return 0
        elseif cmpColorEx("200|574|BABABA-101010,200|668|D98926-101010", 1) == 1 then
          tap(200, 668)
          sleep(1000)
        end
        sleep(1000)
        toast("看广告修车，等待" .. j .. "秒")
      end
      for k = 1, 60 do
        local canBack = false
        local ret = utils.screenFreeze(false)
        keepCapture()
        if cmpColorEx("678|1250|3C2313-101010,690|1250|E9E2DD-101010,670|1250|3C2313-101010", 1) == 1 then
          data["心跳时间"] = os.time()
          data["超时状态"] = false
          return 0
        end
        sleep(1000)
        if ret == 1 then
          ret = utils.screenFreeze(true)
          if ret == 0 then
            canBack = true
          end
          if cmpColorEx("368|150|F9EF33,604|1170|FFFFFF-030303,100|270|FFFFFF-030303", 1) == 1 then
            print("看广告结束")
            data["心跳时间"] = os.time()
            data["超时状态"] = false
            return 1
          end
          sleep(3000)
          if canBack == false then
            print("尝试点击关闭广告")
            tap(656, 1214)
            sleep(500)
            tap(670, 1180)
            tap(644, 76)
            sleep(500)
            tap(644, 76)
            sleep(5000)
          end
          for n = 1, 10 do
            keepCapture()
            if cmpColorEx("368|150|F9EF33,604|1170|FFFFFF-030303,100|270|FFFFFF-030303", 1) == 1 then
              print("看广告结束")
              data["心跳时间"] = os.time()
              data["超时状态"] = false
              return 1
            else
              if cmpColorEx("230|580|10FBC3-101010,230|700|FFFFFF-101010", 1) == 1 then
                break
              end
              if cmpColorEx("530|1060|5400FF-101010,514|824|2B190C-101010,238|712|733C19-101010", 1) == 1 then
                toast("没广告")
                tap(506, 1012)
                sleep(3000)
                data["心跳时间"] = os.time()
                data["超时状态"] = false
                return 0
              elseif cmpColorEx("200|574|BABABA-101010,200|668|D98926-101010", 1) == 1 then
                tap(200, 668)
                sleep(1000)
              end
            end
            sleep(500)
          end
          if cmpColorEx("230|580|10FBC3-101010,230|700|FFFFFF-101010", 1) == 1 then
            break
          end
          if cmpColorEx("368|150|F9EF33,604|1170|FFFFFF-030303,100|270|FFFFFF-030303", 1) == 1 then
            print("看广告结束")
            data["心跳时间"] = os.time()
            data["超时状态"] = false
            return 1
          elseif cmpColorEx("676|1244|3C2313-101010,670|1244|3C2313-101010", 1) == 1 then
            print("不在广告界面")
            data["心跳时间"] = os.time()
            data["超时状态"] = false
            return 0
          end
        end
      end
      if UIconfig.onceAD ~= "" then
        toast("只看一次广告")
        sleep(1000)
        tap(520, 1000)
        sleep(2000)
        data["心跳时间"] = os.time()
        data["超时状态"] = false
        return 1
      end
    end
    data["心跳时间"] = os.time()
    data["超时状态"] = false
    return 0
  end
end
function utils.screenFreeze(isBack)
  local color = ""
  local colors = {}
  keepCapture()
  for i = 0, 9 do
    for j = 1, 10 do
      local gx, gy = j * 72, i * 128
      color = getPixelColor(gx, gy)
      color = utf8.right(color, 6)
      colors[i * 10 + j] = gx .. "|" .. gy .. "|" .. color .. "-050505"
    end
  end
  if isBack then
    keyPress("back")
  end
  sleep(3000)
  keepCapture()
  local count = 0
  for i = 1, 100 do
    if cmpColorEx(colors[i], 1) == 1 then
      count = count + 1
    end
  end
  releaseCapture()
  if 85 <= count then
    return 1
  else
    return 0
  end
end
local function ticketedTrial(screen)
  local project=data['项目']
  local label=(screen.text or ''):gsub('%s','')
  local trialText=label:find('免费试驾',1,true) or label:find('免費試駕',1,true)
  return screen.scene=='car' and screen.fuel==-1 and not screen.starUpPending
    and (trialText or (project=='特殊赛事' and screen.zeroTicketStart)) and startButton(screen)
end
local function verifiedDailyDetail(slot,detail)
  if not detail or detail.scene~='car' then return false end
  if slot.score and slot.score~='' and detail.soloScore==slot.score then return true end
  local model=slot.model or ''
  local name=(detail.soloName or ''):upper():gsub('[^A-Z0-9]','')
  if #model>=3 and name:sub(-#model)==model then return true end
  return slot.score and slot.score~='' and _soloDetailScore and _soloDetailScore()==slot.score
end

-- 比赛 HUD 左上角的 TouchDrive 开关：布局固定（一条指示带 + 开/关字形），
-- 状态优先按文字判断，文字认不出来再退到字形颜色（开=青柠、关=红）。
-- 车联这类从任务面板直接开赛的赛事没有开赛前选车页，只能在这里确认自动驾驶。
local TOUCHDRIVE_TAP = { 609, 90 }
local TOUCHDRIVE_GLYPH = { 595, 204, 614, 222 }
local function touchDriveWordState()
  if not _textWords then return nil end
  keepCapture()
  local ok, words = pcall(_textWords, 585, 20, 625, 235)
  if not ok or type(words) ~= "table" then return nil end
  for _, w in ipairs(words) do
    local text = tostring(w.text or ""):gsub("%s+", "")
    if text == "开" or text == "開" or text == "ON" or text == "On" then return "on" end
    if text == "关" or text == "關" or text == "OFF" or text == "Off" then return "off" end
  end
  return nil
end
local function touchDriveColorState()
  if not getPixelColor then return nil end
  keepCapture()
  local sawOff = false
  for cx = TOUCHDRIVE_GLYPH[1], TOUCHDRIVE_GLYPH[3], 2 do
    for cy = TOUCHDRIVE_GLYPH[2], TOUCHDRIVE_GLYPH[4], 2 do
      local hex = getPixelColor(cx, cy)
      if type(hex) == "string" and 6 <= #hex then
        local hexs = string.sub(hex, -6)
        local b = tonumber(string.sub(hexs, 1, 2), 16) or 0
        local g = tonumber(string.sub(hexs, 3, 4), 16) or 0
        local r = tonumber(string.sub(hexs, 5, 6), 16) or 0
        if 170 < g and 120 < r and b < 90 then return "on" end
        if 180 < r and g < 80 and b < 80 then sawOff = true end
      end
    end
  end
  if sawOff then return "off" end
  return nil
end
function utils.touchDriveState()
  local state = touchDriveWordState()
  if state then return state end
  return touchDriveColorState()
end
-- 开关是翻转语义，状态未识别时绝不盲点，避免把已开启的关掉。
function utils.ensureTouchDrive()
  local state = utils.touchDriveState()
  if state == "on" then
    print("TouchDrive 已开启")
    return true
  end
  if state ~= "off" then
    print("TouchDrive 状态未识别，保持原状")
    return false
  end
  print("TouchDrive 为关，点击开启")
  tap(TOUCHDRIVE_TAP[1], TOUCHDRIVE_TAP[2])
  sleep(900)
  state = utils.touchDriveState()
  print("点击后 TouchDrive 状态：" .. tostring(state or "未识别"))
  return state == "on"
end

function utils.startRace()
  -- Fuel is never OCR'd here: a white start button means the vehicle
  -- is locked or empty, and unrecognized frames are ignored. Fuel OCR stays a
  -- best-effort hint whose failure must not abort the start attempt.
  local readOk,fuel,latestCar=pcall(utils.carFuel)
  if not readOk then fuel,latestCar=nil,nil end
  if fuel=='vehicleLocked' then return fuel,latestCar end
  if fuel=='starUp' then
    print('车辆待升星，跳过本车')
    return 'fuelEmpty'
  end
  if fuel==0 then return 'fuelEmpty' end
  if fuel=='trial' then latestCar=nil end
  data['信息']='开始跑图'
  local driveTutorialPages=0
  local whiteFrames=0
  local loggedStartPixel=false
  for i=1,8 do
    local screen=latestCar or _soloEventUi()
    latestCar=nil
    if screen.scene=='lockedCar' then return 'vehicleLocked',screen end
    local colorReady=false
    if (screen.scene=='car' or screen.scene=='unknown') and not screen.closeX and cmpColorEx and cmpColor then
      keepCapture()
      colorReady=cmpColorEx('368|150|F9EF33-101010,434|150|F9EF33-101010,236|142|F9EF33-101010',1)==1
        and cmpColor(134,1184,'5400FF-101010',1)==0
      if not loggedStartPixel then
        loggedStartPixel=true
        print('开始按钮像素(75,1170)：'..tostring(getPixelColor and getPixelColor(75,1170) or '不可用'))
      end
      -- Rule: a white start button means the vehicle is locked or
      -- out of fuel. Two consecutive reads debounce transition flashes.
      if cmpColor(75,1170,'FFFFFF-101010',1)==1 then
        whiteFrames=whiteFrames+1
        if whiteFrames>=2 then
          print('开始按钮为白色，车辆未解锁或没油')
          return 'fuelEmpty'
        end
      else whiteFrames=0 end
    end
    if whiteFrames==0 and (screen.scene=='car' or colorReady) then
      if screen.starUpPending or screen.fuel==0 then return 'fuelEmpty' end
      if type(utils.verifiedDailyCar)=='table' and not verifiedDailyDetail(utils.verifiedDailyCar,screen) then
        data['信息']='等待再次核对每日战利品车辆性能分'
      elseif fuel=='trial' and not ticketedTrial(screen) then
        data['信息']='等待重新确认赛事试驾按钮'
      elseif utils.mycolor(50,830,'12FBC3-303030')~=1 then
        local off=utils.mycolor(51,921,'12FBC3-303030')==1
        print('开赛页 TouchDrive 为关（关亮='..tostring(off)..'），点击开启')
        tap(50,830);sleep(1000)
      else
        local button=startButton(screen)
        if button or colorReady then
          if run and run.dailyLoot then
            local reason=dailyCup.skipReason()
            if reason~='' then return reason end
          end
          if fuel=='trial' then print('使用赛事提供的免费试驾车辆') end
          -- Start action: two consecutive taps, then wait two seconds.
          for press=1,2 do
            if button then tapUiWord(button) else tap(80,1080) end
          end
          utils.soloStartAt=utils.soloStartAt or tickCount()
          sleep(2000)
          -- Loading is not a race; the host shows the panel after detecting the race HUD.
          if _racePanel then _racePanel(false) end
          return 1
        end
      end
    elseif utils.proceedTipPage(screen) then
      -- 说明页挡在开始与比赛之间，点继续后重新读画面
    elseif driveTutorialPages<2 and dismissDriveTutorial(screen) then
      driveTutorialPages=driveTutorialPages+1
    end
    sleep(500)
  end
  error('未确认车辆开始按钮，保留当前任务')
end
-- Set requested state, rather than toggling blindly on every visit.
function utils.configureGarageFilter(selection)
  if selection == "" then return true end
  local selected={}
  for id in tostring(selection):gmatch('[^,]+') do selected[id]=1 end
  if selected['1'] and selected['3'] then error('星级和性能分只能选择一项') end
  if not _garageFilterState then error("车库筛选识别不可用") end
  if _garageFilterPrepare then _garageFilterPrepare() end
  local function read() return _garageFilterState(true) end
  local state=read()
  if not state.present then
    if state.wide then tap(570,1156) else tap(600,1192) end
    for i=1,20 do
      sleep(300);state=read()
      if state.present then break end
      if data["超时状态"] then error("车库筛选已超时") end
    end
  end
  if not state.present then error("未识别到车库筛选面板，已停止选车") end
  local ownedRow,rankRow,starsRow,sortRow=509,304,368,436
  -- Try both arrows and the center of the sort control if the direction stays the same.
  local sortColumns={[0]={352,340,328},[1]={328,340,352}}
  if state.wide then
    ownedRow,rankRow,starsRow,sortRow=478,224,301,385
    sortColumns={[0]={330,340,350},[1]={350,340,330}}
  elseif state.layout18By9 then
    ownedRow,rankRow,starsRow,sortRow=494,262,334,411
    sortColumns={[0]={350,340,330},[1]={330,340,350}}
  end
  local options={{"0","owned",ownedRow,340,"已拥有"},{"1","rank",rankRow,340,"性能分"},
    {"3","stars",starsRow,340,"星级"},{"2","descending",sortRow,sortColumns,"降序"}}
  -- Selecting one sort key can clear the other in the same redraw.
  if selected['3'] then options[2],options[3]=options[3],options[2] end
  local function refresh(delay)
    sleep(delay);state=read()
    if data['超时状态'] then error('车库筛选已超时') end
    if not state.present then error('车库筛选面板意外关闭') end
  end
  local function applyOptions()
    for _,option in ipairs(options) do
      local id,key,row,control,label=table.unpack(option)
      local wanted=selected[id] or 0
      if key=='descending' and wanted==0 then label='升序' end
      for attempt=1,3 do
        if state[key]==wanted then break end
        if state[key]~=1-wanted then return label end
        local before=state.frameId
        data['信息']='设置车库筛选：'..label
        if key=='descending' then
          tap(row,control[wanted][attempt])
        else
          local center=state[key..'Center']
          tap(center and 719-center.y or row,center and center.x or control)
        end
        -- Return as soon as the change is visible. A slow list redraw gets time to
        -- finish before another tap can accidentally undo the first one.
        for poll=1,9 do
          refresh(poll==1 and 150 or 240)
          if state[key]==wanted and (not before or state.frameId~=before) then break end
        end
        if before and state.frameId==before then return label end
      end
      if state[key]~=wanted then return label end
    end
    -- Confirm all four values together on a second frame before closing the panel.
    local before=state.frameId
    for poll=1,8 do
      refresh(120)
      if not before or state.frameId~=before then
        for _,option in ipairs(options) do
          if state[option[2]]~=(selected[option[1]] or 0) then return option[5] end
        end
        return
      end
    end
    return '筛选画面更新'
  end
  local unconfirmed=applyOptions()
  if unconfirmed then
    print('车库筛选未确认：'..unconfirmed..'，再试一遍')
    refresh(400)
    unconfirmed=applyOptions()
    if unconfirmed then error('无法确认车库筛选：'..unconfirmed) end
  end
  tap(134,320)
  for i=1,20 do
    sleep(300);state=read()
    if not state.present then data["信息"]="车库筛选已应用";return true end
  end
  error("车库筛选未关闭，已停止选车")
end
-- Only fresh vehicle-detail observations can prove empty fuel. Unknown is never empty.
function utils.carFuel(allowGarage)
  local previous=nil;local frame=nil
  local garageFrame=nil;local stillInGarage=false
  local returnedFromUpgrade=false
  local deferredPrompt=false
  local attempts=0
  while attempts<10 do
    attempts=attempts+1
    local screen=_soloEventUi()
    if screen.scene=='vehicleUnlock' and screen.unlockLater and not deferredPrompt then
      tapUiWord(screen.unlockLater)
      deferredPrompt=true
      sleep(1200)
    elseif deferredPrompt and screen.scene=='cars' then
      return 'vehicleLocked',screen
    end
    if screen.scene=='lockedCar' then return 'vehicleLocked',screen end
    if screen.scene=='vehicleUpgrade' and not returnedFromUpgrade then
      data['信息']='误入车辆属性升级，返回车辆详情'
      print(data['信息'])
      tap(690,38)
      returnedFromUpgrade=true
      attempts=0
      previous=nil;frame=screen.frameId
      sleep(1200)
    end
    if allowGarage and screen.scene=='cars' and screen.frameId then
      stillInGarage=stillInGarage or (garageFrame~=nil and garageFrame~=screen.frameId)
      garageFrame=screen.frameId
    else
      garageFrame=nil;stillInGarage=false
    end
    local availability=screen.scene=='car' and screen.starUpPending and 'starUp'
      or screen.scene=='car' and screen.fuel and screen.fuel>=0 and screen.fuel
      or ticketedTrial(screen) and 'trial'
    if availability then
      if screen.frameId~=frame and previous==availability then return availability,screen end
      previous=availability;frame=screen.frameId
    elseif screen.scene~='car' and screen.scene~='unknown' and screen.scene~='loading' then
      -- Only a positively identified non-detail screen disproves the last reading;
      -- flickering 'unknown' frames are ignored.
      previous=nil
    end
    sleep(500)
  end
  if stillInGarage then return nil end
  error('无法确认车辆燃油，保留当前任务')
end
function utils.taskCars()
  local key=data['项目']=='特殊赛事' and '特殊赛事用车' or data['项目']=='车联' and '车手联会用车' or '杯赛用车'
  return utils.defultCfg(run.data[key],{1})
end
function utils.backToGarage()
  keyPress('back');sleep(1200)
  for i=1,20 do
    keepCapture()
    if _soloEventUi and _soloEventUi().scene=='cars' then return end
    if cmpColorEx('666|86|2E2E2E-101010,666|120|373737-101010,574|140|5400CF-201030',1)==1 or
        cmpColorEx('574|130|FFFFFF-101010,688|1076|6B6B6B-101010,692|1246|ECDF00-101010',1)==1 then return end
    sleep(300)
  end
  error('未确认返回车库，保留当前任务')
end
function utils.chooseCarCup(carList)
  if #carList==0 then error('未配置车辆，保留当前任务') end
  local clickMode=tostring(utils.defultCfg(run.data['选车方式'],'0'))=='1' or data['项目']=='车联'
  local rotate=tostring(utils.defultCfg(run.data['用车模式'],'0'))=='1'
  local filter=utils.defultCfg(run.data['选车筛选'],'')
  if filter~='' then
    utils.configureGarageFilter(filter);sleep(2000)
    if _soloGarageUi and vehicleGrid and vehicleGrid.confirmEmptyGarage() then return 'garageEmpty' end
  end
  local dailyGrid=run.dailyLoot and _soloGarageUi and vehicleGrid
  local useGrid=dailyGrid
  if not clickMode and not useGrid and _soloGarageUi and vehicleGrid then
    local page=_soloGarageUi(false)
    useGrid=page.scene=='cars' and page.garage and #page.garage.cards>0
  end
  local origin
  if dailyGrid then utils.verifiedDailyCar=false end
  local entrySwipes=30
  if run.dailyLoot and filter~='' then entrySwipes=clickMode and 6 or 0 end
  if useGrid then
    local reason
    origin,reason=vehicleGrid.soloOrigin(dailyGrid and entrySwipes or 0)
    if reason then return reason end
  else
    for i=1,entrySwipes do swipe(290,70,290,1200,100);sleep(100) end
    sleep(2000)
  end
  local wide=false
  if not clickMode and _garageFilterState then
    keepCapture()
    wide=_garageFilterState().wide==true
  end
  local lastCar=1
  local function openFirstCar(target)
    lastCar=1
    if dailyGrid then
      local first=vehicleGrid.soloCar(origin or vehicleGrid.soloOrigin(),1)
      if not first then return false end
      tapUiWord(first)
    else tap(400,220) end
    sleep(1500)
    if target<0 then tap(360,490);sleep(700);lastCar=-1 end
    return true
  end
  if clickMode and not openFirstCar(tonumber(carList[1])) then return 'garageChanged' end
  local empty={};local unavailable={};local locked={};local missing={}
  for pass=1,2 do
    for j,raw in ipairs(carList) do
      local car=tonumber(raw)
      if not car or car==0 then error('车辆序号无效，保留当前任务') end
      if data['超时状态'] then error('选车超时，保留当前任务') end
      if not missing[car] and not empty[car] and not unavailable[car] and not locked[car] and (not rotate or data['用车次数']['car'..j]~=1) then
        local retryX,retryY,slot,fuel,detail
        if clickMode then
          if not lastCar and not openFirstCar(car) then return 'garageChanged' end
          utils.clickChoose(car,lastCar)
        elseif useGrid then
          if car<1 or car%1~=0 then error('滑动选车需要正整数车辆序号') end
          local reason,count
          if not origin then origin,reason=vehicleGrid.soloOrigin() end
          if not origin then return reason or 'garageChanged' end
          slot,reason,count=vehicleGrid.soloCar(origin,car)
          origin=nil
          if reason=='pastEnd' then
            missing[car]=true
            print('已确认车库末尾，共 '..count..' 辆，跳过配置车辆 '..car)
            goto nextCar
          end
          if not slot then return 'garageChanged' end
          if slot.availability=='locked' or slot.availability=='starUp' then
            fuel=slot.availability=='locked' and 'vehicleLocked' or 'starUp'
            print('配置车辆 '..car..(fuel=='starUp' and ' 待升星' or ' 尚未解锁')..'，已在列表跳过')
          elseif slot.availability=='empty' then
            -- The card-level fuel pill already proves the empty tank; skip the
            -- detail round-trip like the multiplayer garage does.
            fuel=0
          else
            if dailyGrid then
              local focused=_soloGarageScore and _soloGarageScore(slot.left,slot.top)
              if focused and focused~='' then slot.score=focused end
              if (not slot.score or slot.score=='') and #(slot.model or '')<3 then
                error('未读到每日战利品第 '..car..' 辆车的性能分或车型，已保留任务进度')
              end
            end
            tapUiWord(slot)
            sleep(1200)
          end
        else
          local slot=utils.getCarHL(car);local previous=utils.getCarHL(lastCar)
          utils.carSwipe(slot[2],previous[2]);keepCapture()
          local column=math.min(slot[2],3)
          if wide then
            -- Tap the car artwork on the left of the card, away from the blueprint control.
            retryX,retryY=({430,235})[slot[1]],195+(column-1)*370
            tap(retryX,retryY+35)
          else
            local h=({272,40})[slot[1]];local l=({60,500,1000})[column]
            local x,y=findMultiColor(h,l,h+60,l+250,'13FBC3-101010','6|10|39260C-101010|158|-2|FFFFFF-101010',2,1)
            -- Empty cards lack the green badge. Open that configured card to read its counter.
            local tx,ty=h+80,math.min(l+230,1200)
            if x~=-1 and y~=-1 then tx,ty=x+20,y end
            retryX,retryY=tx,ty
            tap(tx,ty)
          end
          sleep(1500)
        end
        lastCar=car
        if not fuel then fuel,detail=utils.carFuel(not clickMode) end
        if fuel==nil and retryX then
          -- A second tap is allowed only while the same garage page is still visible.
          tap(retryX,retryY)
          sleep(1500)
          fuel,detail=utils.carFuel(true)
        end
        if dailyGrid and slot and detail and (type(fuel)=='number' or fuel=='trial') then
          local matched=false
          for attempt=1,4 do
            if verifiedDailyDetail(slot,detail) then matched=true;break end
            sleep(300)
            detail=_soloEventUi()
          end
          if not matched then
            print('每日战利品核对识别：卡片 '..tostring(slot.score)..' '..tostring(slot.model)..
              ' · 详情 '..tostring(detail and detail.soloScore)..' '..tostring(detail and detail.soloName))
            error('每日战利品第 '..car..' 辆车的卡片与详情性能分不一致，已保留任务进度')
          end
          print('每日战利品已核验第 '..car..' 辆：'..(detail.soloName or '')..' · 性能分 '..(detail.soloScore or slot.score or '未识别'))
        end
        if fuel=='vehicleLocked' then
          if #carList==1 then return fuel end
          locked[car]=true
          if not (slot and slot.availability=='locked') then
            print('配置车辆 '..car..' 未解锁，尝试下一辆配置车辆')
            if detail and detail.scene=='cars' then
              if clickMode then lastCar=nil;origin=nil end
            elseif not clickMode then utils.backToGarage() end
          end
        elseif fuel==nil then
          unavailable[car]=true
          print('配置车辆 '..car..' 未打开详情，尝试下一辆配置车辆')
        elseif fuel=='trial' or (type(fuel)=='number' and fuel>0) then
          data['用车次数']['car'..j]=1
          if rotate and _selectCarEntry then _selectCarEntry(j,pass==2) end
          if dailyGrid then utils.verifiedDailyCar=slot or true end
          utils.selectNextTaskCar=false
          data['信息']='使用配置车辆 '..car..(fuel=='trial' and '（免费试驾）' or '，剩余燃油 '..fuel)
          print(data['信息']);return true
        else
          empty[car]=true
          data['信息']='配置车辆 '..car..(fuel=='starUp' and ' 待升星' or ' 已无油')..'，尝试下一辆';print(data['信息'])
          if not clickMode and not (slot and (slot.availability=='starUp' or slot.availability=='empty')) then utils.backToGarage() end
        end
      end
      ::nextCar::
    end
    local noneAvailable=true
    local missingDetails=false
    for _,raw in ipairs(carList) do
      local car=tonumber(raw)
      if not missing[car] and not empty[car] and not locked[car] then
        if wide and unavailable[car] then missingDetails=true else noneAvailable=false end
      end
    end
    if noneAvailable then
      if missingDetails then
        if next(locked) then return 'vehicleUnavailable' end
        break
      end
      if next(missing) then return 'vehicleUnavailable' end
      if next(locked) then return 'vehicleLocked' end
      data['信息']='配置车辆均不可出赛，跳过本轮任务';print(data['信息']);return 'fuelEmpty'
    end
    -- Once-per-car mode starts another ordered round after all used entries were visited.
    if not rotate then break end
    data['用车次数']={}
  end
  error('未能确认配置车辆可用，保留当前任务')
end
function utils.startConfiguredRace()
  local cars=utils.taskCars()
  local verifyDaily=run.dailyLoot and _soloGarageUi and vehicleGrid
  if (utils.selectNextTaskCar and #cars>1) or (verifyDaily and not utils.verifiedDailyCar) then
    utils.backToGarage()
    local selected=utils.chooseCarCup(cars)
    if type(selected)=='string' then return selected end
  end
  utils.selectNextTaskCar=false
  for attempt=1,#cars+1 do
    local result,screen=utils.startRace()
    if result~='fuelEmpty' and result~='vehicleLocked' then return result end
    -- Some special events open the only vehicle directly, without a selection page.
    if #cars==1 then print('配置车辆不可出赛，跳过本轮任务');return result end
    if not screen or screen.scene~='cars' then utils.backToGarage() end
    local selected=utils.chooseCarCup(cars)
    if type(selected)=='string' then return selected end
  end
  error('选车后燃油状态反复变化，保留当前任务')
end
function utils.adjustCar()
  local isJust = false
  if data["用车情况"][2] ~= 0 then
    local arr = {
      "4B00E5-101010",
      "150015-101010",
      "2B002B-101010",
      "2A007F-101010",
      "5400FF-030303"
    }
    for k, v in ipairs(arr) do
      keepCapture()
      x, y = findMultiColor(400, 720, 400, 800, v, "0|19|" .. v .. "|2|19|" .. v, 0, 1)
      if x ~= -1 and y ~= -1 then
        utils.mySwipe(290, 200, 290, 910)
        sleep(1000)
        isJust = true
        break
      end
    end
  end
  releaseCapture()
  return isJust
end
function utils.getCarHL(n)
  local hs = n % 2
  if hs == 0 then
    hs = 2
  end
  local ls = math.floor(n / 2 + 0.5)
  return {hs, ls}
end
function utils.carSwipe(l1, l2)
  if l2 < 3 then
    l2 = 3
  end
  if l1 < 3 then
    l1 = 3
  end
  if l1 == l2 then
    return
  end
  local dx = 293
  if data["项目"] == "杯赛" or data["项目"] == "特殊赛事" or data["项目"] == "车联" then
    dx = 288
  end
  if l2 < l1 then
    local sn = l1 - l2
    for ti = 1, sn do
      local dy = utils.getCarAdjust()
      utils.mySwipe(dx, 710 - dy, dx, 200)
    end
  else
    local sn = l2 - l1
    for ti = 1, sn do
      local dy = utils.getCarAdjust()
      utils.mySwipe(dx, 200, dx, 710 + dy)
    end
  end
  sleep(500)
end
function utils.getCarAdjust()
  local ty = 980
  local tx = 400
  if data["项目"] == "杯赛" or data["项目"] == "特殊赛事" or data["项目"] == "车联" then
    ty = 855
  end
  keepCapture()
  for k, v in ipairs({
    "150015-050505",
    "4B00E5-050505",
    "2B002B-050505",
    "2A007F-050505",
    "5400FF-030303"
  }) do
    x, y = findMultiColor(tx, 720, tx, 1200, v, "0|19|" .. v .. "|2|19|" .. v, 0, 1)
    if x ~= -1 and y ~= -1 then
      return ty - y
    end
  end
  return 0
end
function utils.clickChoose(n1, n2)
  toast(n1)
  n1, n2 = tonumber(n1), tonumber(n2)
  if n1 > n2 then
    for i = 1, n1 - n2 do
      tap(360, 1190)
      sleep(700)
    end
  elseif n1 < n2 then
    for i = 1, n2 - n1 do
      tap(360, 490)
      sleep(700)
    end
  end
end
-- Legacy color specifications are BGR: 12FBC3 is the lime selection underline.
-- Clicking an already selected card enters it; never click the first card just to reset.
function utils.cupSelectedAt(y)
  if _cupCards then
    keepCapture();local cards = _cupCards();releaseCapture()
    for _, card in ipairs(cards) do
      if card.selected and math.abs(card.center - y) < 70 then return true end
    end
    -- A full card at this location was read, so do not trust a covered underline.
    for _, card in ipairs(cards) do if math.abs(card.center - y) < 70 then return false end end
  end
  return utils.mycolor(16, y, "12FBC3-101010") == 1
end
function utils.waitCupSelected(y)
  for attempt = 1, 8 do
    if utils.cupSelectedAt(y) then return true end
    sleep(500)
  end
  return false
end
-- The carousel scrolls after a card tap. Verify the artwork before using the
-- selected card's new center, even when it moved off its original coordinates.
local function sameCupArtwork(a,b)
  if not a.artwork or not b.artwork or #a.artwork==0 or #a.artwork~=#b.artwork then return false end
  local difference=0
  for i,value in ipairs(a.artwork) do difference=difference+math.abs(value-b.artwork[i]) end
  return difference/#a.artwork<=115
end
local function waitWideCupSelected(target)
  if not target.artwork then return utils.waitCupSelected(target.center) and target.center end
  for attempt=1,10 do
    keepCapture();local cards=_cupCards();releaseCapture()
    for _,card in ipairs(cards) do
      if card.selected and card.artwork then
        if math.abs(card.center-target.center)<70 then return card.center end
        if sameCupArtwork(target,card) then return card.center end
      end
    end
    sleep(500)
  end
end
-- At the right stop, count only a continuous row of complete cards. Selecting
-- one enlarges it and may shift the row, so confirm the ordinal again before entry.
local function wideCupFromRight(cards,ordinal)
  local index=#cards-ordinal+1
  if index<1 then return nil end
  for i=index+1,#cards do
    local gap=cards[i].center-cards[i-1].center
    if gap<115 or gap>185 then return nil end
  end
  return cards[index]
end
local function waitCupFromRight(ordinal,rightEdge)
  local lastCenter
  for attempt=1,10 do
    keepCapture();local cards=_cupCards();releaseCapture()
    local target=wideCupFromRight(cards,ordinal)
    if target and target.selected and math.abs(cards[#cards].center-rightEdge.center)<45
        and sameCupArtwork(rightEdge,cards[#cards]) then
      if lastCenter and math.abs(target.center-lastCenter)<=3 then return target.center end
      lastCenter=target.center
    else lastCenter=nil end
    sleep(350)
  end
end
-- Swipe the cup carousel until it stops moving. Stability tolerates 3px of
-- inertia/bounce jitter instead of demanding pixel-identical reads, and gives
-- up after six attempts: the caller then anchors on the current rightmost
-- card and lets the findCup identity check catch a wrong anchor. Returns the
-- latest card list and whether the list settled.
local function scrollCupCarouselToEnd(leftwards)
  if not _cupCards then
    if leftwards then for i = 1, 8 do swipe(50, 200, 50, 1000, 100) end
    else for i = 1, 8 do swipe(100, 1000, 100, 200, 100) end end
    -- Host gestures block; yield once so the settling delay starts after they finish.
    sleep(1);sleep(2000)
    return nil, false
  end
  local previous = nil
  for attempt = 1, 6 do
    if leftwards then swipe(50, 200, 50, 1000, 100) else swipe(100, 1000, 100, 200, 100) end
    sleep(1);sleep(1200)
    keepCapture();local cards = _cupCards();releaseCapture()
    if #cards > 0 then
      local centers = {}
      for _, card in ipairs(cards) do centers[#centers + 1] = tostring(card.center) end
      print("端点稳定检查 #"..attempt.."："..table.concat(centers, ","))
      if previous and #cards == #previous then
        local stable = true
        for i, card in ipairs(cards) do
          if math.abs(card.center - previous[i].center) > 3 then stable = false;break end
        end
        if stable then return cards, true end
      end
      previous = cards
    end
  end
  return previous, false
end
-- The spotlight dims the other cards. Count positions only after the carousel is readable.
local function cupClaimState(center)
  if not _cupClaimState then return '' end
  keepCapture()
  local state=_cupClaimState(center)
  releaseCapture()
  return state
end
local function awaitCupList()
  for attempt=1,8 do
    if cupClaimState()~='spotlight' then return true end
    sleep(400)
  end
end
local function cupEntryGuard(center,useCurrentFrame)
  local state
  if useCurrentFrame then state=_cupClaimState and _cupClaimState(center)
  else state=cupClaimState(center) end
  if state=='claimOnly' then return 'cupClaimOnly' end
  if state=='spotlight' then return 'cupListObscured' end
end
-- The last drag measured the target on a known list frame. Read the highlight,
-- entry button and scroll displacement from the same new frame as the OCR.
local function selectedCupEntry(screen,card,allowBorder)
  local list=screen.scene=='cupList' or screen.scene=='wideCupList'
  if (not list and screen.scene~='unknown') or screen.closeX then return end
  local shift=card.selected and 0 or _cupScrollDelta()
  if not shift then return end
  local expected=card.center-shift
  local center
  for _,current in ipairs(_cupCards() or {}) do
    if current.selected then
      if center or math.abs(current.center-expected)>=55 then return end
      center=current.center
    end
  end
  -- An obscured border can still have a readable entry button. In that case
  -- match the button itself to the tracked position, not to a neighboring card.
  local x=center or expected
  local radius=center and 85 or 54
  local button=findUiWord(screen,{['进入']=true},x-radius,x+radius,570,710)
  if button then return center or button.x,button end
  if list and allowBorder then return center end
end

local function enterDailyEventCard(position,card)
  if not card.selected then
    tap(189,card.center)
    sleep(1);sleep(500)
  end
  local taps,lastTap=0,0
  local previousCenter,lastFrame,lastScene
  local deadline=tickCount()+9000
  repeat
    local screen=_soloEventUi() or {}
    local scene=screen.scene
    lastScene=scene or 'unknown'
    if scene=='event' or scene=='clubRewards' or scene=='dailyRewardOnly' or scene=='cars' or scene=='car'
        or (scene=='unknown' and not screen.closeX and cup and cup.detailVisible()) then
      local title=(screen.text or ''):gsub('%s+',' ')
      data['信息']='杯赛位置 '..position..'：'..lastScene..(title~='' and '，'..utf8.left(title,16) or '')
      print('已确认第 '..position..' 个赛事位置，界面 '..lastScene..' 标题 '..utf8.left(title,40))
      return 'entered'
    end
    if scene=='cupClaimSpotlight' then return nil,nil,'cupListObscured' end
    if not screen.frameId or screen.frameId~=lastFrame then
      lastFrame=screen.frameId
      local center,button
      if taps==0 or (taps==1 and tickCount()-lastTap>=2500) then
        center,button=selectedCupEntry(screen,card,taps==0)
      end
      if center and previousCenter and math.abs(center-previousCenter)<=3 then
        local blocked=cupEntryGuard(center,true)
        if blocked then return nil,nil,blocked end
        if button then tapUiWord(button) else tap(65,center) end
        taps=taps+1
        lastTap=tickCount()
        if taps==1 then deadline=lastTap+10000 end
        previousCenter=nil
      else
        previousCenter=center
      end
    end
    sleep(400)
  until tickCount()>=deadline
  if taps==0 then return nil,'未确认第 '..position..' 张赛事卡的进入按钮' end
  print('已点击第 '..position..' 张赛事卡的进入按钮，未确认赛事详情，最后界面 '..lastScene)
  return nil,nil,'cupEntryUnconfirmed'
end
function utils.chooseCup(num,wide,configuredOnly)
  data["信息"] = "选择杯赛"
  local function unavailable(reason)
    print('无法选择配置的杯赛：'..reason)
    return 'cupPositionUnavailable'
  end
  local cupAddr = LOCAL_ROOT .. "/cup" .. run.index .. ".png"
  local ret, x, y = -1, -1, -1
  num = tonumber(num)
  if not num or num == 0 or num ~= math.floor(num) then return 'cupPositionInvalid' end
  -- The reference artwork is tagged with the position it was captured for, so a
  -- changed configuration drops it while an unchanged one survives restarts.
  local posAddr = cupAddr .. ".pos"
  local function readRefPos()
    local file = io.open(posAddr, "rb")
    if not file then return nil end
    local value = tonumber(file:read("*a"))
    file:close()
    return value
  end
  local function writeRefPos(value)
    local file = io.open(posAddr, "wb")
    if file then file:write(tostring(value)); file:close() end
  end
  if cupClaimState()=='spotlight' then
    -- Move over the card artwork only. Once the hint lifts, anchor the list again
    -- because the game can consume the first swipe to remove its dimming layer.
    configuredOnly=true
    local from,to=250,1020
    if num<0 then from,to=to,from end
    for attempt=1,3 do
      swipe(115,from,115,to,220)
      sleep(1);sleep(350)
      if awaitCupList() then break end
      if attempt==3 then return 'cupListObscured' end
    end
  end
  if wide and num>0 then
    for i=1,6 do swipe(115,260,115,1000,180) end
    sleep(1);sleep(1500)
    if not awaitCupList() then return 'cupListObscured' end
    keepCapture()
    local cards=_cupCards()
    if #cards==0 or cards[1].center>210 then
      return unavailable('超宽屏未能确认配置的赛事位置 '..num..'，未进入赛事')
    end
    local position=math.min(num,#cards)
    for i=2,position do
      local gap=cards[i].center-cards[i-1].center
      if gap<115 or gap>185 then return unavailable('超宽屏赛事卡片未完整识别') end
    end
    local center=cards[position].center
    if not cards[position].selected then
      tap(115,center);sleep(1);sleep(1000)
      center=waitWideCupSelected(cards[position])
      if not center then return unavailable('无法确认已选中杯赛位置 '..position) end
    end
    for step=position+1,num do
      local nextCard
      for attempt=1,4 do
        keepCapture();cards=_cupCards()
        for i,card in ipairs(cards) do
          if card.selected then
            local following=cards[i+1]
            local gap=following and following.center-card.center
            if gap and gap>=115 and gap<=185 then nextCard=following end
            break
          end
        end
        if nextCard then break end
        if attempt<4 then swipe(115,1050,115,745,300);sleep(1);sleep(800) end
      end
      if not nextCard then return unavailable('超宽屏未能确认配置的赛事位置 '..num..'，未进入赛事') end
      center=nextCard.center;tap(115,center);sleep(1);sleep(1000)
      center=waitWideCupSelected(nextCard)
      if not center then return unavailable('无法确认已选中杯赛位置 '..step) end
    end
    local blocked=cupEntryGuard(center)
    if blocked then return blocked end
    tap(115,center)
    return 1
  end
  utils.cupPositions = utils.cupPositions or {}
  if utils.cupPositions[run.index] ~= num then
    utils.cupPositions[run.index] = num
    print("本次任务按配置选择杯赛位置：", num)
  end
  -- Drop the saved reference only when the configured position changed. Keeping
  -- it across restarts lets the artwork check below catch a wrong blind pick
  -- and re-run the search-and-correct path (自愈机制).
  if readRefPos() ~= num then os.remove(cupAddr) end
  local abs = math.abs(num)
  -- 从当前选中卡开始逐张向右点选到第 target 张，返回最终选中卡的中心 y。
  -- 判据照原版：x=16 竖线上的青色竖条标出当前选中卡，比卡片白边识别稳；
  -- 每点一次都等竖条移过去再走下一步，所以滑动距离不准也不会累积成偏移。
  local function stepToCup(target)
    -- 选中卡在左侧有一条与卡片等高的石灰绿描边（色键按 BBGGRR 解析成 C3FB12）。
    -- findColor 只给描边顶端，卡片中心要量出这段竖条的中点。
    local function highlightY()
      keepCapture()
      local top = findColor(16, 10, 16, 1270, "12FBC3-101010")
      if top == nil or top < 0 then releaseCapture() return nil end
      local bottom = top
      while bottom < 1270 and cmpColor(16, bottom + 1, "12FBC3-101010", 1) == 1 do bottom = bottom + 1 end
      releaseCapture()
      if bottom - top < 100 then return nil end
      return math.floor((top + bottom) / 2)
    end
    -- 必须重新截屏再读：直接读缓存的帧拿不到刚才那次点击后的选中状态。
    local function readCards()
      keepCapture()
      local raw = _cupCards() or {}
      releaseCapture()
      -- 底部按钮一类的元素偶尔会被当成卡片，按 180 左右的等距把异常间距剔掉。
      local cards = {}
      for _, card in ipairs(raw) do
        local prev = cards[#cards]
        local gap = prev and card.center - prev.center
        if not gap or (gap >= 140 and gap <= 230) then cards[#cards + 1] = card end
      end
      return cards
    end
    local function currentY(cards)
      for _, card in ipairs(cards) do if card.selected then return card.center end end
      local y = highlightY()
      if not y then return nil end
      -- 描边中点和卡片中心可能差几个像素，有对应卡片就用卡片自己的中心。
      for _, card in ipairs(cards) do
        if math.abs(card.center - y) <= 60 then return card.center end
      end
      return y
    end
    -- 只看读到的相邻卡片，不按固定步长推算坐标：算出来的 y 可能是底部按钮。
    local function nextCardAfter(cards, cur)
      for i, card in ipairs(cards) do
        if math.abs(card.center - cur) < 40 then
          local following = cards[i + 1]
          if not following then return nil end
          local gap = following.center - card.center
          if gap >= 140 and gap <= 230 then return following end
          return nil
        end
      end
      return nil
    end
    -- 卡片下部约 y=610 起是「进入/领取」按钮，点它就直接进赛事：选位必须点图区。
    -- 横向 y≈500 对应竖向 x=219，落在图区中段，离按钮行还有 100 多像素。
    local SELECT_X = 219
    local lastDump=''
    -- 界面切换（列表吸附、结算弹窗）会让这一帧读不到卡片，先等列表稳定再逐张点。
    if #readCards()==0 then
      for attempt = 1, 6 do
        sleep(700)
        if awaitCupList() and #readCards()>0 then break end
      end
    end
    -- 真机上一定有 _soloEventUi；旧版杯赛页（选中卡带白边、点卡只切换选中）已经从游戏里
    -- 移除，只有测试 fixture 没有这个函数，所以直接用函数是否存在来分流，不要再拿
    -- OCR 文字去猜新旧页面——猜错的代价是走回旧路径，一张也选不中。
    local hasSoloUi = type(_soloEventUi) == 'function'
    local page = hasSoloUi and _soloEventUi() or nil
    -- 真机上 OCR 偶尔会超时返回 nil，再等两把；仍然为 nil 就只能当空页面往下走。
    for attempt = 1, 2 do
      if page ~= nil or not hasSoloUi then break end
      sleep(700)
      page = _soloEventUi()
    end
    local pageText = page and tostring(page.text or '') or ''
    -- 只有「限时充值奖励」这种真正盖住列表的促销层才按返回键（scene 由 native 判成
    -- limitedSalePromo）。别用 OCR 文本里出现「充值」当判据：列表页右侧常年挂着
    -- 「精选奖励」之类的角标，OCR 一并读到就会误按返回键，直接退出到主界面，
    -- 后续的拖动和点击全部落在主界面上（实测点出了「练习」页）。
    if tostring(page and page.scene or '') == 'limitedSalePromo' then
      keyPress('back')
      sleep(1); sleep(900)
      page = type(_soloEventUi) == 'function' and _soloEventUi() or nil
      pageText = page and tostring(page.text or '') or ''
      if pageText:find('战利品', 1, true) == nil and pageText:find('活动规则', 1, true) == nil then
        return nil, '关闭促销层后已不在赛事列表页'
      end
    end
    -- 分流只看函数存不存在，不看这一次读到的结果：真机上 _soloEventUi 偶尔读超时返回
    -- nil，用 page==nil 当“旧的测试 fixture”就会误落进旧路径，一条也选不中。
    local anchored = not hasSoloUi
    local pageScene = page and tostring(page.scene or '') or ''
    if not anchored and pageScene ~= '' and pageScene ~= 'cupList' and pageScene ~= 'wideCupList'
        and pageScene ~= 'unknown' then
      -- 不在列表页时拖拽会把页面搅乱，直接交给上层重新进入。
      return nil, '当前不在赛事列表页（'..pageScene..'）'
    end
    if not anchored then
      -- Establish the left boundary, then track the target in screen coordinates.
      -- The final drag can travel less than one card, or stop moving altogether.
      local function observe(direction)
        keepCapture()
        local delta = _cupScrollDelta(direction)
        local cards = _cupCards() or {}
        releaseCapture()
        return delta, cards
      end
      local function drag(direction)
        local from, to = 1050, 510
        if direction < 0 then from, to = to, from end
        swipe(194, from, 194, to, 900)
        sleep(1);sleep(900)
        return observe(direction)
      end
      local function nearest(cards, x)
        local best
        for _,card in ipairs(cards) do
          if math.abs(card.center-x) < 55 and (not best or math.abs(card.center-x)<math.abs(best.center-x)) then best=card end
        end
        return best
      end
      observe(0)
      local cards, stable
      for attempt=1,32 do
        local delta
        delta,cards=drag(-1)
        if not delta then return nil,'无法核对杯赛列表向左滚动的距离' end
        if math.abs(delta)<=3 then
          if stable then break end
          stable=true
        else
          stable=false
        end
        if attempt==32 then return nil,'无法确认杯赛列表左端' end
      end
      -- The selected card is slightly enlarged. Snap the estimated slot back to a
      -- real card before tapping, and never substitute the last visible card.
      local first = nearest(cards,151)
      local x = (first and first.center or 151) + (target-1)*186
      while x>1180 do
        local delta
        delta,cards=drag(1)
        if not delta then return nil,'无法核对杯赛列表向右滚动的距离' end
        x=x-delta
        if delta<=3 then break end
      end
      local card = nearest(cards,x)
      if not card then return nil,'当前列表未找到第 '..target..' 张赛事卡' end
      local blocked = cupEntryGuard(card.center)
      if blocked then return nil,nil,blocked end
      print('杯赛第 '..target..' 张卡实际中心 '..card.center)
      return enterDailyEventCard(target,card)
    end
    for step = 2, target do
      local moved = false
      for attempt = 1, 6 do
        local cards = readCards()
        local cur = currentY(cards)
        if not cur then
          local dump={}
          for _,card in ipairs(cards) do dump[#dump+1]=math.floor(card.center)..(card.selected and '*' or '') end
          lastDump='['..#cards..'] '..table.concat(dump,',')
          sleep(600)
        else
          local nextCard = nextCardAfter(cards, cur)
          if nextCard then
            tap(SELECT_X, nextCard.center)
            sleep(300)
            -- 选中竖条换到别的卡上才算推进：列表吸附后目标卡的中心可能整体平移，
            -- 拿它和点击时的坐标比会误判，只比较「高亮是否离开了原来那张」。
            local now = currentY(readCards())
            if now and math.abs(now - cur) > 40 then moved = true break end
          else
            -- 下一张还在可视区外：照原版用蓝条推算距离滑一格，滑动后重新读卡再点。
            -- 滑动同样要落在图区，滑到底部按钮行/标签栏会被当成点击或切页。
            keepCapture()
            local _, _, bar = _findColorTriple(224, 1180, 224, 1278, "5400FF-101010", 0, 1.0)
            releaseCapture()
            local shift = (bar ~= nil and bar ~= -1) and (1208 - bar) or 0
            utils.mySwipe(SELECT_X, 1195, SELECT_X, 987 + shift)
            sleep(800)
          end
        end
      end
      if not moved then
        snapShot('cupFail')
        local scene=_soloEventUi()
        print('杯赛第 '..step..' 步未推进，当前选中 y='..tostring(currentY(readCards()))..' 卡片 '..lastDump
          ..' 界面 '..tostring(scene and scene.scene))
        return nil, '无法推进到第 '..step..' 个赛事位置，未进入赛事'
      end
    end
    -- 最后一张点完列表会吸附滚动，竖条要等动画结束才稳定，多读几次再下结论。
    for attempt = 1, 8 do
      local cur = currentY(readCards())
      if cur then return cur end
      sleep(500)
    end
    print('杯赛第 '..target..' 位置的高亮没能确认')
    return nil, '无法确认已选中杯赛位置 '..target
  end
  local xy = {100, 150}
  if 0 < num then
    -- 列表本来就在顶部就别滑：这块区域的横滑会被页面当成切换赛事页，滑完列表就没了。
    -- 只有第一张卡明显不在顶部（被卷上去了）时才滑回顶端，终点仍由下面的锚定校验兜住。
    keepCapture()
    local firstCard = (_cupCards() or {})[1]
    releaseCapture()
    if not _soloEventUi and (not firstCard or firstCard.center > 210) then
      for i = 1, 6 do
        swipe(199, 200, 199, 1000, 260)
      end
    end
    -- Host gestures block; yield once so the settling delay starts after they finish.
    sleep(1);sleep(2000)
    if not awaitCupList() then return 'cupListObscured' end
    -- 锚定第 1 张：只有它没被选中时才点，点已选中的卡会直接进入赛事。
    -- 读不到卡片清单（新版列表）时不锚定：这里的点击会直接进赛事。
    if not _soloEventUi and firstCard and not utils.cupSelectedAt(150) then
      tap(219, 150)
      sleep(1);sleep(1500)
      if not awaitCupList() then return 'cupListObscured' end
      if not utils.waitCupSelected(150) then
        return unavailable('无法锚定杯赛列表起点，未进入赛事')
      end
    end
    -- 逐张点选到配置位置。以前是按固定次数滑动再点固定坐标，滑动被遮罩吃掉
    -- 一部分位移时就整体少走（配置 13 实际停在 11）；首次没有参考图时这种错位
    -- 还会被当成「新杯赛」存成参考图，之后每轮都稳定错。
    if _soloEventUi or num > 1 or not firstCard then
      local center, why, blocked = stepToCup(num)
      if blocked then return blocked end
      if not center then return unavailable(why) end
      if center == 'entered' then return 1 end
      xy = {100, center}
    end
  else
    -- 负数位置为旧配置兼容路径（新配置仅支持正数）。尽力滑到右端：
    -- 6 次封顶，未稳定也按当前最右卡片锚定，身份校验与搜索矫正兜底。
    if not _cupCards then error("当前版本无法识别倒数赛事位置") end
    local cards, settled = scrollCupCarouselToEnd(false)
    if not awaitCupList() then return 'cupListObscured' end
    if not cards then return unavailable('无法识别赛事卡片，未进入赛事') end
    if not settled then print("滑动 6 次后列表仍未稳定，按当前最右卡片锚定") end
    local target = cards[#cards]
    print("杯赛列表锚定最右卡片，可见 "..#cards.." 张，最右中心 "..target.center)
    -- The legacy menu entry does not pass the viewport flag. The native reader
    -- supplies artwork on every wide-layout card for both entry paths.
    wide=wide or target.artwork~=nil
    if wide and target.artwork and abs>1 then
      local visible=wideCupFromRight(cards,abs)
      if visible then
        if not visible.selected then tap(100,visible.center);sleep(1);sleep(1000) end
        local center=waitCupFromRight(abs,target)
        if center then
          local blocked=cupEntryGuard(center)
          if blocked then return blocked end
          tap(100,center);return 1
        end
        -- Auto-centering may have scrolled the rightmost card offscreen. Restore
        -- the right stop before using the adjacent-card walk for longer lists.
        scrollCupCarouselToEnd(false)
        if not awaitCupList() then return 'cupListObscured' end
        keepCapture();cards=_cupCards();releaseCapture()
        target=cards[#cards]
        if not target then return unavailable('无法重新定位最右侧赛事') end
      end
    end
    if not target.selected then tap(100, target.center);sleep(1);sleep(1000) end
    local anchored
    if wide then anchored=waitWideCupSelected(target) else anchored=utils.waitCupSelected(target.center) end
    if not anchored then return unavailable('无法确认最后一个赛事已选中') end
    for step = 2, abs do
      local previous = nil
      for attempt = 1, 6 do
        keepCapture();cards = _cupCards()
        local selected = nil
        for i, card in ipairs(cards) do if card.selected then selected = i;break end end
        if selected and selected > 1 then
          local candidate = cards[selected - 1]
          local gap = cards[selected].center - candidate.center
          -- Reject abnormal gaps: a missed intermediate card must fail loudly
          -- instead of silently shifting the counted position.
          local gapOk = gap >= 140 and gap <= 230
          if wide then gapOk = gap >= 115 and gap <= 185 end
          if gapOk then
            previous = candidate
            break
          end
        elseif selected == 1 and attempt < 6 then
          swipe(100, 200, 100, 550, 300);sleep(1);sleep(1000)
        end
        -- Scrolling and selecting can briefly clip or hide an intermediate card.
        sleep(1);sleep(500)
      end
      if not previous then
        local positions={}
        for _,card in ipairs(cards) do positions[#positions+1]=card.center..(card.selected and "*" or "") end
        print("倒数赛事第 "..step.." 张，当前卡片中心："..table.concat(positions,","))
        snapShot(LOCAL_ROOT.."/cup-carousel-unconfirmed.png",0,0,719,1279)
        return unavailable('赛事数量不足或无法定位杯赛位置 '..num)
      end
      print("倒数赛事第 "..step.."/"..abs.." 张：向左选择中心 "..previous.center)
      tap(100, previous.center);sleep(1);sleep(1000)
      if wide then
        if not waitWideCupSelected(previous) then return unavailable('无法确认已向左选择赛事，未进入赛事') end
      elseif not utils.waitCupSelected(previous.center) then
        return unavailable('无法确认已向左选择赛事，未进入赛事')
      end
    end
    keepCapture();cards = _cupCards()
    local selected = nil
    for _, card in ipairs(cards) do if card.selected then selected = card;break end end
    if not selected then return unavailable('无法确认已选中杯赛位置 '..num) end
    if wide then
      local blocked=cupEntryGuard(selected.center)
      if blocked then return blocked end
      tap(100,selected.center);return 1
    end
    xy = {100, selected.center}
  end
  if not utils.cupSelectedAt(xy[2]) then
    tap(xy[1], xy[2])
    sleep(1);sleep(2000)
  end
  if not utils.cupSelectedAt(xy[2]) then return unavailable('无法确认已选中杯赛位置 '..num..'，未进入赛事') end
  local blocked=cupEntryGuard(xy[2])
  if blocked then return blocked end
  if configuredOnly then tap(xy[1],xy[2]);return 1 end
  local res = {-1, -1, -1, -1}
  if utils.fileExist(cupAddr) then res = utils.findCup(cupAddr) end
  if res[3] ~= -1 then
    tap(res[1] + 100, res[2] + 10)
    print("旧杯赛")
    return 1
  elseif utils.fileExist(cupAddr) == false then
    keepCapture()
    snapShot(cupAddr, 350, 212, 520, 330)
    releaseCapture()
    writeRefPos(num)
    tap(xy[1], xy[2])
    print("新杯赛")
    return 1
  else
    toast("杯赛位置可能发生变动")
    for i = 1, 8 do
      swipe(50, 200, 50, 1000, 100)
    end
    sleep(2000)
    if not utils.cupSelectedAt(144) then
      tap(70, 150)
      sleep(1000)
    end
    for i = 0, 20 do
      if i == 20 then
        toast("杯赛可能已经结束")
        for i = 1, 8 do
          swipe(50, 200, 50, 1000, 100)
        end
        sleep(2000)
        if not utils.cupSelectedAt(144) then
          tap(70, 150)
          sleep(1000)
        end
        os.remove(cupAddr)
        return utils.chooseCup(num)
      end
      if 6 < i then
        keepCapture()
        ret, x, y = _findColorTriple(50, 1100, 50, 1278, "12FBC3-101010", 0, 1.0)
        if y ~= -1 then
          y = 1184 - y
        end
        utils.mySwipe(22, 1195, 22, 982 + y)
        sleep(500)
        releaseCapture()
        tap(100, 1250)
        sleep(2000)
      elseif 0 < i and i < 7 then
        tap(100, 100 + 185 * i)
        sleep(2000)
      end
      res = utils.findCup(cupAddr)
      if res[3] ~= -1 then
        toast("已找到杯赛")
        tap(res[1] + 100, res[2] + 20)
        sleep(200)
        run.data["杯赛位置"] = (i + 1) .. ""
        utils.cupPositions[run.index] = i + 1
        writeRefPos(i + 1)
        return 1
      end
    end
  end
  return 0
end
function utils.findCup(cupAddr)
  local ret, x, y, x1, y1 = -1, -1, -1, -1, -1
  keepCapture()
  ret, x, y = _findColorTriple(16, 10, 16, 1270, "12FBC3-101010", 0, 1.0)
  if x == -1 then return {-1, -1, -1, -1} end
  ret, x1, y1 = _findPicTriple(350, 212, 520, 330, cupAddr, "101010", 0, 0.7)
  return {
    x,
    y,
    x1,
    y1
  }
end
local function sameSpecialCard(a,b)
  local shared=math.min(#a.artwork,#b.artwork)
  if shared<14 then return false end
  local matches=0
  for i=1,shared do
    local color=a.artwork[i]
    local other=b.artwork[i]
    local difference=math.abs((color>>16)-(other>>16))
      +math.abs((color>>8&255)-(other>>8&255))+math.abs((color&255)-(other&255))
    if difference<=72 then matches=matches+1 end
  end
  return matches*4>=shared*3
end

local function sameSpecialPage(a,b)
  if a.aligned~=b.aligned or #a.cards~=#b.cards then return false end
  for i,card in ipairs(a.cards) do
    local previous=b.cards[i]
    if math.abs(card.top-previous.top)>3 or #card.artwork~=#previous.artwork or
        not sameSpecialCard(card,previous) then return false end
  end
  return true
end

local function specialListPage()
  local deadline=tickCount()+5000
  local previous
  repeat
    local page=_specialEventList()
    if page.cards and #page.cards>0 then
      if page.frameId and previous and previous.frameId and page.frameId>previous.frameId and
          sameSpecialPage(page,previous) then return page end
      previous=page
    else previous=nil end
    sleep(250)
  until tickCount()>=deadline
  error('未识别特殊赛事入口列表，已保留当前任务')
end

local function specialChapterPage(screen)
  if screen.scene~='unknown' then return false end
  local firstChapter
  for _,word in ipairs(screen.words or {}) do
    local label=word.text:gsub('[ \t\r\n|]','')
    local chapter=label:match('^章节(%d+)$')
    if chapter and word.x<=370 and word.y>=130 and word.y<=670 then
      if screen.stageRail or firstChapter and chapter~=firstChapter then return true end
      firstChapter=chapter
    end
  end
  return false
end

function utils.openSpecialEntry(screen,position,opts)
  opts=opts or {}
  local label=opts.label or '特殊赛事'
  local entry=label..'入口'
  if position<1 or position%1~=0 then error(label..'位置必须为正整数') end
  screen=screen or _soloEventUi()
  if not screen.specialSeasonTab then
    utils.openHomeTab(screen,'season')
    for attempt=1,5 do
      screen=_soloEventUi()
      if screen.specialSeasonTab then break end
      sleep(350)
    end
    if not screen.specialSeasonTab then error('未确认赛季赛事标签，已保留当前任务') end
  end
  -- Reset the rail before trying to read any card at a saved scroll position.
  data['信息']='向下拉取特殊赛事入口列表'
  for pull=1,8 do
    swipe(550,220,200,220,300,90)
  end
  sleep(2000)
  local page=specialListPage()
  local atOrigin=false
  for attempt=1,8 do
    -- Use a short confirmation pull when the leading tile is already visible.
    swipe(page.aligned and 405 or 550,220,200,220,page.aligned and 540 or 460,160)
    sleep(500)
    local previous=page
    page=specialListPage()
    if page.aligned and sameSpecialPage(page,previous) then
      atOrigin=true
      print(entry..'列表已回顶，确认了第 1 张赛事卡')
      break
    end
  end
  if not atOrigin then error('未确认'..entry..'列表起点，已保留当前任务') end

  local offset=0
  local drags=0
  while position>offset+#page.cards or
      (position-offset>2 and page.cards[position-offset].complete==false) do
    drags=drags+1
    if drags>position+3 then error('未确认'..entry..'位置 '..position..'，已保留当前任务') end
    local previous=page
    -- Advance one slot using the measured separator and hold before releasing.
    local distance=page.scrollDistance
    if not distance or distance<=0 then distance=175 end
    swipe(180,220,180+distance,220,200,500)
    sleep(500)
    page=specialListPage()
    if sameSpecialPage(page,previous) and position<=offset+#page.cards then break end
    -- Keep an overlap with the previous page, so a drag cannot skip a position.
    local advanced,travel
    for i,before in ipairs(previous.cards) do
      for j,current in ipairs(page.cards) do
        local dy=before.top-current.top
        if dy>=8 and sameSpecialCard(before,current) then
          if advanced and (advanced~=i-j or math.abs(dy-travel)>6) then
            error(entry..'翻页位置不明确，已保留当前任务')
          end
          advanced,travel=i-j,dy
        end
      end
    end
    if not advanced or advanced<0 then
      error('未确认'..entry..'位置 '..position..'，已保留当前任务')
    end
    offset=offset+advanced
  end

  local target=page.cards[position-offset]
  -- Keep the three card taps; only the coordinates follow the current rail.
  for press=1,3 do tapUiWord(target);sleep(200) end
  local tappedFrame=page.frameId
  for attempt=1,7 do
    sleep(450)
    local preview=_soloEventUi(false,true)
    if preview.frameId and preview.frameId>tappedFrame then
      if preview.specialSeasonTab then
        local button=findUiWord(preview,{['开始']=true,['进入']=true,['进人']=true},930,1200,450,575)
            or preview.specialEntryButton
        -- Entry selection is positional, not a title-OCR comparison.
        if button then
          print('已选中'..entry..'位置 '..position)
          tapUiWord(button)
          -- opts.enterOnce：进入后由调用方自行确认落地页面（如车手联会）。
          if not opts.enterOnce then tapUiWord(button) end
          return
        end
        -- Retry only while the same tile is still under the finger on the entry list.
        if attempt==2 or attempt==4 then
          local current=_specialEventList()
          local card=current.cards and current.cards[position-offset]
          if card and current.frameId>=preview.frameId and math.abs(target.top-card.top)<=3
              and sameSpecialCard(target,card) then
            tapUiWord(card)
            tappedFrame=current.frameId
          end
        end
      elseif specialChapterPage(preview) or preview.scene=="missionRewards" or utils.specialMissionContinue(preview)
          or opts.entered and opts.entered(preview) then
        print(entry..'位置 '..position..' 已直接进入赛事')
        return preview
      end
    end
  end
  error('未确认'..entry..'位置 '..position..' 的进入按钮或赛事页面，已保留当前任务')
end

function utils.intoSpecial(screen)
  data["信息"] = "进入特殊赛事 "
  local entered=utils.openSpecialEntry(screen,tonumber(run.data["特殊赛事位置"]) or 1)
  return utils.selectSpecialStage(entered)
end
function utils.selectSpecialStage(screen)
  if screen and utils.specialMissionContinue(screen) then return 1 end
  local spStage = tonumber(run.data["特殊赛事阶段"]) or 1
  sleep(3000)
  keepCapture()
  if cmpColor(640, 1270, "5400FF-101010", 1) == 1 then
    tap(640, 1270)
    sleep(3000)
  end
  keepCapture()
  if run.data["称霸模式"] == "0" then
    local chapter=tonumber(run.data["称霸位置"]) or 1
    local function numberedWord(screen,label,number,minX,maxX,minY,maxY)
      for _,word in ipairs(screen.words or {}) do
        local digits=word.text:gsub('[ \t\r\n]',''):match(label..'(%d+)')
        if tonumber(digits)==number and word.x>=minX and word.x<=maxX
            and word.y>=(minY or 0) and word.y<=(maxY or 719) then return word end
      end
    end
    local function stageNumber(text)
      return tonumber((text or ''):gsub('[%s|]',''):gsub('階','阶'):match('^阶段(%d+)$'))
    end
    local tab,returnedFromStage
    for attempt=1,12 do
      local screen=_soloEventUi()
      if utils.specialMissionContinue(screen) then return 1 end
      if not returnedFromStage and (screen.scene=='missionRewards' or screen.scene=='specialRanking') then
        print('当前阶段与配置不符或阶段未读清，返回章节列表重新选择')
        keyPress('back')
        returnedFromStage=true
        sleep(750)
      end
      tab=numberedWord(screen,'章节',chapter,0,370)
      if tab then break end
      sleep(500)
    end
    if not tab then error('未找到称霸章节 '..chapter) end
    if spStage<1 or spStage%1~=0 then error('称霸阶段必须为正整数') end
    tapUiWord(tab)
    sleep(900)

    local function stagePage()
      local screen=_soloEventUi(false,true)
      local rail=screen.stageRail
      if not rail or math.abs(rail.y-tab.y)>32 then return end
      local badges={}
      local leading
      for _,word in ipairs(screen.words or {}) do
        local number=stageNumber(word.text)
        if number and word.x>=rail.x+15 and word.x<=1240 and word.y>=185 and word.y<=310 then
          local badge={x=word.x,y=word.y,number=number}
          badges[number]=badge
          if not leading or badge.x<leading.x then leading=badge end
        end
      end
      local first
      local nearRail=leading and leading.x>=rail.x+32 and leading.x<=rail.x+114
      -- 章节里的第一张卡不一定叫「阶段1」：阶段编号在整条赛事里连续编排，
      -- 例如第 2 章从「阶段6」开始。最左一张贴着章节栏且没被裁切，就是列起点。
      if rail.clear and (nearRail or stageNumber(screen.stageFirstLabel)==1) then
        first={x=rail.x+142,y=430}
      end
      local target=spStage>1 and badges[spStage] or nil
      if spStage>1 and not target then
        -- A badge border can add a trailing 1. Keep this fallback tied to a
        -- neighboring number, and require a stage label on the destination.
        local badge=badges[spStage*10+1]
        local before=badges[spStage-1]
        local after=badges[spStage+1]
        if badge and ((before and before.x<badge.x) or (after and after.x>badge.x)) then
          badge.inferred=true
          target=badge
        end
      end
      return {frame=screen.frameId,rail=rail,leading=leading,first=first,target=target}
    end
    local function sameBadge(a,b)
      if not a or not b then return a==b end
      return a.number==b.number and math.abs(a.x-b.x)<=5 and math.abs(a.y-b.y)<=5
    end
    local function samePage(a,b)
      return a and b and a.rail.clear==b.rail.clear and math.abs(a.rail.x-b.rail.x)<=3
        and (a.first and b.first or not a.first and not b.first and sameBadge(a.leading,b.leading))
        and sameBadge(a.target,b.target)
    end
    local function settledPage()
      local deadline=tickCount()+6000
      local previous
      repeat
        local page=stagePage()
        if page and page.frame and previous and previous.frame and page.frame>previous.frame and samePage(page,previous) then
          return page
        end
        if not page or not previous or page.frame and page.frame~=previous.frame then previous=page end
        sleep(250)
      until tickCount()>=deadline
      error('未确认称霸章节 '..chapter..' 的阶段列表，已保留当前任务')
    end

    local page=settledPage()
    local deadline=tickCount()+45000
    local drags,stalled=0,0
    -- 配置的阶段已经在可视区（卡片未被裁切）就直接选，不必先拖回章节列表最左端：
    -- 第 2 章的第一张卡通常就是「阶段6」，原来「最左必须是阶段1」的判据会一直等不到。
    local atOrigin=page.target~=nil
    if atOrigin then print('阶段 '..spStage..' 已在可视区，直接选中') end
    while not atOrigin and tickCount()<deadline and stalled<5 do
      if page.first then
        -- Verify the same first card after a short right pull and its rebound.
        swipe(290,405,290,670,520,140)
      else
        swipe(290,377,290,1155,330,120)
      end
      drags=drags+1
      sleep(1);sleep(550)
      local nextPage=settledPage()
      local unchanged=samePage(page,nextPage)
      if page.first and nextPage.first and unchanged then
        atOrigin=true
        page=nextPage
        print('称霸阶段列表已确认最左起点（右拖 '..drags..' 次）')
        break
      end
      stalled=unchanged and stalled+1 or 0
      page=nextPage
    end
    if not atOrigin then error('未确认称霸阶段列表最左起点，已保留当前任务') end

    local stage=spStage==1 and page.first or page.target
    stalled=0
    for attempt=1,12 do
      if stage then break end
      swipe(290,1050,290,620,450,120)
      sleep(1);sleep(550)
      local nextPage=settledPage()
      stalled=samePage(page,nextPage) and stalled+1 or 0
      if stalled>=2 then break end
      page=nextPage
      stage=page.target
    end
    if not stage then error('当前章节未找到阶段 '..spStage..'，已保留当前任务') end
    tapUiWord(stage)
    local enterDeadline=tickCount()+15000
    repeat
      sleep(500)
      local current=_soloEventUi()
      if utils.specialMissionContinue(current) or
          (not stage.inferred and (current.scene=='car' or current.scene=='cars')) then
        print('已确认称霸章节 '..chapter..'，阶段 '..spStage)
        return 1
      end
    until tickCount()>=enterDeadline
    error('未能确认进入称霸阶段 '..spStage..'，已保留当前任务')
  elseif cmpColorEx("580|161|FFFFFF-101010,607|160|FFFFFF-101010,610|1238|000000-101010,634|1229|190F0A-101010", 1) == 1 then
    return 1
  else
    for i = 1, 7 do
      swipe(60, 250, 60, 1200, 100)
      sleep(500)
    end
  end
  sleep(1500)
  local nowColor, nextColor = "D4D4D4", "FFFFFF"
  local ret, x, y = -1, -1, -1
  ret, x, y = _findColorTriple(490, 50, 494, 400, "D4D4D4-050505", 0, 1.0)
  local noHelp = false
  if x == -1 then
    nowColor, nextColor = "FFFFFF", "FFFFFF"
    noHelp = true
    print("没有帮助包")
  end
  if spStage == 1 then
    tap(400, 370)

  elseif spStage == 2 then
    tap(400, 720)

  elseif spStage == 3 then
    tap(400, 1150)

  else
    for i = 4, spStage do
      keepCapture()
      x, y = findMultiColor(494, 500, 494, 960, nextColor, "0|20|" .. nextColor .. "|0|50|" .. nextColor, 0, 1)
      if y ~= -1 then
        utils.mySwipe(46, y + 20, 46, 310)
        sleep(1000)
      elseif UIconfig.starWay ~= "" then
        utils.mySwipe(30, 720, 30, 310)
        sleep(1000)
      end
    end
    tap(400, 1150)

  end
  for attempt=1,12 do
    sleep(350)
    if utils.specialMissionContinue(_soloEventUi()) then return 1 end
  end
  error('未能确认进入特殊赛事阶段 '..spStage..'，已保留当前任务')
end
--- 车手联会任务页：横条上有「任务」章节卡和带「危险等级」的任务行；以文字为准，不依赖旧配色。
function utils.carClubTaskPage(screen)
  if not screen or screen.specialSeasonTab then return false end
  local text=tostring(screen.text or ''):gsub('[%s|]','')
  if text:find('危险等级',1,true) or text:find('危險等級',1,true) then return true end
  return text:find('下一奖励',1,true)~=nil and text:find('任务',1,true)~=nil
    or text:find('下一獎勵',1,true)~=nil and text:find('任務',1,true)~=nil
end
function utils.carClubPage(pattern)
  keepCapture()
  if pattern and cmpColorEx(pattern,1)==1 then return true end
  return utils.carClubTaskPage(_soloEventUi())
end
--- 把章节横条拉回最左起点：向右滑到画面连续两次都不再变化为止，不靠固定次数。
-- utils.carClubAtOrigin 标记横条在起点：此时 spChoose 不做横向微调；club.lua 向后翻章节时清除。
-- 一次滑动只走 850px（约 1.85 张卡），所以位置越靠右需要的次数越多；40 次够 70 张卡以上。
-- 走满次数仍在动就报错保留当前任务，绝不会在没确认起点的情况下开始选章节。
function utils.carClubToOrigin()
  local function signature()
    -- 每次都重新截屏：色键分发留下的缓存帧不会随滑动更新。
    keepCapture()
    local sig={}
    -- 采样行必须避开卡片图本身：卡片有呼吸/高光动画，落在图上的点一直在变，
    -- 会把「已经到起点」判成「还在滑动」，一路跑到次数上限。
    for _,y in ipairs({250,300,600}) do
      for x=100,1180,60 do sig[#sig+1]=getPixelColor(719-y,x) end
    end
    return sig
  end
  local function moved(a,b)
    local changed=0
    for k=1,#a do
      local r1,g1,b1=tonumber(a[k]:sub(1,2),16),tonumber(a[k]:sub(3,4),16),tonumber(a[k]:sub(5,6),16)
      local r2,g2,b2=tonumber(b[k]:sub(1,2),16),tonumber(b[k]:sub(3,4),16),tonumber(b[k]:sub(5,6),16)
      if math.abs(r1-r2)>40 or math.abs(g1-g2)>40 or math.abs(b1-b2)>40 then changed=changed+1 end
    end
    -- 真滑一屏会整体平移，变化点远超 20%；卡片动画只影响少量点，够不着阈值。
    return changed>#a*0.2
  end
  -- 手指压在卡片上会把任务面板误点开：面板盖住横条后横条怎么滑都不动，
  -- 会被当成“已经到起点”，后面从起点扫描就会漏掉左边没看到的章节。所以先收回面板。
  local function closePanelIfOpen()
    local screen = _soloEventUi()
    if #utils.carClubCards(screen) > 0 then return end
    local close = screen and screen.closeX
    if close then
      tap(719 - math.floor(close.y), math.floor(close.x))
    else
      tap(719 - 110, 573)
    end
    sleep(1200)
  end
  local still = 0
  for i=1,40 do
    -- 只在画面已经不动过一次时才花时间读屏查面板，正常滑动不多付 OCR 开销。
    if still >= 1 then closePanelIfOpen() end
    local before=signature()
    -- 必须在横屏 y≈600 这一行上滑：y≈659 贴着卡片下沿，拖不动章节条。
    swipe(119,300,119,1150,400)
    -- 到起点后松手会回弹，多等一会再比较，避免把回弹当成还在移动。
    sleep(1500)
    if moved(before,signature()) then
      still = 0
    else
      still = still + 1
      -- 连续两次不动才认定到起点：单次不动可能是松手回弹或这一下手指被吃掉。
      if still >= 2 then
        print('车手联会章节卡片已回到起点（第 '..i..' 次滑动后连续两次不再移动）')
        utils.carClubAtOrigin=true
        return
      end
    end
  end
  error('车手联会章节卡片未能回到起点，已保留当前任务')
end
function utils.intoSpecial1(screen)
  data["信息"] = "进入车手联会"
  local position=tonumber(run.data["车手联会位置"]) or 1
  local preview=utils.openSpecialEntry(screen,position,{label='车手联会',entered=utils.carClubTaskPage,enterOnce=true})
  if not utils.carClubTaskPage(preview) then
    local deadline=tickCount()+15000
    repeat
      sleep(400)
      preview=_soloEventUi()
    until utils.carClubTaskPage(preview) or tickCount()>=deadline
  end
  if not utils.carClubTaskPage(preview) then
    if preview.specialSeasonTab then
      error('已点击赛季赛事第 '..position..' 项的「进入」，但未打开车手联会页面，已保留当前任务')
    end
    error('赛季赛事第 '..position..' 项进入后不是车手联会页面（当前：'..tostring(preview.scene)..'），请检查「车手联会位置」')
  end
  print('已进入车手联会（赛季赛事第 '..position..' 项）')
  utils.carClubToOrigin()
end
-- 界面识别以文字为准，不以配色为准：换肤/改配色后文字仍然可定位。
-- 返回区域内命中任一关键字的词框（config 坐标），按屏幕从上到下排序。
function utils.textWordsIn(x1, y1, x2, y2, patterns)
  local found = {}
  if not _textWords then return found end
  keepCapture()
  local ok, words = pcall(_textWords, x1, y1, x2, y2)
  if not ok or type(words) ~= "table" then return found end
  for _, w in ipairs(words) do
    local text = tostring(w.text or ""):gsub("%s+", "")
    for _, p in ipairs(patterns) do
      if string.find(text, p, 1, true) then
        found[#found + 1] = { x = w.x, y = w.y, text = text }
        break
      end
    end
  end
  table.sort(found, function(a, b) return a.x > b.x end)
  return found
end

-- 失败时把区域里 OCR 看到的文字打到日志，便于区分“界面变了”和“识别没对上”。
function utils.logTextWords(x1, y1, x2, y2)
  local words = utils.textWordsIn(x1, y1, x2, y2, { "" })
  local parts = {}
  for _, w in ipairs(words) do
    parts[#parts + 1] = w.text .. "@" .. math.floor(w.x) .. "," .. math.floor(w.y)
  end
  print("OCR(" .. x1 .. "," .. y1 .. "," .. x2 .. "," .. y2 .. "): " .. table.concat(parts, " | "))
end

-- 开始/继续按钮：先用文字定位，找不到再退回原来的固定坐标。
-- allowSide 为真才允许点「开始任务」：主线分支点到它就是配了主线却跑支线。
function utils.startRaceTaps(allowSide)
  local patterns = allowSide and { "开始任务", "開始任務", "继续", "繼續" } or { "继续", "繼續" }
  local rows = utils.textWordsIn(60, 700, 640, 1279, patterns)
  local sx, sy = 116, 1100
  if #rows > 0 then
    sx, sy = rows[1].x, rows[1].y
  end
  tap(sx, sy)
  sleep(1500)
  tap(sx, sy)
  sleep(1000)
  tap(sx, sy)
end

--- 车手联会章节横条：每张章节卡标题行的大字「任务」（横屏坐标，词框中心），按从左到右排序。
function utils.carClubCards(screen)
  local cards = {}
  for _,word in ipairs(screen.words or {}) do
    local label=tostring(word.text or ''):gsub('[%s|]',''):gsub('務','务')
    if label=='任务' and word.y>230 and word.y<330 then
      local card={x=word.x,y=word.y,name=''}
      for _,other in ipairs(screen.words or {}) do
        local text=tostring(other.text or ''):gsub('%s','')
        if other~=word and math.abs(other.x-word.x)<260 and other.y>=word.y-80 and other.y<=word.y-25 and text:find('%d') then
          card.name=text
        end
      end
      cards[#cards+1]=card
    end
  end
  table.sort(cards,function(a,b) return a.x<b.x end)
  return cards
end

-- 开赛页上「这辆车的性能分进不了当前难度」的提示。车联按危险等级卡性能分，点开始会一直没反应。
function utils.carScoreShort(screen)
  local text = tostring(screen and screen.text or '')
  if text == '' then return false end
  return text:find('性能分不足', 1, true) ~= nil or text:find('性能分不够', 1, true) ~= nil
      or text:find('性能分未达标', 1, true) ~= nil or text:find('性能分达不到', 1, true) ~= nil
end

-- 车手联会选任务：先把章节横条拉回最左起点，再从起点向右逐张看章节卡，取当前进度该做的任务。
-- 章节卡上「a/b」且 a<b 说明这条线还在进行；带「特别任务」的卡可以打特别任务。
-- 主线 = 点开章节卡后面板里带「危险等级」的任务行（按「危险等级」配置选行）；支线 = 面板里的特别任务（「开始任务」）。
-- 顺序按「先支线后主线」：先把整条横条的支线扫一遍，扫完没有可做的支线，才回起点扫主线。
-- 卡片上的 a/b 只用来初筛（省掉一次开卡），真正算数的是点开后的面板：面板是支线就刷支线，是难度行就刷主线。
-- 返回 1 已点开始；-1 从起点翻到末尾也没有可做的任务；0 没认出章节卡（界面被遮挡或加载中）；
-- 'noTicket' 危险值（车联门票）不足的弹窗盖住了章节条，本项目这轮做不了。
-- maxLevel：本辆车能打的最高危险等级（性能分不足降档后传入），不传就按配置来。
function utils.spChoose(maxLevel)
  local fz = utils.defultCfg(run.data["分支选择"], "0")
  local wxdj = utils.defultCfg(run.data["危险等级"], "0")
  local ndbz = utils.defultCfg(run.data["难度不足"], "0")
  local cap = tonumber(maxLevel)
  local wantSide = fz == "0" or fz == "2"
  local wantMain = fz == "1" or fz == "2"
  -- 回起点放在后面：降档退回时面板可能就在眼前，先拉横条会把面板滑走。
  -- OCR 会把繁体、异体字混着读（「特別任务」「遠度纪元」），统一成简体再做关键字判断。
  local function clean(w)
    return tostring(w.text or ''):gsub('[%s|]','')
      :gsub('別','别'):gsub('務','务'):gsub('級','级'):gsub('險','险')
      :gsub('贏','赢'):gsub('賽','赛'):gsub('開','开'):gsub('繼','继'):gsub('續','续')
  end
  local function cardWords(screen,card)
    local found={}
    for _,w in ipairs(screen.words or {}) do
      -- 卡片比标题宽：右侧的 SP 分数和「特别任务 a/b」贴着卡片右边缘，范围卡太紧会漏读。
      if w.x>=card.x-100 and w.x<=card.x+420 and w.y>=320 and w.y<=690 then found[#found+1]=w end
    end
    return found
  end
  -- 卡片上有两条 a/b：主线进度在「特别任务」标签上方，标签下方那条是特别任务自己的进度，
  -- 主线已经 a==b 做完时不能被它顶成「主线还有剩余」。
  -- 标签 OCR 偶尔读不到，这时按版面取：上面的 a/b 算主线，下面的 a/b 算支线，
  -- 否则会把支线的 0/54 当成主线没做完，把支线章节点成主线再跳过。
  -- 整章都做完的卡上一条 a/b 都没有（只有一个完成勾），这时不能靠进度判候选。
  local function cardProgress(words)
    local specialY
    for _,w in ipairs(words) do
      local text=clean(w)
      -- 标签会被 OCR 少认一个字（「特別任」），只取「特别任」这一段就能盖住。
      if text:find('特别任',1,true) or text:find('特殊任务',1,true) then specialY=w.y end
    end
    local rows={}
    local hasDigits = false
    for _,w in ipairs(words) do
      local text=clean(w)
      -- 进度会和图标、前缀被读成一个词（「尽0/54」「SP2388/2388」），不能整词匹配。
      local a,b=text:match('(%d+)/(%d+)')
      if a then rows[#rows+1]={y=w.y,left=tonumber(b)-tonumber(a)} end
      if text:find('%d') then hasDigits = true end
    end
    table.sort(rows,function(x,y) return x.y<y.y end)
    local mainLeft, sideLeft
    if specialY then
      for _,row in ipairs(rows) do
        if row.y<specialY then mainLeft=row.left else sideLeft=sideLeft or row.left end
      end
    else
      mainLeft=rows[1] and rows[1].left
      sideLeft=rows[2] and rows[2].left
    end
    return mainLeft, sideLeft, specialY~=nil, #rows>0, hasDigits
  end
  -- 面板类型按排列形式判定（等价于原版用固定色块区分主线/支线，但不依赖配色）：
  -- 主线面板是难度行列表（列表区每行一条「危险等级 N」，在右侧列表区），支线面板只有一条任务（目标 + 底部一个按钮）。
  -- 所以先数难度行：有行就是主线；一行都没有，再看是不是支线的目标文字或单个开始/继续按钮。
  local function panelShape(words)
    local side, rows = nil, {}
    for _,word in ipairs(words) do
      local label=clean(word)
      -- 支线面板左栏也印着「危险等级」，但它贴在左边缘；只有右侧列表区的才是主线任务行。
      if label:find("危险等级",1,true) and word.x>450 and word.y<640 then
        rows[#rows+1]=word
      end
    end
    if #rows == 0 then
      for _,word in ipairs(words) do
        local label=clean(word)
        if label:find("开始任务",1,true) or label:find("赢取比赛",1,true)
            or label:find("赢取额外车联包",1,true) or label=="继续" or label=="开始" then
          side=word
          break
        end
      end
    end
    return side, rows
  end
  -- 选完支线/主线把结果弹到屏幕底部（和任务提示条同一位置），不只是写日志。
  local function announce(message)
    data['信息']=message
    if _taskNotice then _taskNotice(message,2500) end
    toast(message)
  end
  -- 任务行自己的危险等级：行词里带的数字优先，其次同一行右侧 250 像素内的一位数字（等级 1-3，避开 SP 分数）。
  -- 只认 1-3：支线面板左栏也印着「危险等级 0」，读到 0 说明这行不是主线任务行。
  local function levelOf(row, words)
    local digit = clean(row):match('(%d+)')
    if digit and tonumber(digit) >= 1 and tonumber(digit) <= 3 then return tonumber(digit) end
    local best
    for _,w in ipairs(words or {}) do
      if w ~= row and math.abs(w.y - row.y) <= 30 and w.x > row.x and w.x <= row.x + 250 then
        local t = clean(w)
        if t == "1" or t == "2" or t == "3" then
          if not best or math.abs(w.x - row.x) < math.abs(best.x - row.x) then best = w end
        end
      end
    end
    return best and tonumber((clean(best))) or nil
  end
  -- 危险值（车联门票）不足的弹窗会盖住整条章节横条：文字里出现「空危险槽/清空所有槽位」就直接报门票不足，
  -- 不要落到「未识别到章节卡」再去回主重试白跑一轮。
  local function hazardDialog(screen)
    if screen.scene == 'noTicket' then return true end
    for _,w in ipairs(screen.words or {}) do
      local t=clean(w)
      if t:find('空危险槽',1,true) or t:find('空危險槽',1,true)
          or t:find('清空所有槽位',1,true) or t:find('清空全部槽位',1,true) then return true end
    end
    return false
  end
  local skipped = {}
  -- 阶段顺序就是「先支线后主线」：整条横条的支线扫完没有可做的，才回起点扫主线。
  local stages = {}
  if wantSide then stages[#stages+1] = 'side' end
  if wantMain then stages[#stages+1] = 'main' end
  -- 展开的章节面板要能收回章节横条，才能接着看同一屏右侧的卡。
  -- 先看一眼是不是本来就在横条上（卡片点击可能没生效），别把整个车联页面退掉。
  -- 不能按返回键：任务面板上的返回会直接退到赛事页（整个车联页都退掉了），只能点面板自己的 ×。
  local function closeCards()
    local screen = _soloEventUi()
    if #utils.carClubCards(screen) > 0 then return screen end
    for attempt = 1, 2 do
      local close = screen and screen.closeX
      if close then
        tap(719 - math.floor(close.y), math.floor(close.x))
      else
        -- 面板右上角 × 的固定位置（横屏 573,110），识别不到关闭按钮时兜底。
        tap(719 - 110, 573)
      end
      for wait = 1, 2 do
        sleep(900)
        screen = _soloEventUi()
        if #utils.carClubCards(screen) > 0 then return screen end
      end
    end
    return nil
  end
  -- 在一张主线面板里按配置选难度行：先按等级数字找，找不到再按行序兜底。
  -- 返回 选中的行, 提示文字；本车连面板里最低档都打不了时返回 nil（不要再点，交回上层）。
  local function pickMainRow(rows, words)
    for _,row in ipairs(rows) do row.level = levelOf(row, words) end
    table.sort(rows, function(a, b) return a.y < b.y end)
    local levels = {}
    for _,row in ipairs(rows) do
      if row.level then levels[#levels+1] = row.level end
    end
    local want = (tonumber(wxdj) or 0) + 1
    -- 降档后不能超过这辆车能打的等级（否则点进去还是「性能分不足」）。
    if cap and cap >= 1 and want > cap then want = cap end
    -- 配置「难度N」按行上的等级数字找，不按行序：只有两档的章节会把难度3错位成最上面那行。
    for _,row in ipairs(rows) do
      if row.level == want then return row, "刷主线（难度" .. want .. "）" end
    end
    -- 读到等级但全都超过本车上限：这章打不了，不要在面板上乱点。
    if cap and cap >= 1 and #levels > 0 then
      local allowed = false
      for _,row in ipairs(rows) do
        if row.level and row.level <= cap then allowed = true break end
      end
      if not allowed then return nil, "本章节没有本车能打的难度" end
    end
    if #rows == 1 then return rows[1], "刷主线（本章节只有 1 档）" end
    if #levels > 0 then
      table.sort(levels)
      local target
      if cap and cap >= 1 then
        for i = #levels, 1, -1 do if levels[i] <= cap then target = levels[i] break end end
      end
      target = target or (ndbz == "1" and levels[#levels] or levels[1])
      for _,row in ipairs(rows) do
        if row.level == target then
          return row, "本章节只有 " .. table.concat(levels, "、") .. " 档，已选难度" .. target
        end
      end
      return (ndbz == "1" and rows[#rows] or rows[1]), "本章节只有 " .. table.concat(levels, "、") .. " 档"
    end
    -- 等级数字一个都没读到：退回行序，仍按「难度不足」取最低/最高那行。
    local index = want
    if index > #rows then index = ndbz == "1" and #rows or 1; sleep(1500) end
    return rows[index], (index == 1 and #rows > 1) and "少于三个难度时选最上面的难度" or "刷主线"
  end
  -- 降档重选时提示里必须带上原因：用户只看「刷主线（难度2）」不知道刚才为什么降了档。
  local cause = (cap and tonumber(data['车联等级'])) and "性能分不足，" or ""
  -- 性能分不足退回一层后难度行往往还在眼前：就地在这张面板里改选低一档，
  -- 不要退回章节横条从起点重扫（那样每降一档都要把整条横条再扫一遍）。
  -- 只有“上一轮已经选过行（data['车联等级'] 有值）又带降档上限”才可能是这种情况。
  if wantMain and cap and tonumber(data['车联等级']) then
    local current = _soloEventUi()
    local side, rows = panelShape(current.words or {})
    if not side and #rows > 0 then
      local pick, note = pickMainRow(rows, current.words or {})
      if pick then
        print("车联：难度不足，在当前任务面板上改选")
        tap(719 - pick.y, pick.x)
        sleep(800)
        data['车联等级'] = pick.level or ((tonumber(wxdj) or 0) + 1)
        announce(cause .. note)
        utils.startRaceTaps(false)
        return 1
      end
    end
  end
  -- 不是降档退回的面板，就先把横条拉回最左起点再从头看。
  if not utils.carClubAtOrigin then utils.carClubToOrigin() end
  for index, stage in ipairs(stages) do
    -- 支线扫到横条末尾再回起点找主线；没翻过屏就说明本来就在起点。
    if index > 1 and not utils.carClubAtOrigin then utils.carClubToOrigin() end
    -- step 是读屏次数预算：翻一屏算一次，面板收起后重读同一屏也占一次。
    local lastKey
    for step = 1, 60 do
      local screen, cards
      for attempt = 1, 3 do
        screen = _soloEventUi()
        if hazardDialog(screen) then
          print("车联第 " .. step .. " 屏（" .. stage .. "）：危险值不足")
          return 'noTicket'
        end
        cards = utils.carClubCards(screen)
        if #cards > 0 then break end
        sleep(600)
      end
      if #cards == 0 then
        -- 已经停在开赛页且性能分不够：这不是“界面变了”，要交回上层降一档重选，
        -- 否则会被当成未识别章节卡，回主重试后再撞一次同一个难度。
        if utils.carScoreShort and utils.carScoreShort(screen) then
          data["信息"] = "车辆性能分不足，按难度不足配置降档"
          -- 就在开赛页上先把原因提示出来，别让用户只看到画面自己退回去。
          toast(data["信息"])
          print("车联：性能分不足，交回上层降档")
          return 'noScore'
        end
        -- 翻屏时手指压在卡片上会把任务面板误点出来：面板有「危险等级/开始任务/赢取比赛」这几个字，
        -- 点 × 收回横条再看一次，不要直接当“没识别到章节卡”报异常（回去重试会反复卡在同一屏）。
        local panelOpen = false
        for _,w in ipairs(screen.words or {}) do
          local t = clean(w)
          if t:find('危险等级',1,true) or t:find('开始任务',1,true) or t:find('赢取比赛',1,true) then
            panelOpen = true
            break
          end
        end
        if panelOpen then
          screen = closeCards()
          if screen then cards = utils.carClubCards(screen) end
        end
      end
      if #cards == 0 then
        data["信息"] = "车联任务列表未识别到章节卡"
        print("车联任务列表未识别到章节卡")
        return 0
      end
      -- 只看整张都在屏幕里的章节卡（标题距左边至少 95 像素），被左边裁掉的留到下一屏再看。
      -- 右边被裁掉一部分的卡可以直接点：点击点取 card.x+150 仍落在卡内，等下一屏反而要多翻一次屏，
      -- 翻屏时手指压在卡片上会把面板/开赛页误点出来。
      local key, ordered = {}, {}
      for _,card in ipairs(cards) do
        key[#key+1] = card.name .. '@' .. math.floor(card.x/40)
        if card.x >= 95 and card.x <= 1240 then ordered[#ordered+1] = card end
      end
      table.sort(ordered, function(a, b) return a.x < b.x end)
      local names = {}
      for _,card in ipairs(cards) do names[#names+1] = card.name .. '@' .. math.floor(card.x) end
      print("车联第 " .. step .. " 屏（" .. stage .. "）：卡片 " .. table.concat(names, ","))
      -- 从左到右看这一屏的卡：某张卡的这一支被判掉后继续看同屏右侧的卡，不整屏放弃。
      local opened = false
      for _,card in ipairs(ordered) do
        local name = card.name ~= '' and card.name or ('y' .. math.floor(card.y))
        local mark = name .. '#' .. stage
        if not skipped[mark] then
          local words = cardWords(screen, card)
          local mainLeft, sideLeft, special, hasProgress, hasDigits = cardProgress(words)
          -- a/b 只做初筛省一次开卡，真正算数的是点开后的面板。
          -- 必须读到 a/b 进度才算候选：锁定章节卡上也有数字（SP 分数、「下一奖励」），
          -- 但那是分数不是进度，点它面板根本打不开，只会白开一次卡。
          -- 全做完的章节卡上一条 a/b 都没有（只有完成勾），同样不会是候选。
          -- 单行 a/b 且没有「特别任务」标签时归属不明，两支都当候选交给面板裁决。
          local unclear = hasProgress and not special
              and ((mainLeft ~= nil) ~= (sideLeft ~= nil))
          local open = (stage == 'side' and ((sideLeft ~= nil and sideLeft > 0)
                or (special and sideLeft == nil and hasProgress) or unclear))
              or (stage == 'main' and ((mainLeft ~= nil and mainLeft > 0) or unclear))
          print("车联卡（" .. stage .. "）" .. name .. "：主线 " .. tostring(mainLeft) .. " 支线 " ..
            tostring(sideLeft) .. (special and " 有标签" or "") .. (hasProgress and "" or " 无进度") ..
            (hasDigits and "" or " 无数字") .. (open and " → 开卡" or " → 跳过"))
          if not open then
            skipped[mark] = true
            if not hasProgress then
              local texts = {}
              for _,w in ipairs(words) do texts[#texts+1] = clean(w) .. '@' .. math.floor(w.x) .. ',' .. math.floor(w.y) end
              print("车联卡区文字（" .. name .. "）：" .. table.concat(texts, " | "))
            end
          else
            tap(719 - (card.y + 100), math.min(card.x + 150, 1200))
            local side, rows, panelWords = nil, {}, {}
            for read = 1, 2 do
              sleep(read == 1 and 1300 or 1500)
              local panel = _soloEventUi()
              if hazardDialog(panel) then
                print("车联第 " .. step .. " 屏：" .. name .. "，面板：危险值不足")
                return 'noTicket'
              end
              panelWords = panel.words or {}
              side, rows = panelShape(panelWords)
              -- 面板展开有动画：第一次没读到内容时先等一下再读一遍，不急着换下一张卡。
              if side or #rows > 0 then break end
            end
            -- 每行自己的危险等级数字：行词里带的数字，或同一行右侧最近的一位数字词。
            local levels = {}
            for _,row in ipairs(rows) do
              row.level = levelOf(row, panelWords)
              if row.level then levels[#levels+1] = row.level end
            end
            print("车联第 " .. step .. " 屏（" .. stage .. "）：" .. name .. "，面板：" ..
              (side and "特别任务" or (#rows > 0 and "主线任务行 " .. #rows .. " 条" ..
                (#levels > 0 and "（等级 " .. table.concat(levels, "、") .. "）" or "") or "未识别")))
            if not side and #rows == 0 then
              local texts = {}
              for _,w in ipairs(panelWords) do
                texts[#texts+1] = clean(w) .. '@' .. math.floor(w.x) .. ',' .. math.floor(w.y)
              end
              print("车联面板文字：" .. table.concat(texts, " | "))
            end
            if side and stage == 'side' then
              announce("刷支线")
              utils.startRaceTaps(true)
              return 1
            elseif stage == 'side' and not side and #levels == 0
                and ((sideLeft ~= nil and sideLeft > 0) or (special and sideLeft == nil)) then
              -- 卡片上写着支线还有剩余，但面板文字没认出来（目标/按钮文案不固定）：
              -- 原版根本不看面板，直接点开始；这里同样按支线开始，不让这张卡被白白跳过。
              announce("刷支线")
              utils.startRaceTaps(true)
              return 1
            elseif (not side) and #rows > 0 and stage == 'main' then
              local pick, note = pickMainRow(rows, panelWords)
              if not pick then
                print("车联第 " .. step .. " 屏（" .. stage .. "）：" .. name .. "，" .. tostring(note))
                data["信息"] = tostring(note)
                skipped[mark] = true
                if not closeCards() then
                  data["信息"] = "车联面板收不回车联章节列表，本轮跳过"
                  print(data["信息"])
                  return 0
                end
                opened = true
                break
              end
              tap(719 - pick.y, pick.x)
              sleep(800)
              -- 记住这次选的等级：开赛失败（性能分不足）时从这里往下降一档。
              data['车联等级'] = pick.level or ((tonumber(wxdj) or 0) + 1)
              announce(cause .. note)
              -- 主线分支只能点难度行/固定开始点，永远不点「开始任务」。
              utils.startRaceTaps(false)
              return 1
            end
            -- 面板不是这一阶段要的分支（卡片 a/b 读反了）：只判掉这张卡的这一支，退回横条接着看同屏的卡。
            skipped[mark] = true
            if not closeCards() then
              data["信息"] = "车联面板收不回车联章节列表，本轮跳过"
              print(data["信息"])
              return 0
            end
            opened = true
            break
          end
        end
      end
      local signature = table.concat(key, '|')
      if opened then
        -- 面板刚收起，同一屏重新读一遍再继续，不算翻屏。
      elseif lastKey == signature then
        break  -- 横条翻到底，不再前进
      else
        lastKey = signature
        -- 这一屏没有本阶段要做的任务：按住拖动向后翻一屏（不甩动），避免跳过章节卡
        utils.carClubAtOrigin = false
        utils.mySwipe(119, 760, 119, 330)
        sleep(1300)
      end
    end
  end
  data["信息"] = "车联任务列表未找到可进入的危险等级"
  toast(data["信息"])
  print(data["信息"])
  return -1
end
function utils.getClearPrize()
  if UIconfig.clearPrize ~= "" and os.date("%H") == 0 and os.date("%M") < 30 then
    data["下次扫荡时间"] = os.time()
    sleep(1000)
  end
  if UIconfig.clearPrize == "" or os.time() < data["下次扫荡时间"] then
    return 0
  end
  data["信息"] = "至尊通行证领取奖励"
  for i = 1, 100 do
    if utils.mycolor(580, 169, "5400FF-101010") == 1 then
      sleep(1000)
      break
    end
    if 30 < i then
      tap(20, 200)
      sleep(1000)
      for i = 1, 8 do
        swipe(50, 200, 50, 1000, 100)
        sleep(100)
      end
      sleep(1000)
      if utils.mycolor(16, 150, "12FBC3-101010") == 0 then
        tap(100, 150)
      end
      sleep(1000)
      break
    end
    sleep(100)
  end
  local x = -1
  y = -1
  for i = 1, 6 do
    keepCapture()
    ret, x, y = _findColorTriple(16, 50, 16, 1200, "12FAC2-101010", 0, 1.0)
    if utils.checkClear(y) == 0 and i < 6 then
      tap(160, y + 220)
      sleep(500)
    end
  end
  local dx, dy
  for i = 1, 15 do
    keepCapture()
    ret, dx, dy = _findColorTriple(16, 50, 16, 1200, "12FAC2-101010", 0, 1.0)
    if -1 < dy then
      dy = 994 - dy
    end
    utils.mySwipe(22, 1195, 22, 982 + dy)
    sleep(500)
    if utils.mycolor(60, 1147, "12FBC3-101010") ~= 1 then
      tap(100, 1120)
      sleep(1000)
    end
    keepCapture()
    ret, x, y = _findColorTriple(16, 50, 16, 1200, "12FAC2-101010", 0, 1.0)
    if utils.checkClear(y) == 0 then
    end
  end
  for i = 1, 8 do
    swipe(50, 200, 50, 1000, 100)
    sleep(100)
  end
  sleep(3000)
  if utils.mycolor(16, 150, "12FBC3-101010") == 0 then
    tap(100, 150)
  end
  sleep(1000)
  data["下次扫荡时间"] = os.time() + 36000
end
function utils.checkClear(y)
  local temp = 0
  for i = 1, 3 do
    if utils.mycolor(60, y + 30, "0BE4FF-201010") == 1 then
      temp = 1
      break
    end
    sleep(200)
  end
  if temp == 0 then
    return 0
  else
    tap(160, y + 30)
    sleep(500)
    tap(160, y + 30)
    sleep(3000)
    if utils.mycolor(60, 1200, "00D4FF-151510") == 0 then
      tap(518, 811)
      sleep(3000)
    end
    tap(60, 1000)
    sleep(3000)
    for i = 1, 100 do
      tap(30, 1240)
      sleep(300)
      if utils.mycolor(60, 1200, "12FBC3-101010") == 1 or utils.mycolor(612, 122, "5400FF-101010") == 1 then
        break
      end
    end
    utils.backToCup()
    return 1
  end
end
function utils.backToCup()
  keepCapture()
  if cmpColorEx(cup.selectMission, 1) == 0 then
    for i = 1, 30 do
      keepCapture()
      if cmpColorEx(cup.selectMission, 1) == 1 then
        break
      end
      keyPress("back")
      sleep(4000)
    end
  end
  releaseCapture()
end
function utils.tipsHandle()
  keepCapture()
  if _handleLegacyTips and _handleLegacyTips() then keepCapture() end
  if _popupUi then _popupUi() end
end
function utils.initCar()
  data["用车情况"] = {
    0,
    0,
    0
  }
  data["用车次数"] = {}
end
function utils.dailyPrize()
  data["信息"] = "每日战利品奖励检测"
  if _taskNotice then _taskNotice('正在检测俱乐部奖励',2500) end
  local tempCar = utils.defultCfg(run.data["杯赛用车"], {
    1,
    2,
    3,
    4
  })
  -- Reward collection must not rewrite the user's ordered car entries.
  local mission = {}
  for i,car in ipairs(tempCar) do mission[i]=car end
  tap(526, 1078)
  sleep(1000)
  tap(530, 810)
  sleep(1000)
  -- The single fixed color probe missed the claim button after layout shifts.
  -- Read the page first and click the observed 领取 button, keeping the probe as fallback.
  local claimed=false
  local scans=0
  while not claimed and scans<4 do
    scans=scans+1
    local screen=_soloEventUi()
    local scene=screen.scene
    local strict=scene~="event" and scene~="clubRewards" and scene~="missionRewards"
    for _,w in ipairs(screen.words or {}) do
      local label=string.gsub(string.gsub(w.text,"%s",""),"領","领")
      -- On an unrecognized page only accept the legacy probe region, so a shop
      -- dialog elsewhere can never be claimed by mistake.
      if label=="领取" and (not strict or (w.x>=200 and w.x<=610 and w.y>=420 and w.y<=625)) then
        toast("领奖励")
        print("每日战利品：OCR 识别到领取按钮，已点击")
        tap(719-math.floor(w.y),math.floor(w.x))
        claimed=true
        break
      end
    end
    if not claimed and cmpColorEx("165|360|00D8FF,161|445|00D8FF", 1) == 1 then
      toast("领奖励")
      print("每日战利品：色点兜底命中领取按钮，已点击")
      tap(165, 360)
      tap(165, 360)
      claimed=true
    end
    if not claimed then sleep(500) end
  end
  if claimed then
    if _taskNotice then _taskNotice('已领取俱乐部奖励',2500) end
    sleep(2000)
    for i = 1, 20 do
      tap(60, 1242)
      sleep(200)
    end
  elseif _taskNotice then
    _taskNotice('未检测到可领取的俱乐部奖励',2500)
  end
  dailyCup.carList = mission
  if dailyCup.returnToSolo then dailyCup.returnToSolo(_soloEventUi()) end
end
function utils.waitStart()
  for di = 1, 200 do
    keepCapture()
    if _popupUi then _popupUi() end
    if cmpColorEx("680|50|FFFFFF-101010,660|70|FFFFFF-101010", 1) == 1 then
      return 1
    elseif cmpColorEx("476|1112|5400FF-101010,486|982|2B190C-101010,590|396|1B0804-101010", 1) == 1 and cmpColor(112, 117, "DB9714-101010", 1) == 0 then
      return 0
    elseif cmpColor(68, 800, "5400FF-101010", 1) == 1 then
      return 0
    elseif cmpColorEx("607|174|000000-101010,45|916|12FBC3-101010,640|250|FFFFFF-101010,102|1091|12FBC3-101010", 1) == 1 then
      tap(45, 916)
      return 0
    elseif data["多人地图"] == "识别失败" then
      return 0
    elseif cmpColorEx("190|808|FFFFFF-101010,190|912|12FBC3-101010", 1) == 1 then
      return 0
    elseif data["超时状态"] then
      return -1
    else
      data["信息"] = "等待比赛开始-" .. di
      if 200 <= di then
        return 0
      end
    end
    sleep(500)
  end
end
function utils.waitIcon()
  tx, ty = findColor(600, 450, 600, 800, "E7911F-0F191F", 0, 1)
  tx1, ty1 = findColor(600, 450, 600, 800, "2020CB-1F1F1F", 0, 1)
  if tx == -1 and tx1 == -1 then
    data["图标"] = false
  else
    data["图标"] = true
  end
  if data["氮气漂流"] and data["漂移"] == false and cmpColorEx("680|50|FFFFFF-050505,678|840|C3C9EE-191919", 1) == 1 then
    touchUp(2)
    data["漂移"] = false
    for k = 1, 2 do
      touchDown(1, 236, 1000)
      touchUp(1)
      sleep(100)
    end
  end
  if cmpColor(480, 138, "0000FF", 1) == 1 then
    tap(530, 1190)
    for k = 1, 20 do
      keepCapture()
      if cmpColor(480, 138, "0000FF", 1) ~= 1 then
        break
      end
      sleep(100)
    end
    touchUp(2)
    data["漂移"] = false
    for k = 1, 4 do
      touchDown(1, 236, 1000)
      touchUp(1)
      sleep(100)
    end
  end
end
function utils.trackCheck(name, map)
  local ret = ""
  local arr = splitStr(map, ",") or {"1", "N:0:2:750"}
  if #arr % 2 ~= 0 then
    print(name, "，线路错误")
    ret = "线路路程和操作数量不一致，检测每个线路路程后面是否存在对应的操作"
    return {0, ret}
  end
  for i = 2, #arr, 2 do
    if string.find(arr[i], "|", 1) ~= nil then
      local arr1 = splitStr(arr[i], "|") or {"N:0:2:750"}
      for j = 1, #arr1 do
        local temp = utf8.left(arr1[j], 1)
        if temp == "N" then
          local arr2 = splitStr(arr1[j], ":")
          for k = 1, 4 do
            if arr2[k] == nil or arr2[k] == "" then
              ret = "以下线路出错：" .. arr[i - 1] .. "," .. arr[i]
              print(name, arr[i - 1], arr[i])
              return {0, ret}
            end
          end
        elseif temp == "D" or temp == "S" then
          local arr2 = splitStr(arr1[j], ":")
          if arr2[2] == nil or arr2[2] == "" then
            ret = "以下线路出错：" .. arr[i - 1] .. "," .. arr[i]
            print(name, arr[i - 1], arr[i])
            return {0, ret}
          end
        elseif tonumber(arr1[j]) == nil then
          ret = "以下线路出错：" .. arr[i - 1] .. "," .. arr[i]
          print(name, arr[i - 1], arr[i])
          return {0, ret}
        end
      end
    else
      local temp = utf8.left(arr[i], 1)
      if temp == "N" then
        local arr1 = splitStr(arr[i], ":") or {"1", "N:0:2:750"}
        for k = 1, 4 do
          if arr1[k] == nil or arr1[k] == "" then
            ret = "以下线路出错：" .. arr[i - 1] .. "," .. arr[i]
            print(name, arr[i - 1], arr[i])
            return {0, ret}
          end
        end
      elseif temp == "D" or temp == "S" then
        local arr1 = splitStr(arr[i], ":")
        if arr1[2] == nil or arr1[2] == "" then
          ret = "以下线路出错：" .. arr[i - 1] .. "," .. arr[i]
          print(name, arr[i - 1], arr[i])
          return {0, ret}
        end
      elseif tonumber(arr[i]) == nil then
        ret = "以下线路出错：" .. arr[i - 1] .. "," .. arr[i]
        print(name, arr[i - 1], arr[i])
        return {0, ret}
      end
    end
  end
  return {1, "无错误"}
end
function utils.multipleDriver()
  data["信息"] = "等待多人匹配"
  for j = 1, 150 do
    keepCapture()
    if cmpColorEx("600|125|FFFFFF-101010,602|125|FFFFFF-101010", 1) == 1 then
      break
    end
    if cmpColorEx("660|50|FFFFFF-101010,670|70|FFFFFF-101010", 1) == 1 then
      toast("比赛已开始")
      sleep(2000)
      return 0
    elseif cmpColorEx("476|1112|5400FF-101010,486|982|2B190C-101010,590|396|1B0804-101010", 1) == 1 and cmpColor(112, 117, "DB9714-101010", 1) == 0 then
      return -1
    elseif cmpColorEx("70|918|12FBC3-101010,594|317|000000-101010,638|251|FFFFFF-101010,349|554|5400FF-101010", 1) == 1 then
      tap(70, 918)
      sleep(3000)
      return 0
    elseif cmpColorEx("74|110|210900,70|1084|12FBC3,100|188|18160F", 1) == 1 then
      tap(74, 110)
      sleep(500)
      tap(70, 1084)
      sleep(3000)
    elseif cmpColorEx("368|150|F9EF33-101010,604|1170|FFFFFF-101010,70|332|FFFFFF-101010", 1) == 1 then
      tap(80, 1080)
      sleep(2000)
    elseif cmpColorEx("666|86|2E2E2E-101010,666|120|373737-101010,574|140|5400FF-101010", 1) == 1 or cmpColorEx("96|344|FFFFFF-101010,100|930|FFFFFF-101010,674|1250|3B2312-101010", 1) == 1 then
      return 0
    elseif data["超时状态"] then
      toast("游戏闪退")
      data["超时状态"] = true
      return 0
    elseif 120 < j then
      toast("匹配时间太长")
      _requestGameRestart('matching')
      data["心跳时间"] = os.time()
      data["超时状态"] = false
      return 0
    end
    data["信息"] = "等待匹配中-" .. j
    sleep(500)
  end
  data["信息"] = "检测匹配人数"
  keepCapture()
  local posArr = {
    58,
    362,
    666,
    970
  }
  local carNum = 0
  for i = 1, 4 do
    if cmpColor(222, posArr[i], "220D05-050505", 1) ~= 1 then
      carNum = carNum + 1
    end
  end
  for i = 1, 3 do
    if cmpColor(100, posArr[i], "220D05-050505", 1) ~= 1 then
      carNum = carNum + 1
    end
  end
  toast("当前人数：" .. carNum .. "人")
  data["心跳时间"] = os.time()
  local wPath = getWorkPath()
  keepCapture()
  snapShot(LOCAL_ROOT .. "/PIC1.png", 520, 110, 680, 350)
  rotateImage(LOCAL_ROOT .. "/PIC1.png", 270)
  snapShot(LOCAL_ROOT .. "/PIC2.png", 586, 121, 666, 256)
  local x, y = -1, -1
  local ret = utils.localOCR(520, 110, 680, 350)
  data["信息"] = "检测地图信息"
  if UIconfig.picCheck == "" then
    for k, v in ipairs(data["地图"]) do
      x, y = findPic(586, 121, 666, 256, LOCAL_ROOT .. "/" .. v .. ".png", "020202", 2, 0.9)
      if x ~= -1 then
        print("本地图片：", v)
        toast("img：" .. v)
        utils.getMapName(v)
        return 1
      end
      sleep(1)
    end
  end
  if ret == "" then
    status, ret = pcall(utils.bdOCR1, LOCAL_ROOT .. "/PIC1.png")
    toast("bd1：" .. ret)
  elseif utils.getMapName(ret) == 0 then
    status, ret = pcall(utils.bdOCR1, LOCAL_ROOT .. "/PIC1.png")
    toast("bd2：" .. ret)
  else
    toast("local：" .. ret)
  end
  if utils.getMapName(ret) == 0 then
    return 0
  end
  sleep(1000)
  os.rename(LOCAL_ROOT .. "/PIC2.png", LOCAL_ROOT .. "/" .. data["多人地图"] .. ".png")
  releaseCapture()
  return 1
end
