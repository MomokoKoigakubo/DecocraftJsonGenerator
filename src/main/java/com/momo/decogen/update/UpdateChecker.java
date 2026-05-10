package com.momo.decogen.update;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks GitHub releases for updates and (on jpackage builds) performs a
 * guided self-update: downloads the OS-specific asset, extracts it, and
 * relaunches the app via a small swap script. Falls back to opening the
 * releases page when self-update isn't possible.
 */
public class UpdateChecker {

    private static final String GITHUB_OWNER = "MomokoKoigakubo";
    private static final String GITHUB_REPO = "DecocraftJsonGenerator";
    private static final String RELEASES_API = "https://api.github.com/repos/%s/%s/releases/latest";
    private static final String RELEASES_PAGE = "https://github.com/%s/%s/releases/latest";

    private static final String CURRENT_VERSION = "1.2.7";

    public static void checkForUpdatesAsync() {
        checkForUpdatesAsync(null);
    }

    public static void checkForUpdatesAsync(Stage owner) {
        Thread updateThread = new Thread(() -> {
            try {
                String body = fetchLatestReleaseJson();
                if (body == null) return;
                String latestVersion = parseTagName(body);
                if (latestVersion == null || !isNewerVersion(latestVersion, CURRENT_VERSION)) return;
                Map<String, String> assets = parseAssets(body);
                Platform.runLater(() -> showUpdateDialog(latestVersion, assets, owner));
            } catch (Exception e) {
                System.out.println("Update check failed: " + e.getMessage());
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }

    private static String fetchLatestReleaseJson() throws Exception {
        String apiUrl = String.format(RELEASES_API, GITHUB_OWNER, GITHUB_REPO);
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        if (conn.getResponseCode() != 200) return null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private static String parseTagName(String json) {
        Matcher m = Pattern.compile("\"tag_name\"\\s*:\\s*\"v?([^\"]+)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static Map<String, String> parseAssets(String json) {
        Map<String, String> assets = new LinkedHashMap<>();
        Matcher m = Pattern.compile(
                "\"name\"\\s*:\\s*\"([^\"]+)\"[\\s\\S]*?\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(json);
        while (m.find()) {
            assets.put(m.group(1), m.group(2));
        }
        return assets;
    }

    private static String pickAssetForOs(Map<String, String> assets) {
        boolean win = Updater.isWindows();
        boolean linux = Updater.isLinux();
        for (Map.Entry<String, String> e : assets.entrySet()) {
            String n = e.getKey().toLowerCase();
            if (win && n.endsWith(".zip") && n.contains("windows")) return e.getValue();
            if (linux && (n.endsWith(".tar.gz") || n.endsWith(".tgz")) && n.contains("linux")) return e.getValue();
        }
        return null;
    }

    private static boolean isNewerVersion(String latest, String current) {
        String[] latestParts = latest.split("\\.");
        String[] currentParts = current.split("\\.");

        int length = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int latestPart = i < latestParts.length ? parseVersionPart(latestParts[i]) : 0;
            int currentPart = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;

            if (latestPart > currentPart) return true;
            else if (latestPart < currentPart) return false;
        }
        return false;
    }

    private static int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void showUpdateDialog(String newVersion, Map<String, String> assets, Stage owner) {
        String assetUrl = pickAssetForOs(assets);
        boolean canSelfUpdate = assetUrl != null && Updater.canSelfUpdate();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Update Available");
        alert.setHeaderText("A new version is available!");
        alert.setContentText(String.format(
                "Current version: %s%nNew version: %s%n%n%s",
                CURRENT_VERSION, newVersion,
                canSelfUpdate
                        ? "Install the update now? The app will close and relaunch."
                        : "Open the releases page to download manually?"
        ));
        if (owner != null) {
            alert.initOwner(owner);
            alert.initModality(Modality.WINDOW_MODAL);
        }

        ButtonType updateNowBtn = new ButtonType("Update Now");
        ButtonType openPageBtn = new ButtonType("Open Page");
        ButtonType laterBtn = new ButtonType("Later");
        if (canSelfUpdate) {
            alert.getButtonTypes().setAll(updateNowBtn, openPageBtn, laterBtn);
        } else {
            alert.getButtonTypes().setAll(openPageBtn, laterBtn);
        }

        boolean wasMaximized = owner != null && owner.isMaximized();
        boolean wasFullScreen = owner != null && owner.isFullScreen();
        Optional<ButtonType> result = alert.showAndWait();
        if (owner != null && (wasMaximized || wasFullScreen)) {
            Platform.runLater(() -> {
                if (wasFullScreen && !owner.isFullScreen()) owner.setFullScreen(true);
                if (wasMaximized && !owner.isMaximized()) owner.setMaximized(true);
            });
        }
        if (result.isEmpty()) return;
        ButtonType chosen = result.get();
        if (chosen == updateNowBtn && canSelfUpdate) {
            runSelfUpdate(assetUrl, newVersion, owner);
        } else if (chosen == openPageBtn) {
            openReleasesPage();
        }
    }

    private static void runSelfUpdate(String assetUrl, String newVersion, Stage owner) {
        Stage progressStage = new Stage();
        progressStage.setTitle("Updating to v" + newVersion);
        if (owner != null) {
            progressStage.initOwner(owner);
            progressStage.initModality(Modality.WINDOW_MODAL);
        }
        Label status = new Label("Downloading…");
        ProgressBar bar = new ProgressBar(0);
        bar.setPrefWidth(360);
        SimpleDoubleProperty progress = new SimpleDoubleProperty(0);
        bar.progressProperty().bind(progress);
        Button cancel = new Button("Cancel");
        VBox box = new VBox(10, status, bar, cancel);
        box.setPadding(new Insets(16));
        progressStage.setScene(new Scene(box));
        progressStage.setResizable(false);

        final boolean[] cancelled = {false};
        cancel.setOnAction(e -> {
            cancelled[0] = true;
            progressStage.close();
        });

        Thread t = new Thread(() -> {
            try {
                Path archive = Updater.downloadAsset(assetUrl, (read, total) -> {
                    if (cancelled[0]) throw new RuntimeException("cancelled");
                    if (total > 0) {
                        double p = (double) read / (double) total;
                        Platform.runLater(() -> progress.set(p));
                    }
                });
                if (cancelled[0]) return;
                Platform.runLater(() -> {
                    status.setText("Extracting…");
                    progress.unbind();
                    bar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                });
                Path extracted = Updater.extractArchive(archive);
                Path payload = Updater.findPayloadRoot(extracted);
                Path installRoot = Updater.getInstallRoot();
                long pid = Updater.currentPid();
                Updater.launchRelaunchScript(pid, payload, installRoot);
                Platform.runLater(() -> {
                    progressStage.close();
                    Platform.exit();
                    System.exit(0);
                });
            } catch (Exception ex) {
                if (cancelled[0]) return;
                System.out.println("Self-update failed: " + ex.getMessage());
                Platform.runLater(() -> {
                    progressStage.close();
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    if (owner != null) {
                        err.initOwner(owner);
                        err.initModality(Modality.WINDOW_MODAL);
                    }
                    err.setTitle("Update Failed");
                    err.setHeaderText("Could not install the update");
                    err.setContentText(ex.getMessage() + "\n\nOpen the releases page to download manually?");
                    ButtonType openBtn = new ButtonType("Open Page");
                    ButtonType closeBtn = new ButtonType("Close", ButtonType.CANCEL.getButtonData());
                    err.getButtonTypes().setAll(openBtn, closeBtn);
                    Optional<ButtonType> r = err.showAndWait();
                    if (r.isPresent() && r.get() == openBtn) openReleasesPage();
                });
            }
        }, "decogen-self-updater");
        t.setDaemon(true);

        progressStage.show();
        t.start();
    }

    private static void openReleasesPage() {
        try {
            String releasesUrl = String.format(RELEASES_PAGE, GITHUB_OWNER, GITHUB_REPO);
            java.awt.Desktop.getDesktop().browse(new java.net.URI(releasesUrl));
        } catch (Exception e) {
            System.out.println("Could not open browser: " + e.getMessage());
        }
    }

    public static String getCurrentVersion() {
        return CURRENT_VERSION;
    }
}
