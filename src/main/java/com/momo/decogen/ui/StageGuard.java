package com.momo.decogen.ui;

import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.function.Supplier;

/**
 * Keeps an owner {@link Stage} from being resized by the window manager while a
 * modal dialog or native file chooser is open.
 *
 * On Linux, JavaFX runs on GTK and a dialog's layout pass sends fresh geometry
 * hints to the window manager (GNOME/mutter, KWin, XFCE, …); many WMs honour
 * them and resize the owner the moment the dialog appears — a maximized window
 * drops out of that state, a plain windowed one shrinks toward its preferred
 * size. Windows and macOS don't do this.
 *
 * Guard in two parts: (1) before the popup, pin the stage's minimum size to its
 * current size so the WM can't shrink it while the modal is up; (2) after the
 * popup returns, restore maximized/fullscreen state or the exact windowed
 * bounds and relax the minimum back. Off Linux the size never changes, so the
 * restores are no-ops.
 */
public final class StageGuard {

    private StageGuard() {}

    public static <T> T runPreserving(Stage stage, Supplier<T> popup) {
        if (stage == null) return popup.get();

        boolean wasMaximized = stage.isMaximized();
        boolean wasFullScreen = stage.isFullScreen();
        double w = stage.getWidth();
        double h = stage.getHeight();
        double x = stage.getX();
        double y = stage.getY();
        double savedMinW = stage.getMinWidth();
        double savedMinH = stage.getMinHeight();

        boolean windowed = !wasMaximized && !wasFullScreen
                && w > 0 && h > 0 && !Double.isNaN(w) && !Double.isNaN(h);
        if (windowed) {
            stage.setMinWidth(w);
            stage.setMinHeight(h);
        }

        try {
            return popup.get();
        } finally {
            Platform.runLater(() -> {
                if (wasFullScreen && !stage.isFullScreen()) stage.setFullScreen(true);
                if (wasMaximized && !stage.isMaximized()) stage.setMaximized(true);
                if (windowed) {
                    if (stage.getWidth() != w) stage.setWidth(w);
                    if (stage.getHeight() != h) stage.setHeight(h);
                    if (stage.getX() != x) stage.setX(x);
                    if (stage.getY() != y) stage.setY(y);
                    stage.setMinWidth(savedMinW);
                    stage.setMinHeight(savedMinH);
                }
            });
        }
    }

    public static void runPreserving(Stage stage, Runnable popup) {
        runPreserving(stage, () -> {
            popup.run();
            return null;
        });
    }
}
