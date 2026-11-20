-- Local host compatibility and orchestration. No original account or network code is loaded.
function splitStr(s,delimiter)
  local out={}; local start=1
  while true do
    local p=string.find(s,delimiter,start,true)
    if not p then out[#out+1]=string.sub(s,start); break end
    out[#out+1]=string.sub(s,start,p-1); start=p+#delimiter
  end
  return out
end
function _findColorTriple(...) local x,y=findColor(...);return x==-1 and -1 or 0,x,y end
function _findPicTriple(...) local x,y=findPic(...);return x==-1 and -1 or 0,x,y end
utf8.left=function(s,n) local p=utf8.offset(s,n+1); return p and string.sub(s,1,p-1) or s end
utf8.right=function(s,n) local p=utf8.offset(s,-n); return p and string.sub(s,p) or s end
utf8.strCut=function(s,p,n) return string.sub(s,1,p-1)..string.sub(s,p+n) end
jsonLib={encode=_jsonEncode,decode=_jsonDecode}
myocr={}
myocr.smartOcrEx=function(x1,y1,x2,y2) return _textOcr(x1,y1,x2,y2) end
myocr.smartOcr=myocr.smartOcrEx
myocr.myOcrEx=myocr.smartOcrEx
myocr.tuling=function(x1,y1,x2,y2,old) return _digit(x1,y1,x2,y2) end
myocr.dictOcr=myocr.tuling
myocr.level=myocr.smartOcrEx
dialog={newLayout=function(message) error(message) end}
mt={dialog=function(title,lines) error(title..': '..table.concat(lines,'\n')) end}

local function withPopupRecovery(read)
  if not read then return end
  return function(...)
    local screen=read(...)
    if not screen or screen.scene~='interruptingPopup' then return screen end
    local previous,clicks=nil,0
    local deadline=tickCount()+18000
    local status=data and data['信息']
    while screen and screen.scene=='interruptingPopup' and tickCount()<deadline do
      local action=screen.dismissPopup
      local before=previous and previous.dismissPopup
      if data then data['信息']='正在确认弹窗关闭控件' end
      if action and before and screen.frameId and previous.frameId and screen.frameId>previous.frameId
          and action.label==before.label and math.abs(action.x-before.x)<=5 and math.abs(action.y-before.y)<=5 then
        if clicks>=5 then break end
        print('关闭游戏弹窗：'..action.label)
        tap(719-action.y,action.x)
        clicks=clicks+1;previous=nil
        sleep(1);sleep(850)
      else
        previous=screen
        sleep(1);sleep(250)
      end
      screen=read(...)
    end
    if screen and screen.scene=='interruptingPopup' then
      error(clicks>0 and '游戏弹窗关闭后仍遮挡画面，已保留任务进度'
        or '游戏弹窗遮挡画面，未确认可点的关闭控件，已保留任务进度')
    end
    if data then data['信息']=status end
    return screen
  end
end
_soloEventUi=withPopupRecovery(_soloEventUi)
_dailyEventUi=withPopupRecovery(_dailyEventUi)
_multiplayerUi=withPopupRecovery(_multiplayerUi)
_rewardUi=withPopupRecovery(_rewardUi)
_specialEventList=withPopupRecovery(_specialEventList)
_cupCards=withPopupRecovery(_cupCards)
_garageFilterState=withPopupRecovery(_garageFilterState)
_popupUi=withPopupRecovery(_popupUi)

function localInit()
  data['线路集合']=ROUTES
  data['配置路径']=LOCAL_ROOT..'/local-config.json'
  data['包名']=UIconfig.gamePkg
  utils.getPkgName=function() return UIconfig.gamePkg end
  utils.releseRecourse=function() end
  utils.checkCar=function() error('请在项目设置中填写车辆序号') end
  utils.checkGame=function() return true end
  utils.rebootGame=function() error('游戏超时或需要重启，请手动恢复后重新开始') end
  utils.stopGame=function() error('此本地版使用系统无障碍，强制关闭游戏需要手动操作') end
  utils.findApkList=function(pkg) return pkg==UIconfig.gamePkg end
  utils.getInstallPkg=function() return {UIconfig.gamePkg} end
  utils.localOCR=function(x1,y1,x2,y2) return _textOcr(x1,y1,x2,y2) end
  utils.bdOCR1=function() return '' end
  utils.getMapName=function(text)
    if ROUTES[text] then
      _validateRoute(text,ROUTES[text]);data['多人地图']=_routeDisplay(text);data['线路']=ROUTES[text];return 1
    end
    local key,label=_mapMatch(text)
    if not key then error('地图无法唯一匹配：'..text..'。请在路线页核对并手动选择') end
    if not ROUTES[key] then error('地图缺少本地路线：'..key) end
    _validateRoute(key,ROUTES[key]);data['多人地图']=label;data['线路']=ROUTES[key];return 1
  end
  utils.taskRouteAuto=function()
    return (data['项目']=='杯赛' or data['项目']=='每日战利品' or data['项目']=='特殊赛事') and tostring(run.data['路线来源'] or '2')=='2'
  end
  utils.prepareTaskRoute=function(reset)
    if utils.taskRouteAuto() then
      if reset then data['多人地图']='';data['线路']='' end
      return
    end
    local source=tostring(run.data['路线来源'] or '0')
    if source=='0' then
      local key=run.data['已配置地图'] or ''
      if key=='' or not ROUTES[key] then error('请先在任务详情中选择已配置地图') end
      data['多人地图']=_routeDisplay(key);data['线路']=ROUTES[key]
    elseif source=='1' then
      data['多人地图']='自定义跑法';data['线路']=run.data['跑法代码'] or ''
    else error('路线来源无效，请重新配置任务路线') end
    if not _validateRoute(data['多人地图'],data['线路']) then error('任务跑法为空或无效，请在任务详情或地图配置中完善') end
  end
  utils.newAutoDriver=function()
    _validateRoute(data['多人地图'],data['线路'])
    data['信息']='等待比赛开始'
    local ret=utils.waitStart();if ret~=1 then return ret end
    data['信息']='智能选路中'
    data['心跳时间']=os.time();data['超时状态']=false
    if _sessionLegacyMap then _sessionLegacyMap(data['多人地图'] or '',not _validateRoute(data['多人地图'],data['线路'])) end
    _driverBegin(data['线路'],false,false)
    local lastProgress=-1
    local settlement=false
    while true do
      local done,progress,info,confirmedSettlement,popup=_driverStep()
      if popup then
        if data['项目']=='多人上' or data['项目']=='多人下' then _multiplayerUi(false,false) else _soloEventUi() end
      end
      if progress>=0 then
        data['路程']=progress
        if progress>lastProgress then lastProgress=progress;data['心跳时间']=os.time() end
      end
      data['信息']=info
      if done then settlement=confirmedSettlement;break end
      if data['超时状态'] then _driverEnd();return -1 end
      sleep(30)
    end
    _driverEnd();data['路程']=0;data['多人地图']='';return settlement and 'settlement' or 1
  end
  utils.collectRaceRewards=function()
    if not _legacyRewardSample then return end
    local sample=_legacyRewardSample()
    if not sample.rewardPage then return end
    local started=tickCount()
    repeat
      if sample.rewardCanContinue and (sample.rewardReady or tickCount()-started>=8000) then
        _sessionLegacyRewards(sample.rewardCoins or -1,sample.rewardFame or -1);return
      end
      if tickCount()-started>=20000 then error('比赛奖励结算超时，请检查画面') end
      data['信息']='等待结算完成并识别本局收益';sleep(400);sample=_legacyRewardSample()
    until not sample.rewardPage
  end
  run.list={['多人上']=multiple1.operation,['多人下']=multiple2.operation,['杯赛']=cup.operation,
    ['特殊赛事']=spCup.operation,['车联']=dsCup.operation,['每日战利品']=dailyCup.operation}
  run.carDetails={['杯赛']=function()return cup.operation[5][2]()end,['特殊赛事']=function()return spCup.operation[5][2]()end,
    ['车联']=function()return dsCup.operation[4][2]()end,['每日战利品']=function()return dailyCup.operation[5][2]()end}
  run.homeEntries={['杯赛']=function(screen)utils.openHomeTab(screen,'daily')end,
    ['每日战利品']=function(screen)utils.openHomeTab(screen,'daily')end,
    ['特殊赛事']=function(screen)return spCup.operation[2][2](screen)end,
    ['车联']=function(screen)return dsCup.operation[1][2](screen)end}
  run.rebootHandle=function() if data['超时状态'] then error('界面停留超过设定时限，请检查游戏') end end
  run.restHandle=function() sleep(1000) end
end

function monitor()
  while true do
    local status=tostring(data['信息'])..'\n'..tostring(data['多人地图'])..'  '..tostring(data['路程'])..'%'
    _hud(status)
    if data['心跳时间']>0 and os.time()-data['心跳时间']>data['等待时间'] then data['超时状态']=true end
    sleep(500)
  end
end

function main()
  localInit()
  beginThread(monitor)
  if UIconfig.routeOnly~='' then
    data['项目']='手动跑图';data['多人地图']=UIconfig.routeOnly;data['线路']=assert(ROUTES[UIconfig.routeOnly],'路线不存在')
    data['心跳时间']=os.time();data['信息']='请手动进入比赛，等待比赛画面'
    local ready=false
    for i=1,360 do keepCapture();if cmpColorEx('680|50|FFFFFF-101010,660|70|FFFFFF-101010',1)==1 then ready=true;break end;sleep(500) end
    if not ready then error('等待比赛超时') end
    utils.newAutoDriver();_finished();return
  end
  local projects={}
  for i=1,#UIconfig.projects do
    if UIconfig.ActiveProj[i] then projects[#projects+1]=i end
  end
  if #projects==0 then error('没有启用的项目，无法循环执行') end

  local resume=_queueResume and _queueResume() or {cycle=1,index=1,completed=0}
  local rewardCycle=resume.rewardCycle or 0
  local cycle=resume.cycle-1
  while true do
    cycle=cycle+1;_sessionCycle(cycle);print('开始第 '..cycle..' 轮项目队列')
    if currentRewards and currentRewards.enabled() and cycle>rewardCycle then
      data['心跳时间']=0
      data['超时状态']=false
      utils.backToMain()
      currentRewards.collect()
      rewardCycle=cycle
      if _queueRewardsDone then _queueRewardsDone(cycle) end
    end
    for _,i in ipairs(projects) do
      local cfg=UIconfig.projects[i]
      if cycle>resume.cycle or i>=resume.index then
        local completed=_queueEnter and _queueEnter(cycle,i,tonumber(cfg.data['次数']) or 1) or 0
        print('第 '..cycle..' 轮，开始项目 '..i..'：'..cfg['项目']..'，目标 '..tostring(cfg.data['次数'])..' 局')
        run.index=i;run.data=cfg.data;data['项目']=cfg['项目'];data['心跳时间']=os.time()
        _hud('项目 '..i..'：'..data['项目'])
        local outcome=nil
        if data['项目']=='每日战利品' then outcome=dailyCup.skipReason() end
        if outcome and outcome~='' then
          data['信息']='00:00–00:30，跳过每日战利品'
          _hud(data['信息'])
        elseif data['项目']=='多人上' or data['项目']=='多人下' then
          -- Task boundaries return to a shared entry point; multiplayer need not interpret cup pages.
          local restoring=resume.restored and cycle==resume.cycle and i==resume.index
          local tutorial=restoring and spCup and spCup.tutorialButton(_soloEventUi())
          if not tutorial then utils.backToMain() end
          outcome=currentMultiplayer.run(not not tutorial)
        else
          _sessionProject(data['项目'],tonumber(run.data['次数']) or 1)
          local original=run.data['次数']
          run.data['次数']=tostring(math.max(0,(tonumber(original) or 1)-completed))
          if tonumber(run.data['次数'])>0 then
            local atCheckpoint=cycle==resume.cycle and i==resume.index
            local savedScreen=atCheckpoint and resume.restored and _soloEventUi()
            local resumingTutorial=atCheckpoint
              and data['项目']=='特殊赛事' and spCup and spCup.atStartTutorial and spCup.atStartTutorial(savedScreen)
            local resumingSpecialStage=savedScreen and data['项目']=='特殊赛事' and utils.specialMissionContinue(savedScreen)
            if savedScreen and savedScreen.scene=='lockedCar' and
                (data['项目']~='每日战利品' or #utils.taskCars()<=1) then
              outcome='vehicleLocked'
            else
              if not resumingTutorial and not resumingSpecialStage then
                utils.backToMain();utils.checkAuto();utils.initCar()
              end
              outcome=run.project()
            end
          end
          run.data['次数']=original
        end
        if outcome=='dailyUnmatched' then
          local message='跳过每日战利品：未能在配置位置 '..(cfg.data['杯赛位置'] or '2')..' 确认赛事'
          data['信息']=message
          _taskNotice(message,4000)
          sleep(4200)
        end
        if _queueDone then _queueDone() end
        if outcome=='dailyQuietHours' then
          print('项目 '..i..' 已跳过：每日战利品 00:00–00:30 不执行')
        elseif outcome=='dailyExpired' then
          print('第 '..cycle..' 轮，项目 '..i..' 已跳过：配置位置的每日战利品只剩奖励页，无法开赛')
        elseif outcome=='dailyUnmatched' then
          print('第 '..cycle..' 轮，项目 '..i..' 已跳过：未匹配配置位置的每日战利品')
        elseif outcome=='fuelEmpty' then
          print('第 '..cycle..' 轮，项目 '..i..' 因车辆全部无油已跳过：'..data['项目'])
        elseif outcome=='vehicleLocked' then
          print('第 '..cycle..' 轮，项目 '..i..' 因车辆未解锁已跳过：'..data['项目'])
        elseif outcome=='vehicleUnavailable' then
          print('第 '..cycle..' 轮，项目 '..i..' 的配置车辆均未确认可开赛，已跳过：'..data['项目'])
        elseif outcome=='noTicket' then
          print('第 '..cycle..' 轮，项目 '..i..' 因门票不足已跳过剩余场次：'..data['项目'])
        elseif outcome==-1 then
          print('第 '..cycle..' 轮，项目 '..i..' 未完成配置场次，已结束本项目：'..data['项目'])
        else print('第 '..cycle..' 轮，项目 '..i..' 已完成：'..data['项目']) end
        run.data={}
      end
    end
    print('第 '..cycle..' 轮项目队列已完成')
    sleep(1000)
  end
end
