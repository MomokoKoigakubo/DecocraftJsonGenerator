package com.momo.decogen;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModelParser {
    private static final Gson gson = new Gson();

    public static Model parse(Path bbModelFile) throws IOException {
        String json = Files.readString(bbModelFile);
        return gson.fromJson(json, Model.class);
    }
}