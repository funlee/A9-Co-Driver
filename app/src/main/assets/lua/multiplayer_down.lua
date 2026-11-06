multiple2 = {}
local config = {
  main = "690|1240|FFFFFF-101010,690|1252|3B2312-101010,670|1270|3B2312-101010",
  inMulti = "60|342|FFFFFF-101010,224|280|FFFFFF-101010,600|580|000000-101010",
  carport = "666|86|2E2E2E-101010,666|120|373737-101010,574|140|5400CF-201030",
  carInfo = "368|150|F9EF33-101010,302|142|F9EF33-101010,434|150|F9EF33-101010",
  vsFail = "476|1112|5400FF-101010,486|982|2B190C-101010",
  racing = "680|50|FFFFFF-101010,660|70|FFFFFF-101010",
  ending = "70|920|12FBC3-101010,70|970|12FBC3-101010",
  finish = "48|672|00D8FF-101010,100|326|FFFFFF-101010,678|1250|3C2313-101010",
  commonPrize = "87|1164|FFFFFF-101010,110|1102|311406-101010,68|1051|301307-101010"
}
local chooseCar = function(carList)
  local lastCar = 1
  local lenCar = #carList
  local hc = {300, 68}
  local lc = {
    60,
    550,
    1000
  }
  local carFillter = utils.defultCfg(run.data["选车筛选"], "")
  local carUseWay = utils.defultCfg(run.data["选车模式"], "")
  local carUseOnce = utils.defultCfg(run.data["用车模式"], "")
  local carAd = utils.defultCfg(run.data["广告修车"], "")
  if carFillter ~= "" then
    tap(580, 1210)
    sleep(1000)
    utils.wait("146|71|12FBC3-090909,125|139|12FBC3-090909,192|151|5A2805-090909,499|332|210900-090909")
    if string.find(carFillter, "0", 1) ~= nil then
      tap(497, 337)
      sleep(500)
    end
    if string.find(carFillter, "1", 1) ~= nil then
      tap(277, 334)
      sleep(500)
    end
    if string.find(carFillter, "2", 1) ~= nil then
      tap(402, 326)
      sleep(500)
    end
    tap(116, 208)
  else
    tap(600, 820)
  end
  sleep(2000)
  if carUseWay == "1" then
    tap(400, 220)
    toast("即将开始点击选车")
    sleep(3000)
    if tonumber(carList[1]) < 0 then
      toast("负数选车")
      tap(360, 490)
      sleep(700)
      lastCar = -1
    end
  end
  while true do
    for j = 1, lenCar do
      do
      local car = tonumber(carList[j]) or 1
      if car ~= 0 then
        if data["超时状态"] then
          return false
        end
        sleep(100)
        if carUseOnce == "1" and data["用车次数"]["car" .. j] ~= nil then
          if lenCar <= j then
            if data["用车次数"]["car" .. j] == 0 then
              return false
            else
              data["用车次数"] = {}
            end
          end
          if data["用车次数"]["car" .. j] == 1 then
            goto lbl_372
          end
        end
        if carUseWay == "1" then
          utils.clickChoose(car, lastCar)
          keepCapture()
          if cmpColorEx(config.carInfo, 1) == 1 and cmpColor(75, 1030, "FFFFFF-030303", 1) == 0 and (cmpColor(50, 832, "12FBC3-101010", 1) == 1 or cmpColor(50, 832, "210900-101010", 1) == 1) then
            return true
          elseif carAd == "0" then
            if utils.carAd() ~= 1 then
              return false
            end
            return true
          end
        else
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
            x1, y1 = findMultiColor(hc[h], lc[l], hc[h] + 50, lc[l] + 250, "5400FF-101010", "158|-2|FFFFFF-101010", 2, 1)
          end
          if x ~= -1 and y ~= -1 then
            tap(x + 20, y)
            sleep(50)
            tap(x + 20, y)
            data["用车次数"]["car" .. j] = 1
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
        end
        lastCar = car
      end
      end
      ::lbl_372::
    end
    if carUseOnce == "0" then
      return false
    end
  end
end
multiple2.operation = {
  {
    config.main,
    function()
      data["信息"] = "进入多人界面"
      keepCapture()
      if cmpColorEx("690|1252|3B2312-101010,690|1240|FFFFFF-101010,74|724|FFFFFF-101010", 1) == 0 then
        tap(70, 700)
        sleep(500)
      end
      tap(370, 800)
      sleep(200)
      tap(370, 800)
      sleep(2000)
    end
  },
  {
    config.inMulti,
    function()
      data["信息"] = "进入车库"
      utils.checkRank()
      keepCapture()
      if utils.defultCfg(run.data["传奇停跑"], "") ~= "" and cmpColor(378, 406, "A4E2F5-101010", 1) == 1 then
        local rankNums = utils.getRankNum()
        toast(rankNums .. " 分")
        sleep(2000)
        local tempNum = tonumber(utils.defultCfg(run.data["传奇分数"], "1850")) or 1750
        if rankNums >= tempNum then
          toast("已到指定传奇分数: " .. rankNums .. "，不再跑多人")
          return -1
        end
      end
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
      if chooseCar(utils.defultCfg(run.data["多人下用车"], {
        1,
        2,
        3,
        4
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
      if utils.startRace() == 0 then
        return -1
      end
      if utils.defultCfg(run.data["多人选路"], "0") == "3" then
        local stats = utils.multipleDriver()
        if stats == -1 then
          utils.backToMain()
          return
        end
        if stats == 1 then
          utils.newAutoDriver()
          data["信息"] = "智能选路结束"
        end
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
      if data["多人掉分"] then
        tap(664, 61)
        toast("多人上掉分")
        sleep(3000)
        tap(79, 1099)
        sleep(3000)
        data["心跳时间"] = os.time()
        return
      end
      local xl = utils.defultCfg(run.data["多人选路"], "0")
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
  }
}
return multiple2
