package com.grayclient;

import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.Fertilizable;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.grayclient.Util.mc;

/** Normal interaction helpers for renewable farms. No server-side growth or catch simulation. */
public final class AdditionalFarmModules {
    private AdditionalFarmModules() {}

    private static boolean mayInteract() {
        return mc.player != null && mc.world != null && mc.interactionManager != null
                && mc.currentScreen == null && !mc.player.isUsingItem()
                && !mc.options.attackKey.isPressed();
    }

    public static class AutoShear extends Module {
        final Setting.Num range = num("Range", 3.0, 1.0, 4.5, 0.5);
        final Setting.Num delay = num("Delay", 6, 4, 40, 1);
        int cooldown;

        public AutoShear() {
            super("AutoShear", Category.FARMING, "Uses hotbar shears on nearby adult sheep with wool");
        }

        @Override protected void onDisable() { cooldown = 0; }

        @Override public void onTick() {
            if (!mayInteract()) return;
            if (cooldown > 0) { cooldown--; return; }
            int shears = Util.hotbar(Items.SHEARS);
            if (shears < 0) return;
            var sheep = mc.world.getEntitiesByClass(SheepEntity.class,
                            mc.player.getBoundingBox().expand(range.d() + 1),
                            s -> s.isAlive() && s.isShearable() && mc.player.canSee(s)
                                    && Util.reachDist(s) <= range.d()).stream()
                    .min(Comparator.comparingDouble(Util::reachDist));
            if (sheep.isEmpty()) return;
            int previous = mc.player.getInventory().getSelectedSlot();
            try {
                Util.select(shears);
                mc.interactionManager.interactEntity(mc.player, sheep.get(), Hand.MAIN_HAND);
                mc.player.swingHand(Hand.MAIN_HAND);
            } finally {
                Util.select(previous);
            }
            cooldown = delay.i();
        }
    }

    public static class AutoBreed extends Module {
        final Setting.Num range = num("Range", 3.0, 1.0, 4.5, 0.5);
        final Setting.Num delay = num("Delay", 8, 4, 40, 1);
        final Map<UUID, Integer> attempted = new HashMap<>();
        int cooldown;

        public AutoBreed() {
            super("AutoBreed", Category.FARMING, "Feeds eligible adult animals their matching hotbar breeding food");
        }

        @Override protected void onDisable() { cooldown = 0; attempted.clear(); }

        @Override public void onTick() {
            // A short retry window also covers species whose love state is server authoritative.
            attempted.replaceAll((id, ticks) -> ticks - 1);
            attempted.values().removeIf(ticks -> ticks <= 0);
            if (!mayInteract()) return;
            if (cooldown > 0) { cooldown--; return; }
            var animals = mc.world.getEntitiesByClass(AnimalEntity.class,
                    mc.player.getBoundingBox().expand(range.d() + 1),
                    a -> a.isAlive() && !a.isBaby() && a.canEat() && !a.isInLove()
                            && !attempted.containsKey(a.getUuid()) && mc.player.canSee(a)
                            && Util.reachDist(a) <= range.d());
            animals.sort(Comparator.comparingDouble(Util::reachDist));
            for (AnimalEntity animal : animals) {
                int slot = -1;
                for (int i = 0; i < 9; i++) {
                    ItemStack food = mc.player.getInventory().getStack(i);
                    if (!food.isEmpty() && animal.isBreedingItem(food)) { slot = i; break; }
                }
                if (slot < 0) continue;
                int previous = mc.player.getInventory().getSelectedSlot();
                try {
                    Util.select(slot);
                    mc.interactionManager.interactEntity(mc.player, animal, Hand.MAIN_HAND);
                    mc.player.swingHand(Hand.MAIN_HAND);
                } finally {
                    Util.select(previous);
                }
                attempted.put(animal.getUuid(), 100);
                cooldown = delay.i();
                return;
            }
        }
    }

    public static class BonemealAura extends Module {
        final Setting.Num range = num("Range", 3.5, 1.0, 4.5, 0.5);
        final Setting.Num delay = num("Delay", 6, 2, 40, 1);
        final Setting.Bool cropsOnly = bool("Crops Only", true);
        int cooldown;

        public BonemealAura() {
            super("BonemealAura", Category.FARMING, "Applies hotbar bone meal to nearby plants that can still grow");
        }

        @Override protected void onDisable() { cooldown = 0; }

