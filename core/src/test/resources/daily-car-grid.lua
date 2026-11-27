data={['项目']='每日战利品',['用车次数']={}}
run={data={['用车模式']='1',['选车方式']='0',['杯赛用车']={12,22}}}
local frame,offset,resets,chosen=0,scenario=='large' and 150 or 8,0,0
local tapped={}
local entrySwipes=0
local confirmCase=scenario:match('^confirm%-(.*)')
local fullReads=0
local emptyCase=scenario:match('^empty%-(.*)')
local emptyFullReads=0
local endCase=scenario:match('^end%-(.*)')
local filtered=endCase or scenario=='filtered-slide' or scenario=='filtered-click' or confirmCase or emptyCase
if filtered then
  run.data['选车筛选']='0,1,2'
  utils.configureGarageFilter=function()return true end
  if scenario:find('click') then run.data['选车方式']='1' end
end
if filtered or scenario=='at-start' then offset=0 end
if emptyCase=='cup' then data['项目']='杯赛';run.data['选车方式']='1' end
if emptyCase=='special' then data['项目']='特殊赛事' end
if emptyCase=='club' then data['项目']='车联' end
if emptyCase=='unfiltered' then run.data['选车筛选']='' end
local blankReads=(emptyCase or endCase) and 0 or 2
local bounceReads=0
local returns=0
function print()end
function keepCapture()end
local function signature(index)
  local parts,seed={},index*197
  for i=1,80 do
    seed=(seed*48271)%2147483647
    parts[i]=seed%2==0 and '0' or '1'
  end
  return table.concat(parts)
end
function _soloGarageUi(fast)
  frame=frame+250
  if emptyCase and (emptyCase~='recovered' or frame==250) then
    if not fast then emptyFullReads=emptyFullReads+1 end
    if emptyCase=='timeout' then data['超时状态']=true end
    return {scene=(emptyCase=='unknown' or emptyCase=='focused') and 'unknown' or 'cars',
      focusedGarage=emptyCase=='focused',
      frameId=emptyCase~='missing-frame' and (emptyCase=='stale' and 250 or frame) or nil,
      gridOnly=fast,garageEmpty=not fast and emptyCase~='unconfirmed' and (emptyCase~='delayed' or emptyFullReads>1),
      garage={start=false,left=-1,pitch=474,cards={}}}
  end
  if blankReads>0 then
    blankReads=blankReads-1
    return {scene='cars',frameId=frame,gridOnly=fast,garage={start=false,left=-1,pitch=474,cards={}}}
  end
  local cards={}
  for i=1,endCase and math.min(6,8-offset) or 6 do
    local number=offset+i
    cards[i]={index=i,x=280+math.floor((i-1)/2)*474,y=i%2==1 and 308 or 550,
      signature=signature(number),score=fast and '' or tostring(5900-number),model=scenario=='model' and 'CCXR' or '',
      availability=number==12 and (scenario=='locked' or scenario=='locked-only') and 'locked' or number==12 and (scenario=='star-card' or scenario=='star-only') and 'starUp' or 'unknown'}
  end
  local atStart=offset==0 and bounceReads==0
  local left=atStart and 97 or offset==0 and 376 or 200
  bounceReads=math.max(0,bounceReads-1)
  local garage={start=atStart,leadingClear=offset==0,left=left,pitch=474,cards=cards}
  if endCase and offset==4 and endCase~='unconfirmed' then garage.lastIndex=#cards end
  if not fast and confirmCase then
    fullReads=fullReads+1
    if confirmCase=='home' or confirmCase=='car-click' then
      return {scene=confirmCase=='home' and 'home' or 'car',frameId=frame}
    end
    if confirmCase=='obscured' or fullReads==1 then return {scene='unknown',frameId=frame} end
    if confirmCase=='moving' then garage.left=left+fullReads*10 end
    if confirmCase=='stale' then frame=100000 end
    return {scene='unknown',frameId=frame,focusedGarage=true,garage=garage}
  end
  return {scene='cars',frameId=frame,gridOnly=fast,garage=garage}
end
function swipe(y,from,endY,to,duration)
  if y==290 and endY==290 and from==70 and to==1200 then
    assert(duration==100 and frame>0,'Start the entry swipe sequence on a confirmed grid')
    entrySwipes=entrySwipes+1
    resets=resets+1
    if scenario=='large' then offset=math.max(0,offset-4)
    elseif scenario~='stuck' then offset=0 end
    blankReads=4
    if offset==0 then bounceReads=4 end
    return
  end
  assert(blankReads==0,'Wait for the car cards to reappear before dragging again')
  assert(bounceReads==0,'Wait for the visible first column to finish bouncing')
  if to>from then
    resets=resets+1
    if scenario=='large' then offset=math.max(0,offset-4)
    elseif scenario~='stuck' then offset=0 end
    blankReads=4
    if offset==0 then bounceReads=4 end
  else
    assert(resets>=1 or filtered,'Do not count cars before a verified origin')
    offset=endCase and math.min(4,offset+2) or offset+2
  end
