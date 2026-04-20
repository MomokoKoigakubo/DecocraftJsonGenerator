package com.momo.decogen.model;

public enum DecoType {
    SEAT("seat"),
    BED("bed");

    private final String jsonValue;

    DecoType(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    public String getJsonValue() {
        return jsonValue;
    }
}