dailyCup = {}
local config = {
  main = "690|1240|FFFFFF-101010,690|1252|3B2312-101010,670|1270|3B2312-101010",
  selectMission = "614|67|5F2200-501902,620|1236|000000-101010,678|1250|3C2313-101010",
  cupInfo = "612|80|4F00CF-201040,614|54|5F2200-501909,620|1242|000000-101010",
  carport = "666|86|2E2E2E-101010,666|120|373737-101010,574|140|5400CF-201030",
  carInfo = "368|150|F9EF33-101010,302|142|F9EF33-101010,434|150|F9EF33-101010",
  noTicket = "538|1120|5400FF-290909,550|960|2C190C-101010,286|856|5400FF-101010",
  racing = "680|50|FFFFFF-101010,660|70|FFFFFF-101010",
  ending = "72|770|5400FF-101010,40|780|FFFFFF-101010,72|800|5400FF-101010",
  finish = "48|672|00D8FF-101010,100|326|FFFFFF-101010,678|1250|3C2313-101010",
  commonPrize = "87|1164|FFFFFF-101010,110|1102|311406-101010,68|1051|301307-101010",
  tips = "190|808|FFFFFF-101010,190|912|12FBC3-101010"
}
dailyCup.carList = {}
function dailyCup.skipReason()
  return _dailyTaskStatus and _dailyTaskStatus(run.index) or ''
end
-- Loading and settlement differ from multiplayer; neither rank nor XP pages are mandatory.
function dailyCup.readMap()
  data["多人地图"]="";data["线路"]=""
  local candidate=nil;local frames=0;local chosen=nil
  for i=1,60 do
    local screen=_dailyEventUi()
    if screen.scene=="race" then break end
    if screen.scene=="loading" then
      if screen.route and screen.route==candidate then frames=frames+1 else candidate=screen.route;frames=screen.route and 1 or 0 end
      if frames>=2 and chosen~=candidate then
        chosen=candidate;utils.getMapName(chosen)
        print("每日战利品地图连续确认："..data["多人地图"])
      end
    end
    keepCapture()
    if cmpColorEx(config.noTicket,1)==1 or cmpColorEx(config.tips,1)==1 then break end
    if data["超时状态"] then return -1 end
    sleep(200)
  end
  if not chosen then print("每日战利品地图未确认，使用左划兜底") end
end
local function press(screen,label)
  for _,w in ipairs(screen.words or {}) do
    local text=string.gsub(w.text,"%s","")
    if label=="继续" and text=="继绩" then text="继续" end
    if label=="错失机会" and text=="错矢机会" then text="错失机会" end
    if text==label and w.x>750 and w.y>540 then
      tap(719-math.floor(w.y),math.floor(w.x));sleep(800);return true
    end
  end
  return false
end
function dailyCup.settle()
  data["信息"]="每日战利品结算中"
  local started=tickCount();local rewardSince=nil;local readySince=nil;local rewardDone=false;local seen=false;local eventFrames=0
  while tickCount()-started<90000 do
    local screen=_dailyEventUi();local scene=screen.scene
    if scene=="event" then
      eventFrames=eventFrames+1
      if seen and eventFrames>=2 then print("每日战利品结算完成，已返回赛事");return 0 end
    else eventFrames=0 end
    if scene=="results" then
      seen=true;press(screen,"继续")
    elseif scene=="rewards" then
      seen=true;rewardSince=rewardSince or tickCount()
      if screen.rewardCanContinue then readySince=readySince or tickCount() end
      if screen.rewardCanContinue and (rewardDone or screen.rewardReady or tickCount()-(readySince or tickCount())>=8000) then
        if not rewardDone then _sessionLegacyRewards(screen.rewardCoins or -1,screen.rewardFame or -1);rewardDone=true end
        if not press(screen,"错失机会") then press(screen,"继续") end
      elseif tickCount()-rewardSince>=60000 then error("每日战利品收益结算超时") end
    elseif scene=="cards" then
      seen=true;press(screen,"继续")
    elseif scene=="conversion" then
      seen=true;press(screen,"确定")
    elseif scene=="vehicleUnlock" and screen.unlockLater then
      tap(719-math.floor(screen.unlockLater.y),math.floor(screen.unlockLater.x));sleep(800)
    end
    if data["超时状态"] then error("每日战利品结算超时") end
    sleep(300)
  end
  error("每日战利品未确认返回赛事，已停止")
