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
      for t = 1, 30 do
        for k = 1, 60 do
          sleep(1000)
          toast("游戏更新维护中")
        end
        keepCapture()
        if cmpColorEx(_ue7bbb4e68aa4e4b8ad, 1) == 0 then
          break
        end
      end
      tap(170, 640)
      for t = 1, 10 do
        sleep(1000)
      end
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
function utils.backToMain()
  data["信息"] = "返回游戏主界面"
  keepCapture()
  if cmpColorEx("690|1240|FFFFFF-101010,690|1252|3B2312-101010,670|1270|3B2312-101010", 1) ~= 1 then
    tap(690, 1244)
    toast("返回游戏主界面")
    sleep(4000)
    for i = 0, 40 do
      if data["超时状态"] then
        break
      end
      keepCapture()
      if cmpColorEx("689|1251|543E30-050505,678|1251|DAD5D5-050505,701|1252|FFFFFF-050505,690|1262|F4F4F4-101010", 1) == 1 then
        utils.stopGame()
        tap(1, 1)
        sleep(10000)
        utils.rebootGame()
      end
      if cmpColorEx("493|416|FF7856-101010,493|425|FF7956-101010,506|441|FFA157-101010", 1) == 1 then
        keyPress("back")
        sleep(2000)
      end
      if cmpColorEx("690|1240|FFFFFF-101010,690|1252|3B2312-101010,670|1270|3B2312-101010", 1) == 1 then
        toast("到达游戏主界面")
        break
      elseif cmpColorEx("680|50|FFFFFF,660|70|FFFFFF", 1) == 1 then
        tap(670, 60)
        sleep(2000)
        tap(80, 1100)
        sleep(3000)
      elseif cmpColorEx("70|920|10FBC3-101010,70|970|10FBC3-101010", 1) == 1 then
        for i = 1, 20 do
          tap(33, 1080)
          sleep(300)
        end
      elseif cmpColorEx("705|1251|191919-050505,690|1251|060302-050505,666|1226|060302-050505", 1) == 1 or cmpColorEx("698|1249|181818-050505,676|1250|060302-050505,679|1238|151413-050505", 1) == 1 then
        print("弹窗广告")
        tap(600, 1080)
        sleep(3000)
      elseif cmpColorEx("286|397|424242-090909,441|394|444241-090909,398|821|424242-090909,401|983|424242-090909", 1) == 1 then
        tap(317, 774)
        sleep(3000)
      else
        keyPress("back")
        data["信息"] = "返回游戏主界面中 - " .. i
        sleep(4000)
      end
    end
  end
  releaseCapture()
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
function utils.startRace()
  data["信息"] = "开始跑图"
  local ret = 0
  for i = 1, 4 do
    sleep(1000)
    if utils.mycolor(75, 1170, "FFFFFF") == 1 then
      if (data["项目"] == "杯赛" or data["项目"] == "特赛赛事" or data["项目"] == "车联" or data["项目"] == "多人下") and utils.defultCfg(run.data["广告修车"], "") ~= "" and utils.carAd() == 1 then
        return 2
      end
      data["信息"] = "车辆未解锁或没油"
      return 0
    end
    keepCapture()
    if cmpColorEx("74|110|210900,70|1084|12FBC3,100|188|18160F", 1) == 1 then
      tap(74, 110)
      sleep(500)
      tap(74, 1084)
      sleep(3000)
    end
    if cmpColorEx("368|142|F9EF33-101010,580|1166|FFFFFF-101010,130|1184|5400FF-090909", 1) == 1 then
      print("车手联会性能分不足")
      return -2
    end
    if cmpColorEx("368|150|F9EF33-101010,434|150|F9EF33-101010,236|142|F9EF33-101010", 1) == 1 and cmpColor(134, 1184, "5400FF-101010", 1) == 0 then
      if utils.mycolor(50, 830, "12FBC3-101010") == 1 then
        tap(80, 1080)
        tap(80, 1080)
        sleep(2000)
        ret = 1
        break
      else
        tap(50, 830)
        print("打开TouchDrive")
      end
    end
  end
  return ret
