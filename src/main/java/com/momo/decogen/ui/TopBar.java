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

        Button resetBtn = new Button("Reset All");
        resetBtn.setPrefHeight(35);
        resetBtn.setStyle("-fx-background-color: #ed4245; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand;");
        resetBtn.setOnAction(e -> controller.resetAll());
        resetBtn.setTooltip(tooltip("Clear all entries, models, textures, and icons"));

        Button autoMatchBtn = new Button("Auto Match Textures");
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

        // Rainbow button: OKLab-ish 12-stop gradient + stroked Text so the
        // letters stay legible against every band of the spectrum. The dual
        // background layer fakes a 1px dark edge without changing layout.
        Text rainbowLabel = new Text("Rainbow Chain");
        rainbowLabel.setFill(Color.WHITE);
        rainbowLabel.setStroke(Color.rgb(0, 0, 0, 0.85));
        rainbowLabel.setStrokeWidth(0.9);
        rainbowLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

        Button rainbowChainBtn = new Button();
        rainbowChainBtn.setGraphic(rainbowLabel);
        rainbowChainBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        rainbowChainBtn.setAlignment(Pos.CENTER);
        rainbowChainBtn.setPrefHeight(35);
        rainbowChainBtn.setMinHeight(35);
        rainbowChainBtn.setMaxHeight(35);
        rainbowChainBtn.setStyle(
                "-fx-background-color: rgba(0,0,0,0.55), linear-gradient(to right, "
                + "#ff4d4d, #ff8a33, #ffc21f, #f5e93a, #aed852, "
                + "#4dcc75, #1fc4a8, #2aa8dd, #4f7ff0, #8b5ce0, "
                + "#c75bc7, #ff5ca3); "
                + "-fx-background-insets: 0, 1; "
                + "-fx-background-radius: 4, 3; "
                + "-fx-padding: 0 12 0 12; "
                + "-fx-cursor: hand;"
        );
        rainbowChainBtn.setOnAction(e -> controller.buildRainbowChain());
        rainbowChainBtn.setTooltip(tooltip("Sort entries by color and link them in rainbow order (tool_modelswitch)"));

        // Wood button: horizontal gradient that evokes spruce plank grain
        // (warm brown with darker/lighter stripes). Stroked text for parity
        // with the rainbow button.
        Text woodLabel = new Text("Wood Chain");
        woodLabel.setFill(Color.WHITE);
        woodLabel.setStroke(Color.rgb(0, 0, 0, 0.85));
        woodLabel.setStrokeWidth(0.9);
        woodLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

        Button woodChainBtn = new Button();
        woodChainBtn.setGraphic(woodLabel);
        woodChainBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        woodChainBtn.setAlignment(Pos.CENTER);
        woodChainBtn.setPrefHeight(35);
        woodChainBtn.setMinHeight(35);
        woodChainBtn.setMaxHeight(35);
        // 16 hard-stop color blocks = one row of a spruce-plank texture,
        // stretched across the button so each pixel reads as a chunky block
        // without interpolation (no gradient blur).
        String sprucePixels =
                "#5a3e24 0%, #5a3e24 6.25%,"
                + "#7a5738 6.25%, #7a5738 12.5%,"
                + "#98744e 12.5%, #98744e 18.75%,"
                + "#6e5030 18.75%, #6e5030 25%,"
                + "#4d3821 25%, #4d3821 31.25%,"
                + "#80603a 31.25%, #80603a 37.5%,"
                + "#9a7550 37.5%, #9a7550 43.75%,"
                + "#705234 43.75%, #705234 50%,"
                + "#8a6842 50%, #8a6842 56.25%,"
                + "#5a3e24 56.25%, #5a3e24 62.5%,"
                + "#7b5a3a 62.5%, #7b5a3a 68.75%,"
                + "#a07e56 68.75%, #a07e56 75%,"
                + "#806040 75%, #806040 81.25%,"
                + "#604428 81.25%, #604428 87.5%,"
                + "#705030 87.5%, #705030 93.75%,"
                + "#88694a 93.75%, #88694a 100%";
        woodChainBtn.setStyle(
                "-fx-background-color: rgba(0,0,0,0.55), "
                + "linear-gradient(to right, " + sprucePixels + "); "
                + "-fx-background-insets: 0, 1; "
                + "-fx-background-radius: 4, 3; "
                + "-fx-padding: 0 12 0 12; "
                + "-fx-cursor: hand;"
        );
        woodChainBtn.setOnAction(e -> controller.buildWoodChain());
        woodChainBtn.setTooltip(tooltip("Sort entries by wood type and link them (tool_modelswitch)"));

        Button orderLinkBtn = new Button("Link by Order");
        orderLinkBtn.setPrefHeight(35);
        orderLinkBtn.setStyle("-fx-background-color: #ff00ff; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand;");
        orderLinkBtn.setOnAction(e -> controller.linkEntriesByOrder());
        orderLinkBtn.setTooltip(tooltip("Link each entry to the next in the current list order (tool_modelswitch). Drag entries to reorder first."));

        Button linkPairsBtn = new Button("Link Pairs\u2026");
        linkPairsBtn.setPrefHeight(35);
        linkPairsBtn.setStyle("-fx-background-color: #f4a13c; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand;");
        linkPairsBtn.setOnAction(e -> controller.linkStatePairs());
        linkPairsBtn.setTooltip(tooltip("Pair base \u2194 variant entries (e.g., closet \u2194 closet_open) via on_use.link. Run Rainbow/Wood Chain afterward to cycle colors within each state."));

        Label statsLabel = new Label();
        statsLabel.setStyle("-fx-text-fill: #72767d; -fx-font-size: 12px;");
        controller.getEntries().addListener((ListChangeListener<DecoEntry>) c -> {
            long matched = controller.getEntries().stream().filter(e2 -> e2.getMaterial() != null).count();
            statsLabel.setText(String.format("%d entries (%d matched, %d need textures)",
                    controller.getEntries().size(), matched, controller.getEntries().size() - matched));
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        root.getChildren().addAll(exportBtn, autoMatchBtn, normalizeBtn, addWordBtn, rainbowChainBtn, woodChainBtn, orderLinkBtn, linkPairsBtn, resetBtn, spacer, statsLabel);
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