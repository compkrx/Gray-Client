package com.grayclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public enum Category {
        COMBAT("Combat"), PLAYER("Player"), FARMING("Farming"), VISUAL("Visual");
        public final String label;
        Category(String label) { this.label = label; }
    }

    public final String name, description;
    public final Category category;
    public boolean enabled;
    public final List<Setting<?>> settings = new ArrayList<>();

    protected Module(String name, Category category, String description) {
        this.name = name; this.category = category; this.description = description;
    }

    protected Setting.Bool bool(String n, boolean def) {
        Setting.Bool s = new Setting.Bool(n, def); settings.add(s); return s;
    }
    protected Setting.Num num(String n, double def, double min, double max, double step) {
        Setting.Num s = new Setting.Num(n, def, min, max, step); settings.add(s); return s;
    }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean on) {
        if (on == enabled) return;
        // These controllers share one block-breaking channel and cannot mine concurrently.
        if (on && (this instanceof FarmModules.AutoFarm || this instanceof FarmModules.Nuker
                || this instanceof SurvivalModules.VeinMiner)) {
            for (Module other : Modules.ALL) {
                if (other != this && (other instanceof FarmModules.AutoFarm || other instanceof FarmModules.Nuker
                        || other instanceof SurvivalModules.VeinMiner)) other.setEnabled(false);
            }
        }
        enabled = on;
        try { if (on) onEnable(); else onDisable(); }
        catch (RuntimeException ex) {
            enabled = false;
            System.err.println("[Gray Client] " + name + " lifecycle failed: " + ex);
            try { onDisable(); } catch (RuntimeException cleanup) {
                System.err.println("[Gray Client] " + name + " cleanup failed: " + cleanup);
            }
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null)
            mc.player.sendMessage(Text.literal("\u00a77[Gray Client] \u00a7f" + name
                    + (enabled ? " \u00a7fON" : " \u00a78OFF")), true);
    }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}
}
