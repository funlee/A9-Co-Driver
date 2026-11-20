-- Only the game UI and races are simulated. main() and its queue bookkeeping come from bootstrap.lua.
data={}
run={}
function localInit() end
function monitor() sleep(1000000) end
utils={
  backToMain=function(strict)
    if strict then return reachHome() end
  end,
  initCar=function() end,
  checkAuto=function() end,
  specialMissionContinue=function() return false end
}
function _soloEventUi() return {scene='home'} end
currentRewards={
  enabled=function() return true end,
  collect=function()
    if shouldFail('reward') then error('fixture: reward blocked') end
  end
}
local function race()
  if shouldFail('race') then error('fixture: cannot read car') end
  settle()
  sleep(1)
end
function run.project()
  observeQuota(tonumber(run.data['次数']))
  for i=1,tonumber(run.data['次数']) do race() end
end
currentMultiplayer={
  run=function()
    local total=tonumber(run.data['次数'])
    local alreadyDone=_sessionProject(data['项目'],total)
    observeQuota(total-alreadyDone)
    for i=alreadyDone+1,total do race() end
  end
}
