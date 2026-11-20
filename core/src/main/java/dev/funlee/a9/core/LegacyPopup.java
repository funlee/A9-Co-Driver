package dev.funlee.a9.core;

/** Fixed 720 × 1280 BGR signatures used by the original popup handler and its HUD monitor. */
public enum LegacyPopup {
    ACCOUNT(true, 0, 0, "477|1068|5400FF-030303,479|990|2B190C-030303,161|474|1D0D0D-030303,113|116|DB9714-030303,41|62|FFFFFF-030303"),
    TIP_ONE(false, 192, 610, "190|560|FFFFFF-101010,540|330|2C180C-101010,550|1000|2C1A0C-101010"),
    TIP_TWO(false, 580, 1130, "564|370|000000-101010,580|1132|5400FF-101010,584|1080|000000-101010"),
    TIP_THREE(false, 99, 588, "99|587|FFFFFF-101010,152|542|FFFFFE-101010,116|684|14FBC3-101010"),
    TIP_SEQUENCE(false, 158, 692, "150|325|5E6822-101010,496|278|BDD045-101010,580|331|0C0C03-101010"),
    TIP_FIVE(true, 100, 600, "394|446|FFFFFF-101010,88|718|FFFFFF-101010,594|200|000000-101010"),
    AD_BACK(true, 0, 0, "677|1250|060302-020202,677|1238|151413-020202,697|1250|181817-020202,679|1261|151413-020202"),
    TIP_FIVE_ALTERNATE(true, 100, 600, "104|598|FFFFFF-090909,102|678|12FBC3-090909,216|564|02E7FF-090909,213|710|FFFFFF-090909"),
    HUD_PROMPT(true, 240, 800, "505|169|2A190C-101010,240|311|FFFFFF-101010,233|723|FFFFFF-101010,508|1085|2A190C-101010");

    @FunctionalInterface public interface Pixels { int at(int x, int y); }

    private static final LegacyPopup[] RULES = values();
    public final int tapX, tapY;
    private final boolean background;
    private final int[] xs, ys;
    private final ColorPattern.Test[] colors;

    LegacyPopup(boolean background, int tapX, int tapY, String pattern) {
        this.background = background;
        this.tapX = tapX;
        this.tapY = tapY;
        String[] points = pattern.split(",");
        xs = new int[points.length];
        ys = new int[points.length];
        colors = new ColorPattern.Test[points.length];
        for (int i = 0; i < points.length; i++) {
            String[] fields = points[i].split("\\|");
            xs[i] = Integer.parseInt(fields[0]);
            ys[i] = Integer.parseInt(fields[1]);
            colors[i] = new ColorPattern.Test(fields[2], 1);
        }
    }

    public static LegacyPopup match(PixelFrame frame) { return match(frame::pixel, false); }

    public static LegacyPopup match(Pixels pixels, boolean backgroundOnly) {
        for (LegacyPopup rule : RULES) {
            if ((!backgroundOnly || rule.background) && rule.matches(pixels)) return rule;
        }
        return null;
    }

    private boolean matches(Pixels pixels) {
        for (int i = 0; i < colors.length; i++) {
            if (!colors[i].matches(pixels.at(xs[i], ys[i]))) return false;
        }
        return true;
    }

    /** Known menus and sensitive transaction prompts stay with their dedicated handler. */
    public boolean permitsInput(String text) {
        if (this == ACCOUNT || AccountLoginDialog.matches(text) || RecoveryReadiness.incompatibleDevice(text)) return false;
        String scene = MultiplayerUi.classify(text);
        if (this == AD_BACK && scene.equals("promo")) return true;
        if (!scene.equals("unknown") || !DailyEventUi.classifySolo(text).equals("unknown")) return false;
        String label = MapMatcher.normalize(text);
        for (String guarded : new String[]{"支付", "购买", "充值", "花费", "消耗", "解锁", "授权", "订阅", "升星", "兑换", "你已降级", "你已经降级"}) {
            if (label.contains(guarded)) return false;
        }
        return true;
    }
}
