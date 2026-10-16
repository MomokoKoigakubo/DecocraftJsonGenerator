package com.momo.decogen.logic;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.momo.decogen.model.DecoEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses pasted Decocraft entry JSON back into {@link DecoEntry} objects.
 *
 * The exported JSON uses snake_case keys (crafting_color, tool_modelswitch, …).
 * {@code DecoEntry} fields are camelCase while {@code Script}'s are already
 * snake_case; the LOWER_CASE_WITH_UNDERSCORES naming policy handles both
 * (camelCase → snake, and the already-snake Script field names are unchanged),
 * so no per-field annotations are needed. The no-arg constructors keep the
 * field defaults (scale = 1.0, crafting_color = [0,0,0]) when a key is absent.
 */
public class JsonImporter {

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    /** Parse a JSON array (or a single object) of entries. Throws on invalid JSON. */
    public static List<DecoEntry> parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new JsonParseException("No JSON provided.");
        }
        JsonElement root = JsonParser.parseString(json);

        List<DecoEntry> out = new ArrayList<>();
        if (root.isJsonArray()) {
            for (JsonElement el : root.getAsJsonArray()) {
                if (el.isJsonObject()) out.add(parseEntry(el.getAsJsonObject()));
            }
        } else if (root.isJsonObject()) {
            out.add(parseEntry(root.getAsJsonObject()));
        } else {
            throw new JsonParseException("Expected a JSON array or object of entries.");
        }

        for (DecoEntry e : out) {
            if (e.getCraftingColor() == null) e.setCraftingColor(new int[]{0, 0, 0});
            if (e.getScale() <= 0.0) e.setScale(1.0);
        }
        return out;
    }

    /** Older exports wrote "tabs" as a single string — wrap it so it parses as a list. */
    private static DecoEntry parseEntry(JsonObject obj) {
        JsonElement tabs = obj.get("tabs");
        if (tabs != null && tabs.isJsonPrimitive()) {
            JsonArray arr = new JsonArray();
            arr.add(tabs);
            obj.add("tabs", arr);
        }
        return GSON.fromJson(obj, DecoEntry.class);
    }
}
