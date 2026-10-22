package com.grayclient;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

import static com.grayclient.Util.mc;

public class FarmModules {

    // ---- AutoFarm: harvest + replant ----
    public static class AutoFarm extends Module {
        final Setting.Num range = num("Range", 4.0, 2.0, 5.0, 0.5);
        final Setting.Num delay = num("Delay", 1, 0, 10, 1);
        final Setting.Bool replant = bool("Replant", true);
        final Setting.Bool gourds = bool("Melons/Pumpkins", true);
        final Setting.Bool cane = bool("Sugar Cane", true);

        int cd = 0, prevSlot = -1, retryTicks;
        BlockPos current = null;
        Item lastSeed = null;
        final Map<BlockPos, Integer> tries = new HashMap<>();

        public AutoFarm() { super("AutoFarm", Category.FARMING, "Harvests and replants crops in reach"); }

        @Override protected void onDisable() {
            if (mc.player != null) restoreSlot();
            if (mc.interactionManager != null) mc.interactionManager.cancelBlockBreaking();
            prevSlot = -1; current = null; tries.clear(); cd = 0; retryTicks = 0;
        }

        void restoreSlot() { if (prevSlot >= 0) Util.select(prevSlot); prevSlot = -1; }
        void remember() { if (prevSlot < 0) prevSlot = mc.player.getInventory().getSelectedSlot(); }

        boolean hasStem(BlockPos p) {
            for (Direction d : Direction.Type.HORIZONTAL) {
                Block nb = mc.world.getBlockState(p.offset(d)).getBlock();
                if (nb == Blocks.ATTACHED_MELON_STEM || nb == Blocks.ATTACHED_PUMPKIN_STEM) return true;
            }
            return false;
        }

        boolean harvestable(BlockPos p) {
            BlockState st = mc.world.getBlockState(p);
            Block b = st.getBlock();
            if (b instanceof CropBlock c) return c.isMature(st);
            if (b == Blocks.NETHER_WART) return st.get(NetherWartBlock.AGE) >= 3;
            if (gourds.on() && (b == Blocks.MELON || b == Blocks.PUMPKIN)) return hasStem(p);
            if (cane.on() && b == Blocks.SUGAR_CANE) return mc.world.getBlockState(p.down()).isOf(Blocks.SUGAR_CANE);
            return false;
        }

        Item seedFor(Block b) {
            if (b == Blocks.WHEAT) return Items.WHEAT_SEEDS;
            if (b == Blocks.CARROTS) return Items.CARROT;
            if (b == Blocks.POTATOES) return Items.POTATO;
            if (b == Blocks.BEETROOTS) return Items.BEETROOT_SEEDS;
            if (b == Blocks.NETHER_WART) return Items.NETHER_WART;
            return null;
        }

        Item pickSeed() {
            if (lastSeed != null && lastSeed != Items.NETHER_WART && Util.hotbar(lastSeed) >= 0) return lastSeed;
            for (Item i : new Item[]{Items.WHEAT_SEEDS, Items.CARROT, Items.POTATO, Items.BEETROOT_SEEDS})
                if (Util.hotbar(i) >= 0) return i;
            return null;
        }

        BlockPos nearestHarvestable(double r, Vec3d eye) {
            BlockPos me = mc.player.getBlockPos();
            int ir = (int) Math.ceil(r);
            BlockPos best = null; double bd = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.iterate(me.add(-ir, -ir, -ir), me.add(ir, ir, ir))) {
                double d = eye.squaredDistanceTo(Vec3d.ofCenter(p));
                if (d > r * r || d >= bd || !harvestable(p)) continue;
                best = p.toImmutable(); bd = d;
            }
            return best;
        }

