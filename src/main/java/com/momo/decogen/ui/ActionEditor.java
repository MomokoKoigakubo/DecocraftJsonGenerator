package com.momo.decogen.ui;

import com.momo.decogen.model.Action;
import com.momo.decogen.model.AnimationPair;
import com.momo.decogen.model.SoundPair;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Editor for a single script Action (on_use, shift_on_use, added, trigger,
 * animation_start, animation_end, tool_modelswitch).
 *
 * Wraps everything in a TitledPane so each action collapses by default.
 * The enable checkbox inside controls whether the action is emitted at all.
 */
public class ActionEditor {

    private final TitledPane root;
    private final CheckBox enableCheck;
    private final VBox contentBox;

    private final ComboBox<String> linkDropdown;
    private final TextField soundField;
    private final VBox animationsBox;
    private final VBox soundsBox;

    private final CheckBox storageCheck;
    private final Spinner<Integer> storageColsSpinner;
    private final Spinner<Integer> storageRowsSpinner;
    private final HBox storageRow;

    public ActionEditor(String actionKey) {
        root = new TitledPane();
        root.setText(actionKey);
        root.setExpanded(false);
        // Default TitledPane header is a light gradient — use dark, bold text so it's readable.
        root.setStyle("-fx-text-fill: #1e1f22; -fx-font-weight: bold;");

        VBox body = new VBox(5);
        body.setPadding(new Insets(5));

        enableCheck = new CheckBox("Enable " + actionKey);
        enableCheck.setStyle("-fx-text-fill: #dcddde;");

        contentBox = new VBox(5);
        contentBox.setVisible(false);
        contentBox.setManaged(false);
        enableCheck.selectedProperty().addListener((obs, o, n) -> {
            contentBox.setVisible(n);
            contentBox.setManaged(n);
        });

        // Link
        Label linkLabel = label("Link (decoref target)");
        linkDropdown = new ComboBox<>();
        linkDropdown.setEditable(true);
        linkDropdown.setStyle("-fx-background-color: white; -fx-text-fill: black;");
        linkDropdown.setMaxWidth(Double.MAX_VALUE);
        linkDropdown.setPromptText("Select or type decoref");

        // Sound (simple)
        Label soundLabel = label("Sound (one shot id)");
        soundField = new TextField();
        soundField.setPromptText("e.g., doorbell");
        soundField.setStyle("-fx-background-color: white; -fx-text-fill: black;");

        // Animations
        Label animLabel = label("Animations (from \u2192 to)");
        animationsBox = AnimationPairBox.create(actionKey + "_anim");

        // Sounds (animation synced)
        Label soundsLabel = label("Sounds (animation synced)");
        soundsBox = SoundPairBox.create(actionKey + "_sounds");

        // Storage
        storageCheck = new CheckBox("Storage (inventory)");
        storageCheck.setStyle("-fx-text-fill: #dcddde;");

        storageColsSpinner = new Spinner<>(1, 12, 3, 1);
        storageColsSpinner.setEditable(true);
        storageColsSpinner.setPrefWidth(70);
        storageRowsSpinner = new Spinner<>(1, 12, 3, 1);
        storageRowsSpinner.setEditable(true);
        storageRowsSpinner.setPrefWidth(70);

        storageRow = new HBox(5);
        storageRow.setAlignment(Pos.CENTER_LEFT);
        storageRow.getChildren().addAll(new Label("cols"), storageColsSpinner,
                new Label("rows"), storageRowsSpinner);
        storageRow.setVisible(false);
        storageRow.setManaged(false);
        storageCheck.selectedProperty().addListener((obs, o, n) -> {
            storageRow.setVisible(n);
            storageRow.setManaged(n);
        });
        for (javafx.scene.Node child : storageRow.getChildren()) {
            if (child instanceof Label) {
                ((Label) child).setStyle("-fx-text-fill: #b9bbbe; -fx-font-size: 10px;");
            }
        }

        contentBox.getChildren().addAll(
                linkLabel, linkDropdown,
                soundLabel, soundField,
                animLabel, animationsBox,
                soundsLabel, soundsBox,
                storageCheck, storageRow
        );

        body.getChildren().addAll(enableCheck, contentBox);
        root.setContent(body);
    }

    public TitledPane getRoot() {
        return root;
    }

    public void refreshLinkOptions(List<String> linkTargets) {
        String current = linkDropdown.getValue();
        linkDropdown.getItems().setAll(linkTargets);
        if (current != null) linkDropdown.setValue(current);
    }

    public void refreshAnimationOptions(List<String> animations) {
        AnimationPairBox.updateDropdowns(animationsBox, animations);
        SoundPairBox.updateAnimationDropdowns(soundsBox, animations);
    }

    public void loadAction(Action action) {
        if (action == null || action.isEmpty()) {
            clear();
            return;
        }
        enableCheck.setSelected(true);
        linkDropdown.setValue(action.getLink() != null ? action.getLink() : "");
        soundField.setText(action.getSound() != null ? action.getSound() : "");
        AnimationPairBox.setPairs(animationsBox, action.getAnimations());
        SoundPairBox.setPairs(soundsBox, action.getSounds());

        int[] storage = action.getStorage();
        if (storage != null && storage.length >= 2) {
            storageCheck.setSelected(true);
            storageColsSpinner.getValueFactory().setValue(storage[0]);
            storageRowsSpinner.getValueFactory().setValue(storage[1]);
        } else {
            storageCheck.setSelected(false);
        }
    }

    /**
     * Build an Action from the current UI state.
     * Returns null if the enable checkbox is off or the resulting action
     * would have no content.
     */
    public Action buildAction() {
        if (!enableCheck.isSelected()) return null;

        Action a = new Action();

        String link = linkDropdown.getValue();
        if (link == null || link.isEmpty()) link = linkDropdown.getEditor().getText();
        if (link != null && !link.isEmpty()) a.setLink(link);

        String sound = soundField.getText();
        if (sound != null && !sound.isEmpty()) a.setSound(sound);

        List<AnimationPair> anims = AnimationPairBox.getPairs(animationsBox);
        if (!anims.isEmpty()) a.setAnimations(anims);

        List<SoundPair> sounds = SoundPairBox.getPairs(soundsBox);
        if (!sounds.isEmpty()) a.setSounds(sounds);

        if (storageCheck.isSelected()) {
            a.setStorage(new int[]{storageColsSpinner.getValue(), storageRowsSpinner.getValue()});
        }

        return a.isEmpty() ? null : a;
    }

    public void clear() {
        enableCheck.setSelected(false);
        linkDropdown.setValue("");
        soundField.setText("");
        AnimationPairBox.clear(animationsBox);
        SoundPairBox.clear(soundsBox);
        storageCheck.setSelected(false);
        storageColsSpinner.getValueFactory().setValue(3);
        storageRowsSpinner.getValueFactory().setValue(3);
    }

    private static Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #b9bbbe; -fx-font-size: 10px;");
        return l;
    }
}