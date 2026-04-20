package com.momo.decogen.logic;

import com.momo.decogen.model.DecoEntry;

import java.util.Comparator;
import java.util.List;

public class ColorUtils {

    public static final List<String> RAINBOW = List.of(
            "red", "orange", "yellow", "lime", "green", "cyan", "light_blue", "blue", "ocean_blue",
            "purple", "magenta", "pink", "brown", "white", "cream", "light_gray", "gray", "dark_gray", "black"
    );

    // Wood types that appear in NAMES (not including white/black which are ambiguous with colors)
    public static final List<String> WOOD = List.of(
            "birch", "oak", "cherry", "palm", "spruce", "ebony"
    );

    // Full wood list including frame colors (for material-based detection)
    public static final List<String> WOOD_FRAMES = List.of(
            "birch", "oak", "cherry", "palm", "spruce", "ebony", "white", "black"
    );

    public static final List<String> LEATHER = List.of(
            "leather_black", "leather_brown"
    );

    public static String extractColor(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();

        for (String leather : LEATHER) {
            if (lower.contains(leather.replace("_", " ")) || lower.contains(leather)) {
                return leather;
            }
        }

        if (lower.contains("light blue")) return "light_blue";
        if (lower.contains("ocean blue")) return "ocean_blue";
        if (lower.contains("light gray") || lower.contains("light grey")) return "light_gray";
        if (lower.contains("dark gray") || lower.contains("dark grey")) return "dark_gray";

        String[] parts = lower.split("\\s+");
        for (int i = parts.length - 1; i >= 0; i--) {
            String word = parts[i];
            if (RAINBOW.contains(word)) {
                return word;
            }
        }

        return null;
    }

    public static String extractWood(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();

        for (String wood : WOOD) {
            if (lower.contains(wood)) {
                return wood;
            }
        }

        return null;
    }

    public static String extractWoodFromMaterial(String material) {
        if (material == null) return null;
        String lower = material.toLowerCase();
        String[] parts = lower.split("_");
        if (parts.length == 0) return null;

        String lastPart = parts[parts.length - 1];
        boolean lastIsColor = RAINBOW.contains(lastPart) || LEATHER.contains(lastPart);

        // wood + color pattern: e.g. "bed_set_birch_red" -> "birch"
        if (lastIsColor && parts.length >= 2) {
            String beforeLast = parts[parts.length - 2];
            if (WOOD_FRAMES.contains(beforeLast)) return beforeLast;
        }

        // pure wood pattern: e.g. "closet_white" or "closet_spruce" -> "white" / "spruce"
        if (WOOD_FRAMES.contains(lastPart)) return lastPart;

        // fallback: wood buried deeper in the name
        for (int i = parts.length - 2; i >= 0; i--) {
            if (WOOD_FRAMES.contains(parts[i])) return parts[i];
        }

        return null;
    }

    public static int getColorIndex(String color) {
        if (color == null) return 999;

        int idx = RAINBOW.indexOf(color);
        if (idx >= 0) return idx;

        idx = LEATHER.indexOf(color);
        if (idx >= 0) return 100 + idx;

        return 999;
    }

    public static int getWoodIndex(String wood) {
        if (wood == null) return 999;
        int idx = WOOD_FRAMES.indexOf(wood);
        return idx >= 0 ? idx : 999;
    }

    public static Comparator<DecoEntry> rainbowComparator() {
        return (a, b) -> {
            int modelCompare = a.getModel().compareToIgnoreCase(b.getModel());
            if (modelCompare != 0) return modelCompare;

            String colorA = extractColor(a.getName());
            String colorB = extractColor(b.getName());

            return Integer.compare(getColorIndex(colorA), getColorIndex(colorB));
        };
    }
}