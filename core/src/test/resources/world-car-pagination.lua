local phase='event'
local frame=0
local offset=0
local chosen=0
local swipes=0
local dispatched=0
local oldOffset=0
local pageLeft=500
local unreadableAfterRace=scenario=='unreadableAfterRace'
local filtered=scenario=='filtered' or scenario=='filteredMaster'
local master=scenario=='filteredMaster'
local repeatRace=scenario=='remembered' or scenario=='differentPage' or scenario=='reversePage' or unreadableAfterRace or filtered
local wideCar10=scenario=='wideCar10' or scenario=='wideCar10Overscroll'
local restoredPage=scenario=='standardCar9ReturnToAnchor' or scenario=='standardCar9StuckAtAnchor'
local standardCar9=scenario=='standardCar9' or scenario=='standardCar9Overscroll' or restoredPage
local forcedOverscroll=scenario=='wideCar10Overscroll' or scenario=='standardCar9Overscroll'
local firstSwipes=nil
local fullPageReads=0
local filterCalls=0
data={['项目']=master and '多人下' or '多人上',['多人']=0}
run={data={['次数']=repeatRace and 2 or 1,['用车模式']='1'}}
if master then run.data['多人下用车']={27,28,5} else run.data['青铜用车']={27,28,5} end
if scenario=='reversePage' then run.data['青铜用车']={28,5} end
if wideCar10 then run.data['青铜用车']={10} end
if standardCar9 then run.data['青铜用车']={9,5,1,8,2} end
if filtered then run.data['选车筛选']='0,1,2' end
local resets=0
utils={defultCfg=function(v,d)return v or d end}
if filtered then
  utils.configureGarageFilter=function(selection)
    assert(selection=='0,1,2','Selection string must be passed through unchanged')
    assert(phase=='cars','Filter belongs on the garage list, not '..phase)
    if filterCalls==0 then assert(chosen==0,'Filter must precede the first car tap') end
    filterCalls=filterCalls+1
    -- The panel re-sorts the garage, so the previous scroll position is void.
    offset=0;pageLeft=500;oldOffset=0
  end
end
function print()end
local notices=0
function _taskNotice(message,duration)
  local expected=master and '无段位' or (unreadableAfterRace and firstSwipes and '未能确认当前段位' or '青铜')
  assert(phase=='event' and message==expected and duration>0)
  notices=notices+1
end
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
  if restoredPage and swipes>=3 then
    if to>from then
      offset=4;pageLeft=231 -- Observed rollback from left 244 to 231 on the same four cars.
    elseif swipes==3 or scenario=='standardCar9StuckAtAnchor' then
      offset=10 -- Across the overlap, so the offset cannot be inferred from this observation.
    else
      assert(math.abs(to-from)<250,'Retry the forward drag in smaller steps')
      offset=6;pageLeft=406
    end
  elseif wideCar10 or standardCar9 then
    if restoredPage and swipes==2 then pageLeft=244 end
    if to>from then
      assert(forcedOverscroll and swipes==2 and chosen==0,'Unexpected corrective drag')
      offset=2
    elseif math.abs(to-from)>(standardCar9 and 325 or 270) or duration<500 or (hold or 0)<150 or (forcedOverscroll and swipes==1) then
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
  if phase=='event' then
    local league='青铜'
    if unreadableAfterRace and firstSwipes then league=nil end
    return {scene='event',text=master and '大师系列赛' or '世界系列赛',frameId=frame,worldLeague=league,words={{text='开始',x=1110,y=650}}}
  end
  dispatched=dispatched+1
  if swipes>0 and not pixelsOnly then fullPageReads=fullPageReads+1 end
  local visible=(scenario=='delayedPage' and dispatched<3) and oldOffset or offset
  local s={scene='cars',text='车辆选择',frameId=frame,words={},leagueTabs={selected='青铜',['青铜']={x=560,y=106}}}
  s.gridOnly=pixelsOnly
  s.garage={start=visible==0,rank=visible==0 and (master and '大师系列赛' or '青铜') or '',key=visible==0 and 'origin' or '',pitch=standardCar9 and 464 or 359,left=pageLeft,cards={}}
  for i=1,(standardCar9 and 4 or 6) do
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
  assert(notices==(repeatRace and 2 or 1),'One notice per confirmed race')
  if unreadableAfterRace then
    assert(message:find('未确认世界系列赛当前段位') and phase=='event' and swipes==firstSwipes,message)
  elseif scenario=='standardCar9StuckAtAnchor' then
    assert(chosen==0 and swipes<=8 and message:find('无法核对重叠车辆'),'Bound local retries on a stuck garage')
  elseif scenario=='overshoot' or scenario=='stuckPage' or scenario=='targetMoved' then
    assert(chosen==0,message)
  else
    assert(message:find('started'),message)
    local expected=standardCar9 and 9 or wideCar10 and 10 or scenario=='reversePage' and 5 or (repeatRace or scenario=='empty27' or scenario=='locked27') and 28 or scenario=='emptyBoth' and 5 or 27
    assert(chosen==expected,'selected '..chosen..' instead of '..expected)
    if scenario=='car27' then assert(fullPageReads==1,'Only the destination needs full OCR after scrolling') end
    if scenario=='reversePage' then assert(resets==0 and swipes>firstSwipes,'Move left from remembered position without a reset') end
    if scenario=='remembered' then assert(firstSwipes==swipes,'Adjacent car repeated the origin search') end
    if filtered then
      assert(filterCalls==2,'Filter applied once before each race, got '..filterCalls)
      assert(resets==0,'Filtered garage restarts at the list origin without a league detour')
    end
    if scenario=='differentPage' then assert(swipes>firstSwipes,'Changed page must recount from the origin') end
    if restoredPage then
      assert(swipes==5 and resets==0,'Recover in the same garage without selecting another car')
    elseif standardCar9 then
      assert(swipes==(forcedOverscroll and 4 or 3),'Maintain a verified overlap from the bronze rail to car 9')
    elseif wideCar10 then
      assert(swipes==(scenario=='wideCar10Overscroll' and 3 or 2),'Verify each column before selecting car 10')
    else assert(swipes>=11,'Must observe every overlapping column') end
  end
  done=true
end
