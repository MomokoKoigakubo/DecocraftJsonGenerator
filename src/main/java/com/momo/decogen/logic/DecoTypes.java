package com.momo.decogen.logic;

import java.util.List;

/**
 * Valid values for DecoEntry.type and related dropdowns.
 */
public final class DecoTypes {

    /**
     * All block type values. Order matches the docs' "Block Types" list
     * (note: "water" is not a real type — above_water is a top-level field).
     */
    public static final List<String> ALL = List.of(
            "underlayer",
            "animated",
            "bed",
            "seat",
            "jukebox",
            "decobench",
            "decomposer",
            "fake_block",
            "chain"
    );

    /** Valid values for DecoEntry.chain_pattern. */
    public static final List<String> CHAIN_PATTERNS = List.of(
            "mirror",
            "repeat"
    );

    private DecoTypes() {}
}