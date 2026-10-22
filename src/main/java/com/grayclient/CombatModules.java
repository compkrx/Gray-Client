package com.grayclient;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;

import java.util.List;
import java.util.Random;

import static com.grayclient.Util.mc;

public class CombatModules {
    static final Random RNG = new Random();

    private static boolean canInteract() {
        return mc.player != null && mc.world != null && mc.interactionManager != null
                && mc.currentScreen == null && mc.player.isAlive() && !mc.player.isSpectator();
    }

    // ---- HitSelect: gate used by the aura / triggerbot ----
    public static class HitSelect extends Module {
        final Setting.Num minCd = num("Min Cooldown", 0.92, 0.5, 1.0, 0.01);
        final Setting.Bool iframes = bool("Skip I-Frames", true);
        public HitSelect() { super("HitSelect", Category.COMBAT, "Only swing at full charge"); }

        public boolean allow(Entity target) {
            if (!canInteract()) return false;
            float min = enabled ? (float) minCd.d() : 0.9f;
            if (!Util.cooldownReady(min)) return false;
            if (enabled && iframes.on() && target instanceof LivingEntity le
                    && le.hurtTime > 2 && mc.player.hurtTime == 0) return false;
            return true;
        }
    }

    // ---- WTap ----
    public static class WTap extends Module {
        int stage = 0;
        public WTap() { super("WTap", Category.COMBAT, "Resets sprint after a hit"); }
        public void onAttack() { if (enabled && mc.player.isSprinting()) stage = 1; }
        @Override public void onTick() {
            if (!canInteract()) { stage = 0; return; }
            if (stage == 1) { mc.player.setSprinting(false); stage = 2; }
            else if (stage == 2) { mc.player.setSprinting(true); stage = 0; }
        }
        @Override protected void onDisable() { stage = 0; }
    }

    // ---- Velocity (logic in VelocityMixin) ----
    public static class Velocity extends Module {
        public Velocity() { super("Velocity", Category.COMBAT, "Cancels knockback"); }
    }

    // ---- AutoWeapon ----
    public static class AutoWeapon extends Module {
        public AutoWeapon() { super("AutoWeapon", Category.COMBAT, "Switches to a sword/axe when attacking"); }
        public void equip() {
            int best = -1;
            for (int i = 0; i < 9 && best < 0; i++)
                if (mc.player.getInventory().getStack(i).isIn(ItemTags.SWORDS)) best = i;
            for (int i = 0; i < 9 && best < 0; i++)
                if (mc.player.getInventory().getStack(i).isIn(ItemTags.AXES)) best = i;
            Util.select(best);
        }
        @Override public void onTick() {
            if (canInteract() && mc.options.attackKey.isPressed()
                    && mc.crosshairTarget instanceof EntityHitResult) equip();
        }
    }

    // ---- SilentAura ----
    public static class SilentAura extends Module {
        final Setting.Num range = num("Range", 3.0, 1.0, 6.0, 0.1);
        final Setting.Bool mobs = bool("Mobs", true);
        final Setting.Bool players = bool("Players", true);
        final Setting.Bool silent = bool("Silent Rotation", true);
        public SilentAura() { super("SilentAura", Category.COMBAT, "Auto-attacks nearby entities"); }

        @Override public void onTick() {
            if (!canInteract()) return;
            Util.nearest(range.d(), mobs.on(), players.on()).ifPresent(t -> {
                if (!Modules.HIT_SELECT.allow(t)) return;
                if (Modules.AUTO_WEAPON.enabled) Modules.AUTO_WEAPON.equip();
                if (silent.on()) Util.silentLook(t.getBoundingBox().getCenter());
                try { Util.attack(t); }
                finally { if (silent.on()) Util.restoreLook(); }
            });
        }
    }

    // ---- AimAssist ----
    public static class AimAssist extends Module {
        final Setting.Num fov = num("FOV", 70, 10, 180, 5);
        final Setting.Num speed = num("Speed", 4.0, 1.0, 20.0, 0.5);
        final Setting.Num range = num("Range", 4.5, 1.0, 6.0, 0.5);
        public AimAssist() { super("AimAssist", Category.COMBAT, "Smoothly aims at targets while you attack"); }

        @Override public void onTick() {
            if (!canInteract() || !mc.options.attackKey.isPressed()) return;
            Util.nearest(range.d(), true, true).ifPresent(t -> {
                float[] r = Util.rotTo(mc.player.getEyePos(), t.getBoundingBox().getCenter());
                float dy = MathHelper.wrapDegrees(r[0] - mc.player.getYaw());
                float dp = r[1] - mc.player.getPitch();
                if (Math.abs(dy) > fov.d()) return;
                float sp = (float) speed.d();
                mc.player.setYaw(mc.player.getYaw() + MathHelper.clamp(dy, -sp, sp));
                mc.player.setPitch(mc.player.getPitch() + MathHelper.clamp(dp, -sp / 2f, sp / 2f));
            });
        }
    }

