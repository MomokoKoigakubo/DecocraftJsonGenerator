package com.momo.decogen;

public class DecoEntry {
    private String name;
    private String decoref;
    private String model;
    private String material;
    private double scale = 1.0;
    private String tabs;
    private int[] craftingColor = {0, 0, 0};
    private String type;
    private Boolean transparency;
    private Boolean hidden;
    private String defaultAnimation;
    private String shape;
    private String loot;
    private Integer lightLevel;
    private Flipbook flipbook;
    private Script script;

    public DecoEntry(){}

    public DecoEntry(String name, String model, String material, String tabs) {
        this.name = name;
        this.model = model;
        this.material = material;
        this.tabs = tabs;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDecoref() {
        return decoref;
    }

    public void setDecoref(String decoref) {
        this.decoref = decoref;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public String getTabs() {
        return tabs;
    }

    public void setTabs(String tabs) {
        this.tabs = tabs;
    }

    public int[] getCraftingColor() {
        return craftingColor;
    }

    public void setCraftingColor(int[] craftingColor) {
        this.craftingColor = craftingColor;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getTransparency() {
        return transparency;
    }

    public void setTransparency(Boolean transparency) {
        this.transparency = transparency;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public String getDefaultAnimation() {
        return defaultAnimation;
    }

    public void setDefaultAnimation(String defaultAnimation) {
        this.defaultAnimation = defaultAnimation;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public String getLoot() {
        return loot;
    }

    public void setLoot(String loot) {
        this.loot = loot;
    }

    public Integer getLightLevel() {
        return lightLevel;
    }

    public void setLightLevel(Integer lightLevel) {
        this.lightLevel = lightLevel;
    }

    public Flipbook getFlipbook() {
        return flipbook;
    }

    public void setFlipbook(Flipbook flipbook) {
        this.flipbook = flipbook;
    }

    public Script getScript() {
        return script;
    }

    public void setScript(Script script) {
        this.script = script;
    }

    /**
     * Auto-detect and set type based on parsed model.
     * If model has animations, sets type to "animated".
     */
    public void autoDetectType(Model model) {
        if (this.type == null) {
            // Check for animations first - takes priority
            if (model.hasAnimations()) {
                this.type = "animated";
            } else {
                DecoType detected = TypeDetector.detectFromModel(model);
                if (detected != null) {
                    this.type = detected.getJsonValue();
                }
            }
        }
    }
}
