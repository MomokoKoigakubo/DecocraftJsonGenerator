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
     * Find a non-icon texture whose name ends with the given suffix.
     * Prefers unmatched textures, then falls back to any texture that's not an icon.
     */
    public static String findMatchingTextureForSuffix(String suffix,
                                                      List<String> unmatchedTextures,
                                                      Set<String> allTextureNames,
                                                      Set<String> iconNames) {
        String suffixLower = "_" + suffix.toLowerCase();

        for (String textureName : unmatchedTextures) {
            if (textureName.toLowerCase().endsWith(suffixLower)) {
                return textureName;
            }
        }

        for (String textureName : allTextureNames) {
            if (textureName.toLowerCase().endsWith(suffixLower) && !iconNames.contains(textureName)) {
                return textureName;
            }
        }

        return null;
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
        String lower = textureName.toLowerCase();

        String[] knownColors = {
                "light_blue", "light_gray", "dark_gray", "ocean_blue",
                "red", "orange", "yellow", "lime", "green", "cyan", "blue",
                "purple", "magenta", "pink", "brown", "white", "cream",
                "gray", "black"
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