    // ---- TriggerBot ----
    public static class TriggerBot extends Module {
        final Setting.Bool mobs = bool("Mobs", true);
        final Setting.Bool players = bool("Players", true);
        public TriggerBot() { super("TriggerBot", Category.COMBAT, "Attacks whatever is under the crosshair"); }

        @Override public void onTick() {
            if (!canInteract()) return;
            if (mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity le
                    && Util.valid(le, mobs.on(), players.on()) && Modules.HIT_SELECT.allow(le)) Util.attack(le);
        }
    }

    // ---- AutoClicker ----
    public static class AutoClicker extends Module {
        final Setting.Num minCps = num("Min CPS", 8, 1, 20, 1);
        final Setting.Num maxCps = num("Max CPS", 12, 1, 20, 1);
        int delay = 0;
        public AutoClicker() { super("AutoClicker", Category.COMBAT, "Attacks while you hold left click"); }

        @Override protected void onDisable() { delay = 0; }

        @Override public void onTick() {
            if (!canInteract()) { delay = 0; return; }
            if (delay > 0) { delay--; return; }
            if (!mc.options.attackKey.isPressed()) return;
            if (mc.crosshairTarget instanceof EntityHitResult hit) {
                Entity e = hit.getEntity();
                if (Modules.HIT_SELECT.enabled && !Modules.HIT_SELECT.allow(e)) return;
                Util.attack(e);
                double lo = Math.min(minCps.d(), maxCps.d()), hi = Math.max(minCps.d(), maxCps.d());
                double cps = lo + RNG.nextDouble() * (hi - lo);
                delay = Math.max(0, (int) Math.round(20.0 / cps) - 1);
            }
        }
    }

    // ---- CrystalAura ----
    public static class CrystalAura extends Module {
        final Setting.Num range = num("Break Range", 4.5, 3.0, 6.0, 0.5);
        final Setting.Num minHealth = num("Min Health", 6, 1, 20, 1);
        final Setting.Num placeDelay = num("Place Delay", 2, 0, 10, 1);
        int placeCd = 0;
        public CrystalAura() { super("CrystalAura", Category.COMBAT, "Places and breaks end crystals near a player"); }

        @Override protected void onDisable() { placeCd = 0; }

