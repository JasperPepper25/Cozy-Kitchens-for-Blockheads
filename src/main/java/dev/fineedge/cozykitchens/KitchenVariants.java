package dev.fineedge.cozykitchens;

import net.blay09.mods.cookingforblockheads.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.blay09.mods.cookingforblockheads.tile.ToasterBlockEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;

/** Native interaction, ticking, capabilities, menus, drops, and upgrade behavior are inherited. */
public final class KitchenVariants {
    private KitchenVariants() {}

    public static final class Counter extends CounterBlock {
        @Override protected boolean isDyeable() { return false; }
        @Override public boolean tryRecolorBlock(BlockState s, ItemStack i, Level l, BlockPos p, Player u, BlockHitResult h) { return false; }
    }
    public static final class Cabinet extends CabinetBlock {
        @Override protected boolean isDyeable() { return false; }
        @Override public boolean tryRecolorBlock(BlockState s, ItemStack i, Level l, BlockPos p, Player u, BlockHitResult h) { return false; }
    }
    public static final class CookingTable extends CookingTableBlock {
        @Override protected boolean isDyeable() { return false; }
        @Override public boolean tryRecolorBlock(BlockState s, ItemStack i, Level l, BlockPos p, Player u, BlockHitResult h) { return false; }
    }
    public static final class Sink extends SinkBlock {
        @Override protected boolean isDyeable() { return false; }
        @Override public boolean tryRecolorBlock(BlockState s, ItemStack i, Level l, BlockPos p, Player u, BlockHitResult h) { return false; }
    }
    public static final class Corner extends CornerBlock {
        @Override protected boolean isDyeable() { return false; }
        @Override public boolean tryRecolorBlock(BlockState s, ItemStack i, Level l, BlockPos p, Player u, BlockHitResult h) { return false; }
    }
    public static final class HangingCorner extends HangingCornerBlock {
        @Override protected boolean isDyeable() { return false; }
        @Override public boolean tryRecolorBlock(BlockState s, ItemStack i, Level l, BlockPos p, Player u, BlockHitResult h) { return false; }
    }
    public static final class Oven extends OvenBlock {}
    public static final class Toaster extends ToasterBlock {
        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                     InteractionHand hand, BlockHitResult hit) {
            BlockEntity entity = level.getBlockEntity(pos);
            boolean wasActive = entity instanceof ToasterBlockEntity toaster && toaster.isActive();
            InteractionResult result = super.use(state, level, pos, player, hand, hit);
            if (!level.isClientSide && entity instanceof ToasterBlockEntity toaster && toaster.isActive() != wasActive)
                relayEvents(level, pos, toaster.isActive());
            return result;
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            BlockEntityTicker<T> nativeTicker = super.getTicker(level, state, type);
            if (nativeTicker == null) return null;
            return (world, pos, current, entity) -> {
                boolean wasActive = ((ToasterBlockEntity) entity).isActive();
                nativeTicker.tick(world, pos, current, entity);
                boolean active = ((ToasterBlockEntity) entity).isActive();
                if (wasActive != active) relayEvents(world, pos, active);
            };
        }

        private void relayEvents(Level level, BlockPos pos, boolean active) {
            // Native CFB targets its original toaster block for these events.
            level.blockEvent(pos, this, active ? 0 : 1, 0);
            level.blockEvent(pos, this, 2, 0);
        }
    }

    public static final class Fridge extends FridgeBlock {
        @Override protected boolean isDyeable() { return false; }
        @Override public boolean tryRecolorBlock(BlockState s, ItemStack i, Level l, BlockPos p, Player u, BlockHitResult h) { return false; }

        @Override
        public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                      LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
            BlockState below = level.getBlockState(pos.below());
            BlockState above = level.getBlockState(pos.above());
            if (below.getBlock() instanceof FridgeBlock) {
                alignNeighbor(level, pos.below(), below, FridgeModelType.LARGE_LOWER, state);
                return state.setValue(MODEL_TYPE, FridgeModelType.LARGE_UPPER);
            }
            if (above.getBlock() instanceof FridgeBlock) {
                alignNeighbor(level, pos.above(), above, FridgeModelType.LARGE_UPPER, state);
                return state.setValue(MODEL_TYPE, FridgeModelType.LARGE_LOWER);
            }
            return state.setValue(MODEL_TYPE, FridgeModelType.SMALL);
        }

        private static void alignNeighbor(LevelAccessor level, BlockPos pos, BlockState neighbor,
                                          FridgeModelType model, BlockState current) {
            BlockState aligned = neighbor.setValue(MODEL_TYPE, model).setValue(FACING, current.getValue(FACING));
            if (aligned != neighbor) level.setBlock(pos, aligned, Block.UPDATE_ALL);
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
            super.onRemove(state, level, pos, replacement, moving);
            if (!state.is(replacement.getBlock())) {
                for (BlockPos neighbor : new BlockPos[]{pos.above(), pos.below()}) {
                    BlockState other = level.getBlockState(neighbor);
                    if (other.getBlock() instanceof FridgeBlock && other.getValue(MODEL_TYPE) != FridgeModelType.SMALL)
                        level.setBlock(neighbor, other.setValue(MODEL_TYPE, FridgeModelType.SMALL), Block.UPDATE_ALL);
                }
            }
        }
    }
}
