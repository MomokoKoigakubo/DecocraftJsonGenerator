package com.momo.decogen.ui;

import com.momo.decogen.model.DecoEntry;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EntryListPanel {

    private final VBox root;
    private final ListView<DecoEntry> listView;

    public EntryListPanel(AppController controller) {
        root = new VBox(10);
        root.setPadding(new Insets(10));

        Label title = new Label("Entries");
        title.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 16px; -fx-font-weight: bold;");

        listView = new ListView<>(controller.getEntries());
        listView.setStyle("-fx-background-color: #2f3136; -fx-control-inner-background: #2f3136;");
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        VBox.setVgrow(listView, Priority.ALWAYS);

        controller.setEntryListView(listView);

        listView.setCellFactory(lv -> {
            ListCell<DecoEntry> cell = new ListCell<>() {
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

                        boolean isIconEntry = entry.getDecoref() != null;
                        boolean isComplete = entry.getMaterial() != null;

                        String statusIcon = isComplete ? "\u2713" : "\u26a0";
                        String statusColor = isComplete ? "#3ba55c" : "#faa61a";

                        Label status = new Label(statusIcon);
                        status.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 14px;");

                        Label name = new Label(entry.getName());
                        name.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 13px;");

                        String detailText;
                        if (isIconEntry) {
                            boolean hasRealIcon = controller.getIconFiles().containsKey(entry.getDecoref());
                            String iconIndicator = hasRealIcon ? "\uD83D\uDDBC" : "\u26a0";
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

                        setStyle(baseCellStyle(isSelected()));
                    }
                }
            };

            // Internal drag-and-drop for reordering. File drops are still
            // handled at the ListView level (below) — we only intercept our
            // own "row move" payload here.
            final String ROW_PAYLOAD = "__decogen_row:";

            cell.setOnDragDetected(event -> {
                if (cell.getItem() == null) return;
                Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(ROW_PAYLOAD + cell.getIndex());
                db.setContent(content);
                event.consume();
            });

            cell.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasString() && db.getString().startsWith(ROW_PAYLOAD)
                        && event.getGestureSource() != cell) {
                    event.acceptTransferModes(TransferMode.MOVE);
                    // Only draw the drop-indicator on real rows — painting a
                    // background on empty cells would leave phantom "gray
                    // entries" past the last row after the drag leaves.
                    if (!cell.isEmpty()) {
                        boolean above = event.getY() < cell.getHeight() / 2.0;
                        String border = above
                                ? " -fx-border-color: #5865F2; -fx-border-width: 2 0 0 0;"
                                : " -fx-border-color: #5865F2; -fx-border-width: 0 0 2 0;";
                        cell.setStyle(baseCellStyle(cell.isSelected()) + border);
                        if (!cell.getStyleClass().contains("drop-target")) {
                            cell.getStyleClass().add("drop-target");
                        }
                    }
                    event.consume();
                }
            });

            cell.setOnDragExited(event -> {
                if (cell.getStyleClass().remove("drop-target")) {
                    // Empty cells must return to transparent; filled cells get
                    // their normal row background back.
                    if (cell.isEmpty()) {
                        cell.setStyle("-fx-background-color: transparent;");
                    } else {
                        cell.setStyle(baseCellStyle(cell.isSelected()));
                    }
                }
            });

            cell.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasString() && db.getString().startsWith(ROW_PAYLOAD)) {
                    int sourceIdx;
                    try {
                        sourceIdx = Integer.parseInt(db.getString().substring(ROW_PAYLOAD.length()));
                    } catch (NumberFormatException ex) {
                        sourceIdx = -1;
                    }
                    int size = controller.getEntries().size();
                    // Insertion index in the PRE-REMOVAL list: top half -> at
                    // target index (above target); bottom half -> target+1
                    // (below target); empty cell -> end of list.
                    int insertAtPreRemove;
                    if (cell.isEmpty()) {
                        insertAtPreRemove = size;
                    } else {
                        int tgt = cell.getIndex();
                        boolean above = event.getY() < cell.getHeight() / 2.0;
                        insertAtPreRemove = above ? tgt : tgt + 1;
                    }
                    if (sourceIdx >= 0 && sourceIdx < size) {
                        int insertAt = insertAtPreRemove;
                        // Shift down by one if we remove a row that sat before
                        // the target slot — indices after sourceIdx slide up.
                        if (sourceIdx < insertAt) insertAt -= 1;
                        if (insertAt != sourceIdx) {
                            controller.snapshot();
                            DecoEntry moved = controller.getEntries().remove(sourceIdx);
                            int maxIdx = controller.getEntries().size();
                            if (insertAt < 0) insertAt = 0;
                            if (insertAt > maxIdx) insertAt = maxIdx;
                            controller.getEntries().add(insertAt, moved);
                            controller.updateJsonPreview();
                            listView.getSelectionModel().clearAndSelect(insertAt);
                            success = true;
                        }
                    }
                    event.setDropCompleted(success);
                    event.consume();
                }
            });

            return cell;
        });

        listView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<DecoEntry>) c -> {
            EditorPanel editor = controller.getEditorPanel();
            if (editor == null) return;

            int count = listView.getSelectionModel().getSelectedItems().size();
            if (count > 0) {
                if (count == 1) {
                    editor.setTitleText("Entry Editor");
                    editor.loadEntry(listView.getSelectionModel().getSelectedItems().get(0));
                } else {
                    editor.setTitleText("Editing " + count + " entries");
                    editor.clearFields();
                }
                editor.setShown(true);
            } else {
                editor.setShown(false);
            }
        });

        listView.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        listView.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            DecoEntry target = listView.getSelectionModel().getSelectedItem();
            if (db.hasFiles() && target != null) {
                for (File file : db.getFiles()) {
                    if (file.getName().toLowerCase().endsWith(".png")) {
                        controller.assignTextureToEntry(target, file);
                        break;
                    }
                }
            }
            event.setDropCompleted(true);
            event.consume();
        });

        listView.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("DELETE")) {
                List<DecoEntry> selected = new ArrayList<>(listView.getSelectionModel().getSelectedItems());
                if (!selected.isEmpty()) {
                    controller.snapshot();
                    controller.getEntries().removeAll(selected);
                }
            }
        });

        root.getChildren().addAll(title, listView);
    }

    public VBox getRoot() {
        return root;
    }

    public ListView<DecoEntry> getListView() {
        return listView;
    }

    private static String baseCellStyle(boolean selected) {
        return selected
                ? "-fx-background-color: #5865F2; -fx-background-radius: 4; -fx-padding: 6;"
                : "-fx-background-color: #40444b; -fx-background-radius: 4; -fx-padding: 6;";
    }
}