        boolean plantOne(double r, Vec3d eye) {
            BlockPos me = mc.player.getBlockPos();
            int ir = (int) Math.ceil(r);
            for (BlockPos p : BlockPos.iterate(me.add(-ir, -ir, -ir), me.add(ir, ir, ir))) {
                if (eye.squaredDistanceTo(Vec3d.ofCenter(p)) > r * r) continue;
                if (!mc.world.getBlockState(p).isAir()) continue;
                if (mc.player.getBoundingBox().intersects(new Box(p))) continue;
                BlockPos base = p.down().toImmutable();
                BlockState bs = mc.world.getBlockState(base);
                Item seed = null;
                if (bs.isOf(Blocks.FARMLAND)) seed = pickSeed();
                else if (bs.isOf(Blocks.SOUL_SAND) && Util.hotbar(Items.NETHER_WART) >= 0) seed = Items.NETHER_WART;
                if (seed == null) continue;
                int slot = Util.hotbar(seed);
                if (slot < 0) continue;
                if (tries.merge(base, 1, Integer::sum) > 4) continue;
                remember();
                Util.select(slot);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                        new BlockHitResult(Vec3d.ofCenter(base).add(0, 0.5, 0), Direction.UP, base, false));
                mc.player.swingHand(Hand.MAIN_HAND);
                return true;
            }
            return false;
        }

        @Override public void onTick() {
            if (mc.currentScreen != null) return;
            if (++retryTicks >= 100) { tries.clear(); retryTicks = 0; }
            double r = range.d();
            Vec3d eye = mc.player.getEyePos();

            if (current != null && (!harvestable(current)
                    || eye.squaredDistanceTo(Vec3d.ofCenter(current)) > r * r)) current = null;
            if (current == null) {
                if (cd > 0) { cd--; return; }
                current = nearestHarvestable(r, eye);
            }
            if (current != null) {
                BlockState st = mc.world.getBlockState(current);
                Item seed = seedFor(st.getBlock());
                if (seed != null) lastSeed = seed;
                tries.remove(current.down());
                remember();
                Util.select(Util.bestTool(st));
                mc.interactionManager.updateBlockBreakingProgress(current, Direction.UP);
                mc.player.swingHand(Hand.MAIN_HAND);
                if (st.getHardness(mc.world, current) == 0f) { current = null; cd = delay.i(); }
                return;
            }
            if (replant.on() && plantOne(r, eye)) { cd = delay.i(); return; }
            restoreSlot();
        }
    }

    // ---- Nuker ----
    public static class Nuker extends Module {
        final Setting.Num range = num("Range", 4.0, 1.0, 5.0, 0.5);
        final Setting.Num delay = num("Delay", 0, 0, 10, 1);
        final Setting.Bool aboveFeet = bool("Above Feet Only", true);
        BlockPos current = null;
        int cd = 0, prevSlot = -1;

        public Nuker() { super("Nuker", Category.FARMING, "Breaks blocks around you (use with care!)"); }

        @Override protected void onDisable() {
            if (mc.player != null && prevSlot >= 0) Util.select(prevSlot);
            if (mc.interactionManager != null) mc.interactionManager.cancelBlockBreaking();
            prevSlot = -1; current = null; cd = 0;
        }

        boolean valid(BlockPos p, Vec3d eye) {
            BlockState st = mc.world.getBlockState(p);
            if (st.isAir() || !st.getFluidState().isEmpty() || st.getHardness(mc.world, p) < 0) return false;
            if (aboveFeet.on() && p.getY() < mc.player.getBlockY()) return false;
            double r = range.d();
            return eye.squaredDistanceTo(Vec3d.ofCenter(p)) <= r * r;
        }

        @Override public void onTick() {
            if (mc.currentScreen != null) return;
            Vec3d eye = mc.player.getEyePos();
            if (current != null && !valid(current, eye)) current = null;
            if (current == null) {
                if (cd > 0) { cd--; return; }
                BlockPos me = mc.player.getBlockPos();
                int ir = (int) Math.ceil(range.d());
                double bd = Double.MAX_VALUE;
                for (BlockPos p : BlockPos.iterate(me.add(-ir, -ir, -ir), me.add(ir, ir, ir))) {
                    double d = eye.squaredDistanceTo(Vec3d.ofCenter(p));
                    if (d >= bd || !valid(p, eye)) continue;
                    current = p.toImmutable(); bd = d;
                }
                if (current == null) {
                    if (prevSlot >= 0) Util.select(prevSlot);
                    prevSlot = -1;
                    return;
                }
            }
            BlockState st = mc.world.getBlockState(current);
            if (prevSlot < 0) prevSlot = mc.player.getInventory().getSelectedSlot();
            Util.select(Util.bestTool(st));
            mc.interactionManager.updateBlockBreakingProgress(current, Direction.UP);
            mc.player.swingHand(Hand.MAIN_HAND);
            if (st.getHardness(mc.world, current) == 0f) { current = null; cd = delay.i(); }
        }
    }
}
