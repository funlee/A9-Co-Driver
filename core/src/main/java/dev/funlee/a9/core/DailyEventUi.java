package dev.funlee.a9.core;

import java.util.List;
import java.util.Map;

/** The daily-loot flow has a solo result table and optional blueprint/conversion screens. */
public final class DailyEventUi {
    private DailyEventUi() {}
    public static boolean rewardExit(String text,int x,int y) {
        return !RaceRewards.exitLabel(text,x,y).isEmpty();
    }
    public static boolean zeroTicketStart(String text,List<?> words,PixelFrame frame){
        String normalized=MapMatcher.normalize(text).replace('級','级').toUpperCase(java.util.Locale.ROOT);
        if(!normalized.contains("TOUCHDRIVE")||!normalized.contains("升级")||!normalized.contains("加速")||
                normalized.contains("立即购买")||normalized.contains("立即入手"))return false;
        for(Object value:words){
            Map<?,?> word=(Map<?,?>)value;
            int x=((Number)word.get("x")).intValue(),y=((Number)word.get("y")).intValue();
            if(x<990||x>1175||y<605||y>685)continue;
            String label=MapMatcher.normalize(word.get("text").toString());
            if(!label.matches("开始[令Aa]?0")&&!(label.equals("开始40")&&isolatedZeroCost(frame)))continue;
            int lime=0;
            for(int dy:new int[]{-15,15})for(int dx:new int[]{-55,0,55}){
                int color=frame.pixel(719-y-dy,x+dx);
                if(ColorPattern.color(color,"12FBC3-303030",1))lime++;
            }
            if(lime>=4)return true;
        }
        return false;
    }
    private static boolean isolatedZeroCost(PixelFrame frame){
        // In the standard layout OCR can read the adjacent ticket icon as a 4.
        // Read the ink to the right of the icon independently and require one hollow digit.
        int left=1185,right=0,top=665,bottom=0;
        for(int x=1146;x<1185;x++)for(int y=631;y<665;y++)if(ticketInk(frame,x,y)){
            left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
        }
        if(left<1147||left>1152||right-left<9||right-left>16||top<634||bottom-top<17||bottom-top>25)return false;
        int x=(left+right)/2,y=(top+bottom)/2;
        return ticketInk(frame,left+1,y)&&ticketInk(frame,right-1,y)&&ticketInk(frame,x,top+1)&&
                ticketInk(frame,x,bottom-1)&&!ticketInk(frame,x,y);
    }
    private static boolean ticketInk(PixelFrame frame,int x,int y){
        int color=frame.pixel(719-y,x);
        return (color>>16&255)<115&&(color>>8&255)<175;
    }
    public static boolean seasonPassUnlock(String text) {
        String normalized=MapMatcher.normalize(text).replace('級','级').replace('別','别')
                .replace('鎖','锁').replace('賽','赛').replace('購','购').replace('買','买')
                .replace('證','证').replace('現','现');
        return normalized.contains("级别")&&normalized.contains("现已解锁")&&normalized.contains("购买赛季通行证");
    }
    private static boolean dailyLootTitle(String text) {
        return text.replace('車','车').replace('輛','辆').replace('戰','战').contains("每日车辆战利品");
    }
    public static boolean dailyLootLobby(String text) {
        String normalized=MapMatcher.normalize(text).replace('單','单').replace('獎','奖').replace('勵','励');
        return dailyLootTitle(normalized)&&normalized.contains("单人奖励");
    }
    public static String classify(String text) {
        String t=MapMatcher.normalize(text).replace('賽','赛').replace('兌','兑');
        if(VehicleUnlockGuard.prompt(text))return "vehicleUnlock";
        if(ProfileUnlockUi.prompt(text))return "profileUnlock";
        if(DynamicRewards.prompt(text))return "rewardChoice";
        if(t.contains("立即购买")||t.contains("立即入手"))return "unknown";
        if(VehicleUnlockGuard.lockedDetail(text))return "lockedCar";
        if(VehicleUnlockGuard.pendingDetail(text))return "car";
        if(t.toUpperCase(java.util.Locale.ROOT).contains("TOUCHDRIVE")&&t.contains("性能分")&&
                (t.contains("开始")||t.contains("跳过")))return "car";
        if(t.contains("比赛结果")&&t.contains("你的用时")&&(t.contains("排名")||t.contains("车名")))return "results";
        if(RaceRewards.page(text))return "rewards";
        if(t.contains("红币")&&t.contains("将被兑换为红币"))return "conversion";
        if((t.contains("恭喜")&&t.contains("这些奖励"))||
                (t.contains("名人堂")&&t.contains("继续")&&!t.contains("你的用时")&&!t.contains("比赛奖励")))return "cards";
        if(dailyLootLobby(t)&&t.contains("比赛")&&
                (t.contains("最佳用时纪录")||t.contains("俱乐部奖励")))return "event";
        if(dailyLootLobby(t)&&t.contains("俱乐部奖励")&&
                !t.contains("继续")&&!t.contains("比赛")&&
                (t.contains("俱乐部成员")||t.contains("已领取")))return "clubRewards";
        if(dailyLootTitle(t)&&t.contains("限时")&&t.contains("赛事")&&!t.contains("单人奖励"))return "loading";
        return "unknown";
    }
    /** Cup/special result pages share the solo reward controls, but not the daily lobby title. */
    public static String classifySolo(String text) {
        String scene=classify(text);
        if(!scene.equals("unknown"))return scene;
        String t=MapMatcher.normalize(text).replace('賽','赛').replace('擇','择')
                .replace('圖','图').replace('紙','纸').replace('鎖','锁').replace('獲','获')
                .replace('該','该').replace('車','车').replace('輛','辆');
        if((t.contains("筛选条件")||t.contains("筛进条件"))&&t.contains("已拥有")&&
                t.contains("排序方式")&&t.contains("完成"))return "garageFilter";
        if(t.contains("性能分")&&t.contains("最大速度")&&t.contains("操控性")&&t.contains("氮气")&&
                (t.contains("仓库")||t.contains("倉庫"))&&
                (t.contains("阶段")||t.contains("升級")||t.contains("升级"))&&
                !t.toUpperCase(java.util.Locale.ROOT).contains("TOUCHDRIVE"))return "vehicleUpgrade";
        if(t.contains("立即购买")||t.contains("立即入手"))return "unknown";
        if((t.contains("车辆选择")||t.contains("车辅选择"))&&
                t.contains("选择参与此赛事的车辆"))return "cars";
        if((t.contains("门票不足")||t.contains("门票已用完")||t.contains("没有足够的门票")))return "noTicket";
        // The club mode gate asks to clear occupied hazard slots when its value is short.
        if((t.contains("空危险槽")||t.contains("空危險槽")||t.contains("清空所有槽位")||t.contains("清空全部槽位")))return "noTicket";
        if((t.matches(".*阶段[0-9]+.*")||t.contains("顶尖排名")&&t.contains("前30%"))&&t.contains("排行榜")&&
                t.contains("开始"))return "specialRanking";
        // Credit cups and car hunts can have only a solo rewards tab.
        boolean timedEvent=t.contains("最佳用时")||
                (t.contains("寻车赛事")&&t.contains("打破比赛用时纪录"));
        if(t.contains("单人奖励")&&timedEvent&&t.contains("比赛")&&
                !t.contains("继续"))return "event";
        // Ranked cups can return to their ranking-reward tab instead of single-player rewards.
        if(t.contains("单人奖励")&&t.contains("排名奖励")&&t.contains("比赛")&&
                (t.contains("顶尖排名")||t.contains("排行榜")||t.contains("最佳用时")))return "event";
        if((t.contains("继续")||t.contains("继绩"))&&
                (t.contains("俱乐部奖励")||t.contains("具乐部奖励"))&&
                (t.contains("单人奖励")||t.contains("单人笑励")||t.contains("个人奖励")))return "missionRewards";
        if(t.contains("俱乐部奖励")&&t.contains("单人奖励")) {
            if(t.contains("比赛")&&(t.contains("最佳用时")||t.contains("阶段")||t.contains("赛事")))return "event";
        }
        if(t.contains("限时")&&t.contains("赛事")&&!t.contains("单人奖励")&&!t.contains("继续")&&
                !t.contains("探索赛事")&&!t.contains("挑战限时")&&!MultiplayerUi.creditPromo(text))return "loading";
        return "unknown";
    }

    public static String classifySolo(String text,List<?> words) {
        String scene=classifySolo(text);
        if(!scene.equals("unknown"))return scene;
        // An activity hint can hide TouchDrive, and OCR can merge the performance
        // heading with the score. The stat panel and the race button remain visible.
        String t=VehicleUnlockGuard.normalize(text);
        if(t.contains("升级")&&t.contains("加速")&&t.contains("操控性")&&
                !t.contains("立即购买")&&!t.contains("立即入手")&&VehicleUnlockGuard.hasRaceStart(words))return "car";
        return scene;
    }

    /** An expired daily card can still open its reward tabs without offering a race. */
    public static boolean rewardOnly(String text, List<?> words, PixelFrame frame) {
        String normalized=MapMatcher.normalize(text);
        if(!dailyLootLobby(normalized)||
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
