package com.momo.decogen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonExporter {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /**
     * Convert a DecoEntry to an ordered map (preserves key order in JSON)
     */
    private static Map<String, Object> toOrderedMap(DecoEntry entry) {
        Map<String, Object> map = new LinkedHashMap<>();

        // Add in specific order - only add non-null optional fields
        map.put("name", entry.getName());

        if (entry.getDecoref() != null) {
            map.put("decoref", entry.getDecoref());
        }

        map.put("model", entry.getModel());
        map.put("material", entry.getMaterial());
        map.put("scale", entry.getScale());
        map.put("tabs", entry.getTabs());

        if (entry.getType() != null) {
            map.put("type", entry.getType());
        }

        map.put("crafting_color", entry.getCraftingColor());

        if (entry.getTransparency() != null) {
            map.put("transparency", entry.getTransparency());
        }

        if (Boolean.TRUE.equals(entry.getHidden())) {
            map.put("hidden", true);
        }

        if (entry.getLoot() != null) {
            map.put("loot", entry.getLoot());
        }

        if (entry.getDefaultAnimation() != null) {
            map.put("default_animation", entry.getDefaultAnimation());
        }

        if (entry.getFlipbook() != null) {
            map.put("flipbook", entry.getFlipbook());
        }

        if (entry.getScript() != null) {
            Script s = entry.getScript();
            Map<String, Object> scriptMap = new LinkedHashMap<>();

            if (s.getLight() != null) {
                scriptMap.put("light", s.getLight());
            }

            if (s.getToolModelSwitch() != null) {
                Map<String, Object> switchMap = new LinkedHashMap<>();
                switchMap.put("link", s.getToolModelSwitch().getLink());
                scriptMap.put("tool_modelswitch", switchMap);
            }

            // Handle on_use (can have link and/or animations)
            if (s.getOnUse() != null) {
                Map<String, Object> onUseMap = buildAnimationScriptMap(s.getOnUse());
                if (!onUseMap.isEmpty()) {
                    scriptMap.put("on_use", onUseMap);
                }
            }

            // Handle animation_end
            if (s.getAnimationEnd() != null) {
                Map<String, Object> animEndMap = buildAnimationScriptMap(s.getAnimationEnd());
                if (!animEndMap.isEmpty()) {
                    scriptMap.put("animation_end", animEndMap);
                }
            }

            if (!scriptMap.isEmpty()) {
                map.put("script", scriptMap);
            }
        }

        return map;
    }

    /**
     * Build a map for an AnimationScript (on_use or animation_end)
     */
    private static Map<String, Object> buildAnimationScriptMap(AnimationScript animScript) {
        Map<String, Object> map = new LinkedHashMap<>();

        // Add link if present
        if (animScript.getLink() != null && !animScript.getLink().isEmpty()) {
            map.put("link", animScript.getLink());
        }

        // Add animations array if present
        if (animScript.hasAnimations()) {
            List<Map<String, String>> animList = new ArrayList<>();
            for (AnimationPair pair : animScript.getAnimations()) {
                Map<String, String> pairMap = new LinkedHashMap<>();
                if (pair.getFrom() != null && !pair.getFrom().isEmpty()) {
                    pairMap.put("from", pair.getFrom());
                }
                if (pair.getTo() != null && !pair.getTo().isEmpty()) {
                    pairMap.put("to", pair.getTo());
                }
                if (!pairMap.isEmpty()) {
                    animList.add(pairMap);
                }
            }
            if (!animList.isEmpty()) {
                map.put("animations", animList);
            }
        }

        return map;
    }

    /**
     * Export a list of entries to JSON string
     */
    public static String toJson(List<DecoEntry> entries) {
        List<Map<String, Object>> ordered = entries.stream()
                .map(JsonExporter::toOrderedMap)
                .toList();

        String json = gson.toJson(ordered);

        // Compact crafting_color array onto one line
        json = json.replaceAll(
                "\"crafting_color\":\\s*\\[\\s*(\\d+),\\s*(\\d+),\\s*(\\d+)\\s*]",
                "\"crafting_color\": [$1, $2, $3]"
        );

        return json;
    }

    /**
     * Export a list of entries to a JSON file
     */
    public static void export(List<DecoEntry> entries, Path outputFile) throws IOException {
        String json = toJson(entries);
        Files.writeString(outputFile, json);
    }
}