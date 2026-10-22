package com.grayclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.grayclient.Util.mc;

/** Small information and camera helpers, implemented independently for Gray Client. */
public final class InfoModules {
    private InfoModules() {}

    public static class InfoHUD extends Module {
        private static final EquipmentSlot[] PLAYER_EQUIPMENT = {
                EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        final Setting.Bool coordinates = bool("Coordinates", true);
        final Setting.Bool direction = bool("Direction", true);
        final Setting.Bool fps = bool("FPS", true);
        final Setting.Bool speed = bool("Speed", false);
        final Setting.Bool durability = bool("Durability", true);
        final Setting.Bool lowestEquipped = bool("Lowest Equipped", false);
        final Setting.Bool targetBlock = bool("Target Block", false);
        final Setting.Bool light = bool("Light Level", true);

        public InfoHUD() {
            super("InfoHUD", Category.VISUAL, "Coordinates, performance, equipment and block information");
        }

        private ItemStack durabilityStack() {
            ItemStack held = mc.player.getMainHandStack();
            if (!lowestEquipped.on()) return held;
            ItemStack weakest = ItemStack.EMPTY;
            double least = Double.POSITIVE_INFINITY;
            for (EquipmentSlot slot : PLAYER_EQUIPMENT) {
                ItemStack stack = mc.player.getEquippedStack(slot);
                if (!stack.isDamageable() || stack.getMaxDamage() <= 0) continue;
                double remaining = (double) (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage();
                if (remaining < least) { weakest = stack; least = remaining; }
            }
            return weakest;
        }

        /** Called by the registered client HUD renderer; nothing is rendered in menus. */
        public void render(DrawContext context) {
            if (!enabled || mc.player == null || mc.world == null || !mc.player.isAlive()
                    || mc.currentScreen != null || mc.options.hudHidden) return;
            List<String> lines = new ArrayList<>(7);
            if (coordinates.on()) {
                BlockPos position = mc.player.getBlockPos();
                lines.add("XYZ  " + position.getX() + "  " + position.getY() + "  " + position.getZ());
            }
            if (direction.on()) lines.add("Facing  " + mc.player.getHorizontalFacing().asString());
            if (fps.on()) lines.add("FPS  " + mc.getCurrentFps());
            if (speed.on()) {
                Vec3d velocity = mc.player.getVelocity();
                lines.add(String.format(Locale.ROOT, "Speed  %.2f blocks/s", Math.hypot(velocity.x, velocity.z) * 20.0));
            }
            if (durability.on()) {
                ItemStack stack = durabilityStack();
                if (stack.isDamageable() && stack.getMaxDamage() > 0) {
                    int left = Math.max(0, stack.getMaxDamage() - stack.getDamage());
                    lines.add((lowestEquipped.on() ? "Lowest gear  " : "Held item  ")
                            + left + "/" + stack.getMaxDamage());
                }
            }
            BlockPos lightPosition = mc.player.getBlockPos();
            if (mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                    && mc.world.isChunkLoaded(hit.getBlockPos())) {
                if (targetBlock.on()) lines.add("Block  " + mc.world.getBlockState(hit.getBlockPos()).getBlock().getName().getString());
                // Sample the exposed face, rather than the opaque interior of a solid block.
                lightPosition = hit.getBlockPos().offset(hit.getSide());
            }
            if (light.on() && mc.world.isChunkLoaded(lightPosition))
                lines.add("Light  block " + mc.world.getLightLevel(LightType.BLOCK, lightPosition)
                        + " / sky " + mc.world.getLightLevel(LightType.SKY, lightPosition));

            int availableWidth = Math.min(260, context.getScaledWindowWidth() - 12);
            if (availableWidth < 16 || lines.isEmpty()) return;
            // Keep the final line above the vanilla health/armor/hotbar region.
            int y = Math.max(6, Math.min(context.getScaledWindowHeight() - 90,
                    context.getScaledWindowHeight() - 70 - lines.size() * 10));
            for (String line : lines) {
                String text = mc.textRenderer.trimToWidth(line, availableWidth);
                context.fill(3, y - 1, 9 + mc.textRenderer.getWidth(text), y + 9, 0x800E0F11);
                context.drawTextWithShadow(mc.textRenderer, text, 6, y, 0xFFD8DADE);
                y += 10;
            }
        }
    }

    /** A toggle zoom using the vanilla FOV option; it does not intercept keyboard input. */
    public static class Zoom extends Module {
        final Setting.Num zoomFov = num("Zoom FOV", 30, 30, 90, 5);
        int previousFov = -1;
        int appliedFov = -1;

        public Zoom() {
            super("Zoom", Category.VISUAL, "Toggle a lower field of view; restores your original FOV on disable");
        }

        @Override protected void onEnable() {
            previousFov = mc.options.getFov().getValue();
            appliedFov = -1;
        }

        private void restoreFov() {
            if (previousFov >= 0 && appliedFov >= 0 && mc.options.getFov().getValue() == appliedFov)
                mc.options.getFov().setValue(previousFov);
            previousFov = -1;
            appliedFov = -1;
        }

        @Override protected void onDisable() { restoreFov(); }

        @Override public void onTick() {
            if (mc.player == null || mc.world == null || !mc.player.isAlive() || mc.currentScreen != null) {
                restoreFov();
                return;
            }
            int currentFov = mc.options.getFov().getValue();
            // Preserve changes made by the user or another option controller while active.
            if (previousFov < 0 || (appliedFov >= 0 && currentFov != appliedFov)) previousFov = currentFov;
            int requested = Math.min(previousFov, zoomFov.i());
            if (currentFov != requested) mc.options.getFov().setValue(requested);
            appliedFov = requested;
        }
    }
}
