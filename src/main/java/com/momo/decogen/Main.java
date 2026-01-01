package com.momo.decogen;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class Main extends Application {

    private ObservableList<DecoEntry> entries = FXCollections.observableArrayList();
    private ListView<DecoEntry> entryListView;
    private TextArea jsonPreview;
    private VBox editorPanel;
    private Label editorTitle;

    // Editor fields
    private TextField nameField;
    private TextField modelField;
    private ComboBox<String> materialDropdown;
    private ComboBox<String> decorefDropdown;
    private ComboBox<String> tabsDropdown;
    private Label typeLabel;
    private CheckBox transparencyCheck;
    private CheckBox hiddenCheck;
    private Spinner<Double> scaleSpinner;
    private TextField lootField;
    private Label lootLabel;
    private CheckBox lightCheck;
    private Spinner<Integer> lightSpinner;
    private Label lightValueLabel;
    private CheckBox onUseCheck;
    private ComboBox<String> onUseLinkDropdown;
    private Label onUseLinkLabel;
    private CheckBox flipbookCheck;
    private Spinner<Integer> flipbookImagesSpinner;
    private Spinner<Integer> flipbookFrametimeSpinner;
    private Label flipbookImagesLabel;
    private Label flipbookFrametimeLabel;

    // Animation UI
    private ComboBox<String> defaultAnimationDropdown;
    private Label defaultAnimationLabel;
    private CheckBox onUseAnimCheck;
    private VBox onUseAnimBox;
    private CheckBox animEndCheck;
    private VBox animEndBox;

    // Track dropped files
    private Map<String, Path> modelFiles = new HashMap<>();      // modelName -> path
    private Map<String, Path> textureFiles = new HashMap<>();    // textureName -> path
    private Map<String, Path> iconFiles = new HashMap<>();       // iconName -> path (icons waiting for models)
    private List<String> unmatchedTextures = new ArrayList<>();
    private ListView<String> unmatchedListView;
    private ListView<String> iconListView;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #36393f;");

        // === TOP BAR ===
        HBox topBar = createTopBar(stage);
        root.setTop(topBar);

        // === LEFT: Drop Zone ===
        VBox leftPanel = createDropZone();
        root.setLeft(leftPanel);

        // === CENTER: Entry List + JSON Preview in SplitPane ===
        VBox centerPanel = createEntryList();
        VBox bottomPanel = createJsonPreview();

        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.getItems().addAll(centerPanel, bottomPanel);
        splitPane.setDividerPositions(0.65); // 65% entries, 35% JSON preview
        splitPane.setStyle("-fx-background-color: #36393f;");

        root.setCenter(splitPane);

        // === RIGHT: Editor Panel ===
        editorPanel = createEditorPanel();
        editorPanel.setVisible(false);
        editorPanel.setManaged(false);
        root.setRight(editorPanel);

        // Update JSON preview when entries change
        entries.addListener((ListChangeListener<DecoEntry>) c -> updateJsonPreview());

        Scene scene = new Scene(root, 1200, 800);
        stage.setTitle("Decocraft JSON Generator v" + UpdateChecker.getCurrentVersion());
        stage.setScene(scene);

        // Set application icon
        try {
            java.io.File iconFile = new java.io.File("src/main/java/com/momo/resources/generator_icon.png");
            if (iconFile.exists()) {
                stage.getIcons().add(new javafx.scene.image.Image(iconFile.toURI().toString()));
            } else {
                System.out.println("App icon not found at: " + iconFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("Could not load app icon: " + e.getMessage());
        }

        stage.show();

        // Check for updates in background
        UpdateChecker.checkForUpdatesAsync();
    }

    private HBox createTopBar(Stage stage) {
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(10));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #2f3136;");

        Button exportBtn = new Button("Export JSON");
        exportBtn.setPrefHeight(35);
        exportBtn.setStyle("-fx-background-color: #5865F2; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand;");
        exportBtn.setOnAction(e -> exportJson(stage));
        exportBtn.setTooltip(createTooltip("Save all entries to a JSON file"));

        Button resetBtn = new Button("Reset All");
        resetBtn.setPrefHeight(35);
        resetBtn.setStyle("-fx-background-color: #ed4245; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand;");
        resetBtn.setOnAction(e -> resetAll());
        resetBtn.setTooltip(createTooltip("Clear all entries, models, textures, and icons"));

        Button autoMatchBtn = new Button("Auto-Match Textures");
        autoMatchBtn.setPrefHeight(35);
        autoMatchBtn.setStyle("-fx-background-color: #3ba55c; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand;");
        autoMatchBtn.setOnAction(e -> autoMatchTextures());
        autoMatchBtn.setTooltip(createTooltip("Automatically assign unmatched textures to entries based on naming"));

        Button rainbowChainBtn = new Button("Rainbow Chain");
        rainbowChainBtn.setPrefHeight(35);
        rainbowChainBtn.setStyle("-fx-background-color: linear-gradient(to right, #ff0000, #ff8000, #ffff00, #00ff00, #00ffff, #0080ff, #8000ff); -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4;");
        rainbowChainBtn.setOnAction(e -> buildRainbowChain());
        rainbowChainBtn.setTooltip(createTooltip("Sort entries by color and link them in rainbow order (tool_modelswitch)"));

        Button woodChainBtn = new Button("Wood Chain");
        woodChainBtn.setPrefHeight(35);
        woodChainBtn.setStyle("-fx-background-color: #8B4513; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand;");
        woodChainBtn.setOnAction(e -> buildWoodChain());
        woodChainBtn.setTooltip(createTooltip("Sort entries by wood type and link them (tool_modelswitch)"));

        Label statsLabel = new Label();
        statsLabel.setStyle("-fx-text-fill: #72767d; -fx-font-size: 12px;");
        entries.addListener((ListChangeListener<DecoEntry>) c -> {
            long matched = entries.stream().filter(e2 -> e2.getMaterial() != null).count();
            statsLabel.setText(String.format("%d entries (%d matched, %d need textures)",
                    entries.size(), matched, entries.size() - matched));
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(exportBtn, autoMatchBtn, rainbowChainBtn, woodChainBtn, resetBtn, spacer, statsLabel);
        return topBar;
    }

    private VBox createDropZone() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(250);
        panel.setStyle("-fx-background-color: #2f3136;");

        Label title = new Label("Drop Files");
        title.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 14px; -fx-font-weight: bold;");

        // Models drop zone
        VBox modelZone = createDropBox("Models (.bbmodel)", "#5865F2", files -> {
            for (File file : files) {
                if (file.getName().toLowerCase().endsWith(".bbmodel")) {
                    addModel(file);
                }
            }
        });

        // Textures drop zone
        VBox textureZone = createDropBox("Textures (.png)", "#3ba55c", files -> {
            for (File file : files) {
                if (file.getName().toLowerCase().endsWith(".png")) {
                    addTexture(file);
                }
            }
        });

        // Icons drop zone
        VBox iconZone = createDropBox("Icons (.png)", "#faa61a", files -> {
            for (File file : files) {
                if (file.getName().toLowerCase().endsWith(".png")) {
                    addIcon(file);
                }
            }
        });

        // Dropped icons list (shows icons available for pairing)
        Label iconLabel = new Label("Dropped Icons (" + iconFiles.size() + "):");
        iconLabel.setStyle("-fx-text-fill: #faa61a; -fx-font-size: 12px;");

        iconListView = new ListView<>();
        iconListView.setStyle("-fx-background-color: #40444b; -fx-control-inner-background: #40444b;");
        iconListView.setPrefHeight(80);
        iconListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill: #faa61a;");
            }
        });

        // Unmatched textures list (multi-select enabled)
        Label unmatchedLabel = new Label("Unmatched Textures:");
        unmatchedLabel.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 12px;");

        unmatchedListView = new ListView<>();
        unmatchedListView.setStyle("-fx-background-color: #40444b; -fx-control-inner-background: #40444b;");
        unmatchedListView.setPrefHeight(120);
        unmatchedListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        unmatchedListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill: #dcddde;");
            }
        });

        // Update lists when entries change
        entries.addListener((ListChangeListener<DecoEntry>) c -> {
            unmatchedListView.getItems().setAll(unmatchedTextures);
            updateIconListView(iconLabel);
        });

        // Auto-Create All button - creates entries from icons and pairs textures automatically
        Button autoCreateBtn = new Button("Auto-Create All Entries");
        autoCreateBtn.setMaxWidth(Double.MAX_VALUE);
        autoCreateBtn.setStyle("-fx-background-color: #3ba55c; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;");
        autoCreateBtn.setOnAction(e -> autoCreateAllEntries());
        autoCreateBtn.setTooltip(createTooltip("Create entries from all icons and match with textures by suffix"));

        // Pair button - creates entries from selected textures + selected model
        Button pairBtn = new Button("Pair Selected with Entry");
        pairBtn.setMaxWidth(Double.MAX_VALUE);
        pairBtn.setStyle("-fx-background-color: #5865F2; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand;");
        pairBtn.setOnAction(e -> pairSelectedTexturesWithModel());
        pairBtn.setTooltip(createTooltip("Create entries from selected unmatched textures using the selected entry as template"));

        Label pairHint = new Label("Auto-Create: Uses icons + textures to generate all entries\nPair Selected: Manual pairing for selected textures");
        pairHint.setStyle("-fx-text-fill: #72767d; -fx-font-size: 9px;");
        pairHint.setWrapText(true);

        panel.getChildren().addAll(title, modelZone, textureZone, iconZone,
                new Separator(Orientation.HORIZONTAL),
                iconLabel, iconListView,
                unmatchedLabel, unmatchedListView, autoCreateBtn, pairBtn, pairHint);
        VBox.setVgrow(unmatchedListView, Priority.SOMETIMES);
        VBox.setVgrow(iconListView, Priority.SOMETIMES);

        return panel;
    }

    private VBox createDropBox(String label, String color, java.util.function.Consumer<List<File>> handler) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(15));
        box.setAlignment(Pos.CENTER);
        box.setMinHeight(70);
        String defaultStyle = String.format("-fx-border-color: %s; -fx-border-style: dashed; -fx-border-width: 2; " +
                "-fx-border-radius: 8; -fx-background-color: #40444b; -fx-background-radius: 8;", color);
        box.setStyle(defaultStyle);

        Label dropLabel = new Label(label);
        dropLabel.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 12px;");
        box.getChildren().add(dropLabel);

        box.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
                box.setStyle(String.format("-fx-border-color: %s; -fx-border-style: solid; -fx-border-width: 2; " +
                        "-fx-border-radius: 8; -fx-background-color: %s33; -fx-background-radius: 8;", color, color));
            }
            event.consume();
        });

        box.setOnDragExited(event -> {
            box.setStyle(defaultStyle);
            event.consume();
        });

        box.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                handler.accept(db.getFiles());
            }
            event.setDropCompleted(true);
            event.consume();
        });

        return box;
    }

    private VBox createEntryList() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));

        Label title = new Label("Entries");
        title.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 16px; -fx-font-weight: bold;");

        entryListView = new ListView<>(entries);
        entryListView.setStyle("-fx-background-color: #2f3136; -fx-control-inner-background: #2f3136;");
        entryListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        VBox.setVgrow(entryListView, Priority.ALWAYS);

        entryListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(DecoEntry entry, boolean empty) {
                super.updateItem(entry, empty);
                if (empty || entry == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);

                    // Determine entry type and status
                    boolean isIconEntry = entry.getDecoref() != null;
                    boolean isComplete = isIconEntry ?
                            (entry.getMaterial() != null) :  // Icon entries need material (shared texture)
                            (entry.getMaterial() != null);   // Regular entries need material

                    String statusIcon = isComplete ? "✓" : "⚠";
                    String statusColor = isComplete ? "#3ba55c" : "#faa61a";

                    Label status = new Label(statusIcon);
                    status.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 14px;");

                    Label name = new Label(entry.getName());
                    name.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 13px;");

                    // Show different details for icon entries vs regular entries
                    String detailText;
                    if (isIconEntry) {
                        // Check if the decoref matches a dropped icon
                        boolean hasRealIcon = iconFiles.containsKey(entry.getDecoref());
                        String iconIndicator = hasRealIcon ? "🖼" : "⚠";
                        detailText = String.format("%s [%s] decoref:%s mat:%s",
                                iconIndicator,
                                entry.getModel() != null ? entry.getModel() : "?",
                                entry.getDecoref(),
                                entry.getMaterial() != null ? entry.getMaterial() : "needs texture");
                    } else {
                        detailText = String.format("[%s] %s",
                                entry.getModel() != null ? entry.getModel() : "no model",
                                entry.getMaterial() != null ? entry.getMaterial() : "no texture");
                    }

                    Label details = new Label(detailText);
                    details.setStyle("-fx-text-fill: #72767d; -fx-font-size: 11px;");

                    row.getChildren().addAll(status, name, details);

                    setGraphic(row);
                    setText(null);

                    // Visual feedback for selection
                    if (isSelected()) {
                        setStyle("-fx-background-color: #5865F2; -fx-background-radius: 4; -fx-padding: 6;");
                    } else {
                        setStyle("-fx-background-color: #40444b; -fx-background-radius: 4; -fx-padding: 6;");
                    }
                }
            }
        });

        // Show editor when entry selected and update title for multi-select
        entryListView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<DecoEntry>) c -> {
            int count = entryListView.getSelectionModel().getSelectedItems().size();
            if (count > 0) {
                if (count == 1) {
                    editorTitle.setText("Entry Editor");
                    loadEntryToEditor(entryListView.getSelectionModel().getSelectedItems().get(0));
                } else {
                    editorTitle.setText("Editing " + count + " entries");
                    clearEditorFields();
                }
                editorPanel.setVisible(true);
                editorPanel.setManaged(true);
            } else {
                editorPanel.setVisible(false);
                editorPanel.setManaged(false);
            }
        });

        // Allow dropping textures onto entries
        entryListView.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        entryListView.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            DecoEntry target = entryListView.getSelectionModel().getSelectedItem();
            if (db.hasFiles() && target != null) {
                for (File file : db.getFiles()) {
                    if (file.getName().toLowerCase().endsWith(".png")) {
                        String textureName = DirectoryScanner.getStem(file.toPath());
                        target.setMaterial(textureName);
                        textureFiles.put(textureName, file.toPath());
                        unmatchedTextures.remove(textureName);
                        entryListView.refresh();
                        updateJsonPreview();
                        break;
                    }
                }
            }
            event.setDropCompleted(true);
            event.consume();
        });

        // Delete key to remove all selected entries
        entryListView.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("DELETE")) {
                List<DecoEntry> selected = new ArrayList<>(entryListView.getSelectionModel().getSelectedItems());
                if (!selected.isEmpty()) {
                    entries.removeAll(selected);
                }
            }
        });

        panel.getChildren().addAll(title, entryListView);
        return panel;
    }

    private VBox createEditorPanel() {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(280);
        panel.setStyle("-fx-background-color: #2f3136;");

        editorTitle = new Label("Entry Editor");
        editorTitle.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label multiSelectHint = new Label("(Empty fields won't change selected entries)");
        multiSelectHint.setStyle("-fx-text-fill: #72767d; -fx-font-size: 10px;");
        multiSelectHint.setWrapText(true);

        nameField = createTextField("Name");
        modelField = createTextField("Model");

        // Material dropdown (populated with available textures)
        Label materialLabel = new Label("Material");
        materialLabel.setStyle("-fx-text-fill: #b9bbbe; -fx-font-size: 11px;");
        materialDropdown = new ComboBox<>();
        materialDropdown.setEditable(true); // Allow typing custom values
        materialDropdown.setStyle("-fx-background-color: #40444b;");
        materialDropdown.setMaxWidth(Double.MAX_VALUE);
        materialDropdown.setPromptText("Select or type texture name");

        // Decoref dropdown (populated with available textures)
        Label decorefLabel = new Label("Decoref (icon)");
        decorefLabel.setStyle("-fx-text-fill: #b9bbbe; -fx-font-size: 11px;");
        decorefDropdown = new ComboBox<>();
        decorefDropdown.setEditable(true);
        decorefDropdown.setStyle("-fx-background-color: #40444b;");
        decorefDropdown.setMaxWidth(Double.MAX_VALUE);
        decorefDropdown.setPromptText("Select or type decoref");

        // Tabs dropdown
        Label tabsLabel = new Label("Tab");
        tabsLabel.setStyle("-fx-text-fill: #b9bbbe; -fx-font-size: 11px;");
        tabsDropdown = new ComboBox<>();
        tabsDropdown.setEditable(true);
        tabsDropdown.getItems().add(""); // Empty option for "don't change"
        tabsDropdown.getItems().addAll(Tabs.ALL);
        tabsDropdown.setStyle("-fx-background-color: white; -fx-text-fill: black;");
        tabsDropdown.setMaxWidth(Double.MAX_VALUE);
        tabsDropdown.setPromptText("Select tab");

        // Type (auto-detected, read-only)
        Label typeLabelHeader = new Label("Type (auto-detected)");
        typeLabelHeader.setStyle("-fx-text-fill: #b9bbbe; -fx-font-size: 11px;");
        typeLabel = new Label("-");
        typeLabel.setStyle("-fx-text-fill: black; -fx-background-color: white; -fx-padding: 5 8; -fx-background-radius: 3;");
        typeLabel.setMaxWidth(Double.MAX_VALUE);

        // Default Animation dropdown (for animated types)
        defaultAnimationLabel = new Label("Default Animation (idle)");
        defaultAnimationLabel.setStyle("-fx-text-fill: #b9bbbe; -fx-font-size: 11px;");
        defaultAnimationDropdown = new ComboBox<>();
        defaultAnimationDropdown.setEditable(true);
        defaultAnimationDropdown.setStyle("-fx-background-color: white; -fx-text-fill: black;");
        defaultAnimationDropdown.setMaxWidth(Double.MAX_VALUE);
        defaultAnimationDropdown.setPromptText("Select animation");

        // On Use Animation toggle
        onUseAnimCheck = new CheckBox("On Use Animations");
        onUseAnimCheck.setStyle("-fx-text-fill: #dcddde;");
        onUseAnimBox = createAnimationPairBox("on_use");
        onUseAnimBox.setVisible(false);
        onUseAnimBox.setManaged(false);
        onUseAnimCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            onUseAnimBox.setVisible(newVal);
            onUseAnimBox.setManaged(newVal);
        });

        // Animation End toggle
        animEndCheck = new CheckBox("Animation End (random idle)");
        animEndCheck.setStyle("-fx-text-fill: #dcddde;");
        animEndBox = createAnimationPairBox("animation_end");
        animEndBox.setVisible(false);
        animEndBox.setManaged(false);
        animEndCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            animEndBox.setVisible(newVal);
            animEndBox.setManaged(newVal);
        });

        // Scale spinner
        Label scaleLabel = new Label("Scale");
        scaleLabel.setStyle("-fx-text-fill: #b9bbbe; -fx-font-size: 11px;");
        scaleSpinner = new Spinner<>(0.1, 10.0, 1.0, 0.1);
        scaleSpinner.setEditable(true);
        scaleSpinner.setStyle("-fx-background-color: #40444b;");
        scaleSpinner.setMaxWidth(Double.MAX_VALUE);

        // Checkboxes
        transparencyCheck = new CheckBox("Transparency");
        transparencyCheck.setStyle("-fx-text-fill: #dcddde;");

        // Light toggle with value spinner
        lightCheck = new CheckBox("Light");
        lightCheck.setStyle("-fx-text-fill: #dcddde;");

        lightValueLabel = new Label("Light Level (0-15)");
        lightValueLabel.setStyle("-fx-text-fill: #b9bbbe; -fx-font-size: 11px;");
        lightSpinner = new Spinner<>(0, 15, 15, 1);
        lightSpinner.setEditable(true);
        lightSpinner.setStyle("-fx-background-color: white;");
        lightSpinner.setMaxWidth(Double.MAX_VALUE);
        lightValueLabel.setVisible(false);
        lightValueLabel.setManaged(false);
        lightSpinner.setVisible(false);
        lightSpinner.setManaged(false);

        // Toggle light spinner visibility when light is checked
        lightCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            lightValueLabel.setVisible(newVal);
            lightValueLabel.setManaged(newVal);
            lightSpinner.setVisible(newVal);
            lightSpinner.setManaged(newVal);
        });

        // On Use toggle with link dropdown
        onUseCheck = new CheckBox("On Use (link)");
        onUseCheck.setStyle("-fx-text-fill: #dcddde;");

        onUseLinkLabel = new Label("Link to Entry");
        onUseLinkLabel.setStyle("-fx-text-fill: #b9bbbe; -fx-font-size: 11px;");
        onUseLinkDropdown = new ComboBox<>();
        onUseLinkDropdown.setEditable(true);
        onUseLinkDropdown.setStyle("-fx-background-color: white; -fx-text-fill: black;");
        onUseLinkDropdown.setMaxWidth(Double.MAX_VALUE);
        onUseLinkDropdown.setPromptText("Select or type entry decoref");
        onUseLinkLabel.setVisible(false);
        onUseLinkLabel.setManaged(false);
        onUseLinkDropdown.setVisible(false);
        onUseLinkDropdown.setManaged(false);

        // Toggle on_use dropdown visibility
        onUseCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            onUseLinkLabel.setVisible(newVal);
            onUseLinkLabel.setManaged(newVal);
            onUseLinkDropdown.setVisible(newVal);
            onUseLinkDropdown.setManaged(newVal);
            if (newVal) {
                refreshOnUseLinkDropdown();
            }
        });

        // Flipbook toggle with images and frametime spinners
        flipbookCheck = new CheckBox("Flipbook (animation)");
        flipbookCheck.setStyle("-fx-text-fill: #dcddde;");

        flipbookImagesLabel = new Label("Frame Count");
        flipbookImagesLabel.setStyle("-fx-text-fill: #b9bbbe; -fx-font-size: 11px;");
        flipbookImagesSpinner = new Spinner<>(1, 100, 2, 1);
        flipbookImagesSpinner.setEditable(true);
        flipbookImagesSpinner.setStyle("-fx-background-color: white;");
        flipbookImagesSpinner.setMaxWidth(Double.MAX_VALUE);
        flipbookImagesLabel.setVisible(false);
        flipbookImagesLabel.setManaged(false);
        flipbookImagesSpinner.setVisible(false);
        flipbookImagesSpinner.setManaged(false);

        flipbookFrametimeLabel = new Label("Ticks per Frame");
        flipbookFrametimeLabel.setStyle("-fx-text-fill: #b9bbbe; -fx-font-size: 11px;");
        flipbookFrametimeSpinner = new Spinner<>(1, 200, 8, 1);
        flipbookFrametimeSpinner.setEditable(true);
        flipbookFrametimeSpinner.setStyle("-fx-background-color: white;");
        flipbookFrametimeSpinner.setMaxWidth(Double.MAX_VALUE);
        flipbookFrametimeLabel.setVisible(false);
        flipbookFrametimeLabel.setManaged(false);
        flipbookFrametimeSpinner.setVisible(false);
        flipbookFrametimeSpinner.setManaged(false);

        // Toggle flipbook fields visibility
        flipbookCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            flipbookImagesLabel.setVisible(newVal);
            flipbookImagesLabel.setManaged(newVal);
            flipbookImagesSpinner.setVisible(newVal);
            flipbookImagesSpinner.setManaged(newVal);
            flipbookFrametimeLabel.setVisible(newVal);
            flipbookFrametimeLabel.setManaged(newVal);
            flipbookFrametimeSpinner.setVisible(newVal);
            flipbookFrametimeSpinner.setManaged(newVal);
        });

        hiddenCheck = new CheckBox("Hidden");
        hiddenCheck.setStyle("-fx-text-fill: #dcddde;");

        // Loot field (only visible when Hidden is checked)
        lootLabel = new Label("Loot (drop item)");
        lootLabel.setStyle("-fx-text-fill: #b9bbbe; -fx-font-size: 11px;");
        lootField = new TextField();
        lootField.setPromptText("e.g., fridge_closed");
        lootField.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-prompt-text-fill: #888888;");
        lootLabel.setVisible(false);
        lootLabel.setManaged(false);
        lootField.setVisible(false);
        lootField.setManaged(false);

        // Toggle loot field visibility when hidden is checked
        hiddenCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            lootLabel.setVisible(newVal);
            lootLabel.setManaged(newVal);
            lootField.setVisible(newVal);
            lootField.setManaged(newVal);
        });

        // Button row
        HBox buttonRow1 = new HBox(5);

        // Duplicate button
        Button duplicateBtn = new Button("Duplicate");
        duplicateBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(duplicateBtn, Priority.ALWAYS);
        duplicateBtn.setStyle("-fx-background-color: #5865F2; -fx-text-fill: white; -fx-font-size: 12px; -fx-cursor: hand;");
        duplicateBtn.setOnAction(e -> duplicateSelectedEntries());
        duplicateBtn.setTooltip(createTooltip("Create copies of selected entries (without material)"));

        // Save button
        Button saveBtn = new Button("Apply");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(saveBtn, Priority.ALWAYS);
        saveBtn.setStyle("-fx-background-color: #3ba55c; -fx-text-fill: white; -fx-font-size: 12px; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> saveSelectedEntries());
        saveBtn.setTooltip(createTooltip("Apply editor changes to all selected entries"));

        buttonRow1.getChildren().addAll(duplicateBtn, saveBtn);

        // Delete button
        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setStyle("-fx-background-color: #ed4245; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> {
            List<DecoEntry> selected = new ArrayList<>(entryListView.getSelectionModel().getSelectedItems());
            entries.removeAll(selected);
        });
        deleteBtn.setTooltip(createTooltip("Remove selected entries from the list"));

        panel.getChildren().addAll(
                editorTitle, multiSelectHint,
                new Label("Name") {{ setStyle("-fx-text-fill: #b9bbbe; -fx-font-size: 11px;"); }}, nameField,
                new Label("Model") {{ setStyle("-fx-text-fill: #b9bbbe; -fx-font-size: 11px;"); }}, modelField,
                materialLabel, materialDropdown,
                decorefLabel, decorefDropdown,
                tabsLabel, tabsDropdown,
                typeLabelHeader, typeLabel,
                defaultAnimationLabel, defaultAnimationDropdown,
                onUseAnimCheck, onUseAnimBox,
                animEndCheck, animEndBox,
                scaleLabel, scaleSpinner,
                transparencyCheck,
                lightCheck, lightValueLabel, lightSpinner,
                onUseCheck, onUseLinkLabel, onUseLinkDropdown,
                flipbookCheck, flipbookImagesLabel, flipbookImagesSpinner,
                flipbookFrametimeLabel, flipbookFrametimeSpinner,
                hiddenCheck, lootLabel, lootField,
                new Separator(Orientation.HORIZONTAL),
                buttonRow1, deleteBtn
        );

        return panel;
    }

    private TextField createTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-prompt-text-fill: #888888;");
        return field;
    }

    /**
     * Create a tooltip with faster show delay
     */
    private Tooltip createTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setShowDelay(Duration.millis(200));
        tooltip.setShowDuration(Duration.seconds(10));
        tooltip.setStyle("-fx-font-size: 12px;");
        return tooltip;
    }

    /**
     * Create a box for managing animation pairs (from -> to)
     */
    private VBox createAnimationPairBox(String id) {
        VBox box = new VBox(5);
        box.setStyle("-fx-padding: 5; -fx-background-color: #40444b; -fx-background-radius: 4;");

        ListView<HBox> pairsList = new ListView<>();
        pairsList.setPrefHeight(100);
        pairsList.setStyle("-fx-background-color: #2f3136; -fx-control-inner-background: #2f3136;");
        pairsList.setId(id + "_pairs");

        HBox addRow = new HBox(5);
        addRow.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> fromDropdown = new ComboBox<>();
        fromDropdown.setEditable(true);
        fromDropdown.setPromptText("From");
        fromDropdown.setStyle("-fx-background-color: white; -fx-pref-width: 100;");
        fromDropdown.setId(id + "_from");

        Label arrowLabel = new Label("→");
        arrowLabel.setStyle("-fx-text-fill: #dcddde;");

        ComboBox<String> toDropdown = new ComboBox<>();
        toDropdown.setEditable(true);
        toDropdown.setPromptText("To");
        toDropdown.setStyle("-fx-background-color: white; -fx-pref-width: 100;");
        toDropdown.setId(id + "_to");

        Button addBtn = new Button("+");
        addBtn.setStyle("-fx-background-color: #3ba55c; -fx-text-fill: white; -fx-cursor: hand;");
        addBtn.setOnAction(e -> {
            String from = fromDropdown.getValue();
            if (from == null) from = fromDropdown.getEditor().getText();
            String to = toDropdown.getValue();
            if (to == null) to = toDropdown.getEditor().getText();

            if ((from != null && !from.isEmpty()) || (to != null && !to.isEmpty())) {
                HBox pairRow = createAnimationPairRow(from, to, pairsList);
                pairsList.getItems().add(pairRow);
                fromDropdown.setValue("");
                toDropdown.setValue("");
            }
        });

        addRow.getChildren().addAll(fromDropdown, arrowLabel, toDropdown, addBtn);

        Label hint = new Label("Add from→to pairs (use 'any' for wildcards)");
        hint.setStyle("-fx-text-fill: #72767d; -fx-font-size: 9px;");

        box.getChildren().addAll(pairsList, addRow, hint);
        return box;
    }

    /**
     * Create a row for displaying an animation pair with remove button.
     * Stores the from/to values as userData on the row itself for easy retrieval.
     */
    private HBox createAnimationPairRow(String from, String to, ListView<HBox> parentList) {
        HBox row = new HBox(5);
        row.setAlignment(Pos.CENTER_LEFT);
        // Store the pair data on the row for easy retrieval
        row.setUserData(new String[]{from != null ? from : "", to != null ? to : ""});

        Label fromLabel = new Label(from != null && !from.isEmpty() ? from : "(empty)");
        fromLabel.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 11px;");

        Label arrow = new Label("→");
        arrow.setStyle("-fx-text-fill: #72767d;");

        Label toLabel = new Label(to != null && !to.isEmpty() ? to : "(empty)");
        toLabel.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 11px;");

        Button removeBtn = new Button("×");
        removeBtn.setStyle("-fx-background-color: #ed4245; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 0 5;");
        removeBtn.setOnAction(e -> parentList.getItems().remove(row));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(fromLabel, arrow, toLabel, spacer, removeBtn);
        return row;
    }

    /**
     * Refresh animation dropdowns based on selected entry's model
     */
    private void refreshAnimationDropdowns(String modelName) {
        List<String> animations = new ArrayList<>();
        animations.add(""); // Empty option

        if (modelName != null && parsedModels.containsKey(modelName)) {
            Model model = parsedModels.get(modelName);
            animations.addAll(model.getAnimationNames());
        }

        // Also add common wildcards
        if (!animations.contains("any")) animations.add("any");
        if (!animations.contains("any_other")) animations.add("any_other");

        defaultAnimationDropdown.getItems().setAll(animations);

        // Update the from/to dropdowns in animation pair boxes
        updateAnimationPairDropdowns(onUseAnimBox, animations);
        updateAnimationPairDropdowns(animEndBox, animations);
    }

    /**
     * Update the from/to dropdowns in an animation pair box
     */
    @SuppressWarnings("unchecked")
    private void updateAnimationPairDropdowns(VBox box, List<String> animations) {
        for (javafx.scene.Node node : box.getChildren()) {
            if (node instanceof HBox) {
                for (javafx.scene.Node child : ((HBox) node).getChildren()) {
                    if (child instanceof ComboBox) {
                        ((ComboBox<String>) child).getItems().setAll(animations);
                    }
                }
            }
        }
    }

    /**
     * Get animation pairs from a pair box
     */
    @SuppressWarnings("unchecked")
    private List<AnimationPair> getAnimationPairsFromBox(VBox box) {
        List<AnimationPair> pairs = new ArrayList<>();

        for (javafx.scene.Node node : box.getChildren()) {
            if (node instanceof ListView) {
                ListView<HBox> list = (ListView<HBox>) node;
                for (HBox row : list.getItems()) {
                    // Read from/to from the row's userData (stored as String[])
                    Object data = row.getUserData();
                    if (data instanceof String[]) {
                        String[] pairData = (String[]) data;
                        String from = pairData[0];
                        String to = pairData[1];
                        // Only add if at least one value is non-empty
                        if ((from != null && !from.isEmpty()) || (to != null && !to.isEmpty())) {
                            pairs.add(new AnimationPair(from, to));
                        }
                    }
                }
            }
        }
        return pairs;
    }

    /**
     * Set animation pairs in a pair box
     */
    @SuppressWarnings("unchecked")
    private void setAnimationPairsInBox(VBox box, List<AnimationPair> pairs) {
        // Find the ListView in the box and clear it
        for (javafx.scene.Node node : box.getChildren()) {
            if (node instanceof ListView) {
                ListView<HBox> list = (ListView<HBox>) node;
                list.getItems().clear();
                if (pairs != null) {
                    for (AnimationPair pair : pairs) {
                        HBox row = createAnimationPairRow(pair.getFrom(), pair.getTo(), list);
                        list.getItems().add(row);
                    }
                }
                break;
            }
        }
    }

    /**
     * Clear animation pairs in a pair box
     */
    @SuppressWarnings("unchecked")
    private void clearAnimationPairBox(VBox box) {
        for (javafx.scene.Node node : box.getChildren()) {
            if (node instanceof ListView) {
                ((ListView<HBox>) node).getItems().clear();
            }
        }
    }

    private VBox createJsonPreview() {
        VBox panel = new VBox(5);
        panel.setPadding(new Insets(10));
        panel.setMinHeight(100);
        panel.setStyle("-fx-background-color: #2f3136;");

        Label title = new Label("JSON Preview");
        title.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 14px; -fx-font-weight: bold;");

        jsonPreview = new TextArea();
        jsonPreview.setEditable(false);
        jsonPreview.setWrapText(false);
        jsonPreview.setStyle("-fx-control-inner-background: #1e1f22; -fx-text-fill: #dcddde; -fx-font-family: monospace;");
        VBox.setVgrow(jsonPreview, Priority.ALWAYS);

        panel.getChildren().addAll(title, jsonPreview);
        return panel;
    }

    // === FILE HANDLING ===

    // Store parsed models for type detection
    private Map<String, Model> parsedModels = new HashMap<>();

    private void addModel(File file) {
        try {
            Model model = ModelParser.parse(file.toPath());
            String modelName = DirectoryScanner.getStem(file.toPath());

            modelFiles.put(modelName, file.toPath());
            parsedModels.put(modelName, model);

            System.out.println("Added model: " + modelName);

            // Check if there are unmatched textures waiting for this model
            int matchedCount = createEntriesForMatchingTextures(modelName);

            // If no textures matched, create a placeholder entry for this model
            // so user can manually assign textures
            if (matchedCount == 0) {
                String displayName = EntryBuilder.toDisplayName(modelName);
                DecoEntry entry = new DecoEntry(displayName, modelName, null, "clutter");
                entry.autoDetectType(model);
                entries.add(entry);
                entryListView.refresh();
                updateJsonPreview();
                System.out.println("Created placeholder entry for model: " + modelName);
            }

        } catch (Exception e) {
            System.err.println("Error loading model: " + e.getMessage());
        }
    }

    private void addTexture(File file) {
        String textureName = DirectoryScanner.getStem(file.toPath());
        textureFiles.put(textureName, file.toPath());

        // Find matching model and create a NEW entry for this texture
        String matchedModel = findMatchingModel(textureName);

        if (matchedModel != null) {
            createEntryForTexture(textureName, matchedModel);
            System.out.println("Created entry: " + textureName + " -> " + matchedModel);
        } else {
            unmatchedTextures.add(textureName);
            unmatchedListView.getItems().setAll(unmatchedTextures); // Update list view immediately
            System.out.println("Added unmatched texture: " + textureName);
        }
    }

    private void addIcon(File file) {
        String iconName = DirectoryScanner.getStem(file.toPath());

        // Store icon in both textureFiles (for general lookup) and iconFiles (for tracking)
        textureFiles.put(iconName, file.toPath());
        iconFiles.put(iconName, file.toPath());

        // Update the icon list view
        updateIconListView(null);

        // Find matching model for this icon
        String matchedModel = findMatchingModel(iconName);

        if (matchedModel != null) {
            // Create entry with decoref (for shared textures)
            // The decoref IS the identifier, material will be the shared texture
            String displayName = EntryBuilder.toDisplayName(iconName);
            DecoEntry entry = new DecoEntry(displayName, matchedModel, null, "clutter");
            entry.setDecoref(iconName);

            // Try to auto-detect type from model
            Model model = parsedModels.get(matchedModel);
            if (model != null) {
                entry.autoDetectType(model);
            }

            entries.add(entry);
            entryListView.refresh();
            updateJsonPreview();
            System.out.println("Created icon entry: " + iconName + " (decoref) -> " + matchedModel);
        } else {
            System.out.println("Stored icon (no model yet): " + iconName);
        }
    }

    /**
     * Update the icon list view to show available icons and their status
     */
    private void updateIconListView(Label iconLabel) {
        if (iconListView == null) return;

        List<String> iconDisplay = new ArrayList<>();
        for (String iconName : iconFiles.keySet()) {
            // Check if this icon has been used in an entry
            boolean used = entries.stream().anyMatch(e -> iconName.equals(e.getDecoref()));
            String status = used ? "✓ " : "○ ";
            iconDisplay.add(status + iconName);
        }
        Collections.sort(iconDisplay);
        iconListView.getItems().setAll(iconDisplay);

        // Update label count
        if (iconLabel != null) {
            long usedCount = entries.stream().filter(e -> e.getDecoref() != null && iconFiles.containsKey(e.getDecoref())).count();
            iconLabel.setText("Dropped Icons (" + usedCount + "/" + iconFiles.size() + " used):");
        }
    }

    private String findMatchingModel(String textureName) {
        String texLower = textureName.toLowerCase();

        // Find the best matching model (longest prefix match)
        String bestMatch = null;
        int bestLength = 0;

        for (String modelName : modelFiles.keySet()) {
            String modelLower = modelName.toLowerCase();

            // Check if texture matches this model
            if (texLower.equals(modelLower) ||
                texLower.startsWith(modelLower + "_") ||
                texLower.startsWith(modelLower + "-")) {

                if (modelName.length() > bestLength) {
                    bestMatch = modelName;
                    bestLength = modelName.length();
                }
            }
        }

        return bestMatch;
    }

    private void createEntryForTexture(String textureName, String modelName) {
        String displayName = EntryBuilder.toDisplayName(textureName);
        DecoEntry entry = new DecoEntry(displayName, modelName, textureName, "clutter");

        // Try to auto-detect type from model
        Model model = parsedModels.get(modelName);
        if (model != null) {
            entry.autoDetectType(model);
        }

        entries.add(entry);
        unmatchedTextures.remove(textureName);
        entryListView.refresh();
        updateJsonPreview();
    }

    private int createEntriesForMatchingTextures(String modelName) {
        int count = 0;
        // Check unmatched textures for ones that match this model
        for (String textureName : new ArrayList<>(unmatchedTextures)) {
            if (findMatchingModel(textureName) != null &&
                findMatchingModel(textureName).equals(modelName)) {
                createEntryForTexture(textureName, modelName);
                count++;
            }
        }
        return count;
    }

    private boolean textureMatchesModel(String textureName, String modelName) {
        if (modelName == null) return false;
        String texLower = textureName.toLowerCase();
        String modelLower = modelName.toLowerCase();

        // Exact match
        if (texLower.equals(modelLower)) return true;

        // Texture starts with model name (e.g., chair_red matches chair)
        if (texLower.startsWith(modelLower + "_")) return true;
        if (texLower.startsWith(modelLower + "-")) return true;

        return false;
    }

    private void autoMatchTexturesForModel(String modelName) {
        Iterator<String> it = unmatchedTextures.iterator();
        while (it.hasNext()) {
            String textureName = it.next();
            if (textureMatchesModel(textureName, modelName)) {
                // Find entry with this model that needs a texture
                for (DecoEntry entry : entries) {
                    if (entry.getModel().equals(modelName) && entry.getMaterial() == null) {
                        entry.setMaterial(textureName);
                        it.remove();
                        entryListView.refresh();
                        updateJsonPreview();
                        System.out.println("Auto-matched: " + textureName + " -> " + modelName);
                        break;
                    }
                }
            }
        }
    }

    private void autoMatchTextures() {
        for (String textureName : new ArrayList<>(unmatchedTextures)) {
            for (DecoEntry entry : entries) {
                if (entry.getMaterial() == null && textureMatchesModel(textureName, entry.getModel())) {
                    entry.setMaterial(textureName);
                    unmatchedTextures.remove(textureName);
                    break;
                }
            }
        }

        // Create new entries for remaining unmatched textures that match models
        for (String textureName : new ArrayList<>(unmatchedTextures)) {
            for (String modelName : modelFiles.keySet()) {
                if (textureMatchesModel(textureName, modelName)) {
                    String displayName = EntryBuilder.toDisplayName(textureName);
                    DecoEntry entry = new DecoEntry(displayName, modelName, textureName, "clutter");
                    entries.add(entry);
                    unmatchedTextures.remove(textureName);
                    System.out.println("Created entry for texture: " + textureName);
                    break;
                }
            }
        }

        entryListView.refresh();
        updateJsonPreview();
    }

    /**
     * Automatically creates entries by matching icons with textures for each model.
     * For each icon: finds matching texture, creates entry with decoref=icon, material=texture
     * Also updates existing entries that are missing materials.
     */
    private void autoCreateAllEntries() {
        if (modelFiles.isEmpty()) {
            showAlert("No Models", "Drop at least one model (.bbmodel) first.");
            return;
        }

        if (iconFiles.isEmpty() && unmatchedTextures.isEmpty()) {
            showAlert("No Icons or Textures", "Drop icons and/or textures first.");
            return;
        }

        List<DecoEntry> newEntries = new ArrayList<>();
        List<String> usedTextures = new ArrayList<>();
        int iconsMatched = 0;
        int texturesOnly = 0;
        int entriesUpdated = 0;

        // First pass: Update existing entries or create new ones from icons
        for (String iconName : iconFiles.keySet()) {
            // Find which model this icon belongs to
            String modelName = findMatchingModel(iconName);
            if (modelName == null) continue;

            // Extract the suffix from icon name (e.g., "single_bed_oak_red" -> "oak_red")
            String suffix = iconName.substring(modelName.length() + 1); // +1 for the underscore

            // Find matching texture for this icon's suffix
            String matchingTexture = findMatchingTextureForSuffix(suffix);

            // Check if entry already exists with this decoref
            DecoEntry existingEntry = entries.stream()
                    .filter(e -> iconName.equals(e.getDecoref()))
                    .findFirst()
                    .orElse(null);

            if (existingEntry != null) {
                // Update existing entry if it's missing a material
                if (existingEntry.getMaterial() == null && matchingTexture != null) {
                    existingEntry.setMaterial(matchingTexture);
                    usedTextures.add(matchingTexture);
                    entriesUpdated++;
                }
                continue;
            }

            // Create new entry
            String displayName = EntryBuilder.toDisplayName(iconName);
            DecoEntry entry = new DecoEntry(displayName, modelName, matchingTexture, "clutter");
            entry.setDecoref(iconName);

            // Auto-detect type from model
            Model model = parsedModels.get(modelName);
            if (model != null) {
                entry.autoDetectType(model);
            }

            newEntries.add(entry);
            iconsMatched++;

            if (matchingTexture != null) {
                usedTextures.add(matchingTexture);
            }
        }

        // Second pass: Create entries for remaining unmatched textures (without icons)
        for (String textureName : new ArrayList<>(unmatchedTextures)) {
            if (usedTextures.contains(textureName)) continue;

            String modelName = findMatchingModel(textureName);
            if (modelName == null) continue;

            // Extract suffix and generate decoref
            String suffix = extractColorSuffix(textureName);
            String generatedDecoref = modelName + "_" + suffix;

            // Skip if we already have this decoref
            boolean alreadyExists = entries.stream().anyMatch(e ->
                generatedDecoref.equals(e.getDecoref()) || textureName.equals(e.getMaterial()));
            if (alreadyExists) continue;

            // Check if there's an icon for this (might have been missed in first pass)
            String existingIcon = findMatchingIcon(modelName, suffix);

            String displayName = EntryBuilder.toDisplayName(modelName + "_" + suffix);
            DecoEntry entry = new DecoEntry(displayName, modelName, textureName, "clutter");
            entry.setDecoref(existingIcon != null ? existingIcon : generatedDecoref);

            Model model = parsedModels.get(modelName);
            if (model != null) {
                entry.autoDetectType(model);
            }

            newEntries.add(entry);
            usedTextures.add(textureName);
            texturesOnly++;
        }

        if (newEntries.isEmpty() && entriesUpdated == 0) {
            showAlert("No Changes", "All icons and textures have already been paired, or no matches found.");
            return;
        }

        // Remove placeholder entries (model-only entries without decoref or material)
        // Keep icon entries even if they don't have a material yet
        List<DecoEntry> placeholders = entries.stream()
                .filter(e -> e.getMaterial() == null && e.getDecoref() == null)
                .toList();
        entries.removeAll(placeholders);

        // Add new entries
        entries.addAll(newEntries);

        // Remove used textures from unmatched list
        unmatchedTextures.removeAll(usedTextures);
        unmatchedListView.getItems().setAll(unmatchedTextures);

        entryListView.refresh();
        updateJsonPreview();
        updateIconListView(null);

        showAlert("Auto-Create Complete",
                String.format("Created %d new entries, updated %d existing:\n• %d from icons with textures\n• %d from textures only\n• %d entries got materials assigned\n\n%d textures still unmatched",
                        newEntries.size(), entriesUpdated, iconsMatched, texturesOnly, entriesUpdated, unmatchedTextures.size()));
    }

    /**
     * Find a texture that matches the given suffix (e.g., "oak_red" finds "bed_set_oak_red")
     */
    private String findMatchingTextureForSuffix(String suffix) {
        String suffixLower = "_" + suffix.toLowerCase();

        // First check unmatched textures
        for (String textureName : unmatchedTextures) {
            if (textureName.toLowerCase().endsWith(suffixLower)) {
                return textureName;
            }
        }

        // Also check all texture files
        for (String textureName : textureFiles.keySet()) {
            if (textureName.toLowerCase().endsWith(suffixLower) && !iconFiles.containsKey(textureName)) {
                return textureName;
            }
        }

        return null;
    }

    /**
     * Pairs selected unmatched textures with the selected model entry.
     * Auto-detects matching icons based on color suffix.
     *
     * Example: Model = single_bed, Texture = bed_set_red
     * - Extracts color suffix "red" from texture
     * - Finds icon "single_bed_red" if it exists
     * - Creates entry with material=bed_set_red, decoref=single_bed_red
     */
    private void pairSelectedTexturesWithModel() {
        // Get selected textures from unmatched list
        List<String> selectedTextures = new ArrayList<>(unmatchedListView.getSelectionModel().getSelectedItems());
        if (selectedTextures.isEmpty()) {
            showAlert("No Textures Selected", "Select textures from the Unmatched Textures list first.");
            return;
        }

        // Get selected model entry
        DecoEntry templateEntry = entryListView.getSelectionModel().getSelectedItem();
        if (templateEntry == null || templateEntry.getModel() == null) {
            showAlert("No Model Selected", "Select a model entry from the Entries list first.");
            return;
        }

        String modelName = templateEntry.getModel();
        List<DecoEntry> newEntries = new ArrayList<>();

        for (String textureName : selectedTextures) {
            // Extract color/variant suffix from texture name
            // e.g., "bed_set_red" -> "red", "bed_set_light_blue" -> "light_blue"
            String suffix = extractColorSuffix(textureName);

            // Try to find matching icon: {model}_{suffix}
            String iconName = findMatchingIcon(modelName, suffix);

            // Create new entry
            DecoEntry entry = new DecoEntry();

            // Build display name from model + color suffix (e.g., "Single Bed Light Gray")
            String displayName = EntryBuilder.toDisplayName(modelName + "_" + suffix);

            // Auto-generate decoref as {model}_{suffix} (e.g., single_bed_oak_blue)
            // Use existing icon if found, otherwise generate the name
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

        // Remove the placeholder entry (the template) if it has no material
        if (templateEntry.getMaterial() == null) {
            entries.remove(templateEntry);
        }

        entries.addAll(newEntries);
        unmatchedListView.getItems().setAll(unmatchedTextures);
        entryListView.refresh();
        updateJsonPreview();
        updateIconListView(null);  // Refresh icon status

        // Select the new entries
        entryListView.getSelectionModel().clearSelection();
        for (DecoEntry entry : newEntries) {
            entryListView.getSelectionModel().select(entry);
        }

        // Count how many entries got matched icons
        long matchedIcons = newEntries.stream().filter(e -> iconFiles.containsKey(e.getDecoref())).count();
        showAlert("Pairing Complete",
                String.format("Created %d entries for model '%s'\n(%d with matched icons, %d with generated decorefs)",
                        newEntries.size(), modelName, matchedIcons, newEntries.size() - matchedIcons));
    }

    /**
     * Extracts the color/variant suffix from a texture name.
     * e.g., "bed_set_red" -> "red"
     *       "bed_set_light_blue" -> "light_blue"
     *       "bed_set_white_red" -> "white_red" (frame color + blanket color)
     *       "bed_set_palm_pink" -> "palm_pink" (wood + color)
     */
    private String extractColorSuffix(String textureName) {
        String lower = textureName.toLowerCase();

        // Known blanket/item colors
        String[] knownColors = {
            "light_blue", "light_gray", "dark_gray", "ocean_blue",
            "red", "orange", "yellow", "lime", "green", "cyan", "blue",
            "purple", "magenta", "pink", "brown", "white", "cream",
            "gray", "black"
        };

        // Wood types and frame colors that can prefix the blanket color
        String[] woodsAndFrames = {"birch", "oak", "cherry", "palm", "spruce", "ebony", "white", "black"};

        // Check for wood/frame + color combinations like "palm_pink" or "white_red"
        for (String woodOrFrame : woodsAndFrames) {
            for (String color : knownColors) {
                String combo = woodOrFrame + "_" + color;
                if (lower.endsWith(combo)) {
                    return combo;
                }
            }
            // Also check for wood/frame + multi-word colors like "white_light_blue"
            for (String color : new String[]{"light_blue", "light_gray", "dark_gray", "ocean_blue"}) {
                String combo = woodOrFrame + "_" + color;
                if (lower.endsWith(combo)) {
                    return combo;
                }
            }
        }

        // Check for single color at end (no wood/frame prefix)
        for (String color : knownColors) {
            if (lower.endsWith("_" + color)) {
                return color;
            }
        }

        // Fallback: take everything after the last known prefix pattern
        // e.g., "bed_set_palm_pink" - if we find "bed_set_", take "palm_pink"
        String[] parts = textureName.split("_");
        if (parts.length >= 2) {
            // Return last 1-2 parts as suffix
            if (parts.length >= 3) {
                return parts[parts.length - 2] + "_" + parts[parts.length - 1];
            }
            return parts[parts.length - 1];
        }

        return textureName;
    }

    /**
     * Finds a matching icon texture for the given model and color suffix.
     * e.g., model="single_bed", suffix="red" -> looks for "single_bed_red"
     */
    private String findMatchingIcon(String modelName, String suffix) {
        // Try exact match: {model}_{suffix}
        String exactMatch = modelName + "_" + suffix;
        if (textureFiles.containsKey(exactMatch)) {
            return exactMatch;
        }

        // Try case-insensitive match
        String lowerExact = exactMatch.toLowerCase();
        for (String texName : textureFiles.keySet()) {
            if (texName.toLowerCase().equals(lowerExact)) {
                return texName;
            }
        }

        // Try partial suffix match (e.g., suffix="palm_pink", try just "pink")
        if (suffix.contains("_")) {
            String[] suffixParts = suffix.split("_");
            String lastPart = suffixParts[suffixParts.length - 1];
            String partialMatch = modelName + "_" + lastPart;

            if (textureFiles.containsKey(partialMatch)) {
                return partialMatch;
            }

            // Case-insensitive
            String lowerPartial = partialMatch.toLowerCase();
            for (String texName : textureFiles.keySet()) {
                if (texName.toLowerCase().equals(lowerPartial)) {
                    return texName;
                }
            }
        }

        return null; // No matching icon found
    }

    // === EDITOR ===

    private void refreshTextureDropdowns() {
        // Get all texture names (both matched and unmatched)
        List<String> allTextures = new ArrayList<>();
        allTextures.add(""); // Empty option
        allTextures.addAll(textureFiles.keySet());
        Collections.sort(allTextures.subList(1, allTextures.size())); // Sort excluding empty

        materialDropdown.getItems().setAll(allTextures);
        decorefDropdown.getItems().setAll(allTextures);
    }

    private void refreshOnUseLinkDropdown() {
        // Populate with decorefs and materials from all entries
        List<String> linkTargets = new ArrayList<>();
        linkTargets.add(""); // Empty option
        for (DecoEntry entry : entries) {
            // Prefer decoref, fallback to material
            String target = entry.getDecoref() != null ? entry.getDecoref() : entry.getMaterial();
            if (target != null && !target.isEmpty() && !linkTargets.contains(target)) {
                linkTargets.add(target);
            }
        }
        Collections.sort(linkTargets.subList(1, linkTargets.size())); // Sort excluding empty
        onUseLinkDropdown.getItems().setAll(linkTargets);
    }

    private void loadEntryToEditor(DecoEntry entry) {
        refreshTextureDropdowns();
        refreshAnimationDropdowns(entry.getModel());

        nameField.setText(entry.getName() != null ? entry.getName() : "");
        modelField.setText(entry.getModel() != null ? entry.getModel() : "");
        materialDropdown.setValue(entry.getMaterial() != null ? entry.getMaterial() : "");
        decorefDropdown.setValue(entry.getDecoref() != null ? entry.getDecoref() : "");
        tabsDropdown.setValue(entry.getTabs());
        typeLabel.setText(entry.getType() != null ? entry.getType() : "(none)");

        // Load default animation
        defaultAnimationDropdown.setValue(entry.getDefaultAnimation() != null ? entry.getDefaultAnimation() : "");

        // Load on_use animations
        boolean hasOnUseAnims = entry.getScript() != null && entry.getScript().getOnUse() != null
                && entry.getScript().getOnUse().hasAnimations();
        onUseAnimCheck.setSelected(hasOnUseAnims);
        if (hasOnUseAnims) {
            setAnimationPairsInBox(onUseAnimBox, entry.getScript().getOnUse().getAnimations());
        } else {
            clearAnimationPairBox(onUseAnimBox);
        }

        // Load animation_end animations
        boolean hasAnimEnd = entry.getScript() != null && entry.getScript().getAnimationEnd() != null
                && entry.getScript().getAnimationEnd().hasAnimations();
        animEndCheck.setSelected(hasAnimEnd);
        if (hasAnimEnd) {
            setAnimationPairsInBox(animEndBox, entry.getScript().getAnimationEnd().getAnimations());
        } else {
            clearAnimationPairBox(animEndBox);
        }

        scaleSpinner.getValueFactory().setValue(entry.getScale());
        transparencyCheck.setSelected(entry.getTransparency() != null && entry.getTransparency());

        // Load light value from script
        boolean hasLight = entry.getScript() != null && entry.getScript().getLight() != null;
        lightCheck.setSelected(hasLight);
        if (hasLight) {
            lightSpinner.getValueFactory().setValue(entry.getScript().getLight());
        } else {
            lightSpinner.getValueFactory().setValue(15);
        }

        hiddenCheck.setSelected(entry.getHidden() != null && entry.getHidden());
        lootField.setText(entry.getLoot() != null ? entry.getLoot() : "");

        // Load on_use value from script
        boolean hasOnUse = entry.getScript() != null && entry.getScript().getOnUse() != null
                && entry.getScript().getOnUse().getLink() != null;
        onUseCheck.setSelected(hasOnUse);
        if (hasOnUse) {
            refreshOnUseLinkDropdown();
            onUseLinkDropdown.setValue(entry.getScript().getOnUse().getLink());
        } else {
            onUseLinkDropdown.setValue("");
        }

        // Load flipbook values
        boolean hasFlipbook = entry.getFlipbook() != null;
        flipbookCheck.setSelected(hasFlipbook);
        if (hasFlipbook) {
            flipbookImagesSpinner.getValueFactory().setValue(entry.getFlipbook().getImages());
            flipbookFrametimeSpinner.getValueFactory().setValue(entry.getFlipbook().getFrametime());
        } else {
            flipbookImagesSpinner.getValueFactory().setValue(2);
            flipbookFrametimeSpinner.getValueFactory().setValue(8);
        }
    }

    private void clearEditorFields() {
        refreshTextureDropdowns();

        nameField.setText("");
        modelField.setText("");
        materialDropdown.setValue("");
        decorefDropdown.setValue("");
        tabsDropdown.setValue("");
        typeLabel.setText("-");
        defaultAnimationDropdown.setValue("");
        onUseAnimCheck.setSelected(false);
        clearAnimationPairBox(onUseAnimBox);
        animEndCheck.setSelected(false);
        clearAnimationPairBox(animEndBox);
        scaleSpinner.getValueFactory().setValue(1.0);
        transparencyCheck.setSelected(false);
        lightCheck.setSelected(false);
        lightSpinner.getValueFactory().setValue(15);
        onUseCheck.setSelected(false);
        onUseLinkDropdown.setValue("");
        flipbookCheck.setSelected(false);
        flipbookImagesSpinner.getValueFactory().setValue(2);
        flipbookFrametimeSpinner.getValueFactory().setValue(8);
        hiddenCheck.setSelected(false);
        lootField.setText("");
    }

    private void duplicateSelectedEntries() {
        List<DecoEntry> selected = new ArrayList<>(entryListView.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) return;

        List<DecoEntry> duplicates = new ArrayList<>();
        for (DecoEntry original : selected) {
            DecoEntry copy = new DecoEntry();
            copy.setName(original.getName() + " (copy)");
            copy.setModel(original.getModel());
            copy.setMaterial(null); // Clear material so user can assign new one
            copy.setDecoref(original.getDecoref());
            copy.setTabs(original.getTabs());
            copy.setType(original.getType());
            copy.setDefaultAnimation(original.getDefaultAnimation());
            copy.setScale(original.getScale());
            copy.setTransparency(original.getTransparency());
            copy.setHidden(original.getHidden());
            copy.setLoot(original.getLoot());
            copy.setCraftingColor(original.getCraftingColor().clone());
            // Copy flipbook if present
            if (original.getFlipbook() != null) {
                Flipbook flipbookCopy = new Flipbook();
                flipbookCopy.setImages(original.getFlipbook().getImages());
                flipbookCopy.setFrametime(original.getFlipbook().getFrametime());
                copy.setFlipbook(flipbookCopy);
            }
            // Copy script (including light, on_use, and animation_end)
            if (original.getScript() != null) {
                Script scriptCopy = new Script();
                scriptCopy.setLight(original.getScript().getLight());
                // Copy on_use if present
                if (original.getScript().getOnUse() != null) {
                    AnimationScript onUseCopy = new AnimationScript();
                    onUseCopy.setLink(original.getScript().getOnUse().getLink());
                    if (original.getScript().getOnUse().hasAnimations()) {
                        List<AnimationPair> pairsCopy = new ArrayList<>();
                        for (AnimationPair p : original.getScript().getOnUse().getAnimations()) {
                            pairsCopy.add(new AnimationPair(p.getFrom(), p.getTo()));
                        }
                        onUseCopy.setAnimations(pairsCopy);
                    }
                    scriptCopy.setOnUse(onUseCopy);
                }
                // Copy animation_end if present
                if (original.getScript().getAnimationEnd() != null && original.getScript().getAnimationEnd().hasAnimations()) {
                    AnimationScript animEndCopy = new AnimationScript();
                    List<AnimationPair> pairsCopy = new ArrayList<>();
                    for (AnimationPair p : original.getScript().getAnimationEnd().getAnimations()) {
                        pairsCopy.add(new AnimationPair(p.getFrom(), p.getTo()));
                    }
                    animEndCopy.setAnimations(pairsCopy);
                    scriptCopy.setAnimationEnd(animEndCopy);
                }
                // Note: tool_modelswitch is not copied as it's chain-specific
                copy.setScript(scriptCopy);
            }
            duplicates.add(copy);
        }

        entries.addAll(duplicates);
        entryListView.refresh();
        updateJsonPreview();

        // Select the new duplicates
        entryListView.getSelectionModel().clearSelection();
        for (DecoEntry dup : duplicates) {
            entryListView.getSelectionModel().select(dup);
        }
    }

    private void saveSelectedEntries() {
        List<DecoEntry> selected = entryListView.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) return;

        String materialValue = materialDropdown.getValue();
        if (materialValue == null) materialValue = materialDropdown.getEditor().getText();

        String decorefValue = decorefDropdown.getValue();
        if (decorefValue == null) decorefValue = decorefDropdown.getEditor().getText();

        // Apply non-empty fields to all selected entries
        for (DecoEntry entry : selected) {
            // Only apply if field is not empty (allows partial updates for multi-select)
            if (!nameField.getText().isEmpty()) {
                entry.setName(nameField.getText());
            }
            if (!modelField.getText().isEmpty()) {
                entry.setModel(modelField.getText());
            }
            if (materialValue != null && !materialValue.isEmpty()) {
                entry.setMaterial(materialValue);
                unmatchedTextures.remove(materialValue); // Remove from unmatched if used
            }
            if (decorefValue != null && !decorefValue.isEmpty()) {
                entry.setDecoref(decorefValue);
            }
            if (tabsDropdown.getValue() != null && !tabsDropdown.getValue().isEmpty()) {
                entry.setTabs(tabsDropdown.getValue());
            }
            // Type is auto-detected from model, not editable

            // Handle default animation
            String defaultAnimValue = defaultAnimationDropdown.getValue();
            if (defaultAnimValue == null) defaultAnimValue = defaultAnimationDropdown.getEditor().getText();
            if (defaultAnimValue != null && !defaultAnimValue.isEmpty()) {
                entry.setDefaultAnimation(defaultAnimValue);
            } else {
                entry.setDefaultAnimation(null);
            }

            // Handle on_use animation pairs
            if (onUseAnimCheck.isSelected()) {
                List<AnimationPair> pairs = getAnimationPairsFromBox(onUseAnimBox);
                if (!pairs.isEmpty()) {
                    Script script = entry.getScript();
                    if (script == null) {
                        script = new Script();
                        entry.setScript(script);
                    }
                    AnimationScript onUse = script.getOnUse();
                    if (onUse == null) {
                        onUse = new AnimationScript();
                        script.setOnUse(onUse);
                    }
                    onUse.setAnimations(pairs);
                }
            } else {
                // Clear on_use animations if unchecked (but keep link if present)
                if (entry.getScript() != null && entry.getScript().getOnUse() != null) {
                    entry.getScript().getOnUse().setAnimations(null);
                }
            }

            // Handle animation_end pairs
            if (animEndCheck.isSelected()) {
                List<AnimationPair> pairs = getAnimationPairsFromBox(animEndBox);
                if (!pairs.isEmpty()) {
                    Script script = entry.getScript();
                    if (script == null) {
                        script = new Script();
                        entry.setScript(script);
                    }
                    AnimationScript animEnd = new AnimationScript();
                    animEnd.setAnimations(pairs);
                    script.setAnimationEnd(animEnd);
                }
            } else {
                // Clear animation_end if unchecked
                if (entry.getScript() != null) {
                    entry.getScript().setAnimationEnd(null);
                }
            }

            // Scale always applies (spinner always has a value)
            entry.setScale(scaleSpinner.getValue());
            // Checkboxes: apply if checked
            if (transparencyCheck.isSelected()) {
                entry.setTransparency(true);
            }

            // Handle light setting
            if (lightCheck.isSelected()) {
                Script script = entry.getScript();
                if (script == null) {
                    script = new Script();
                    entry.setScript(script);
                }
                script.setLight(lightSpinner.getValue());
            } else {
                // Clear light if unchecked
                if (entry.getScript() != null) {
                    entry.getScript().setLight(null);
                }
            }

            // Handle on_use link setting (separate from on_use animations)
            if (onUseCheck.isSelected()) {
                String onUseLinkValue = onUseLinkDropdown.getValue();
                if (onUseLinkValue == null) onUseLinkValue = onUseLinkDropdown.getEditor().getText();
                if (onUseLinkValue != null && !onUseLinkValue.isEmpty()) {
                    Script script = entry.getScript();
                    if (script == null) {
                        script = new Script();
                        entry.setScript(script);
                    }
                    script.setOnUseLink(onUseLinkValue);
                }
            } else {
                // Only clear the link, not the entire on_use (keep animations if set)
                if (entry.getScript() != null && entry.getScript().getOnUse() != null) {
                    entry.getScript().getOnUse().setLink(null);
                }
            }

            // Handle flipbook setting
            if (flipbookCheck.isSelected()) {
                Flipbook flipbook = entry.getFlipbook();
                if (flipbook == null) {
                    flipbook = new Flipbook();
                    entry.setFlipbook(flipbook);
                }
                flipbook.setImages(flipbookImagesSpinner.getValue());
                flipbook.setFrametime(flipbookFrametimeSpinner.getValue());
            } else {
                // Clear flipbook if unchecked
                entry.setFlipbook(null);
            }

            if (hiddenCheck.isSelected()) {
                entry.setHidden(true);
                // Save loot value if provided
                if (!lootField.getText().isEmpty()) {
                    entry.setLoot(lootField.getText());
                }
            } else {
                entry.setHidden(false);
                entry.setLoot(null); // Clear loot if not hidden
            }
        }

        entryListView.refresh();
        updateJsonPreview();
    }

    // === JSON PREVIEW ===

    private void updateJsonPreview() {
        if (entries.isEmpty()) {
            jsonPreview.setText("// Drop models and textures to generate entries");
            return;
        }

        String json = JsonExporter.toJson(new ArrayList<>(entries));
        jsonPreview.setText(json);
    }

    // === EXPORT / RESET ===

    private void exportJson(Stage stage) {
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

    private void resetAll() {
        entries.clear();
        modelFiles.clear();
        textureFiles.clear();
        iconFiles.clear();
        unmatchedTextures.clear();
        parsedModels.clear();
        entryListView.refresh();
        iconListView.getItems().clear();
        updateJsonPreview();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // === CHAIN BUILDING ===

    private void buildRainbowChain() {
        if (entries.isEmpty()) {
            showAlert("No Entries", "Add some entries first.");
            return;
        }

        // Build chains and get sorted order
        List<DecoEntry> sorted = ChainBuilder.buildRainbowChains(new ArrayList<>(entries));

        // Apply the sorted order to our entries list
        entries.clear();
        entries.addAll(sorted);

        entryListView.refresh();
        updateJsonPreview();

        showAlert("Rainbow Chain Built",
                "Entries have been sorted and linked in rainbow color order.\n" +
                "Check the JSON for 'tool_modelswitch' links.");
    }

    private void buildWoodChain() {
        if (entries.isEmpty()) {
            showAlert("No Entries", "Add some entries first.");
            return;
        }

        // Build chains and get sorted order
        List<DecoEntry> sorted = ChainBuilder.buildWoodChains(new ArrayList<>(entries));

        // Apply the sorted order to our entries list
        entries.clear();
        entries.addAll(sorted);

        entryListView.refresh();
        updateJsonPreview();

        showAlert("Wood Chain Built",
                "Entries have been sorted and linked in wood type order.\n" +
                "Check the JSON for 'tool_modelswitch' links.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}