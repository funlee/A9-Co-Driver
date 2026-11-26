dsCup = {}
-- 这辆车打不了当前危险等级（性能分不够）时，本轮车联的最高可选等级。
-- 性能分是车的固定属性，所以降下来后整个任务期间都保留，不再回高难度。
local maxLevel
-- 退回章节面板：只需要退到能看见章节卡的那一层，退过了会直接回主界面。
local function backToPanel()
  for i = 1, 3 do
    local screen = type(_soloEventUi) == 'function' and _soloEventUi() or nil
    if screen and #utils.carClubCards(screen) > 0 then return true end
    keyPress('back')
    sleep(1500)
  end
  return false
end
-- 降一档重选：先回章节面板，再按上限重新选任务。
local function dropLevel()
  local level = tonumber(data['车联等级'])
  if not level or level <= 1 then return false end
  maxLevel = level - 1
  data['信息'] = '车辆性能分不足，降到难度' .. maxLevel
  toast(data['信息'])
  if not backToPanel() then
    data['信息'] = '性能分不足后没回到车联任务面板'
    return false
  end
  return utils.spChoose(maxLevel) == 1
end
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
      -- 每次选任务都先回到章节横条的最左起点，再从起点逐张章节卡按当前进度选任务。
      local chosen = utils.spChoose(maxLevel)
      -- 危险值不足：关掉弹窗并按确定条件跳过本项目，不让它变成「回主重试」。
      if chosen == 'noTicket' then return dsCup.operation[6][2]() end
      if chosen ~= 1 then
        -- 车联是线性推进：从起点翻到底都没有可做的任务，说明界面变了，直接收尾。
        if chosen == -1 then data["信息"] = "车联任务列表未找到可进入的危险等级" end
        return -1
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
      -- 开始按钮点不动（性能分不足）时开赛页会一直循环到报错，先看文字提前降档。
      local startScreen = type(_soloEventUi) == 'function' and _soloEventUi() or nil
      if utils.carScoreShort and utils.carScoreShort(startScreen) then
        if dropLevel() then return nil end
        data['信息'] = '车辆性能分不足以进入任何难度，跳过车联'
        toast(data['信息'])
        keyPress('back')
        sleep(2000)
        return -1
      end
      local ok, sRet = pcall(utils.startConfiguredRace)
      if not ok then
        if dropLevel() then return nil end
        error(sRet)
      end
      if type(sRet) == "string" then return sRet end
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
      local chosen = utils.spChoose(maxLevel)
      if chosen == 'noTicket' then return dsCup.operation[6][2]() end
      if chosen ~= 1 then
        data["信息"] = "车联任务列表未找到可进入的危险等级"
        return -1
      end
      sleep(2000)
    end
  },
  {
    config.noTicket,
    function()
      -- 危险值（车联门票）不足是确定条件：跳过本项目继续队列，回主重试只会白跑一轮。
      data["信息"] = "车联危险值不足，已跳过"
      keyPress("back")
      toast("车联危险值不足，已跳过")
      sleep(2000)
      return 'clubNoTicket'
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
        elseif cup.isSettlement(_soloEventUi().scene) then
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
      data["信息"] = "开始结算"
      for i = 1, 2 do
        for j = 1, 200 do
          keepCapture()
          if utils.carClubPage(config.dsCupInfo) then
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
