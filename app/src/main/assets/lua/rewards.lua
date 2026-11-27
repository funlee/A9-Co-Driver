-- Current CN free packs and daily goal rewards. Never click prices, ads or Go buttons.
currentRewards={}
-- OCR host calls consume wall time inside one Lua step. Yield once to refresh the
-- scheduler clock before waiting, so an OCR read cannot eat the post-click delay.
local function pause(ms) sleep(1);sleep(ms) end
local function clean(s)
  return (s or ''):gsub('[ \t\r\n]',''):gsub('!',''):gsub('！',''):gsub('領','领'):gsub('獎','奖'):gsub('賽','赛'):gsub('內','内'):gsub('免費','免费'):gsub('免兔费包','免费包'):gsub('兔费包','免费包'):gsub('免费贵包','免费包'):gsub('兔费贵包','免费包'):gsub('免费費包','免费包'):gsub('|',''):gsub('%[',''):gsub('%]','')
end
local function has(screen,label) return clean(screen.text):find(label,1,true)~=nil end
local function word(screen,label,minX,minY,maxY)
  for _,w in ipairs(screen.words or {}) do
    if clean(w.text)==label and w.x>=(minX or 0) and w.y>=(minY or 0) and w.y<=(maxY or 720) then return w end
  end
end
local function click(w,valid,settleMs,read)
  if w.text then
    local stable=false
    for attempt=1,5 do
      pause(500);local s=(read or _rewardUi)();local candidate=nil
      for _,v in ipairs(s.words or {}) do
        if clean(v.text)==clean(w.text) and math.abs(v.y-w.y)<30 and (not valid or valid(s,v)) then
          if not candidate or math.abs(v.x-w.x)<math.abs(candidate.x-w.x) then candidate=v end
        end
      end
      if candidate then
        if math.abs(candidate.x-w.x)<=8 and math.abs(candidate.y-w.y)<=8 then w=candidate;stable=true;break end
        w=candidate
      end
    end
    if not stable then return false end
  end
  tap(719-math.floor(w.y),math.floor(w.x));pause(settleMs or 1600);return true
end
local function waitFor(predicate,timeout,read)
  local deadline=tickCount()+(timeout or 7600)
  repeat
    local s=(read or _rewardUi)()
    if predicate(s) then return s end
    pause(400)
  until tickCount()>=deadline
end
local function slide(x1,y1,x2,y2)
  swipe(719-y1,x1,719-y2,x2,400);pause(1400)
end
local function activity(message)
  if data['信息']~=message then data['信息']=message;_hud(message) end
end
local function report(message,activity) data['信息']=activity or message;_hud(data['信息']);print(message) end
local function home(screen)
  if screen.scene=='home' then return true end
  if screen.scene=='exitGame' then return false end
  local daily,club,play=false,false,false
  for _,w in ipairs(screen.words or {}) do
    if w.y>=600 then
      local t=clean(w.text)
      if w.x>=430 and w.x<=650 and t:find('每日',1,true) then daily=true end
      if w.x>=850 and w.x<=1100 and t:find('俱乐部',1,true) then club=true end
      if w.x>=650 and w.x<=850 and t:find('游戏',1,true) then play=true end
    end
  end
  return (daily and club) or (play and club) or (daily and play)
end
local function freePackHeader(w)
  local label=clean(w.text)
  return w.x>=950 and w.y<=80 and (label=='免费包' or label=='兔费贵包')
end
local function freePackEntry(s)
  if not home(s) then return nil end
  for _,w in ipairs(s.words or {}) do if freePackHeader(w) then return w end end
