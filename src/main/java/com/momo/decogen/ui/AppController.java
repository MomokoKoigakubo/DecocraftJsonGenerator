package com.momo.decogen.ui;

import com.momo.decogen.bbmodel.BBModel;
import com.momo.decogen.bbmodel.BBModelParser;
import com.momo.decogen.io.DirectoryScanner;
import com.momo.decogen.io.JsonExporter;
import com.momo.decogen.logic.ChainBuilder;
import com.momo.decogen.logic.EntryBuilder;
import com.momo.decogen.logic.EntrySorter;
import com.momo.decogen.logic.History;
import com.momo.decogen.logic.JsonImporter;
import com.momo.decogen.logic.TextureMatcher;
import com.momo.decogen.model.DecoEntry;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Holds app state (entries + dropped files) and wires user actions.
 * UI panels call into this controller and register their nodes via setters.
 */
public class AppController {

    // --- State ---
    private final ObservableList<DecoEntry> entries = FXCollections.observableArrayList();
    private final History history = new History();
    private final Map<String, java.nio.file.Path> modelFiles = new HashMap<>();
    private final Map<String, java.nio.file.Path> textureFiles = new HashMap<>();
    private final Map<String, java.nio.file.Path> iconFiles = new HashMap<>();
    private final List<String> unmatchedTextures = new ArrayList<>();
    private final Map<String, BBModel> parsedModels = new HashMap<>();

    // --- UI references (set by panels) ---
    private ListView<DecoEntry> entryListView;
    private ListView<String> unmatchedListView;
    private ListView<String> iconListView;
    private Label iconLabel;
    private TextArea jsonPreview;
    private EditorPanel editorPanel;
    // Needed so dialogs can initOwner() — unowned modals un-maximize the
    // parent on Linux (GNOME/mutter).
    private Stage primaryStage;

    // --- Registration ---
    public void setEntryListView(ListView<DecoEntry> v) { this.entryListView = v; }
    public void setUnmatchedListView(ListView<String> v) { this.unmatchedListView = v; }
    public void setIconListView(ListView<String> v) { this.iconListView = v; }
    public void setIconLabel(Label l) { this.iconLabel = l; }
    public void setJsonPreview(TextArea a) { this.jsonPreview = a; }
    public void setEditorPanel(EditorPanel p) { this.editorPanel = p; }
    public void setPrimaryStage(Stage s) { this.primaryStage = s; }
    public Stage getPrimaryStage() { return primaryStage; }

    // --- State accessors ---
    public ObservableList<DecoEntry> getEntries() { return entries; }
    public Map<String, java.nio.file.Path> getModelFiles() { return modelFiles; }
    public Map<String, java.nio.file.Path> getTextureFiles() { return textureFiles; }
    public Map<String, java.nio.file.Path> getIconFiles() { return iconFiles; }
    public List<String> getUnmatchedTextures() { return unmatchedTextures; }
    public Map<String, BBModel> getParsedModels() { return parsedModels; }

    public ListView<DecoEntry> getEntryListView() { return entryListView; }
    public ListView<String> getUnmatchedListView() { return unmatchedListView; }
    public EditorPanel getEditorPanel() { return editorPanel; }

    // --- Undo / redo ---

    /** Record current state before a mutating command. */
    public void snapshot() {
        history.record(entries, unmatchedTextures);
    }

    public void undo() {
        History.Snapshot s = history.undo(new ArrayList<>(entries), new ArrayList<>(unmatchedTextures));
        if (s != null) applySnapshot(s);
    }

    public void redo() {
        History.Snapshot s = history.redo(new ArrayList<>(entries), new ArrayList<>(unmatchedTextures));
        if (s != null) applySnapshot(s);
    }

    private void applySnapshot(History.Snapshot s) {
        entries.setAll(s.entries);
        unmatchedTextures.clear();
        unmatchedTextures.addAll(s.unmatched);
        if (unmatchedListView != null) unmatchedListView.getItems().setAll(unmatchedTextures);
        if (entryListView != null) entryListView.refresh();
        updateJsonPreview();
        updateIconListView();
    }

    // --- File intake (self-reconciling) ---

    public void addModel(File file) {
        snapshot();
        try {
            BBModel model = BBModelParser.parse(file.toPath());
            String modelName = DirectoryScanner.getStem(file.toPath());

            modelFiles.put(modelName, file.toPath());
            parsedModels.put(modelName, model);
            System.out.println("Added model: " + modelName);

            int created = 0;

            // 1) For every known texture whose resolved model set includes this
            //    new model, ensure an entry exists. This covers textures still
            //    sitting in unmatchedTextures AND textures already placed on a
            //    previously-loaded base model (when this new model is a
            //    state-variant sibling like "locker_open" of "locker").
            Set<String> textureCandidates = new java.util.LinkedHashSet<>();
            textureCandidates.addAll(unmatchedTextures);
            textureCandidates.addAll(textureFiles.keySet());
            for (String textureName : textureCandidates) {
                if (iconFiles.containsKey(textureName)) continue;
                List<String> resolved = resolveModelsForTexture(textureName);
                if (!resolved.contains(modelName)) continue;
                if (entryExists(modelName, textureName)) continue;

                ensureEntryForTextureModel(textureName, modelName);
                unmatchedTextures.remove(textureName);
                created++;
            }

            // 2) Sweep stored icons waiting for this model.
            for (String iconName : new ArrayList<>(iconFiles.keySet())) {
                String im = TextureMatcher.findMatchingModel(iconName, modelFiles.keySet());
                if (im == null || !im.equals(modelName)) continue;
                if (hasIconEntry(iconName)) continue;

                createIconEntry(iconName, modelName);
                created++;
            }

            // 3) Nothing matched — drop a placeholder so the user sees the model.
            if (created == 0) {
                String displayName = EntryBuilder.toDisplayName(modelName);
                DecoEntry entry = new DecoEntry(displayName, modelName, null, "clutter");
                entry.setDecoref(modelName);
                entry.autoDetectType(model);
                entries.add(entry);
                System.out.println("Created placeholder entry for model: " + modelName);
            }

            if (unmatchedListView != null) {
                unmatchedListView.getItems().setAll(unmatchedTextures);
            }
            refreshEntryList();
        } catch (Exception e) {
            System.err.println("Error loading model: " + e.getMessage());
        }
    }

    public void addTexture(File file) {
        snapshot();
        String textureName = DirectoryScanner.getStem(file.toPath());
        textureFiles.put(textureName, file.toPath());

        List<String> resolved = resolveModelsForTexture(textureName);
        if (resolved.isEmpty()) {
            // No model matched by prefix. This is usually a SHARED generic
            // texture (e.g. the one "candle_holder_black_flipbook" flame used
            // by every candle_holder_N): fan it out to every still-empty icon
            // of the same color in its family. Falls back to a single
            // longest-match bind for odd cases (covers the school_desk_red ->
            // school_set_red divergence).
            int filled = fanOutLooseTextureToIcons(textureName);
            if (filled == 0) {
                DecoEntry waiting = findEmptyIconEntryForLooseTexture(textureName);
                if (waiting != null) {
                    waiting.setMaterial(textureName);
                    filled = 1;
                }
            }
            if (filled > 0) {
                System.out.println("Bound loose texture " + textureName
                        + " to " + filled + " icon entr" + (filled == 1 ? "y" : "ies"));
                refreshEntryList();
                return;
            }
            unmatchedTextures.add(textureName);
            if (unmatchedListView != null) {
                unmatchedListView.getItems().setAll(unmatchedTextures);
            }
            System.out.println("Added unmatched texture: " + textureName);
            return;
        }

        for (String modelName : resolved) {
            if (entryExists(modelName, textureName)) continue;
            removePlaceholdersForModel(modelName);
            ensureEntryForTextureModel(textureName, modelName);
            System.out.println("Created entry: " + textureName + " -> " + modelName);
        }
        unmatchedTextures.remove(textureName);
        refreshEntryList();
    }

