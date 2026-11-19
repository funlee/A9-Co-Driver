run = {}
run.data = {}
run.index = 1
run.dataList = {
  ["多人上"] = function()
    data["多人"] = data["多人"] + 1
  end,
  ["多人下"] = function()
    data["多人"] = data["多人"] + 1
  end,
  ["杯赛"] = function()
    data["杯赛"] = data["杯赛"] + 1
  end,
  ["特殊赛事"] = function()
    data["特赛"] = data["特赛"] + 1
  end,
  ["车联"] = function()
    data["车联"] = data["车联"] + 1
  end,
  ["每日战利品"] = function()
    data["杯赛"] = data["杯赛"] + 1
  end
}
function run.main()
  while true do
    for i = 1, 20 do
      local cfg = UIconfig.projects[i]
      data["项目"] = cfg["项目"]
      run.index = i
      if UIconfig.ActiveProj[i] and data["项目"] ~= "无" then
        utils.backToMain()
        utils.checkAuto()
        utils.initCar()
        run.data = cfg.data
        run.project()
        run.data = {}
      end
    end
    run.restHandle()
  end
end
function run.project()
  run.nitro()
  local ret = 0
  local times = tonumber(run.data["次数"]) or 1
  for i = 1, times do
    if data["项目"] == "每日战利品" then
      local reason=dailyCup.skipReason()
      if reason~='' then return reason end
    end
    data["心跳时间"] = os.time()
    -- Reward menus must not be mistaken for a race or expand the race panel.
    if _sessionLegacyPrepare then _sessionLegacyPrepare() end
    data["心跳时间"] = os.time()
    local soloRaceSeen=false
    while true do
      local carHandler=run.carDetails and run.carDetails[data['项目']]
      local screen=carHandler and _soloEventUi()
      local tutorial=data['项目']=='特殊赛事' and spCup and spCup.tutorialButton and spCup.tutorialButton(screen)
      if screen and screen.scene=='race' then soloRaceSeen=true end
      local homeHandler=run.homeEntries and run.homeEntries[data['项目']]
      if screen and screen.scene=='home' and homeHandler then
        ret=homeHandler(screen)
        if ret~=nil then goto lbl_129 end
      elseif screen and screen.scene=='exitGame' then
        utils.backToMain()
      elseif screen and screen.scene=='wideCupList' and
          (data['项目']=='每日战利品' or data['项目']=='杯赛') then
        data['信息']='选择配置的赛事'
        utils.chooseCup(run.data['杯赛位置'] or '2',true)
        sleep(2500)
      elseif screen and screen.scene=='profileUnlock' and dailyCup then
        dailyCup.dismissProfile(screen)
      elseif screen and screen.scene=='clubRewards' and dailyCup then
        dailyCup.returnToSolo(screen)
      elseif screen and data['项目']=='每日战利品' and screen.scene=='dailyRewardOnly' then
        sleep(1);sleep(2000)
        local fresh=_soloEventUi()
        if fresh.scene=='dailyRewardOnly' and fresh.frameId~=screen.frameId then return 'dailyExpired' end
      elseif screen and data['项目']=='每日战利品' and dailyCup and dailyCup.dismissClaimSpotlight
          and dailyCup.dismissClaimSpotlight(screen) then
        -- Read the configured event carousel again after dismissing the claim hint.
      elseif screen and screen.scene=='lockedCar' then
        return 'vehicleLocked'
      elseif screen and screen.scene=='car' then
        ret=carHandler()
        if ret~=nil then goto lbl_129 end
      elseif screen and screen.scene=='cars' and data['项目']=='杯赛' then
        ret=cup.operation[4][2]()
        if ret~=nil then goto lbl_129 end
      elseif screen and screen.scene=='cars' and data['项目']=='每日战利品' then
        ret=dailyCup.operation[4][2]()
        if ret~=nil then goto lbl_129 end
      elseif screen and screen.scene=='event' and data['项目']=='杯赛' and cup.openRaceGarage(screen) then
        -- The verified button has been pressed. Read the new screen next.
      elseif screen and screen.scene=='event' and data['项目']=='每日战利品' and dailyCup.openRaceGarage(screen) then
        -- On wide displays the older event pattern may miss the verified Race button.
      elseif tutorial then
        ret=spCup.operation[1][2](tutorial)
        if ret~=nil then goto lbl_129 end
      elseif not soloRaceSeen and screen and data['项目']=='特殊赛事' and utils.continueSpecialMission(screen) then
        -- Continue from the configured stage's pre-race reward overview.
      elseif soloRaceSeen and screen and cup.isSettlement(screen.scene) then
        ret=dailyCup.settle(data['项目'],_soloEventUi)
        if ret~=nil then goto lbl_129 end
      else
      for im1 = 1, #run.list[data["项目"]] do
        keepCapture()
        if cmpColorEx(run.list[data["项目"]][im1][1], 1) == 1 then
          ret = run.list[data["项目"]][im1][2]()
          if ret ~= nil then
            goto lbl_129
          end
        end
        sleep(10)
      end
      end
      utils.tipsHandle()
      run.rebootHandle()
      sleep(500)
    end
    ::lbl_129::
    if ret == -1 then
      error(data["项目"].."流程未确认完成，保留当前任务，禁止切换到下一赛事")
    elseif type(ret) == "string" then
      return ret
    else
      run.dataList[data["项目"]]()
      _sessionLegacyCompleted()
      utils.selectNextTaskCar=true
    end
  end
  return ret
end
function run.nitro()
  utils.selectNextTaskCar=false
  data["氮气漂流"] = false
  data["用车情况"] = {
    0,
    0,
    0
  }
  data["用车次数"] = {}
end
