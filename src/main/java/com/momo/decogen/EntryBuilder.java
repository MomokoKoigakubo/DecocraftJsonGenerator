package com.momo.decogen;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class EntryBuilder {

    /**
     * Build entries from models, auto-matching textures where possible.
     * Entries without matches will have material = null (user assigns later).
     */
    public static List<DecoEntry> buildEntries(List<Path> modelFiles, List<Path> textureFiles, Path modelsRoot) throws IOException {
        List<DecoEntry> entries = new ArrayList<>();

        List<String> textureStems = textureFiles.stream()
                .map(DirectoryScanner::getStem)
                .toList();

        for (Path modelFile : modelFiles) {
            String modelName = DirectoryScanner.getStem(modelFile);
            String tab = DirectoryScanner.getTabFromPath(modelFile, modelsRoot);

            Model model = ModelParser.parse(modelFile);

            // Find textures that start with model name
            List<String> matches = textureStems.stream()
                    .filter(t -> t.toLowerCase().startsWith(modelName.toLowerCase()))
                    .toList();

            if (matches.isEmpty()) {
                // No match - create entry with null material (user assigns later)
                DecoEntry entry = new DecoEntry();
                entry.setName(toDisplayName(modelName));
                entry.setModel(modelName);
                entry.setMaterial(null);  // User must assign
                entry.setTabs(tab);
                entry.autoDetectType(model);
                entries.add(entry);
            } else {
                // Create entry for each matching texture
                for (String texture : matches) {
                    DecoEntry entry = new DecoEntry();
                    entry.setName(toDisplayName(texture));
                    entry.setModel(modelName);
                    entry.setMaterial(texture);
                    entry.setTabs(tab);
                    entry.autoDetectType(model);
                    entries.add(entry);
                }
            }
        }

        return entries;
    }

    /**
     * Check if entry needs user attention (missing material)
     */
    public static boolean needsAttention(DecoEntry entry) {
        return entry.getMaterial() == null;
    }

    /**
     * Get all entries that need material assigned
     */
    public static List<DecoEntry> getUnmatched(List<DecoEntry> entries) {
        return entries.stream()
                .filter(EntryBuilder::needsAttention)
                .toList();
    }

    /**
     * Convert snake_case to Title Case
     */
    public static String toDisplayName(String snakeName) {
        String[] parts = snakeName.split("_");
        StringBuilder sb = new StringBuilder();

        for (String part : parts) {
            if (!part.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(part.substring(0, 1).toUpperCase());
                sb.append(part.substring(1).toLowerCase());
            }
        }

        return sb.toString();
    }
}