data={['信息']='正在选车',['心跳时间']=0,['等待时间']=420}
local reads,actions=0,0
function print() end
function _hud() end
function _legacyPopupProbe(backgroundOnly)
  reads=reads+1
  if mode=='recovering' or mode=='modal' then error('The monitor must not inspect while returning to the main menu') end
  if mode=='sequence' then
    assert(not backgroundOnly)
    if reads>3 then return nil end
    return {rule='TIP_SEQUENCE',frameId=reads}
  end
  if mode=='background' or mode=='stale' then
    assert(backgroundOnly)
    return {rule='AD_BACK',frameId=mode=='stale' and 3 or reads}
  end
  if mode=='foreground' then
    assert(not backgroundOnly)
    return {rule='TIP_ONE',frameId=reads}
  end
end
function _legacyPopupDismiss(rule,continuation)
  actions=actions+1
  if mode=='sequence' then
    assert(rule=='TIP_SEQUENCE' and (not continuation and actions==1 or continuation and actions==2))
  elseif mode=='background' then assert(rule=='AD_BACK' and actions==1)
  elseif mode=='foreground' then assert(rule=='TIP_ONE' and actions==1)
  else error('No confirmed popup is available') end
  return true
end
local modernReads,modernTaps=0,0
function _soloEventUi()
  modernReads=modernReads+1
  if modernTaps>0 then return {scene='home',words={}} end
  return {scene='interruptingPopup',frameId=modernReads,dismissPopup={label='×',x=1044,y=118}}
end
function tap(y,x)
  assert(mode=='modal' and x==1044 and y==601)
  modernTaps=modernTaps+1
end
function test()
  if mode=='modal' then
    beginThread(monitor)
    assert(_soloEventUi().scene=='home' and modernTaps==1 and reads==0)
    queueRecovering=true
  elseif mode=='sequence' or mode=='foreground' then
    assert(_handleLegacyTips())
    assert(actions==(mode=='sequence' and 2 or 1))
  else
    queueRecovering=mode=='recovering'
    beginThread(monitor)
    sleep(1900)
    assert(actions==(mode=='background' and 1 or 0))
    if mode=='recovering' then assert(reads==0) end
    queueRecovering=true
  end
  done=true
end
