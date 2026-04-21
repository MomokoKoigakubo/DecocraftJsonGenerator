package com.momo.decogen.model;

public class Script {
    private Action on_use;
    private Action shift_on_use;
    private Action added;
    private Action trigger;
    private Action animation_start;
    private Action animation_end;
    private Action tool_modelswitch;
    private Integer counter;
    private Integer light;

    public Action getOnUse() { return on_use; }
    public void setOnUse(Action on_use) { this.on_use = on_use; }

    public Action getShiftOnUse() { return shift_on_use; }
    public void setShiftOnUse(Action shift_on_use) { this.shift_on_use = shift_on_use; }

    public Action getAdded() { return added; }
    public void setAdded(Action added) { this.added = added; }

    public Action getTrigger() { return trigger; }
    public void setTrigger(Action trigger) { this.trigger = trigger; }

    public Action getAnimationStart() { return animation_start; }
    public void setAnimationStart(Action animation_start) { this.animation_start = animation_start; }

    public Action getAnimationEnd() { return animation_end; }
    public void setAnimationEnd(Action animation_end) { this.animation_end = animation_end; }

    public Action getToolModelSwitch() { return tool_modelswitch; }
    public void setToolModelSwitch(Action tool_modelswitch) { this.tool_modelswitch = tool_modelswitch; }

    public Integer getCounter() { return counter; }
    public void setCounter(Integer counter) { this.counter = counter; }

    public Integer getLight() { return light; }
    public void setLight(Integer light) { this.light = light; }

    // Convenience helpers

    public void setToolModelSwitchLink(String link) {
        if (this.tool_modelswitch == null) {
            this.tool_modelswitch = new Action();
        }
        this.tool_modelswitch.setLink(link);
    }

    public void setOnUseLink(String link) {
        if (this.on_use == null) {
            this.on_use = new Action();
        }
        this.on_use.setLink(link);
    }
}