    public void addIcon(File file) {
        snapshot();
        String iconName = DirectoryScanner.getStem(file.toPath());

        textureFiles.put(iconName, file.toPath());
        iconFiles.put(iconName, file.toPath());
        updateIconListView();

        String matchedModel = TextureMatcher.findMatchingModel(iconName, modelFiles.keySet());
        if (matchedModel == null) {
            System.out.println("Stored icon (no model yet): " + iconName);
            return;
        }
        if (hasIconEntry(iconName)) return;

        createIconEntry(iconName, matchedModel);
        if (unmatchedListView != null) {
            unmatchedListView.getItems().setAll(unmatchedTextures);
        }
        refreshEntryList();
    }

    /**
     * Create an icon-based entry (decoref = iconName) for the given model,
     * filling its material from an unmatched texture with the same suffix if
     * one exists. Also clears any placeholder entry for the same model.
     */
    private void createIconEntry(String iconName, String modelName) {
        String matchingTexture = findUnmatchedTextureForIcon(iconName, modelName);

        removePlaceholdersForModel(modelName);

        String displayName = EntryBuilder.toDisplayName(iconName);
        DecoEntry entry = new DecoEntry(displayName, modelName, matchingTexture, "clutter");
        entry.setDecoref(iconName);

        BBModel model = parsedModels.get(modelName);
        if (model != null) entry.autoDetectType(model);

        entries.add(entry);
        if (matchingTexture != null) unmatchedTextures.remove(matchingTexture);

        System.out.println("Created icon entry: " + iconName + " -> " + modelName
                + (matchingTexture != null ? " (material: " + matchingTexture + ")" : ""));
    }

    private void createEntryForTexture(String textureName, String modelName) {
        String displayName = EntryBuilder.toDisplayName(textureName);
        DecoEntry entry = new DecoEntry(displayName, modelName, textureName, "clutter");
        // Decoref is always present — default to the texture name for icon-less entries.
        entry.setDecoref(textureName);

        BBModel model = parsedModels.get(modelName);
        if (model != null) entry.autoDetectType(model);

        entries.add(entry);
        unmatchedTextures.remove(textureName);
        refreshEntryList();
    }

    /**
     * Look for an entry that was built from an icon (decoref set, material
     * still empty) whose decoref ends with the color/variant suffix of the
     * given texture. That entry is waiting for this texture.
     */
    private DecoEntry findEmptyIconEntryForTexture(String modelName, String textureName) {
        // Prefer the icon whose decoref IS this texture's base name (ignoring a
        // trailing "_flipbook"): candle_holder_4_black_flipbook fills the
        // candle_holder_4_black icon exactly, never a same-color sibling.
        String base = TextureMatcher.stripFlipbook(textureName.toLowerCase());
        for (DecoEntry e : entries) {
            if (!modelName.equals(e.getModel())) continue;
            if (e.getDecoref() == null) continue;
            if (!e.getDecoref().toLowerCase().equals(base)) continue;
            // Fill an empty icon, OR upgrade one that a shared generic filled
            // earlier — a per-model texture is more specific and should win
            // regardless of drop order. If it already holds this exact
            // texture, there's nothing to do.
            String mat = e.getMaterial();
            if (mat == null || !TextureMatcher.stripFlipbook(mat.toLowerCase()).equals(base)) {
                return e;
            }
            return null;
        }

        String suffix = TextureMatcher.extractColorSuffix(textureName);
        if (suffix == null || suffix.isEmpty()) return null;
        String suffixLower = "_" + suffix.toLowerCase();

        for (DecoEntry e : entries) {
            if (!modelName.equals(e.getModel())) continue;
            if (e.getMaterial() != null) continue;
            if (e.getDecoref() == null) continue;
            if (e.getDecoref().toLowerCase().endsWith(suffixLower)) return e;
        }
        return null;
    }

    /**
     * Find an icon-driven entry (material null, decoref set, has a model)
     * whose decoref ends with the texture's color suffix. Used when a texture
     * has no model-prefix match — covers school_desk-style cases where the
     * material naming (school_set_red) and decoref naming (school_desk_red)
     * diverge but share a color suffix. Prefers the longest-matching decoref
     * suffix to disambiguate red vs dark_red.
     */
    /**
     * A shared generic texture (matches no model by prefix) fills EVERY
     * still-empty icon entry of the same colour. Decocraft flame textures like
     * "candle_holder_black_flipbook" are deliberately shared across families —
     * candle_holder_1_black, candle_holder_wall_2_black AND chandelier_1_black
     * all use the same flame — so matching is by colour only, NOT family. Only
     * empty entries are touched, so a per-model/exact texture already bound to
     * an icon still wins. Returns the number of icon entries filled.
     */
    private int fanOutLooseTextureToIcons(String textureName) {
        String suffix = TextureMatcher.extractColorSuffix(textureName);
        if (suffix == null || suffix.isEmpty()) return 0;
        String suffixLower = "_" + suffix.toLowerCase();

        String texBase = TextureMatcher.stripFlipbook(textureName.toLowerCase());
        if (!texBase.endsWith(suffixLower)) return 0;
        String texFamily = texBase.substring(0, texBase.length() - suffixLower.length());
        // A bare "black_flipbook" (no family) is too generic to fan out safely;
        // defer to the single-bind fallback instead.
        if (texFamily.isEmpty()) return 0;

        int filled = 0;
        for (DecoEntry e : entries) {
            if (e.getModel() == null) continue;
            if (e.getMaterial() != null) continue;
            if (e.getDecoref() == null) continue;
            String dl = e.getDecoref().toLowerCase();
            // The icon's OWN colour must equal the texture's colour, so a "gold"
            // flame doesn't leak into a "rose_gold" icon (which also ends in
            // "_gold"). extractColorSuffix parses the longest known colour.
            String iconColor = TextureMatcher.extractColorSuffix(e.getDecoref());
            if (!suffix.equalsIgnoreCase(iconColor)) continue;
            if (!dl.endsWith(suffixLower)) continue;
            e.setMaterial(textureName);
            filled++;
        }
        return filled;
    }

    private DecoEntry findEmptyIconEntryForLooseTexture(String textureName) {
        String suffix = TextureMatcher.extractColorSuffix(textureName);
        if (suffix == null || suffix.isEmpty()) return null;
        String suffixLower = "_" + suffix.toLowerCase();

        DecoEntry best = null;
        int bestLen = -1;
        for (DecoEntry e : entries) {
            if (e.getMaterial() != null) continue;
            if (e.getDecoref() == null) continue;
            if (e.getModel() == null) continue;
            String decorefLower = e.getDecoref().toLowerCase();
            if (!decorefLower.endsWith(suffixLower)) continue;
            // Prefer the longest decoref so that a textureNamed *_red
            // assigns to school_desk_dark_red over school_desk_red when both
            // are waiting (unlikely, but deterministic).
            if (decorefLower.length() > bestLen) {
                best = e;
                bestLen = decorefLower.length();
            }
        }
        return best;
    }

