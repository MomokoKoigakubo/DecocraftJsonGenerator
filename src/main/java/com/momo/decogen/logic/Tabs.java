package com.momo.decogen.logic;

import java.util.List;

public class Tabs {

    public static final List<String> ALL = List.of(
            "bathroom",
            "bedroom",
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
            "paintings",
            "patreon",
            "pets",
            "seasonal",
            "seating",
            "shops",
            "signs",
            "storage",
            "surface",
            "tech",
            "toys",
            "trees",
            "wall_decor"
    );

    public static boolean isValid(String tab) {
        return ALL.contains(tab.toLowerCase());
    }
}