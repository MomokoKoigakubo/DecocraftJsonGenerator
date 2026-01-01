package com.momo.decogen;

/**
 * Represents an animation transition pair (from -> to).
 */
public class AnimationPair {
    private String from;
    private String to;

    public AnimationPair() {}

    public AnimationPair(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
}
