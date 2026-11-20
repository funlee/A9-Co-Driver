data={}
local down,up,frame=0,0,0
local taps={}
local bounce=0
local position=scenario=='fourth' and 4 or scenario=='third' and 3 or 1
local function card(y,color)
  return {x=220,y=y,top=y-90,artwork=color and {color,color,color,color} or {}}
end
function print()end
function utils.openHomeTab(_,name) assert(name=='season') end
function _specialEventList()
  frame=frame+1
  if bounce>0 then
    bounce=bounce-1
    if bounce%2==0 then return {frameId=frame,cards={}} end
    return {frameId=frame,aligned=false,cards={card(284,0x16a3c0),card(490,0x635451)}}
  end
  if down==0 or scenario=='noOrigin' then
    return {frameId=frame,aligned=false,cards={card(210,0x635451),card(416,0xb90010)}}
  end
  if up>0 then
    return {frameId=frame,aligned=false,cards={card(200,0x635451),card(402,0xb90010),card(550)}}
  end
  return {frameId=frame,aligned=true,cards={card(174,0x16a3c0),card(380,0x635451),card(531)}}
end
function swipe(from,x,to,endX)
  assert(x==220 and endX==220 and #taps==0 and bounce==0)
  if from>to then
    if scenario~='noOrigin' and down==1 then assert(from==405,'Use a short pull at the top') end
    down=down+1;bounce=5
  else assert(down==2);up=up+1 end
end
function _soloEventUi()
  assert(#taps==1)
  return {specialSeasonTab=scenario~='wrongPreview',words={{text='开始',x=1100,y=530}}}
end
function tap(y,x)
  assert(down==2,'Wait for a stable origin after revealing the hidden first card')
  taps[#taps+1]=x..','..(719-y)
end
function test()
  local ok,message=pcall(utils.openSpecialEntry,{},position)
  if scenario=='noOrigin' then
    assert(not ok and message:find('列表起点') and #taps==0 and down==8,message)
  elseif scenario=='wrongPreview' then
    assert(not ok and message:find('进入按钮') and #taps==1,message)
  else
    assert(ok,message)
    local y=position==1 and 174 or position==3 and 531 or 550
    assert(table.concat(taps,';')=='220,'..y..';1100,530')
    assert(up==(position==4 and 1 or 0))
  end
  done=true
end