end
-- Set requested state, rather than toggling blindly on every visit.
function utils.configureGarageFilter(selection)
  if selection == "" then return true end
  if not _garageFilterState then error("车库筛选识别不可用") end
  if _garageFilterPrepare then _garageFilterPrepare() end
  local function read() keepCapture();return _garageFilterState() end
  local state=read()
  if not state.present then
    tap(600,1192)
    for i=1,20 do
      sleep(300);state=read()
      if state.present then break end
      if data["超时状态"] then error("车库筛选已超时") end
    end
  end
  if not state.present then error("未识别到车库筛选面板，已停止选车") end
  local options={{"0","owned",509,340,"已拥有"},{"1","rank",304,340,"性能分"},{"2","descending",436,340,"降序"}}
  for _,option in ipairs(options) do
    local wanted=string.find(selection,option[1],1,true) and 1 or 0
    local attempts=0
    for i=1,16 do
      state=read()
      if not state.present then error("车库筛选面板意外关闭") end
      if state[option[2]]==wanted then break end
      if state[option[2]]==1-wanted and attempts<3 then
        data["信息"]="设置车库筛选："..option[5]
        tap(option[3],option[4]);attempts=attempts+1;sleep(700)
      else sleep(300) end
      if data["超时状态"] then error("车库筛选已超时") end
    end
    state=read()
    if not state.present or state[option[2]]~=wanted then error("无法确认车库筛选："..option[5]) end
  end
  -- Confirm the complete combination immediately before applying it.
  for _,option in ipairs(options) do
    local wanted=string.find(selection,option[1],1,true) and 1 or 0
    if state[option[2]]~=wanted then error("车库筛选状态发生变化") end
  end
  tap(134,240)
  for i=1,20 do
    sleep(300);state=read()
    if not state.present then data["信息"]="车库筛选已应用";return true end
  end
  error("车库筛选未关闭，已停止选车")
