multiple1 = {}
local config = {
  main = "690|1240|FFFFFF-101010,690|1252|3B2312-101010,670|1270|3B2312-101010",
  inMulti = "60|342|FFFFFF-101010,224|280|FFFFFF-101010,600|580|000000-101010",
  carport = "666|86|2E2E2E-101010,666|120|373737-101010,574|140|5400CF-201030",
  carInfo = "368|150|F9EF33-101010,302|142|F9EF33-101010,434|150|F9EF33-101010",
  vsFail = "476|1112|5400FF-101010,486|982|2B190C-101010",
  racing = "680|50|FFFFFF-101010,660|70|FFFFFF-101010",
  ending = "70|920|12FBC3-101010,70|970|12FBC3-101010",
  finish = "48|672|00D8FF-101010,100|326|FFFFFF-101010,678|1250|3C2313-101010",
  commonPrize = "87|1164|FFFFFF-101010,110|1102|311406-101010,68|1051|301307-101010",
  inCarGroup = "589|660|3B1F0C-101010,579|606|FFFFFF-101010,560|582|2A1203-101010",
  canNoUseCar = "368|150|F9EF33-101010,520|936|48372C-101010,70|250|48362C-101010"
}
local carList = {}
local function chooseCar(n)
  if n <= 0 then
    return false
  end
  local lastCar = 5
  local lenCar = #carList[data["段位"]]
  local hasLastCar = false
  local hc = {300, 68}
  local lc = {
    60,
    550,
    1000
  }
  local carUseOnce = utils.defultCfg(run.data["用车模式"], "")
  local carAd = utils.defultCfg(run.data["广告修车"], "")
  if data["用车情况"][2] ~= data["段位"] then
    utils.initCar()
  end
  if data["用车情况"][1] ~= 0 and data["用车情况"][1] > 6 and data["用车情况"][2] ~= 0 and utils.adjustCar() then
    lastCar = data["用车情况"][1]
    n = data["用车情况"][2]
    hasLastCar = true
  else
    local ydw = {
      [6] = 1100,
      [5] = 1030,
      [4] = 960,
      [3] = 880,
      [2] = 810,
      [1] = 740
    }
    sleep(1000)
    tap(600, ydw[n])
    sleep(2000)
  end
  while true do
    local temp = carList[n]
    local car1 = tonumber(temp[1]) or 0
    if car1 == 0 then
      return chooseCar(n - 1)
    end
    for j = 1, #temp do
      do
      local car = tonumber(temp[j]) or 1
      if hasLastCar then
        car = lastCar
        j = data["用车情况"][3]
        hasLastCar = false
      end
      if data["超时状态"] then
        return false
      end
      sleep(100)
      if carUseOnce == "1" and data["用车次数"]["car" .. j] ~= nil then
        if lenCar <= j then
          if data["用车次数"]["car" .. j] == 0 then
            return chooseCar(n - 1)
          else
            data["用车次数"] = {}
          end
        end
        if data["用车次数"]["car" .. j] == 1 then
          goto lbl_297
        end
      end
      local tempHL = utils.getCarHL(car)
      local h, l = tempHL[1], tempHL[2]
      local lastl = utils.getCarHL(lastCar)
      lastl = lastl[2]
      utils.carSwipe(l, lastl)
      keepCapture()
      if 4 < car then
        l = 3
      end
      x, y = findMultiColor(hc[h], lc[l], hc[h] + 50, lc[l] + 250, "13FBC3-101010", "6|10|39260C-101010|158|-2|FFFFFF-101010", 2, 1)
      if carAd ~= "" then
        x1, y1 = findMultiColor(hc[h], lc[l], hc[h] + 60, lc[l] + 250, "5400FF-101010", "158|-2|FFFFFF-101010", 2, 1)
      end
      if x ~= -1 and y ~= -1 then
        tap(x + 20, y)
        sleep(50)
        tap(x + 20, y)
        data["用车次数"]["car" .. j] = 1
        data["用车情况"] = {
          car,
          n,
          j
        }
        sleep(2000)
        return true
      else
        if carAd ~= "" then
          tap(x1, y1)
          utils.wait(config.carInfo)
          sleep(1000)
          if utils.carAd() == 1 then
            data["用车次数"]["car" .. j] = 1
            data["用车情况"] = {
              car,
              n,
              j
            }
            return true
          else
            return false
          end
        end
        data["用车次数"]["car" .. j] = 0
      end
      lastCar = car
      end
      ::lbl_297::
    end
    if carUseOnce == "0" then
      if n <= 0 then
        return false
      end
      return chooseCar(n - 1)
    end
  end
