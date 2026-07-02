package com.momo.decogen.logic;

import com.momo.decogen.model.DecoEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Composable multi-key sorting for the entry list. Each {@link Key} is an
 * independent axis; the Sort dialog lets the user enable several and order
 * them by priority, because a single entry can carry more than one axis
 * (e.g. "Daybed Rose Gold Blue" has a metal AND a colour).
 */
public class EntrySorter {

    public enum Key { GROUP, COLOUR, METAL, WOOD, NUMBER, ALPHABETICAL }

    /** Metals ("medals") in Decocraft finish-cycle order. */
    public static final List<String> METALS = List.of("silver", "gold", "rose_gold", "brass");

    /**
     * Colour order used by the sort's Colour axis: true rainbow order
     * (red → … → blue → purple → pink), with the neutrals (white, cream,
     * grays, black) trailing at the end. Metals are handled by the separate
     * Metal axis, so a combined Metal→Colour sort still lists the finishes
     * first, then the colours in rainbow order. Shares
     * {@link ColorUtils#RAINBOW} directly.
     */
    public static final List<String> SORT_COLOURS = ColorUtils.RAINBOW;

    private static final Pattern NUMBER = Pattern.compile("\\d+");

    // Multi-word variant phrases removed before single tokens so "rose gold"
    // isn't clipped to "gold" and "light blue" isn't clipped to "blue".
    private static final String[] MULTIWORD = {
            "rose gold", "light blue", "ocean blue", "light gray", "light grey",
            "dark gray", "dark grey", "leather black", "leather brown"
    };

    /** Lowercase, underscores → spaces, whitespace collapsed. */
    private static String norm(DecoEntry e) {
        String s = e.getName() != null ? e.getName()
                : (e.getDecoref() != null ? e.getDecoref() : "");
        return s.toLowerCase().replace('_', ' ').replaceAll("\\s+", " ").trim();
    }

    private static boolean hasWord(String s, String w) {
        return Pattern.compile("\\b" + Pattern.quote(w) + "\\b").matcher(s).find();
    }

    /** Index in {@link #METALS} finish order, or 999 if the entry has no metal. */
    public static int metalIndex(DecoEntry e) {
        String s = norm(e);
        if (s.contains("rose gold")) return 2;   // before the bare "gold" test
        if (hasWord(s, "brass")) return 3;
        if (hasWord(s, "silver")) return 0;
        if (hasWord(s, "gold")) return 1;
        return 999;
    }

    /** Canonical-sort index of the entry's colour (metals are not colours), or 999. */
    public static int colourIndex(DecoEntry e) {
        String colour = ColorUtils.extractColor(norm(e));
        if (colour == null) return 999;
        int i = SORT_COLOURS.indexOf(colour);
        return i >= 0 ? i : 999;
    }

    /** Wood-type index (birch → oak → cherry → palm → spruce → ebony), or 999. */
    public static int woodIndex(DecoEntry e) {
        String wood = ColorUtils.extractWood(norm(e));
        if (wood == null && e.getMaterial() != null) {
            wood = ColorUtils.extractWoodFromMaterial(e.getMaterial());
        }
        if (wood == null) return 999;
        int i = ColorUtils.WOOD.indexOf(wood);
        return i >= 0 ? i : 999;
    }

    /** First integer in the name/decoref (candle_holder_2 → 2), or MAX if none. */
    public static int number(DecoEntry e) {
        Matcher m = NUMBER.matcher(norm(e));
        return m.find() ? Integer.parseInt(m.group()) : Integer.MAX_VALUE;
    }

    /**
     * The entry name with its metal / colour / number variant tokens removed,
     * so "Candle Holder 4 Silver" and "Candle Holder 4 Black" share the base
     * "candle holder" and group together — leaving the metal/colour/number
     * keys to order within the group.
     */
    public static String baseLabel(DecoEntry e) {
        String s = norm(e);
        for (String p : MULTIWORD) s = s.replaceAll("\\b" + Pattern.quote(p) + "\\b", " ");
        for (String c : ColorUtils.RAINBOW) s = s.replaceAll("\\b" + Pattern.quote(c) + "\\b", " ");
        for (String m : METALS) s = s.replaceAll("\\b" + Pattern.quote(m) + "\\b", " ");
        s = s.replaceAll("\\bgrey\\b", " ");
        s = s.replaceAll("\\b\\d+\\b", " ");
        return s.replaceAll("\\s+", " ").trim();
    }

    /** Grouping key: the entry's model (empty if none), for keeping variants together. */
    public static String group(DecoEntry e) {
        return e.getModel() != null ? e.getModel().toLowerCase() : "";
    }

    /**
     * Compare strings with digit runs treated as numbers, so
     * "candle_holder_2" precedes "candle_holder_10" instead of following it.
     */
    public static int naturalCompare(String a, String b) {
        int i = 0, j = 0;
        while (i < a.length() && j < b.length()) {
            char ca = a.charAt(i), cb = b.charAt(j);
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                int si = i, sj = j;
                while (i < a.length() && Character.isDigit(a.charAt(i))) i++;
                while (j < b.length() && Character.isDigit(b.charAt(j))) j++;
                String na = a.substring(si, i).replaceFirst("^0+(?=\\d)", "");
                String nb = b.substring(sj, j).replaceFirst("^0+(?=\\d)", "");
                if (na.length() != nb.length()) return na.length() - nb.length();
                int c = na.compareTo(nb);
                if (c != 0) return c;
            } else {
                if (ca != cb) return Character.compare(ca, cb);
                i++;
                j++;
            }
        }
        return (a.length() - i) - (b.length() - j);
    }

    public static Comparator<DecoEntry> comparator(Key key) {
        switch (key) {
            case GROUP:        return Comparator.comparing(EntrySorter::group, EntrySorter::naturalCompare);
            case COLOUR:       return Comparator.comparingInt(EntrySorter::colourIndex);
            case METAL:        return Comparator.comparingInt(EntrySorter::metalIndex);
            case WOOD:         return Comparator.comparingInt(EntrySorter::woodIndex);
            case NUMBER:       return Comparator.comparingInt(EntrySorter::number);
            case ALPHABETICAL: return Comparator.comparing(EntrySorter::baseLabel, String.CASE_INSENSITIVE_ORDER);
            default:           return (a, b) -> 0;
        }
    }

    /** Chain the keys in the given priority order into one comparator. */
    public static Comparator<DecoEntry> chain(List<Key> keysInPriority) {
        Comparator<DecoEntry> c = null;
        for (Key k : keysInPriority) {
            Comparator<DecoEntry> next = comparator(k);
            c = (c == null) ? next : c.thenComparing(next);
        }
        return c != null ? c : (a, b) -> 0;
    }

    /** Return a stably-sorted copy of {@code entries} under the chained keys. */
    public static List<DecoEntry> sorted(List<DecoEntry> entries, List<Key> keysInPriority, boolean reverse) {
        List<DecoEntry> out = new ArrayList<>(entries);
        Comparator<DecoEntry> c = chain(keysInPriority);
        out.sort(reverse ? c.reversed() : c);
        return out;
    }
}
