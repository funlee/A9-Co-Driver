data={}
run={data={['特殊赛事阶段']='1'}}
local down,up,frame,bounce=0,0,0,0
local taps={}
local position=scenario=='fourth' and 4 or scenario=='first' and 1 or 3
local function card(y,color,count,top,complete)
  local artwork={}
  for i=1,count or 35 do artwork[i]=color end
  return {x=220,y=y,top=top or y-90,artwork=artwork,complete=complete~=false}
end
function print()end
function utils.openHomeTab(_,name) assert(name=='season') end
function _specialEventList()
  assert(down>=8,'Reset the rail before reading a clipped initial page')
  frame=frame+1
  if bounce>0 then
    bounce=bounce-1
    if bounce%2==0 then return {frameId=frame,cards={}} end
    return {frameId=frame,aligned=false,cards={card(284,0x16a3c0),card(490,0x635451)}}
  end
  if scenario=='noOrigin' then
    return {frameId=frame,aligned=false,cards={card(210,0x635451),card(416,0xb90010)}}
  end
  if scenario=='wideThird' then
    if up==0 then
      return {frameId=frame,aligned=true,scrollDistance=255,cards={card(212,0x16a3c0,35,92),card(449,0x635451,14,347,false)}}
    end
    return {frameId=frame,aligned=false,cards={card(310,0x635451,35,191),card(503,0xb90010,28,446,false)}}
  end
  if up>0 and scenario~='clamped' then
    if scenario=='skip' then
      return {frameId=frame,aligned=true,cards={card(174,0xa91680),card(380,0x398505)}}
    end
    return {frameId=frame,aligned=false,cards={card(200,0x635451),card(400,0xb90010),card(550,0x397531,14,516,false)}}
  end
  return {frameId=frame,aligned=true,scrollDistance=208,cards={card(174,0x16a3c0),card(380,0x635451),card(531,0xb90010,21,490,false)}}
end
function swipe(from,x,to,endX,duration,hold)
  assert(x==220 and endX==220 and #taps==0)
  if from>to then
    if down<8 then
      assert(from==550 and to==200 and duration==300 and hold==90)
    else
      assert(bounce==0)
      if scenario~='noOrigin' then assert(from==405,'Use a short confirmation pull at the top') end
      bounce=5
    end
    down=down+1
  else
    assert(down==9 and bounce==0 and duration==200 and hold==500)
    if up==0 then assert(to-from==(scenario=='wideThird' and 255 or 208)) end
    up=up+1
  end
end
function _soloEventUi(_,probe)
  frame=frame+1
  if #taps==0 then return {frameId=frame,specialSeasonTab=scenario~='wrongTab'} end
  assert(probe)
  local screen={scene='home',frameId=frame,specialSeasonTab=true,words={{text='开始',x=1100,y=530}}}
  if scenario=='directChapter' or scenario=='directLeftLabels' or scenario=='wrongPreview' then
    screen.scene='unknown'
    screen.specialSeasonTab=false
    screen.words={{text='章节1',x=scenario=='wrongPreview' and 1150 or 165,y=223}}
    if scenario=='directChapter' then screen.stageRail={x=287,y=257,clear=false} end
    if scenario=='directLeftLabels' then screen.words[2]={text='章节2',x=165,y=350} end
  elseif scenario=='directRanking' or scenario=='wrongRankStage' then
    screen.scene='specialRanking';screen.specialSeasonTab=false
    screen.words={{text=scenario=='wrongRankStage' and '阶段2' or '阶段1',x=170,y=112},{text='开始',x=1100,y=650}}
  elseif scenario=='pixelButton' then
    screen.words={};screen.specialEntryButton={x=1098,y=514}
  elseif scenario=='retry' and #taps==3 then
    screen.words={}
  elseif scenario=='stale' then
    screen.frameId=1
  end
  if screen.specialSeasonTab and scenario~='noTitles' then
    local top=position==1 and 84 or position==4 and 516 or scenario=='wideThird' and 446 or scenario=='clamped' and 490 or 310
    table.insert(screen.words,{text='N VISION 74',x=180,y=top+15})
    table.insert(screen.words,{text=scenario=='previousPreview' and 'MISTRAL' or 'N VISION 74',x=580,y=140})
  end
  return screen
end
function tap(y,x)
  assert(down==9 and bounce==0,'Wait for a stable origin before selecting')
  taps[#taps+1]=x..','..(719-y)
end
function test()
  local ok,message=pcall(utils.openSpecialEntry,{},position)
  if scenario=='noOrigin' then
    assert(not ok and message:find('列表起点') and #taps==0 and down==16,message)
  elseif scenario=='wrongTab' then
    assert(not ok and message:find('赛季赛事标签') and #taps==0 and down==0,message)
  elseif scenario=='wrongPreview' or scenario=='stale' or scenario=='wrongRankStage' then
    assert(not ok and message:find('进入按钮') and #taps==3,message)
  elseif scenario=='skip' then
    assert(not ok and message:find('未确认特殊赛事入口位置') and #taps==0,message)
  else
    assert(ok,scenario.." "..tostring(message))
    local y=position==1 and 174 or scenario=='wideThird' and 503 or
        scenario=='clamped' and 531 or position==4 and 550 or 400
    local expected=table.concat({'220,'..y,'220,'..y,'220,'..y},';')
    if scenario=='retry' then expected=expected..';220,'..y end
    if not scenario:find('direct') then
      local entry=scenario=='pixelButton' and ';1098,514' or ';1100,530'
      expected=expected..entry..entry
    end
    assert(table.concat(taps,';')==expected,table.concat(taps,';'))
    assert(up==(position==4 and 2 or position==1 and 0 or 1))
  end
  done=true
end
