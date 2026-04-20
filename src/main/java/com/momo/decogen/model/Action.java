package com.momo.decogen.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A script action (on_use, shift_on_use, added, trigger, animation_start,
 * animation_end, tool_modelswitch). Carries any combination of:
 *   - link     : decoref of another block to switch to
 *   - sound    : single one-shot sound id
 *   - animations : list of animation state transitions
 *   - sounds   : list of animation-synced sounds
 *   - storage  : inventory size [columns, rows]
 */
public class Action {
    private String link;
    private String sound;
    private List<AnimationPair> animations;
    private List<SoundPair> sounds;
    private int[] storage;

    public Action() {}

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public String getSound() { return sound; }
    public void setSound(String sound) { this.sound = sound; }

    public List<AnimationPair> getAnimations() { return animations; }
    public void setAnimations(List<AnimationPair> animations) { this.animations = animations; }

    public List<SoundPair> getSounds() { return sounds; }
    public void setSounds(List<SoundPair> sounds) { this.sounds = sounds; }

    public int[] getStorage() { return storage; }
    public void setStorage(int[] storage) { this.storage = storage; }

    public void addAnimation(String from, String to) {
        if (animations == null) animations = new ArrayList<>();
        animations.add(new AnimationPair(from, to));
    }

    public boolean hasAnimations() {
        return animations != null && !animations.isEmpty();
    }

    public boolean hasSounds() {
        return sounds != null && !sounds.isEmpty();
    }

    public boolean isEmpty() {
        return (link == null || link.isEmpty())
                && (sound == null || sound.isEmpty())
                && !hasAnimations()
                && !hasSounds()
                && (storage == null || storage.length == 0);
    }
}