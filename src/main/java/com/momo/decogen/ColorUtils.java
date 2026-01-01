package com.momo.decogen;

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

    /**
     * Extract color token from a name
     * e.g., "Couch Red" → "red", "Table Light Blue" → "light_blue"
     */
    public static String extractColor(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();

        // Check leather first
        for (String leather : LEATHER) {
            if (lower.contains(leather.replace("_", " ")) || lower.contains(leather)) {
                return leather;
            }
        }

        // Check multi-word colors (must check these before single words)
        if (lower.contains("light blue")) return "light_blue";
        if (lower.contains("ocean blue")) return "ocean_blue";
        if (lower.contains("light gray") || lower.contains("light grey")) return "light_gray";
        if (lower.contains("dark gray") || lower.contains("dark grey")) return "dark_gray";

        // Check single-word colors - find the LAST color word in the name
        String[] parts = lower.split("\\s+");
        for (int i = parts.length - 1; i >= 0; i--) {
            String word = parts[i];
            if (RAINBOW.contains(word)) {
                return word;
            }
        }

        return null;
    }

    /**
     * Extract wood type from a name.
     * Only detects actual wood types (birch, oak, etc.), not white/black which are colors.
     */
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

    /**
     * Extract wood frame type from material name.
     * Material format is typically: prefix_frametype_color (e.g., "bed_set_birch_red")
     * This extracts the frame/wood type which appears BEFORE the color.
     */
    public static String extractWoodFromMaterial(String material) {
        if (material == null) return null;
        String lower = material.toLowerCase();
        String[] parts = lower.split("_");

        // Look for wood/frame type in the middle segments (not the last one, which is color)
        // Skip first segment(s) that are prefixes like "bed", "set", etc.
        for (int i = 0; i < parts.length - 1; i++) {  // -1 to skip last segment (color)
            String part = parts[i];
            if (WOOD_FRAMES.contains(part)) {
                return part;
            }
        }

        return null;
    }

    /**
     * Get sort index for a color (lower = earlier in rainbow)
     */
    public static int getColorIndex(String color) {
        if (color == null) return 999;

        int idx = RAINBOW.indexOf(color);
        if (idx >= 0) return idx;

        idx = LEATHER.indexOf(color);
        if (idx >= 0) return 100 + idx;  // Leather after rainbow

        return 999;
    }

    /**
     * Get sort index for wood type
     */
    public static int getWoodIndex(String wood) {
        if (wood == null) return 999;
        int idx = WOOD_FRAMES.indexOf(wood);
        return idx >= 0 ? idx : 999;
    }

    /**
     * Comparator to sort entries by rainbow order
     */
    public static Comparator<DecoEntry> rainbowComparator() {
        return (a, b) -> {
            // First by model name
            int modelCompare = a.getModel().compareToIgnoreCase(b.getModel());
            if (modelCompare != 0) return modelCompare;

            // Then by color index
            String colorA = extractColor(a.getName());
            String colorB = extractColor(b.getName());

            return Integer.compare(getColorIndex(colorA), getColorIndex(colorB));
        };
    }
}
