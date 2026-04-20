package com.momo.decogen.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.momo.decogen.model.Action;
import com.momo.decogen.model.AnimationPair;
import com.momo.decogen.model.Composite;
import com.momo.decogen.model.DecoEntry;
import com.momo.decogen.model.Flipbook;
import com.momo.decogen.model.Script;
import com.momo.decogen.model.SoundPair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serializes DecoEntry objects to JSON using the field order defined in
 * json_fields.txt.
 */
public class JsonExporter {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /**
     * Animation-pair key order in the output JSON. Both orderings are
     * semantically identical to any JSON reader (order of object keys is
     * not significant), but some downstream tooling and existing example
     * files write "to" before "from". Flip this to false to emit
     * {"from":..., "to":...} instead.
     */
    private static final boolean TO_BEFORE_FROM = true;

    // === Entry ===

    private static Map<String, Object> toOrderedMap(DecoEntry entry) {
        Map<String, Object> map = new LinkedHashMap<>();

        // Identity (1-3). Decoref is always emitted; fall back to material
        // and then to model if the entry was created without one.
        map.put("name", entry.getName());
        String decoref = entry.getDecoref();
        if (decoref == null || decoref.isEmpty()) decoref = entry.getMaterial();
        if (decoref == null || decoref.isEmpty()) decoref = entry.getModel();
        if (decoref != null) map.put("decoref", decoref);
        if (entry.getMaterial() != null) map.put("material", entry.getMaterial());

        // Model & rendering (4-8)
        if (entry.getModel() != null) map.put("model", entry.getModel());
        map.put("scale", entry.getScale());
        if (entry.getShape() != null) map.put("shape", entry.getShape());
        if (entry.getTransparency() != null) map.put("transparency", entry.getTransparency());
        if (entry.getCulling() != null) map.put("culling", entry.getCulling());

        // Tab & display (9-10)
        if (entry.getTabs() != null) map.put("tabs", entry.getTabs());
        if (entry.getDefaultAnimation() != null) map.put("default_animation", entry.getDefaultAnimation());

        // Block type & behavior (11-17)
        if (entry.getType() != null) map.put("type", entry.getType());
        if (entry.getPassable() != null) map.put("passable", entry.getPassable());
        if (entry.getAboveWater() != null) map.put("above_water", entry.getAboveWater());
        if (entry.getRotatable() != null) map.put("rotatable", entry.getRotatable());
        if (entry.getDisplayable() != null) map.put("displayable", entry.getDisplayable());
        if (Boolean.TRUE.equals(entry.getHidden())) map.put("hidden", true);
        if (entry.getLoot() != null) map.put("loot", entry.getLoot());

        // Crafting color (18) - last flat field before nested/array/script groups
        map.put("crafting_color", entry.getCraftingColor());

        // Composite & flipbook (19-20)
        if (entry.getComposite() != null) {
            Map<String, Object> cm = buildCompositeMap(entry.getComposite());
            if (!cm.isEmpty()) map.put("composite", cm);
        }
        if (entry.getFlipbook() != null) {
            map.put("flipbook", buildFlipbookMap(entry.getFlipbook()));
        }

        // Chain (21-24)
        if (entry.getChainModels() != null && !entry.getChainModels().isEmpty()) {
            map.put("chain_models", entry.getChainModels());
        }
        if (entry.getChainMaterials() != null && !entry.getChainMaterials().isEmpty()) {
            map.put("chain_materials", entry.getChainMaterials());
        }
        if (entry.getChainPattern() != null) map.put("chain_pattern", entry.getChainPattern());
        if (entry.getLighting() != null) map.put("lighting", entry.getLighting());

        // Growable (25-26)
        if (entry.getStructures() != null && !entry.getStructures().isEmpty()) {
            map.put("structures", entry.getStructures());
        }
        if (entry.getInstant() != null) map.put("instant", entry.getInstant());

        // Script (27)
        if (entry.getScript() != null) {
            Map<String, Object> scriptMap = buildScriptMap(entry.getScript());
            if (!scriptMap.isEmpty()) map.put("script", scriptMap);
        }

        return map;
    }

    // === Composite (28-30) ===

