package com.momo.decogen.bbmodel;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BBModelParser {
    private static final Gson gson = new Gson();

    public static BBModel parse(Path bbModelFile) throws IOException {
        String json = Files.readString(bbModelFile);
        return gson.fromJson(json, BBModel.class);
    }
}