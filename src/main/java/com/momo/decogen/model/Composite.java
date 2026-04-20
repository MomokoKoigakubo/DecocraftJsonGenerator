package com.momo.decogen.model;

/**
 * A child model rendered on top of the parent (e.g. a disc on a jukebox).
 * Nested under {@link DecoEntry#composite}.
 */
public class Composite {
    private String model;
    private String texture;
    private Boolean transparency;

    public Composite() {}

    public Composite(String model) {
        this.model = model;
    }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getTexture() { return texture; }
    public void setTexture(String texture) { this.texture = texture; }

    public Boolean getTransparency() { return transparency; }
    public void setTransparency(Boolean transparency) { this.transparency = transparency; }
}