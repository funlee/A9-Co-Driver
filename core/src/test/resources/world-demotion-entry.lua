local phase='series'
local frame,returns,notices=0,0,0
data={['项目']='多人上'}
run={data={['次数']=1}}
utils={defultCfg=function(v,d)return v or d end}
function utils.backToMain()
  assert(phase=='event' and returns==0)
  returns=returns+1
  phase='series'
end
function print()end
function _hud()end
function _sessionProject()return 0 end
function _taskNotice(label)
  assert(returns==1 and label==(current=='白金' and '铂金' or current))
  notices=notices+1
end
function _multiplayerUi()
  frame=frame+1
  if phase=='series' then
    return {scene=phase,frameId=frame,text='世界系列赛 大师系列赛',entryLeague=returns==0 and previous or current,
      words={{text='世界系列赛',x=354,y=455}}}
  elseif phase=='leagueDemotion' then
    return {scene=phase,frameId=frame,text=previous..' '..current,demotionConfirm={x=637,y=528}}
  end
  return {scene='event',frameId=frame,text='世界系列赛',words={{text='开始',x=1110,y=650}}}
end
function tap(y,x)
  if phase=='series' and x==354 then phase=returns==0 and 'leagueDemotion' or 'event'
  elseif phase=='leagueDemotion' and x==637 then phase='event'
  elseif phase=='event' and x==1110 and returns==1 and notices==1 then error('entered garage')
  else error('unexpected input') end
end
function test()
  local ok,message=pcall(currentMultiplayer.run)
  assert(not ok and message:find('entered garage'),message)
  done=true
end
