package com.momo.decogen;

import com.momo.decogen.model.DecoEntry;
import com.momo.decogen.ui.AppController;
import com.momo.decogen.ui.DropZonePanel;
import com.momo.decogen.ui.EditorPanel;
import com.momo.decogen.ui.EntryListPanel;
import com.momo.decogen.ui.JsonPreviewPanel;
import com.momo.decogen.ui.TopBar;
import com.momo.decogen.update.UpdateChecker;
import javafx.application.Application;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.File;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        AppController controller = new AppController();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #36393f;");

        TopBar topBar = new TopBar(controller, stage);
        root.setTop(topBar.getRoot());

        DropZonePanel dropZone = new DropZonePanel(controller);
        root.setLeft(dropZone.getRoot());

        EditorPanel editorPanel = new EditorPanel(controller);
        controller.setEditorPanel(editorPanel);
        root.setRight(editorPanel.getRoot());

        EntryListPanel entryList = new EntryListPanel(controller);
        JsonPreviewPanel jsonPanel = new JsonPreviewPanel();
        controller.setJsonPreview(jsonPanel.getJsonPreview());

        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.getItems().addAll(entryList.getRoot(), jsonPanel.getRoot());
        splitPane.setDividerPositions(0.65);
        splitPane.setStyle("-fx-background-color: #36393f;");
        root.setCenter(splitPane);

        // Keep JSON preview in sync with entry list
        controller.getEntries().addListener((ListChangeListener<DecoEntry>) c -> controller.updateJsonPreview());

        Scene scene = new Scene(root, 1200, 800);

        // Global undo/redo shortcuts. Using addEventHandler (not an accelerator)
        // so focused TextField/TextArea controls still get their native Ctrl+Z
        // for typing undo — they consume the event before it bubbles up here.
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (!event.isControlDown() || event.isAltDown()) return;
            KeyCode code = event.getCode();
            if (code == KeyCode.Z && !event.isShiftDown()) {
                controller.undo();
                event.consume();
            } else if (code == KeyCode.Y
                    || (code == KeyCode.Z && event.isShiftDown())) {
                controller.redo();
                event.consume();
            }
        });

        stage.setTitle("Decocraft JSON Generator v" + UpdateChecker.getCurrentVersion());
        stage.setScene(scene);

        try {
            File iconFile = new File("src/main/java/com/momo/resources/generator_icon.png");
            if (iconFile.exists()) {
                stage.getIcons().add(new javafx.scene.image.Image(iconFile.toURI().toString()));
            } else {
                System.out.println("App icon not found at: " + iconFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("Could not load app icon: " + e.getMessage());
        }

        stage.show();

        UpdateChecker.checkForUpdatesAsync();
    }

    public static void main(String[] args) {
        launch(args);
    }
}