end
function utils.chooseCarCup(carList)
  if #carList == 0 then
    carList = {
      1,
      2,
      3,
      4,
      5,
      6,
      7,
      8,
      9
    }
  end
  local lastCar = 1
  local lenCar = #carList
  local hc = {272, 40}
  local lc = {
    60,
    500,
    1000
  }
  local carFillter = utils.defultCfg(run.data["选车筛选"], "")
  local carUseWay = utils.defultCfg(run.data["选车方式"], "")
  local carUseOnce = utils.defultCfg(run.data["用车模式"], "")
  local carAd = utils.defultCfg(run.data["广告修车"], "")
  if carFillter ~= "" then
    utils.configureGarageFilter(carFillter)
    sleep(2000)
    if carUseWay == "1" then
      for i = 1, 6 do
        swipe(290, 70, 290, 1200, 100)
        sleep(100)
      end
    end
  elseif data["项目"] ~= "生涯" then
    for i = 1, 30 do
      swipe(290, 70, 290, 1200, 100)
      sleep(100)
    end
  end
  sleep(2000)
  if carUseWay == "1" then
    tap(400, 220)
    toast("即将开始点击选车")
    sleep(3000)
    if 0 > tonumber(carList[1]) then
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
        if carUseOnce == "1" and data["用车次数"]["car" .. j] == 1 then
          goto lbl_422
        end
        if carUseWay == "1" then
          utils.clickChoose(car, lastCar)
          keepCapture()
          if cmpColorEx("368|150|F9EF33-101010,302|142|F9EF33-101010,434|150|F9EF33-10101", 1) == 1 and cmpColor(75, 1030, "FFFFFF-030303", 1) == 0 and (cmpColor(50, 832, "12FBC3-101010", 1) == 1 or cmpColor(50, 832, "210900-101010", 1) == 1) then
            data["用车次数"]["car" .. j] = 1
            return true
          elseif carAd == "0" then
            if utils.carAd() ~= 1 then
              return false
            end
            data["用车次数"]["car" .. j] = 1
            return true
          end
          data["用车次数"]["car" .. j] = 0
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
          x, y = findMultiColor(hc[h], lc[l], hc[h] + 60, lc[l] + 250, "13FBC3-101010", "6|10|39260C-101010|158|-2|FFFFFF-101010", 2, 1)
          if carAd ~= "" then
            x1, y1 = findMultiColor(hc[h], lc[l], hc[h] + 60, lc[l] + 250, "5400FF-101010", "158|-2|FFFFFF-101010", 2, 1)
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
              utils.wait("368|150|F9EF33-101010,302|142|F9EF33-101010,434|150|F9EF33-101010")
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
      ::lbl_422::
    end
    if carUseOnce == "0" then return false end
    local used=false
    for j=1,lenCar do if data["用车次数"]["car" .. j]==1 then used=true end end
    if not used then return false end
    data["用车次数"]={}

  end
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
  elseif data["项目"] == "生涯" then
    dx = 262
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
  elseif data["项目"] == "生涯" then
    ty = 900
    tx = 374
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
function utils.chooseCup(num)
  data["信息"] = "选择杯赛"
  local cupAddr = LOCAL_ROOT .. "/cup" .. run.index .. ".png"
  local ret, x, y = -1, -1, -1
  num = tonumber(num)
  if not num or num == 0 or num ~= math.floor(num) then error("杯赛位置必须为非零整数；3 表示从左起第 3 个赛事") end
  utils.cupPositions = utils.cupPositions or {}
  if utils.cupPositions[run.index] ~= num then
    os.remove(cupAddr)
    utils.cupPositions[run.index] = num
    print("本次任务按配置选择杯赛位置：", num)
  end
  local abs = math.abs(num)
  local xy = {100, 150}
  if 0 < num then
    for i = 1, 8 do
      swipe(50, 200, 50, 1000, 100)
    end
    sleep(1000)
    if utils.mycolor(16, 150, "12FBC3-101010") == 0 then
      tap(100, 150)
      sleep(1000)
    end
    if 7 <= num then
      print("7位以上的杯赛")
      for i = 1, num - 7 do
        keepCapture()
        ret, x, y = _findColorTriple(224, 1180, 224, 1278, "5400FF-101010", 0, 1.0)
        if y ~= -1 then
          y = 1208 - y
        end
        utils.mySwipe(22, 1195, 22, 987 + y)
        sleep(500)
      end
      xy = {100, 1250}
    elseif num ~= 1 then
      xy = {
        100,
        120 + 180 * (num - 1)
      }
    end
  else
    xy = {100, 1130}
    for i = 1, 8 do
      swipe(50, 1000, 50, 200, 100)
    end
    sleep(2000)
    if utils.mycolor(16, 1130, "12FBC3-101010") == 0 then
      tap(100, 1130)
      sleep(1000)
    end
    if num <= -7 then
      for i = 1, abs - 7 do
        keepCapture()
        ret, x, y = _findColorTriple(224, 10, 224, 90, "5400FF-101010", 2, 1.0)
        if y ~= -1 then
          y = 53 - y
        end
        utils.mySwipe(22, 987 + y, 22, 1195)
        sleep(500)
      end
      xy = {100, 60}
    elseif num ~= -1 then
      xy = {
        100,
        1150 + 180 * (num + 1)
      }
    end
  end
  tap(xy[1], xy[2])
  sleep(1500)
  local res = {-1, -1, -1, -1}
  if utils.fileExist(cupAddr) then res = utils.findCup(cupAddr) end
  if res[3] ~= -1 then
    for i = 1, 3 do
      tap(res[1] + 100, res[2] + 10)
      sleep(200)
    end
    print("旧杯赛")
    return 1
  elseif utils.fileExist(cupAddr) == false then
    keepCapture()
    snapShot(cupAddr, 350, 212, 520, 330)
    releaseCapture()
    tap(xy[1], xy[2])
    print("新杯赛")
    return 1
  else
    toast("杯赛位置可能发生变动")
    for i = 1, 8 do
      swipe(50, 200, 50, 1000, 100)
    end
    sleep(2000)
    if utils.mycolor(16, 144, "12FBC3-101010") == 0 then
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
        if utils.mycolor(16, 144, "12FBC3-101010") == 0 then
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
  ret, x1, y1 = _findPicTriple(350, 212, 520, 330, cupAddr, "101010", 0, 0.7)
  return {
    x,
    y,
    x1,
    y1
  }
