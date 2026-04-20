package com.momo.decogen.logic;

import com.momo.decogen.model.DecoEntry;
import com.momo.decogen.model.Script;

import java.util.*;

public class ChainBuilder {

    /**
     * Entries whose decoref (or name) ends with one of these tokens are
     * treated as a separate "state" when chaining — so the rainbow/wood
     * cycle for closed closets doesn't leak into open closets. Detection is
     * only used when an entry WITH the suffix and a partner entry WITHOUT it
     * both exist; a bare "_on" or "_open" alone still chains with everything.
     */
    public static final List<String> KNOWN_STATE_SUFFIXES =
            Arrays.asList("open", "on", "lit", "pressed", "closed");

    /**
     * Build rainbow chains for entries.
     * Groups entries by (model + wood type + state), sorts by color, links each to the next.
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

        Set<String> pairedStates = detectPairedStates(entries);

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

            String state = extractState(entry, pairedStates);
            String key = entry.getModel()
                    + ":" + (wood != null ? wood : "no_wood")
                    + ":" + (state != null ? state : "no_state");
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
     * Groups by (model + color + state), sorts by wood type, links each to next.
     */
    public static List<DecoEntry> buildWoodChains(List<DecoEntry> entries) {
        Set<String> pairedStates = detectPairedStates(entries);
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

            String state = extractState(entry, pairedStates);
            String key = entry.getModel()
                    + ":" + (color != null ? color : "no_color")
                    + ":" + (state != null ? state : "no_state");

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
     *
     * State-aware: entries whose decoref ends with a known state suffix
     * (e.g. "_open") chain among themselves separately from unsuffixed
     * entries, so closed-state and open-state variants each form their own
     * cycle. Within each bucket, the original list order is preserved.
     *
     * Returns the number of entries that received a link.
     */
    public static int linkByOrder(List<DecoEntry> entries) {
        Set<String> pairedStates = detectPairedStates(entries);

        Map<String, List<DecoEntry>> buckets = new LinkedHashMap<>();
        for (DecoEntry e : entries) {
            if (getLinkTarget(e) == null) continue;
            String state = extractState(e, pairedStates);
            String key = state != null ? state : "no_state";
            buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }

        int linked = 0;
        for (List<DecoEntry> chain : buckets.values()) {
            if (chain.size() < 2) continue;
            for (int i = 0; i < chain.size(); i++) {
                DecoEntry current = chain.get(i);
                DecoEntry next = chain.get((i + 1) % chain.size());
                String linkTarget = getLinkTarget(next);
                if (linkTarget != null) setModelSwitchLink(current, linkTarget);
            }
            linked += chain.size();
        }
        return linked;
    }

    /**
     * For every entry whose decoref/material contains the suffix as a
     * whole underscore-delimited token, and whose decoref without that token
     * matches another entry's decoref, write bidirectional on_use.link
     * between them. Handles both naming conventions:
     *   closet_7_birch &harr; closet_7_birch_open  (suffix at end)
     *   closet_7_white &harr; closet_7_open_white  (suffix in middle)
     *
     * Does not touch tool_modelswitch, so a later Rainbow/Wood/Link-by-Order
     * pass can cycle the color/wood variants independently.
     *
     * Returns the number of pairs linked.
     */
    public static int linkStatePairs(List<DecoEntry> entries, String suffix) {
        if (suffix == null) return 0;
        String norm = suffix.trim().toLowerCase();
        if (norm.isEmpty()) return 0;

        Map<String, DecoEntry> byLinkTarget = new HashMap<>();
        for (DecoEntry e : entries) {
            String target = getLinkTarget(e);
            if (target != null) byLinkTarget.put(target.toLowerCase(), e);
        }

        int pairs = 0;
        Set<DecoEntry> seen = new HashSet<>();
        for (DecoEntry variant : entries) {
            if (seen.contains(variant)) continue;
            String target = getLinkTarget(variant);
            if (target == null) continue;

            String baseKey = stripStateToken(target.toLowerCase(), norm);
            if (baseKey == null || baseKey.isEmpty()) continue;

            DecoEntry base = byLinkTarget.get(baseKey);
            if (base == null || base == variant) continue;
            if (seen.contains(base)) continue;

            setOnUseLink(base, getLinkTarget(variant));
            setOnUseLink(variant, getLinkTarget(base));
            seen.add(base);
            seen.add(variant);
            pairs++;
        }
        return pairs;
    }

    /**
     * Remove the first occurrence of {@code token} (compared case-insensitively
     * as a whole underscore-delimited segment) from {@code text}. Returns null
     * if the token isn't present, otherwise the remaining tokens rejoined by
     * underscores. Examples with token="open":
     *   closet_7_birch_open &rarr; closet_7_birch
     *   closet_7_open_white &rarr; closet_7_white
     *   closet_7_white      &rarr; null
     */
    private static String stripStateToken(String text, String token) {
        if (text == null) return null;
        String[] parts = text.split("_");
        List<String> out = new ArrayList<>(parts.length);
        boolean removed = false;
        for (String p : parts) {
            if (!removed && p.equalsIgnoreCase(token)) {
                removed = true;
                continue;
            }
            out.add(p);
        }
        if (!removed) return null;
        return String.join("_", out);
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

    private static void setOnUseLink(DecoEntry entry, String link) {
        if (link == null || link.isEmpty()) return;
        Script script = entry.getScript();
        if (script == null) {
            script = new Script();
            entry.setScript(script);
        }
        script.setOnUseLink(link);
    }

    /**
     * Figure out which of {@link #KNOWN_STATE_SUFFIXES} are actually "paired"
     * in this entry set — i.e. both an entry whose decoref contains the
     * suffix as a token and an entry whose decoref matches it with that token
     * removed. Only paired suffixes drive the chain-builder state split; an
     * isolated `closet_on` with no `closet` partner keeps chaining with
     * everything else.
     */
    private static Set<String> detectPairedStates(List<DecoEntry> entries) {
        Set<String> targets = new HashSet<>();
        for (DecoEntry e : entries) {
            String t = getLinkTarget(e);
            if (t != null) targets.add(t.toLowerCase());
        }

        Set<String> paired = new HashSet<>();
        for (String target : targets) {
            for (String suffix : KNOWN_STATE_SUFFIXES) {
                String base = stripStateToken(target, suffix);
                if (base != null && !base.isEmpty() && targets.contains(base)) {
                    paired.add(suffix);
                }
            }
        }
        return paired;
    }

    /**
     * Return the first paired state suffix that appears as a token in this
     * entry's decoref/material, or null if none. {@code pairedStates} is
     * usually the output of {@link #detectPairedStates}, so only suffixes
     * that actually have a partner entry in the current set count.
     */
    private static String extractState(DecoEntry entry, Set<String> pairedStates) {
        String target = getLinkTarget(entry);
        if (target == null) return null;
        String lower = target.toLowerCase();
        for (String suffix : KNOWN_STATE_SUFFIXES) {
            if (!pairedStates.contains(suffix)) continue;
            if (stripStateToken(lower, suffix) != null) return suffix;
        }
        return null;
    }
}