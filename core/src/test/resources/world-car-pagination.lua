local phase='event'
local frame=0
local offset=0
local chosen=0
local swipes=0
local dispatched=0
local oldOffset=0
local pageLeft=500
data={['项目']='多人上'}
run={data={['次数']=1,['青铜用车']={27,28,5},['用车模式']='1'}}
utils={defultCfg=function(v,d)return v or d end}
function print()end
function _hud()end
function _sessionProject()return 0 end
function swipe()
  swipes=swipes+1;oldOffset=offset;dispatched=0
  if scenario=='sameColumnShift' and swipes%2==1 then pageLeft=pageLeft-150
  elseif scenario~='stuckPage' then offset=offset+(scenario=='overshoot' and 8 or 2);pageLeft=500 end
end
function tap(y,x)
  if phase=='event' then phase='cars'
  elseif phase=='car' then error('started')
  elseif x==560 then offset=0
  else chosen=offset+(x-400)//10;phase='car' end
end
function cmpColor()return 1 end
function _multiplayerUi()
  frame=frame+2000
  if phase=='car' then return {scene='car',text='车辆详情',frameId=frame,carDetail={name=scenario=='fuzzyName' and 'PRAGAR1' or 'CAR'..chosen,performance=chosen..'/999'},fuel=3,words={{text='开始',x=1110,y=650}}} end
  if phase=='event' then return {scene='event',text='世界系列赛',frameId=frame,words={{text='开始',x=1110,y=650}}} end
  dispatched=dispatched+1
  local visible=(scenario=='delayedPage' and dispatched<3) and oldOffset or offset
  local s={scene='cars',text='车辆选择',frameId=frame,words={},leagueTabs={selected='青铜',['青铜']={x=560,y=106}}}
  s.garage={start=visible==0,rank=visible==0 and '青铜' or '',key=visible==0 and 'origin' or '',pitch=359,left=pageLeft,cards={}}
  for i=1,6 do
    local n=visible+i
    local fuel=3
    if scenario=='empty27' and n==27 or scenario=='emptyBoth' and (n==27 or n==28) then fuel=0 end
    if scenario~='missingBottom' or i%2==1 then
      s.garage.cards[#s.garage.cards+1]={index=i,x=400+i*10,y=300,performance=n..'/999',tokens={scenario=='fuzzyName' and n==27 and 'PRCGAR1' or 'CAR'..n},fuel=fuel,capacity=3}
    end
  end
  return s
end
function test()
  local ok,message=pcall(currentMultiplayer.run)
  assert(not ok)
  if scenario=='overshoot' or scenario=='stuckPage' then
    assert(chosen==0,message)
  else
    assert(message:find('started'),message)
    local expected=scenario=='empty27' and 28 or scenario=='emptyBoth' and 5 or 27
    assert(chosen==expected,'selected '..chosen..' instead of '..expected)
    assert(swipes>=11,'Must observe every overlapping column')
  end
  done=true
end
