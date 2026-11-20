package dev.funlee.a9.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** A centered panel drawn over a dimmed game screen. Coordinates are upright. */
public final class ModalPopup {
    private static final Set<String> LATER = Set.of("稍后", "稍后查看", "稍后领取", "稍后再说", "下次再说", "以后再说", "关闭");
    private static final Pattern GUARDED = Pattern.compile(
            "购买|付费|充值|支付|兑换|消耗|花费|代币|解锁|升星|升级|退出|帐号|账号|登录|重置|删除|覆盖|确认|同意|授权|广告|交易|扣除|会员|订阅|开通");
    private static final Pattern COPY = Pattern.compile("[一-龥]{4}");
    private static final Set<String> SPECIALIZED = Set.of("vehicleUnlock", "profileUnlock", "exitGame", "rewardChoice",
            "raceDisconnected", "raceCancelled", "noOpponents", "serverError", "loginHistory", "tutorial",
            "result", "rewardClaim");
    private static final Set<String> SETTLEMENT = Set.of("results", "rewards", "missionRewards", "conversion", "cards");

    private ModalPopup() {}

    public static final class Panel {
        public final int left, top, right, bottom;
        private Panel(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
        private boolean contains(int x, int y, int margin) {
            return x >= left + margin && x <= right - margin && y >= top + margin && y <= bottom - margin;
        }
    }

    public static Panel locate(PixelFrame frame) {
        if (frame.width != 720 || frame.height != 1280) return null;
        int dim = 0;
        for (int x : new int[]{70, 100, 1180, 1210}) {
            for (int y : new int[]{125, 350, 570}) if (maximum(pixel(frame, x, y)) < 94) dim++;
        }
        if (dim < 10) return null;
        int top = boundary(frame, false, 47, 300, 1, 465, 815);
        int bottom = boundary(frame, false, 382, 682, -1, 465, 815);
        if (top < 0 || bottom < 0 || bottom - top < 155 || Math.abs(top + bottom - 720) > 125) return null;
        int inset = (bottom - top) / 8;
        int left = boundary(frame, true, 110, 510, 1, top + inset, bottom - inset);
        int right = boundary(frame, true, 770, 1170, -1, top + inset, bottom - inset);
        if (left < 0 || right < 0 || Math.abs(left + right - 1280) > 98) return null;
        return new Panel(left, top, right, bottom);
    }

    private static int boundary(PixelFrame frame, boolean vertical, int from, int to, int direction, int lo, int hi) {
        int best = 0, first = -1, last = -1;
        for (int at = from; at <= to; at++) {
            int hits = 0;
            for (int i = 0; i < 16; i++) {
                int along = lo + (hi - lo) * i / 15;
                int inner = axis(frame, vertical, at + 3 * direction, along);
                int outer = axis(frame, vertical, at - 3 * direction, along);
                if (maximum(outer) <= 108 && brightness(inner) - brightness(outer) >= 56) hits++;
            }
            if (hits > best) {
                best = hits;
                first = last = at;
            } else if (hits == best && at == last + 1) last = at;
        }
        return best >= 12 ? (first + last) / 2 : -1;
    }

    private static int axis(PixelFrame frame, boolean vertical, int cross, int along) {
        return vertical ? pixel(frame, cross, along) : pixel(frame, along, cross);
    }

    /** Null delegates to the normal workflow. An empty map means no safe dismissal is visible yet. */
    public static Map<String, Object> dismiss(PixelFrame frame, Panel panel, List<?> words, double aspect) {
        Map<String, Object> conversion = redCoinConversion(frame, words);
        if (conversion != null) return conversion;
        if (panel == null) return null;
        StringBuilder text = new StringBuilder();
        List<Map<String, Object>> later = new ArrayList<>();
        List<Map<String, Object>> next = new ArrayList<>();
        boolean body = false;
        for (Object value : words) {
            Map<?, ?> word = (Map<?, ?>) value;
            int x = ((Number) word.get("x")).intValue(), y = ((Number) word.get("y")).intValue();
            if (!panel.contains(x, y, 12)) continue;
            String label = normalize(word.get("text").toString());
            text.append(label).append('\n');
            if (y < panel.top + (panel.bottom - panel.top) * 55 / 100 && COPY.matcher(label).find()) body = true;
            if (y < panel.top + (panel.bottom - panel.top) / 2 ||
                    !(LATER.contains(label) || label.equals("继续") || label.equals("继续游戏")) ||
                    !button(frame, panel, x, y, label.length())) continue;
            Map<String, Object> action = Map.of("x", x, "y", y, "label", label);
            (LATER.contains(label) ? later : next).add(action);
        }
        String content = text.toString();
        if (SPECIALIZED.contains(MultiplayerUi.classify(content)) || SETTLEMENT.contains(DailyEventUi.classifySolo(content)) ||
                content.contains("你已降级") || content.contains("你已经降级") ||
                RecoveryReadiness.incompatibleDevice(content) || AccountLoginDialog.matches(content)) return null;

        int[] cross = PopupCloseDetector.findInPanel(frame, panel, aspect);
        if (cross == null && MultiplayerUi.limitedSalePromo(content)) {
            int[] legacy = PopupCloseDetector.findLimitedSale(frame);
            if (legacy != null && legacy[0] >= panel.right - 100 && legacy[1] <= panel.top + 100 &&
                    panel.contains(legacy[0], legacy[1], 5)) cross = legacy;
        }
        if (cross != null) return Map.of("x", cross[0], "y", cross[1], "label", "×");
        if (later.size() == 1) return later.get(0);
        if (later.isEmpty() && next.size() == 1 && body && !GUARDED.matcher(content).find()) return next.get(0);
        return content.contains("新手狂欢礼") ? null : Map.of();
    }

