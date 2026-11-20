package dev.funlee.a9.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Visible tiles in the vertical rail of the selected Season Events tab. */
public final class SpecialEventList {
    private SpecialEventList() {}

    public static Map<String, Object> read(PixelFrame frame, boolean wide) {
        if (!activeTab(frame)) return Map.of();
        int top = wide ? 92 : 73;
        int bottom = wide ? 558 : 572;
        int fullHeight = wide ? 240 : 196;
        List<Map<String, Object>> cards = new ArrayList<>();
        int start = -1;
        int last = -1;
        for (int y = top; y <= bottom + 6; y++) {
            if (y <= bottom && tileRow(frame, y)) {
                if (start == -1) start = y;
                last = y;
            } else if (start >= 0 && y - last == 6) {
                int height = last - start + 1;
                // A lower clipped card keeps its top, whose visible portion can be tapped.
                boolean complete = height >= fullHeight - 28;
                if (complete || (last >= bottom - 5 && start > top + 15 && height >= 42)) {
                    List<Integer> artwork = new ArrayList<>();
                    if (complete) {
                        for (int dy : new int[]{27, 51, 77, 111}) {
                            for (int x = 92; x <= 332; x += 40) artwork.add(pixel(frame, x, start + dy));
                        }
                    }
                    cards.add(Map.of("x", 220, "y", (start + last) / 2, "top", start, "artwork", artwork));
                }
                start = -1;
            }
        }
        for (int i = 1; i < cards.size(); i++) {
            int spacing = (int) cards.get(i).get("top") - (int) cards.get(i - 1).get("top");
            if (spacing < fullHeight - 16 || spacing > fullHeight + 30) return Map.of();
        }
        boolean aligned = !cards.isEmpty() && !((List<?>)cards.get(0).get("artwork")).isEmpty()
                && (int) cards.get(0).get("top") <= top + 18;
        return Map.of("aligned", aligned, "cards", cards);
    }

    public static boolean activeTab(PixelFrame frame) {
        for (int x : new int[]{310, 330, 350}) {
            int fill = pixel(frame, x, 636);
            if ((fill >> 16 & 255) < 112 || (fill >> 8 & 255) < 218 || (fill & 255) < 234) return false;
        }
        for (int y = 584; y <= 610; y++) {
            if (pink(pixel(frame, 310, y)) && pink(pixel(frame, 350, y))) return true;
        }
        return false;
    }

    /** Find the selected chapter's rail and the clear inset next to an unclipped stage card. */
    public static Map<String, Object> stageRail(PixelFrame frame) {
        int railX = -1, railY = -1;
        for (int x = 210; x <= 365; x++) {
            int start = -1;
            for (int y = 170; y <= 660; y++) {
                if (y < 660 && pink(pixel(frame, x, y))) {
                    if (start < 0) start = y;
                } else if (start >= 0) {
                    int height = y - start, center = start + height / 2;
                    if (height >= 48 && height <= 138 && selectedChapter(frame, x, center)) {
                        railX = x;
                        railY = center;
                    }
                    start = -1;
                }
            }
        }
        if (railX < 0) return Map.of();
        int dark = 0;
        for (int dx : new int[]{8, 14}) for (int y : new int[]{224, 245, 275, 315, 360, 410, 467, 522, 585, 644}) {
            int color = pixel(frame, railX + dx, y);
            int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
            if (r <= 43 && g <= 61 && b <= 100 && b >= r + 6) dark++;
        }
        return Map.of("x", railX, "y", railY, "clear", dark >= 18);
    }

    private static boolean selectedChapter(PixelFrame frame, int x, int y) {
        int white = 0;
        for (int dx : new int[]{32, 58}) for (int dy : new int[]{-22, 22}) {
            int color = pixel(frame, x - dx, y + dy);
            if ((color >> 16 & 255) >= 220 && (color >> 8 & 255) >= 220 && (color & 255) >= 220) white++;
        }
        return white >= 3;
    }

    private static boolean pink(int color) {
        return (color >> 16 & 255) > 175 && (color >> 8 & 255) < 62 && (color & 255) >= 55 && (color & 255) <= 120;
    }

    private static boolean tileRow(PixelFrame frame, int y) {
        int left = pixel(frame, 46, y);
        int right = pixel(frame, 419, y);
        int hits = 0;
        for (int x = 85; x <= 330; x += 11) {
            int color = pixel(frame, x, y);
            if (colorDistance(color, left) > 44 && colorDistance(color, right) > 44) hits++;
        }
        return hits >= 12;
    }

    private static int colorDistance(int a, int b) {
        return Math.abs((a >> 16 & 255) - (b >> 16 & 255))
                + Math.abs((a >> 8 & 255) - (b >> 8 & 255)) + Math.abs((a & 255) - (b & 255));
    }

    private static int pixel(PixelFrame frame, int x, int y) {
        return frame.pixel(719 - y, x) & 0xffffff;
    }
}
