cup = {}
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

-- The event is already recognized by OCR. Use its Race button when the
-- legacy cup page colors no longer line up with the display.
function cup.openRaceGarage(screen, eventName)
  for _,word in ipairs(screen.words or {}) do
    local label=word.text:gsub('%s',''):gsub('賽','赛')
    if label=='比赛' and word.x>=900 and word.y>=560 then
      data['信息']=(eventName or '杯赛')..'：点击比赛，进入选车'
      print(data['信息'])
      tap(719-word.y,word.x)
      sleep(1);sleep(3000)
      return true
    end
  end
  return false
end

function cup.isSettlement(scene)
  return scene=='results' or scene=='rewards' or scene=='cards' or scene=='missionRewards' or scene=='conversion'
end

-- Recognize the map on the cup loading screen, using the existing OCR region.
function cup.readMap()
  data["多人地图"]="";data["线路"]=""
  for i=1,100 do
    keepCapture()
    if cmpColorEx("664|436|5400FF-101010,664|490|5400FF-101010,624|434|4800FF-101010",1)==1 then
      releaseCapture()
      local ok=pcall(utils.getMapName,utils.localOCR(309,418,482,656))
      if not ok then data["多人地图"]="";data["线路"]="";print("杯赛地图未确认，使用左划兜底") end
      return
    elseif cmpColorEx(config.noTicket,1)==1 or cmpColorEx(config.tips,1)==1 or cmpColorEx(config.racing,1)==1 then break end
    sleep(100)
  end
  releaseCapture()
  print("杯赛地图未确认，使用左划兜底")
end
cup.operation = {
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
      local chosen=utils.chooseCarCup(utils.taskCars())
      if type(chosen)=="string" then return chosen end
      sleep(2000)
    end
  },
  {
    config.carInfo,
    function()
      data["信息"] = "即将开始游戏"
      utils.prepareTaskRoute(true)
      local sRet = utils.startConfiguredRace()
      if type(sRet)=="string" then return sRet end
      if sRet == 0 or sRet == -2 then
        return -1
      elseif sRet == -1 or sRet == 2 then
        return nil
      end
      if utils.taskRouteAuto() then cup.readMap() end
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
      if _soloEventUi().scene~='noTicket' then return end
      data["信息"] = "杯赛门票检查"
      keyPress("back")
      toast("没票")
      sleep(2000)
      return 'noTicket'
    end
  },
  {
    config.racing,
    function()
      utils.prepareTaskRoute()
      utils.newAutoDriver()
      data["信息"] = "比赛中"
      repeat
        keepCapture()
        if cmpColorEx(config.racing, 1) == 1 then
          tap(236, 1100)
          sleep(650)
        elseif _soloEventUi and cup.isSettlement(_soloEventUi().scene) then
          return dailyCup.settle(data['项目'],_soloEventUi)
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
      return dailyCup.settle(data['项目'],_soloEventUi)
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
return cup
