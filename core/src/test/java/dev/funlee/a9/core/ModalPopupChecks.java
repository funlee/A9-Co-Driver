package dev.funlee.a9.core;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

final class ModalPopupChecks {
    static int run(Path assets) throws Exception {
        int count = LegacyPopupChecks.run(assets);
        for (int[] sample : new int[][]{
                {250, 72, 1028, 647, 981, 114, 0xe35728, 0x3b2024},
                {250, 72, 1028, 647, 992, 118, 0xffe200, 0xf21870},
                {144, 142, 1136, 578, 1083, 192, 0x264667, 0xffffff}
        }) {
            int[] pixels = panel(sample[0], sample[1], sample[2], sample[3], sample[6]);
            cross(pixels, sample[4], sample[5], sample[7]);
            var action = action(pixels, List.of(word("立即购买", 640, 573)));
            expect(action != null && "×".equals(action.get("label")) &&
                    Math.abs((int) action.get("x") - sample[4]) <= 3 &&
                    Math.abs((int) action.get("y") - sample[5]) <= 3, "Close X is relative to the popup");
            count++;
        }

        int[] pixels = panel(250, 126, 1030, 594, 0x263c65);
        fill(pixels, 355, 472, 548, 532, 0xe6eef0);
        fill(pixels, 723, 472, 929, 532, 0x95e328);
        var title = word("新挑战已经开放", 640, 218);
        var later = word("稍后查看", 451, 502);
        var next = word("继续", 827, 502);
        expect(action(pixels, List.of(title, later, next)).equals(Map.of("x", 451, "y", 502, "label", "稍后查看")),
                "A visible Later button has priority");
        expect(action(pixels, List.of(title, next)).equals(Map.of("x", 827, "y", 502, "label", "继续")),
                "A sole neutral Continue button can dismiss the panel");
        expect(action(pixels, List.of(word("购买礼包后消耗代币", 640, 218), next)).isEmpty(),
                "Never advance a purchase or currency prompt via generic Continue");
        expect(action(pixels, List.of(title, word("继续", 1095, 502))).isEmpty(),
                "Background text is not a popup control");
        expect(action(pixels, List.of(title, word("继续", 640, 390))).isEmpty(),
                "Caption without a button cannot authorize a click");
        expect(action(pixels, List.of(title, word("稍后", 451, 502), word("稍后", 827, 502))).isEmpty(),
                "Two equally plausible Later controls are ambiguous");
        expect(action(pixels, List.of(word("恭喜解锁车辆", 640, 215), later, next)) == null,
                "Vehicle unlock remains under the existing guard");
        expect(action(pixels, List.of(word("检测到并行存取行为", 640, 215), next)) == null,
                "Account-displacement handling takes priority");
        expect(action(pixels, List.of(word("比赛结果 你的用时 排名", 640, 215), next)) == null,
                "Settlement stays in the race workflow");

        int[] openMenu = new int[720 * 1280];
        fill(openMenu, 0, 0, 1279, 719, 0x3d6998);
        cross(openMenu, 992, 118, 0xf21870);
        expect(action(openMenu, List.of(next)) == null, "An X on an ordinary page is not a modal close");
        int[] outside = panel(250, 126, 1030, 594, 0x263c65);
        cross(outside, 1082, 164, 0xfcfcfc);
        expect(action(outside, List.of()).isEmpty(), "An X outside the panel is not used");
        count += 11;

        int[] conversion = panel(0, 0, 1279, 719, 0x0b1832);
        fill(conversion, 84, 54, 89, 80, 0xf20c5c);
        fill(conversion, 966, 622, 1245, 695, 0xbcff00);
        var conversionTitle = word("「紅幣", 117, 67);
        var conversionBody = word("多餘的圖紙或獎勵將被兌換為紅幣", 302, 128);
        var confirm = word("確定", 1105, 660);
        expect(ModalPopup.possibleRedCoinConversion(new PixelFrame(720, 1280, conversion)),
                "Full-screen reward needs OCR even without a centered modal");
        expect(Map.of("x", 1105, "y", 660, "label", "红币兑换确定").equals(action(conversion,
                List.of(conversionTitle, conversionBody, word("不再提示", 160, 660), confirm))),
                "Only the conversion page's visible Confirm button is used");
        expect(action(conversion, List.of(conversionTitle, word("使用红币购买礼包", 302, 128), confirm)) == null,
                "A currency purchase cannot use conversion confirmation");
        fill(conversion, 966, 622, 1245, 695, 0x43531c);
        expect(Map.of().equals(action(conversion, List.of(conversionTitle, conversionBody, confirm))),
                "Wait for confirmation to become enabled");
        count += 4;

        int[] gift = panel(153, 71, 1127, 647, 0x245294);
        fill(gift, 824, 501, 1064, 576, 0xbaff00);
        fill(gift, 878, 520, 1010, 560, 0x161a08);
        var giftTitle = word("新手狂歡禮", 465, 137);
        var giftText = word("連續登錄７日，領取豐厚好禮！", 363, 192);
        var claim = word("點擊領取", 945, 539);
        expect(Map.of("x", 945, "y", 539, "label", "新手狂欢礼领取").equals(
                action(gift, List.of(giftTitle, giftText, claim))), "Claim only the identified newcomer gift");
        expect(action(gift, List.of(word("购买组合包", 465, 137), claim)).isEmpty(),
                "A generic Claim button is not authorization");
        count += 2;

        int[] blueprint = new int[720*1280];
        fill(blueprint,285,135,488,176,0xff0053);
        fill(blueprint,770,530,975,609,0x9afe00);
        var banner=word("超能福利免要送",393,155);
        var headline=word("万能图纸随心升级",450,203);
        var immediate=word("立即领取！",874,567);
        expect("万能图纸福利领取".equals(action(blueprint,List.of(banner,headline,immediate)).get("label")),
                "Recognize the campaign even when OCR misreads the small banner");
        expect(action(blueprint,List.of(immediate))==null,"Generic claim buttons do not identify the campaign");
expect(action(blueprint,List.of(banner,headline,word("立即购买",874,567))).isEmpty(),
"A purchase button cannot advance the blueprint gift");
expect("万能图纸福利领取".equals(action(blueprint,List.of(banner,headline,word("立P领耳！",874,567))).get("label")),
"Tolerate OCR misreads on the claim button of the confirmed campaign");
expect("万能图纸福利领取".equals(action(blueprint,List.of(banner,headline,word("立即",790,567),word("领取！",874,567))).get("label")),
"A claim caption split by OCR still advances the confirmed campaign");
count+=5;

        for (int halfWidth : new int[]{388, 486}) {
            int right = 640 + halfWidth;
            int[] offer = panel(640 - halfWidth, 72, right, 647, 0x10082e);
            fill(offer, 1190, 320, 1240, 380, 0xdde6e3);
            fill(offer, right - halfWidth * 3 / 5, 540, right - halfWidth / 12, 617, 0x9afe00);
            var purchase = word("立即购买", right - halfWidth / 3, 578);
            expect(Map.of().equals(action(offer, List.of(purchase))),
                    "The league offer waits when its close control has not appeared");
            cross(offer, right - 35, 114, 0xffffff);
            var close = action(offer, List.of());
            expect(close != null && "×".equals(close.get("label")) &&
                    Math.abs((int) close.get("x") - (right - 35)) <= 3 && Math.abs((int) close.get("y") - 114) <= 3,
                    "Dark promotional artwork can be closed even without OCR text");
            expect(close.equals(action(offer, List.of(purchase, word("赛季赛事", 490, 645)))),
                    "Purchase and lobby labels do not change the close target");
            count += 3;
        }

        int[] series = panel(222, 92, 1054, 560, 0x915032);
        fill(series, 486, 92, 507, 560, 0x080a18);
        fill(series, 772, 92, 793, 560, 0x080a18);
        fill(series, 0, 58, 1279, 66, 0x405898);
        fill(series, 0, 596, 1279, 605, 0x405898);
        expect(ModalPopup.locate(new PixelFrame(720,1280,series))==null,
                "Series card edges and full-width navigation bars must not form a blocking dialog");
        count++;

        fill(series, 0, 58, 1279, 66, 0x080a18);
        fill(series, 445, 80, 969, 147, 0x222e3a);
        fill(series, 648, 604, 840, 608, 0xff0054);
        fill(series, 648, 618, 840, 705, 0xffffff);
        expect(ModalPopup.locate(new PixelFrame(720,1280,series))==null,
                "The recovery countdown above three series cards is not a blocking dialog");
        fill(series, 220, 80, 1056, 605, 0x413050);
        expect(ModalPopup.locate(new PixelFrame(720,1280,series))!=null,
                "A real dialog covering the gutters still blocks a visible multiplayer tab");
        count+=2;

        int[] store = panel(336, 223, 914, 619, 0x915032);
        // 车联赛事页：亮卡压在暗背景上，但由 club.lua 自己驱动，文案可证明它不是遮挡弹窗。
        var clubCards = List.of(word("任务", 240, 300), word("任务", 520, 300), word("任务", 800, 300),
                word("速度纪元", 240, 150), word("铭蛇竞速", 520, 150), word("铭蛇竞速", 800, 150));
        expect(action(store, List.of()) != null, "The club fixture must still satisfy the panel geometry");
        expect(action(store, clubCards) == null, "Club stage cards belong to the club workflow, not a dialog");
        var clubRows = List.of(word("危险等级", 240, 300), word("危险等级", 520, 300), word("危险等级", 800, 300));
        expect(action(store, clubRows) == null, "Club task rows belong to the club workflow, not a dialog");
        count += 2;

        int[] scrolledClub = panel(199, 57, 1120, 667, 0x915032);
        var visibleChapters = List.of(word("任务", 325, 300), word("任務", 793, 300));
        for (int y : new int[]{218, 667}) {
            fill(scrolledClub, 230, y, 660, y, 0xffffff);
            fill(scrolledClub, 688, y, 1118, y, 0xffffff);
        }
        expect(action(scrolledClub, List.of()).isEmpty(), "The scrolled chapter geometry still resembles a modal");
        expect(action(scrolledClub, visibleChapters) == null, "Two undimmed chapter cards remain a valid club page");
        fill(scrolledClub, 688, 667, 1118, 667, 0x777777);
        expect(action(scrolledClub, visibleChapters).isEmpty(), "Dimmed chapter borders cannot dismiss a covering dialog");
        count += 3;

        var storeWords = List.of(word("充值活动", 128, 84), word("高级通行证", 139, 157),
                word("限时钜惠", 128, 304), word("卡牌包", 116, 670),
                word("卡牌包详情", 438, 164), word("开始", 468, 596), word("免费", 764, 596));
        for (int y : new int[]{84, 157, 304}) fill(store, 120, y - 2, 135, y + 2, 0xffffff);
        expect(action(store, storeWords) == null, "Undimmed pack carousel is not an interrupting popup");
        var pricedStoreWords = List.of(storeWords.get(0), storeWords.get(1), storeWords.get(2), storeWords.get(3),
                storeWords.get(4), word("即将可用", 764, 596), word("65", 1078, 596));
        expect(action(store, pricedStoreWords) == null, "Claimed cards with cooldowns and prices remain a store, not a modal");
        count++;
        expect(action(store, List.of()).isEmpty(), "Pixel artwork alone cannot identify a safe store");
        fill(store, 120, 302, 135, 306, 0x999999);
        expect(action(store, storeWords).isEmpty(), "Two undimmed sidebar labels are insufficient");
        fill(store, 120, 82, 135, 86, 0x999999);
        fill(store, 120, 155, 135, 159, 0x999999);
        expect(action(store, storeWords).isEmpty(), "Store words behind a dimmed modal remain blocked");
        for (int y : new int[]{84, 157, 304}) fill(store, 120, y - 2, 135, y + 2, 0xffffff);
        cross(store, 879, 265, 0xffffff);
        expect("×".equals(action(store, storeWords).get("label")), "A real close control retains priority over store evidence");
        count += 5;

        // New artwork, no campaign text or footer button, and a mid-tone close glyph.
        int[] redesign = panel(212, 101, 1068, 619, 0x280b18);
        cross(redesign, 1034, 142, 0x1cb594);
        var redesignedClose = action(redesign, List.of());
        expect(redesignedClose != null && "×".equals(redesignedClose.get("label")) &&
                Math.abs((int) redesignedClose.get("x") - 1034) <= 3 && Math.abs((int) redesignedClose.get("y") - 142) <= 3,
                "An unfamiliar color scheme and a relocated panel need no OCR or campaign signature");
        cross(redesign, 1002, 143, 0x1cb594);
        expect(Map.of().equals(action(redesign, List.of())), "Ambiguous close glyphs do not authorize a tap");
        count += 2;

        int[] goals = panel(0, 0, 1279, 719, 0x122645);
        fill(goals, 1172, 106, 1220, 165, 0xf70865);
        cross(goals, 1196, 135, 0xffffff);
        var goalWords = List.of(word("成就", 154, 137), word("每日目标", 315, 137),
                word("赛季任务", 482, 137), word("领取", 1089, 360));
        PixelFrame goalsFrame = new PixelFrame(720, 1280, goals);
        var closeGoals = ModalPopup.dismiss(goalsFrame, ModalPopup.locate(goalsFrame), goalWords, 20.0 / 9);
        expect(closeGoals != null && "每日目标关闭".equals(closeGoals.get("label")) &&
                Math.abs((int) closeGoals.get("x") - 1196) <= 3, "Daily goals uses its own wide close control");
        expect(ModalPopup.dismiss(goalsFrame, ModalPopup.locate(goalsFrame), goalWords, 20.0 / 9, true) == null,
                "The configured daily-goals collector keeps its own page open");
        count += 2;

        String fixtures;
        try (var input = ModalPopupChecks.class.getResourceAsStream("/modal-popup.lua")) {
            fixtures = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (String mode : List.of("solo", "multiplayer", "rewards", "unknown", "repeatedFrame")) {
            try (LuaRuntime lua = new LuaRuntime((name, state) -> 0, List.of())) {
                lua.set("mode", mode);
                lua.load("popup-fixture", fixtures);
                lua.load("bootstrap", Files.readString(assets.resolve("lua/bootstrap.lua")));
                lua.start("test");
                for (int i = 0; i < 100; i++) if (!lua.step(i * 500L)) break;
                lua.load("verify", "assert(done,mode)");
            }
            count++;
        }
        return count;
    }

    private static Map<String, Object> action(int[] pixels, List<?> words) {
        PixelFrame frame = new PixelFrame(720, 1280, pixels);
        return ModalPopup.dismiss(frame, ModalPopup.locate(frame), words, 20.0 / 9);
    }

    private static Map<String, Object> word(String text, int x, int y) {
        return Map.of("text", text, "x", x, "y", y);
    }

    private static void expect(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }

    private static int[] panel(int left, int top, int right, int bottom, int color) {
        int[] pixels = new int[720 * 1280];
        fill(pixels, 0, 0, 1279, 719, 0x080a18);
        fill(pixels, left, top, right, bottom, color);
        return pixels;
    }

    private static void cross(int[] pixels, int x, int y, int color) {
        for (int dx = -12; dx <= 12; dx++) {
            int dy = (int) Math.round(dx * 1.25);
            fill(pixels, x + dx, y + dy - 1, x + dx, y + dy + 1, color);
            fill(pixels, x + dx, y - dy - 1, x + dx, y - dy + 1, color);
        }
    }

    private static void fill(int[] pixels, int left, int top, int right, int bottom, int color) {
        for (int x = left; x <= right; x++)
            for (int y = top; y <= bottom; y++) pixels[x * 720 + 719 - y] = color;
    }
}