        @Override public void onTick() {
            if (!canInteract()) return;
            if (mc.player.getHealth() + mc.player.getAbsorptionAmount() < minHealth.d()) return;
            var targetOpt = Util.nearest(10, false, true);
            if (targetOpt.isEmpty()) return;
            LivingEntity target = targetOpt.get();
            double br = range.d();

            List<EndCrystalEntity> crystals = mc.world.getEntitiesByClass(EndCrystalEntity.class,
                    mc.player.getBoundingBox().expand(br + 1),
                    c -> c.squaredDistanceTo(target) <= 36 && Util.reachDist(c) <= br);
            if (!crystals.isEmpty()) {
                EndCrystalEntity c = crystals.get(0);
                Util.silentLook(c.getBoundingBox().getCenter());
                mc.interactionManager.attackEntity(mc.player, c);
                mc.player.swingHand(Hand.MAIN_HAND);
                Util.restoreLook();
                return;
            }

            if (placeCd-- > 0) return;
            Hand hand = Hand.MAIN_HAND;
            if (mc.player.getOffHandStack().isOf(Items.END_CRYSTAL)) hand = Hand.OFF_HAND;
            else if (!mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) {
                int s = Util.hotbar(Items.END_CRYSTAL);
                if (s < 0) return;
                Util.select(s);
            }
            BlockPos best = null; double bestD = 999;
            BlockPos me = mc.player.getBlockPos();
            for (BlockPos p : BlockPos.iterate(me.add(-4, -3, -4), me.add(4, 3, 4))) {
                var b = mc.world.getBlockState(p).getBlock();
                if (b != Blocks.OBSIDIAN && b != Blocks.BEDROCK) continue;
                if (!mc.world.isAir(p.up()) || !mc.world.isAir(p.up(2))) continue;
                if (!mc.world.getOtherEntities(null, new Box(p.up())).isEmpty()) continue;
                Vec3d cp = Vec3d.ofCenter(p.up());
                if (mc.player.getEntityPos().squaredDistanceTo(cp) < 6.25) continue;
                if (mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(p)) > 20) continue;
                double d = target.getEntityPos().squaredDistanceTo(cp);
                if (d < bestD && d <= 36) { bestD = d; best = p.toImmutable(); }
            }
            if (best != null) {
                Vec3d face = Vec3d.ofCenter(best).add(0, 0.5, 0);
                Util.silentLook(face);
                mc.interactionManager.interactBlock(mc.player, hand,
                        new BlockHitResult(face, Direction.UP, best, false));
                mc.player.swingHand(hand);
                Util.restoreLook();
                placeCd = placeDelay.i();
            }
        }
    }

    // ---- CrystalAssist ----
    public static class CrystalAssist extends Module {
        int cd = 0;
        public CrystalAssist() { super("CrystalAssist", Category.COMBAT, "Instant crystal break/place where you aim"); }

        @Override protected void onDisable() { cd = 0; }

        @Override public void onTick() {
            if (!canInteract()) return;
            if (cd > 0) { cd--; return; }
            HitResult hr = mc.crosshairTarget;
            if (hr instanceof EntityHitResult eh && eh.getEntity() instanceof EndCrystalEntity c
                    && mc.options.attackKey.isPressed()) {
                mc.interactionManager.attackEntity(mc.player, c);
                mc.player.swingHand(Hand.MAIN_HAND);
            } else if (hr instanceof BlockHitResult bh && hr.getType() == HitResult.Type.BLOCK
                    && mc.options.useKey.isPressed() && mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) {
                var b = mc.world.getBlockState(bh.getBlockPos()).getBlock();
                if ((b == Blocks.OBSIDIAN || b == Blocks.BEDROCK) && bh.getSide() == Direction.UP
                        && mc.world.getOtherEntities(null, new Box(bh.getBlockPos().up())).isEmpty()) {
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bh);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    cd = 1;
                }
            }
        }
    }

    // ---- MaceBot ----
    public static class MaceBot extends Module {
        final Setting.Num minFall = num("Min Fall", 1.5, 1.0, 10.0, 0.5);
        int prev = -1, equippedSlot = -1;
        double peakY = Double.NaN;
        public MaceBot() { super("MaceBot", Category.COMBAT, "Mace smash when falling onto a target"); }

        void restoreSlot() {
            if (mc.player != null && prev >= 0 && mc.player.getInventory().getSelectedSlot() == equippedSlot)
                Util.select(prev);
            prev = -1;
            equippedSlot = -1;
        }

        @Override protected void onEnable() {
            peakY = mc.player == null ? Double.NaN : mc.player.getY();
            prev = -1;
            equippedSlot = -1;
        }

        @Override public void onTick() {
            if (!canInteract()) { restoreSlot(); peakY = Double.NaN; return; }
            double y = mc.player.getY();
            if (!Double.isFinite(peakY) || mc.player.isOnGround()) peakY = y;
            else peakY = Math.max(peakY, y);
            double fallen = Math.max(0, peakY - y);

            int mace = Util.hotbar(Items.MACE);
            if (mace < 0) { restoreSlot(); return; }
            if (fallen >= minFall.d() && !mc.player.isOnGround() && mc.player.getVelocity().y < 0) {
                var t = Util.nearest(3.0, true, true);
                if (t.isEmpty()) { restoreSlot(); return; }
                if (t.isPresent() && Modules.HIT_SELECT.allow(t.get())) {
                    if (prev < 0) prev = mc.player.getInventory().getSelectedSlot();
                    equippedSlot = mace;
                    Util.select(mace);
                    Util.silentLook(t.get().getBoundingBox().getCenter());
                    try { Util.attack(t.get()); }
                    finally { Util.restoreLook(); }
                }
            } else {
                restoreSlot();
            }
        }
        @Override protected void onDisable() { restoreSlot(); peakY = Double.NaN; }
    }

    /** Uses a normal hotbar axe while the player attacks a blocking target. */
    public static class ShieldBreaker extends Module {
        final Setting.Bool restore = bool("Restore Slot", true);
        int previousSlot = -1, axeSlot = -1;

        public ShieldBreaker() {
            super("ShieldBreaker", Category.COMBAT, "Selects a hotbar axe when attacking a raised shield");
        }

        void releaseSlot() {
            if (restore.on() && mc.player != null && previousSlot >= 0
                    && mc.player.getInventory().getSelectedSlot() == axeSlot) Util.select(previousSlot);
            previousSlot = -1;
            axeSlot = -1;
        }

        @Override protected void onDisable() { releaseSlot(); }

        @Override public void onTick() {
            if (!canInteract() || !mc.options.attackKey.isPressed()
                    || !(mc.crosshairTarget instanceof EntityHitResult hit)
                    || !(hit.getEntity() instanceof LivingEntity target) || !target.isBlocking()) {
                releaseSlot();
                return;
            }
            if (previousSlot >= 0 && mc.player.getInventory().getSelectedSlot() != axeSlot) {
                // Respect a manual slot change rather than restoring over it later.
                previousSlot = -1;
                axeSlot = -1;
            }
            for (int slot = 0; slot < 9; slot++) {
                if (!mc.player.getInventory().getStack(slot).isIn(ItemTags.AXES)) continue;
                if (previousSlot < 0) previousSlot = mc.player.getInventory().getSelectedSlot();
                axeSlot = slot;
                Util.select(slot);
                return;
            }
            releaseSlot();
        }
    }

    /** Releases an already drawn bow; it never selects an item or starts use. */
    public static class BowRelease extends Module {
        final Setting.Num chargeTicks = num("Charge Ticks", 20, 3, 40, 1);

        public BowRelease() {
            super("BowRelease", Category.COMBAT, "Releases your held bow after the configured draw time");
        }

        @Override public void onTick() {
            if (!canInteract() || !mc.player.isUsingItem() || !mc.player.getActiveItem().isOf(Items.BOW)) return;
            if (mc.player.getItemUseTime() >= chargeTicks.i()) mc.interactionManager.stopUsingItem(mc.player);
        }
    }
}
