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
      if utils.chooseCarCup(utils.defultCfg(run.data["杯赛用车"], {
        1,
        2,
        3,
        4,
        5,
        6
      })) == false then
        return -1
      end
      sleep(2000)
    end
  },
  {
    config.carInfo,
    function()
      data["信息"] = "即将开始游戏"
      local sRet = utils.startRace()
      if sRet == 0 or sRet == -2 then
        return -1
      elseif sRet == -1 or sRet == 2 then
        return nil
      end
      if utils.defultCfg(run.data["杯赛选路"], "4") == "4" then
        local mapAppear = false
        for i = 1, 100 do
          keepCapture()
          if cmpColorEx("664|436|5400FF-101010,664|490|5400FF-101010,624|434|4800FF-101010", 1) == 1 then
            mapAppear = true
            break
          elseif cmpColorEx(config.noTicket, 1) == 1 or cmpColorEx(config.tips, 1) == 1 then
            break
          end
          sleep(100)
        end
        releaseCapture()
        if mapAppear then
          local cupMap = utils.localOCR(309, 418, 482, 656)
          utils.getMapName(cupMap)
          if string.find(data["多人地图"], "识别失败", 1) ~= nil then
            print("自动识别失败")
            utils.getMapName(run.data["杯赛线路"])
          end
        end
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
        return -1
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
      data["信息"] = "开始结算"
      for i = 1, 2 do
        for j = 1, 200 do
          keepCapture()
          if utils.collectRaceRewards then utils.collectRaceRewards() end
          if cmpColorEx(config.cupInfo, 1) == 1 then
            break
          end
          if cup.choosePrize and cmpColorEx(cup.choosePrize, 1) == 1 then
            tap(314, 526)
            sleep(1000)
            tap(115, 563)
            sleep(1000)
          end
          utils.tipsHandle()
          tap(96, 1224)
          sleep(500)
        end
        for j = 1, 10 do
          tap(96, 1224)
          sleep(300)
        end
      end
      return 0
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
