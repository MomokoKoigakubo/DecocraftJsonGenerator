package com.momo.decogen.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * A single-window full-spectrum colour picker: a rainbow hue bar spanning all
 * 360° plus a saturation/brightness field for the chosen hue, with a live hex
 * field. Built from real JavaFX gradient nodes (not CSS {@code hsb()}), so the
 * field always shows the full colour range rather than a single tone.
 */
public final class ColorChooser {

    private static final double W = 240;   // field width
    private static final double H = 190;   // field height
    private static final double HUE_H = 20; // hue bar height

    private ColorChooser() {}

    public static Optional<Color> show(Stage owner, Color initial) {
        Color start = initial != null ? initial : Color.WHITE;
        // [hue 0-360, saturation 0-1, brightness 0-1]
        final double[] hsb = {start.getHue(), start.getSaturation(), start.getBrightness()};

        // Saturation/brightness field: pure-hue base + white→transparent (left→
        // right) + transparent→black (top→bottom) overlays.
        Rectangle base = new Rectangle(W, H);
        Rectangle satOverlay = new Rectangle(W, H, new LinearGradient(0, 0, 1, 0, true,
                CycleMethod.NO_CYCLE, new Stop(0, Color.WHITE), new Stop(1, Color.TRANSPARENT)));
        Rectangle valOverlay = new Rectangle(W, H, new LinearGradient(0, 0, 0, 1, true,
                CycleMethod.NO_CYCLE, new Stop(0, Color.TRANSPARENT), new Stop(1, Color.BLACK)));
        Circle thumb = new Circle(6, Color.TRANSPARENT);
        thumb.setStroke(Color.WHITE);
        thumb.setStrokeWidth(2);
        thumb.setMouseTransparent(true);
        Pane field = new Pane(base, satOverlay, valOverlay, thumb);
        field.setMinSize(W, H);
        field.setPrefSize(W, H);
        field.setMaxSize(W, H);

        // Full-spectrum hue bar.
        Rectangle hueBar = new Rectangle(W, HUE_H, hueGradient());
        Rectangle hueThumb = new Rectangle(3, HUE_H + 4, Color.WHITE);
        hueThumb.setStroke(Color.rgb(0, 0, 0, 0.6));
        hueThumb.setMouseTransparent(true);
        hueThumb.setY(-2);
        Pane huePane = new Pane(hueBar, hueThumb);
        huePane.setMinSize(W, HUE_H);
        huePane.setPrefSize(W, HUE_H);
        huePane.setMaxSize(W, HUE_H);

        Rectangle preview = new Rectangle(52, 26);
        preview.setStroke(Color.web("#888"));
        TextField hexField = new TextField();
        hexField.setPrefColumnCount(8);

        // Guard so programmatic setText() from render() doesn't re-parse itself.
        final boolean[] syncing = {false};

        Runnable render = () -> {
            base.setFill(Color.hsb(hsb[0], 1, 1));
            thumb.setCenterX(hsb[1] * W);
            thumb.setCenterY((1 - hsb[2]) * H);
            hueThumb.setX(hsb[0] / 360.0 * W - 1.5);
            Color c = Color.hsb(hsb[0], hsb[1], hsb[2]);
            preview.setFill(c);
            if (!hexField.isFocused()) {
                syncing[0] = true;
                try {
                    hexField.setText(toHex(c));
                    hexField.setStyle("");
                } finally {
                    syncing[0] = false;
                }
            }
        };

        // Apply whatever is in the hex field to the picker. Returns true if it
        // parsed. Accepts "#RRGGBB", "RRGGBB", "#RGB", "RGB" and "0xRRGGBB".
        java.util.function.BooleanSupplier applyHex = () -> {
            Color c = parseHex(hexField.getText());
            if (c == null) {
                hexField.setStyle("-fx-border-color: #e06c75; -fx-border-width: 1.5;");
                return false;
            }
            hexField.setStyle("");
            hsb[0] = c.getHue();
            hsb[1] = c.getSaturation();
            hsb[2] = c.getBrightness();
            render.run();
            return true;
        };

        field.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> pickField(e, hsb, render));
        field.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> pickField(e, hsb, render));
        huePane.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> pickHue(e, hsb, render));
        huePane.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> pickHue(e, hsb, render));

        // Live: update the picker as the user types (no Enter needed). This is
        // what makes "type a code, click Select" work — previously the typed
        // value was only read on Enter, so clicking Select returned the old
        // colour. Also commit on Enter and on focus loss.
        hexField.textProperty().addListener((obs, o, n) -> {
            if (syncing[0]) return;
            if (parseHex(n) != null) applyHex.getAsBoolean();
            else hexField.setStyle(n == null || n.trim().isEmpty() ? ""
                    : "-fx-border-color: #e06c75; -fx-border-width: 1.5;");
        });
        hexField.setOnAction(e -> {
            if (!applyHex.getAsBoolean()) render.run(); // invalid: revert to current colour
            e.consume();
        });
        hexField.focusedProperty().addListener((obs, was, is) -> {
            if (was && !is && !applyHex.getAsBoolean()) render.run();
        });

        HBox previewRow = new HBox(8, preview, hexField);
        previewRow.setAlignment(Pos.CENTER_LEFT);
        VBox content = new VBox(10, field, huePane, previewRow);
        content.setPadding(new Insets(12));

        Dialog<ButtonType> dialog = new Dialog<>();
        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        }
        dialog.setTitle("Pick a colour");
        dialog.getDialogPane().setContent(content);
        ButtonType selectBtn = new ButtonType("Select", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectBtn, ButtonType.CANCEL);

        render.run();

        Optional<ButtonType> result = StageGuard.runPreserving(owner, dialog::showAndWait);
        if (result.isPresent() && result.get() == selectBtn) {
            // The hex field is the final authority if it holds a valid code:
            // covers "typed a code, clicked Select" even if no focus-lost event
            // fired before the dialog closed (seen on some Linux window managers).
            Color typed = parseHex(hexField.getText());
            if (typed != null) return Optional.of(typed);
            return Optional.of(Color.hsb(hsb[0], hsb[1], hsb[2]));
        }
        return Optional.empty();
    }

    /**
     * Lenient hex parser: "#RRGGBB", "RRGGBB", "#RGB", "RGB", "0xRRGGBB", any
     * case, surrounding whitespace ignored. Returns null if it isn't one of those.
     */
    static Color parseHex(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (t.startsWith("#")) t = t.substring(1);
        else if (t.startsWith("0x") || t.startsWith("0X")) t = t.substring(2);
        if (t.length() == 3) {
            t = "" + t.charAt(0) + t.charAt(0) + t.charAt(1) + t.charAt(1) + t.charAt(2) + t.charAt(2);
        }
        if (t.length() != 6) return null;
        for (int i = 0; i < 6; i++) {
            if (Character.digit(t.charAt(i), 16) < 0) return null;
        }
        int rgb = Integer.parseInt(t, 16);
        return Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    private static void pickField(MouseEvent e, double[] hsb, Runnable render) {
        hsb[1] = clamp(e.getX() / W);
        hsb[2] = 1 - clamp(e.getY() / H);
        render.run();
    }

    private static void pickHue(MouseEvent e, double[] hsb, Runnable render) {
        hsb[0] = clamp(e.getX() / W) * 360.0;
        render.run();
    }

    private static LinearGradient hueGradient() {
        return new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.hsb(0, 1, 1)),
                new Stop(1.0 / 6, Color.hsb(60, 1, 1)),
                new Stop(2.0 / 6, Color.hsb(120, 1, 1)),
                new Stop(3.0 / 6, Color.hsb(180, 1, 1)),
                new Stop(4.0 / 6, Color.hsb(240, 1, 1)),
                new Stop(5.0 / 6, Color.hsb(300, 1, 1)),
                new Stop(1.0, Color.hsb(360, 1, 1)));
    }

    private static double clamp(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    static String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }
}
