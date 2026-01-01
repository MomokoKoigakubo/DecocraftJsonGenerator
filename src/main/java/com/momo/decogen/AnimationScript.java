package com.momo.decogen;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an animation script handler (on_use or animation_end).
 * Contains a list of animation transitions.
 */
public class AnimationScript {
    private String link;  // For simple link-only on_use
    private List<AnimationPair> animations;

    public AnimationScript() {}

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public List<AnimationPair> getAnimations() { return animations; }
    public void setAnimations(List<AnimationPair> animations) { this.animations = animations; }

    /**
     * Add an animation pair
     */
    public void addAnimation(String from, String to) {
        if (animations == null) {
            animations = new ArrayList<>();
        }
        animations.add(new AnimationPair(from, to));
    }

    /**
     * Check if this has animation pairs (not just a simple link)
     */
    public boolean hasAnimations() {
        return animations != null && !animations.isEmpty();
    }
}
