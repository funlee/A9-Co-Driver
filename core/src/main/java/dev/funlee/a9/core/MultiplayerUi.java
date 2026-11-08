package dev.funlee.a9.core;

/** Semantic guards for the current Chinese multiplayer menus. Unknown screens never trigger Start. */
public final class MultiplayerUi {
    public static String displayMode(String mode) {
        if("多人上".equals(mode))return "世界系列赛（原多人上）";
        if("多人下".equals(mode))return "大师系列赛（原多人下）";
        return mode;
    }
    public static String classify(String text) {
        String t=MapMatcher.normalize(text).replace('賽','赛').toUpperCase(java.util.Locale.ROOT);
        if(VehicleUnlockGuard.prompt(text))return "vehicleUnlock";
        if(t.contains("比赛还未开始")&&t.contains("未找到车手"))return "raceCancelled";
        if((t.contains("未找到对手")||t.contains("末找到对手"))&&t.contains("重试"))return "noOpponents";
        if(t.contains("服务器错误")&&t.contains("执行操作"))return "serverError";
        if(t.contains("登录历史")&&t.contains("游戏号")&&t.contains("最近登录"))return "loginHistory";
        if(t.contains("里程碑")&&t.contains("稍后领取")&&t.contains("前往领奖"))return "milestoneLater";
        // Live frames sometimes read 搜索 as 搜素; keep the same matching episode in either case.
        if(t.contains("正在搜")&&t.contains("玩家")&&t.contains("地点"))return "matching";
        if(t.contains("晋级奖励")&&t.contains("联赛")&&t.contains("领取"))return "rewardClaim";
        if(t.contains("动态奖励")&&t.contains("从这组奖励中选"))return "rewardChoice";
        if(t.contains("你的用时")&&(t.contains("你的排名")||t.contains("比赛奖励")||t.contains("排行榜")))return "result";
        if(t.contains("顶级玩家")&&t.contains("俱乐部名称")&&t.contains("我附近的"))return "leaderboard";
        if((t.contains("礼包")&&t.contains("立即购买"))||t.contains("新手狂欢礼")||
                (t.contains("车库")&&t.contains("立即领取"))||
                (t.contains("限时特惠")&&(t.contains("专属特效")||t.contains("立即购买")))||
                (t.contains("限时抢购")&&t.contains("立即入手")))return "promo";
        if((t.contains("倾斜转弯")||t.contains("键盘操作")||t.contains("智能操作"))&&(t.contains("继续")||t.contains("确定"))&&!t.contains("升级"))return "tutorial";
        if(t.contains("车辆选择")&&t.contains("选择参与"))return "cars";
        if(t.contains("TOUCHDRIVE")&&(t.contains("升级")||t.contains("升級")||t.contains("性能分"))&&t.contains("开始"))return "car";
        if(t.contains("世界系列赛")&&t.contains("大师系列赛"))return "series";
        if(t.contains("开始")&&((t.contains("排行榜")&&t.contains("里程碑"))||
                (t.contains("系列赛")&&(t.contains("排行榜")||t.contains("里程碑")))))return "event";
        if(t.contains("多人游戏")&&t.contains("单人模式"))return "home";
        return "unknown";
    }
}
