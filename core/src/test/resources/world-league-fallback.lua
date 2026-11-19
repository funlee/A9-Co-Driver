local bronze,silver,gold='青铜','白银','黄金'
local phase='event'
local rank=(scenario=='skipBlank' or scenario=='cascade') and gold or silver
local tab=rank
local railDelay=0
local frame=0
local chosen={}
local switches={}
local fuelRead=scenario=='unknown' and -1 or 0

data={['项目']='多人上'}
run={data={['次数']=1,['青铜用车']={2},['白银用车']={1},['黄金用车']={1}}}
if scenario=='skipBlank' then run.data['白银用车']={'0'} end
if scenario=='detailEmpty' then run.data['青铜用车']={1} end
utils={defultCfg=function(v,d)return v or d end}
function print()end
function _hud()end
function _sessionProject()return 0 end
function _multiplayerUi()
  frame=frame+2000
  local s={scene=phase,text='世界系列赛',frameId=frame,words={}}
  if phase=='event' then
    s.words={{text='开始',x=1110,y=650}}
  elseif phase=='car' then
    s.fuel=0;s.carDetail={performance='1/2',name='LANCER'}
  else
    if railDelay>0 then
      railDelay=railDelay-1
      if railDelay==0 then rank=tab end
    end
    s.leagueTabs={selected=tab,[bronze]={x=560,y=106},[silver]={x=616,y=106}}
    if scenario=='missingTab' then s.leagueTabs[bronze]=nil end
    s.garage={rank=rank,start=true,key=rank..':1/2',cards={}}
    for i=1,2 do
      local fuel=fuelRead
      if rank==bronze and scenario~='allEmpty' or rank==silver and
          (scenario=='detailEmpty' or scenario=='silverFueled') then fuel=3 end
      s.garage.cards[i]={index=i,x=410,y=300+100*i,performance='1/2',tokens={'LANCER'},fuel=fuel,capacity=3}
    end
  end
  return s
end
function tap(y,x)
  y=719-y
  if phase=='event' and x==1110 then phase='cars'
  elseif phase=='car' and x==38 then phase='cars'
  elseif phase=='cars' and y==106 then
    tab=x==560 and bronze or silver
    switches[#switches+1]=tab
    if scenario~='stuckTab' then railDelay=2 end
  elseif phase=='cars' and x==410 then
    chosen[#chosen+1]=rank..((y-300)//100)
    if scenario=='detailEmpty' and rank==silver then phase='car' else error('chosen') end
  else error('unexpected tap') end
end
function test()
  local ok,result=pcall(currentMultiplayer.run)
  local selections=table.concat(chosen,',')
  local route=table.concat(switches,',')
  if scenario=='missingTab' or scenario=='stuckTab' then
    assert(not ok and result:find('起点') and selections=='',result)
  elseif scenario=='unknown' then
    assert(not ok and result:find('核对车辆序号') and selections=='' and route=='',result)
  elseif scenario=='allEmpty' then
    assert(ok and result=='fuelEmpty' and selections=='' and route==bronze)
  else
    assert(not ok and result:find('chosen'),tostring(result))
    local expected=scenario=='detailEmpty' and '白银1,青铜1' or scenario=='silverFueled' and '白银1' or '青铜2'
    assert(selections==expected,selections)
    assert(route==(scenario=='silverFueled' and '' or scenario=='cascade' and '白银,青铜' or bronze),route)
  end
  done=true
end
