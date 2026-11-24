data={['杯赛']=0};local selected,started,settled,completed,notices=0,0,0,0,0
local phase=unmatchedScene
UIconfig={routeOnly='',ActiveProj={true,true},projects={
  {['项目']='每日战利品',data={['次数']='5',['杯赛位置']='5'}},
  {['项目']='多人下',data={['次数']='1'}}}}
function localInit()end;function monitor()end;function _hud()end;function print()end
function _sessionCycle(cycle)assert(cycle==1)end
function _queueEnter(cycle,index)
  assert(cycle==1 and index==completed+1)
  return 0
end
function _sessionProject()end
function _sessionLegacyCompleted()assert(settled==started);return true end
function _queueDone()completed=completed+1 end
function _taskNotice()notices=notices+1 end
function _requirePlayerAction()error('A different event title must not stop the queue')end
function _soloEventUi()
  return {scene=phase,text='巅峰对决 单人奖励 最佳用时 比赛'}
end
utils={backToMain=function()end,checkAuto=function()end,initCar=function()end,tipsHandle=function()end}
run.list={['每日战利品']={{'flow',function()
  if phase=='race' then phase='results' else phase='car' end
end}}}
run.carDetails={['每日战利品']=function()
  started=started+1;phase='race';utils.soloStartAt=tickCount()
end}
dailyCup.openRaceGarage=function(screen)
  assert(screen.text:find('巅峰对决',1,true));selected=selected+1;phase='car';return true
end
dailyCup.operation[4][2]=function()phase='car' end
dailyCup.settle=function(label)
  assert(label=='每日战利品' and phase=='results');settled=settled+1;phase='event';return 0
end
function keepCapture()end;function cmpColorEx()return 1 end
function run.rebootHandle()end
currentMultiplayer={run=function()
  assert(started==5 and settled==5 and data['杯赛']==5 and notices==0)
  assert(UIconfig.projects[1].data['次数']=='5' and completed==1)
  assert(selected==(unmatchedScene=='event' and 5 or 4))
  verified=true;sleep(999999)
end}
function test()main()end
