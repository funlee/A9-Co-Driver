run = {}
run.data = {}
run.index = 1
run.dataList = {
  ["世界系列赛"] = function()
    data["多人"] = data["多人"] + 1
  end,
  ["周末系列赛"] = function()
    data["多人"] = data["多人"] + 1
  end,
  ["大师系列赛"] = function()
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
  if data['项目']=='车联' then dsCup.resetDifficulty() end
  if _usedCarEntries then
    for _,entry in ipairs(_usedCarEntries()) do
      data['用车次数'][string.format('car%d',entry)]=1
    end
  end
  local ret = 0
  local times = tonumber(run.data["次数"]) or 1
  local cupTask=data['项目']=='杯赛'
  for i = 1, times do
    data["心跳时间"] = os.time()
    -- Reward menus must not be mistaken for a race or expand the race panel.
    if _sessionLegacyPrepare then _sessionLegacyPrepare() end
    data["心跳时间"] = os.time()
    local soloRaceSeen=false
    local clubRewardChecked=false
    local homeStreak=0
    utils.soloStartAt=nil;run.challengeConfirmed=false
    ::observeRace::
    while true do
      local carHandler=run.carDetails and run.carDetails[data['项目']]
      local special=data['项目']=='特殊赛事'
      local readRoute=special and utils.soloStartAt and utils.taskRouteAuto and utils.taskRouteAuto() and data['线路']==''
      local screen=carHandler and _soloEventUi(not not readRoute)
      local scene=screen and screen.scene
      if special and screen and spCup and spCup.observeStart then spCup.observeStart(screen) end
      if utils.soloStartAt and scene~='race' then
        dailyCup.confirmChallenge(screen)
        if tickCount()-utils.soloStartAt>=120000 then error('已点击开始，但未确认进入比赛，保留当前任务') end
      end
      if cupTask and screen then
        if scene=='home' or scene=='cupList' or scene=='wideCupList' or scene=='cupClaimSpotlight' then
          run.dailyLoot=false
        elseif screen.dailyLoot then
          run.dailyLoot=true
        end
        if run.dailyLoot and not soloRaceSeen and not utils.soloStartAt and scene~='race' then
          local reason=dailyCup.skipReason()
          if reason~='' then return reason end
        end
      end
      local tutorial=data['项目']=='特殊赛事' and spCup and spCup.tutorialButton and spCup.tutorialButton(screen)
      if scene=='race' then soloRaceSeen=true;utils.soloStartAt=nil end
      local homeHandler=run.homeEntries and run.homeEntries[data['项目']]
      if screen and screen.scene~='home' then homeStreak=0 end
      if screen and screen.scene=='home' and homeHandler then
        homeStreak=homeStreak+1
        -- 入口连续几次没能离开主界面就停任务，不再空转等看门狗。
        if homeStreak>3 then error('已连续 '..homeStreak..' 次回到游戏主界面仍未进入'..data['项目']..'，已保留当前任务') end
        ret=homeHandler(screen)
        if ret~=nil then goto lbl_129 end
      elseif screen and screen.scene=='exitGame' then
        utils.backToMain()
      elseif cupTask and (scene=='cupList' or scene=='wideCupList' or scene=='cupClaimSpotlight') then
        data['信息']='选择配置的赛事'
        local chosen=utils.chooseCup(run.data['杯赛位置'] or '2',scene=='wideCupList' or screen.wide,true)
        if type(chosen)=='string' or chosen==0 then return chosen==0 and -1 or chosen end
        sleep(2500)
      elseif screen and screen.scene=='profileUnlock' and dailyCup then
        dailyCup.dismissProfile(screen)
      elseif screen and screen.scene=='clubRewards' and dailyCup then
        dailyCup.returnToSolo(screen)
      elseif cupTask and scene=='dailyRewardOnly' then
        sleep(1);sleep(2000)
        local fresh=_soloEventUi()
        if fresh.scene=='dailyRewardOnly' and fresh.frameId~=screen.frameId then return 'dailyExpired' end
      elseif scene=='noTicket' and data['项目']=='车联' then
        -- 车联按危险值收费：文字认出「空危险槽」即跳过，不依赖 panel 配色探针。
        utils.soloStartAt=nil
        ret=run.list[data['项目']][6][2]()
        if ret~=nil then goto lbl_129 end
      elseif utils.soloStartAt and scene=='noTicket' then
        utils.soloStartAt=nil
        ret=run.list[data['项目']][6][2]()
        if ret~=nil then goto lbl_129 end
      elseif scene=='lockedCar' then
        if #utils.taskCars()==1 then return 'vehicleLocked' end
        utils.backToGarage()
      elseif scene=='car' then
        ret=carHandler()
        if ret~=nil then goto lbl_129 end
      elseif cupTask and scene=='cars' then
        ret=cup.operation[4][2]()
        if ret~=nil then goto lbl_129 end
      elseif cupTask and screen and screen.dailyLoot and not clubRewardChecked
          and run.data['是否领取俱乐部奖励']=='0' and (scene=='event' or scene=='unknown') then
        clubRewardChecked=true
        utils.dailyPrize()
      elseif cupTask and scene=='event' and cup.openRaceGarage(screen) then
        -- The verified button has been pressed. Read the new screen next.
      elseif tutorial then
        ret=spCup.operation[1][2](tutorial)
        if ret~=nil then goto lbl_129 end
      elseif not soloRaceSeen and screen and data['项目']=='特殊赛事'
          and (scene=='missionRewards' or scene=='specialRanking' or utils.specialStageList(screen)) then
        if not utils.continueSpecialMission(screen) then
          utils.selectSpecialStage(screen)
        end
      elseif soloRaceSeen and screen and cup.isSettlement(screen.scene) then
        ret=dailyCup.settle(data['项目'],_soloEventUi)
        if ret~=nil then goto lbl_129 end
      elseif utils.proceedTipPage and utils.proceedTipPage(screen) then
        -- 说明页已点击继续，下一轮重新识别画面
        else
          local operations=run.list[data['项目']]
          local matched=false
          for im1 = 1, #operations do
            keepCapture()
            -- OCR is supplementary; a missed label must not block the color dispatch.
            if cmpColorEx(operations[im1][1], 1) == 1 then
              matched=true
              if operations[im1][1]=='680|50|FFFFFF-101010,660|70|FFFFFF-101010' then
                soloRaceSeen=true;utils.soloStartAt=nil
              end
              ret = operations[im1][2]()
              if type(ret)=="string" then return ret end
              if ret ~= nil then goto lbl_129 end
              if daily and im1<=2 then
                break  -- Re-read the destination before allowing any race actions.
              end
            end
            sleep(10)
          end
          -- 车手联会的旧配色已不匹配当前页面：没有色键命中时用文字确认任务页再选任务。
          if not matched and data['项目']=='车联' and utils.carClubTaskPage(_soloEventUi()) then
            ret=dsCup.operation[2][2]()
            if type(ret)=="string" then return ret end
            if ret ~= nil then goto lbl_129 end
          end
        end
      utils.tipsHandle()
      run.rebootHandle()
      sleep(500)
    end
    ::lbl_129::
    if ret == -1 then
      if cupTask and not data['超时状态'] then return ret end
      error(data["项目"].."流程未确认完成，保留当前任务，禁止切换到下一赛事")
    elseif type(ret) == "string" then
      return ret
    else
      -- A monitor dismissal can invalidate the final action. Observe this race again
      -- without increasing its count or resetting the confirmed start.
      if _sessionLegacyCompleted()==false then goto observeRace end
      run.dataList[data["项目"]]()
      utils.selectNextTaskCar=true
      utils.verifiedDailyCar=false
    end
  end
  return ret
end
function run.nitro()
  run.dailyLoot=false
  utils.selectNextTaskCar=false
  utils.verifiedDailyCar=false
  data["氮气漂流"] = false
  data["用车情况"] = {
    0,
    0,
    0
  }
  data["用车次数"] = {}
end
