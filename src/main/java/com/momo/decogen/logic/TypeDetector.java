package com.momo.decogen.logic;

import com.momo.decogen.bbmodel.BBElement;
import com.momo.decogen.bbmodel.BBGroup;
import com.momo.decogen.bbmodel.BBModel;
import com.momo.decogen.model.DecoType;

public class TypeDetector {
    public static DecoType detectFromModel(BBModel model) {
        if (model == null) {
            return null;
        }

        if (model.getGroups() != null) {
            for (BBGroup group : model.getGroups()) {
                DecoType type = detectFromName(group.getName());
                if (type != null) return type;
            }
        }

        if (model.getElements() != null) {
            for (BBElement element : model.getElements()) {
                if (element.isLocator()) {
                    DecoType type = detectFromName(element.getName());
                    if (type != null) return type;
                }
            }
        }

        return null;
    }

    private static DecoType detectFromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();

        if (lower.startsWith("bed_node") || lower.startsWith("sleeping_node")) {
            return DecoType.BED;
        }

        if (lower.startsWith("sitting_node") || lower.startsWith("seat_node")) {
            return DecoType.SEAT;
        }

        return null;
    }
}