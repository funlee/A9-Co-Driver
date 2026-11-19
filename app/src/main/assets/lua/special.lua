spCup = {}
local config = {
  main = "690|1240|FFFFFF-101010,690|1252|3B2312-101010,670|1270|3B2312-101010",
  selectMission = "614|67|5F2200-501902,620|1236|000000-101010,678|1250|3C2313-101010",
  spCupInfo = "580|160|FFFFFF-101010,606|160|FFFFFF-101010,680|1250|3C2313-101010",
  carport = "666|86|2E2E2E-101010,666|120|373737-101010,574|140|5400CF-201030",
  carInfo = "368|150|F9EF33-101010,302|142|F9EF33-101010,434|150|F9EF33-101010",
  noTicket = "538|1120|5400FF-290909,550|960|2C190C-101010,286|856|5400FF-101010",
  -- Start button and tutorial artwork, sampled away from the race overlay.
  startTutorial = "104|1080|12FBC3-101010,40|1210|12FBC3-101010,115|252|5400FF-101010,31|235|5400FF-101010,594|1129|23210F-101010",
  racing = "680|50|FFFFFF-101010,660|70|FFFFFF-101010",
  ending = "72|770|5400FF-101010,40|780|FFFFFF-101010,72|800|5400FF-101010",
  finish = "48|672|00D8FF-101010,100|326|FFFFFF-101010,678|1250|3C2313-101010",
  commonPrize = "87|1164|FFFFFF-101010,110|1102|311406-101010,68|1051|301307-101010",
  tips = "190|808|FFFFFF-101010,190|912|12FBC3-101010"
}
function spCup.tutorialButton(screen)
  if not screen or screen.scene~='unknown' then return nil end
  local text=(screen.text or ''):gsub('%s','')
  if not text:find('不甘落后',1,true) or not text:find('氮气',1,true) or not text:find('滑流',1,true) then return nil end
  for _,word in ipairs(screen.words or {}) do
    if word.text:gsub('%s','')=='开始' and word.x>=980 and word.y>=560 then return word end
  end
end

function spCup.atStartTutorial(screen)
  if spCup.tutorialButton(screen) then return true end
  keepCapture()
  return cmpColorEx(config.startTutorial, 1) == 1
end

spCup.operation = {
  {
    config.startTutorial,
    function(button)
      data["信息"] = "开赛教学：点击开始"
      print("特殊赛事：已识别开赛教学，点击开始")
      utils.prepareTaskRoute()
      if button then tap(719-button.y,button.x) else tap(73,1157) end
      local started=tickCount()
      local carSince=nil;local carFrame=nil;local carFrames=0
      while tickCount()-started<90000 do
        sleep(500)
        local screen=_soloEventUi()
        if screen.scene=='race' then return end
        -- The car page can flash behind a closing tutorial immediately before loading.
        -- Only a stable, readable car page may send control back to fuel selection.
        if screen.scene=='car' and screen.fuel and screen.fuel>=0 then
          if not carSince then carSince=tickCount() end
          if screen.frameId and screen.frameId~=carFrame then carFrame=screen.frameId;carFrames=carFrames+1 end
          if carFrames>=2 and tickCount()-carSince>=3000 then return end
        else carSince=nil;carFrame=nil;carFrames=0 end
        if screen.scene=='noTicket' then return 'noTicket' end
        data['信息']='特赛开赛中，等待比赛画面'
      end
      error("特殊赛事点击开始后未确认比赛画面，保留当前任务")
    end
  },
  {
    config.main,
    function(screen)
      data["信息"] = "进入特殊赛事"
      if utils.intoSpecial(screen) == 0 then
        return -1
      end
      sleep(2000)
    end
  },
  {
    config.spCupInfo,
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
      utils.prepareTaskRoute()
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
return spCup
