package com.grayclient;

import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

import static com.grayclient.Util.mc;

public class UtilityModules {

    // ---- AutoSprint ----
    public static class AutoSprint extends Module {
        public AutoSprint() { super("AutoSprint", Category.PLAYER, "Sprints automatically while moving forward"); }
        @Override public void onTick() {
            if (mc.currentScreen != null || mc.player == null || !mc.player.isAlive()) return;
            if (mc.options.forwardKey.isPressed() && !mc.player.isSneaking()
                    && !mc.player.horizontalCollision
                    && mc.player.getHungerManager().getFoodLevel() > 6)
                mc.player.setSprinting(true);
        }
    }

    // ---- AutoRespawn ----
    public static class AutoRespawn extends Module {
        int cd = 0;
        public AutoRespawn() { super("AutoRespawn", Category.PLAYER, "Respawns immediately after dying"); }
        @Override public void onTick() {
            if (cd > 0) { cd--; return; }
            if (mc.player.isDead()) { mc.player.requestRespawn(); cd = 20; }
        }
    }

    // ---- AutoEat ----
    public static class AutoEat extends Module {
        final Setting.Num below = num("Eat Below", 14, 1, 19, 1);
        final Setting.Bool gapples = bool("Eat Gapples", false);
        boolean eating = false;
        int prev = -1;
        public AutoEat() { super("AutoEat", Category.PLAYER, "Eats food when hungry"); }

        boolean isFood(ItemStack s) {
            if (s.isEmpty() || !s.contains(DataComponentTypes.FOOD)) return false;
            Item i = s.getItem();
            if (i == Items.ROTTEN_FLESH || i == Items.SPIDER_EYE || i == Items.POISONOUS_POTATO
                    || i == Items.PUFFERFISH || i == Items.CHORUS_FRUIT || i == Items.SUSPICIOUS_STEW) return false;
            if (!gapples.on() && (i == Items.GOLDEN_APPLE || i == Items.ENCHANTED_GOLDEN_APPLE)) return false;
            return true;
        }

        void stop() {
            if (eating) mc.options.useKey.setPressed(false);
            if (mc.player != null && prev >= 0) Util.select(prev);
            prev = -1; eating = false;
        }

        @Override protected void onDisable() { stop(); }

        @Override public void onTick() {
            if (mc.currentScreen != null || mc.player == null || !mc.player.isAlive()) { stop(); return; }
            int food = mc.player.getHungerManager().getFoodLevel();
            if (eating) {
                if (food >= 20 || !isFood(mc.player.getMainHandStack())) stop();
                else mc.options.useKey.setPressed(true);
                return;
            }
            if (food > below.i()) return;
            for (int i = 0; i < 9; i++) {
                if (isFood(mc.player.getInventory().getStack(i))) {
                    prev = mc.player.getInventory().getSelectedSlot();
                    Util.select(i);
                    mc.options.useKey.setPressed(true);
                    eating = true;
                    return;
                }
            }
        }
    }

    // ---- AutoTotem ----
    public static class AutoTotem extends Module {
        int cd = 0;
        public AutoTotem() { super("AutoTotem", Category.PLAYER, "Keeps a totem in your offhand"); }
        @Override public void onTick() {
            if (cd > 0) { cd--; return; }
            if (mc.currentScreen != null) return;
            if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
            for (int i = 0; i < 36; i++) {
                if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                    int slot = i < 9 ? 36 + i : i;   // hotbar -> 36..44, main inventory -> 9..35
                    mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, 40,
                            SlotActionType.SWAP, mc.player);
                    cd = 5;
                    return;
                }
            }
        }
    }

    // ---- AutoTool ----
    public static class AutoTool extends Module {
        int prev = -1;
        public AutoTool() { super("AutoTool", Category.PLAYER, "Picks the fastest tool while you mine"); }
        @Override protected void onDisable() {
            if (mc.player != null && prev >= 0) Util.select(prev);
            prev = -1;
        }
        @Override public void onTick() {
            if (mc.player == null || mc.world == null) { prev = -1; return; }
            if (mc.currentScreen == null && mc.options.attackKey.isPressed()
                    && mc.crosshairTarget instanceof BlockHitResult bh
                    && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                BlockState st = mc.world.getBlockState(bh.getBlockPos());
                int cur = mc.player.getInventory().getSelectedSlot();
                int best = Util.bestTool(st);
                if (best != cur) {
                    if (prev < 0) prev = cur;
                    Util.select(best);
                }
            } else if (prev >= 0) {
                Util.select(prev); prev = -1;
            }
        }
    }
}
