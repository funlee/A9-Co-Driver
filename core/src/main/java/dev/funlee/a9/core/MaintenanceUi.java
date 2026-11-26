package dev.funlee.a9.core;

/** The game's shutdown notice ("游戏未开服") blocks login until the server reopens. */
public final class MaintenanceUi {
    /**
     * The upstream helper matched four pixels of that page when reloading the game, but two of
     * them sit on the rotating hero artwork, so the notice text is the primary signal and the
     * original probe is kept for layouts where the whole pattern still matches.
     */
    private static final String UPSTREAM="118|117|513512-050505,45|134|625858-050505,178|474|494141-050505,242|639|453D3D-050505";
    private MaintenanceUi() {}
    public static boolean detected(String text,PixelFrame frame){
        return notice(text)||(frame!=null&&ColorPattern.matches(frame,UPSTREAM,1));
    }
    public static boolean notice(String text){
        if(text==null)return false;
        String t=MapMatcher.normalize(text).replace('開','开').replace('護','护').replace('無','无')
                .replace('錄','录').replace('時','时').replace('間','间').replace('諒','谅');
        if(t.contains("未开服")||t.contains("游戏维护"))return true;
        return t.contains("维护")&&(t.contains("无法登录")||t.contains("开服时间")||t.contains("敬请谅解")||t.contains("维护公告"));
    }
}
