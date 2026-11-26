data={}
-- chapter-two: 第 2 章的第一张卡就是「阶段6」（阶段编号跨章节连续），
-- 配置阶段6 时卡片已在可视区，必须直接选中而不是等「阶段1」出现。
local chapterTwo=scenario=='chapter-two'
local target=chapterTwo and 6 or scenario=='third' and 3 or 1
run={data={['特殊赛事位置']='2',['称霸模式']='0',['称霸位置']=chapterTwo and '2' or '1',['特殊赛事阶段']=tostring(target)}}
local frame,drags,forward,transient=0,0,0,0
local chapterSelected,stageSelected=false,false

utils.openSpecialEntry=function(_,position)assert(position==2)end
local returned=false
function keyPress(key)
  assert((scenario=='saved-fifth' or scenario=='reselect-fifth') and key=='back' and not returned)
  returned=true
end
function keepCapture()end
function cmpColor()return 0 end
function print()end
utils.mySwipe=function() error('Do not pull left while resetting') end

function swipe(_,from,__,destination)
  assert(chapterSelected and not stageSelected)
  assert(not chapterTwo,'target stage already visible must not drag')
  if destination>from then
    assert(forward==0)
    drags=drags+1
  else
    assert(target==3 and drags==7)
    forward=forward+1
  end
  transient=2
end

function tap(_,x)
  if not chapterSelected then
    assert(x==165)
    chapterSelected=true
    return
  end
  assert(scenario~='stuck' and (chapterTwo and drags==0 or drags==7),'Premature selection before the list reached the end')
  assert(x==(chapterTwo and 370 or target==3 and 750 or 429))
  stageSelected=true
end

function _soloEventUi(_,stageProbe)
  frame=frame+1
  if stageSelected then
    local actual=scenario=='wrong-stage' and 2 or target
    return {scene='specialRanking',words={{text='阶段'..actual,x=195,y=112},{text='开始',x=1100,y=650}}}
  end
  if (scenario=='saved-fifth' or scenario=='reselect-fifth') and not returned then
    return {scene='missionRewards',stageLabel='阶段5',words={{text='继续',x=1100,y=650}}}
  end
  if not chapterSelected then
    return {scene='unknown',words={{text=chapterTwo and '章节2' or '章节1',x=165,y=250}}}
  end
  assert(stageProbe)
  if transient>0 then
    transient=transient-1
    return {scene='unknown',frameId=frame}
  end
  local clear=chapterTwo or drags>=6 and forward==0 and scenario~='stuck'
  local page={scene='unknown',frameId=frame,stageRail={x=287,y=257,clear=clear},words={}}
  if clear and chapterTwo then
    page.words={{text='阶段6',x=370,y=238},{text='阶段7',x=1019,y=238}}
  elseif clear then
    if scenario=='focused' or scenario=='noisy-first' and frame%2==0 then
      page.stageFirstLabel='|阶段1'
    else page.words[1]={text='阶段1',x=370,y=238} end
    table.insert(page.words,{text='阶段2',x=1019,y=238})
  elseif forward>0 then
    page.words={{text='阶段2',x=393,y=238},{text='阶段3',x=750,y=238},{text='阶段4',x=1100,y=238}}
  elseif drags==0 then
    -- The chapter can be visible with every badge clipped or unreadable.
  elseif drags==1 or scenario=='stuck' then
    -- A tempting reading at the normal x position must not bypass the clipped-card guard.
    page.words[1]={text='阶段1',x=355,y=238}
  else
    page.words[1]={text='阶段'..(8-drags),x=350+drags*13,y=238}
  end
  return page
end

function test()
  local ok,result
  if scenario=='reselect-fifth' then
    utils.openSpecialEntry=function()error('Reselect inside the event without reopening it')end
    ok,result=pcall(utils.selectSpecialStage,_soloEventUi())
  else
    ok,result=pcall(utils.intoSpecial,{})
  end
  if scenario=='stuck' then
    assert(not ok and result:find('列表最左',1,true) and not stageSelected,result)
  elseif scenario=='wrong-stage' then
    assert(not ok and result:find('未能确认进入',1,true) and stageSelected,result)
  else
    assert(ok and result==1 and stageSelected and (target~=3 or forward==1),result)
  end
  done=true
end
