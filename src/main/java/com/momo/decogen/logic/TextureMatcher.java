package com.momo.decogen.logic;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Pure matching algorithms for connecting textures, icons, and models
 * by shared name prefixes/suffixes.
 */
public class TextureMatcher {

    /**
     * Find the model whose name is the longest prefix of the given texture/icon name.
     * Matches on exact equality, "name_" prefix, or "name-" prefix (case-insensitive).
     */
    public static String findMatchingModel(String textureName, Collection<String> modelNames) {
        String texLower = textureName.toLowerCase();

        String bestMatch = null;
        int bestLength = 0;

        for (String modelName : modelNames) {
            String modelLower = modelName.toLowerCase();

            if (texLower.equals(modelLower) ||
                    texLower.startsWith(modelLower + "_") ||
                    texLower.startsWith(modelLower + "-")) {

                if (modelName.length() > bestLength) {
                    bestMatch = modelName;
                    bestLength = modelName.length();
                }
            }
        }

        return bestMatch;
    }

    /**
     * True if the texture name matches the model name exactly or with a "name_"/"name-" prefix.
     */
    public static boolean textureMatchesModel(String textureName, String modelName) {
        if (modelName == null) return false;
        String texLower = textureName.toLowerCase();
        String modelLower = modelName.toLowerCase();

        if (texLower.equals(modelLower)) return true;
        if (texLower.startsWith(modelLower + "_")) return true;
        if (texLower.startsWith(modelLower + "-")) return true;

        return false;
    }

    /**
     * Find an icon texture named {model}_{suffix}, with fallback to the trailing
     * part of a multi-part suffix (e.g. "palm_pink" falls back to "pink").
     */
    public static String findMatchingIcon(String modelName, String suffix, Set<String> textureNames) {
        String exactMatch = modelName + "_" + suffix;
        if (textureNames.contains(exactMatch)) {
            return exactMatch;
        }

        String lowerExact = exactMatch.toLowerCase();
        for (String texName : textureNames) {
            if (texName.toLowerCase().equals(lowerExact)) {
                return texName;
            }
        }

        if (suffix.contains("_")) {
            String[] suffixParts = suffix.split("_");
            String lastPart = suffixParts[suffixParts.length - 1];
            String partialMatch = modelName + "_" + lastPart;

            if (textureNames.contains(partialMatch)) {
                return partialMatch;
            }

            String lowerPartial = partialMatch.toLowerCase();
            for (String texName : textureNames) {
                if (texName.toLowerCase().equals(lowerPartial)) {
                    return texName;
                }
            }
        }

        return null;
    }

    /**
     * Find the block texture that belongs to this exact icon: one whose name
     * equals the icon name, or the icon name plus a trailing "_flipbook".
     * This keeps a "candle_holder_4_black" icon bound to
     * "candle_holder_4_black_flipbook" instead of grabbing another model's
     * "candle_holder_1_black_flipbook" via a loose color-suffix match.
     * Prefers unmatched textures, then any non-icon texture.
     */
    public static String findExactTextureForIcon(String iconName,
                                                 List<String> unmatchedTextures,
                                                 Set<String> allTextureNames,
                                                 Set<String> iconNames) {
        String want = iconName.toLowerCase();

        for (String textureName : unmatchedTextures) {
            if (stripFlipbook(textureName.toLowerCase()).equals(want)) {
                return textureName;
            }
        }

        for (String textureName : allTextureNames) {
            if (iconNames.contains(textureName)) continue;
            if (stripFlipbook(textureName.toLowerCase()).equals(want)) {
                return textureName;
            }
        }

        return null;
    }

    /**
     * Find a non-icon texture whose name ends with the given suffix.
     * Prefers unmatched textures, then falls back to any texture that's not an icon.
     *
     * A trailing "_flipbook" token on the texture name is ignored when
     * comparing, so an icon "candle_holder_4_black" pairs with the animated
     * block texture "candle_holder_4_black_flipbook".
     */
    public static String findMatchingTextureForSuffix(String suffix,
                                                      List<String> unmatchedTextures,
                                                      Set<String> allTextureNames,
                                                      Set<String> iconNames) {
        String suffixLower = "_" + suffix.toLowerCase();

        for (String textureName : unmatchedTextures) {
            if (stripFlipbook(textureName.toLowerCase()).endsWith(suffixLower)) {
                return textureName;
            }
        }

        for (String textureName : allTextureNames) {
            if (stripFlipbook(textureName.toLowerCase()).endsWith(suffixLower) && !iconNames.contains(textureName)) {
                return textureName;
            }
        }

        return null;
    }

    /**
     * Strip a single trailing "_flipbook" token from a (lower-cased) name.
     * Decocraft animated block textures are named "{base}_flipbook"; that
     * token is not part of the color/variant identity, so it must be removed
     * before matching a texture against an icon or extracting its color.
     */
    public static String stripFlipbook(String lower) {
        if (lower.endsWith("_flipbook")) {
            return lower.substring(0, lower.length() - "_flipbook".length());
        }
        return lower;
    }

    /**
     * Extract the color/variant suffix from a texture name.
     * Examples:
     *   "bed_set_red"        → "red"
     *   "bed_set_light_blue" → "light_blue"
     *   "bed_set_white_red"  → "white_red"
     *   "bed_set_palm_pink"  → "palm_pink"
     */
    public static String extractColorSuffix(String textureName) {
        // "candle_holder_4_black_flipbook" → color is "black", not the whole
        // name. The animated-block "_flipbook" token is not part of the color.
        String lower = stripFlipbook(textureName.toLowerCase());

        // Leather finishes ("leather_black", "leather_brown") must be checked
        // before the bare colour loop, otherwise "couch_set_leather_black" is
        // truncated to "black" and collides with a plain "couch_set_black" —
        // two different materials producing the same decoref. Sourced from
        // ColorUtils.LEATHER so the matcher stays in sync with the rest of the
        // app's colour handling.
        for (String leather : ColorUtils.LEATHER) {
            if (lower.endsWith(leather)) {
                return leather;
            }
        }

        // Multi-token finishes ("rose_gold") must precede their single-token
        // suffix ("gold") so "..._rose_gold" isn't truncated to "gold".
        String[] knownColors = {
                "light_blue", "light_gray", "dark_gray", "ocean_blue", "rose_gold",
                "red", "orange", "yellow", "lime", "green", "cyan", "blue",
                "purple", "magenta", "pink", "brown", "white", "cream",
                "gray", "black", "gold", "silver", "brass", "bronze", "copper"
        };

        String[] woodsAndFrames = {"birch", "oak", "cherry", "palm", "spruce", "ebony", "white", "black"};

        for (String woodOrFrame : woodsAndFrames) {
            for (String color : knownColors) {
                String combo = woodOrFrame + "_" + color;
                if (lower.endsWith(combo)) {
                    return combo;
                }
            }
            for (String color : new String[]{"light_blue", "light_gray", "dark_gray", "ocean_blue"}) {
                String combo = woodOrFrame + "_" + color;
                if (lower.endsWith(combo)) {
                    return combo;
                }
            }
        }

        for (String color : knownColors) {
            if (lower.endsWith("_" + color)) {
                return color;
            }
        }

        // No known color/wood suffix — use the full texture name so each
        // texture produces a unique decoref. Trimming to the trailing
        // segment(s) collapses unrelated names: e.g. north_korea/south_korea
        // would both become "korea", and every *_islands country flag would
        // collide on "islands".
        return textureName;
    }
}