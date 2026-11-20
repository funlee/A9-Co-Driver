data={['项目']='每日战利品',['用车次数']={}}
run={data={['用车模式']='1',['选车方式']='0',['杯赛用车']={12,22}}}
local frame,offset,resets,chosen=0,8,0,0
local tapped={}
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
  local cards={}
  for i=1,6 do
    local number=offset+i
    cards[i]={index=i,x=280+math.floor((i-1)/2)*474,y=i%2==1 and 308 or 550,
      signature=signature(number),score=fast and '' or tostring(5900-number)}
  end
  return {scene='cars',frameId=frame,gridOnly=fast,garage={start=offset==0,left=offset==0 and 97 or 200,pitch=474,cards=cards}}
end
function swipe(_,from,__,to)
  if to>from then
    resets=resets+1
    if scenario~='stuck' then offset=0 end
  else
    assert(resets>=2,'Do not count cars before a verified origin')
    offset=offset+2
  end
end
function tap(y,x)
  chosen=offset+2*math.floor((x-280)/474)+(y==169 and 2 or 1)
  tapped[#tapped+1]=chosen
end
function _soloEventUi()
  return {scene='car',soloScore=tostring(5900-chosen+(scenario=='mismatch' and 1 or 0)),soloName='Test car'}
end
utils.carFuel=function()return 2,_soloEventUi()end
function test()
  if scenario=='stuck' or scenario=='mismatch' then
    local ok,message=pcall(utils.chooseCarCup,utils.taskCars())
    assert(not ok and message:find(scenario=='stuck' and '最左起点' or '性能分不一致'),message)
    assert((scenario=='stuck' and #tapped==0) or (scenario=='mismatch' and tapped[1]==12 and not utils.verifiedDailyCar))
  else
    assert(utils.chooseCarCup(utils.taskCars()) and utils.verifiedDailyCar=='5888')
    offset=12
    assert(utils.chooseCarCup(utils.taskCars()) and utils.verifiedDailyCar=='5878')
    assert(table.concat(tapped,',')=='12,22','Actual tiles match the configured order')
  end
  done=true
end
