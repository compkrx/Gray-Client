package com.grayclient;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import static com.grayclient.Util.mc;

/** Original survival helpers. All interactions still obey the server's rules. */
public final class SurvivalModules {
    private SurvivalModules() {}

    private static final Set<Block> ORES = Set.of(
            Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE, Blocks.ANCIENT_DEBRIS);

    private static boolean inReach(BlockPos pos, double range) {
        return mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) <= range * range;
    }

    public static class VeinMiner extends Module {
        final Setting.Num range = num("Range", 4.0, 1.0, 4.5, 0.5);
        final Setting.Num limit = num("Max Blocks", 32, 1, 64, 1);
        final Setting.Bool logs = bool("Include Logs", false);
        final Deque<BlockPos> pending = new ArrayDeque<>();
        Block type;
        BlockPos current;
        int previousSlot = -1;
        boolean completed;

        public VeinMiner() {
            super("VeinMiner", Category.FARMING, "Hold attack on ore to mine a connected vein in reach");
        }

        /** Prevent vanilla's air-target cancellation from resetting a queued block every tick. */
        public boolean ownsMining() {
            return enabled && type != null && !completed && mc.currentScreen == null
                    && mc.options.attackKey.isPressed();
        }

        private void restoreSlot() {
            if (previousSlot >= 0 && mc.player != null) Util.select(previousSlot);
            previousSlot = -1;
        }

        @Override protected void onDisable() {
            pending.clear(); current = null; type = null; completed = false;
            restoreSlot();
            if (mc.interactionManager != null) mc.interactionManager.cancelBlockBreaking();
        }

        private void collect(BlockPos seed, Block block) {
            Set<BlockPos> seen = new HashSet<>();
            Deque<BlockPos> visit = new ArrayDeque<>();
            visit.add(seed.toImmutable());
            while (!visit.isEmpty() && pending.size() < limit.i()) {
                BlockPos pos = visit.removeFirst();
                if (!seen.add(pos) || !mc.world.isChunkLoaded(pos) || !inReach(pos, range.d())) continue;
                if (!mc.world.getBlockState(pos).isOf(block)) continue;
                pending.addLast(pos);
                for (Direction direction : Direction.values()) visit.addLast(pos.offset(direction));
            }
        }

        @Override public void onTick() {
            if (mc.currentScreen != null || !mc.options.attackKey.isPressed() || mc.player.isUsingItem()) {
                if (current != null && mc.interactionManager != null) mc.interactionManager.cancelBlockBreaking();
                pending.clear(); current = null; type = null; completed = false; restoreSlot();
                return;
            }
            // Finish one bounded cluster per held click. Release attack to arm another cluster.
            if (completed) return;
            if (type == null) {
                if (!(mc.crosshairTarget instanceof BlockHitResult hit)
                        || hit.getType() != HitResult.Type.BLOCK) return;
                BlockState state = mc.world.getBlockState(hit.getBlockPos());
                if (!ORES.contains(state.getBlock()) && !(logs.on() && state.isIn(BlockTags.LOGS))) return;
                type = state.getBlock();
                collect(hit.getBlockPos(), type);
            }
            if (current != null && (!mc.world.getBlockState(current).isOf(type) || !inReach(current, range.d()))) {
                current = null;
            }
            while (current == null && !pending.isEmpty()) {
                BlockPos next = pending.removeFirst();
                if (mc.world.isChunkLoaded(next) && mc.world.getBlockState(next).isOf(type)
                        && inReach(next, range.d())) current = next;
            }
            if (current == null) { restoreSlot(); completed = true; return; }
            BlockState state = mc.world.getBlockState(current);
            if (state.getHardness(mc.world, current) < 0) { current = null; return; }
            if (previousSlot < 0) previousSlot = mc.player.getInventory().getSelectedSlot();
            Util.select(Util.bestTool(state));
            mc.interactionManager.updateBlockBreakingProgress(current, Direction.UP);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    public static class OreFinder extends Module {
        final Setting.Num radius = num("Radius", 8, 2, 16, 1);
        final Setting.Num interval = num("Scan Delay", 40, 20, 200, 20);
        BlockPos origin, nearest;
        int cursor, wait, scanRadius;
        double nearestDistance;

        public OreFinder() {
            super("OreFinder", Category.FARMING, "Reports nearby exposed ore already present in loaded chunks");
        }

        @Override protected void onDisable() {
            origin = null; nearest = null; cursor = 0; wait = 0;
        }

        private boolean exposed(BlockPos pos) {
            for (Direction side : Direction.values()) {
                BlockPos adjacent = pos.offset(side);
                if (mc.world.isChunkLoaded(adjacent) && mc.world.getBlockState(adjacent).isAir()) return true;
            }
            return false;
        }

        @Override public void onTick() {
            if (mc.currentScreen != null) return;
            if (wait > 0) { wait--; return; }
            if (origin == null) {
                origin = mc.player.getBlockPos().toImmutable();
                scanRadius = radius.i(); cursor = 0; nearest = null; nearestDistance = Double.MAX_VALUE;
            }
            int side = scanRadius * 2 + 1;
            int total = side * side * side;
            // Spread the scan across ticks so larger radii do not stall a frame.
            for (int budget = 0; budget < 256 && cursor < total; budget++, cursor++) {
                int x = cursor % side - scanRadius;
                int y = (cursor / side) % side - scanRadius;
                int z = cursor / (side * side) - scanRadius;
                BlockPos pos = origin.add(x, y, z);
                double distance = mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos));
                if (distance > scanRadius * scanRadius || distance >= nearestDistance
                        || !mc.world.isChunkLoaded(pos)) continue;
                if (ORES.contains(mc.world.getBlockState(pos).getBlock()) && exposed(pos)) {
                    nearest = pos; nearestDistance = distance;
                }
            }
            if (cursor >= total) {
                if (nearest == null) {
                    mc.player.sendMessage(Text.literal("Gray Client · No exposed ore within " + scanRadius + " blocks"), true);
                } else {
                    String name = mc.world.getBlockState(nearest).getBlock().getName().getString();
                    mc.player.sendMessage(Text.literal("Gray Client · " + name + " at "
                            + nearest.getX() + ", " + nearest.getY() + ", " + nearest.getZ()), true);
                }
                origin = null; wait = interval.i();
            }
        }
    }

    public static class AutoPlant extends Module {
        final Setting.Num range = num("Range", 3.5, 1.0, 4.5, 0.5);
        final Setting.Num delay = num("Delay", 4, 2, 20, 1);
        final Setting.Bool wart = bool("Nether Wart", true);
        int cooldown;

        public AutoPlant() {
            super("AutoPlant", Category.FARMING, "Plants hotbar seeds into nearby empty farmland or soul sand");
        }

        @Override protected void onDisable() { cooldown = 0; }

        private int seeds(BlockState soil) {
            if (soil.isOf(Blocks.SOUL_SAND)) return wart.on() ? Util.hotbar(Items.NETHER_WART) : -1;
            if (!soil.isOf(Blocks.FARMLAND)) return -1;
            for (Item seed : new Item[]{Items.WHEAT_SEEDS, Items.CARROT, Items.POTATO, Items.BEETROOT_SEEDS}) {
                int slot = Util.hotbar(seed);
                if (slot >= 0) return slot;
            }
            return -1;
        }

        @Override public void onTick() {
            if (mc.currentScreen != null || mc.player.isUsingItem() || mc.options.attackKey.isPressed()) return;
            if (cooldown > 0) { cooldown--; return; }
            double reach = range.d();
            int r = (int) Math.ceil(reach);
            BlockPos origin = mc.player.getBlockPos();
            BlockPos best = null;
            int seedSlot = -1;
            double distance = Double.MAX_VALUE;
            for (BlockPos soil : BlockPos.iterate(origin.add(-r, -r, -r), origin.add(r, r, r))) {
                if (!mc.world.isChunkLoaded(soil) || !mc.world.getBlockState(soil.up()).isAir()) continue;
                double d = mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(soil).add(0, 0.5, 0));
                if (d > reach * reach || d >= distance) continue;
                int slot = seeds(mc.world.getBlockState(soil));
                if (slot < 0) continue;
                best = soil.toImmutable(); seedSlot = slot; distance = d;
            }
            if (best == null) return;
            int previous = mc.player.getInventory().getSelectedSlot();
            try {
                Util.select(seedSlot);
                Vec3d top = Vec3d.ofCenter(best).add(0, 0.5, 0);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                        new BlockHitResult(top, Direction.UP, best, false));
                mc.player.swingHand(Hand.MAIN_HAND);
            } finally {
                Util.select(previous);
            }
            cooldown = delay.i();
        }
    }

    public static class AutoReplenish extends Module {
        final Setting.Num threshold = num("Refill Below", 8, 0, 32, 1);
        final Setting.Num delay = num("Delay", 5, 2, 20, 1);
        final ItemStack[] remembered = new ItemStack[9];
        int cooldown;

        public AutoReplenish() {
            super("AutoReplenish", Category.PLAYER, "Refills depleted hotbar stacks from matching inventory stacks");
            clearMemory();
        }

        private void clearMemory() {
            for (int i = 0; i < remembered.length; i++) remembered[i] = ItemStack.EMPTY;
            cooldown = 0;
        }

        @Override protected void onEnable() { clearMemory(); }
        @Override protected void onDisable() { clearMemory(); }

        @Override public void onTick() {
            if (mc.currentScreen != null || mc.player.isUsingItem()
                    || mc.player.currentScreenHandler != mc.player.playerScreenHandler) return;
            var inventory = mc.player.getInventory();
            for (int hotbar = 0; hotbar < 9; hotbar++) {
                ItemStack current = inventory.getStack(hotbar);
                if (!current.isEmpty()) remembered[hotbar] = current.copy();
            }
            if (cooldown > 0) { cooldown--; return; }
            for (int hotbar = 0; hotbar < 9; hotbar++) {
                ItemStack current = inventory.getStack(hotbar);
                ItemStack wanted = current.isEmpty() ? remembered[hotbar] : current;
                if (wanted.isEmpty() || wanted.getMaxCount() <= 1 || current.getCount() > threshold.i()) continue;
                int bestSlot = -1;
                int bestCount = current.getCount();
                for (int slot = 9; slot < 36; slot++) {
                    ItemStack source = inventory.getStack(slot);
                    if (ItemStack.areItemsAndComponentsEqual(source, wanted) && source.getCount() > bestCount) {
                        bestSlot = slot; bestCount = source.getCount();
                    }
                }
                if (bestSlot < 0) continue;
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, bestSlot, hotbar,
                        SlotActionType.SWAP, mc.player);
                cooldown = delay.i();
                return;
            }
        }
    }

    public static class FastPlace extends Module {
        final Setting.Num interval = num("Interval", 4, 2, 20, 1);
        int cooldown;

        public FastPlace() {
            super("FastPlace", Category.PLAYER, "Repeats normal block placement while you hold use");
        }

        @Override protected void onDisable() { cooldown = 0; }

        @Override public void onTick() {
            if (mc.currentScreen != null || !mc.options.useKey.isPressed() || mc.player.isUsingItem()) return;
            if (cooldown > 0) { cooldown--; return; }
            if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem)) return;
            if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
            if (!inReach(hit.getBlockPos(), 4.5)) return;
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            mc.player.swingHand(Hand.MAIN_HAND);
            cooldown = interval.i();
        }
    }
}
