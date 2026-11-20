data={};run={}
function localInit() end
function monitor() sleep(1000000) end
local strictHome=utils.backToMain
function utils.backToMain(strict)
  if strict then return strictHome(true) end
end
function utils.checkAuto() end
function utils.initCar() end
local reads,taps=0,0
function _recoveryUi()
  reads=reads+1
  local screen=reads<=2 and modal or home
  screen.frameId=reads
  return screen
end
function tap(y,x)
  local later=modal.dismissPopup
  assert(reads==2 and taps==0 and x==later.x and y==719-later.y,'Unexpected recovery tap')
  taps=taps+1
end
local confirmed=_queueHomeConfirmed
function _queueHomeConfirmed()
  assert(reads==4 and taps==1,'A covered or unconfirmed home frame must not authorize work')
  return confirmed()
end
function run.project()
  remainingQuota(tonumber(run.data['次数']))
  finishRace()
  error('fixture: business failed')
end
currentMultiplayer={run=function()
  local total=tonumber(run.data['次数'])
  remainingQuota(total-_sessionProject(data['项目'],total))
  finishRace()
end}
