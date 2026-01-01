package com.momo.decogen;

public class Script {
    private Integer light;
    private ModelSwitch tool_modelswitch;
    private AnimationScript on_use;
    private AnimationScript animation_end;

    public Integer getLight() { return light; }
    public void setLight(Integer light) { this.light = light; }

    public ModelSwitch getToolModelSwitch() { return tool_modelswitch; }
    public void setToolModelSwitch(ModelSwitch tool_modelswitch) { this.tool_modelswitch = tool_modelswitch; }

    public AnimationScript getOnUse() { return on_use; }
    public void setOnUse(AnimationScript on_use) { this.on_use = on_use; }

    public AnimationScript getAnimationEnd() { return animation_end; }
    public void setAnimationEnd(AnimationScript animation_end) { this.animation_end = animation_end; }

    // Convenience methods
    public void setToolModelSwitchLink(String link) {
        this.tool_modelswitch = new ModelSwitch(link);
    }

    public void setOnUseLink(String link) {
        if (this.on_use == null) {
            this.on_use = new AnimationScript();
        }
        this.on_use.setLink(link);
    }
}
