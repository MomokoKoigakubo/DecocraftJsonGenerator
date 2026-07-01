package com.momo.decogen.ui;

import com.momo.decogen.model.DecoEntry;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

public class TopBar {

    private final HBox root;

    public TopBar(AppController controller, Stage stage) {
        root = new HBox(10);
        root.setPadding(new Insets(10));
        root.setAlignment(Pos.CENTER_LEFT);
        root.setStyle("-fx-background-color: #2f3136;");

        Button exportBtn = new Button("Export JSON");
        exportBtn.setPrefHeight(35);
        exportBtn.setStyle("-fx-background-color: #5865F2; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand;");
        exportBtn.setOnAction(e -> controller.exportJson(stage));
        exportBtn.setTooltip(tooltip("Save all entries to a JSON file"));

        Button importBtn = new Button("Import JSON…");
        importBtn.setPrefHeight(35);
        importBtn.setStyle("-fx-background-color: #5865F2; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand;");
        importBtn.setOnAction(e -> controller.importJson());
        importBtn.setTooltip(tooltip("Paste a JSON array of entries to load and edit"));

        Button resetBtn = new Button("Reset All");
        resetBtn.setPrefHeight(35);
        resetBtn.setStyle("-fx-background-color: #ed4245; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand;");
        resetBtn.setOnAction(e -> controller.resetAll());
        resetBtn.setTooltip(tooltip("Clear all entries, models, textures, and icons"));

        Button autoMatchBtn = new Button("Auto Match");
        autoMatchBtn.setPrefHeight(35);
        autoMatchBtn.setStyle("-fx-background-color: #3ba55c; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand;");
        autoMatchBtn.setOnAction(e -> controller.autoMatchTextures());
        autoMatchBtn.setTooltip(tooltip("Automatically assign unmatched textures to entries based on naming"));

        Button normalizeBtn = new Button("Normalize\u2026");
        normalizeBtn.setPrefHeight(35);
        normalizeBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand;");
        normalizeBtn.setOnAction(e -> controller.normalizeRemoveWord());
        normalizeBtn.setTooltip(tooltip("Pick a word and strip it from every entry's name, decoref, and material"));

        Button addWordBtn = new Button("Add Word\u2026");
        addWordBtn.setPrefHeight(35);
        addWordBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand;");
        addWordBtn.setOnAction(e -> controller.normalizeAddWord());
        addWordBtn.setTooltip(tooltip("Insert a word at a chosen position in every entry's name, decoref, and/or material"));

        Button sortBtn = new Button("Sort\u2026");
        sortBtn.setPrefHeight(35);
        sortBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand;");
        sortBtn.setTooltip(tooltip("Reorder entries by Metal, Colour, Number, and/or Alphabetical \u2014 combine axes in one dialog."));
        sortBtn.setOnAction(e -> controller.openSortDialog());

        // Chain / Link by Order / Pair on_use share one style (they all build
        // links). USE_PREF_SIZE stops the labels truncating in a crowded bar.
        String linkStyle = "-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand;";

        Button chainBtn = new Button("Chain…");
        chainBtn.setPrefHeight(35);
        chainBtn.setMinWidth(Region.USE_PREF_SIZE);
        chainBtn.setStyle(linkStyle);
        chainBtn.setOnAction(e -> controller.openChainDialog());
        chainBtn.setTooltip(tooltip("Build tool_modelswitch chains — drag to weight Colour / Wood / State pairs / list order and combine them."));

        Button orderLinkBtn = new Button("Link by Order");
        orderLinkBtn.setPrefHeight(35);
        orderLinkBtn.setMinWidth(Region.USE_PREF_SIZE);
        orderLinkBtn.setStyle(linkStyle);
        orderLinkBtn.setOnAction(e -> controller.linkEntriesByOrder());
        orderLinkBtn.setTooltip(tooltip("Link each entry to the next in the current list order (tool_modelswitch). Drag entries to reorder first."));

        Button pairBtn = new Button("Pair on_use…");
        pairBtn.setPrefHeight(35);
        pairBtn.setMinWidth(Region.USE_PREF_SIZE);
        pairBtn.setStyle(linkStyle);
        pairBtn.setOnAction(e -> controller.openPairDialog());
        pairBtn.setTooltip(tooltip("Link entries with on_use (right-click) — pick By State / By Order / By Sequential in a dialog."));

        Label statsLabel = new Label();
        statsLabel.setStyle("-fx-text-fill: #72767d; -fx-font-size: 12px;");
        controller.getEntries().addListener((ListChangeListener<DecoEntry>) c -> {
            long matched = controller.getEntries().stream().filter(e2 -> e2.getMaterial() != null).count();
            statsLabel.setText(String.format("%d entries (%d matched, %d need textures)",
                    controller.getEntries().size(), matched, controller.getEntries().size() - matched));
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        root.getChildren().addAll(exportBtn, importBtn, autoMatchBtn, sortBtn, normalizeBtn, addWordBtn, chainBtn, orderLinkBtn, pairBtn, resetBtn, spacer, statsLabel);
    }

    public HBox getRoot() {
        return root;
    }

    static Tooltip tooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setShowDelay(Duration.millis(200));
        tooltip.setShowDuration(Duration.seconds(10));
        tooltip.setStyle("-fx-font-size: 12px;");
        return tooltip;
    }
}