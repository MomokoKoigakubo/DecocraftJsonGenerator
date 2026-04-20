package com.momo.decogen.ui;

import com.momo.decogen.model.AnimationPair;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * A VBox widget for editing a list of animation (from -> to) pairs.
 * Stateless helper — call static methods on a VBox created by {@link #create(String)}.
 */
public final class AnimationPairBox {

    private AnimationPairBox() {}

    public static VBox create(String id) {
        VBox box = new VBox(5);
        box.setStyle("-fx-padding: 5; -fx-background-color: #40444b; -fx-background-radius: 4;");

        ListView<HBox> pairsList = new ListView<>();
        pairsList.setPrefHeight(100);
        pairsList.setStyle("-fx-background-color: #2f3136; -fx-control-inner-background: #2f3136;");
        pairsList.setId(id + "_pairs");
        pairsList.setPlaceholder(buildExampleRow());

        HBox addRow = new HBox(5);
        addRow.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> fromDropdown = new ComboBox<>();
        fromDropdown.setEditable(true);
        fromDropdown.setPromptText("e.g., idle or any");
        fromDropdown.setStyle("-fx-background-color: white; -fx-pref-width: 100;");
        fromDropdown.setId(id + "_from");

        Label arrowLabel = new Label("\u2192");
        arrowLabel.setStyle("-fx-text-fill: #dcddde;");

        ComboBox<String> toDropdown = new ComboBox<>();
        toDropdown.setEditable(true);
        toDropdown.setPromptText("e.g., open or any_other");
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
                HBox pairRow = createRow(from, to, pairsList);
                pairsList.getItems().add(pairRow);
                fromDropdown.setValue("");
                toDropdown.setValue("");
            }
        });

        addRow.getChildren().addAll(fromDropdown, arrowLabel, toDropdown, addBtn);

        Label hint = new Label("Add from\u2192to pairs (use 'any' for wildcards)");
        hint.setStyle("-fx-text-fill: #72767d; -fx-font-size: 9px;");

        box.getChildren().addAll(pairsList, addRow, hint);
        return box;
    }

    /** Faded preview row shown while the list is empty. */
    private static Node buildExampleRow() {
        HBox ghost = new HBox(5);
        ghost.setAlignment(Pos.CENTER_LEFT);
        ghost.setMouseTransparent(true);
        ghost.setOpacity(0.45);

        Label fromLabel = new Label("idle");
        fromLabel.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 11px; -fx-font-style: italic;");

        Label arrow = new Label("\u2192");
        arrow.setStyle("-fx-text-fill: #72767d;");

        Label toLabel = new Label("open");
        toLabel.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 11px; -fx-font-style: italic;");

        Label tag = new Label("  (example)");
        tag.setStyle("-fx-text-fill: #72767d; -fx-font-size: 9px; -fx-font-style: italic;");

        ghost.getChildren().addAll(fromLabel, arrow, toLabel, tag);
        return ghost;
    }

    private static HBox createRow(String from, String to, ListView<HBox> parentList) {
        HBox row = new HBox(5);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setUserData(new String[]{from != null ? from : "", to != null ? to : ""});

        Label fromLabel = new Label(from != null && !from.isEmpty() ? from : "(empty)");
        fromLabel.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 11px;");

        Label arrow = new Label("\u2192");
        arrow.setStyle("-fx-text-fill: #72767d;");

        Label toLabel = new Label(to != null && !to.isEmpty() ? to : "(empty)");
        toLabel.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 11px;");

        Button removeBtn = new Button("\u00d7");
        removeBtn.setStyle("-fx-background-color: #ed4245; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 0 5;");
        removeBtn.setOnAction(e -> parentList.getItems().remove(row));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(fromLabel, arrow, toLabel, spacer, removeBtn);
        return row;
    }

    @SuppressWarnings("unchecked")
    public static void updateDropdowns(VBox box, List<String> animations) {
        for (Node node : box.getChildren()) {
            if (node instanceof HBox) {
                for (Node child : ((HBox) node).getChildren()) {
                    if (child instanceof ComboBox) {
                        ((ComboBox<String>) child).getItems().setAll(animations);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static List<AnimationPair> getPairs(VBox box) {
        List<AnimationPair> pairs = new ArrayList<>();
        for (Node node : box.getChildren()) {
            if (node instanceof ListView) {
                ListView<HBox> list = (ListView<HBox>) node;
                for (HBox row : list.getItems()) {
                    Object data = row.getUserData();
                    if (data instanceof String[]) {
                        String[] pairData = (String[]) data;
                        String from = pairData[0];
                        String to = pairData[1];
                        if ((from != null && !from.isEmpty()) || (to != null && !to.isEmpty())) {
                            pairs.add(new AnimationPair(from, to));
                        }
                    }
                }
            }
        }
        return pairs;
    }

    @SuppressWarnings("unchecked")
    public static void setPairs(VBox box, List<AnimationPair> pairs) {
        for (Node node : box.getChildren()) {
            if (node instanceof ListView) {
                ListView<HBox> list = (ListView<HBox>) node;
                list.getItems().clear();
                if (pairs != null) {
                    for (AnimationPair pair : pairs) {
                        list.getItems().add(createRow(pair.getFrom(), pair.getTo(), list));
                    }
                }
                break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void clear(VBox box) {
        for (Node node : box.getChildren()) {
            if (node instanceof ListView) {
                ((ListView<HBox>) node).getItems().clear();
            }
        }
    }
}