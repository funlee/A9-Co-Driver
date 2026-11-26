package dev.funlee.a9.core;

import java.util.List;
import java.util.Map;

/** Semantic guards for the current Chinese multiplayer menus. Unknown screens never trigger Start. */
public final class MultiplayerUi {
    public static boolean raceEnded(String scene) {
        return "result".equals(scene)||"raceDisconnected".equals(scene)||"raceCancelled".equals(scene);
    }
    public static String displayMode(String mode) {
        if("多人上".equals(mode))return "世界系列赛";
        if("多人下".equals(mode))return "大师系列赛";
        return mode;
    }
    public static boolean limitedSalePromo(String text) {
        String t=MapMatcher.normalize(text).replace('賽','赛').replace('傳','传').replace('銷','销');
        return (t.contains("传奇促销")||t.contains("赛道制霸"))
                &&(t.contains("限时发售")||t.contains("一举成名"));
    }
    /**
     * Credit and reward-store banners ("限时充值奖励", "精选奖励") layered over the lobby. They
     * hide every menu control, so they never count as a usable screen; Back closes them.
     */
    public static boolean creditPromo(String text) {
        String t=MapMatcher.normalize(text).replace('賽','赛').replace('獎','奖').replace('勵','励')
                .replace('禮','礼').replace('購','购').replace('買','买').replace('儲','储');
        if(t.contains("充值")||t.contains("储值"))return t.contains("奖励")||t.contains("礼包")||t.contains("赛事");
        return t.contains("精选奖励")&&(t.contains("限时")||t.contains("购买")||t.contains("补充"));
    }
    public static boolean leagueDemotionLayout(PixelFrame frame) {
        // The dialog's centered white action sits on a dark blue panel. Sample
        // away from the two Chinese glyphs and verify the button's outside edges.
        return ColorPattern.color(frame.pixel(719-517,550),"FFFFFF-202020",1)
                &&ColorPattern.color(frame.pixel(719-540,716),"FFFFFF-202020",1)
                &&ColorPattern.color(frame.pixel(719-525,520),"533016-252525",1)
                &&ColorPattern.color(frame.pixel(719-525,749),"533016-252525",1)
                &&ColorPattern.color(frame.pixel(719-162,525),"2B190C-151515",1);
    }
    public static Map<String,Integer> leagueDemotionButton(PixelFrame frame,List<?> words) {
        if(!leagueDemotionLayout(frame))return Map.of();
        boolean title=false;
        Map<String,Integer> button=Map.of();
        for(Object item:words){
            if(!(item instanceof Map))continue;
            Map<?,?> word=(Map<?,?>)item;
            Object label=word.get("text"), x=word.get("x"), y=word.get("y");
            if(!(label instanceof String)||!(x instanceof Number)||!(y instanceof Number))continue;
            int centerX=((Number)x).intValue(),centerY=((Number)y).intValue();
            String t=MapMatcher.normalize((String)label).replace('級','级').replace('經','经')
                    .replace('己','已').replace('巳','已').replace('確','确').replace('丨',' ').trim();
            if(centerX>=550&&centerX<=730&&centerY>=158&&centerY<=208&&(t.equals("你已降级")||t.equals("你已经降级")))title=true;
            if(centerX>=555&&centerX<=708&&centerY>=513&&centerY<=543&&t.equals("确定"))button=Map.of("x",centerX,"y",centerY);
        }
        return title?button:Map.of();
    }
    public static String classify(String text,List<?> words) {
        String scene=classify(text);
        String menu=MainMenuUi.classify(text,words);
        // The multiplayer series cards are shown above the same bottom navigation.
        return menu.isEmpty()||(menu.equals("home")&&scene.equals("series"))?scene:menu;
    }
    public static String classify(String text) {
        String t=MapMatcher.normalize(text).replace('賽','赛').replace('擇','择').toUpperCase(java.util.Locale.ROOT);
        if(MainMenuUi.exitDialog(text))return "exitGame";
        if(VehicleUnlockGuard.prompt(text))return "vehicleUnlock";
        if(ProfileUnlockUi.prompt(text))return "profileUnlock";
        if(t.contains("断开连接")||t.contains("连接丢失")||t.contains("连接丟失"))return "raceDisconnected";
        if(t.contains("还未开始")&&t.contains("未找到车手"))return "raceCancelled";
        if((t.contains("未找到对手")||t.contains("末找到对手"))&&t.contains("重试"))return "noOpponents";
        if(t.contains("服务器错误")&&t.contains("执行操作"))return "serverError";
        if(t.contains("登录历史")&&t.contains("游戏号")&&t.contains("最近登录"))return "loginHistory";
        if(t.contains("游戏选项")&&t.contains("游戏设置")&&t.contains("游戏信息"))return "options";
        if(t.contains("每日目标")&&t.contains("成就")&&t.contains("赛季任务"))return "dailyGoals";
        if(t.contains("里程碑")&&t.contains("稍后领取")&&t.contains("前往领奖"))return "milestoneLater";
        // Live frames sometimes read 搜索 as 搜素; keep the same matching episode in either case.
        if(t.matches(".*正在.{1,3}其他玩家和地点.*"))return "matching";
        if(t.contains("晋级奖励")&&t.contains("联赛")&&t.contains("领取"))return "rewardClaim";
        if(DynamicRewards.prompt(text))return "rewardChoice";
        if(t.contains("你的用时")&&(t.contains("你的排名")||t.contains("比赛奖励")||t.contains("排行榜")))return "result";
        if(t.contains("顶级玩家")&&t.contains("俱乐部名称")&&t.contains("我附近的"))return "leaderboard";
        if((t.contains("礼包")&&t.contains("立即购买"))||t.contains("新手狂欢礼")||
                (t.contains("车库")&&t.contains("立即领取"))||
                (t.contains("限时特惠")&&(t.contains("专属特效")||t.contains("立即购买")))||
                (t.contains("限时抢购")&&t.contains("立即入手"))||limitedSalePromo(text)||creditPromo(text))return "promo";
        if((t.contains("倾斜转弯")||t.contains("键盘操作")||t.contains("智能操作"))&&(t.contains("继续")||t.contains("确定"))&&!t.contains("升级"))return "tutorial";
        if((t.contains("车辆选择")||t.contains("车輛选择")||t.contains("车辅选择"))&&t.contains("选择参与"))return "cars";
        if(VehicleUnlockGuard.lockedDetail(text))return "lockedCar";
        if(VehicleUnlockGuard.pendingDetail(text))return "car";
        if(t.contains("TOUCHDRIVE")&&(t.contains("升级")||t.contains("升級")||t.contains("性能分"))&&(t.contains("开始")||t.contains("跳过")))return "car";
        if(t.contains("世界系列赛")&&t.contains("大师系列赛"))return "series";
        if(t.contains("开始")&&((t.contains("排行榜")&&t.contains("里程碑"))||
                (t.contains("系列赛")&&(t.contains("排行榜")||t.contains("里程碑")))))return "event";
        if(t.contains("单人模式")&&(t.contains("多人游戏")||
                (t.contains("每日赛事")&&t.contains("俱乐部")&&t.contains("游戏"))))return "home";
        return "unknown";
    }
}
