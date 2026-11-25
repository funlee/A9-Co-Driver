package dev.funlee.a9.core;

import java.util.Set;

/** A foreground splash/login animation is not yet an actionable game menu. */
public final class RecoveryReadiness {
    private static final Set<String> MENUS=Set.of("home","series","event","cars","car","promo","options","dailyGoals","milestoneLater","vehicleUnlock","profileUnlock","leagueDemotion");
    private static final Set<String> WAIT_OR_PROMPT=Set.of("loading","matching","loginHistory","rewardChoice",
            "serverError","raceCancelled","raceDisconnected","noOpponents","exitGame","interruptingPopup");
    private RecoveryReadiness() {}
    /** Restart-screen signature, sampled on the normalized 720 × 1280 frame. */
    public static boolean restartScreen(PixelFrame frame){
        return ColorPattern.matches(frame,"689|1251|543E30-050505,678|1251|DAD5D5-050505,701|1252|FFFFFF-050505,690|1262|F4F4F4-101010",1);
    }
    public static boolean incompatibleDevice(String text) {
        String normalized=MapMatcher.normalize(text).replace('遊','游').replace('戲','戏');
        return normalized.contains("设备无法兼容该游戏")&&normalized.contains("请更换");
    }
    public static boolean ready(String text){return ready(text,"");}
    public static boolean ready(String text,String scene){return ready(text,scene,false);}
    public static boolean ready(String text,String scene,boolean hasHomeButton){
        String normalized=MapMatcher.normalize(text);
        if(normalized.contains("登录中")||normalized.contains("登录历史")||normalized.contains("加载中")
                ||normalized.contains("检查游戏更新")||normalized.contains("健康游戏忠告")||normalized.contains("适龄提示"))return false;
        String multiplayer=MultiplayerUi.classify(text);
        String solo=DailyEventUi.classifySolo(text);

        // An unnamed in-game subpage can still expose verified Back and Home controls.
        // Do not use navigation behind a login, reward choice or matchmaking overlay.
        if(WAIT_OR_PROMPT.contains(scene)||WAIT_OR_PROMPT.contains(multiplayer)||WAIT_OR_PROMPT.contains(solo))return false;
        return hasHomeButton||MENUS.contains(scene)||MENUS.contains(multiplayer)||MENUS.contains(solo);
    }
}
