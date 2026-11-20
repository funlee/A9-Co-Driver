package dev.funlee.a9.core;

import java.util.Set;

/** A foreground splash/login animation is not yet an actionable game menu. */
public final class RecoveryReadiness {
    private static final Set<String> MENUS=Set.of("home","series","event","cars","car","promo","options","dailyGoals","milestoneLater","vehicleUnlock","profileUnlock","leagueDemotion");
    private RecoveryReadiness() {}
    public static boolean incompatibleDevice(String text) {
        String normalized=MapMatcher.normalize(text).replace('遊','游').replace('戲','戏');
        return normalized.contains("设备无法兼容该游戏")&&normalized.contains("请更换");
    }
    public static boolean ready(String text){return ready(text,"");}
    public static boolean ready(String text,String scene){
        String normalized=MapMatcher.normalize(text);
        if(normalized.contains("登录中")||normalized.contains("登录历史")||normalized.contains("加载中"))return false;
        return MENUS.contains(scene)||MENUS.contains(MultiplayerUi.classify(text))||
                MENUS.contains(DailyEventUi.classifySolo(text));
    }
}
