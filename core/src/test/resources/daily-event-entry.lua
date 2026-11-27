-- The first visible card is the recharge event. Selecting Daily Loot shifts the
-- carousel left by 102 px. Its old center is now closer to the third card.
data={['项目']='杯赛'}
run.index=1
run.data={['次数']='10',['杯赛位置']=scenario=='missing' and '8' or '2'}
local frame,shift,picked,entries,afterEntry=0,0,1,0,0
local inGarage=false
local function cards()
  local visible={}
  for i=1,3 do visible[i]={center=151+(i-1)*186-shift,selected=i==picked} end
  return visible
end
function keepCapture()end
function releaseCapture()end
function _cupCards()
  local visible=cards()
  if scenario=='covered-border' and picked==2 then visible[2].selected=false end
  return visible
end
function _cupScrollDelta(direction)return direction and 0 or shift end
function swipe()assert(picked==1,'dragged after choosing the requested card')end
function cmpColorEx()return 0 end
function _soloEventUi()
  frame=frame+1
  if inGarage then return {scene='cars',frameId=frame} end
  if entries>0 then
    afterEntry=afterEntry+1
    if scenario=='unknown' then return {scene='unknown',frameId=frame} end
    if scenario=='stale' then return {scene='cupList',frameId=10000,words={{text='进入',x=235,y=655}}} end
    if scenario=='wrong-card' then picked=3 end
    if scenario=='delayed' and afterEntry==2 then return {scene='unknown',frameId=frame} end
  end
  local direct=scenario=='direct' and picked==2
  if direct or (scenario=='covered-border' and entries==1) or (scenario=='delayed' and afterEntry>=4) or (scenario=='retry' and entries==2) then
    return {scene='event',dailyLoot=true,frameId=frame,text='每日车辆战利品 单人奖励 俱乐部奖励 比赛',
        words={{text='比赛',x=1150,y=660}}}
  end
  return {scene='cupList',frameId=frame,words={{text='进入',x=cards()[picked].center,y=655}}}
end
function tap(x,y)
  if x==189 then
    assert(y==337 and picked==1 and entries==0,'selected the wrong event or selected it twice')
    picked=2
    shift=102
  elseif x==64 then
    assert(y==235 and picked==2 and not inGarage,'entry tap used a stale center or the wrong selected card')
    entries=entries+1
    assert(entries<=2,'retried more than once')
    if entries==2 then assert(scenario=='retry' and afterEntry>=2,'unverified retry') end
  elseif x==59 and y==1150 then
    assert(not inGarage and run.dailyLoot,'Race pressed outside the Daily Loot detail')
    inGarage=true
  else error('unexpected tap '..x..','..y) end
end
function test()
  utils.tipsHandle=function()end
  run.nitro=function()end
  run.rebootHandle=function()end
  run.carDetails={['杯赛']=function()error('unexpected car detail')end}
  dailyCup={skipReason=function()return '' end}
  cup.operation[4][2]=function()assert(inGarage);return 'garageReached' end
  local result=run.project()
  if scenario=='missing' then
    assert(result=='cupPositionUnavailable' and entries==0 and picked==1,result)
  elseif scenario=='unknown' or scenario=='stale' or scenario=='wrong-card' then
    assert(result=='cupEntryUnconfirmed' and entries==1 and not inGarage,result)
  else
    local expected=scenario=='direct' and 0 or scenario=='retry' and 2 or 1
    assert(result=='garageReached' and inGarage and entries==expected,result)
  end
end
