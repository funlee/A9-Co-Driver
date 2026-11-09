career = {}
local config = {
  main = "690|1240|FFFFFF-101010,690|1252|3B2312-101010,670|1270|3B2312-101010",
  career1 = "680|1250|3B2312-101010,40|540|000000-101010,34|50|021A12-101010",
  careerInfo = "26|42|5400FF-101010,102|106|5400FF-101010,680|1250|3B2312-101010",
  carport = "666|86|2E2E2E-101010,666|120|373737-101010,574|140|5400CF-201030",
  carInfo = "368|150|F9EF33-101010,302|142|F9EF33-101010,434|150|F9EF33-101010",
  noTicket = "538|1120|5400FF-290909,550|960|2C190C-101010,286|856|5400FF-101010",
  racing = "680|50|FFFFFF-101010,660|70|FFFFFF-101010",
  ending = "72|770|5400FF-101010,40|780|FFFFFF-101010,72|800|5400FF-101010",
  finish = "48|672|00D8FF-101010,100|326|FFFFFF-101010,678|1250|3C2313-101010",
  commonPrize = "87|1164|FFFFFF-101010,110|1102|311406-101010,68|1051|301307-101010",
  tips = "160|194|200800-101010,200|830|FFFFFF-101010,200|880|10FBC3-101010"
}
local chooseCareer = function(z, h, l)
  local arr1 = {
    752,
    816,
    880,
    960,
    1036,
    1110
  }
  tap(26, arr1[z])
  if 7 < l then
    local diff = l - 7
    l = 7
    sleep(1000)
    tap(26, arr1 + 5 * diff)
  end
  sleep(1000)
  tap(540 - (h - 1) * 180, 90 + (l - 1) * 180)
  sleep(1000)
end
local chooseCareer1 = function(n)
  toast("选择第" .. n .. "关")
  sleep(1000)
  if n == 2 then
    keepCapture()
    ret, x, y = _findColorTriple(280, 150, 520, 800, "12FBC3-101010", 4, 1.0)
    tap(x, y)
  elseif 2 < n then
    for i = 1, n - 2 do
      keepCapture()
      ret, x, y = _findColorTriple(450, 150, 658, 800, "12FBC3-101010", 4, 1.0)
      tap(x, y)
      sleep(1200)
    end
  end
end
career.operation = {
  {
    config.main,
    function()
      data["信息"] = "进入生涯界面"
      tap(68, 1100)
      tap(68, 1100)
      sleep(3000)
    end
  },
  {
    config.career1,
    function()
      data["信息"] = "选择生涯"
      local z, h, l = 4, 2, 4
      if utils.defultCfg(run.data["自定义生涯位置"], "") ~= "" then
        z = tonumber(run.data["生涯大章"]) + 1 or 4
        h = tonumber(run.data["生涯行"]) or 2
        l = tonumber(run.data["生涯列"]) or 4
      end
      chooseCareer(z, h, l)
      sleep(2000)
    end
  },
  {
    config.careerInfo,
    function()
      data["信息"] = "选小关"
      if utils.defultCfg(run.data["自定义生涯位置"], "") ~= "" then
        local n = tonumber(run.data["生涯小关"]) or 12
        chooseCareer1(n)
      else
        for i = 1, 4 do
          swipe(650, 850, 50, 850, 500)
        end
        sleep(1000)
        keepCapture()
        local x = -1
        y = -1
        ret, x, y = _findColorTriple(100, 380, 500, 540, "12FBC3-101010", 0, 1.0)
        releaseCapture()
        if x ~= -1 and y ~= -1 then
          tap(x, y)
          sleep(1000)
        end
      end
      tap(32, 1040)
      sleep(500)
      utils.wait(config.carport)
      sleep(1000)
    end
  },
  {
    config.carport,
    function()
      data["信息"] = "开始选车"
      if utils.chooseCarCup(utils.defultCfg(run.data["生涯用车"], {
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
      sleep(2000)
    end
  },
  {
    config.racing,
    function()
      local xl = utils.defultCfg(run.data["生涯选路"], "0")
      if xl == "3" then
        data["线路"] = utils.defultCfg(run.data["生涯线路"], "")
        data["多人地图"] = "百分比选路中"
        utils.newAutoDriver()
      elseif xl == "4" then
        utils.getMapName(utils.defultCfg(run.data["生涯线路"], "无线路"))
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
      for j = 1, 200 do
        keepCapture()
        if utils.collectRaceRewards then utils.collectRaceRewards() end
        if cmpColorEx(config.careerInfo, 1) == 1 then
          break
        end
        utils.tipsHandle()
        tap(90, 874)
        sleep(500)
      end
      for j = 1, 5 do
        tap(90, 874)
        sleep(300)
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
      tap(200, 830)
      sleep(2000)
    end
  }
}
return career
