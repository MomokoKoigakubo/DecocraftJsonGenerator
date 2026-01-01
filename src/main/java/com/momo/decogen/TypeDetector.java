package com.momo.decogen;

public class TypeDetector {
    public static DecoType detectFromModel(Model model) {
        if (model == null) {
            return null;
        }

        // Check groups
        if (model.getGroups() != null) {
            for (BBGroup group : model.getGroups()) {
                DecoType type = detectFromName(group.getName());
                if (type != null) return type;
            }
        }

        // Check elements (locators)
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

        // Check for bed node (bed_node, bed_node2, etc.)
        if (lower.startsWith("bed_node") || lower.startsWith("sleeping_node")) {
            return DecoType.BED;
        }

        // Check for sitting node (sitting_node, sitting_node2, etc.)
        if (lower.startsWith("sitting_node") || lower.startsWith("seat_node")) {
            return DecoType.SEAT;
        }

        return null;
    }
}