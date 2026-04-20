package com.momo.decogen.logic;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.momo.decogen.model.DecoEntry;

import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Command-level undo/redo for entries + unmatched textures. Each snapshot
 * is a deep copy of the entry list (via Gson round-trip) plus a shallow
 * copy of the unmatched texture list. File maps aren't tracked — dropped
 * files are still remembered across undo.
 */
public class History {

    public static class Snapshot {
        public final List<DecoEntry> entries;
        public final List<String> unmatched;
        Snapshot(List<DecoEntry> entries, List<String> unmatched) {
            this.entries = entries;
            this.unmatched = unmatched;
        }
    }

    private static final int MAX = 50;
    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<DecoEntry>>(){}.getType();

    private final Deque<Snapshot> undoStack = new ArrayDeque<>();
    private final Deque<Snapshot> redoStack = new ArrayDeque<>();

    public void record(List<DecoEntry> entries, List<String> unmatched) {
        undoStack.push(copy(entries, unmatched));
        redoStack.clear();
        while (undoStack.size() > MAX) undoStack.pollLast();
    }

    public Snapshot undo(List<DecoEntry> current, List<String> currentUnmatched) {
        if (undoStack.isEmpty()) return null;
        redoStack.push(copy(current, currentUnmatched));
        return undoStack.pop();
    }

    public Snapshot redo(List<DecoEntry> current, List<String> currentUnmatched) {
        if (redoStack.isEmpty()) return null;
        undoStack.push(copy(current, currentUnmatched));
        return redoStack.pop();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    private static Snapshot copy(List<DecoEntry> entries, List<String> unmatched) {
        String json = GSON.toJson(entries != null ? entries : new ArrayList<>());
        List<DecoEntry> deep = GSON.fromJson(json, LIST_TYPE);
        return new Snapshot(
                deep != null ? deep : new ArrayList<>(),
                new ArrayList<>(unmatched != null ? unmatched : new ArrayList<>())
        );
    }
}