package com.grayclient;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.Optional;

public class Util {
    public static final MinecraftClient mc = MinecraftClient.getInstance();

    // targeting options (edit here if you want to hit these too)
    public static boolean skipArmorStands = true;
    public static boolean skipTamedPets = true;

    public static float[] rotTo(Vec3d from, Vec3d to) {
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.clamp(pitch, -90f, 90f)};
    }

    /** Server sees this look direction; your camera does not move. */
    public static void silentLook(Vec3d target) {
        float[] r = rotTo(mc.player.getEyePos(), target);
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                r[0], r[1], mc.player.isOnGround(), mc.player.horizontalCollision));
    }

    /** Puts the server-side look direction back to your real camera. */
    public static void restoreLook() {
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround(), mc.player.horizontalCollision));
    }

    public static void attack(Entity e) {
        mc.interactionManager.attackEntity(mc.player, e);
        mc.player.swingHand(Hand.MAIN_HAND);
        Modules.WTAP.onAttack();
    }

    public static boolean cooldownReady(float min) {
        return mc.player.getAttackCooldownProgress(0.5f) >= min;
    }

    /** Distance from your eyes to the closest point of the entity's hitbox. */
    public static double reachDist(Entity e) {
        Vec3d eye = mc.player.getEyePos();
        Box b = e.getBoundingBox();
        double x = MathHelper.clamp(eye.x, b.minX, b.maxX);
        double y = MathHelper.clamp(eye.y, b.minY, b.maxY);
        double z = MathHelper.clamp(eye.z, b.minZ, b.maxZ);
        return eye.distanceTo(new Vec3d(x, y, z));
    }

    public static Optional<LivingEntity> nearest(double range, boolean mobs, boolean players) {
        return mc.world.getEntitiesByClass(LivingEntity.class,
                        mc.player.getBoundingBox().expand(range + 1), e -> valid(e, mobs, players)).stream()
                .filter(e -> reachDist(e) <= range)
                .min(Comparator.comparingDouble(Util::reachDist));
    }

    /** mobs = every non-player living entity (hostile, passive, neutral, villagers...). */
    public static boolean valid(LivingEntity e, boolean mobs, boolean players) {
        if (e == mc.player || !e.isAlive() || e.isInvulnerable()) return false;
        if (e instanceof PlayerEntity p) return players && !p.isCreative() && !p.isSpectator();
        if (!mobs) return false;
        if (skipArmorStands && e instanceof ArmorStandEntity) return false;
        if (skipTamedPets && e instanceof TameableEntity t && t.isTamed()) return false;
        return true;
    }

    public static int hotbar(Item item) {
        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        return -1;
    }

    public static void select(int slot) {
        if (slot >= 0 && slot < 9) mc.player.getInventory().setSelectedSlot(slot);
    }

    /** Hotbar slot with the fastest mining speed for this block (current slot if none is better). */
    public static int bestTool(BlockState st) {
        var inv = mc.player.getInventory();
        int best = inv.getSelectedSlot();
        float bestSpeed = inv.getStack(best).getMiningSpeedMultiplier(st);
        for (int i = 0; i < 9; i++) {
            float sp = inv.getStack(i).getMiningSpeedMultiplier(st);
            if (sp > bestSpeed) { bestSpeed = sp; best = i; }
        }
        return best;
    }
}
