package com.momo.decogen;

/**
 * Represents an animation from a Blockbench model file.
 */
public class BBAnimation {
    private String uuid;
    private String name;
    private String loop;  // "loop", "once", "hold"
    private double length;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLoop() { return loop; }
    public void setLoop(String loop) { this.loop = loop; }

    public double getLength() { return length; }
    public void setLength(double length) { this.length = length; }
}