    /**
     * Given an icon name "model_suffix", find an unmatched texture whose name
     * ends with that suffix (so it can become the icon entry's material).
     */
    private String findUnmatchedTextureForIcon(String iconName, String modelName) {
        // Same-name (± "_flipbook") texture wins first, so this model's icon
        // never borrows another model's same-color texture.
        String exact = TextureMatcher.findExactTextureForIcon(
                iconName, unmatchedTextures, textureFiles.keySet(), iconFiles.keySet());
        if (exact != null) return exact;

        if (iconName.length() <= modelName.length() + 1) return null;
        String suffix = iconName.substring(modelName.length() + 1);
        return TextureMatcher.findMatchingTextureForSuffix(
                suffix, unmatchedTextures, textureFiles.keySet(), iconFiles.keySet());
    }

    /** Placeholder entries carry a model only — no material, no decoref. */
    private void removePlaceholdersForModel(String modelName) {
        entries.removeIf(e -> modelName.equals(e.getModel())
                && e.getMaterial() == null
                && e.getDecoref() == null);
    }

    private boolean hasIconEntry(String iconName) {
        return entries.stream().anyMatch(e -> iconName.equals(e.getDecoref()));
    }

    /**
     * Every model a texture should produce an entry for: the longest-prefix
     * match, plus any state-variant sibling model (base + "_open"/"_on"/etc.)
     * that exists in the project. Lets "locker_yellow" populate both the
     * "locker" entry and the "locker_open" entry even though the texture name
     * has no state suffix of its own.
     */
    private List<String> resolveModelsForTexture(String textureName) {
        String best = TextureMatcher.findMatchingModel(textureName, modelFiles.keySet());
        if (best == null) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        out.add(best);
        String bestLower = best.toLowerCase();
        for (String other : modelFiles.keySet()) {
            if (other.equalsIgnoreCase(best)) continue;
            String otherLower = other.toLowerCase();
            if (!otherLower.startsWith(bestLower + "_")) continue;
            String remainder = otherLower.substring(bestLower.length() + 1);
            if (ChainBuilder.KNOWN_STATE_SUFFIXES.contains(remainder)) {
                out.add(other);
            }
        }
        return out;
    }

    /** Returns "open"/"on"/... if variantModel is baseModel + "_<state>", else null. */
    private String stateSuffixFor(String baseModel, String variantModel) {
        if (baseModel == null || variantModel == null) return null;
        String bl = baseModel.toLowerCase();
        String vl = variantModel.toLowerCase();
        if (!vl.startsWith(bl + "_")) return null;
        String rem = vl.substring(bl.length() + 1);
        return ChainBuilder.KNOWN_STATE_SUFFIXES.contains(rem) ? rem : null;
    }

    private boolean entryExists(String modelName, String textureName) {
        for (DecoEntry e : entries) {
            if (modelName.equals(e.getModel()) && textureName.equals(e.getMaterial())) return true;
        }
        return false;
    }

    /**
     * Create (or skip if already present) an entry for a specific (texture,
     * model) pair. For state-variant models, decoref gets the state suffix
     * appended so linkStatePairs can later pair them by stripping that token.
     */
    private void ensureEntryForTextureModel(String textureName, String modelName) {
        if (entryExists(modelName, textureName)) return;

        DecoEntry emptyIcon = findEmptyIconEntryForTexture(modelName, textureName);
        if (emptyIcon != null) {
            emptyIcon.setMaterial(textureName);
            return;
        }

        String baseModel = TextureMatcher.findMatchingModel(textureName, modelFiles.keySet());
        String stateSuffix = baseModel == null ? null : stateSuffixFor(baseModel, modelName);
        String decorefName = stateSuffix != null ? textureName + "_" + stateSuffix : textureName;

        String displayName = EntryBuilder.toDisplayName(decorefName);
        DecoEntry entry = new DecoEntry(displayName, modelName, textureName, "clutter");
        entry.setDecoref(decorefName);

        BBModel model = parsedModels.get(modelName);
        if (model != null) entry.autoDetectType(model);

        entries.add(entry);
    }

    // --- Auto-match / pair ---

    public void autoMatchTextures() {
        if (modelFiles.isEmpty()) {
            showAlert("No Models Loaded",
                    "Drop a .bbmodel into the Models zone first. Auto-match needs a model to pair textures and icons against.");
            return;
        }
        snapshot();
        int prefixMatched = 0;
        // Pass 1: prefix-based matching (existing behavior). "locker_yellow"
        // matches model "locker" and populates state-variant siblings too.
        for (String textureName : new ArrayList<>(unmatchedTextures)) {
            List<String> resolved = resolveModelsForTexture(textureName);
            if (resolved.isEmpty()) continue;
            for (String modelName : resolved) {
                if (entryExists(modelName, textureName)) continue;
                removePlaceholdersForModel(modelName);
                ensureEntryForTextureModel(textureName, modelName);
                System.out.println("Created entry for texture: " + textureName + " -> " + modelName);
            }
            unmatchedTextures.remove(textureName);
            prefixMatched++;
        }

        // Pass 2: fan-out remaining unmatched textures over a single
        // placeholder entry. Covers cases like wall_flag_1 + 250 country
        // textures where the texture names share no prefix with the model.
        List<DecoEntry> placeholders = new ArrayList<>();
        for (DecoEntry e : entries) {
            if (e.getMaterial() == null && e.getModel() != null && e.getDecoref() != null) {
                placeholders.add(e);
            }
        }
        int fannedOut = 0;
        if (!unmatchedTextures.isEmpty() && placeholders.size() == 1) {
            DecoEntry template = placeholders.get(0);
            String modelName = template.getModel();
            List<DecoEntry> newEntries = new ArrayList<>();
            for (String textureName : new ArrayList<>(unmatchedTextures)) {
                String suffix = TextureMatcher.extractColorSuffix(textureName);
                String iconName = TextureMatcher.findMatchingIcon(modelName, suffix, textureFiles.keySet());
                DecoEntry entry = History.deepCopy(template, DecoEntry.class);
                String displayName = EntryBuilder.toDisplayName(modelName + "_" + suffix);
                String decoref = (iconName != null) ? iconName : (modelName + "_" + suffix);
                entry.setDecoref(decoref);
                entry.setName(displayName);
                entry.setModel(modelName);
                entry.setMaterial(textureName);
                newEntries.add(entry);
                unmatchedTextures.remove(textureName);
                fannedOut++;
            }
            entries.remove(template);
            entries.addAll(newEntries);
            System.out.println("Fanned " + fannedOut + " textures across placeholder for model " + modelName);
        }

        if (unmatchedListView != null) {
            unmatchedListView.getItems().setAll(unmatchedTextures);
        }
        refreshEntryList();
        updateIconListView();

        // Feedback when Auto-Match couldn't do anything visible.
        if (prefixMatched == 0 && fannedOut == 0) {
            if (placeholders.size() > 1 && !unmatchedTextures.isEmpty()) {
                showAlert("Multiple Placeholders",
                        unmatchedTextures.size() + " textures still unmatched. With more than one placeholder entry, Auto-Match can't pick a target safely — use Pair Selected to fan textures across the entries you choose.");
            } else if (!unmatchedTextures.isEmpty()) {
                showAlert("Nothing to Match",
                        unmatchedTextures.size() + " textures have no model-prefix match and no placeholder entry to fan them out to. Drop a matching model, or use Pair Selected for manual pairing.");
            } else {
                showAlert("Nothing to Match",
                        "No unmatched textures to process.");
            }
        }
    }

    // --- Sort ---

    public void sortEntriesByDecoref(boolean ascending) {
        sortEntriesBy(java.util.Comparator
                .comparing((DecoEntry e) -> nullSafe(e.getDecoref()), String.CASE_INSENSITIVE_ORDER),
                ascending);
    }

