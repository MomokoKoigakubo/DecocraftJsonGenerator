package com.momo.decogen.logic;

import com.momo.decogen.model.Action;
import com.momo.decogen.model.DecoEntry;
import com.momo.decogen.model.Script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Turns old style lamp pairs into one toggle lamp each.
 *
 * Lamps used to be two entries: the unlit one (no script light) and a hidden lit copy, each
 * linking to the other with on_use.link. Decocraft now switches a lamp in place: one entry
 * with a script light and on_use.toggle_light gets a "lit" block state, placed off, toggled
 * by right click, with the light level following the state.
 *
 * A pair is merged when the two entries link to each other with on_use, exactly one of them
 * has a script light, and they share model, material and type (a ceiling fan whose lit copy
 * is also an animated spinning block is a different block and is left as a pair). The unlit
 * entry stays (it is the item players hold), takes the lit one's light and light colour, and
 * its on_use link becomes toggle_light; the lit entry is removed.
 *
 * Decocraft keeps the old "<decoref>_on" id of a toggle lamp registered so worlds saved with
 * the lamp switched on still load it lit. A removed lit entry named anything else has no such
 * fallback, so those ids are reported.
 */
public final class LampMerger {
    private LampMerger() {}

    public static final class Result {
        public int merged;
        /** Lit entries removed whose decoref was not "<lamp>_on" (old worlds lose those lamps). */
        public final List<String> unusualIds = new ArrayList<>();
    }

    public static Result merge(List<DecoEntry> entries) {
        Result result = new Result();
        Map<String, DecoEntry> byId = new HashMap<>();
        for (DecoEntry e : entries) {
            String id = id(e);
            if (id != null) byId.putIfAbsent(id, e);
        }

        List<DecoEntry> remove = new ArrayList<>();
        for (DecoEntry off : entries) {
            if (remove.contains(off) || light(off) > 0) continue;
            DecoEntry on = byId.get(link(off));
            if (on == null || on == off || remove.contains(on) || light(on) <= 0) continue;
            if (!Objects.equals(link(on), id(off))) continue;
            if (!Objects.equals(off.getModel(), on.getModel())
                    || !Objects.equals(off.getMaterial(), on.getMaterial())
                    || !Objects.equals(off.getType(), on.getType())) continue;

            Script script = off.getScript();
            script.setLight(light(on));
            Action use = script.getOnUse();
            use.setLink(null);
            use.setToggleLight(Boolean.TRUE);
            Action onUse = on.getScript().getOnUse();
            if ((use.getSound() == null || use.getSound().isEmpty()) && onUse.getSound() != null) {
                use.setSound(onUse.getSound()); // the switch click
            }
            if (off.getLightColor() == null && on.getLightColor() != null) {
                off.setLightColor(on.getLightColor());
            }

            remove.add(on);
            result.merged++;
            if (!Objects.equals(id(on), id(off) + "_on")) result.unusualIds.add(id(on));
        }
        entries.removeAll(remove);
        return result;
    }

    private static String id(DecoEntry e) {
        String id = e.getDecoref();
        if (id == null || id.isEmpty()) id = e.getMaterial();
        return id;
    }

    private static String link(DecoEntry e) {
        Script s = e.getScript();
        Action use = s != null ? s.getOnUse() : null;
        return use != null ? use.getLink() : null;
    }

    private static int light(DecoEntry e) {
        Script s = e.getScript();
        return s != null && s.getLight() != null ? s.getLight() : 0;
    }
}