    private static Map<String, Object> buildCompositeMap(Composite c) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (c.getModel() != null) map.put("model", c.getModel());
        if (c.getTexture() != null) map.put("texture", c.getTexture());
        if (c.getTransparency() != null) map.put("transparency", c.getTransparency());
        return map;
    }

    // === Flipbook (31-32) ===

    private static Map<String, Object> buildFlipbookMap(Flipbook f) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("frametime", f.getFrametime());
        map.put("images", f.getImages());
        return map;
    }

    // === Script (33-41) ===

    private static Map<String, Object> buildScriptMap(Script s) {
        Map<String, Object> map = new LinkedHashMap<>();

        putActionIfPresent(map, "on_use", s.getOnUse());
        putActionIfPresent(map, "shift_on_use", s.getShiftOnUse());
        putActionIfPresent(map, "added", s.getAdded());
        putActionIfPresent(map, "trigger", s.getTrigger());
        putActionIfPresent(map, "animation_start", s.getAnimationStart());
        putActionIfPresent(map, "animation_end", s.getAnimationEnd());
        putActionIfPresent(map, "tool_modelswitch", s.getToolModelSwitch());

        if (s.getCounter() != null) map.put("counter", s.getCounter());
        if (s.getLight() != null) map.put("light", s.getLight());

        return map;
    }

    private static void putActionIfPresent(Map<String, Object> map, String key, Action action) {
        if (action == null || action.isEmpty()) return;
        Map<String, Object> am = buildActionMap(action);
        if (!am.isEmpty()) map.put(key, am);
    }

    // === Action (42-46) ===

    private static Map<String, Object> buildActionMap(Action a) {
        Map<String, Object> map = new LinkedHashMap<>();

        if (a.getLink() != null && !a.getLink().isEmpty()) map.put("link", a.getLink());
        if (a.getSound() != null && !a.getSound().isEmpty()) map.put("sound", a.getSound());

        if (a.hasAnimations()) {
            List<Map<String, String>> list = buildAnimationList(a.getAnimations());
            if (!list.isEmpty()) map.put("animations", list);
        }
        if (a.hasSounds()) {
            List<Map<String, Object>> list = buildSoundList(a.getSounds());
            if (!list.isEmpty()) map.put("sounds", list);
        }
        if (a.getStorage() != null && a.getStorage().length > 0) {
            map.put("storage", a.getStorage());
        }

        return map;
    }

    // === animations[] entry (47-48) ===

    private static List<Map<String, String>> buildAnimationList(List<AnimationPair> pairs) {
        List<Map<String, String>> out = new ArrayList<>();
        for (AnimationPair p : pairs) {
            Map<String, String> m = new LinkedHashMap<>();
            String from = (p.getFrom() != null && !p.getFrom().isEmpty()) ? p.getFrom() : null;
            String to = (p.getTo() != null && !p.getTo().isEmpty()) ? p.getTo() : null;
            if (TO_BEFORE_FROM) {
                if (to != null) m.put("to", to);
                if (from != null) m.put("from", from);
            } else {
                if (from != null) m.put("from", from);
                if (to != null) m.put("to", to);
            }
            if (!m.isEmpty()) out.add(m);
        }
        return out;
    }

    // === sounds[] entry (49-52) ===

    private static List<Map<String, Object>> buildSoundList(List<SoundPair> pairs) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SoundPair p : pairs) {
            Map<String, Object> m = new LinkedHashMap<>();
            if (p.getFrom() != null && !p.getFrom().isEmpty()) m.put("from", p.getFrom());
            if (p.getTo() != null && !p.getTo().isEmpty()) m.put("to", p.getTo());
            if (p.getSound() != null && !p.getSound().isEmpty()) m.put("sound", p.getSound());
            if (p.getLoop() != null) m.put("loop", p.getLoop());
            if (!m.isEmpty()) out.add(m);
        }
        return out;
    }

    // === Top-level serialization ===

    public static String toJson(List<DecoEntry> entries) {
        List<Map<String, Object>> ordered = entries.stream()
                .map(JsonExporter::toOrderedMap)
                .toList();

        String json = gson.toJson(ordered);

        // Compact short numeric arrays onto one line
        json = json.replaceAll(
                "\"crafting_color\":\\s*\\[\\s*(\\d+(?:\\.\\d+)?),\\s*(\\d+(?:\\.\\d+)?),\\s*(\\d+(?:\\.\\d+)?)\\s*]",
                "\"crafting_color\": [$1, $2, $3]"
        );
        json = json.replaceAll(
                "\"storage\":\\s*\\[\\s*(\\d+),\\s*(\\d+)\\s*]",
                "\"storage\": [$1, $2]"
        );

        // Compact string arrays (chain_models, chain_materials, structures) onto one line
        for (String field : new String[]{"chain_models", "chain_materials", "structures"}) {
            json = compactStringArray(json, field);
        }

        return json;
    }

    private static String compactStringArray(String json, String field) {
        Pattern p = Pattern.compile("\"" + field + "\":\\s*\\[([^\\]]*?)\\]", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String inner = m.group(1).replaceAll("\\s+", " ").trim();
            m.appendReplacement(sb, Matcher.quoteReplacement("\"" + field + "\": [" + inner + "]"));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static void export(List<DecoEntry> entries, Path outputFile) throws IOException {
        String json = toJson(entries);
        Files.writeString(outputFile, json);
    }
}