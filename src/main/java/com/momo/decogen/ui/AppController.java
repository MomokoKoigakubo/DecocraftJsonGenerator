package com.momo.decogen.ui;

import com.momo.decogen.bbmodel.BBModel;
import com.momo.decogen.bbmodel.BBModelParser;
import com.momo.decogen.io.DirectoryScanner;
import com.momo.decogen.io.JsonExporter;
import com.momo.decogen.logic.ChainBuilder;
import com.momo.decogen.logic.EntryBuilder;
import com.momo.decogen.logic.History;
import com.momo.decogen.logic.TextureMatcher;
import com.momo.decogen.model.DecoEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
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

    // --- Registration ---
    public void setEntryListView(ListView<DecoEntry> v) { this.entryListView = v; }
    public void setUnmatchedListView(ListView<String> v) { this.unmatchedListView = v; }
    public void setIconListView(ListView<String> v) { this.iconListView = v; }
    public void setIconLabel(Label l) { this.iconLabel = l; }
    public void setJsonPreview(TextArea a) { this.jsonPreview = a; }
    public void setEditorPanel(EditorPanel p) { this.editorPanel = p; }

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

            // 1) Sweep unmatched textures for this model: try to fill an empty
            //    icon entry first, else create a new texture entry.
            for (String textureName : new ArrayList<>(unmatchedTextures)) {
                String tm = TextureMatcher.findMatchingModel(textureName, modelFiles.keySet());
                if (tm == null || !tm.equals(modelName)) continue;

                DecoEntry emptyIcon = findEmptyIconEntryForTexture(modelName, textureName);
                if (emptyIcon != null) {
                    emptyIcon.setMaterial(textureName);
                    unmatchedTextures.remove(textureName);
                } else {
                    createEntryForTexture(textureName, modelName);
                }
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

        String matchedModel = TextureMatcher.findMatchingModel(textureName, modelFiles.keySet());
        if (matchedModel == null) {
            unmatchedTextures.add(textureName);
            if (unmatchedListView != null) {
                unmatchedListView.getItems().setAll(unmatchedTextures);
            }
            System.out.println("Added unmatched texture: " + textureName);
            return;
        }

        // Prefer to fill an existing empty icon entry so we don't duplicate.
        DecoEntry emptyIcon = findEmptyIconEntryForTexture(matchedModel, textureName);
        if (emptyIcon != null) {
            emptyIcon.setMaterial(textureName);
            unmatchedTextures.remove(textureName);
            refreshEntryList();
            System.out.println("Filled icon entry " + emptyIcon.getDecoref() + " <- " + textureName);
            return;
        }

        // No icon entry waiting — create a fresh entry, and clear any placeholder
        // for this model that we would otherwise be shadowing.
        removePlaceholdersForModel(matchedModel);
        createEntryForTexture(textureName, matchedModel);
        System.out.println("Created entry: " + textureName + " -> " + matchedModel);
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
     * Given an icon name "model_suffix", find an unmatched texture whose name
     * ends with that suffix (so it can become the icon entry's material).
     */
    private String findUnmatchedTextureForIcon(String iconName, String modelName) {
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

    // --- Auto-match / pair ---

    public void autoMatchTextures() {
        snapshot();
        for (String textureName : new ArrayList<>(unmatchedTextures)) {
            for (DecoEntry entry : entries) {
                if (entry.getMaterial() == null && TextureMatcher.textureMatchesModel(textureName, entry.getModel())) {
                    entry.setMaterial(textureName);
                    unmatchedTextures.remove(textureName);
                    break;
                }
            }
        }

        for (String textureName : new ArrayList<>(unmatchedTextures)) {
            for (String modelName : modelFiles.keySet()) {
                if (TextureMatcher.textureMatchesModel(textureName, modelName)) {
                    String displayName = EntryBuilder.toDisplayName(textureName);
                    DecoEntry entry = new DecoEntry(displayName, modelName, textureName, "clutter");
                    entries.add(entry);
                    unmatchedTextures.remove(textureName);
                    System.out.println("Created entry for texture: " + textureName);
                    break;
                }
            }
        }

        refreshEntryList();
    }

    public void pairSelectedTexturesWithModel() {
        List<String> selectedTextures = new ArrayList<>(unmatchedListView.getSelectionModel().getSelectedItems());
        if (selectedTextures.isEmpty()) {
            showAlert("No Textures Selected", "Select textures from the Unmatched Textures list first.");
            return;
        }

        DecoEntry templateEntry = entryListView.getSelectionModel().getSelectedItem();
        if (templateEntry == null || templateEntry.getModel() == null) {
            showAlert("No Model Selected", "Select a model entry from the Entries list first.");
            return;
        }

        snapshot();
        String modelName = templateEntry.getModel();
        List<DecoEntry> newEntries = new ArrayList<>();

        for (String textureName : selectedTextures) {
            String suffix = TextureMatcher.extractColorSuffix(textureName);
            String iconName = TextureMatcher.findMatchingIcon(modelName, suffix, textureFiles.keySet());

            DecoEntry entry = new DecoEntry();
            String displayName = EntryBuilder.toDisplayName(modelName + "_" + suffix);
            String decoref = (iconName != null) ? iconName : (modelName + "_" + suffix);
            entry.setDecoref(decoref);
            entry.setName(displayName);
            entry.setModel(modelName);
            entry.setMaterial(textureName);
            entry.setTabs(templateEntry.getTabs());
            entry.setType(templateEntry.getType());
            entry.setScale(templateEntry.getScale());
            entry.setTransparency(templateEntry.getTransparency());
            entry.setHidden(templateEntry.getHidden());
            entry.setCraftingColor(templateEntry.getCraftingColor().clone());

            newEntries.add(entry);
            unmatchedTextures.remove(textureName);

            System.out.println("Paired: " + textureName + " -> " + modelName +
                    (iconName != null ? " (icon: " + iconName + ")" : ""));
        }

        if (templateEntry.getMaterial() == null) {
            entries.remove(templateEntry);
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
        showAlert("Pairing Complete",
                String.format("Created %d entries for model '%s'\n(%d with matched icons, %d with generated decorefs)",
                        newEntries.size(), modelName, matchedIcons, newEntries.size() - matchedIcons));
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
        dialog.setTitle("Normalize");
        dialog.setHeaderText("Pick a word and the fields to strip it from.");
        dialog.getDialogPane().setContent(content);

        ButtonType applyBtn = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyBtn, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
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
        dialog.setTitle("Add Word");
        dialog.setHeaderText("Pick a word and where to insert it in every entry.");
        dialog.getDialogPane().setContent(content);

        ButtonType applyBtn = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyBtn, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
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

        File file = fileChooser.showSaveDialog(stage);
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
        alert.showAndWait();
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