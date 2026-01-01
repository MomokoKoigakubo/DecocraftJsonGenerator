package com.momo.decogen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class DirectoryScanner {

    /**
     * Find all .bbmodel files in a directory (recursive)
     */
    public static List<Path> findModels(Path modelsDir) throws IOException {
        if (!Files.exists(modelsDir)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.walk(modelsDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".bbmodel"))
                    .toList();
        }
    }

    /**
     * Find all .png files in a directory (non-recursive, top level only)
     */
    public static List<Path> findTextures(Path texturesDir) throws IOException {
        if (!Files.exists(texturesDir)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(texturesDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".png"))
                    .toList();
        }
    }

    /**
     * Get the tab name from a model's path (subfolder under models/)
     * e.g., models/kitchen/toaster.bbmodel → "kitchen"
     */
    public static String getTabFromPath(Path modelFile, Path modelsRoot) {
        try {
            Path relative = modelsRoot.relativize(modelFile);
            if (relative.getNameCount() >= 2) {
                return relative.getName(0).toString();
            }
        } catch (Exception e) {
            // Fall through to default
        }
        return "misc";
    }

    /**
     * Get file stem (filename without extension)
     */
    public static String getStem(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}