        @Override public void onTick() {
            if (!mayInteract()) return;
            if (cooldown > 0) { cooldown--; return; }
            int slot = Util.hotbar(Items.BONE_MEAL);
            if (slot < 0) return;
            BlockPos origin = mc.player.getBlockPos();
            int r = (int) Math.ceil(range.d());
            double nearest = range.d() * range.d();
            BlockPos target = null;
            for (BlockPos pos : BlockPos.iterate(origin.add(-r, -r, -r), origin.add(r, r, r))) {
                if (!mc.world.isChunkLoaded(pos)) continue;
                double distance = mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos));
                if (distance > nearest) continue;
                BlockState state = mc.world.getBlockState(pos);
                if (!(state.getBlock() instanceof Fertilizable plant)) continue;
                if (cropsOnly.on() && !(state.getBlock() instanceof CropBlock)) continue;
                if (!plant.isFertilizable(mc.world, pos, state)) continue;
                target = pos.toImmutable(); nearest = distance;
            }
            if (target == null) return;
            int previous = mc.player.getInventory().getSelectedSlot();
            try {
                Util.select(slot);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                        new BlockHitResult(Vec3d.ofCenter(target).add(0, 0.5, 0), Direction.UP, target, false));
                mc.player.swingHand(Hand.MAIN_HAND);
            } finally {
                Util.select(previous);
            }
            cooldown = delay.i();
        }
    }

    public static class AutoFish extends Module {
        final Setting.Num reaction = num("Reel Delay", 2, 1, 10, 1);
        final Setting.Num recast = num("Recast Delay", 20, 10, 60, 1);
        final Setting.Bool autoCast = bool("Auto Cast", true);
        final Setting.Bool selectRod = bool("Select Rod", true);
        int previousSlot = -1, cooldown, reelCountdown = -1;

        public AutoFish() {
            super("AutoFish", Category.FARMING, "Reels your bobber after its splash and casts again with a held rod");
        }

        @Override protected void onEnable() {
            cooldown = 10; reelCountdown = -1; previousSlot = -1;
            if (mc.player == null || !selectRod.on() || rodHand() != null) return;
            int slot = Util.hotbar(Items.FISHING_ROD);
            if (slot >= 0) {
                previousSlot = mc.player.getInventory().getSelectedSlot();
                Util.select(slot);
            }
        }

        @Override protected void onDisable() {
            cooldown = 0; reelCountdown = -1;
            if (previousSlot >= 0 && mc.player != null) Util.select(previousSlot);
            previousSlot = -1;
        }

        private Hand rodHand() {
            if (mc.player.getMainHandStack().isOf(Items.FISHING_ROD)) return Hand.MAIN_HAND;
            if (mc.player.getOffHandStack().isOf(Items.FISHING_ROD)) return Hand.OFF_HAND;
            return null;
        }

        /** Call on the client thread only for the fishing-bobber splash sound. */
        public void onSplash(double x, double y, double z) {
            if (!enabled || !mayInteract() || rodHand() == null || mc.player.fishHook == null
                    || reelCountdown >= 0 || cooldown > 0) return;
            var hook = mc.player.fishHook;
            if (hook.getPlayerOwner() != mc.player || hook.getHookedEntity() != null) return;
            if (hook.getEntityPos().squaredDistanceTo(new Vec3d(x, y, z)) <= 2.25) {
                reelCountdown = reaction.i();
            }
        }

        @Override public void onTick() {
            if (!mayInteract()) { reelCountdown = -1; return; }
            Hand hand = rodHand();
            if (hand == null && selectRod.on()) {
                int slot = Util.hotbar(Items.FISHING_ROD);
                if (slot >= 0) {
                    if (previousSlot < 0) previousSlot = mc.player.getInventory().getSelectedSlot();
                    Util.select(slot);
                    hand = Hand.MAIN_HAND;
                }
            }
            if (hand == null) { reelCountdown = -1; return; }
            if (cooldown > 0) { cooldown--; return; }
            if (mc.player.fishHook != null) {
                if (reelCountdown < 0) return;
                if (reelCountdown-- > 0) return;
                mc.interactionManager.interactItem(mc.player, hand);
                mc.player.swingHand(hand);
                reelCountdown = -1; cooldown = recast.i();
                return;
            }
            reelCountdown = -1;
            if (!autoCast.on()) return;
            mc.interactionManager.interactItem(mc.player, hand);
            mc.player.swingHand(hand);
            // Also limits retries when a cast is rejected or no hook has arrived yet.
            cooldown = Math.max(20, recast.i());
        }
    }
}
