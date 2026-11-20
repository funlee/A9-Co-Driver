package dev.funlee.a9.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

final class SpecialEventListChecks {
    static int run(Path assets) throws Exception {
        checkEntryButton();
        int[] origin = seasonTab();
        tile(origin, 88, 268, 0x16a3c0);
        tile(origin, 281, 477, 0x635451);
        tile(origin, 490, 572, 0xb90010);
        Map<String, Object> top = SpecialEventList.read(new PixelFrame(720, 1280, origin), false);
        List<?> cards = (List<?>) top.get("cards");
        if (!Boolean.TRUE.equals(top.get("aligned")) || cards.size() != 3 ||
                !((Map<?, ?>) cards.get(2)).get("y").equals(531)) {
            throw new AssertionError("The lower clipped third card has a visible tap target");
        }

        int[] shifted = seasonTab();
        tile(shifted, 73, 95, 0x16a3c0);
        tile(shifted, 108, 303, 0x635451);
        tile(shifted, 316, 511, 0xb90010);
        for (int y = 96; y < 108; y++) shifted[419 * 720 + 719 - y] = 0x243d50;
        Map<String, Object> offset = SpecialEventList.read(new PixelFrame(720, 1280, shifted), false);
        if (!Boolean.FALSE.equals(offset.get("aligned")) || ((List<?>) offset.get("cards")).size() != 2) {
            throw new AssertionError("The first complete card on the shifted rail is not position 1");
        }
        int[] wide = seasonTab();
        tile(wide, 92, 331, 0x16a3c0);
        tile(wide, 347, 551, 0x635451);
        Map<String, Object> widePage = SpecialEventList.read(new PixelFrame(720, 1280, wide), true);
        List<?> wideCards = (List<?>) widePage.get("cards");
        if (!Boolean.TRUE.equals(widePage.get("aligned")) || wideCards.size() != 2 ||
                !Boolean.FALSE.equals(((Map<?, ?>) wideCards.get(1)).get("complete")) ||
                ((List<?>) ((Map<?, ?>) wideCards.get(1)).get("artwork")).size() < 14) {
            throw new AssertionError("A clipped second wide tile retains its visible artwork for scroll matching");
        }
        // The reported wide MISTRAL card starts with low-contrast lettering. Its
        // first strongly contrasting image row is 43 px below the visible top.
        int[] dark = seasonTab();
        tile(dark, 92, 134, 0x090913);
        tile(dark, 135, 332, 0x4e9a81);
        tile(dark, 333, 345, 0x170a36);
        tile(dark, 346, 558, 0x42383c);
        Map<String, Object> darkPage = SpecialEventList.read(new PixelFrame(720, 1280, dark), true);
        List<?> darkCards = (List<?>) darkPage.get("cards");
        if (!Boolean.TRUE.equals(darkPage.get("aligned")) || darkCards.size() != 2
                || !((Map<?, ?>) darkCards.get(0)).get("top").equals(92)
                || !Boolean.FALSE.equals(((Map<?, ?>) darkCards.get(1)).get("complete"))
                || !darkPage.get("scrollDistance").equals(254)) {
            throw new AssertionError("The shared purple separator anchors a dark first card and a one-card drag");
        }
        fill(dark, 0, 346, 1279, 558, 0x170a36);
        Map<String, Object> lastPage = SpecialEventList.read(new PixelFrame(720, 1280, dark), true);
        if (((List<?>) lastPage.get("cards")).size() != 1 || !lastPage.get("scrollDistance").equals(0)) {
            throw new AssertionError("Empty rail below the last card cannot supply another position");
        }
        if (!SpecialEventList.read(new PixelFrame(720, 1280, new int[720 * 1280]), false).isEmpty()) {
            throw new AssertionError("Only the selected Season Events tab can expose the special list");
        }

        int[] stage = new int[720 * 1280];
        java.util.Arrays.fill(stage, 0x040d19);
        fill(stage, 55, 210, 274, 305, 0xffffff);
        fill(stage, 282, 210, 287, 305, 0xff0054);
        fill(stage, 315, 210, 944, 674, 0x9d1015);
        if (!SpecialEventList.stageRail(new PixelFrame(720, 1280, stage)).equals(
                Map.of("x", 287, "y", 258, "clear", true))) {
            throw new AssertionError("A full stage card leaves a clear chapter inset");
        }
        fill(stage, 291, 210, 315, 674, 0xcbd0d7);
        if (!Boolean.FALSE.equals(SpecialEventList.stageRail(new PixelFrame(720, 1280, stage)).get("clear"))) {
            throw new AssertionError("Artwork touching the rail indicates a clipped stage card");
        }

        String utilities = Files.readString(assets.resolve("lua/utilities.lua"));
        String fixture;
        try (var input = SpecialEventListChecks.class.getResourceAsStream("/special-event-list.lua")) {
            fixture = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        for (String scenario : List.of("first", "third", "wideThird", "fourth", "noOrigin", "wrongPreview",
                "directChapter", "directLeftLabels", "directRanking", "pixelButton", "retry", "stale", "clamped",
                "skip", "wrongTab", "wrongRankStage")) {
            try (LuaRuntime lua = new LuaRuntime((name, state) -> 0, List.of())) {
                lua.set("scenario", scenario);
                lua.load("utilities", utilities);
                lua.load("special-entry", fixture);
                lua.start("test");
                for (int i = 0; i < 260; i++) if (!lua.step(i * 250L)) break;
                lua.load("verify", "assert(done,scenario)");
            }
        }
        try (var input = SpecialEventListChecks.class.getResourceAsStream("/special-stages.lua")) {
            fixture = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        for (String scenario : List.of("first", "third", "focused", "noisy-first", "stuck", "wrong-stage")) {
            try (LuaRuntime lua = new LuaRuntime((name, state) -> 0, List.of())) {
                lua.set("scenario", scenario);
                lua.load("utilities", utilities);
                lua.load("stage-fixture", fixture);
                lua.start("test");
                for (int i = 0; i < 420; i++) if (!lua.step(i * 250L)) break;
                lua.load("verify", "assert(done,scenario)");
            }
        }
        return 32;
    }

    private static void checkEntryButton() {
        int[] preview = seasonTab();
        fill(preview, 1010, 482, 1186, 545, 0xc3fb12);
        fill(preview, 1073, 498, 1120, 529, 0x192330);
        if (!Map.of("x", 1098, "y", 514).equals(SpecialEventList.entryButton(new PixelFrame(720, 1280, preview)))) {
            throw new AssertionError("Locate the wide preview button around its dark caption");
        }
        int[] otherTab = new int[720 * 1280];
        fill(otherTab, 1010, 482, 1186, 545, 0xc3fb12);
        if (!SpecialEventList.entryButton(new PixelFrame(720, 1280, otherTab)).isEmpty()) {
            throw new AssertionError("A matching button outside Season Events cannot enter a special event");
        }
        fill(preview, 1060, 482, 1186, 545, 0);
        if (!SpecialEventList.entryButton(new PixelFrame(720, 1280, preview)).isEmpty()) {
            throw new AssertionError("A small green patch in the preview cannot serve as its action button");
        }
    }

    private static int[] seasonTab() {
        int[] frame = new int[720 * 1280];
        for (int x : new int[]{310, 330, 350}) frame[x * 720 + 719 - 636] = 0xffffff;
        for (int x : new int[]{310, 350}) frame[x * 720 + 719 - 605] = 0xff0054;
        return frame;
    }

    private static void tile(int[] frame, int top, int bottom, int color) {
        fill(frame, 52, top, 408, bottom, color);
    }

    private static void fill(int[] frame, int left, int top, int right, int bottom, int color) {
        for (int x = left; x <= right; x++) {
            for (int y = top; y <= bottom; y++) frame[x * 720 + 719 - y] = color;
        }
    }
}