end
function tap(y,x)
  chosen=offset+2*math.floor((x-280)/474)+(y==169 and 2 or 1)
  tapped[#tapped+1]=chosen
end
function _soloEventUi()
  return {scene='car',soloScore=tostring(5900-chosen+((scenario=='mismatch' or scenario=='model') and 1 or 0)),
    soloName=scenario=='model' and 'Koenigsegg CCXR' or 'Test car'}
end
utils.carFuel=function()
  if scenario=='star-detail' and chosen==12 then return 'starUp',_soloEventUi() end
  if scenario=='locked-detail' and chosen==12 then return 'vehicleLocked' end
  return 2,_soloEventUi()
end
utils.backToGarage=function()returns=returns+1 end
function test()
  if emptyCase then
    function toast()end
    local ok,result=pcall(utils.chooseCarCup,{1})
    if emptyCase=='filtered' or emptyCase=='cup' or emptyCase=='special' or emptyCase=='club' or emptyCase=='focused' or emptyCase=='delayed' or emptyCase=='unfiltered' then
      assert(ok and result=='garageEmpty' and #tapped==0 and resets==0 and not next(data['用车次数']),tostring(result))
      assert(run.data['选车筛选']==(emptyCase=='unfiltered' and '' or '0,1,2'),'Keep configured filters unchanged')
    elseif emptyCase=='recovered' then
      assert(ok and result==true and tapped[1]==1,'Transient empty list must recover')
    else
      assert(not ok and #tapped==0 and not next(data['用车次数']),'Unconfirmed/stale/timeout is not a normal skip: '..tostring(result))
    end
  elseif endCase then
    if endCase=='confirmed' then
      local cars={5,10,1,6,8,7,2}
      for i=1,5 do assert(utils.chooseCarCup(cars)==true) end
      assert(table.concat(tapped,',')=='5,1,6,8,7')
      assert(utils.chooseCarCup({10,20})=='vehicleUnavailable' and #tapped==5)
    else
      local ok=pcall(utils.chooseCarCup,{10,1})
      assert(not ok and #tapped==0,'A stalled drag without an end marker must not skip the car')
    end
  elseif confirmCase then
    function toast()end
    local ok,result=pcall(utils.chooseCarCup,{1})
    if confirmCase=='home' or confirmCase=='car-click' then
      assert(ok and result=='garageChanged' and #tapped==0)
    elseif confirmCase=='obscured' or confirmCase=='moving' or confirmCase=='stale' then
      assert(not ok and result:find('未能确认选车页面') and #tapped==0)
    else
      assert(ok and result==true and fullReads==3 and #tapped==1 and tapped[1]==1)
    end
  elseif filtered or scenario=='at-start' then
    function toast()end
    assert(utils.chooseCarCup({1}) and tapped[1]==1)
    local expected=not filtered and 30 or scenario=='filtered-click' and 6 or 0
    assert(entrySwipes==expected and resets==expected,'Apply entry swipes even if the first column is already visible')
  elseif scenario=='stuck' or scenario=='mismatch' then
    local ok,message=pcall(utils.chooseCarCup,utils.taskCars())
    assert(not ok and message:find(scenario=='stuck' and '最左起点' or '性能分不一致'),message)
    assert((scenario=='stuck' and #tapped==0) or (scenario=='mismatch' and tapped[1]==12 and not utils.verifiedDailyCar))
  elseif scenario=='star-only' then
    assert(utils.chooseCarCup({12})=='fuelEmpty' and #tapped==0 and returns==0)
  elseif scenario=='locked-only' then
    assert(utils.chooseCarCup({12})=='vehicleLocked' and #tapped==0 and returns==0)
  elseif scenario=='locked' or scenario=='locked-detail' or scenario=='star-card' or scenario=='star-detail' then
    local skippedInGrid=scenario=='locked' or scenario=='star-card'
    assert(utils.chooseCarCup(utils.taskCars()) and utils.verifiedDailyCar.score=='5878')
    assert(table.concat(tapped,',')==(skippedInGrid and '22' or '12,22'))
    assert(returns==(skippedInGrid and 0 or 1) and entrySwipes==30,'Trying another candidate must not repeat the entry sequence')
  else
    assert(utils.chooseCarCup(utils.taskCars()) and utils.verifiedDailyCar.score=='5888')
    if scenario=='large' then assert(resets>30,'A large garage must not stop after 30 drags') end
    offset=12
    assert(utils.chooseCarCup(utils.taskCars()) and utils.verifiedDailyCar.score=='5878')
    assert(table.concat(tapped,',')=='12,22' and entrySwipes==60,'Each entry resets before choosing the configured tile')
  end
  done=true
end
