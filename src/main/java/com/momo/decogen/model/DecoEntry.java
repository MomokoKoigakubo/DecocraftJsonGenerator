package com.momo.decogen.model;

import com.momo.decogen.bbmodel.BBModel;
import com.momo.decogen.logic.ModelInspector;
import com.momo.decogen.logic.TypeDetector;

import java.util.List;

public class DecoEntry {
    // identity
    private String name;
    private String decoref;
    private String material;

    // model & rendering
    private String model;
    private double scale = 1.0;
    private String shape;
    private Boolean transparency;
    private Boolean culling;

    // tab & display
    private String tabs;
    private int[] craftingColor = {0, 0, 0};
    private String defaultAnimation;

    // block type & behavior
    private String type;
    private Boolean passable;
    private Boolean aboveWater;
    private Boolean rotatable;
    private Boolean hidden;
    private String loot;
    private Boolean displayable;

    // composite / flipbook
    private Composite composite;
    private Flipbook flipbook;

    // chain
    private List<String> chainModels;
    private List<String> chainMaterials;
    private String chainPattern;
    private Integer lighting;

    // growable
    private List<String> structures;
    private Boolean instant;

    // scripting
    private Script script;

    public DecoEntry() {}

    public DecoEntry(String name, String model, String material, String tabs) {
        this.name = name;
        this.model = model;
        this.material = material;
        this.tabs = tabs;
    }

    // --- identity ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDecoref() { return decoref; }
    public void setDecoref(String decoref) { this.decoref = decoref; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    // --- model & rendering ---
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public double getScale() { return scale; }
    public void setScale(double scale) { this.scale = scale; }

    public String getShape() { return shape; }
    public void setShape(String shape) { this.shape = shape; }

    public Boolean getTransparency() { return transparency; }
    public void setTransparency(Boolean transparency) { this.transparency = transparency; }

    public Boolean getCulling() { return culling; }
    public void setCulling(Boolean culling) { this.culling = culling; }

    // --- tab & display ---
    public String getTabs() { return tabs; }
    public void setTabs(String tabs) { this.tabs = tabs; }

    public int[] getCraftingColor() { return craftingColor; }
    public void setCraftingColor(int[] craftingColor) { this.craftingColor = craftingColor; }

    public String getDefaultAnimation() { return defaultAnimation; }
    public void setDefaultAnimation(String defaultAnimation) { this.defaultAnimation = defaultAnimation; }

    // --- block type & behavior ---
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Boolean getPassable() { return passable; }
    public void setPassable(Boolean passable) { this.passable = passable; }

    public Boolean getAboveWater() { return aboveWater; }
    public void setAboveWater(Boolean aboveWater) { this.aboveWater = aboveWater; }

    public Boolean getRotatable() { return rotatable; }
    public void setRotatable(Boolean rotatable) { this.rotatable = rotatable; }

    public Boolean getHidden() { return hidden; }
    public void setHidden(Boolean hidden) { this.hidden = hidden; }

    public String getLoot() { return loot; }
    public void setLoot(String loot) { this.loot = loot; }

    public Boolean getDisplayable() { return displayable; }
    public void setDisplayable(Boolean displayable) { this.displayable = displayable; }

    // --- composite / flipbook ---
    public Composite getComposite() { return composite; }
    public void setComposite(Composite composite) { this.composite = composite; }

    public Flipbook getFlipbook() { return flipbook; }
    public void setFlipbook(Flipbook flipbook) { this.flipbook = flipbook; }

    // --- chain ---
    public List<String> getChainModels() { return chainModels; }
    public void setChainModels(List<String> chainModels) { this.chainModels = chainModels; }

    public List<String> getChainMaterials() { return chainMaterials; }
    public void setChainMaterials(List<String> chainMaterials) { this.chainMaterials = chainMaterials; }

    public String getChainPattern() { return chainPattern; }
    public void setChainPattern(String chainPattern) { this.chainPattern = chainPattern; }

    public Integer getLighting() { return lighting; }
    public void setLighting(Integer lighting) { this.lighting = lighting; }

    // --- growable ---
    public List<String> getStructures() { return structures; }
    public void setStructures(List<String> structures) { this.structures = structures; }

    public Boolean getInstant() { return instant; }
    public void setInstant(Boolean instant) { this.instant = instant; }

    // --- scripting ---
    public Script getScript() { return script; }
    public void setScript(Script script) { this.script = script; }

    /**
     * Auto-detect and set type based on parsed model.
     * If model has animations, sets type to "animated" and picks a
     * default animation (first "idle"-named, else first animation).
     */
    public void autoDetectType(BBModel model) {
        if (this.type == null) {
            if (model.hasAnimations()) {
                this.type = "animated";
            } else {
                DecoType detected = TypeDetector.detectFromModel(model);
                if (detected != null) {
                    this.type = detected.getJsonValue();
                }
            }
        }

        if ("animated".equals(this.type) && this.defaultAnimation == null) {
            this.defaultAnimation = ModelInspector.pickDefaultAnimation(model);
        }
    }
}