package com.grayclient;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;

import static com.grayclient.Util.mc;

public class VisualModules {

    // ---- Fullbright (client-side night vision) ----
    public static class Fullbright extends Module {
        private StatusEffectInstance applied;
        private LivingEntity owner;
        public Fullbright() { super("Fullbright", Category.VISUAL, "See in the dark"); }
        @Override public void onTick() {
            if (mc.player == null) return;
            if (owner != mc.player) { owner = mc.player; applied = null; }
            StatusEffectInstance cur = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
            // Leave potion/beacon/server effects intact. Track the exact instance we add.
            if (cur == null || (cur == applied && cur.getDuration() < 300)) {
                mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000, 0, true, false, false));
                applied = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
            } else if (cur != applied) applied = null;
        }
        @Override protected void onDisable() {
            if (owner != null && applied != null && owner.getStatusEffect(StatusEffects.NIGHT_VISION) == applied)
                owner.removeStatusEffect(StatusEffects.NIGHT_VISION);
            applied = null;
            owner = null;
        }
    }

    // ---- ESP (glow outline) ----
    public static class Esp extends Module {
        final Setting.Bool players = bool("Players", true);
        final Setting.Bool hostile = bool("Hostile", true);
        final Setting.Bool passive = bool("Passive", false);
        final Setting.Num range = num("Range", 64, 8, 128, 8);
        final Map<Integer, Entity> flagged = new HashMap<>();
        public Esp() { super("ESP", Category.VISUAL, "Glow outline on entities"); }

        void clearGlow(Entity entity) {
            if (!(entity instanceof LivingEntity living) || !living.hasStatusEffect(StatusEffects.GLOWING))
                entity.setGlowing(false);
        }

        @Override public void onTick() {
            if (mc.world == null || mc.player == null) return;
            // Entity IDs can be recycled and unloaded IDs must not accumulate forever.
            flagged.entrySet().removeIf(entry -> mc.world.getEntityById(entry.getKey()) != entry.getValue());
            for (Entity e : mc.world.getEntities()) {
                if (!(e instanceof LivingEntity) || e == mc.player) continue;
                boolean want;
                if (e instanceof PlayerEntity) want = players.on();
                else if (e instanceof Monster) want = hostile.on();
                else want = passive.on();
                if (want && mc.player.distanceTo(e) <= range.d()) {
                    if (!e.isGlowing()) {
                        e.setGlowing(true);
                        flagged.put(e.getId(), e);
                    }
                } else if (flagged.remove(e.getId()) != null) {
                    clearGlow(e);
                }
            }
        }

        @Override protected void onDisable() {
            for (Entity entity : flagged.values()) clearGlow(entity);
            flagged.clear();
        }
    }

    // ---- HUD ----
    public static class Hud extends Module {
        public final Setting.Bool watermark = bool("Watermark", true);
        public final Setting.Bool list = bool("Module List", true);
        public Hud() { super("HUD", Category.VISUAL, "Watermark and enabled-module list"); enabled = true; }
    }
}
