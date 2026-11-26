package dev.funlee.a9.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Interruptions drawn as panels or full-screen reward prompts. Coordinates are upright. */
public final class ModalPopup {
    private static final Set<String> LATER = Set.of("稍后", "稍后查看", "稍后领取", "稍后再说", "下次再说", "以后再说", "关闭");
    private static final Pattern GUARDED = Pattern.compile(
            "购买|付费|充值|支付|兑换|消耗|花费|代币|解锁|升星|升级|退出|帐号|账号|登录|重置|删除|覆盖|确认|同意|授权|广告|交易|扣除|会员|订阅|开通");
    private static final Pattern COPY = Pattern.compile("[一-龥]{4}");
    private static final Set<String> SPECIALIZED = Set.of("vehicleUnlock", "profileUnlock", "exitGame", "rewardChoice",
            "raceDisconnected", "raceCancelled", "noOpponents", "serverError", "loginHistory", "tutorial",
            "result", "rewardClaim");
    private static final Set<String> SETTLEMENT = Set.of("results", "rewards", "missionRewards", "conversion", "cards");
    private static final Pattern GARAGE_CARD = Pattern.compile("性能分|规定车辆|規定车辆|規定車輛|规定车輛");
    // 车联赛事页（章节卡片、任务列表、额外车联包）都是亮卡压在暗背景上，几何上像弹窗，
    // 但它们由车联流程自己操作。用页面文案识别，避免把正常页面当成需要关闭的弹窗。
    private static final Pattern CLUB_PAGE = Pattern.compile("危险等级|危險等級|车联包|車聯包|特殊任务|特殊任務|下一奖励|下一獎勵");

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
        if (top < 0) return null;
        int bottom = boundary(frame, false, Math.max(382, 720 - top - 125), Math.min(682, 720 - top + 125),
                -1, 465, 815);
        if (bottom < 0 || bottom - top < 155) return null;
        int inset = (bottom - top) / 8;
        int left = boundary(frame, true, 110, 510, 1, top + inset, bottom - inset);
        if (left < 0) return null;
        int right = boundary(frame, true, Math.max(770, 1280 - left - 98), Math.min(1170, 1280 - left + 98),
                -1, top + inset, bottom - inset);
        if (right < 0) return null;
        // The series cards supply two vertical edges between the game's full-width
        // header and tab bar. Those horizontal seams continue past both card edges.
        if (spansBothMargins(frame, top, left, right) && spansBothMargins(frame, bottom, left, right)) return null;
        // Menu screens (series cards, daily promos, the wait notice) draw bright
        // card regions that satisfy the panel geometry. The selected tab's pink
        // highlight stays undimmed on all of them; a real modal dims the whole
        // screen, breaking the highlight colors. The two gutters between the
        // series cards must also remain visible for a menu to be unobstructed.
        int third = (right - left) / 3;
        if (selectedTabHighlight(frame, bottom) &&
                cardGutter(frame, left + third, top + inset, bottom - inset) &&
                cardGutter(frame, right - third, top + inset, bottom - inset)) return null;
        return new Panel(left, top, right, bottom);
    }

    // The selected tab draws a pink line at the bar's top edge over a white tab.
    // Scan the full tab-bar width at the panel bottom: notification badges can
    // cover fixed sample points. A dimming modal breaks both colors, so real
    // popups over the menu still fail this check.
    private static boolean selectedTabHighlight(PixelFrame frame, int bottom) {
        if (bottom < 590 || bottom > 625) return false;
        int run = 0, best = 0;
        for (int x = 40; x <= 1240; x += 4) {
            if (distance(pixel(frame, x, bottom), 0xff0054) <= 70 &&
                    distance(pixel(frame, x, bottom + 20), 0xffffff) <= 60) {
                run += 4;
                if (run > best) best = run;
            } else run = 0;
        }
        return best >= 60;
    }

    // The gap between two series cards is a narrow seam of the dimmed lobby
    // bounded by both card edges.
    private static boolean cardGutter(PixelFrame frame, int center, int top, int bottom) {
        int end = boundary(frame, true, center - 28, center - 2, -1, top, bottom);
        int start = boundary(frame, true, center + 2, center + 28, 1, top, bottom);
        return end >= 0 && start - end >= 8 && start - end <= 40;
    }

    private static boolean spansBothMargins(PixelFrame frame, int y, int left, int right) {
        int leftHits = 0, rightHits = 0;
        for (int gap = 16; gap <= 80; gap += 8) {
            if (distance(pixel(frame, left-gap, y-3), pixel(frame, left-gap, y+3)) >= 24) leftHits++;
            if (distance(pixel(frame, right+gap, y-3), pixel(frame, right+gap, y+3)) >= 24) rightHits++;
        }
        return leftHits >= 6 && rightHits >= 6;
    }

    private static int boundary(PixelFrame frame, boolean vertical, int from, int to, int direction, int lo, int hi) {
        int best = 0, first = -1, last = -1;
        // With equal support prefer the outer seam over an edge inside the artwork.
        for (int at = direction > 0 ? from : to; at >= from && at <= to; at += direction) {
            int hits = 0;
            for (int i = 0; i < 16; i++) {
                int along = lo + (hi - lo) * i / 15;
                int inner = axis(frame, vertical, at + 3 * direction, along);
                int outer = axis(frame, vertical, at - 3 * direction, along);
                // Look for a sharp seam against the dimmed game on either side of the color spectrum.
                // Inside artwork and outside scenery may be equally bright.
                int jump = distance(inner, outer);
                if (maximum(outer) > 108 || jump < 24) continue;
                int fartherIn = axis(frame, vertical, at + 6 * direction, along);
                int fartherOut = axis(frame, vertical, at - 6 * direction, along);
                if (jump >= distance(inner, fartherIn) + 10 && jump >= distance(outer, fartherOut) + 10) hits++;
            }
            if (hits > best) {
                best = hits;
                first = last = at;
            } else if (hits == best && at == last + direction) last = at;
        }
        return best >= 12 ? (first + last) / 2 : -1;
    }

    private static int axis(PixelFrame frame, boolean vertical, int cross, int along) {
        return vertical ? pixel(frame, cross, along) : pixel(frame, along, cross);
    }

    /** Null delegates to the normal workflow. An empty map means no safe dismissal is visible yet. */
    public static Map<String, Object> dismiss(PixelFrame frame, Panel panel, List<?> words, double aspect) {
        return dismiss(frame, panel, words, aspect, false);
    }

    public static Map<String, Object> dismiss(PixelFrame frame, Panel panel, List<?> words, double aspect, boolean keepDailyGoals) {
        Map<String, Object> milestone = clubMilestone(frame, words);
        if (milestone != null) return milestone;
        Map<String, Object> conversion = redCoinConversion(frame, words);
        if (conversion != null) return conversion;
        String textOnScreen = words.stream().map(value -> normalize(((Map<?, ?>) value).get("text").toString()))
                .collect(java.util.stream.Collectors.joining());
        if (textOnScreen.contains("超能福利") && textOnScreen.contains("万能图纸随心升级") &&
                possibleBlueprintGift(frame))
            return giftClaim(frame, words, true);
        if (dailyGoals(textOnScreen)) {
            if (keepDailyGoals) return null;
            int[] close = PopupCloseDetector.findDailyGoals(frame);
            return close == null ? Map.of() : Map.of("x", close[0], "y", close[1], "label", "每日目标关闭");
        }
        if (newcomerGift(textOnScreen)) return giftClaim(frame, words, false);
        if (clubPage(textOnScreen, words)) return null;
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
        // A filtered multiplayer garage (e.g. owned cars by descending rank) can form
        // a bright rectangle that satisfies the panel geometry. Card labels inside
        // the bounds prove it is the garage grid rather than a modal covering it.
        if (GARAGE_CARD.matcher(content).find()) return null;
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
        if (unobstructedPackStore(frame, panel, words)) return null;
        if (later.size() == 1) return later.get(0);
        if (later.isEmpty() && next.size() == 1 && body && !GUARDED.matcher(content).find()) return next.get(0);
        return Map.of();
    }

    /** 车联赛事页由 club.lua 自己驱动，这里只负责识别并交还控制权。 */
    private static boolean clubPage(String textOnScreen, List<?> words) {
        if (clubPage(textOnScreen)) return true;
        int taskLabels = 0;
        for (Object value : words) {
            if (normalize(((Map<?, ?>) value).get("text").toString()).equals("任务")) taskLabels++;
        }
        return taskLabels >= 3;
    }
    /** The club event pages carry their own chapter list, so only their text identifies them. */
    public static boolean clubPage(String textOnScreen) {
        return CLUB_PAGE.matcher(textOnScreen).find();
    }
    private static boolean unobstructedPackStore(PixelFrame frame, Panel panel, List<?> words) {
        if (panel.left < 280 || panel.right > 970) return false;
        boolean hasPacks = false, hasDetails = false;
        Set<String> categories = new java.util.HashSet<>();
        List<Integer> offerColumns = new ArrayList<>();
        Set<String> storeCategories = Set.of("充值活动", "高级通行证", "新手专属", "限时钜惠", "热销",
                "每日超能包", "首充优惠", "限时商品包", "卡牌包");
        for (Object value : words) {
            Map<?, ?> word = (Map<?, ?>) value;
            int x = ((Number) word.get("x")).intValue(), y = ((Number) word.get("y")).intValue();
            String label = normalize(word.get("text").toString());
            if (x < 265 && y >= 60 && y <= 700 && storeCategories.contains(label)) {
                int bright = 0;
                for (int px = Math.max(0, x - 65); px <= Math.min(264, x + 65); px++)
                    for (int py = y - 9; py <= y + 9; py++) {
                        int rgb = pixel(frame, px, py);
                        if ((rgb >> 16 & 255) >= 210 && (rgb >> 8 & 255) >= 210 && (rgb & 255) >= 210) bright++;
                    }
                if (bright >= 20) categories.add(label);
                hasPacks |= label.equals("卡牌包");
            }
            hasDetails |= x >= 300 && y >= 90 && y <= 200 && label.equals("卡牌包详情");
            if (x >= 300 && y >= 540 && y <= 670 &&
                    (label.equals("免费") || label.equals("开始") || label.equals("立刻获取") ||
                            label.equals("即将可用") || label.matches("[0-9]{1,5}"))) {
                if (offerColumns.stream().noneMatch(column -> Math.abs(column - x) < 160)) offerColumns.add(x);
            }
        }
        // Text can remain readable behind a real overlay: require three distinct
        // undimmed sidebar labels as well as the title and multiple card columns.
        return hasPacks && hasDetails && categories.size() >= 3 && offerColumns.size() >= 2;
    }

    private static boolean dailyGoals(String text) {
        return text.contains("每日目标") && (text.contains("成就") && text.contains("赛季任务") ||
                text.contains("连环目标") && text.contains("重置还需"));
    }

    private static boolean newcomerGift(String text) {
        return text.contains("连续登录7日") && (text.contains("新手狂欢礼") ||
                text.contains("领取丰厚好礼") && text.matches(".*第[1-7]天礼物.*"));
    }

    /** The full-screen club reward has a white left button beside the lime claim button. */
    public static boolean possibleClubMilestone(PixelFrame frame) {
        return clubMilestoneButtonRow(frame) >= 0;
    }

    private static int clubMilestoneButtonRow(PixelFrame frame) {
        if (frame.width != 720 || frame.height != 1280) return -1;
        int start = -1;
        for (int y = 515; y <= 701; y += 4) {
            boolean buttons = y <= 690 && white(pixel(frame, 350, y)) && white(pixel(frame, 551, y)) &&
                    lime(pixel(frame, 729, y)) && lime(pixel(frame, 931, y));
            if (buttons) {
                if (start < 0) start = y;
            } else if (start >= 0) {
                if (y - start >= 48) return (start + y - 4) / 2;
                start = -1;
            }
        }
        return -1;
    }

    private static Map<String, Object> clubMilestone(PixelFrame frame, List<?> words) {
        StringBuilder heading = new StringBuilder();
        int laterX = -1, laterY = -1;
        boolean claim = false, claimOnLeft = false;
        for (Object value : words) {
            Map<?, ?> word = (Map<?, ?>) value;
            int x = ((Number) word.get("x")).intValue(), y = ((Number) word.get("y")).intValue();
            String label = normalize(word.get("text").toString())
                    .replace('樂', '乐').replace('達', '达').replace('該', '该').replace('獎', '奖');
            if (x >= 300 && x <= 980 && y >= 30 && y <= 225) heading.append(label);
            if (y >= 530 && y <= 680) {
                if (x >= 280 && x < 625) {
                    if (label.equals("稍后领取")) { laterX = x; laterY = y; }
                    claimOnLeft |= label.equals("前往领奖") || label.equals("立即领取");
                } else if (x >= 655 && x <= 1010) claim |= label.equals("前往领奖");
            }
        }
        String title = heading.toString();
        boolean club = title.contains("俱乐部"), reached = title.contains("达成"), milestone = title.contains("里程碑");
        if (!(club && reached && milestone) && !(laterX >= 0 && claim &&
                (club && (reached || milestone) || reached && milestone))) return null;
        if (claimOnLeft) return Map.of();
        int row = clubMilestoneButtonRow(frame);
        if (row >= 0) return Map.of("x", 450, "y", row, "label", "俱乐部里程碑稍后领取");
        // Narrower layouts may still expose the exact Later label on a white button.
        if (laterX < 0 || !white(pixel(frame, laterX - 68, laterY - 18)) ||
                !white(pixel(frame, laterX + 68, laterY + 18))) return Map.of();
        return Map.of("x", laterX, "y", laterY, "label", "俱乐部里程碑稍后领取");
    }

    private static Map<String, Object> giftClaim(PixelFrame frame, List<?> words, boolean blueprint) {
        Map<String, Object> action = Map.of();
        for (Object value : words) {
            Map<?, ?> word = (Map<?, ?>) value;
            int x = ((Number) word.get("x")).intValue(), y = ((Number) word.get("y")).intValue();
            if (x < 735 || x > 1120 || y < 445 || y > 670 ||
                    !limeButton(frame, x, y, blueprint ? 84 : 88)) continue;
            String label = normalize(word.get("text").toString());
            // The campaign itself is confirmed by banner text and pixels; the button
            // word only has to rule out a purchase variant. OCR on the small lime
            // button misreads or splits the caption, so tolerate that noise.
            boolean matches = blueprint ? label.contains("领") && !GUARDED.matcher(label).find()
                    : label.equals("点击领取");
            if (!matches) continue;
            if (!action.isEmpty()) return Map.of();
            action = Map.of("x", x, "y", y, "label", blueprint ? "万能图纸福利领取" : "新手狂欢礼领取");
        }
        return action;
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
                    (label.equals("确定") || label.equals("确认")) && limeButton(frame, x, y, 46)) {
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
        if (frame.width != 720 || frame.height != 1280 || !limeButton(frame, 1100, 650, 46)) return false;
        int red = 0;
        for (int x = 45; x <= 155; x += 3) for (int y = 45; y <= 95; y += 3) {
            int c = pixel(frame, x, y), r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
            if (r >= 175 && r - g >= 95 && r - b >= 20) red++;
        }
        return red >= 7;
    }

    public static boolean possibleBlueprintGift(PixelFrame frame) {
        if (frame.width != 720 || frame.height != 1280 || maximum(pixel(frame, 75, 250)) >= 90) return false;
        // On the dim lobby the campaign has a pink headline above the cards and a lime footer button.
        boolean footer = false;
        for (int x = 770; x <= 1000; x += 16) {
            if (lime(pixel(frame, x, 540)) && lime(pixel(frame, x, 595))) { footer = true; break; }
        }
        if (!footer) return false;
        for (int x = 225; x <= 535; x += 8) {
            int c = pixel(frame, x, 144);
            if ((c >> 16 & 255) > 205 && (c >> 8 & 255) < 65 && (c & 255) > 45 && (c & 255) < 145) return true;
        }
        return false;
    }

    public static boolean possibleNewcomerGift(PixelFrame frame) {
        if (frame.width != 720 || frame.height != 1280 || !limeButton(frame, 945, 539, 88)) return false;
        return maximum(pixel(frame, 88, 300)) < 90 && maximum(pixel(frame, 1192, 300)) < 90;
    }

    // Dark caption glyphs can sit exactly on a fixed sample grid and veto a whole
    // lime button, so judge by the lime fraction inside the button area instead.
    private static boolean limeButton(PixelFrame frame, int x, int y, int margin) {
        int hits = 0, total = 0;
        for (int dx = -margin; dx <= margin; dx += 4) for (int dy = -14; dy <= 14; dy += 7) {
            total++;
            if (lime(pixel(frame, x + dx, y + dy))) hits++;
        }
        // The caption can cover most of the area (worst case ~73% in tests), so
        // one fifth lime is enough proof against a non-button background.
        return hits * 5 >= total;
    }

    private static boolean white(int color) {
        return (color >> 16 & 255) >= 224 && (color >> 8 & 255) >= 224 && (color & 255) >= 224;
    }

    private static boolean lime(int color) {
        int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
        return r >= 145 && g >= 207 && g >= r - 12 && b <= 92;
    }

    private static String normalize(String text) {
        return MapMatcher.normalize(text).replace('繼','继').replace('續','续').replace('遊','游')
                .replace('戲','戏').replace('後','后').replace('領','领').replace('閉','闭')
                .replace('購','购').replace('買','买').replace('級','级').replace('鎖','锁')
                .replace('認','认').replace('確','确').replace('號','号').replace('錄','录').replace('兌','兑')
                .replace('點','点').replace('擊','击').replace('歡','欢').replace('禮','礼').replace('連','连')
                .replace('賽','赛').replace('環','环').replace('標','标').replace('務','务').replace('還','还')
                .replace('豐','丰').replace('７','7').replace('費','费').replace('萬','万')
                .replace('圖','图').replace('紙','纸').replace('隨','随');
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
    static int distance(int a, int b) {
        return Math.abs((a >> 16 & 255) - (b >> 16 & 255))
                + Math.abs((a >> 8 & 255) - (b >> 8 & 255)) + Math.abs((a & 255) - (b & 255));
    }
}