end
local function confirmFreePackEntry(s)
  for attempt=1,5 do
    local entry=freePackEntry(s)
    if entry then return s,entry end
    if not home(s) then return s,nil end
    local labels={}
    for _,w in ipairs(s.words or {}) do
      if w.x>=950 and w.y<=80 then labels[#labels+1]=w.text..'@'..w.x..','..w.y end
    end
    print('领奖：免费包入口复核 '..attempt..'/5，帧='..tostring(s.frameId or '未知')..
      '，右上角OCR='..(#labels>0 and table.concat(labels,' | ') or '未识别到文字'))
    if attempt==5 then break end
    pause(800)
    local fresh=_rewardUi()
    -- Repeated projection frames must not replace a newer observation.
    if not s.frameId or not fresh.frameId or fresh.frameId>s.frameId then s=fresh end
  end
  return s,nil
end
local function sidebarWord(s,label)
  for _,w in ipairs(s.words or {}) do
    if clean(w.text)==label and w.x<265 and w.y>=80 and w.y<=700 then return w end
  end
end
local function shopSidebar(s)
  local matches=0
  for _,label in ipairs({'充值活动','高级通行证','新手专属','限时钜惠','热销','每日超能包','首充优惠','限时商品包','卡牌包','特殊赛事'}) do
    if sidebarWord(s,label) then matches=matches+1 end
  end
  return matches>=2
end
local function backHome()
  for i=1,15 do
    local s=_rewardUi()
    local stay=word(s,'再玩一会',0,350)
    local next=has(s,'恭喜') and word(s,'继续',800,540)
    if s.scene=='exitGame' or has(s,'退出游戏') or stay then
      if stay then click(stay) else pause(800) end
    elseif has(s,'恭喜') then
      if next then click(next) else pause(800) end
    elseif home(s) then
      pause(700);local fresh=_rewardUi()
      if home(fresh) and not has(fresh,'退出游戏') and not has(fresh,'恭喜') then return fresh end
    elseif has(s,'卡牌包详情') or shopSidebar(s) then
      -- The current store has a fixed Home icon; Back may race its closing animation.
      click(s.homeButton or {x=1250,y=30})
    elseif has(s,'每日目标') or has(s,'掉落几率') or has(s,'此包内可能包含') or
           has(s,'每日车辆战利品') or has(s,'世界系列赛') or has(s,'大师系列赛') or has(s,'周末系列赛') then
      keyPress('back');pause(1600)
    else pause(800) end
  end
  return nil
end

local function rowSignature(s,y)
  local parts={}
  for _,w in ipairs(s.words or {}) do
    if w.x>=400 and w.y>=y-50 and w.y<=y+35 then parts[#parts+1]=clean(w.text) end
  end
  return table.concat(parts,'|')
end
local function packMenu(s) return has(s,'卡牌包') and has(s,'卡牌包详情') end
local function shopPage(s)
  return packMenu(s) or sidebarWord(s,'卡牌包')~=nil or shopSidebar(s)
end
local function revealed(s) return has(s,'恭喜') and (has(s,'你获得了') or has(s,'获得以下')) end
local function featuredPackName(page,button)
  local hasWelfare=false
  for _,label in ipairs(page.words or {}) do
    if label.x>=300 and math.abs(label.x-button.x)<210 and math.abs(label.y-button.y)<260
      and clean(label.text):find('免费福利',1,true) then hasWelfare=true end
  end
  for _,label in ipairs(page.words or {}) do
    local title=clean(label.text)
    -- Decorative card artwork can be appended to a readable gift name by OCR.
    -- The local welfare marker is mandatory before tolerating that suffix.
    if hasWelfare then title=title:match('^(.-包)') or title end
    local dy=button.y-label.y
    -- Welfare tiles place the pack name below the Free button. Only accept
    -- that layout when the same tile also identifies itself as free welfare.
    if label.x>=300 and label.y>=90 and math.abs(label.x-button.x)<150
      and ((dy>30 and dy<260) or (hasWelfare and dy< -30 and dy> -120))
      and title:match('包$') and title~='免费包' then return title end
  end
  -- An exact Free control on a verified store welfare tile is still safe when
  -- the animated gift name itself is unreadable. Prices and ads never qualify.
  if hasWelfare then return '商店免费福利包' end
end
local function featuredFreeButton(page,button)
  -- Called only after the verified home Free Pack entry auto-selects an offer.
  -- The pack title is descriptive, not a prerequisite for an exact Free control.
  return shopPage(page) and not home(page) and clean(button.text)=='免费'
    and button.x>=300 and button.x<=1220 and button.y>=170 and button.y<=680
end
local function claimFeaturedPack(s,free)
  local title=featuredPackName(s,free)
  local name=packMenu(s) and '免费卡牌包' or title=='特殊赛事包' and '特殊赛事免费包' or title or '商店免费福利包'
  activity('正在领取'..name)
  if not click(free,featuredFreeButton,300) then return 0 end
  local function detail(page)
    return has(page,'此包内可能包含') or has(page,'掉落几率')
  end
  s=waitFor(function(page)return revealed(page) or detail(page) end) or _rewardUi()
  if not revealed(s) and detail(s) then
    -- Detail text can appear before the claim control, or be misread on one frame.
    local ready=waitFor(function(page)
      return revealed(page) or (detail(page) and word(page,'免费',300,550)~=nil)
    end)
    if ready then s=ready end
    if not revealed(s) then
      local detailFree=word(s,'免费',300,550)
      if detailFree and click(detailFree,detail,300) then s=waitFor(revealed) or _rewardUi() end
    end
  end
  if not revealed(s) then report('领奖：'..name..'领取结果未确认');return 0 end
  report('领奖：已确认领取'..name)
  local next=word(s,'继续',800,540)
  if next then click(next) end
  return 1
end
local function freePacksAtEntry(entry)
  if not click(entry,function(s,w)return home(s) and freePackHeader(w) end,300) then
    report('领奖：免费包入口尚未稳定，暂未领取');return 0
  end
  local s=waitFor(shopPage)
  if not s then report('领奖：未确认商店页面，免费包暂未领取');return 0 end
  -- The home entry auto-selects the pending offer. Never override that landing
  -- page with a category click or carousel swipe, even while content is loading.
  for attempt=1,5 do
    if shopPage(s) then
      for _,button in ipairs(s.words or {}) do
        if featuredFreeButton(s,button) then return claimFeaturedPack(s,button) end
      end
    end
    if attempt==5 then break end
    activity('正在检查定位到的免费包')
    pause(800);s=_rewardUi()
  end
  report('领奖：当前定位页面未确认免费领取按钮，不切换商店分类')
  return 0
end

local function freePackages(s)
  if UIconfig.freePkg~='0' then return s end
  local entry
  s,entry=confirmFreePackEntry(s)
  if not entry then
    print('领奖：多帧复核后未确认免费包入口，跳过本次检查')
    return s
  end
  report('领奖：检查商店免费包与免费福利','正在检查免费包')
  for attempt=1,5 do
    local claimed=freePacksAtEntry(entry)
    s=backHome()
    if not s then return nil end
    s,entry=confirmFreePackEntry(s)
    if not entry then return s end
    if claimed==0 then break end
  end
  report('领奖：商店仍提示有免费包，尚未确认全部领取')
  return s
end

local function tokens(s)
  if UIconfig.autoToken~='0' then return end
  if s.dailyGoalsHighlighted==true then s=_rewardUi() end
  if not home(s) or s.dailyGoalsHighlighted~=true then
    print('领奖：主界面每日目标图标未标黄，跳过每日目标检查')
    return
  end
  report('领奖：检查每日目标奖励','正在检查每日奖励')
  local function readGoals()return _rewardUi(true)end
  -- The clipboard is the daily-goal entry on the verified current CN home header.
  click({x=440,y=30},nil,300);s=waitFor(function(page)return word(page,'每日目标',0,60,155)~=nil end,nil,readGoals)
  local tab=s and word(s,'每日目标',0,60,155)
  if not tab then report('领奖：未确认每日目标页面，暂未领取');backHome();return end
  click(tab,nil,nil,readGoals);s=readGoals()
  local count=0;local pages=0;local lastPage='';local stalled=false;local unknown=0
  local untilTime=tickCount()+90000
  while tickCount()<untilTime and count<20 do
    if not has(s,'每日目标') then
      if revealed(s) then
        local next=word(s,'继续',800,540)
        if next then click(next,nil,nil,readGoals) else pause(800) end
        s=readGoals()
      else
        unknown=unknown+1;if unknown>10 then stalled=true;break end
        pause(800);s=readGoals()
      end
    else
      unknown=0
      activity(count>0 and ('检查剩余每日奖励 · 已领取 '..count..' 项') or '正在检查每日奖励')
      local claim=word(s,'领取',900,170) or word(s,'领取奖励',900,170)
      if claim then
        activity('正在领取每日奖励')
        if not click(claim,nil,300,readGoals) then stalled=true;break end
        local beforeText=clean(s.text)
        local beforeRow=rowSignature(s,claim.y)
        local after=waitFor(function(page)
          if revealed(page) then return true end
          if clean(page.text)==beforeText or not has(page,'每日目标') then return false end
          local still=word(page,'领取',900,170) or word(page,'领取奖励',900,170)
          return not still or rowSignature(page,claim.y)~=beforeRow
        end,nil,readGoals)
        if not after then stalled=true;break end
        count=count+1;report('领奖：已确认领取每日目标奖励 '..count..' 项');s=after
      elseif pages<6 then
        if pages>0 and clean(s.text)==lastPage then break end
      lastPage=clean(s.text);pages=pages+1;slide(810,650,810,320);s=readGoals()
      else break end
    end
  end
  if stalled or tickCount()>=untilTime then report('领奖：每日目标领取结果未确认，稍后重试')
  elseif count==0 then report('领奖：当前没有可领取的每日目标奖励')
  else report('领奖：每日目标领取完成，共 '..count..' 项') end
  backHome()
end

function currentRewards.enabled()
  return UIconfig.freePkg=='0' or UIconfig.autoToken=='0'
end

function currentRewards.collect()
  if not currentRewards.enabled() then return end
  if _racePanel then _racePanel(false) end
  local s=backHome()
  if not s then report('领奖：未确认主界面，暂未检查奖励');return end
  s=freePackages(s)
  if s then tokens(s) end
  report('领奖检查结束，继续任务')
end
