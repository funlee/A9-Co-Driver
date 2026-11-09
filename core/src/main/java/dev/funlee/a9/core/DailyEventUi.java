package dev.funlee.a9.core;

/** The daily-loot flow has a solo result table and optional blueprint/conversion screens. */
public final class DailyEventUi {
    private DailyEventUi() {}
    public static boolean rewardExit(String text,int x,int y) {
        return !RaceRewards.exitLabel(text,x,y).isEmpty();
    }
    public static String classify(String text) {
        String t=MapMatcher.normalize(text).replace('賽','赛').replace('兌','兑');
        if(VehicleUnlockGuard.prompt(text))return "vehicleUnlock";
        if(t.contains("立即购买")||t.contains("立即入手"))return "unknown";
        if(t.contains("比赛结果")&&t.contains("你的用时")&&t.contains("排名")&&t.contains("车名"))return "results";
        if(ResultRank.personalReward(text)&&t.contains("总金币"))return "rewards";
        if(t.contains("红币")&&t.contains("将被兑换为红币"))return "conversion";
        if(t.contains("恭喜")&&t.contains("这些奖励"))return "cards";
        if(t.contains("每日车辆战利品")&&t.contains("单人奖励")&&t.contains("最佳用时纪录")&&t.contains("比赛"))return "event";
        if(t.contains("每日车辆战利品")&&t.contains("限时")&&t.contains("赛事")&&!t.contains("单人奖励"))return "loading";
        return "unknown";
    }
}
