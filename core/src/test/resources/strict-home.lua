data={['超时状态']=true}
local reads,backs,lastBack,hard=0,0,-4000,false
local taps={}
function toast()end
function print()end
function _soloEventUi()error('Strict recovery requires the unwrapped snapshot')end
function _legacyPopupDismiss(rule)assert(rule=='fixture');taps[#taps+1]='legacy';return true end
function requirePlayerAction(message)hard=true;error(message)end
function keyPress(key)
  assert(key=='back' and tickCount()-lastBack>=4000,'Pace repeated Back presses')
  backs=backs+1;lastBack=tickCount()
end
function tap(y,x)taps[#taps+1]=y..','..x end
local function home(mask,id)
  return {scene='home',frameId=id or reads,homeEvidence=mask}
end
function _recoveryUi()
  reads=reads+1
  if scenario=='sameRule' then
    if reads<3 then return home(1,1) end
    return home(2)
  elseif scenario=='stale' then return home(3,1)
  elseif scenario=='unconfirmedHome' then
    return {scene=reads%2==1 and 'home' or 'unknown',text='世界系列赛 大师系列赛 对决',frameId=reads}
  elseif scenario=='unknownTimeout' then return {scene='unknown',frameId=reads}
  elseif scenario=='masked' then
    if reads==2 then return {scene='interruptingPopup',frameId=reads,homeEvidence=3} end
  elseif scenario=='nestedBack' then
    if backs<3 then return {scene='interruptingPopup',frameId=reads} end
  elseif scenario=='exit' then
    if reads==1 then return {scene='exitGame',frameId=reads,homeEvidence=3,
      words={{text='再玩一会',x=570,y=520},{text='退出游戏',x=790,y=520}}} end
    if reads<=4 then return {scene='unknown',frameId=reads} end
  elseif scenario=='popup' then
    if #taps==0 then return {scene='interruptingPopup',frameId=reads,homeEvidence=2,
      dismissPopup={x=880,y=90,label='×'}} end
    if #taps==1 then return {scene='unknown',frameId=reads,legacyPopup='fixture'} end
  elseif scenario=='originalActions' then
    local action=({'continue','ad','gray','back'})[#taps+backs+1]
    if action then return {scene='unknown',frameId=reads,returnOverlay=action} end
  elseif scenario=='rewardChoice' or scenario=='loginHistory' then return {scene=scenario,frameId=reads}
  end
  return home(3)
end
function test()
  local ok,result=pcall(utils.backToMain,true)
  if scenario=='rewardChoice' or scenario=='loginHistory' then
    assert(not ok and hard and #taps==0 and backs==0)
  elseif scenario=='unknownTimeout' or scenario=='unconfirmedHome' then
    assert(not ok and not hard and #taps==1 and backs>=2 and backs<=22 and tickCount()>=90000)
  elseif scenario=='stale' then assert(not ok and #taps==0 and backs==0)
  else
    assert(ok and result==true,tostring(result))
    if scenario=='sameRule' then assert(reads==4 and #taps==0 and backs==0)
    elseif scenario=='masked' then assert(reads==4 and backs==1)
    elseif scenario=='nestedBack' then assert(backs==3 and #taps==0)
    elseif scenario=='exit' then assert(table.concat(taps,'/')=='199,570' and backs==0 and reads==6)
    elseif scenario=='popup' then assert(table.concat(taps,'/')=='629,880/legacy' and backs==0)
    elseif scenario=='originalActions' then assert(table.concat(taps,'/')=='33,1080/600,1080/317,774' and backs==1) end
  end
  done=true
end