end

dailyCup.operation = {
  {
    config.main,
    function()
      data["信息"] = "进入杯赛界面"
      for i = 1, 20 do
        keepCapture()
        if cmpColorEx(config.selectMission, 1) == 1 then
          for j = 1, 10 do
            tap(14, 500)
            sleep(200)
          end
          break
        else
          tap(14, 500)
          sleep(500)
        end
        releaseCapture()
        sleep(1000)
      end
      utils.getClearPrize()
      sleep(2000)
    end
  },
  {
    config.selectMission,
    function()
      data["信息"] = "选择指定杯赛"
      if utils.chooseCup(utils.defultCfg(run.data["杯赛位置"], "2")) == 0 then
        return -1
      end
      data["信息"] = "选择杯赛完毕"
      sleep(3000)
    end
  },
  {
    config.cupInfo,
    function()
      data["信息"] = "即将进入车库"
      utils.dailyPrize()
      tap(24, 1200)
      sleep(3000)
      for cc = 1, 30 do
        keepCapture()
        if cmpColorEx(config.carport, 1) == 1 or cmpColorEx(config.carInfo, 1) == 1 then
          break
        end
        sleep(300)
      end
    end
  },
  {
    config.carport,
    function()
      data["信息"] = "开始选车"
      if utils.chooseCarCup(dailyCup.carList) == false then
        return -1
      end
      sleep(2000)
    end
  },
  {
    config.carInfo,
    function()
      local reason=dailyCup.skipReason()
      if reason~='' then return reason end
      data["信息"] = "即将开始游戏"
      local sRet = utils.startRace()
      if sRet == 0 or sRet == -2 then
        return -1
      elseif sRet == -1 or sRet == 2 then
        return nil
      end
      if utils.defultCfg(run.data["杯赛选路"], "4") == "4" then
        dailyCup.readMap()
      end
      for ci = 1, 30 do
        keepCapture()
        if cmpColorEx(config.racing, 1) == 1 or cmpColorEx(config.noTicket, 1) == 1 then
          break
        end
        sleep(500)
      end
    end
  },
  {
    config.noTicket,
    function()
      data["信息"] = "杯赛门票检查"
      if UIconfig.cupTicketAD == "0" then
        if utils.ticketAd() ~= 1 then
          return -1
        end
        sleep(3000)
        keyPress("back")
        sleep(3000)
      elseif utils.defultCfg(run.data["代币买票"], "") == "0" then
        local times = tonumber(run.data["买票次数"]) or 0
        if 0 < times then
          tap(200, 300)
          toast("代币买票")
          sleep(3000)
          keyPress("back")
          sleep(3000)
          run.data["买票次数"] = times - 1 .. ""
        else
          keyPress("back")
          toast("买票次数用完")
          sleep(3000)
          return -1
        end
      else
        keyPress("back")
        toast("没票")
        sleep(2000)
        return "noTicket"
      end
    end
  },
  {
    config.racing,
    function()
      local xl = utils.defultCfg(run.data["杯赛选路"], "0")
      if xl == "3" then
        data["线路"] = run.data["杯赛线路"]
        data["多人地图"] = "百分比选路中"
        utils.newAutoDriver()
      elseif xl == "4" then
        utils.newAutoDriver()
      end
      data["信息"] = "比赛中"
      repeat
        keepCapture()
        if cmpColorEx(config.racing, 1) == 1 then
          tap(236, 1100)
          sleep(650)
          if xl == "0" then
          elseif xl == "1" then
            swipe(288, 840, 288, 440, 50)
          elseif xl == "2" then
            swipe(288, 440, 288, 840, 50)
          end
        end
        if data["超时状态"] then
          return -1
        end
      until cmpColorEx(config.ending, 1) == 1
    end
  },
  {
    config.ending,
    function()
      return dailyCup.settle()
    end
  },
  {
    config.commonPrize,
    function()
      tap(50, 1220)
      sleep(2000)
    end
  },
  {
    config.tips,
    function()
      data["信息"] = "关闭杯赛提示框"
      tap(160, 194)
      sleep(1000)
      tap(200, 700)
      sleep(2000)
    end
  }
}
return dailyCup
