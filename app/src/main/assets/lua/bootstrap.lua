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
  utils.newAutoDriver=function()
    _validateRoute(data['多人地图'],data['线路'])
    data['信息']='等待比赛开始'
    local ret=utils.waitStart();if ret~=1 then return ret end
    data['信息']='智能选路中'
    data['心跳时间']=os.time();data['超时状态']=false
    _driverBegin(data['线路'],data['项目']=='杯赛' and (run.data['杯赛两圈赛制'] or '')~='',data['氮气漂流'])
    local lastProgress=-1
    while true do
      local done,progress,info=_driverStep()
      if progress>=0 then
        data['路程']=progress
        if progress>lastProgress then lastProgress=progress;data['心跳时间']=os.time() end
      end
      data['信息']=info
      if done then break end
      if data['超时状态'] then _driverEnd();return -1 end
      sleep(30)
    end
    _driverEnd();data['路程']=0;data['多人地图']='';return 1
  end
  run.list={['多人上']=multiple1.operation,['多人下']=multiple2.operation,['杯赛']=cup.operation,
    ['特殊赛事']=spCup.operation,['车联']=dsCup.operation,['生涯']=career.operation,['每日战利品']=dailyCup.operation}
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
  local cycle=0
  repeat
    cycle=cycle+1;_sessionCycle(cycle);print('开始第 '..cycle..' 轮项目队列')
    local enabled=0
    for i=1,#UIconfig.projects do
      local cfg=UIconfig.projects[i]
      if UIconfig.ActiveProj[i] then
        enabled=enabled+1
        print('第 '..cycle..' 轮，开始项目 '..i..'：'..cfg['项目']..'，目标 '..tostring(cfg.data['次数'])..' 局')
        run.index=i;run.data=cfg.data;data['项目']=cfg['项目'];data['心跳时间']=os.time()
        _hud('项目 '..i..'：'..data['项目'])
        if data['项目']=='多人上' or data['项目']=='多人下' then
          currentMultiplayer.run()
        else
          _sessionProject(data['项目'],tonumber(run.data['次数']) or 1)
          utils.backToMain();utils.checkAuto();utils.initCar();run.project()
        end
        print('第 '..cycle..' 轮，项目 '..i..' 已完成：'..data['项目'])
        run.data={}
      end
    end
    if enabled==0 then error('没有启用的项目，无法循环执行') end
    print('第 '..cycle..' 轮项目队列已完成')
    if UIconfig.repeatProjects then sleep(1000) end
  until not UIconfig.repeatProjects
  _finished()
end
