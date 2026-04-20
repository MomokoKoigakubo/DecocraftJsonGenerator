package com.momo.decogen.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class JsonPreviewPanel {

    private final VBox root;
    private final TextArea jsonPreview;

    public JsonPreviewPanel() {
        root = new VBox(5);
        root.setPadding(new Insets(10));
        root.setMinHeight(100);
        root.setStyle("-fx-background-color: #2f3136;");

        Label title = new Label("JSON Preview");
        title.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 14px; -fx-font-weight: bold;");

        jsonPreview = new TextArea();
        jsonPreview.setEditable(false);
        jsonPreview.setWrapText(false);
        jsonPreview.setStyle("-fx-control-inner-background: #1e1f22; -fx-text-fill: #dcddde; -fx-font-family: monospace;");
        VBox.setVgrow(jsonPreview, Priority.ALWAYS);

        root.getChildren().addAll(title, jsonPreview);
    }

    public VBox getRoot() {
        return root;
    }

    public TextArea getJsonPreview() {
        return jsonPreview;
    }
}