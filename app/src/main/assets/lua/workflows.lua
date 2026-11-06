run = {}
run.data = {}
run.index = 1
run.dataList = {
  ["多人上"] = function()
    data["多人"] = data["多人"] + 1
  end,
  ["多人下"] = function()
    data["多人"] = data["多人"] + 1
  end,
  ["杯赛"] = function()
    data["杯赛"] = data["杯赛"] + 1
  end,
  ["特殊赛事"] = function()
    data["杯赛"] = data["杯赛"] + 1
  end,
  ["车联"] = function()
    data["杯赛"] = data["杯赛"] + 1
  end,
  ["生涯"] = function()
    data["生涯"] = data["生涯"] + 1
  end,
  ["每日战利品"] = function()
    data["杯赛"] = data["杯赛"] + 1
  end
}
function run.main()
  while true do
    for i = 1, 20 do
      local cfg = UIconfig.projects[i]
      data["项目"] = cfg["项目"]
      run.index = i
      if UIconfig.ActiveProj[i] and data["项目"] ~= "无" then
        utils.backToMain()
        utils.checkAuto()
        utils.initCar()
        run.data = cfg.data
        run.project()
        run.data = {}
      end
    end
    run.restHandle()
  end
end
function run.project()
  run.nitro()
  local ret = 0
  local times = tonumber(run.data["次数"]) or 1
  if UIconfig.spPrize ~= "" then
    for sp = 1, #UIconfig.spPos do
      utils.getSpPrize(UIconfig.spPos[sp])
    end
  end
  if data["项目"] == "每日战利品" and os.date("%H") == 0 and os.date("%M") < 30 then
    toast("0:00到0:30不打每日战利品")
    os.remove(LOCAL_ROOT .. "/cup" .. run.index .. ".png")
    dailyCup.carList = {}
    return 0
  end
  for i = 1, times do
    data["心跳时间"] = os.time()
    utils.freePkg(3)
    utils.getToken()
    utils.getCoupons()
    while true do
      for im1 = 1, #run.list[data["项目"]] do
        keepCapture()
        if cmpColorEx(run.list[data["项目"]][im1][1], 1) == 1 then
          ret = run.list[data["项目"]][im1][2]()
          if ret ~= nil then
            goto lbl_129
          end
        end
        sleep(10)
      end
      utils.tipsHandle()
      run.rebootHandle()
      sleep(500)
    end
    ::lbl_129::
    if ret == -1 then
      return -1
    else
      run.dataList[data["项目"]]()
      _sessionLegacyCompleted()
    end
  end
  return ret
end
function run.nitro()
  data["氮气漂流"] = false
  if data["项目"] == "多人上" and utils.defultCfg(run.data["多人上漂流"], "") ~= "" then
    data["氮气漂流"] = true
  elseif data["项目"] == "多人下" and utils.defultCfg(run.data["多人下漂流"], "") ~= "" then
    data["氮气漂流"] = true
  elseif data["项目"] == "杯赛" and utils.defultCfg(run.data["杯赛漂流"], "") ~= "" then
    data["氮气漂流"] = true
  elseif data["项目"] == "特殊赛事" and utils.defultCfg(run.data["特殊赛事漂流"], "") ~= "" then
    data["氮气漂流"] = true
  elseif data["项目"] == "生涯" and utils.defultCfg(run.data["生涯漂流"], "") ~= "" then
    data["氮气漂流"] = true
  elseif data["项目"] == "每日战利品" and utils.defultCfg(run.data["杯赛漂流"], "") ~= "" then
    data["氮气漂流"] = true
  end
  data["用车情况"] = {
    0,
    0,
    0
  }
  data["用车次数"] = {}
end
