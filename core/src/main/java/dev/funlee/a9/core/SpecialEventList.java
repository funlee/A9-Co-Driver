package dev.funlee.a9.core;

import java.util.ArrayList;
import java.util.Arrays;
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
        boolean[] tileRows = new boolean[bottom + 1];
        for (int y = top; y <= bottom; y++) tileRows[y] = tileRow(frame, y);
        int sectionTop = top;
        for (int y = top; y <= bottom; y++) {
            if (!separatorRow(frame, y)) continue;
            int gap = y;
            while (y < bottom && separatorRow(frame, y + 1)) y++;
            if (y - gap + 1 < 5) continue;
            restoreTile(tileRows, sectionTop, gap, fullHeight);
            Arrays.fill(tileRows, gap, y + 1, false);
            sectionTop = y + 1;
        }
        if (sectionTop > top) restoreTile(tileRows, sectionTop, bottom + 1, fullHeight);
        List<Map<String, Object>> cards = new ArrayList<>();
        int start = -1;
        int last = -1;
        for (int y = top; y <= bottom + 6; y++) {
            if (y <= bottom && tileRows[y]) {
                if (start == -1) start = y;
                last = y;
            } else if (start >= 0 && y - last == 6) {
                int height = last - start + 1;
                // A lower clipped card keeps its top, whose visible portion can be tapped.
                boolean atLowerEdge = last >= bottom - 8;
                boolean complete = height >= fullHeight - (atLowerEdge ? 12 : 28);
                if (complete || (atLowerEdge && start > top + 15 && height >= 42)) {
                    List<Integer> artwork = new ArrayList<>();
                    // A clipped tile can still prove overlap by its visible top rows.
                    for (int dy : new int[]{14, 27, 51, 77, 111}) {
                        if (dy >= height) break;
                        for (int x = 92; x <= 332; x += 40) artwork.add(pixel(frame, x, start + dy));
                    }
                    cards.add(Map.of("x", 220, "y", (start + last) / 2, "top", start,
                            "complete", complete, "artwork", artwork));
                }
                start = -1;
            }
        }
        for (int i = 1; i < cards.size(); i++) {
            int spacing = (int) cards.get(i).get("top") - (int) cards.get(i - 1).get("top");
            if (spacing < fullHeight - 16 || spacing > fullHeight + 30) return Map.of();
        }
        boolean aligned = !cards.isEmpty() && Boolean.TRUE.equals(cards.get(0).get("complete"))
                && (int) cards.get(0).get("top") <= top + 18;
        int scrollDistance = 0;
        for (Map<String, Object> card : cards) {
            int distance = (int) card.get("top") - top;
            if (distance > 18) {
                scrollDistance = distance;
                break;
            }
        }
        return Map.of("aligned", aligned, "cards", cards, "scrollDistance", scrollDistance);
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

    /** The preview's lime action button can still be located during its animated text highlight. */
    public static Map<String, Integer> entryButton(PixelFrame frame) {
        if (!activeTab(frame)) return Map.of();
        int top = -1;
        // The left inset remains filled even when the highlight crosses the caption.
        for (int y = 450; y <= 575; y++) {
            if (y < 575 && buttonFill(pixel(frame, 1040, y))) {
                if (top < 0) top = y;
                continue;
            }
            if (top < 0) continue;
            int height = y - top;
            if (height >= 40 && height <= 78) {
                int center = top + height / 2;
                int left = -1, right = -1, filled = 0;
                for (int x = 942; x <= 1220; x++) {
                    int hits = 0;
                    if (buttonFill(pixel(frame, x, top + 7))) hits++;
                    if (buttonFill(pixel(frame, x, center))) hits++;
                    if (buttonFill(pixel(frame, x, y - 8))) hits++;
                    if (hits < 2) continue;
                    if (left < 0) left = x;
                    right = x;
                    filled++;
                }
                int width = right - left + 1;
                if (width >= 145 && width <= 210 && filled * 2 >= width &&
                        left >= 975 && right >= 1160 && right <= 1210) {
                    return Map.of("x", (left + right) / 2, "y", center);
                }
            }
            top = -1;
        }
        return Map.of();
    }

    private static boolean buttonFill(int color) {
        int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
        return r >= 150 && r <= 228 && g >= 225 && g >= r + 20 && b <= 85;
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

    // The selector tracks the purple gap (BGR 360A17 ± 09). Sample across
    // the rail because its left edge moves on wide displays and artwork can share the color.
    private static boolean separatorRow(PixelFrame frame, int y) {
        int hits = 0;
        for (int x = 100; x <= 345; x += 35) {
            int color = pixel(frame, x, y);
            if (Math.abs((color >> 16) - 0x17) <= 9 && Math.abs((color >> 8 & 255) - 0x0a) <= 9
                    && Math.abs((color & 255) - 0x36) <= 9) hits++;
        }
        return hits >= 6;
    }

    private static void restoreTile(boolean[] rows, int start, int end, int fullHeight) {
        // Only bridge dark artwork within a single card bounded by a separator. A long
        // section may contain another gap in a different shade, so retain its measured rows.
        if (end - start > fullHeight + 6) return;
        int visible = 0;
        for (int y = start; y < end; y++) if (rows[y]) visible++;
        if (visible >= 8) Arrays.fill(rows, start, end, true);
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