    public void sortEntriesByName(boolean ascending) {
        sortEntriesBy(java.util.Comparator
                .comparing((DecoEntry e) -> nullSafe(e.getName()), String.CASE_INSENSITIVE_ORDER),
                ascending);
    }

    public void sortEntriesByModelThenDecoref() {
        java.util.Comparator<DecoEntry> cmp = java.util.Comparator
                .comparing((DecoEntry e) -> nullSafe(e.getModel()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(e -> nullSafe(e.getDecoref()), String.CASE_INSENSITIVE_ORDER);
        sortEntriesBy(cmp, true);
    }

    private void sortEntriesBy(java.util.Comparator<DecoEntry> cmp, boolean ascending) {
        if (entries.isEmpty()) return;
        snapshot();
        List<DecoEntry> sorted = new ArrayList<>(entries);
        sorted.sort(ascending ? cmp : cmp.reversed());
        entries.setAll(sorted);
        refreshEntryList();
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    /** One draggable, tickable row in a Sort/Chain dialog. Tag identifies the axis. */
    private static final class AxisToggle {
        final Object tag;
        final String label;
        final javafx.beans.property.BooleanProperty enabled;
        AxisToggle(Object tag, String label, boolean on) {
            this.tag = tag;
            this.label = label;
            this.enabled = new javafx.beans.property.SimpleBooleanProperty(on);
        }
    }

    /**
     * Sort dialog: the axes are draggable boxes in a list — drag to set their
     * weight against each other (top = applied first), tick to include. An
     * entry that carries several axes (e.g. metal + colour) falls through to
     * the next ticked axis. Default order reproduces the canonical per-model
     * finish sort (Group → … → Metal → Colour).
     */
    public void openSortDialog() {
        if (entries.isEmpty()) {
            showAlert("Nothing to Sort", "Drop some models/textures first — there are no entries to sort.");
            return;
        }

        ObservableList<AxisToggle> axes = FXCollections.observableArrayList(
                new AxisToggle(EntrySorter.Key.GROUP, "Group — keep same-model variants together", true),
                new AxisToggle(EntrySorter.Key.ALPHABETICAL, "Alphabetical — base name", true),
                new AxisToggle(EntrySorter.Key.NUMBER, "Number — e.g. holder 1, 2, 3", true),
                new AxisToggle(EntrySorter.Key.METAL, "Metal colour — silver, gold, rose gold, brass", true),
                new AxisToggle(EntrySorter.Key.WOOD, "Wood colour — birch, oak, cherry, palm, spruce, ebony", true),
                new AxisToggle(EntrySorter.Key.COLOUR, "Colour — black, green, white, then rainbow", true)
        );

        ListView<AxisToggle> list = new ListView<>(axes);
        list.setPrefHeight(220);
        list.setCellFactory(lv -> makeAxisCell(axes));

        Label hint = new Label("Drag the boxes to weight them — top is applied first. "
                + "Untick an axis to ignore it; an entry with several (metal + colour) "
                + "uses the next ticked axis to break ties.");
        hint.setWrapText(true);
        hint.setStyle("-fx-text-fill: #1a1a1a;");
        CheckBox reverse = new CheckBox("Reverse (descending)");

        VBox content = new VBox(8, hint, list, reverse);
        content.setPadding(new Insets(10));
        content.setPrefWidth(360);

        Dialog<ButtonType> dialog = new Dialog<>();
        if (primaryStage != null) {
            dialog.initOwner(primaryStage);
            dialog.initModality(Modality.WINDOW_MODAL);
        }
        dialog.setTitle("Sort Entries");
        dialog.setHeaderText("Drag to weight the sort axes against each other.");
        dialog.getDialogPane().setContent(content);

        ButtonType applyBtn = new ButtonType("Sort", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyBtn, ButtonType.CANCEL);

        Optional<ButtonType> result = runPreservingStage(dialog::showAndWait);
        if (result.isEmpty() || result.get() != applyBtn) return;

        List<EntrySorter.Key> order = new ArrayList<>();
        for (AxisToggle a : axes) {
            if (a.enabled.get()) order.add((EntrySorter.Key) a.tag);
        }
        if (order.isEmpty()) {
            showAlert("Sort Entries", "Tick at least one axis to sort by.");
            return;
        }

        snapshot();
        entries.setAll(EntrySorter.sorted(entries, order, reverse.isSelected()));
        refreshEntryList();
    }

    /** A draggable, tickable cell for a Sort/Chain dialog's axis list. */
    private ListCell<AxisToggle> makeAxisCell(ObservableList<AxisToggle> axes) {
        ListCell<AxisToggle> cell = new ListCell<>() {
            private final CheckBox check = new CheckBox();
            @Override
            protected void updateItem(AxisToggle item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                check.setStyle("-fx-text-fill: black;");
                check.setText("☰  " + item.label);   // ≡ = drag handle
                check.setOnAction(null);
                check.setSelected(item.enabled.get());
                check.setOnAction(e -> item.enabled.set(check.isSelected()));
                setGraphic(check);
            }
        };

        cell.setOnDragDetected(ev -> {
            if (cell.isEmpty()) return;
            javafx.scene.input.Dragboard db = cell.startDragAndDrop(javafx.scene.input.TransferMode.MOVE);
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(Integer.toString(cell.getIndex()));
            db.setContent(cc);
            ev.consume();
        });
        cell.setOnDragOver(ev -> {
            if (ev.getGestureSource() != cell && ev.getDragboard().hasString()) {
                ev.acceptTransferModes(javafx.scene.input.TransferMode.MOVE);
            }
            ev.consume();
        });
        cell.setOnDragDropped(ev -> {
            if (!ev.getDragboard().hasString()) {
                ev.setDropCompleted(false);
                ev.consume();
                return;
            }
            int from = Integer.parseInt(ev.getDragboard().getString());
            if (from < 0 || from >= axes.size()) {
                ev.setDropCompleted(false);
                ev.consume();
                return;
            }
            int to = cell.isEmpty() ? axes.size() - 1 : cell.getIndex();
            AxisToggle moved = axes.remove(from);
            if (to > axes.size()) to = axes.size();
            axes.add(to, moved);
            ev.setDropCompleted(true);
            ev.consume();
        });
        return cell;
    }

    /**
     * Chain dialog (tool_modelswitch): the ordering axes are draggable boxes —
     * drag to weight them, top applied first. Entries are cycled within each
     * model (and each open/closed state, auto-detected) sorted by all ticked
     * axes combined, so Metal + Colour link in the canonical single cycle
     * Silver→Gold→Rose Gold→Brass→Black→Green→White. on_use state pairs are a
     * separate button, so an entry can carry both link types.
     */
    public void openChainDialog() {
        if (entries.isEmpty()) {
            showAlert("Nothing to Chain", "There are no entries to chain.");
            return;
        }

        ObservableList<AxisToggle> axes = FXCollections.observableArrayList(
                new AxisToggle(ChainBuilder.ChainAxis.METAL, "Metal colour — silver, gold, rose gold, brass", true),
                new AxisToggle(ChainBuilder.ChainAxis.COLOUR, "Colour — black, green, white, then rainbow", true),
                new AxisToggle(ChainBuilder.ChainAxis.WOOD, "Wood type — birch, oak, cherry, palm, spruce, ebony", false),
                new AxisToggle(ChainBuilder.ChainAxis.ORDER, "Current list order", false)
        );

        ListView<AxisToggle> list = new ListView<>(axes);
        list.setPrefHeight(180);
        list.setCellFactory(lv -> makeAxisCell(axes));

        Label hint = new Label("Drag to weight the axes (top applied first). Entries are cycled "
                + "per model — and per open/closed state — sorted by all ticked axes combined "
                + "(so Metal + Colour give one Silver→…→White cycle). Sort first, then chain by "
                + "\"Current list order\" to lock in a hand-arranged sequence. Open↔closed on_use "
                + "linking is the separate \"Pair States\" button.");
        hint.setWrapText(true);
        hint.setStyle("-fx-text-fill: #1a1a1a;");

        VBox content = new VBox(8, hint, list);
        content.setPadding(new Insets(10));
        content.setPrefWidth(380);

        Dialog<ButtonType> dialog = new Dialog<>();
        if (primaryStage != null) {
            dialog.initOwner(primaryStage);
            dialog.initModality(Modality.WINDOW_MODAL);
        }
        dialog.setTitle("Build Chain");
        dialog.setHeaderText("Drag to weight the chain axes against each other.");
        dialog.getDialogPane().setContent(content);
        ButtonType buildBtn = new ButtonType("Build Chain", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(buildBtn, ButtonType.CANCEL);

        Optional<ButtonType> result = runPreservingStage(dialog::showAndWait);
        if (result.isEmpty() || result.get() != buildBtn) return;

        List<ChainBuilder.ChainAxis> orderingAxes = new ArrayList<>();
        for (AxisToggle a : axes) {
            if (a.enabled.get() && a.tag instanceof ChainBuilder.ChainAxis) {
                orderingAxes.add((ChainBuilder.ChainAxis) a.tag);
            }
        }
        if (orderingAxes.isEmpty()) {
            showAlert("Build Chain", "Tick at least one axis to chain by.");
            return;
        }

        snapshot();
        List<DecoEntry> chained = ChainBuilder.buildWeightedChain(new ArrayList<>(entries), orderingAxes);
        entries.setAll(chained);
        refreshEntryList();
        updateJsonPreview();

        String cycledBy = orderingAxes.stream().map(x -> x.name().toLowerCase()).reduce((x, y) -> x + " → " + y).orElse("");
        showAlert("Chain Built", "Cycled per model by: " + cycledBy + " (tool_modelswitch).");
    }

    public void pairSelectedTexturesWithModel() {
        List<String> selectedTextures = new ArrayList<>(unmatchedListView.getSelectionModel().getSelectedItems());
        if (selectedTextures.isEmpty()) {
            showAlert("No Textures Selected", "Select textures from the Unmatched Textures list first.");
            return;
        }

        // Multi-select: one template entry per model. Selecting two closet_7
        // and one closet_7_open produces two buckets (closet_7, closet_7_open)
        // and each texture spawns an entry in every bucket.
        List<DecoEntry> selectedEntries = new ArrayList<>(entryListView.getSelectionModel().getSelectedItems());
        java.util.LinkedHashMap<String, DecoEntry> templatesByModel = new java.util.LinkedHashMap<>();
        for (DecoEntry e : selectedEntries) {
            if (e == null || e.getModel() == null) continue;
            templatesByModel.putIfAbsent(e.getModel(), e);
        }
        if (templatesByModel.isEmpty()) {
            showAlert("No Model Selected", "Select one or more model entries from the Entries list first.");
            return;
        }

        snapshot();
        List<DecoEntry> newEntries = new ArrayList<>();

        for (String textureName : selectedTextures) {
            String suffix = TextureMatcher.extractColorSuffix(textureName);

            for (Map.Entry<String, DecoEntry> tpl : templatesByModel.entrySet()) {
                String modelName = tpl.getKey();
                DecoEntry templateEntry = tpl.getValue();

                String iconName = TextureMatcher.findMatchingIcon(modelName, suffix, textureFiles.keySet());

                DecoEntry entry = History.deepCopy(templateEntry, DecoEntry.class);
                String displayName = EntryBuilder.toDisplayName(modelName + "_" + suffix);
                String decoref = (iconName != null) ? iconName : (modelName + "_" + suffix);
                entry.setDecoref(decoref);
                entry.setName(displayName);
                entry.setModel(modelName);
                entry.setMaterial(textureName);

                newEntries.add(entry);

                System.out.println("Paired: " + textureName + " -> " + modelName +
                        (iconName != null ? " (icon: " + iconName + ")" : ""));
            }

            unmatchedTextures.remove(textureName);
        }

        // Remove any placeholder templates (material == null) whose model
        // we just populated.
        for (DecoEntry templateEntry : templatesByModel.values()) {
            if (templateEntry.getMaterial() == null) {
                entries.remove(templateEntry);
            }
        }

        entries.addAll(newEntries);
        unmatchedListView.getItems().setAll(unmatchedTextures);
        refreshEntryList();
        updateIconListView();

        entryListView.getSelectionModel().clearSelection();
        for (DecoEntry entry : newEntries) {
            entryListView.getSelectionModel().select(entry);
        }

        long matchedIcons = newEntries.stream().filter(e -> iconFiles.containsKey(e.getDecoref())).count();
        String modelsLabel = String.join(", ", templatesByModel.keySet());
        showAlert("Pairing Complete",
                String.format("Created %d entries across %d model%s (%s)\n(%d with matched icons, %d with generated decorefs)",
                        newEntries.size(),
                        templatesByModel.size(), templatesByModel.size() == 1 ? "" : "s",
                        modelsLabel,
                        matchedIcons, newEntries.size() - matchedIcons));
    }

    /**
     * Assign a dropped texture file to a specific entry.
     */
    /**
     * Drop a set of unmatched textures: remove from the unmatched list and
     * from the known-texture-files map so the user can re-drop them cleanly
     * later if needed.
     */
    public void removeUnmatchedTextures(List<String> names) {
        if (names == null || names.isEmpty()) return;
        snapshot();
        for (String name : names) {
            unmatchedTextures.remove(name);
            textureFiles.remove(name);
        }
        if (unmatchedListView != null) {
            unmatchedListView.getItems().setAll(unmatchedTextures);
        }
    }

    public void assignTextureToEntry(DecoEntry entry, File textureFile) {
        snapshot();
        String textureName = DirectoryScanner.getStem(textureFile.toPath());
        entry.setMaterial(textureName);
        textureFiles.put(textureName, textureFile.toPath());
        unmatchedTextures.remove(textureName);
        refreshEntryList();
    }

    // --- Chain actions ---

    public void buildRainbowChain() {
        if (entries.isEmpty()) {
            showAlert("No Entries", "Add some entries first.");
            return;
        }

        snapshot();
        List<DecoEntry> sorted = ChainBuilder.buildRainbowChains(new ArrayList<>(entries));
        entries.clear();
        entries.addAll(sorted);

        refreshEntryList();

        showAlert("Rainbow Chain Built",
                "Entries have been sorted and linked in rainbow color order.\n" +
                        "Check the JSON for 'tool_modelswitch' links.");
    }

    public void linkEntriesByOrder() {
        if (entries.isEmpty()) {
            showAlert("No Entries", "Add some entries first.");
            return;
        }
        snapshot();
        int linked = ChainBuilder.linkByOrder(entries);
        refreshEntryList();
        if (linked < 2) {
            showAlert("Link by Order", "Need at least 2 entries with a decoref or material to link.");
        } else {
            showAlert("Linked by Order",
                    "Linked " + linked + " entries in current list order (tool_modelswitch).");
        }
    }

    public void buildWoodChain() {
        if (entries.isEmpty()) {
            showAlert("No Entries", "Add some entries first.");
            return;
        }

        snapshot();
        List<DecoEntry> sorted = ChainBuilder.buildWoodChains(new ArrayList<>(entries));
        entries.clear();
        entries.addAll(sorted);

        refreshEntryList();

        showAlert("Wood Chain Built",
                "Entries have been sorted and linked in wood type order.\n" +
                        "Check the JSON for 'tool_modelswitch' links.");
    }

    /**
     * Find each pair of entries that share a base decoref and a suffix variant
     * (e.g., closet_7_birch + closet_7_birch_open) and write bidirectional
     * on_use.link between them. Does not touch tool_modelswitch.
     */
    public void linkStatePairs() {
        if (entries.isEmpty()) {
            showAlert("Link Pairs", "No entries to link.");
            return;
        }

        ComboBox<String> suffixCombo = new ComboBox<>();
        suffixCombo.getItems().addAll(ChainBuilder.KNOWN_STATE_SUFFIXES);
        suffixCombo.setValue("open");
        suffixCombo.setEditable(true);
        suffixCombo.setMaxWidth(Double.MAX_VALUE);
        suffixCombo.setPromptText("e.g., open");

        VBox content = new VBox(8,
                new Label("State suffix:"), suffixCombo,
                new Label("Pairs like 'closet_7_birch' \u2194 'closet_7_birch_open' get\nbidirectional on_use.link. Run Rainbow/Wood Chain\nafter this to cycle colors within each state."));
        content.setPadding(new Insets(10));

        Dialog<ButtonType> dialog = new Dialog<>();
        if (primaryStage != null) {
            dialog.initOwner(primaryStage);
            dialog.initModality(Modality.WINDOW_MODAL);
        }
        dialog.setTitle("Link Pairs");
        dialog.setHeaderText("Link base \u2194 variant entries via on_use.");
        dialog.getDialogPane().setContent(content);

        ButtonType applyBtn = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyBtn, ButtonType.CANCEL);

        Optional<ButtonType> result = runPreservingStage(dialog::showAndWait);
        if (result.isEmpty() || result.get() != applyBtn) return;

        String suffix = suffixCombo.getValue();
        if (suffix == null) suffix = suffixCombo.getEditor().getText();
        if (suffix == null || suffix.trim().isEmpty()) return;

        snapshot();
        int pairs = ChainBuilder.linkStatePairs(new ArrayList<>(entries), suffix.trim());
        refreshEntryList();

        if (pairs == 0) {
            showAlert("Link Pairs",
                    "No pairs found for suffix '_" + suffix.trim() + "'.\n" +
                            "Make sure both a base entry and a 'base_" + suffix.trim() + "' entry exist.");
        } else {
            showAlert("Link Pairs",
                    "Linked " + pairs + " pair" + (pairs == 1 ? "" : "s") +
                            " via on_use (suffix '_" + suffix.trim() + "').");
        }
    }

    /** on_use pairing "by order": directed right-click cycle in list order. */
    public void pairByOrder() {
        if (entries.isEmpty()) {
            showAlert("Pair by Order", "There are no entries to pair.");
            return;
        }
        snapshot();
        int n = ChainBuilder.pairByOrder(new ArrayList<>(entries));
        refreshEntryList();
        updateJsonPreview();
        showAlert("Pair by Order", n == 0
                ? "Need at least two entries to pair."
                : "Linked " + n + " entries in a right-click on_use cycle (arrange the list order first).");
    }

    /**
     * on_use pairing dialog: pick a mode (By State / By Order / By Sequential)
     * via radio buttons — a popup like the Chain dialog, not a dropdown. The
     * state-suffix field is only enabled for "By State".
     */
    public void openPairDialog() {
        if (entries.isEmpty()) {
            showAlert("Nothing to Pair", "There are no entries to pair.");
            return;
        }

        ToggleGroup group = new ToggleGroup();
        RadioButton byState = new RadioButton("By State — base ↔ open / on / closed variant");
        RadioButton byOrder = new RadioButton("By Order — cycle every entry in the current list order");
        RadioButton bySeq = new RadioButton("By Sequential — numbered steps (…_open_1 → …_2 → …_3)");
        for (RadioButton r : new RadioButton[]{byState, byOrder, bySeq}) {
            r.setToggleGroup(group);
        }
        byState.setSelected(true);

        Label suffixLabel = new Label("State suffix:");
        ComboBox<String> suffixCombo = new ComboBox<>();
        suffixCombo.setEditable(true);
        suffixCombo.getItems().addAll(ChainBuilder.KNOWN_STATE_SUFFIXES);
        suffixCombo.setValue("open");
        suffixCombo.setMaxWidth(Double.MAX_VALUE);
        Runnable syncSuffix = () -> {
            boolean s = byState.isSelected();
            suffixLabel.setDisable(!s);
            suffixCombo.setDisable(!s);
        };
        group.selectedToggleProperty().addListener((o, a, b) -> syncSuffix.run());
        syncSuffix.run();

        VBox content = new VBox(8,
                new Label("Link entries with on_use (right-click). Independent of the\n"
                        + "tool_modelswitch chain, so an entry can carry both."),
                byState, byOrder, bySeq,
                new Separator(), suffixLabel, suffixCombo);
        content.setPadding(new Insets(10));

        Dialog<ButtonType> dialog = new Dialog<>();
        if (primaryStage != null) {
            dialog.initOwner(primaryStage);
            dialog.initModality(Modality.WINDOW_MODAL);
        }
        dialog.setTitle("Pair on_use");
        dialog.setHeaderText("Choose how to link entries by on_use.");
        dialog.getDialogPane().setContent(content);
        ButtonType pairBtn = new ButtonType("Pair", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(pairBtn, ButtonType.CANCEL);

        Optional<ButtonType> result = runPreservingStage(dialog::showAndWait);
        if (result.isEmpty() || result.get() != pairBtn) return;

        if (byOrder.isSelected()) {
            pairByOrder();
        } else if (bySeq.isSelected()) {
            pairSequential();
        } else {
            String suffix = suffixCombo.getValue();
            if (suffix == null) suffix = suffixCombo.getEditor().getText();
            if (suffix == null || suffix.trim().isEmpty()) {
                showAlert("Pair by State", "Enter a state suffix (e.g. open).");
                return;
            }
            snapshot();
            int pairs = ChainBuilder.linkStatePairs(new ArrayList<>(entries), suffix.trim());
            refreshEntryList();
            updateJsonPreview();
            showAlert("Pair by State", pairs == 0
                    ? "No pairs found for suffix '_" + suffix.trim() + "'.\n"
                            + "Make sure both a base entry and a 'base_" + suffix.trim() + "' entry exist."
                    : "Linked " + pairs + " pair" + (pairs == 1 ? "" : "s")
                            + " via on_use (suffix '_" + suffix.trim() + "').");
        }
    }

    /** on_use pairing "by sequential": numbered progressions (…_1 → …_2 → …_3 → …_1). */
    public void pairSequential() {
        if (entries.isEmpty()) {
            showAlert("Pair Sequential", "There are no entries to pair.");
            return;
        }
        snapshot();
        int linked = ChainBuilder.pairSequential(new ArrayList<>(entries));
        refreshEntryList();
        updateJsonPreview();
        showAlert("Pair Sequential", linked == 0
                ? "No numbered sequences found. Sequential pairs entries like\n"
                        + "drawer_1_open_1 → drawer_1_open_2 → drawer_1_open_3 by their trailing number."
                : "Linked " + linked + " entries into numbered on_use sequences (…_1 → …_2 → … → wraps).");
    }

    // --- Normalize ---

    /**
     * Open a dialog with (a) a dropdown of every distinct word found in entry
     * names, decorefs, and materials, and (b) checkboxes to pick which of
     * those fields to strip the word from. Material defaults to off because
     * materials are usually shared textures.
     */
    public void normalizeRemoveWord() {
        if (entries.isEmpty()) {
            showAlert("Normalize", "No entries to normalize.");
            return;
        }

        Set<String> tokens = new TreeSet<>();
        for (DecoEntry e : entries) {
            collectTokens(tokens, e.getName(), " ");
            collectTokens(tokens, e.getDecoref(), "_");
            collectTokens(tokens, e.getMaterial(), "_");
        }
        if (tokens.isEmpty()) {
            showAlert("Normalize", "Entries have no words to remove.");
            return;
        }

        List<String> choices = new ArrayList<>(tokens);

        ComboBox<String> wordCombo = new ComboBox<>();
        wordCombo.getItems().addAll(choices);
        wordCombo.setValue(choices.get(0));
        wordCombo.setEditable(true);
        wordCombo.setMaxWidth(Double.MAX_VALUE);

        CheckBox nameCheck = new CheckBox("Name");
        nameCheck.setSelected(true);
        CheckBox decorefCheck = new CheckBox("Decoref");
        decorefCheck.setSelected(true);
        CheckBox materialCheck = new CheckBox("Material (shared texture — usually off)");
        materialCheck.setSelected(false);

        VBox content = new VBox(8,
                new Label("Word:"), wordCombo,
                new Label("Strip from:"), nameCheck, decorefCheck, materialCheck);
        content.setPadding(new Insets(10));

        Dialog<ButtonType> dialog = new Dialog<>();
        if (primaryStage != null) {
            dialog.initOwner(primaryStage);
            dialog.initModality(Modality.WINDOW_MODAL);
        }
        dialog.setTitle("Normalize");
        dialog.setHeaderText("Pick a word and the fields to strip it from.");
        dialog.getDialogPane().setContent(content);

        ButtonType applyBtn = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyBtn, ButtonType.CANCEL);

        Optional<ButtonType> result = runPreservingStage(dialog::showAndWait);
        if (result.isEmpty() || result.get() != applyBtn) return;

        String word = wordCombo.getValue();
        if (word == null) word = wordCombo.getEditor().getText();
        if (word == null) return;
        word = word.trim();
        if (word.isEmpty()) return;

        boolean stripName = nameCheck.isSelected();
        boolean stripDecoref = decorefCheck.isSelected();
        boolean stripMaterial = materialCheck.isSelected();

        if (!stripName && !stripDecoref && !stripMaterial) {
            showAlert("Normalize", "Pick at least one field to strip from.");
            return;
        }

        snapshot();
        int modified = 0;
        for (DecoEntry entry : entries) {
            boolean changed = false;
            if (stripName) {
                String n = entry.getName();
                if (n != null) {
                    String s = stripToken(n, word, " ");
                    if (!s.equals(n)) { entry.setName(s); changed = true; }
                }
            }
            if (stripDecoref) {
                String d = entry.getDecoref();
                if (d != null) {
                    String s = stripToken(d, word, "_");
                    if (!s.equals(d)) { entry.setDecoref(s); changed = true; }
                }
            }
            if (stripMaterial) {
                String m = entry.getMaterial();
                if (m != null) {
                    String s = stripToken(m, word, "_");
                    if (!s.equals(m)) { entry.setMaterial(s); changed = true; }
                }
            }
            if (changed) modified++;
        }

        refreshEntryList();

        List<String> scopes = new ArrayList<>();
        if (stripName) scopes.add("name");
        if (stripDecoref) scopes.add("decoref");
        if (stripMaterial) scopes.add("material");
        showAlert("Normalize", "Removed '" + word + "' from " + String.join(", ", scopes)
                + " on " + modified + " entries.");
    }

    /**
     * Open a dialog to insert a word into every entry at a chosen position
     * (prepend, append, or a specific token index). Mirrors the field-scope
     * checkboxes used by {@link #normalizeRemoveWord()}.
     */
    public void normalizeAddWord() {
        if (entries.isEmpty()) {
            showAlert("Add Word", "No entries to modify.");
            return;
        }

        TextField wordField = new TextField();
        wordField.setPromptText("e.g., V2 or old");
        wordField.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> positionCombo = new ComboBox<>();
        positionCombo.getItems().addAll("At start", "At end", "Between two words");
        positionCombo.setValue("At start");
        positionCombo.setMaxWidth(Double.MAX_VALUE);

        TextField beforeField = new TextField();
        beforeField.setPromptText("word before (e.g., closet)");
        beforeField.setDisable(true);
        TextField afterField = new TextField();
        afterField.setPromptText("word after (e.g., white)");
        afterField.setDisable(true);

        positionCombo.valueProperty().addListener((obs, o, n) -> {
            boolean between = "Between two words".equals(n);
            beforeField.setDisable(!between);
            afterField.setDisable(!between);
        });

        CheckBox nameCheck = new CheckBox("Name");
        nameCheck.setSelected(true);
        CheckBox decorefCheck = new CheckBox("Decoref");
        decorefCheck.setSelected(true);
        CheckBox materialCheck = new CheckBox("Material (shared texture — usually off)");
        materialCheck.setSelected(false);

        VBox content = new VBox(8,
                new Label("Word to add:"), wordField,
                new Label("Position:"), positionCombo,
                new Label("Word before (for Between):"), beforeField,
                new Label("Word after (for Between):"), afterField,
                new Label("Add to:"), nameCheck, decorefCheck, materialCheck);
        content.setPadding(new Insets(10));

        Dialog<ButtonType> dialog = new Dialog<>();
        if (primaryStage != null) {
            dialog.initOwner(primaryStage);
            dialog.initModality(Modality.WINDOW_MODAL);
        }
        dialog.setTitle("Add Word");
        dialog.setHeaderText("Pick a word and where to insert it in every entry.");
        dialog.getDialogPane().setContent(content);

        ButtonType applyBtn = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyBtn, ButtonType.CANCEL);

        Optional<ButtonType> result = runPreservingStage(dialog::showAndWait);
        if (result.isEmpty() || result.get() != applyBtn) return;

        String rawWord = wordField.getText();
        if (rawWord == null) return;
        rawWord = rawWord.trim();
        if (rawWord.isEmpty()) return;

        boolean addName = nameCheck.isSelected();
        boolean addDecoref = decorefCheck.isSelected();
        boolean addMaterial = materialCheck.isSelected();

        if (!addName && !addDecoref && !addMaterial) {
            showAlert("Add Word", "Pick at least one field to add to.");
            return;
        }

        String pos = positionCombo.getValue();
        String beforeWord = null;
        String afterWord = null;
        if ("Between two words".equals(pos)) {
            beforeWord = beforeField.getText() != null ? beforeField.getText().trim() : "";
            afterWord = afterField.getText() != null ? afterField.getText().trim() : "";
            if (beforeWord.isEmpty() || afterWord.isEmpty()) {
                showAlert("Add Word", "For 'Between two words', fill in both the word before and the word after.");
                return;
            }
        }

        snapshot();
        // Name keeps original casing; decoref/material are always snake_case lowercase.
        String nameWord = rawWord;
        String idWord = rawWord.toLowerCase().replaceAll("\\s+", "_");

        int modified = 0;
        for (DecoEntry entry : entries) {
            boolean changed = false;
            if (addName && entry.getName() != null) {
                String s = insertToken(entry.getName(), nameWord, " ", pos, beforeWord, afterWord);
                if (!s.equals(entry.getName())) { entry.setName(s); changed = true; }
            }
            if (addDecoref && entry.getDecoref() != null) {
                String s = insertToken(entry.getDecoref(), idWord, "_", pos, beforeWord, afterWord);
                if (!s.equals(entry.getDecoref())) { entry.setDecoref(s); changed = true; }
            }
            if (addMaterial && entry.getMaterial() != null) {
                String s = insertToken(entry.getMaterial(), idWord, "_", pos, beforeWord, afterWord);
                if (!s.equals(entry.getMaterial())) { entry.setMaterial(s); changed = true; }
            }
            if (changed) modified++;
        }

        refreshEntryList();

        List<String> scopes = new ArrayList<>();
        if (addName) scopes.add("name");
        if (addDecoref) scopes.add("decoref");
        if (addMaterial) scopes.add("material");
        showAlert("Add Word", "Added '" + rawWord + "' to " + String.join(", ", scopes)
                + " on " + modified + " entries.");
    }

    /**
     * Insert {@code word} into {@code text} (split by {@code separator}) using
     * one of the three position modes:
     *   "At start"           — prepend
     *   "At end"             — append
     *   "Between two words"  — find the first adjacent pair whose left token
     *                          matches {@code beforeWord} and right token
     *                          matches {@code afterWord} (case-insensitive),
     *                          insert between them. If no such pair exists,
     *                          the text is returned unchanged (entry skipped).
     */
    private static String insertToken(String text, String word, String separator,
                                      String positionMode, String beforeWord, String afterWord) {
        String[] parts = text.split(Pattern.quote(separator));
        List<String> tokens = new ArrayList<>();
        for (String p : parts) if (!p.isEmpty()) tokens.add(p);

        int insertAt;
        if ("At end".equals(positionMode)) {
            insertAt = tokens.size();
        } else if ("Between two words".equals(positionMode)) {
            insertAt = -1;
            for (int i = 0; i < tokens.size() - 1; i++) {
                if (tokens.get(i).equalsIgnoreCase(beforeWord)
                        && tokens.get(i + 1).equalsIgnoreCase(afterWord)) {
                    insertAt = i + 1;
                    break;
                }
            }
            if (insertAt < 0) return text;
        } else {
            // default: At start
            insertAt = 0;
        }

        tokens.add(insertAt, word);
        return String.join(separator, tokens);
    }

    private static void collectTokens(Set<String> out, String text, String separator) {
        if (text == null) return;
        for (String t : text.split(Pattern.quote(separator))) {
            String lower = t.trim().toLowerCase();
            if (!lower.isEmpty()) out.add(lower);
        }
    }

    private static String stripToken(String text, String word, String separator) {
        String[] parts = text.split(Pattern.quote(separator));
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (p.equalsIgnoreCase(word)) continue;
            if (sb.length() > 0) sb.append(separator);
            sb.append(p);
        }
        return sb.toString();
    }

    // --- Export / reset ---

    /**
     * Import dialog: paste a JSON array (or object) of Decocraft entries. The
     * entries load into the editable list (replacing or appended), and the JSON
     * preview re-formats them. Foreign fields the tool doesn't model are dropped.
     */
    public void importJson() {
        TextArea area = new TextArea();
        area.setPromptText("Paste a JSON array of entries here…");
        area.setPrefRowCount(18);
        area.setPrefColumnCount(64);
        area.setStyle("-fx-font-family: 'monospace'; -fx-control-inner-background: white; -fx-text-fill: black;");

        CheckBox replaceCheck = new CheckBox("Replace current entries (otherwise append)");
        replaceCheck.setSelected(entries.isEmpty());

        VBox content = new VBox(8, new Label("Paste Decocraft entry JSON:"), area, replaceCheck);
        content.setPadding(new Insets(10));

        Dialog<ButtonType> dialog = new Dialog<>();
        if (primaryStage != null) {
            dialog.initOwner(primaryStage);
            dialog.initModality(Modality.WINDOW_MODAL);
        }
        dialog.setTitle("Import JSON");
        dialog.setHeaderText("Paste JSON to load entries for editing.");
        dialog.getDialogPane().setContent(content);
        dialog.setResizable(true);
        ButtonType importBtn = new ButtonType("Import", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(importBtn, ButtonType.CANCEL);

        Optional<ButtonType> result = runPreservingStage(dialog::showAndWait);
        if (result.isEmpty() || result.get() != importBtn) return;

        String text = area.getText();
        if (text == null || text.trim().isEmpty()) {
            showAlert("Import JSON", "Nothing to import — paste some JSON first.");
            return;
        }

        List<DecoEntry> parsed;
        try {
            parsed = JsonImporter.parse(text);
        } catch (Exception ex) {
            showAlert("Import Failed", "Couldn't parse the JSON:\n" + ex.getMessage());
            return;
        }
        if (parsed.isEmpty()) {
            showAlert("Import JSON", "No entries found in the pasted JSON.");
            return;
        }

        snapshot();
        if (replaceCheck.isSelected()) entries.setAll(parsed);
        else entries.addAll(parsed);
        refreshEntryList();
        updateJsonPreview();
        showAlert("Import Complete",
                "Imported " + parsed.size() + " entr" + (parsed.size() == 1 ? "y" : "ies") + ".");
    }

    public void exportJson(Stage stage) {
        if (entries.isEmpty()) {
            showAlert("No Entries", "Add some models and textures first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export JSON");
        fileChooser.setInitialFileName("decocraft_entries.json");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"));

        File file = runPreservingStage(() -> fileChooser.showSaveDialog(stage));
        if (file != null) {
            try {
                JsonExporter.export(new ArrayList<>(entries), file.toPath());
                showAlert("Export Complete", "Saved to: " + file.getName());
            } catch (Exception e) {
                showAlert("Export Failed", e.getMessage());
            }
        }
    }

    public void resetAll() {
        snapshot();
        entries.clear();
        modelFiles.clear();
        textureFiles.clear();
        iconFiles.clear();
        unmatchedTextures.clear();
        parsedModels.clear();
        if (entryListView != null) entryListView.refresh();
        if (iconListView != null) iconListView.getItems().clear();
        updateJsonPreview();
    }

    public void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (primaryStage != null) {
            alert.initOwner(primaryStage);
            alert.initModality(Modality.WINDOW_MODAL);
        }
        runPreservingStage(alert::showAndWait);
    }

    // Guard the owner stage against Linux window-manager resizing while a modal
    // dialog / native chooser is open. See StageGuard for the full rationale.
    private <T> T runPreservingStage(java.util.function.Supplier<T> popup) {
        return StageGuard.runPreserving(primaryStage, popup);
    }

    // --- Updates ---

    public void updateJsonPreview() {
        if (jsonPreview == null) return;

        if (entries.isEmpty()) {
            jsonPreview.setText("// Drop models and textures to generate entries");
            return;
        }

        String json = JsonExporter.toJson(new ArrayList<>(entries));
        jsonPreview.setText(json);
    }

    public void updateIconListView() {
        if (iconListView == null) return;

        List<String> iconDisplay = new ArrayList<>();
        for (String iconName : iconFiles.keySet()) {
            boolean used = entries.stream().anyMatch(e -> iconName.equals(e.getDecoref()));
            String status = used ? "\u2713 " : "\u25cb ";
            iconDisplay.add(status + iconName);
        }
        Collections.sort(iconDisplay);
        iconListView.getItems().setAll(iconDisplay);

        if (iconLabel != null) {
            long usedCount = entries.stream()
                    .filter(e -> e.getDecoref() != null && iconFiles.containsKey(e.getDecoref()))
                    .count();
            iconLabel.setText("Dropped Icons (" + usedCount + "/" + iconFiles.size() + " used):");
        }
    }

    private void refreshEntryList() {
        if (entryListView != null) entryListView.refresh();
        updateJsonPreview();
    }
}