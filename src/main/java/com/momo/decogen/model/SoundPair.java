package com.momo.decogen.model;

/**
 * An animation-synced sound trigger.
 * from/to match animation state transitions; loop=true plays while in "from".
 */
public class SoundPair {
    private String from;
    private String to;
    private String sound;
    private Boolean loop;

    public SoundPair() {}

    public SoundPair(String from, String to, String sound, Boolean loop) {
        this.from = from;
        this.to = to;
        this.sound = sound;
        this.loop = loop;
    }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getSound() { return sound; }
    public void setSound(String sound) { this.sound = sound; }

    public Boolean getLoop() { return loop; }
    public void setLoop(Boolean loop) { this.loop = loop; }
}