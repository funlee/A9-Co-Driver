data={['信息']='当前赛事'}
local reads=0
local taps={}
function print() end
local function read()
  reads=reads+1
  if #taps==2 then return {scene='home',words={{text='每日赛事'}}} end
  if mode=='unknown' then return {scene='interruptingPopup',frameId=reads} end
  local action=#taps==0 and {label='×',x=981,y=114} or {label='稍后',x=451,y=502}
  if reads==1 and mode~='repeatedFrame' then action.x=1010 end
  return {scene='interruptingPopup',frameId=mode=='repeatedFrame' and 1 or reads,dismissPopup=action}
end
_soloEventUi=read
_multiplayerUi=read
_rewardUi=read
function tap(y,x)
  taps[#taps+1]=x..','..(719-y)
  assert(reads>=3,'A changed position requires another fresh frame')
end
function test()
  local entry=mode=='multiplayer' and _multiplayerUi or mode=='rewards' and _rewardUi or _soloEventUi
  local ok,result=pcall(entry)
  if mode=='unknown' or mode=='repeatedFrame' then
    assert(not ok and result:find('未确认可点的关闭控件') and #taps==0,result)
  else
    assert(ok and result.scene=='home' and data['信息']=='当前赛事',result)
    assert(table.concat(taps,';')=='981,114;451,502')
  end
  done=true
end
