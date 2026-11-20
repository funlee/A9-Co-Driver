data={['项目']='每日战利品',['用车次数']={}}
run={data={['用车模式']='1',['选车方式']='0',['杯赛用车']={12,22}}}
local frame,offset,resets,chosen=0,scenario=='large' and 150 or 8,0,0
local tapped={}
local blankReads=2
local bounceReads=0
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
  if blankReads>0 then
    blankReads=blankReads-1
    return {scene='cars',frameId=frame,gridOnly=fast,garage={start=false,left=-1,pitch=474,cards={}}}
  end
  local cards={}
  for i=1,6 do
    local number=offset+i
    cards[i]={index=i,x=280+math.floor((i-1)/2)*474,y=i%2==1 and 308 or 550,
      signature=signature(number),score=fast and '' or tostring(5900-number),model=scenario=='model' and 'CCXR' or ''}
  end
  local atStart=offset==0 and bounceReads==0
  local left=atStart and 97 or offset==0 and 376 or 200
  bounceReads=math.max(0,bounceReads-1)
  return {scene='cars',frameId=frame,gridOnly=fast,garage={start=atStart,leadingClear=offset==0,left=left,pitch=474,cards=cards}}
end
function swipe(_,from,__,to)
  assert(blankReads==0,'Wait for the car cards to reappear before dragging again')
  assert(bounceReads==0,'Wait for the visible first column to finish bouncing')
  if to>from then
    resets=resets+1
    if scenario=='large' then offset=math.max(0,offset-4)
    elseif scenario~='stuck' then offset=0 end
    blankReads=4
    if offset==0 then bounceReads=4 end
  else
    assert(resets>=1,'Do not count cars before a verified origin')
    offset=offset+2
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
utils.carFuel=function()return 2,_soloEventUi()end
function test()
  if scenario=='stuck' or scenario=='mismatch' then
    local ok,message=pcall(utils.chooseCarCup,utils.taskCars())
    assert(not ok and message:find(scenario=='stuck' and '最左起点' or '性能分不一致'),message)
    assert((scenario=='stuck' and #tapped==0) or (scenario=='mismatch' and tapped[1]==12 and not utils.verifiedDailyCar))
  else
    assert(utils.chooseCarCup(utils.taskCars()) and utils.verifiedDailyCar.score=='5888')
    if scenario=='large' then assert(resets>30,'A large garage must not stop after 30 drags') end
    offset=12
    assert(utils.chooseCarCup(utils.taskCars()) and utils.verifiedDailyCar.score=='5878')
    assert(table.concat(tapped,',')=='12,22','Actual tiles match the configured order')
  end
  done=true
end
