package com.momo.decogen.ui;

import com.momo.decogen.bbmodel.BBModel;
import com.momo.decogen.logic.DecoTypes;
import com.momo.decogen.logic.ModelInspector;
import com.momo.decogen.logic.Tabs;
import com.momo.decogen.model.Action;
import com.momo.decogen.model.AnimationPair;
import com.momo.decogen.model.Composite;
import com.momo.decogen.model.DecoEntry;
import com.momo.decogen.model.Flipbook;
import com.momo.decogen.model.Script;
import com.momo.decogen.model.SoundPair;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EditorPanel {

    private final AppController controller;
    private final VBox root;          // outer wrapper (scroll pane + buttons)
    private final VBox form;          // inside scroll pane
    private final Label editorTitle;

    // Identity
    private final TextField nameField;
    private final ComboBox<String> decorefDropdown;
    private final ComboBox<String> materialDropdown;

    // Rendering
    private final TextField modelField;
    private final Spinner<Double> scaleSpinner;
    private final TextField shapeField;
    private final CheckBox transparencyCheck;
    private final CheckBox cullingDisableCheck;

    // Display
    private final ComboBox<String> tabsDropdown;
    private final ComboBox<String> typeDropdown;
    private final ComboBox<String> defaultAnimationDropdown;

    // Behavior
    private final CheckBox passableCheck;
    private final CheckBox aboveWaterCheck;
    private final CheckBox rotatableCheck;
    private final CheckBox hiddenCheck;
    private final Label lootLabel;
    private final TextField lootField;
    private final CheckBox displayableCheck;

    // Composite
    private final CheckBox compositeCheck;
    private final VBox compositeBox;
    private final TextField compositeModelField;
    private final ComboBox<String> compositeTextureDropdown;
    private final CheckBox compositeTransparencyCheck;

    // Flipbook
    private final CheckBox flipbookCheck;
    private final VBox flipbookBox;
    private final Spinner<Integer> flipbookFrametimeSpinner;
    private final Spinner<Integer> flipbookImagesSpinner;

    // Chain
    private final TextArea chainModelsArea;
    private final TextArea chainMaterialsArea;
    private final ComboBox<String> chainPatternDropdown;
    private final CheckBox lightingCheck;
    private final Spinner<Integer> lightingSpinner;
    private final VBox lightingRow;

    // Growable
    private final TextArea structuresArea;
    private final CheckBox instantCheck;

    // Script header
    private final CheckBox lightCheck;
    private final Spinner<Integer> lightSpinner;
    private final VBox lightRow;
    private final CheckBox counterCheck;
    private final Spinner<Integer> counterSpinner;
    private final VBox counterRow;

    // Particle hint (from model inspection)
    private final Label particleHintLabel;

    // Script actions
    private final ActionEditor onUseEditor;
    private final ActionEditor shiftOnUseEditor;
    private final ActionEditor addedEditor;
    private final ActionEditor triggerEditor;
    private final ActionEditor animationStartEditor;
    private final ActionEditor animationEndEditor;
    private final ActionEditor toolModelSwitchEditor;
    private final List<ActionEditor> allActionEditors;

    public EditorPanel(AppController controller) {
        this.controller = controller;

        root = new VBox(6);
        root.setPadding(new Insets(10));
        root.setPrefWidth(320);
        root.setStyle("-fx-background-color: #2f3136;");

        editorTitle = new Label("Entry Editor");
        editorTitle.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label multiSelectHint = new Label("(Empty text fields won't change selected entries)");
        multiSelectHint.setStyle("-fx-text-fill: #72767d; -fx-font-size: 10px;");
        multiSelectHint.setWrapText(true);

        form = new VBox(6);

        // --- Identity ---
        form.getChildren().add(sectionHeader("Identity"));
        nameField = textField("Name");
        decorefDropdown = editableCombo("Decoref (unique id)");
        materialDropdown = editableCombo("Select or type texture name");
        form.getChildren().addAll(
                fieldLabel("Name"), nameField,
                fieldLabel("Decoref"), decorefDropdown,
                fieldLabel("Material"), materialDropdown
        );

        // --- Rendering ---
        form.getChildren().add(sectionHeader("Model & Rendering"));
        modelField = textField("Model (.bbmodel name)");
        scaleSpinner = new Spinner<>(0.1, 10.0, 1.0, 0.1);
        scaleSpinner.setEditable(true);
        scaleSpinner.setMaxWidth(Double.MAX_VALUE);
        shapeField = textField("Shape (custom hitbox bbmodel name)");
        transparencyCheck = darkCheck("Transparency");
        cullingDisableCheck = darkCheck("Disable face culling (emit culling: false)");
        form.getChildren().addAll(
                fieldLabel("Model"), modelField,
                fieldLabel("Scale"), scaleSpinner,
                fieldLabel("Shape"), shapeField,
                transparencyCheck,
                cullingDisableCheck
        );

        // --- Display ---
        form.getChildren().add(sectionHeader("Tab & Display"));
        tabsDropdown = editableCombo("Select tab");
        tabsDropdown.getItems().add("");
        tabsDropdown.getItems().addAll(Tabs.ALL);
        typeDropdown = editableCombo("(auto)");
        typeDropdown.getItems().add("");
        typeDropdown.getItems().addAll(DecoTypes.ALL);
        defaultAnimationDropdown = editableCombo("Select animation");
        form.getChildren().addAll(
                fieldLabel("Tab"), tabsDropdown,
                fieldLabel("Type"), typeDropdown,
                fieldLabel("Default Animation"), defaultAnimationDropdown
        );

        // --- Behavior ---
        form.getChildren().add(sectionHeader("Behavior"));
        passableCheck = darkCheck("Passable (walk through)");
        aboveWaterCheck = darkCheck("Above water");
        rotatableCheck = darkCheck("Rotatable (45\u00b0 snap)");
        hiddenCheck = darkCheck("Hidden (state variant)");
        lootLabel = fieldLabel("Loot drop (decoref)");
        lootField = textField("e.g., fridge_closed");
        lootLabel.setVisible(false);
        lootLabel.setManaged(false);
        lootField.setVisible(false);
        lootField.setManaged(false);
        hiddenCheck.selectedProperty().addListener((obs, o, n) -> {
            lootLabel.setVisible(n);
            lootLabel.setManaged(n);
            lootField.setVisible(n);
            lootField.setManaged(n);
        });
        displayableCheck = darkCheck("Displayable (can place in display slots)");
        form.getChildren().addAll(
                passableCheck, aboveWaterCheck, rotatableCheck,
                hiddenCheck, lootLabel, lootField,
                displayableCheck
        );

        // --- Composite ---
        form.getChildren().add(sectionHeader("Composite (child model)"));
        compositeCheck = darkCheck("Enable composite");
        compositeBox = new VBox(5);
        compositeBox.setVisible(false);
        compositeBox.setManaged(false);
        compositeCheck.selectedProperty().addListener((obs, o, n) -> {
            compositeBox.setVisible(n);
            compositeBox.setManaged(n);
        });
        compositeModelField = textField("Composite model name");
        compositeTextureDropdown = editableCombo("Optional texture override");
        compositeTransparencyCheck = darkCheck("Composite transparency");
        compositeBox.getChildren().addAll(
                fieldLabel("Model"), compositeModelField,
                fieldLabel("Texture"), compositeTextureDropdown,
                compositeTransparencyCheck
        );
        form.getChildren().addAll(compositeCheck, compositeBox);

        // --- Flipbook ---
        form.getChildren().add(sectionHeader("Flipbook (animated texture)"));
        flipbookCheck = darkCheck("Enable flipbook");
        flipbookBox = new VBox(5);
        flipbookBox.setVisible(false);
        flipbookBox.setManaged(false);
        flipbookCheck.selectedProperty().addListener((obs, o, n) -> {
            flipbookBox.setVisible(n);
            flipbookBox.setManaged(n);
        });
        flipbookFrametimeSpinner = new Spinner<>(1, 200, 8, 1);
        flipbookFrametimeSpinner.setEditable(true);
        flipbookFrametimeSpinner.setMaxWidth(Double.MAX_VALUE);
        flipbookImagesSpinner = new Spinner<>(1, 100, 2, 1);
        flipbookImagesSpinner.setEditable(true);
        flipbookImagesSpinner.setMaxWidth(Double.MAX_VALUE);
        flipbookBox.getChildren().addAll(
                fieldLabel("Frametime (ticks)"), flipbookFrametimeSpinner,
                fieldLabel("Images (frame count)"), flipbookImagesSpinner
        );
        form.getChildren().addAll(flipbookCheck, flipbookBox);

        // --- Chain ---
        form.getChildren().add(sectionHeader("Chain"));
        chainModelsArea = textArea("One model name per line");
        chainMaterialsArea = textArea("One texture per line (optional)");
        chainPatternDropdown = editableCombo("mirror | repeat");
        chainPatternDropdown.getItems().add("");
        chainPatternDropdown.getItems().addAll(DecoTypes.CHAIN_PATTERNS);
        lightingCheck = darkCheck("Lighting (chain light level)");
        lightingSpinner = new Spinner<>(0, 15, 15, 1);
        lightingSpinner.setEditable(true);
        lightingSpinner.setMaxWidth(Double.MAX_VALUE);
        lightingRow = new VBox(2);
        lightingRow.getChildren().addAll(fieldLabel("Level 0 to 15"), lightingSpinner);
        lightingRow.setVisible(false);
        lightingRow.setManaged(false);
        lightingCheck.selectedProperty().addListener((obs, o, n) -> {
            lightingRow.setVisible(n);
            lightingRow.setManaged(n);
        });
        form.getChildren().addAll(
                fieldLabel("Chain models"), chainModelsArea,
                fieldLabel("Chain materials"), chainMaterialsArea,
                fieldLabel("Pattern"), chainPatternDropdown,
                lightingCheck, lightingRow
        );

        // --- Growable ---
        form.getChildren().add(sectionHeader("Growable"));
        structuresArea = textArea("One structure name per line");
        instantCheck = darkCheck("Instant growth");
        form.getChildren().addAll(
                fieldLabel("Structures"), structuresArea,
                instantCheck
        );

        // --- Script ---
        form.getChildren().add(sectionHeader("Script"));
        lightCheck = darkCheck("Light emission");
        lightSpinner = new Spinner<>(0, 15, 15, 1);
        lightSpinner.setEditable(true);
        lightSpinner.setMaxWidth(Double.MAX_VALUE);
        lightRow = new VBox(2);
        lightRow.getChildren().addAll(fieldLabel("Level 0 to 15"), lightSpinner);
        lightRow.setVisible(false);
        lightRow.setManaged(false);
        lightCheck.selectedProperty().addListener((obs, o, n) -> {
            lightRow.setVisible(n);
            lightRow.setManaged(n);
        });

        counterCheck = darkCheck("Counter (trigger tick interval)");
        counterSpinner = new Spinner<>(1, 9999, 40, 1);
        counterSpinner.setEditable(true);
        counterSpinner.setMaxWidth(Double.MAX_VALUE);
        counterRow = new VBox(2);
        counterRow.getChildren().addAll(fieldLabel("Ticks"), counterSpinner);
        counterRow.setVisible(false);
        counterRow.setManaged(false);
        counterCheck.selectedProperty().addListener((obs, o, n) -> {
            counterRow.setVisible(n);
            counterRow.setManaged(n);
        });

        form.getChildren().addAll(
                lightCheck, lightRow,
                counterCheck, counterRow
        );

        particleHintLabel = new Label();
        particleHintLabel.setStyle("-fx-text-fill: #faa61a; -fx-font-size: 10px;");
        particleHintLabel.setWrapText(true);
        particleHintLabel.setVisible(false);
        particleHintLabel.setManaged(false);
        form.getChildren().add(particleHintLabel);

        // Action editors
        form.getChildren().add(subHeader("Actions"));
        onUseEditor = new ActionEditor("on_use");
        shiftOnUseEditor = new ActionEditor("shift_on_use");
        addedEditor = new ActionEditor("added");
        triggerEditor = new ActionEditor("trigger");
        animationStartEditor = new ActionEditor("animation_start");
        animationEndEditor = new ActionEditor("animation_end");
        toolModelSwitchEditor = new ActionEditor("tool_modelswitch");
        allActionEditors = List.of(onUseEditor, shiftOnUseEditor, addedEditor, triggerEditor,
                animationStartEditor, animationEndEditor, toolModelSwitchEditor);
        for (ActionEditor a : allActionEditors) {
            form.getChildren().add(a.getRoot());
        }

        // Scroll + buttons
        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #2f3136; -fx-background: #2f3136;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        HBox buttonRow = new HBox(5);
        Button duplicateBtn = new Button("Duplicate");
        duplicateBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(duplicateBtn, Priority.ALWAYS);
        duplicateBtn.setStyle("-fx-background-color: #5865F2; -fx-text-fill: white; -fx-cursor: hand;");
        duplicateBtn.setOnAction(e -> duplicateSelectedEntries());
        duplicateBtn.setTooltip(TopBar.tooltip("Create copies of selected entries (without material)"));

        Button saveBtn = new Button("Apply");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(saveBtn, Priority.ALWAYS);
        saveBtn.setStyle("-fx-background-color: #3ba55c; -fx-text-fill: white; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> saveSelectedEntries());
        saveBtn.setTooltip(TopBar.tooltip("Apply editor changes to all selected entries"));

        buttonRow.getChildren().addAll(duplicateBtn, saveBtn);

        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setStyle("-fx-background-color: #ed4245; -fx-text-fill: white; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> {
            List<DecoEntry> selected = new ArrayList<>(controller.getEntryListView().getSelectionModel().getSelectedItems());
            controller.getEntries().removeAll(selected);
        });

        root.getChildren().addAll(editorTitle, multiSelectHint, scroll,
                new Separator(Orientation.HORIZONTAL), buttonRow, deleteBtn);

        setShown(false);
    }

    public VBox getRoot() { return root; }

    public void setTitleText(String text) { editorTitle.setText(text); }

    public void setShown(boolean shown) {
        root.setVisible(shown);
        root.setManaged(shown);
    }

    // --- Refresh helpers ---

    private void refreshTextureDropdowns() {
        List<String> allTextures = new ArrayList<>();
        allTextures.add("");
        allTextures.addAll(controller.getTextureFiles().keySet());
        if (allTextures.size() > 1) {
            Collections.sort(allTextures.subList(1, allTextures.size()));
        }
        materialDropdown.getItems().setAll(allTextures);
        decorefDropdown.getItems().setAll(allTextures);
        compositeTextureDropdown.getItems().setAll(allTextures);
    }

    private List<String> buildLinkTargets() {
        List<String> targets = new ArrayList<>();
        targets.add("");
        for (DecoEntry e : controller.getEntries()) {
            String t = e.getDecoref() != null ? e.getDecoref() : e.getMaterial();
            if (t != null && !t.isEmpty() && !targets.contains(t)) {
                targets.add(t);
            }
        }
        if (targets.size() > 1) Collections.sort(targets.subList(1, targets.size()));
        return targets;
    }

    private void refreshActionLinkOptions() {
        List<String> targets = buildLinkTargets();
        for (ActionEditor ae : allActionEditors) {
            ae.refreshLinkOptions(targets);
        }
    }

    private void refreshAnimationOptions(String modelName) {
        List<String> animations = new ArrayList<>();
        animations.add("");

        if (modelName != null && controller.getParsedModels().containsKey(modelName)) {
            BBModel model = controller.getParsedModels().get(modelName);
            animations.addAll(model.getAnimationNames());
        }

        if (!animations.contains("any")) animations.add("any");
        if (!animations.contains("any_other")) animations.add("any_other");

        defaultAnimationDropdown.getItems().setAll(animations);
        for (ActionEditor ae : allActionEditors) {
            ae.refreshAnimationOptions(animations);
        }
    }

    private void refreshParticleHint(String modelName) {
        if (modelName == null || !controller.getParsedModels().containsKey(modelName)) {
            particleHintLabel.setVisible(false);
            particleHintLabel.setManaged(false);
            return;
        }
        BBModel model = controller.getParsedModels().get(modelName);
        List<String> locators = ModelInspector.getCandidateParticleLocators(model);
        if (locators.isEmpty()) {
            particleHintLabel.setVisible(false);
            particleHintLabel.setManaged(false);
            return;
        }
        particleHintLabel.setText("Particle locators detected: " + String.join(", ", locators));
        particleHintLabel.setVisible(true);
        particleHintLabel.setManaged(true);
    }

    // --- Load / clear ---

    public void loadEntry(DecoEntry entry) {
        refreshTextureDropdowns();
        refreshActionLinkOptions();
        refreshAnimationOptions(entry.getModel());
        refreshParticleHint(entry.getModel());

        // Identity
        nameField.setText(nn(entry.getName()));
        decorefDropdown.setValue(nn(entry.getDecoref()));
        materialDropdown.setValue(nn(entry.getMaterial()));

        // Rendering
        modelField.setText(nn(entry.getModel()));
        scaleSpinner.getValueFactory().setValue(entry.getScale());
        shapeField.setText(nn(entry.getShape()));
        transparencyCheck.setSelected(Boolean.TRUE.equals(entry.getTransparency()));
        cullingDisableCheck.setSelected(Boolean.FALSE.equals(entry.getCulling()));

        // Display
        tabsDropdown.setValue(nn(entry.getTabs()));
        typeDropdown.setValue(nn(entry.getType()));
        defaultAnimationDropdown.setValue(nn(entry.getDefaultAnimation()));

        // Behavior
        passableCheck.setSelected(Boolean.TRUE.equals(entry.getPassable()));
        aboveWaterCheck.setSelected(Boolean.TRUE.equals(entry.getAboveWater()));
        rotatableCheck.setSelected(Boolean.TRUE.equals(entry.getRotatable()));
        hiddenCheck.setSelected(Boolean.TRUE.equals(entry.getHidden()));
        lootField.setText(nn(entry.getLoot()));
        displayableCheck.setSelected(Boolean.TRUE.equals(entry.getDisplayable()));

        // Composite
        Composite comp = entry.getComposite();
        if (comp != null) {
            compositeCheck.setSelected(true);
            compositeModelField.setText(nn(comp.getModel()));
            compositeTextureDropdown.setValue(nn(comp.getTexture()));
            compositeTransparencyCheck.setSelected(Boolean.TRUE.equals(comp.getTransparency()));
        } else {
            compositeCheck.setSelected(false);
            compositeModelField.setText("");
            compositeTextureDropdown.setValue("");
            compositeTransparencyCheck.setSelected(false);
        }

        // Flipbook
        Flipbook fb = entry.getFlipbook();
        if (fb != null) {
            flipbookCheck.setSelected(true);
            flipbookFrametimeSpinner.getValueFactory().setValue(fb.getFrametime());
            flipbookImagesSpinner.getValueFactory().setValue(fb.getImages());
        } else {
            flipbookCheck.setSelected(false);
            flipbookFrametimeSpinner.getValueFactory().setValue(8);
            flipbookImagesSpinner.getValueFactory().setValue(2);
        }

        // Chain
        chainModelsArea.setText(linesFromList(entry.getChainModels()));
        chainMaterialsArea.setText(linesFromList(entry.getChainMaterials()));
        chainPatternDropdown.setValue(nn(entry.getChainPattern()));
        if (entry.getLighting() != null) {
            lightingCheck.setSelected(true);
            lightingSpinner.getValueFactory().setValue(entry.getLighting());
        } else {
            lightingCheck.setSelected(false);
            lightingSpinner.getValueFactory().setValue(15);
        }

        // Growable
        structuresArea.setText(linesFromList(entry.getStructures()));
        instantCheck.setSelected(Boolean.TRUE.equals(entry.getInstant()));

        // Script
        Script script = entry.getScript();
        if (script != null && script.getLight() != null) {
            lightCheck.setSelected(true);
            lightSpinner.getValueFactory().setValue(script.getLight());
        } else {
            lightCheck.setSelected(false);
            lightSpinner.getValueFactory().setValue(15);
        }

        if (script != null && script.getCounter() != null) {
            counterCheck.setSelected(true);
            counterSpinner.getValueFactory().setValue(script.getCounter());
        } else {
            counterCheck.setSelected(false);
            counterSpinner.getValueFactory().setValue(40);
        }

        if (script != null) {
            onUseEditor.loadAction(script.getOnUse());
            shiftOnUseEditor.loadAction(script.getShiftOnUse());
            addedEditor.loadAction(script.getAdded());
            triggerEditor.loadAction(script.getTrigger());
            animationStartEditor.loadAction(script.getAnimationStart());
            animationEndEditor.loadAction(script.getAnimationEnd());
            toolModelSwitchEditor.loadAction(script.getToolModelSwitch());
        } else {
            for (ActionEditor ae : allActionEditors) ae.clear();
        }
    }

    public void clearFields() {
        refreshTextureDropdowns();
        refreshActionLinkOptions();

        nameField.setText("");
        decorefDropdown.setValue("");
        materialDropdown.setValue("");

        modelField.setText("");
        scaleSpinner.getValueFactory().setValue(1.0);
        shapeField.setText("");
        transparencyCheck.setSelected(false);
        cullingDisableCheck.setSelected(false);

        tabsDropdown.setValue("");
        typeDropdown.setValue("");
        defaultAnimationDropdown.setValue("");

        passableCheck.setSelected(false);
        aboveWaterCheck.setSelected(false);
        rotatableCheck.setSelected(false);
        hiddenCheck.setSelected(false);
        lootField.setText("");
        displayableCheck.setSelected(false);

        compositeCheck.setSelected(false);
        compositeModelField.setText("");
        compositeTextureDropdown.setValue("");
        compositeTransparencyCheck.setSelected(false);

        flipbookCheck.setSelected(false);
        flipbookFrametimeSpinner.getValueFactory().setValue(8);
        flipbookImagesSpinner.getValueFactory().setValue(2);

        chainModelsArea.setText("");
        chainMaterialsArea.setText("");
        chainPatternDropdown.setValue("");
        lightingCheck.setSelected(false);
        lightingSpinner.getValueFactory().setValue(15);

        structuresArea.setText("");
        instantCheck.setSelected(false);

        lightCheck.setSelected(false);
        lightSpinner.getValueFactory().setValue(15);
        counterCheck.setSelected(false);
        counterSpinner.getValueFactory().setValue(40);

        for (ActionEditor ae : allActionEditors) ae.clear();

        particleHintLabel.setVisible(false);
        particleHintLabel.setManaged(false);
    }

    // --- Save ---

    private void saveSelectedEntries() {
        List<DecoEntry> selected = controller.getEntryListView().getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) return;

        // Values that are applied only when non-empty (text fields)
        String nameVal = nullIfEmpty(nameField.getText());
        String modelVal = nullIfEmpty(modelField.getText());
        String shapeVal = nullIfEmpty(shapeField.getText());
        String materialVal = comboValue(materialDropdown);
        String decorefVal = comboValue(decorefDropdown);
        String tabsVal = comboValue(tabsDropdown);
        String typeVal = comboValue(typeDropdown);
        String defaultAnimVal = comboValue(defaultAnimationDropdown);
        String lootVal = nullIfEmpty(lootField.getText());
        String chainPatternVal = comboValue(chainPatternDropdown);

        // Composite fields (only read when the composite checkbox is on)
        String compositeModelVal = nullIfEmpty(compositeModelField.getText());
        String compositeTextureVal = comboValue(compositeTextureDropdown);

        // Lists (always apply — empty list means "clear")
        List<String> chainModels = listFromLines(chainModelsArea.getText());
        List<String> chainMaterials = listFromLines(chainMaterialsArea.getText());
        List<String> structures = listFromLines(structuresArea.getText());

        for (DecoEntry entry : selected) {
            // Identity
            if (nameVal != null) entry.setName(nameVal);
            if (decorefVal != null) entry.setDecoref(decorefVal);
            if (materialVal != null) {
                entry.setMaterial(materialVal);
                controller.getUnmatchedTextures().remove(materialVal);
            }

            // Rendering
            if (modelVal != null) entry.setModel(modelVal);
            entry.setScale(scaleSpinner.getValue());
            if (shapeVal != null) entry.setShape(shapeVal);
            entry.setTransparency(transparencyCheck.isSelected() ? Boolean.TRUE : null);
            entry.setCulling(cullingDisableCheck.isSelected() ? Boolean.FALSE : null);

            // Display
            if (tabsVal != null) entry.setTabs(tabsVal);
            if (typeVal != null) entry.setType(typeVal);
            if (defaultAnimVal != null) entry.setDefaultAnimation(defaultAnimVal);

            // Behavior
            entry.setPassable(passableCheck.isSelected() ? Boolean.TRUE : null);
            entry.setAboveWater(aboveWaterCheck.isSelected() ? Boolean.TRUE : null);
            entry.setRotatable(rotatableCheck.isSelected() ? Boolean.TRUE : null);
            if (hiddenCheck.isSelected()) {
                entry.setHidden(Boolean.TRUE);
                entry.setLoot(lootVal);
            } else {
                entry.setHidden(null);
                entry.setLoot(null);
            }
            entry.setDisplayable(displayableCheck.isSelected() ? Boolean.TRUE : null);

            // Composite
            if (compositeCheck.isSelected()) {
                Composite c = new Composite();
                c.setModel(compositeModelVal);
                c.setTexture(compositeTextureVal);
                c.setTransparency(compositeTransparencyCheck.isSelected() ? Boolean.TRUE : null);
                entry.setComposite(c);
            } else {
                entry.setComposite(null);
            }

            // Flipbook
            if (flipbookCheck.isSelected()) {
                Flipbook fb = entry.getFlipbook();
                if (fb == null) {
                    fb = new Flipbook();
                    entry.setFlipbook(fb);
                }
                fb.setFrametime(flipbookFrametimeSpinner.getValue());
                fb.setImages(flipbookImagesSpinner.getValue());
            } else {
                entry.setFlipbook(null);
            }

            // Chain
            entry.setChainModels(chainModels.isEmpty() ? null : chainModels);
            entry.setChainMaterials(chainMaterials.isEmpty() ? null : chainMaterials);
            entry.setChainPattern(chainPatternVal);
            entry.setLighting(lightingCheck.isSelected() ? lightingSpinner.getValue() : null);

            // Growable
            entry.setStructures(structures.isEmpty() ? null : structures);
            entry.setInstant(instantCheck.isSelected() ? Boolean.TRUE : null);

            // Script
            Script script = entry.getScript();
            Action onUse = onUseEditor.buildAction();
            Action shiftOnUse = shiftOnUseEditor.buildAction();
            Action added = addedEditor.buildAction();
            Action trigger = triggerEditor.buildAction();
            Action animStart = animationStartEditor.buildAction();
            Action animEnd = animationEndEditor.buildAction();
            Action toolSwitch = toolModelSwitchEditor.buildAction();
            Integer lightVal = lightCheck.isSelected() ? lightSpinner.getValue() : null;
            Integer counterVal = counterCheck.isSelected() ? counterSpinner.getValue() : null;

            boolean anyScriptContent = onUse != null || shiftOnUse != null || added != null
                    || trigger != null || animStart != null || animEnd != null || toolSwitch != null
                    || lightVal != null || counterVal != null;

            if (anyScriptContent) {
                if (script == null) {
                    script = new Script();
                    entry.setScript(script);
                }
                script.setOnUse(onUse);
                script.setShiftOnUse(shiftOnUse);
                script.setAdded(added);
                script.setTrigger(trigger);
                script.setAnimationStart(animStart);
                script.setAnimationEnd(animEnd);
                script.setToolModelSwitch(toolSwitch);
                script.setLight(lightVal);
                script.setCounter(counterVal);
            } else {
                entry.setScript(null);
            }
        }

        controller.getEntryListView().refresh();
        controller.updateJsonPreview();
    }

    // --- Duplicate ---

    private void duplicateSelectedEntries() {
        List<DecoEntry> selected = new ArrayList<>(controller.getEntryListView().getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) return;

        List<DecoEntry> duplicates = new ArrayList<>();
        for (DecoEntry original : selected) {
            DecoEntry copy = new DecoEntry();

            // Identity (name gets suffix, material cleared so user can re-pair)
            copy.setName(original.getName() + " (copy)");
            copy.setDecoref(original.getDecoref());
            copy.setMaterial(null);

            // Rendering
            copy.setModel(original.getModel());
            copy.setScale(original.getScale());
            copy.setShape(original.getShape());
            copy.setTransparency(original.getTransparency());
            copy.setCulling(original.getCulling());

            // Display
            copy.setTabs(original.getTabs());
            copy.setType(original.getType());
            copy.setDefaultAnimation(original.getDefaultAnimation());

            // Behavior
            copy.setPassable(original.getPassable());
            copy.setAboveWater(original.getAboveWater());
            copy.setRotatable(original.getRotatable());
            copy.setHidden(original.getHidden());
            copy.setLoot(original.getLoot());
            copy.setDisplayable(original.getDisplayable());

            // crafting color
            copy.setCraftingColor(original.getCraftingColor() != null
                    ? original.getCraftingColor().clone() : new int[]{0, 0, 0});

            // Composite
            Composite oc = original.getComposite();
            if (oc != null) {
                Composite cc = new Composite();
                cc.setModel(oc.getModel());
                cc.setTexture(oc.getTexture());
                cc.setTransparency(oc.getTransparency());
                copy.setComposite(cc);
            }

            // Flipbook
            if (original.getFlipbook() != null) {
                Flipbook fc = new Flipbook();
                fc.setFrametime(original.getFlipbook().getFrametime());
                fc.setImages(original.getFlipbook().getImages());
                copy.setFlipbook(fc);
            }

            // Chain
            if (original.getChainModels() != null) copy.setChainModels(new ArrayList<>(original.getChainModels()));
            if (original.getChainMaterials() != null) copy.setChainMaterials(new ArrayList<>(original.getChainMaterials()));
            copy.setChainPattern(original.getChainPattern());
            copy.setLighting(original.getLighting());

            // Growable
            if (original.getStructures() != null) copy.setStructures(new ArrayList<>(original.getStructures()));
            copy.setInstant(original.getInstant());

            // Script (skip tool_modelswitch — chain-specific)
            if (original.getScript() != null) {
                Script os = original.getScript();
                Script cs = new Script();
                cs.setOnUse(cloneAction(os.getOnUse()));
                cs.setShiftOnUse(cloneAction(os.getShiftOnUse()));
                cs.setAdded(cloneAction(os.getAdded()));
                cs.setTrigger(cloneAction(os.getTrigger()));
                cs.setAnimationStart(cloneAction(os.getAnimationStart()));
                cs.setAnimationEnd(cloneAction(os.getAnimationEnd()));
                cs.setLight(os.getLight());
                cs.setCounter(os.getCounter());
                copy.setScript(cs);
            }

            duplicates.add(copy);
        }

        controller.getEntries().addAll(duplicates);
        controller.getEntryListView().refresh();
        controller.updateJsonPreview();

        controller.getEntryListView().getSelectionModel().clearSelection();
        for (DecoEntry d : duplicates) {
            controller.getEntryListView().getSelectionModel().select(d);
        }
    }

    private static Action cloneAction(Action a) {
        if (a == null) return null;
        Action c = new Action();
        c.setLink(a.getLink());
        c.setSound(a.getSound());
        if (a.getAnimations() != null) {
            List<AnimationPair> list = new ArrayList<>();
            for (AnimationPair p : a.getAnimations()) list.add(new AnimationPair(p.getFrom(), p.getTo()));
            c.setAnimations(list);
        }
        if (a.getSounds() != null) {
            List<SoundPair> list = new ArrayList<>();
            for (SoundPair p : a.getSounds()) {
                list.add(new SoundPair(p.getFrom(), p.getTo(), p.getSound(), p.getLoop()));
            }
            c.setSounds(list);
        }
        if (a.getStorage() != null) c.setStorage(a.getStorage().clone());
        return c;
    }

    // --- Small helpers ---

    private static TextField textField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-prompt-text-fill: #888;");
        return f;
    }

    private static TextArea textArea(String prompt) {
        TextArea a = new TextArea();
        a.setPromptText(prompt);
        a.setPrefRowCount(3);
        a.setWrapText(false);
        a.setStyle("-fx-control-inner-background: white; -fx-text-fill: black; -fx-prompt-text-fill: #888;");
        return a;
    }

    private static ComboBox<String> editableCombo(String prompt) {
        ComboBox<String> c = new ComboBox<>();
        c.setEditable(true);
        c.setStyle("-fx-background-color: white;");
        c.setMaxWidth(Double.MAX_VALUE);
        c.setPromptText(prompt);
        return c;
    }

    private static CheckBox darkCheck(String text) {
        CheckBox c = new CheckBox(text);
        c.setStyle("-fx-text-fill: #dcddde;");
        return c;
    }

    private static Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #b9bbbe; -fx-font-size: 11px;");
        return l;
    }

    private static VBox sectionHeader(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 0 2 0;");
        VBox wrapper = new VBox(l, new Separator(Orientation.HORIZONTAL));
        return wrapper;
    }

    private static Label subHeader(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 6 0 2 0;");
        return l;
    }

    private static String nn(String s) { return s == null ? "" : s; }

    private static String nullIfEmpty(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }

    private static String comboValue(ComboBox<String> c) {
        String v = c.getValue();
        if (v == null || v.isEmpty()) {
            if (c.getEditor() != null) v = c.getEditor().getText();
        }
        return nullIfEmpty(v);
    }

    private static String linesFromList(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return String.join("\n", list);
    }

    private static List<String> listFromLines(String text) {
        if (text == null || text.isEmpty()) return new ArrayList<>();
        List<String> out = new ArrayList<>();
        for (String line : Arrays.asList(text.split("\\r?\\n"))) {
            String t = line.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }
}