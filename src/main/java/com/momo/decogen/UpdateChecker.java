package com.momo.decogen;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.VBox;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks GitHub releases for updates and notifies the user.
 */
public class UpdateChecker {

    private static final String GITHUB_OWNER = "MomokoKoigakubo";
    private static final String GITHUB_REPO = "DecocraftJsonGenerator";
    private static final String RELEASES_API = "https://api.github.com/repos/%s/%s/releases/latest";
    private static final String RELEASES_PAGE = "https://github.com/%s/%s/releases/latest";

    private static final String CURRENT_VERSION = "1.0.0";

    /**
     * Check for updates in background thread
     */
    public static void checkForUpdatesAsync() {
        Thread updateThread = new Thread(() -> {
            try {
                String latestVersion = fetchLatestVersion();
                if (latestVersion != null && isNewerVersion(latestVersion, CURRENT_VERSION)) {
                    Platform.runLater(() -> showUpdateDialog(latestVersion));
                }
            } catch (Exception e) {
                // Silently fail - don't bother user if update check fails
                System.out.println("Update check failed: " + e.getMessage());
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }

    /**
     * Fetch the latest version tag from GitHub releases API
     */
    private static String fetchLatestVersion() throws Exception {
        String apiUrl = String.format(RELEASES_API, GITHUB_OWNER, GITHUB_REPO);
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        if (conn.getResponseCode() != 200) {
            return null;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        // Simple regex to extract tag_name from JSON response
        Pattern pattern = Pattern.compile("\"tag_name\"\\s*:\\s*\"v?([^\"]+)\"");
        Matcher matcher = pattern.matcher(response.toString());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Compare version strings (e.g., "1.0.0" vs "1.1.0")
     */
    private static boolean isNewerVersion(String latest, String current) {
        String[] latestParts = latest.split("\\.");
        String[] currentParts = current.split("\\.");

        int length = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int latestPart = i < latestParts.length ? parseVersionPart(latestParts[i]) : 0;
            int currentPart = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;

            if (latestPart > currentPart) {
                return true;
            } else if (latestPart < currentPart) {
                return false;
            }
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

    /**
     * Show update available dialog
     */
    private static void showUpdateDialog(String newVersion) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Update Available");
        alert.setHeaderText("A new version is available!");
        alert.setContentText(String.format(
            "Current version: %s\nNew version: %s\n\nWould you like to download the update?",
            CURRENT_VERSION, newVersion
        ));

        ButtonType downloadBtn = new ButtonType("Download");
        ButtonType laterBtn = new ButtonType("Later");
        alert.getButtonTypes().setAll(downloadBtn, laterBtn);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == downloadBtn) {
            openReleasesPage();
        }
    }

    /**
     * Open the GitHub releases page in the default browser
     */
    private static void openReleasesPage() {
        try {
            String releasesUrl = String.format(RELEASES_PAGE, GITHUB_OWNER, GITHUB_REPO);
            java.awt.Desktop.getDesktop().browse(new java.net.URI(releasesUrl));
        } catch (Exception e) {
            System.out.println("Could not open browser: " + e.getMessage());
        }
    }

    /**
     * Get the current version string
     */
    public static String getCurrentVersion() {
        return CURRENT_VERSION;
    }
}
