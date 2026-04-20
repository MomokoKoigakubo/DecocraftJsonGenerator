package com.momo.decogen.logic;

import com.momo.decogen.bbmodel.BBAnimation;
import com.momo.decogen.bbmodel.BBElement;
import com.momo.decogen.bbmodel.BBModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only queries over a parsed Blockbench model that drive auto-fill
 * in the editor (default animation, particle locators).
 *
 * Note: display slots (display_* groups) are resolved at runtime by the
 * mod — we don't surface them here.
 */
public final class ModelInspector {

    // Locator prefixes the mod already handles as bed/seat nodes — exclude
    // these when suggesting particle locators.
    private static final String[] SIT_BED_PREFIXES = {
            "bed_node", "sleeping_node", "sitting_node", "seat_node"
    };

    // Prefix reserved for display-slot locators; skipped when listing particles.
    private static final String DISPLAY_PREFIX = "display_";

    private ModelInspector() {}

    /**
     * Pick the animation to use as "default_animation":
     *   1. first whose name contains "idle" (case-insensitive)
     *   2. otherwise the first animation
     *   3. null if the model has no animations
     */
    public static String pickDefaultAnimation(BBModel model) {
        if (model == null || !model.hasAnimations()) return null;

        List<BBAnimation> animations = model.getAnimations();
        String firstName = null;

        for (BBAnimation anim : animations) {
            String name = anim.getName();
            if (name == null || name.isEmpty()) continue;
            if (firstName == null) firstName = name;
            if (name.toLowerCase().contains("idle")) return name;
        }

        return firstName;
    }

    /**
     * Locator element names that are likely particle-effect references —
     * i.e. any locator whose name does not match a known bed/seat prefix
     * and is not inside a display_ group namespace.
     *
     * The returned names are the locator names as written in Blockbench;
     * they should match a .particle.json file name to actually trigger
     * particles at runtime.
     */
    public static List<String> getCandidateParticleLocators(BBModel model) {
        List<String> out = new ArrayList<>();
        if (model == null || model.getElements() == null) return out;

        for (BBElement el : model.getElements()) {
            if (!el.isLocator()) continue;
            String name = el.getName();
            if (name == null || name.isEmpty()) continue;

            if (isSitBedLocator(name)) continue;
            if (name.toLowerCase().startsWith(DISPLAY_PREFIX)) continue;

            out.add(name);
        }
        return out;
    }

    private static boolean isSitBedLocator(String name) {
        String lower = name.toLowerCase();
        for (String prefix : SIT_BED_PREFIXES) {
            if (lower.startsWith(prefix)) return true;
        }
        return false;
    }
}