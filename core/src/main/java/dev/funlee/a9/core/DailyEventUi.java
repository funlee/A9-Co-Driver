package dev.funlee.a9.core;

import java.util.List;
import java.util.Map;

/** The daily-loot flow has a solo result table and optional blueprint/conversion screens. */
public final class DailyEventUi {
    private DailyEventUi() {}
    public static boolean rewardExit(String text,int x,int y) {
        return !RaceRewards.exitLabel(text,x,y).isEmpty();
    }
    public static String classify(String text) {
        String t=MapMatcher.normalize(text).replace('賽','赛').replace('兌','兑');
        if(VehicleUnlockGuard.prompt(text))return "vehicleUnlock";
        if(ProfileUnlockUi.prompt(text))return "profileUnlock";
        if(DynamicRewards.prompt(text))return "rewardChoice";
        if(t.contains("立即购买")||t.contains("立即入手"))return "unknown";
        if(t.toUpperCase(java.util.Locale.ROOT).contains("TOUCHDRIVE")&&t.contains("性能分")&&
                (t.contains("开始")||t.contains("跳过")))return "car";
        if(t.contains("比赛结果")&&t.contains("你的用时")&&(t.contains("排名")||t.contains("车名")))return "results";
        if(ResultRank.personalReward(text)&&t.contains("总金币"))return "rewards";
        if(t.contains("红币")&&t.contains("将被兑换为红币"))return "conversion";
        if((t.contains("恭喜")&&t.contains("这些奖励"))||
                (t.contains("名人堂")&&t.contains("继续")&&!t.contains("你的用时")&&!t.contains("比赛奖励")))return "cards";
        if(t.contains("每日车辆战利品")&&t.contains("单人奖励")&&t.contains("比赛")&&
                (t.contains("最佳用时纪录")||t.contains("俱乐部奖励")))return "event";
        if(t.contains("每日车辆战利品")&&t.contains("单人奖励")&&t.contains("俱乐部奖励")&&
                !t.contains("继续")&&!t.contains("比赛")&&
                (t.contains("俱乐部成员")||t.contains("已领取")))return "clubRewards";
        if(t.contains("每日车辆战利品")&&t.contains("限时")&&t.contains("赛事")&&!t.contains("单人奖励"))return "loading";
        return "unknown";
    }
    /** Cup/special result pages share the solo reward controls, but not the daily lobby title. */
    public static String classifySolo(String text) {
        String scene=classify(text);
        if(!scene.equals("unknown"))return scene;
        String t=MapMatcher.normalize(text).replace('賽','赛').replace('擇','择')
                .replace('紙','纸').replace('鎖','锁');
        if(t.contains("性能分")&&t.contains("取图纸")&&
                (t.contains("立刻获取")||t.contains("图纸以解锁")))return "lockedCar";
        if(t.contains("性能分")&&t.contains("获取钥匙")&&t.contains("车钥匙以解锁"))return "lockedCar";
        if(t.contains("立即购买")||t.contains("立即入手"))return "unknown";
        if((t.contains("车辆选择")||t.contains("车辅选择"))&&
                t.contains("选择参与此赛事的车辆"))return "cars";
        if((t.contains("门票不足")||t.contains("门票已用完")||t.contains("没有足够的门票")))return "noTicket";
        // Credit cups and car hunts can have only a solo rewards tab.
        boolean timedEvent=t.contains("最佳用时")||
                (t.contains("寻车赛事")&&t.contains("打破比赛用时纪录"));
        if(t.contains("单人奖励")&&timedEvent&&t.contains("比赛")&&
                !t.contains("继续"))return "event";
        // Ranked cups can return to their ranking-reward tab instead of single-player rewards.
        if(t.contains("单人奖励")&&t.contains("排名奖励")&&t.contains("比赛")&&
                (t.contains("顶尖排名")||t.contains("排行榜")||t.contains("最佳用时")))return "event";
        if(t.contains("俱乐部奖励")&&t.contains("单人奖励")) {
            if(t.contains("继续")&&t.contains("阶段"))return "missionRewards";
            if(t.contains("比赛")&&(t.contains("最佳用时")||t.contains("阶段")||t.contains("赛事")))return "event";
        }
        return "unknown";
    }

    /** An expired daily card can still open its reward tabs without offering a race. */
    public static boolean rewardOnly(String text, List<?> words, PixelFrame frame) {
        String normalized=MapMatcher.normalize(text);
        if(!normalized.contains("每日车辆战利品")||!normalized.contains("单人奖励")||
                !normalized.contains("俱乐部奖励")||!normalized.contains("最佳用时纪录"))return false;
        for(Object value:words){
            Map<?,?> word=(Map<?,?>)value;
            int x=((Number)word.get("x")).intValue(),y=((Number)word.get("y")).intValue();
            String label=MapMatcher.normalize(word.get("text").toString()).replace('賽','赛');
            if(x>850&&y>535&&label.equals("比赛"))return false;
            if(x<300&&y<260&&(label.contains("小时")||label.contains("分钟")))return false;
        }
        int lime=0;
        for(int y=570;y<=700;y+=8)for(int x=880;x<=1250;x+=8){
            int c=frame.pixel(719-y,x),r=(c>>16)&255,g=(c>>8)&255,b=c&255;
            if(r>=145&&g>=215&&b<=65)lime++;
        }
        return lime<20;
    }
}