end
function multiple1.init()
  carList = {
    utils.defultCfg(run.data["青铜用车"], {1}),
    utils.defultCfg(run.data["白银用车"], {1}),
    utils.defultCfg(run.data["黄金用车"], {1}),
    utils.defultCfg(run.data["白金用车"], {1}),
    utils.defultCfg(run.data["钻石用车"], {1}),
    utils.defultCfg(run.data["传奇用车"], {1})
  }

end
multiple1.operation = {
  {
    config.main,
    function()
      data["信息"] = "进入多人界面"
      keepCapture()
      if cmpColorEx("690|1252|3B2312-101010,690|1240|FFFFFF-101010,74|724|FFFFFF-101010", 1) == 0 then
        tap(70, 700)
        sleep(500)
      end
      tap(505, 800)
      sleep(200)
      tap(505, 800)
      sleep(2000)
    end
  },
  {
    config.inMulti,
    function()
      data["信息"] = "进入车库"
      utils.checkRank()
      keepCapture()
      for t = 1, 4 do
        tap(40, 680)
        sleep(200)
      end
      utils.wait(config.carport)
    end
  },
  {
    config.carport,
    function()
      data["信息"] = "开始选车"
      multiple1.init()
      if chooseCar(data["段位"]) == false then
        return -1
      end
      sleep(2000)
    end
  },
  {
    config.carInfo,
    function()
      data["信息"] = "即将开始游戏"
      if utils.startRace() == 0 then
        return -1
      end
      local stats = utils.multipleDriver()
      if stats == -1 then
        utils.backToMain()
        return
      end
      if stats == 1 then
        utils.newAutoDriver()
        data["信息"] = "智能选路结束"
      end
      sleep(3000)
    end
  },
  {
    config.vsFail,
    function()
      keepCapture()
      if cmpColor(112, 117, "DB9714-101010", 1) == 0 then
        data["信息"] = "匹配失败"
        tap(470, 1100)
        sleep(5000)
      end
    end
  },
  {
    config.racing,
    function()
      data["信息"] = "比赛中"
      repeat
        keepCapture()
        if cmpColorEx(config.racing, 1) == 1 then
          tap(236, 1100)
          sleep(650)

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
      keepCapture()
      local ci = 8
      if cmpColor(708, 288, "5400FF-101010", 1) == 1 then
      elseif cmpColor(130, 48, "FFFFFF-050505", 1) == 1 then
        toast("开始检测排名")
        tap(60, 670)
        for i = 1, 100 do
          keepCapture()
          if cmpColorEx("600|60|000000-101010,600|120|000000-101010", 1) == 1 then
            releaseCapture()
            break
          end
          sleep(100)
        end
        local temp = {
          560,
          520,
          480,
          440,
          400,
          360,
          320,
          280
        }
        keepCapture()
        for i = 1, 8 do
          if cmpColor(temp[i], 50, "5500FF-101010", 1) == 1 then
            toast("第" .. i .. "名")
            ci = i
            if i <= 3 then
              data["多人胜场"] = data["多人胜场"] + 1
            end
            break
          end
        end
      end
      if ci <= UIconfig.coinADNum and UIconfig.coinAD == "0" then
        tap(33, 1080)
        sleep(3000)
        tap(33, 1080)
        sleep(5000)
        utils.coinAd()
      end
      for i = 1, 100 do
        keepCapture()
        if cmpColorEx(config.inMulti, 1) == 1 then
          for j = 1, 10 do
            tap(33, 1080)
            sleep(200)
          end
          break
        end
        utils.tipsHandle()
        if data["超时状态"] then
          return -1
        end
        for j = 1, 5 do
          tap(33, 1080)
          sleep(200)
        end
      end
      return 0
    end
  },
  {
    config.finish,
    function()
      data["信息"] = "赛季结束，领奖"
      tap(40, 680)
      sleep(2000)
      for i = 1, 15 do
        tap(58, 1065)
        sleep(500)
      end
      sleep(2000)
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
    config.inCarGroup,
    function()
      data["信息"] = "在车组中"
      if chooseCar(data["段位"]) == false then
        return -1
      end
      sleep(2000)
    end
  },
  {
    config.canNoUseCar,
    function()
      data["信息"] = "车辆不能使用"
      utils.backToMain()
    end
  }
}
return multiple1
