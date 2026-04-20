package com.momo.decogen.ui;

import com.momo.decogen.model.SoundPair;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * A VBox widget for editing a list of animation-synced sounds
 * (from -> to: sound, with optional loop).
 * Stateless helper — call static methods on a VBox created by {@link #create(String)}.
 */
public final class SoundPairBox {

    private SoundPairBox() {}

    public static VBox create(String id) {
        VBox box = new VBox(5);
        box.setStyle("-fx-padding: 5; -fx-background-color: #40444b; -fx-background-radius: 4;");

        ListView<HBox> pairsList = new ListView<>();
        pairsList.setPrefHeight(100);
        pairsList.setStyle("-fx-background-color: #2f3136; -fx-control-inner-background: #2f3136;");
        pairsList.setId(id + "_sound_pairs");
        pairsList.setPlaceholder(buildExampleRow());

        HBox addRow1 = new HBox(5);
        addRow1.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> fromDropdown = new ComboBox<>();
        fromDropdown.setEditable(true);
        fromDropdown.setPromptText("e.g., idle");
        fromDropdown.setStyle("-fx-background-color: white; -fx-pref-width: 100;");
        fromDropdown.setId(id + "_sound_from");

        Label arrowLabel = new Label("\u2192");
        arrowLabel.setStyle("-fx-text-fill: #dcddde;");

        ComboBox<String> toDropdown = new ComboBox<>();
        toDropdown.setEditable(true);
        toDropdown.setPromptText("e.g., open (omit = looping)");
        toDropdown.setStyle("-fx-background-color: white; -fx-pref-width: 100;");
        toDropdown.setId(id + "_sound_to");

        addRow1.getChildren().addAll(fromDropdown, arrowLabel, toDropdown);

        HBox addRow2 = new HBox(5);
        addRow2.setAlignment(Pos.CENTER_LEFT);

        TextField soundField = new TextField();
        soundField.setPromptText("e.g., doorbell");
        soundField.setStyle("-fx-background-color: white; -fx-text-fill: black;");
        HBox.setHgrow(soundField, Priority.ALWAYS);

        CheckBox loopCheck = new CheckBox("loop");
        loopCheck.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 10px;");

        Button addBtn = new Button("+");
        addBtn.setStyle("-fx-background-color: #3ba55c; -fx-text-fill: white; -fx-cursor: hand;");
        addBtn.setOnAction(e -> {
            String from = fromDropdown.getValue();
            if (from == null) from = fromDropdown.getEditor().getText();
            String to = toDropdown.getValue();
            if (to == null) to = toDropdown.getEditor().getText();
            String sound = soundField.getText();
            boolean loop = loopCheck.isSelected();

            boolean anyContent = (from != null && !from.isEmpty())
                    || (to != null && !to.isEmpty())
                    || (sound != null && !sound.isEmpty());
            if (!anyContent) return;

            HBox row = createRow(from, to, sound, loop, pairsList);
            pairsList.getItems().add(row);
            fromDropdown.setValue("");
            toDropdown.setValue("");
            soundField.setText("");
            loopCheck.setSelected(false);
        });

        addRow2.getChildren().addAll(soundField, loopCheck, addBtn);

        Label hint = new Label("Add from\u2192to with sound id (loop = play while in 'from')");
        hint.setStyle("-fx-text-fill: #72767d; -fx-font-size: 9px;");

        box.getChildren().addAll(pairsList, addRow1, addRow2, hint);
        return box;
    }

    /** Faded preview row shown while the list is empty. */
    private static Node buildExampleRow() {
        HBox ghost = new HBox(5);
        ghost.setAlignment(Pos.CENTER_LEFT);
        ghost.setMouseTransparent(true);
        ghost.setOpacity(0.45);

        Label fromLabel = new Label("idle");
        fromLabel.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 10px; -fx-font-style: italic;");

        Label arrow = new Label("\u2192");
        arrow.setStyle("-fx-text-fill: #72767d;");

        Label toLabel = new Label("open");
        toLabel.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 10px; -fx-font-style: italic;");

        Label soundLabel = new Label(": doorbell");
        soundLabel.setStyle("-fx-text-fill: #faa61a; -fx-font-size: 10px; -fx-font-style: italic;");

        Label loopLabel = new Label("\u21bb");
        loopLabel.setStyle("-fx-text-fill: #3ba55c; -fx-font-size: 11px;");

        Label tag = new Label("  (example)");
        tag.setStyle("-fx-text-fill: #72767d; -fx-font-size: 9px; -fx-font-style: italic;");

        ghost.getChildren().addAll(fromLabel, arrow, toLabel, soundLabel, loopLabel, tag);
        return ghost;
    }

    private static HBox createRow(String from, String to, String sound, boolean loop, ListView<HBox> parentList) {
        HBox row = new HBox(5);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setUserData(new Object[]{
                from != null ? from : "",
                to != null ? to : "",
                sound != null ? sound : "",
                loop
        });

        Label fromLabel = new Label(from != null && !from.isEmpty() ? from : "(any)");
        fromLabel.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 10px;");

        Label arrow = new Label("\u2192");
        arrow.setStyle("-fx-text-fill: #72767d;");

        Label toLabel = new Label(to != null && !to.isEmpty() ? to : "(-)");
        toLabel.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 10px;");

        Label soundLabel = new Label(": " + (sound != null && !sound.isEmpty() ? sound : "(none)"));
        soundLabel.setStyle("-fx-text-fill: #faa61a; -fx-font-size: 10px;");

        Label loopLabel = new Label(loop ? "\u21bb" : "");
        loopLabel.setStyle("-fx-text-fill: #3ba55c; -fx-font-size: 11px;");

        Button removeBtn = new Button("\u00d7");
        removeBtn.setStyle("-fx-background-color: #ed4245; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 0 5;");
        removeBtn.setOnAction(e -> parentList.getItems().remove(row));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(fromLabel, arrow, toLabel, soundLabel, loopLabel, spacer, removeBtn);
        return row;
    }

    /** Update the from/to ComboBoxes with the given animation-state names. */
    @SuppressWarnings("unchecked")
    public static void updateAnimationDropdowns(VBox box, List<String> animations) {
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
    public static List<SoundPair> getPairs(VBox box) {
        List<SoundPair> pairs = new ArrayList<>();
        for (Node node : box.getChildren()) {
            if (node instanceof ListView) {
                ListView<HBox> list = (ListView<HBox>) node;
                for (HBox row : list.getItems()) {
                    Object data = row.getUserData();
                    if (data instanceof Object[]) {
                        Object[] arr = (Object[]) data;
                        String from = (String) arr[0];
                        String to = (String) arr[1];
                        String sound = (String) arr[2];
                        boolean loop = (boolean) arr[3];
                        if (isEmpty(from) && isEmpty(to) && isEmpty(sound)) continue;
                        pairs.add(new SoundPair(
                                isEmpty(from) ? null : from,
                                isEmpty(to) ? null : to,
                                isEmpty(sound) ? null : sound,
                                loop ? Boolean.TRUE : null
                        ));
                    }
                }
            }
        }
        return pairs;
    }

    @SuppressWarnings("unchecked")
    public static void setPairs(VBox box, List<SoundPair> pairs) {
        for (Node node : box.getChildren()) {
            if (node instanceof ListView) {
                ListView<HBox> list = (ListView<HBox>) node;
                list.getItems().clear();
                if (pairs != null) {
                    for (SoundPair p : pairs) {
                        list.getItems().add(createRow(
                                p.getFrom(), p.getTo(), p.getSound(),
                                Boolean.TRUE.equals(p.getLoop()),
                                list));
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

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
}