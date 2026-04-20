package com.momo.decogen.logic;

import com.momo.decogen.model.DecoEntry;
import com.momo.decogen.model.Script;

import java.util.*;

public class ChainBuilder {

    /**
     * Build rainbow chains for entries.
     * Groups entries by (model + wood type), sorts by color, links each to the next.
     */
    public static List<DecoEntry> buildRainbowChains(List<DecoEntry> entries) {
        // First pass: which models actually have real wood variants (birch,
        // oak, cherry, palm, spruce, ebony)? For those models, "white"/"black"
        // behave as woods. For models with no real woods, "white"/"black" are
        // rainbow colors and should chain with the other color variants.
        Set<String> modelsWithRealWood = new HashSet<>();
        for (DecoEntry entry : entries) {
            if (entry.getModel() == null) continue;
            if (hasRealWood(entry)) modelsWithRealWood.add(entry.getModel());
        }

        Map<String, List<DecoEntry>> groups = new LinkedHashMap<>();

        for (DecoEntry entry : entries) {
            if (entry.getModel() == null) continue;

            String wood = null;
            if (modelsWithRealWood.contains(entry.getModel())) {
                wood = ColorUtils.extractWood(entry.getName());
                if (wood == null && entry.getMaterial() != null) {
                    wood = ColorUtils.extractWoodFromMaterial(entry.getMaterial());
                }
            }

            String key = entry.getModel() + ":" + (wood != null ? wood : "no_wood");
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
        }

        List<String> sortedKeys = new ArrayList<>(groups.keySet());
        Collections.sort(sortedKeys);

        List<DecoEntry> result = new ArrayList<>();

        for (String key : sortedKeys) {
            List<DecoEntry> list = groups.get(key);

            list.sort((a, b) -> {
                String colorA = ColorUtils.extractColor(a.getName());
                String colorB = ColorUtils.extractColor(b.getName());
                return Integer.compare(
                        ColorUtils.getColorIndex(colorA),
                        ColorUtils.getColorIndex(colorB)
                );
            });

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

            result.addAll(list);
        }

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
     */
    public static List<DecoEntry> buildWoodChains(List<DecoEntry> entries) {
        Map<String, List<DecoEntry>> groups = new LinkedHashMap<>();

        for (DecoEntry entry : entries) {
            if (entry.getModel() == null) continue;

            String wood = ColorUtils.extractWood(entry.getName());

            if (wood == null && entry.getMaterial() != null) {
                wood = ColorUtils.extractWoodFromMaterial(entry.getMaterial());
            }

            if (wood == null) continue;

            String color = ColorUtils.extractColor(entry.getName());
            // "Closet White" parses as wood=white AND color=white. For pure
            // wood-only entries that collision would split each wood into its
            // own singleton group. Treat color as absent when it mirrors wood.
            if (color != null && color.equals(wood)) color = null;

            String key = entry.getModel() + ":" + (color != null ? color : "no_color");

            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
        }

        List<String> sortedKeys = new ArrayList<>(groups.keySet());
        Collections.sort(sortedKeys);

        List<DecoEntry> result = new ArrayList<>();
        Set<DecoEntry> processed = new HashSet<>();

        for (String key : sortedKeys) {
            List<DecoEntry> list = groups.get(key);

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

            result.addAll(list);
            processed.addAll(list);
        }

        for (DecoEntry entry : entries) {
            if (!processed.contains(entry)) {
                result.add(entry);
            }
        }

        return result;
    }

    /**
     * Link entries in the order they appear in the given list. Each entry's
     * tool_modelswitch points to the next entry's decoref (or material), and
     * the last entry wraps back to the first. Does not reorder the list.
     * Returns the number of entries that received a link.
     */
    public static int linkByOrder(List<DecoEntry> entries) {
        List<DecoEntry> chain = new ArrayList<>();
        for (DecoEntry e : entries) {
            if (getLinkTarget(e) != null) chain.add(e);
        }
        if (chain.size() < 2) return 0;
        for (int i = 0; i < chain.size(); i++) {
            DecoEntry current = chain.get(i);
            DecoEntry next = chain.get((i + 1) % chain.size());
            String linkTarget = getLinkTarget(next);
            if (linkTarget != null) setModelSwitchLink(current, linkTarget);
        }
        return chain.size();
    }

    /**
     * True if the entry is made of a real wood (birch/oak/cherry/palm/spruce/
     * ebony) — NOT the ambiguous "white"/"black" which only live in
     * WOOD_FRAMES. Checks the name first, then falls back to the material.
     */
    private static boolean hasRealWood(DecoEntry entry) {
        // extractWood(name) already restricts to the real-wood list.
        if (ColorUtils.extractWood(entry.getName()) != null) return true;
        if (entry.getMaterial() != null) {
            String fromMaterial = ColorUtils.extractWoodFromMaterial(entry.getMaterial());
            if (fromMaterial != null
                    && !fromMaterial.equals("white")
                    && !fromMaterial.equals("black")) {
                return true;
            }
        }
        return false;
    }

    private static String getLinkTarget(DecoEntry entry) {
        if (entry.getDecoref() != null && !entry.getDecoref().isEmpty()) {
            return entry.getDecoref();
        }
        return entry.getMaterial();
    }

    private static void setModelSwitchLink(DecoEntry entry, String link) {
        Script script = entry.getScript();
        if (script == null) {
            script = new Script();
            entry.setScript(script);
        }
        script.setToolModelSwitchLink(link);
    }
}