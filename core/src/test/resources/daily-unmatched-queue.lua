data={};local began,notice,nextTask,reads;reads=0
UIconfig={routeOnly='',ActiveProj={true,true},projects={
  {['项目']='每日战利品',data={['次数']='5',['杯赛位置']='2'}},
  {['项目']='多人下',data={['次数']='1'}}}}
local function unexpected()error('unverified event must never start a race or count a finish')end
function localInit()end;function monitor()end;function _hud()end;function print()end
function _sessionCycle(cycle)if cycle==2 then assert(nextTask);verified=true;sleep(999999)end end
function _queueEnter(cycle,index)
  if index==1 then began=tickCount() else assert(notice and tickCount()-notice>=4000)end
  return 0
end
function _sessionProject()end
function _sessionLegacyCompleted()unexpected()end
function _taskNotice(message,duration)
  assert(notice==nil and message:find('位置 2',1,true) and duration==4000)
  assert(tickCount()-began>=dailyCup.matchTimeout and reads>1)
  notice=tickCount()
end
function _soloEventUi()
  reads=reads+1;return {scene=unmatchedScene,text='金币赛 单人奖励 最佳用时 比赛'}
end
utils={backToMain=function()end,checkAuto=function()end,initCar=function()end,tipsHandle=unexpected}
run.list={['每日战利品']={{'home',unexpected},{'carousel',unexpected},{'race',unexpected}}}
run.carDetails={['每日战利品']=unexpected};dailyCup.openRaceGarage=unexpected
function keepCapture()end;function cmpColorEx(pattern)return pattern=='race' and 1 or 0 end
function run.rebootHandle()end
currentMultiplayer={run=function()nextTask=true end}
function test()main()end
