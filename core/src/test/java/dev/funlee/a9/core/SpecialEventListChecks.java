package dev.funlee.a9.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

final class SpecialEventListChecks {
    static int run(Path assets) throws Exception {
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
        if (!SpecialEventList.read(new PixelFrame(720, 1280, new int[720 * 1280]), false).isEmpty()) {
            throw new AssertionError("Only the selected Season Events tab can expose the special list");
        }

        String utilities = Files.readString(assets.resolve("lua/utilities.lua"));
        String fixture;
        try (var input = SpecialEventListChecks.class.getResourceAsStream("/special-event-list.lua")) {
            fixture = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        for (String scenario : List.of("first", "third", "fourth", "noOrigin", "wrongPreview")) {
            try (LuaRuntime lua = new LuaRuntime((name, state) -> 0, List.of())) {
                lua.set("scenario", scenario);
                lua.load("utilities", utilities);
                lua.load("special-entry", fixture);
                lua.start("test");
                for (int i = 0; i < 260; i++) if (!lua.step(i * 250L)) break;
                lua.load("verify", "assert(done,scenario)");
            }
        }
        return 8;
    }

    private static int[] seasonTab() {
        int[] frame = new int[720 * 1280];
        for (int x : new int[]{310, 330, 350}) frame[x * 720 + 719 - 636] = 0xffffff;
        for (int x : new int[]{310, 350}) frame[x * 720 + 719 - 605] = 0xff0054;
        return frame;
    }

    private static void tile(int[] frame, int top, int bottom, int color) {
        for (int x = 52; x <= 408; x++) {
            for (int y = top; y <= bottom; y++) frame[x * 720 + 719 - y] = color;
        }
    }
}
