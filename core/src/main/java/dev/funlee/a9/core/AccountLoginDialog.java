package dev.funlee.a9.core;

/** The game reports concurrent access separately from login history or network failures. */
public final class AccountLoginDialog {
    public static final String STOP_REASON = "游戏账号已在其他设备登录";
    private AccountLoginDialog() {}

    public static boolean matches(String text) {
        String normalized = MapMatcher.normalize(text).replace('檢','检').replace('測','测')
                .replace('並','并').replace('為','为').replace('該','该').replace('帳','账')
                .replace('賬','账').replace('號','号').replace('臺','台').replace('設','设')
                .replace('備','备').replace('錄','录').replace('陸','陆').replace('從','从')
                .replace("帐号","账号").replace("测测到","测到").replace("井行存取","并行存取");
        return normalized.contains("检测到并行存取行为") ||
                (normalized.contains("账号从另一台设备") &&
                        (normalized.contains("设备登录") || normalized.contains("设备登陆")));
    }
}