end
function utils.intoSpecial()
  data["信息"] = "进入特殊赛事 "
  keepCapture()
  if cmpColorEx("702|1252|FFFFFF-101010,690|1252|3B2312-101010,670|1270|3B2312-101010", 1) == 1 then
  else
    utils.backToMain()
  end
  for i = 1, 5 do
    keepCapture()
    if cmpColor(88, 300, "FFFFFF-101010", 1) == 0 then
      tap(88, 300)
      sleep(1000)
    end
    sleep(200)
  end
  for i = 1, 8 do
    utils.mySwipe(550, 406, 200, 406)
  end
  sleep(2000)
  local spPos = tonumber(run.data["特殊赛事位置"]) or 1
  local spStage = tonumber(run.data["特殊赛事阶段"]) or 1
  local swipes = function(n)
    local dx = 0
    for i = 1, n do
      dx = 0
      keepCapture()
      local ret, x, y = _findColorTriple(330, 60, 560, 60, "360A17-090909", 0, 1.0)
      if x ~= -1 then
        dx = 425 - x
        print(dx, x)
      end
      utils.mySwipe1(180, 408, 430 + dx, 408, 500)
      sleep(500)
    end
  end
  if spPos == 1 then
    for i = 1, 3 do
      tap(500, 406)
      sleep(200)
    end
  else
    swipes(spPos - 2)
    keepCapture()
    local ret, x, y = _findColorTriple(250, 60, 400, 60, "360A17-090909", 0, 1.0)
    local dx = 320
    print(x, dx)
    for i = 1, 3 do
      tap(dx, 406)
      sleep(200)
    end
  end
  tap(20, 320)
  tap(20, 320)
  sleep(3000)
  keepCapture()
  if cmpColor(640, 1270, "5400FF-101010", 1) == 1 then
    tap(640, 1270)
    sleep(3000)
  end
  keepCapture()
  if cmpColorEx("580|161|FFFFFF-101010,607|160|FFFFFF-101010,610|1238|000000-101010,634|1229|190F0A-101010", 1) == 1 then
    return 1
  end
  if run.data["称霸模式"] ~= "" then
    toast("称霸模式，阶段:" .. spStage)
    sleep(1000)
    local temp = tonumber(run.data["称霸位置"]) or 1
    spStage = spStage - 5 * temp
    tap(460 - 110 * temp, 160)
    sleep(1000)
    for i = 1, 3 do
      swipe(60, 350, 60, 1200, 100)
      sleep(500)
    end
    utils.mySwipe(60, 600, 60, 250)
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
    sleep(2000)
    return 1
  elseif spStage == 2 then
    tap(400, 720)
    sleep(2000)
    return 1
  elseif spStage == 3 then
    tap(400, 1150)
    sleep(2000)
    return 1
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
    sleep(2000)
    return 1
  end
end
function utils.intoSpecial1()
  data["信息"] = "进入车手联会"
  keepCapture()
  if cmpColorEx("702|1252|FFFFFF-101010,690|1252|3B2312-101010,670|1270|3B2312-101010", 1) == 1 then
  else
    utils.backToMain()
  end
  for i = 1, 5 do
    keepCapture()
    if cmpColor(88, 300, "FFFFFF-101010", 1) == 0 then
      tap(88, 300)
      sleep(1000)
    end
    sleep(200)
  end
  for i = 1, 8 do
    utils.mySwipe(550, 406, 200, 406)
  end
  sleep(1000)
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
  local dsPos = tonumber(run.data["车手联会位置"]) or 1
  if dsPos == 1 then
    for i = 1, 3 do
      tap(500, 406)
      sleep(200)
    end
  else
    swipes(dsPos - 2)
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
  tap(20, 320)
  tap(20, 320)
  sleep(3000)
  keepCapture()
  if cmpColor(658, 1274, "6412FB-101010", 1) == 1 then
    tap(658, 1270)
    sleep(2500)
  end
  for i = 1, 15 do
    swipe(60, 250, 60, 1200, 100)
    sleep(500)
  end
  sleep(1500)
  releaseCapture()
