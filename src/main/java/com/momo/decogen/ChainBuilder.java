package com.momo.decogen;

import java.util.*;

public class ChainBuilder {

    /**
     * Build rainbow chains for entries.
     * Groups entries by (model + wood type), sorts by color, links each to the next.
     *
     * For entries WITH wood: groups by model+wood, chains by color
     *   e.g., all birch beds chain in rainbow order, all spruce beds chain separately
     *
     * For entries WITHOUT wood: groups by model + frame type (from material), chains by color
     *   e.g., all white-frame beds chain together, all black-frame beds chain separately
     *
     * Returns the entries in sorted order (grouped and rainbow-ordered within groups).
     */
    public static List<DecoEntry> buildRainbowChains(List<DecoEntry> entries) {
        // Group entries by model + wood/frame type
        Map<String, List<DecoEntry>> groups = new LinkedHashMap<>();

        for (DecoEntry entry : entries) {
            if (entry.getModel() == null) continue;

            // First try to get wood from name (birch, oak, etc.)
            String wood = ColorUtils.extractWood(entry.getName());

            // If no wood in name, try to get frame type from material (white, black, etc.)
            if (wood == null && entry.getMaterial() != null) {
                wood = ColorUtils.extractWoodFromMaterial(entry.getMaterial());
            }

            String key = entry.getModel() + ":" + (wood != null ? wood : "no_wood");
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
        }

        // Sort groups by key for consistent ordering
        List<String> sortedKeys = new ArrayList<>(groups.keySet());
        Collections.sort(sortedKeys);

        List<DecoEntry> result = new ArrayList<>();

        // Build chains for each group
        for (String key : sortedKeys) {
            List<DecoEntry> list = groups.get(key);

            // Sort by color (rainbow order)
            list.sort((a, b) -> {
                String colorA = ColorUtils.extractColor(a.getName());
                String colorB = ColorUtils.extractColor(b.getName());
                return Integer.compare(
                        ColorUtils.getColorIndex(colorA),
                        ColorUtils.getColorIndex(colorB)
                );
            });

            // Link each to next (circular) - only if more than 1 entry
            if (list.size() >= 2) {
                for (int i = 0; i < list.size(); i++) {
                    DecoEntry current = list.get(i);
                    DecoEntry next = list.get((i + 1) % list.size());

                    String linkTarget = getLinkTarget(next);
                    if (linkTarget != null) {
                        setModelSwitchLink(current, linkTarget);
                    }
                }
            }

            // Add sorted entries to result
            result.addAll(list);
        }

        // Add any entries that weren't in groups (no model)
        for (DecoEntry entry : entries) {
            if (entry.getModel() == null && !result.contains(entry)) {
                result.add(entry);
            }
        }

        return result;
    }

    /**
     * Build wood chains for entries.
     * Groups by (model + color), sorts by wood type, links each to next.
     *
     * e.g., red birch bed -> red oak bed -> red spruce bed -> red birch bed
     *
     * Returns the entries in sorted order.
     */
    public static List<DecoEntry> buildWoodChains(List<DecoEntry> entries) {
        // Group by model + color (only entries that have wood types)
        Map<String, List<DecoEntry>> groups = new LinkedHashMap<>();

        for (DecoEntry entry : entries) {
            if (entry.getModel() == null) continue;

            // Check for wood in name first
            String wood = ColorUtils.extractWood(entry.getName());

            // Also check material for frame type
            if (wood == null && entry.getMaterial() != null) {
                wood = ColorUtils.extractWoodFromMaterial(entry.getMaterial());
            }

            if (wood == null) continue; // Only include entries with wood/frame types

            String color = ColorUtils.extractColor(entry.getName());
            String key = entry.getModel() + ":" + (color != null ? color : "no_color");

            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
        }

        // Sort groups by key for consistent ordering
        List<String> sortedKeys = new ArrayList<>(groups.keySet());
        Collections.sort(sortedKeys);

        List<DecoEntry> result = new ArrayList<>();
        Set<DecoEntry> processed = new HashSet<>();

        // Build chains for each group
        for (String key : sortedKeys) {
            List<DecoEntry> list = groups.get(key);

            // Sort by wood order
            list.sort((a, b) -> {
                String woodA = ColorUtils.extractWood(a.getName());
                if (woodA == null) woodA = ColorUtils.extractWoodFromMaterial(a.getMaterial());

                String woodB = ColorUtils.extractWood(b.getName());
                if (woodB == null) woodB = ColorUtils.extractWoodFromMaterial(b.getMaterial());

                return Integer.compare(
                        ColorUtils.getWoodIndex(woodA),
                        ColorUtils.getWoodIndex(woodB)
                );
            });

            // Link each to next (circular) - only if more than 1 entry
            if (list.size() >= 2) {
                for (int i = 0; i < list.size(); i++) {
                    DecoEntry current = list.get(i);
                    DecoEntry next = list.get((i + 1) % list.size());

                    String linkTarget = getLinkTarget(next);
                    if (linkTarget != null) {
                        setModelSwitchLink(current, linkTarget);
                    }
                }
            }

            // Add sorted entries to result
            result.addAll(list);
            processed.addAll(list);
        }

        // Add entries without wood types at the end (unchanged)
        for (DecoEntry entry : entries) {
            if (!processed.contains(entry)) {
                result.add(entry);
            }
        }

        return result;
    }

    /**
     * Get the link target for an entry (decoref if present, otherwise material)
     */
    private static String getLinkTarget(DecoEntry entry) {
        if (entry.getDecoref() != null && !entry.getDecoref().isEmpty()) {
            return entry.getDecoref();
        }
        return entry.getMaterial();
    }

    /**
     * Set the tool_modelswitch link on an entry's script
     */
    private static void setModelSwitchLink(DecoEntry entry, String link) {
        Script script = entry.getScript();
        if (script == null) {
            script = new Script();
            entry.setScript(script);
        }
        script.setToolModelSwitchLink(link);
    }
}
