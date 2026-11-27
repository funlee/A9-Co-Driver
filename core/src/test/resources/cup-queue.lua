local daily=scenario~='ordinary'
local claim=scenario~='daily-no-claim'
local expected=scenario=='quiet' and 0 or scenario=='midnight' and 1 or 5
local phase='cupList'
local selected,started,settled,completed,claims,notices=0,0,0,0,0,0
data={['杯赛']=0}
UIconfig={routeOnly='',ActiveProj={true,true},projects={
  {['项目']='杯赛',data={['次数']='5',['杯赛位置']='5',['是否领取俱乐部奖励']=claim and '0' or ''}},
  {['项目']='多人下',data={['次数']='1'}}}}
function localInit()end
function monitor()end
function _hud()end
function print()end
function _sessionCycle(cycle)assert(cycle==1)end
function _queueEnter(cycle,index)
  assert(cycle==1 and index==completed+1)
  return 0
end
function _sessionProject()end
function _sessionLegacyCompleted()assert(settled==started);return true end
function _queueDone()completed=completed+1 end
function _taskNotice(message)
  assert(message:find('00:00–00:30',1,true))
  notices=notices+1
end
function _dailyTaskStatus()
  assert(daily,'Ordinary cups must not use the daily schedule')
  return (scenario=='quiet' or scenario=='midnight' and started>0) and 'dailyQuietHours' or ''
end
function _soloEventUi()
  return {scene=phase,dailyLoot=phase=='event' and daily}
end
utils={
  backToMain=function()end,checkAuto=function()end,initCar=function()end,tipsHandle=function()end,
  chooseCup=function(position,wide,configuredOnly)
    assert(position=='5' and not wide and configuredOnly)
    selected=selected+1
    phase='event'
  end,
  dailyPrize=function()assert(daily and claim and phase=='event');claims=claims+1 end
}
run.list={['杯赛']={{'flow',function()assert(phase=='race');phase='results' end}}}
run.carDetails={['杯赛']=function()
  assert(run.dailyLoot==daily)
  started=started+1
  phase='race'
  utils.soloStartAt=tickCount()
end}
cup.openRaceGarage=function(screen)
  assert(screen.dailyLoot==daily and run.dailyLoot==daily)
  phase='car'
  return true
end
dailyCup.settle=function(label)
  assert(label=='杯赛' and phase=='results')
  settled=settled+1
  phase='event'
  return 0
end
function keepCapture()end
function cmpColorEx()return 1 end
function run.rebootHandle()end
currentMultiplayer={run=function()
  assert(selected==1 and started==expected and settled==expected and data['杯赛']==expected)
  assert(claims==(daily and claim and expected or 0))
  assert(notices==(expected<5 and 1 or 0))
  assert(UIconfig.projects[1].data['次数']=='5' and completed==1)
  verified=true
  sleep(999999)
end}
function test()main()end
