dsCup = {}
local config = {
  main = "690|1240|FFFFFF-101010,690|1252|3B2312-101010,670|1270|3B2312-101010",
  selectMission = "614|67|5F2200-501902,620|1236|000000-101010,678|1250|3C2313-101010",
  dsCupInfo = "678|1245|0E020D-101010,692|1245|ECDC02-101010,688|1074|3D1E0D-101010,679|1149|E2D227-101010",
  carport = "574|130|FFFFFF-101010,688|1076|6B6B6B-101010,692|1246|ECDF00-101010",
  carInfo = "368|150|F9EF33-101010,302|142|F9EF33-101010,434|150|F9EF33-101010",
  noTicket = "438|269|008AFF-101010,518|1020|0000E4-101010,465|308|FFFFFF-101010",
  racing = "680|50|FFFFFF-101010,660|70|FFFFFF-101010",
  ending = "72|770|5400FF-101010,40|780|FFFFFF-101010,72|800|5400FF-101010",
  noCar = "368|142|F9EF33-101010,606|1180|FFFFFF-101010,130|1184|5400FF-290909",
  commonPrize = "87|1164|FFFFFF-101010,110|1102|311406-101010,68|1051|301307-101010",
  tips = "190|808|FFFFFF-101010,190|912|12FBC3-101010"
}
dsCup.operation = {
  {
    config.main,
    function(screen)
      if utils.intoSpecial1(screen) == 0 then
        return -1
      end
      sleep(2000)
    end
  },
  {
    config.dsCupInfo,
    function()
      data["信息"] = "即将进入车库"
      for j = 1, 10 do
        tap(96, 1224)
        sleep(200)
      end
      for i = 1, 50 do
        local chosen = utils.spChoose()
        if chosen == 1 then
          break
        end
        if chosen == -1 then
          -- 车联是线性推进：界面上的任务就是当前可打的任务，找不到入口说明界面变了，直接收尾。
          data["信息"] = "车联任务列表未找到可进入的危险等级"
          return -1
        end
        if 50 <= i then
          return -1
        else
          local ret, x1, y1 = _findColorTriple(470, 400, 470, 1000, "FEBB29-101010", 0, 1.0)
          local dy = 560 - y1
          if y1 == -1 then
            dy = 0
          end
          utils.mySwipe(60, 580 - dy, 60, 90)
          sleep(1000)
        end
      end
      tap(24, 1200)
      sleep(3000)
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
      local sRet = utils.startConfiguredRace()
      if type(sRet)=="string" then return sRet end
      if sRet == 0 or sRet == -2 then
        return -1
      elseif sRet == -1 or sRet == 2 then
        return nil
      end
      sleep(3000)
    end
  },
  {
    config.noCar,
    function()
      data["信息"] = "性能分不足"
      keyPress("back")
      sleep(3000)
      utils.mySwipe(60, 700, 60, 67)
      sleep(1000)
      for i = 1, 60 do
        local chosen = utils.spChoose()
        if chosen == 1 then
          break
        end
        if chosen == -1 then
          data["信息"] = "车联任务列表未找到可进入的危险等级"
          return -1
        end
        if 60 <= i then
          return -1
        else
          local ret, x1, y1 = _findColorTriple(470, 400, 470, 1000, "FEBB29-101010", 0, 1.0)
          local dy = 560 - y1
          if y1 == -1 then
            dy = 0
          end
          utils.mySwipe(60, 580 - dy, 60, 90)
          sleep(1000)
        end
      end
      sleep(2000)
    end
  },
  {
    config.noTicket,
    function()
      data["信息"] = "危险度检查"
      keyPress("back")
      toast("危险度已满")
      sleep(2000)
      return -1
    end
  },
  {
    config.racing,
    function()
      data["信息"] = "比赛中"
      -- 车联从任务面板直接开赛，没有开赛前选车页，自动驾驶只能进比赛后按 HUD 开关确认
      local tdReady = false
      local tdTries = 0
      repeat
        keepCapture()
        if cmpColorEx(config.racing, 1) == 1 then
          if not tdReady and tdTries < 10 then
            tdTries = tdTries + 1
            tdReady = utils.ensureTouchDrive()
          end
          tap(236, 1100)
          sleep(650)
          -- 车手联会选路固定为左划（选路档位1），不提供其他选路与广告回油
          swipe(288, 840, 288, 440, 50)
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
          if cmpColorEx(config.dsCupInfo, 1) == 1 then
            break
          end
          if dsCup.choosePrize and cmpColorEx(dsCup.choosePrize, 1) == 1 then
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
return dsCup
