package com.momo.decogen;

import java.util.ArrayList;
import java.util.List;

public class Model {
    private String name;
    private List<BBGroup> groups;
    private List<BBElement> elements;
    private List<Object> outliner;
    private List<BBAnimation> animations;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<BBGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<BBGroup> groups) {
        this.groups = groups;
    }

    public List<BBElement> getElements() {
        return elements;
    }

    public void setElements(List<BBElement> elements) {
        this.elements = elements;
    }

    public List<Object> getOutliner() {
        return outliner;
    }

    public void setOutliner(List<Object> outliner) {
        this.outliner = outliner;
    }

    public List<BBAnimation> getAnimations() {
        return animations;
    }

    public void setAnimations(List<BBAnimation> animations) {
        this.animations = animations;
    }

    /**
     * Check if this model has any animations
     */
    public boolean hasAnimations() {
        return animations != null && !animations.isEmpty();
    }

    /**
     * Get animation names for UI dropdowns
     */
    public List<String> getAnimationNames() {
        List<String> names = new ArrayList<>();
        if (animations != null) {
            for (BBAnimation anim : animations) {
                if (anim.getName() != null) {
                    names.add(anim.getName());
                }
            }
        }
        return names;
    }
}
