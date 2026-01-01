package com.momo.decogen;

import java.util.List;

public class Tabs {

    public static final List<String> ALL = List.of(
            "bathroom",
            "clutter",
            "comfort",
            "crafting",
            "flags",
            "food",
            "hobby",
            "kitchen",
            "laundry",
            "lighting",
            "medieval",
            "patreon",
            "pets",
            "seasonal",
            "shops",
            "signs",
            "storage",
            "surface",
            "tech",
            "toys",
            "wall_decor"
    );

    public static boolean isValid(String tab) {
        return ALL.contains(tab.toLowerCase());
    }
}