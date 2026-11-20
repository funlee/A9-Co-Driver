local phase='event'
local frame=0
local offset=0
local chosen=0
local swipes=0
local dispatched=0
local oldOffset=0
local pageLeft=500
local repeatRace=scenario=='remembered' or scenario=='differentPage' or scenario=='reversePage'
local wideCar10=scenario=='wideCar10' or scenario=='wideCar10Overscroll'
local firstSwipes=nil
local fullPageReads=0
data={['项目']='多人上',['多人']=0}
run={data={['次数']=repeatRace and 2 or 1,['青铜用车']={27,28,5},['用车模式']='1'}}
if scenario=='reversePage' then run.data['青铜用车']={28,5} end
if wideCar10 then run.data['青铜用车']={10} end
local resets=0
utils={defultCfg=function(v,d)return v or d end}
function print()end
function _hud()end
function _sessionProject()return 0 end
function _sessionRaceBegin()end
function _sessionRaceResult()end
function _fallbackBegin()end
function _sessionOperation()end
function _fallbackStep()return 99 end
function _sessionRaceReturned()
  if scenario=='differentPage' then offset=12 else offset=offset+2 end
end
function swipe(_,from,__,to,duration,hold)
  if phase~='cars' then return end
  swipes=swipes+1;oldOffset=offset;dispatched=0
  if wideCar10 then
    if to>from then
      assert(scenario=='wideCar10Overscroll' and swipes==2 and chosen==0,'Unexpected corrective drag')
      offset=2
    elseif math.abs(to-from)>270 or duration<500 or hold<150 or (scenario=='wideCar10Overscroll' and swipes==1) then
      offset=offset+6 -- The gesture moved past every car in the verified screenshot.
    else offset=offset+2 end
  elseif scenario=='sameColumnShift' and swipes%2==1 then pageLeft=pageLeft-150
  elseif scenario~='stuckPage' then offset=math.max(0,offset+(to<from and 1 or -1)*(scenario=='overshoot' and 8 or 2));pageLeft=500 end
end
function tap(y,x)
  if phase=='event' then phase='cars'
  elseif phase=='car' then
    if repeatRace and not firstSwipes then
      assert(chosen==(scenario=='reversePage' and 28 or 27));firstSwipes=swipes;phase='race'
    else error('started') end
  elseif phase=='result' or phase=='race' then return
  elseif x==560 then offset=0;resets=resets+1
  else chosen=offset+(x-400)//10;phase='car' end
end
function cmpColor()return 1 end
local function thumbnail(n)
  local parts={}
  local state=n*197
  for i=1,80 do
    state=(state*48271)%2147483647
    parts[i]=state%2==0 and '0' or '1'
  end
  return table.concat(parts)
end
function _multiplayerUi(_,__,pixelsOnly)
  frame=frame+2000
  if phase=='race' then phase='result';return {scene='race',text='',frameId=frame} end
  if phase=='result' then phase='event';return {scene='result',text='你的排名',frameId=frame,finalRank=3} end
  if phase=='car' then return {scene='car',text='车辆详情',frameId=frame,carDetail={name=scenario=='fuzzyName' and 'PRAGAR1' or 'CAR'..chosen,performance=chosen..'/999'},fuel=3,words={{text='开始',x=1110,y=650}}} end
  if phase=='event' then return {scene='event',text='世界系列赛',frameId=frame,words={{text='开始',x=1110,y=650}}} end
  dispatched=dispatched+1
  if swipes>0 and not pixelsOnly then fullPageReads=fullPageReads+1 end
  local visible=(scenario=='delayedPage' and dispatched<3) and oldOffset or offset
  local s={scene='cars',text='车辆选择',frameId=frame,words={},leagueTabs={selected='青铜',['青铜']={x=560,y=106}}}
  s.gridOnly=pixelsOnly
  s.garage={start=visible==0,rank=visible==0 and '青铜' or '',key=visible==0 and 'origin' or '',pitch=359,left=pageLeft,cards={}}
  for i=1,6 do
    local n=visible+i
    local fuel=3
    if scenario=='empty27' and n==27 or scenario=='emptyBoth' and (n==27 or n==28) then fuel=0 end
    if scenario~='missingBottom' or i%2==1 then
      s.garage.cards[#s.garage.cards+1]={index=i,x=400+i*10,y=300,signature=thumbnail(n),availability=scenario=='locked27' and n==27 and 'locked' or fuel==0 and 'empty' or 'ready'}
    end
  end
  if scenario=='clippedOverlap' or scenario=='lockedOverlap' then
    if visible==18 then
      s.garage.cards[6]=nil;s.garage.cards[5]=nil
      s.garage.cards[3].clipped=scenario=='clippedOverlap';s.garage.cards[4].clipped=scenario=='clippedOverlap'
      local state=scenario=='clippedOverlap' and 'unknown' or 'locked'
      s.garage.cards[3].availability=state;s.garage.cards[4].availability=state
      s.garage.cards[3].signature='00000000010000000111000000010100100000100100000100001000000001000010100100010011'
      s.garage.cards[4].signature='00000000000000000010000000100000010001000000000100000000000000000010000000000000'
    elseif visible==20 then
      s.garage.cards[1].availability='locked';s.garage.cards[2].availability='locked'
      s.garage.cards[1].signature='00000000010000000111000000011100000000100000000100001000000000000010000100000001'
      s.garage.cards[2].signature='00000000000000000010000000100000011000100001100110000000000000000010000010000000'
    end
  end
  if scenario=='targetMoved' and fullPageReads>0 then s.garage.left=s.garage.left+50*(1+fullPageReads%2) end
  if scenario=='targetSettled' and fullPageReads>0 then s.garage.left=s.garage.left+50 end
  return s
end
function test()
  local ok,message=pcall(currentMultiplayer.run)
  assert(not ok)
  if scenario=='overshoot' or scenario=='stuckPage' or scenario=='targetMoved' then
    assert(chosen==0,message)
  else
    assert(message:find('started'),message)
    local expected=wideCar10 and 10 or scenario=='reversePage' and 5 or (repeatRace or scenario=='empty27' or scenario=='locked27') and 28 or scenario=='emptyBoth' and 5 or 27
    assert(chosen==expected,'selected '..chosen..' instead of '..expected)
    if scenario=='car27' then assert(fullPageReads==1,'Only the destination needs full OCR after scrolling') end
    if scenario=='reversePage' then assert(resets==0 and swipes>firstSwipes,'Move left from remembered position without a reset') end
    if scenario=='remembered' then assert(firstSwipes==swipes,'Adjacent car repeated the origin search') end
    if scenario=='differentPage' then assert(swipes>firstSwipes,'Changed page must recount from the origin') end
    if wideCar10 then
      assert(swipes==(scenario=='wideCar10Overscroll' and 3 or 2),'Verify each column before selecting car 10')
    else assert(swipes>=11,'Must observe every overlapping column') end
  end
  done=true
end
