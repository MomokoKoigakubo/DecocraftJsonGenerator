package com.momo.decogen.update;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Self-update helpers: locate the jpackage install root, download a release
 * asset, extract it, and write a relaunch script that swaps the install dir
 * after the running app exits.
 */
public final class Updater {

    public interface ProgressListener {
        void onProgress(long bytesRead, long contentLength);
    }

    private Updater() {}

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public static boolean isLinux() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");
    }

    /**
     * Returns the jpackage install root, or null if not running from a jpackage
     * bundle (e.g. running from gradle/IDE).
     *
     * Windows layout: <root>/DecocraftJsonGenerator.exe — root is parent of exe.
     * Linux layout:   <root>/bin/DecocraftJsonGenerator — root is parent of bin.
     */
    public static Path getInstallRoot() {
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath == null || appPath.isEmpty()) return null;
        Path launcher = Paths.get(appPath);
        Path parent = launcher.getParent();
        if (parent == null) return null;
        if (isWindows()) {
            return parent;
        }
        // Linux: launcher is in <root>/bin/
        if (parent.getFileName() != null && "bin".equals(parent.getFileName().toString())) {
            return parent.getParent();
        }
        return parent;
    }

    public static boolean canSelfUpdate() {
        Path root = getInstallRoot();
        return root != null && Files.isWritable(root);
    }

    public static Path downloadAsset(String url, ProgressListener listener) throws IOException {
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestProperty("Accept", "application/octet-stream");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(30_000);
        conn.setInstanceFollowRedirects(true);

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IOException("Download failed: HTTP " + code);
        }

        long contentLength = conn.getContentLengthLong();
        String name = url.substring(url.lastIndexOf('/') + 1);
        Path tmp = Files.createTempFile("decogen-update-", "-" + name);

        try (InputStream in = conn.getInputStream();
             OutputStream out = new BufferedOutputStream(Files.newOutputStream(tmp))) {
            byte[] buf = new byte[64 * 1024];
            long total = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                total += n;
                if (listener != null) listener.onProgress(total, contentLength);
            }
        }
        return tmp;
    }

    public static Path extractArchive(Path archive) throws IOException {
        Path dest = Files.createTempDirectory("decogen-update-extract-");
        String name = archive.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".zip")) {
            extractZip(archive, dest);
        } else if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
            extractTarGz(archive, dest);
        } else {
            throw new IOException("Unsupported archive: " + name);
        }
        return dest;
    }

    private static void extractZip(Path zip, Path dest) throws IOException {
        try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                Path out = dest.resolve(entry.getName()).normalize();
                if (!out.startsWith(dest)) throw new IOException("Bad zip entry: " + entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(zin, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void extractTarGz(Path tarGz, Path dest) throws IOException {
        // Use system tar (always present on Linux) to avoid adding a dep.
        ProcessBuilder pb = new ProcessBuilder("tar", "-xzf", tarGz.toAbsolutePath().toString(),
                "-C", dest.toAbsolutePath().toString());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try {
            int code = p.waitFor();
            if (code != 0) throw new IOException("tar exited " + code);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("tar interrupted", e);
        }
    }

    /**
     * Finds the directory inside the extracted archive that contains the
     * actual app payload (e.g. .../DecocraftJsonGenerator/). The archives
     * wrap their content in a single top-level folder.
     */
    public static Path findPayloadRoot(Path extracted) throws IOException {
        try (var stream = Files.list(extracted)) {
            return stream.filter(Files::isDirectory).findFirst()
                    .orElseThrow(() -> new IOException("Empty archive"));
        }
    }

    /**
     * Writes a relaunch script that waits for the given PID to exit, swaps
     * payload contents into installRoot, then starts the new launcher. Runs
     * the script detached. Caller should immediately exit the JVM.
     */
    public static void launchRelaunchScript(long pid, Path payloadRoot, Path installRoot) throws IOException {
        if (isWindows()) {
            launchWindows(pid, payloadRoot, installRoot);
        } else {
            launchUnix(pid, payloadRoot, installRoot);
        }
    }

    private static void launchWindows(long pid, Path payloadRoot, Path installRoot) throws IOException {
        Path script = Files.createTempFile("decogen-update-", ".bat");
        Path log = Files.createTempFile("decogen-update-", ".log");
        Path launcher = installRoot.resolve("DecocraftJsonGenerator.exe");

        String content = "@echo off\r\n"
                + "setlocal\r\n"
                + ":wait\r\n"
                + "tasklist /FI \"PID eq " + pid + "\" 2>NUL | find \"" + pid + "\" >NUL\r\n"
                + "if not errorlevel 1 (timeout /t 1 /nobreak >NUL & goto wait)\r\n"
                + "xcopy /E /Y /I /Q \"" + payloadRoot + "\\*\" \"" + installRoot + "\\\" >> \"" + log + "\" 2>&1\r\n"
                + "if errorlevel 1 (echo xcopy failed >> \"" + log + "\" & exit /b 1)\r\n"
                + "start \"\" \"" + launcher + "\"\r\n"
                + "rmdir /S /Q \"" + payloadRoot.getParent() + "\" 2>NUL\r\n";
        Files.writeString(script, content);

        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", "\"updater\"", "/MIN",
                script.toAbsolutePath().toString());
        pb.redirectErrorStream(true);
        pb.redirectOutput(log.toFile());
        pb.start();
    }

    private static void launchUnix(long pid, Path payloadRoot, Path installRoot) throws IOException {
        Path script = Files.createTempFile("decogen-update-", ".sh");
        Path log = Files.createTempFile("decogen-update-", ".log");
        Path launcher = installRoot.resolve("bin").resolve("DecocraftJsonGenerator");

        String content = "#!/bin/sh\n"
                + "exec >> \"" + log + "\" 2>&1\n"
                + "PID=" + pid + "\n"
                + "while kill -0 \"$PID\" 2>/dev/null; do sleep 1; done\n"
                + "cp -a \"" + payloadRoot + "/.\" \"" + installRoot + "/\" || exit 1\n"
                + "chmod +x \"" + launcher + "\" 2>/dev/null\n"
                + "rm -rf \"" + payloadRoot.getParent() + "\"\n"
                + "nohup \"" + launcher + "\" >/dev/null 2>&1 &\n";
        Files.writeString(script, content);
        script.toFile().setExecutable(true, true);

        ProcessBuilder pb = new ProcessBuilder("/bin/sh", script.toAbsolutePath().toString());
        pb.redirectErrorStream(true);
        pb.redirectOutput(log.toFile());
        pb.start();
    }

    public static long currentPid() {
        return ProcessHandle.current().pid();
    }
}