    // This reward is a full screen; the central card is artwork rather than a modal.
    private static Map<String, Object> redCoinConversion(PixelFrame frame, List<?> words) {
        boolean title = false;
        StringBuilder explanation = new StringBuilder();
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (Object value : words) {
            Map<?, ?> word = (Map<?, ?>) value;
            int x = ((Number) word.get("x")).intValue(), y = ((Number) word.get("y")).intValue();
            String label = normalize(word.get("text").toString())
                    .replace('紅', '红').replace('幣', '币').replace('餘', '余')
                    .replace('換', '换').replace('爲', '为').replace('為', '为')
                    .replace('紙', '纸').replace('輛', '辆').replace('獎', '奖').replace('勵', '励')
                    .replace('額', '额').replace('複', '复');
            if (x >= 50 && x <= 265 && y >= 34 && y <= 122 && label.equals("红币")) title = true;
            if (x >= 100 && x <= 880 && y >= 80 && y <= 205) explanation.append(label);
            if (x >= 930 && x <= 1205 && y >= 575 && y <= 695 &&
                    (label.equals("确定") || label.equals("确认")) && limeButton(frame, x, y)) {
                candidates.add(Map.of("x", x, "y", y, "label", "红币兑换确定"));
            }
        }
        String body = explanation.toString();
        if (!title || !(body.contains("多余") && body.contains("兑换为红币") ||
                body.contains("满级车辆") && body.contains("额外图纸") && body.contains("重复"))) return null;
        return candidates.size() == 1 ? candidates.get(0) : Map.of();
    }

    /** Cheap precheck for callers that otherwise use pixel-only menu recognition. */
    public static boolean possibleRedCoinConversion(PixelFrame frame) {
        if (frame.width != 720 || frame.height != 1280 || !limeButton(frame, 1100, 650)) return false;
        int red = 0;
        for (int x = 45; x <= 155; x += 3) for (int y = 45; y <= 95; y += 3) {
            int c = pixel(frame, x, y), r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
            if (r >= 175 && r - g >= 95 && r - b >= 20) red++;
        }
        return red >= 7;
    }

    private static boolean limeButton(PixelFrame frame, int x, int y) {
        int hits = 0;
        for (int dx : new int[]{-46, 46}) for (int dy : new int[]{-14, 0, 14}) {
            int c = pixel(frame, x + dx, y + dy), r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
            if (r >= 145 && g >= 207 && g >= r - 12 && b <= 92) hits++;
        }
        return hits >= 5;
    }

    private static String normalize(String text) {
        return MapMatcher.normalize(text).replace('繼','继').replace('續','续').replace('遊','游')
                .replace('戲','戏').replace('後','后').replace('領','领').replace('閉','闭')
                .replace('購','购').replace('買','买').replace('級','级').replace('鎖','锁')
                .replace('認','认').replace('確','确').replace('號','号').replace('錄','录').replace('兌','兑');
    }

    private static boolean button(PixelFrame frame, Panel panel, int x, int y, int length) {
        int dx = 12 + length * 11;
        if (!panel.contains(x - dx, y, 5) || !panel.contains(x + dx, y, 5)) return false;
        for (int direction : new int[]{-1, 1}) {
            int left = pixel(frame, x - dx, y + direction * 10);
            int right = pixel(frame, x + dx, y + direction * 10);
            if (distance(left, right) > 76) continue;
            for (int dy : new int[]{24, 34, 45}) {
                int edge = y + direction * dy;
                if (panel.contains(x, edge, 1) && distance(left, pixel(frame, x - dx, edge)) >= 72 &&
                        distance(right, pixel(frame, x + dx, edge)) >= 72) return true;
            }
        }
        return false;
    }

    static int pixel(PixelFrame frame, int x, int y) { return frame.pixel(719 - y, x); }
    static int maximum(int rgb) { return Math.max(rgb >> 16 & 255, Math.max(rgb >> 8 & 255, rgb & 255)); }
    private static int brightness(int rgb) { return (rgb >> 16 & 255) + (rgb >> 8 & 255) + (rgb & 255); }
    static int distance(int a, int b) {
        return Math.abs((a >> 16 & 255) - (b >> 16 & 255))
                + Math.abs((a >> 8 & 255) - (b >> 8 & 255)) + Math.abs((a & 255) - (b & 255));
    }
}
