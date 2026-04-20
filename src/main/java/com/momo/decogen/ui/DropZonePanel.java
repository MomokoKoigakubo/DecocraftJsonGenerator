package com.momo.decogen.ui;

import com.momo.decogen.model.DecoEntry;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DropZonePanel {

    private final VBox root;

    public DropZonePanel(AppController controller) {
        root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setPrefWidth(250);
        root.setStyle("-fx-background-color: #2f3136;");

        Label title = new Label("Drop Files");
        title.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 14px; -fx-font-weight: bold;");

        VBox modelZone = createDropBox("Models (.bbmodel)", "#5865F2", files -> {
            for (File file : files) {
                if (file.getName().toLowerCase().endsWith(".bbmodel")) {
                    controller.addModel(file);
                }
            }
        });

        VBox textureZone = createDropBox("Textures (.png)", "#3ba55c", files -> {
            for (File file : files) {
                if (file.getName().toLowerCase().endsWith(".png")) {
                    controller.addTexture(file);
                }
            }
        });

        VBox iconZone = createDropBox("Icons (.png)", "#faa61a", files -> {
            for (File file : files) {
                if (file.getName().toLowerCase().endsWith(".png")) {
                    controller.addIcon(file);
                }
            }
        });

        Label iconLabel = new Label("Dropped Icons (" + controller.getIconFiles().size() + "):");
        iconLabel.setStyle("-fx-text-fill: #faa61a; -fx-font-size: 12px;");

        ListView<String> iconListView = new ListView<>();
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

        Label unmatchedLabel = new Label("Unmatched Textures:");
        unmatchedLabel.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 12px;");

        ListView<String> unmatchedListView = new ListView<>();
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

        unmatchedListView.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("DELETE")) {
                List<String> selected = new ArrayList<>(unmatchedListView.getSelectionModel().getSelectedItems());
                if (!selected.isEmpty()) {
                    controller.removeUnmatchedTextures(selected);
                }
            }
        });

        controller.setIconListView(iconListView);
        controller.setUnmatchedListView(unmatchedListView);
        controller.setIconLabel(iconLabel);

        // Keep lists in sync when entries change
        controller.getEntries().addListener((ListChangeListener<DecoEntry>) c -> {
            unmatchedListView.getItems().setAll(controller.getUnmatchedTextures());
            controller.updateIconListView();
        });

        Button pairBtn = new Button("Pair Selected with Entries");
        pairBtn.setMaxWidth(Double.MAX_VALUE);
        pairBtn.setStyle("-fx-background-color: #5865F2; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand;");
        pairBtn.setOnAction(e -> controller.pairSelectedTexturesWithModel());
        pairBtn.setTooltip(TopBar.tooltip("Create entries from selected unmatched textures using the selected entries as templates. Select multiple template entries (Ctrl-click) to fan out each texture across every selected model."));

        Label pairHint = new Label("Drops auto match icons and textures. Use Pair Selected for manual overrides.");
        pairHint.setStyle("-fx-text-fill: #72767d; -fx-font-size: 9px;");
        pairHint.setWrapText(true);

        root.getChildren().addAll(title, modelZone, textureZone, iconZone,
                new Separator(Orientation.HORIZONTAL),
                iconLabel, iconListView,
                unmatchedLabel, unmatchedListView, pairBtn, pairHint);
        VBox.setVgrow(unmatchedListView, Priority.SOMETIMES);
        VBox.setVgrow(iconListView, Priority.SOMETIMES);
    }

    public VBox getRoot() {
        return root;
    }

    private static VBox createDropBox(String label, String color, Consumer<List<File>> handler) {
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
}