end
function utils.spChoose()
  local ret, x, y, x1, y1, x2, y2 = -1, -1, -1, -1, -1, -1, -1
  local fz = utils.defultCfg(run.data["分支选择"], "0")
  local wxdj = utils.defultCfg(run.data["危险等级"], "0")
  local ndbz = utils.defultCfg(run.data["难度不足"], "0")
  keepCapture()
  ret, x, y = _findColorTriple(368, 10, 370, 500, "3900FF-100410", 0, 1.0)
  if y ~= -1 and (fz == "0" or fz == "2") then
    local dx = y - 120
    utils.mySwipe(64, 200 + dx, 64, 200)
    sleep(200)
    print("支线微调", y)
    tap(300, 150)
    sleep(1500)
    tap(145, 790)
    toast("刷支线")
    sleep(1000)
    tap(116, 1100)
    sleep(1500)
    tap(116, 1100)
    sleep(1000)
    tap(116, 1100)
    return 1
  end
  x, y = findMultiColor(324, 58, 379, 502, "FFFFFF-050505", "4|2|FFFFFF-050505|7|3|FFFFFF-050505|11|5|FFFFFF-050505|15|7|FFFFFF-050505|21|9|FFFFFF-050505", 0, 1)
  x1, y1 = findMultiColor(73, 48, 515, 535, "FFFFFF-050505", "29|-2|FFFFFF-050505|-5|26|FFFFFF-050505|10|75|FFFFFF-050505|27|77|FFFFFF-050505", 0, 1)
  if y ~= -1 and (fz == "1" or fz == "2") then
    local dx = y1 - 124
    utils.mySwipe(64, 200 + dx, 64, 200)
    sleep(200)
    print("主线微调", y)
    tap(340, 160)
    sleep(1500)
    x2, y2 = findMultiColor(73, 48, 515, 535, "FFFFFF-050505", "29|-2|FFFFFF-050505|-5|26|FFFFFF-050505|10|75|FFFFFF-050505|27|77|FFFFFF-050505", 0, 1)
    local difficul = tonumber(wxdj) + 1 or 1
    for i = difficul, 1, -1 do
      keepCapture()
      ret, x, y = _findColorTriple(70, y2 + 587, 460, y2 + 620, "0089FF-100410", 2, 1.0)
      ret, x1, y1 = _findColorTriple(70, y2 + 587, 460, y2 + 620, "0089FF-100410", 0, 1.0)
      if x ~= -1 and 360 < x then
        if i == 1 then
          tap(x - 10, y + 100)
        elseif i == 2 then
          tap(x - 150, y + 100)
        elseif i == 3 then
          tap(x - 300, y + 100)
        end
      elseif ndbz == "0" then
        toast("少于三个难度时选最上面的难度")
        sleep(2000)
        tap(x - 10, y + 100)
      elseif ndbz == "1" then
        toast("少于三个难度时选最下面的难度")
        sleep(2000)
        tap(x1 + 10, y1 + 100)
      end
    end
    releaseCapture()
    toast("刷主线")
    sleep(2000)
    tap(116, 1100)
    sleep(1000)
    tap(116, 1100)
    sleep(1000)
    tap(116, 1100)
    sleep(2000)
    return 1
  end
  keepCapture()
  ret, x, y = _findColorTriple(410, 10, 410, 500, "FFFFFF-090910", 0, 1.0)
  ret, x2, y2 = _findColorTriple(360, 10, 360, 500, "5400FF-100410", 0, 1.0)
  if y ~= -1 and y2 ~= -1 and (fz == "1" or fz == "2") then
    tap(273, 848)
    toast("刷特殊主线")
    sleep(2000)
    tap(116, 1100)
    sleep(1500)
    tap(116, 110)
    sleep(1000)
    tap(116, 1100)
    return 1
  end
  return 0
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
  if cmpColorEx("190|560|FFFFFF-101010,540|330|2C180C-101010,550|1000|2C1A0C-101010", 1) == 1 then
    tap(192, 610)
  end
  keepCapture()
  if cmpColorEx("564|370|000000-101010,580|1132|5400FF-101010,584|1080|000000-101010", 1) == 1 then
    tap(580, 1130)
  end
  keepCapture()
  if cmpColorEx("99|587|FFFFFF-101010,152|542|FFFFFE-101010,116|684|14FBC3-101010", 1) == 1 then
    tap(99, 588)
  end
  keepCapture()
  if cmpColorEx("150|325|5E6822-101010,496|278|BDD045-101010,580|331|0C0C03-101010", 1) == 1 then
    tap(158, 692)
    for i = 1, 15 do
      tap(30, 1240)
      sleep(500)
    end
  end
  keepCapture()
  if cmpColorEx("394|446|FFFFFF-101010,88|718|FFFFFF-101010,594|200|000000-101010", 1) == 1 then
    tap(100, 600)
  end
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
  local tempCar = utils.defultCfg(run.data["杯赛用车"], {
    1,
    2,
    3,
    4
  })
  local mission = {
    "1",
    "1",
    "1",
    "1",
    "2",
    "1"
  }
  local my = {
    220,
    520,
    820,
    1120
  }
  tap(526, 1078)
  sleep(1000)
  for i = 1, #my do
    if utils.mycolor(222, my[i], "220D04-101010") == 1 then
      mission[i] = "0"
    else
      mission[i] = tempCar[i]
    end
  end
  tap(530, 810)
  sleep(1000)
  if cmpColorEx("165|360|00D8FF,161|445|00D8FF", 1) == 1 then
    toast("领奖励")
    tap(165, 360)
    tap(165, 360)
    sleep(2000)
    for i = 1, 20 do
      tap(60, 1242)
      sleep(200)
    end
  end
  dailyCup.carList = mission
end
function utils.waitStart()
  for di = 1, 200 do
    keepCapture()
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
      utils.rebootGame()
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
