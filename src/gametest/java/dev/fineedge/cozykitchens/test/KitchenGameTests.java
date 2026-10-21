package dev.fineedge.cozykitchens.test;

import dev.fineedge.cozykitchens.CozyKitchens;
import net.blay09.mods.cookingforblockheads.KitchenMultiBlock;
import net.blay09.mods.cookingforblockheads.api.capability.IKitchenItemProvider;
import net.blay09.mods.cookingforblockheads.block.FridgeBlock;
import net.blay09.mods.cookingforblockheads.block.ModBlocks;
import net.blay09.mods.cookingforblockheads.tile.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@GameTestHolder(CozyKitchens.ID)
@PrefixGameTestTemplate(false)
public final class KitchenGameTests {
    private static Block block(String path) {
        Block b = ForgeRegistries.BLOCKS.getValue(CozyKitchens.id(path));
        if (b == null || b == Blocks.AIR) throw new AssertionError("Missing variant: " + path);
        return b;
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void everyVariantHasValidNativeEntityAndReloads(GameTestHelper h) {
        int i = 0;
        for (CozyKitchens.Variant v : CozyKitchens.VARIANTS) {
            BlockPos p = new BlockPos(1 + i % 12, 1, 1 + i / 12);
            h.setBlock(p, v.block().get());
            BlockEntity entity = h.getBlockEntity(p);
            if (v.kind() != CozyKitchens.Kind.HANGING_CORNER) {
                h.assertTrue(entity != null, "Missing native entity for " + v.path());
                h.assertTrue(entity.getType().isValid(entity.getBlockState()), "Invalid entity type for " + v.path());
                CompoundTag saved = entity.saveWithFullMetadata();
                BlockEntity loaded = BlockEntity.loadStatic(h.absolutePos(p), entity.getBlockState(), saved);
                h.assertTrue(loaded != null && loaded.getClass() == entity.getClass(), "Reload failed for " + v.path());
            }
            List<ItemStack> drops = Block.getDrops(h.getBlockState(p), h.getLevel(), h.absolutePos(p), entity);
            h.assertTrue(drops.size() == 1 && drops.get(0).is(v.item().get()), "Wrong survival drop for " + v.path());
            i++;
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void storageAndUpgradeRoundTrip(GameTestHelper h) {
        for (String path : List.of("oak_counter", "cherry_cabinet", "sage_fridge")) {
            BlockPos p = new BlockPos(1, 1, 1);
            h.setBlock(p, block(path));
            BlockEntity entity = h.getBlockEntity(p);
            if (entity instanceof CounterBlockEntity counter) {
                counter.getContainer().setItem(0, new ItemStack(Items.CARROT, 19));
                counter.setCustomName(Component.literal("Pantry QA"));
                counter.setHasPreservationUpgrade(true);
                CounterBlockEntity copy = (CounterBlockEntity) BlockEntity.loadStatic(h.absolutePos(p), entity.getBlockState(), entity.saveWithFullMetadata());
                h.assertTrue(copy.getContainer().getItem(0).getCount() == 19, path + " lost storage");
                h.assertTrue(copy.hasPreservationUpgrade() && copy.getName().getString().equals("Pantry QA"), path + " lost upgrade/name");
            } else if (entity instanceof FridgeBlockEntity fridge) {
                fridge.getContainer().setItem(0, new ItemStack(Items.APPLE, 23));
                fridge.setHasIceUpgrade(true);
                fridge.setHasPreservationUpgrade(true);
                FridgeBlockEntity copy = (FridgeBlockEntity) BlockEntity.loadStatic(h.absolutePos(p), entity.getBlockState(), entity.saveWithFullMetadata());
                h.assertTrue(copy.getContainer().getItem(0).getCount() == 23 && copy.hasIceUpgrade() && copy.hasPreservationUpgrade(), "Fridge lost stored state");
            }
            h.setBlock(p, Blocks.AIR);
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void mixedKitchenCrossesBothCornerTypes(GameTestHelper h) {
        h.setBlock(new BlockPos(1, 1, 1), block("birch_cooking_table"));
        h.setBlock(new BlockPos(2, 1, 1), block("oak_counter"));
        h.setBlock(new BlockPos(3, 1, 1), block("spruce_corner"));
        h.setBlock(new BlockPos(4, 1, 1), ModBlocks.counter);
        h.setBlock(new BlockPos(5, 1, 1), block("warped_hanging_corner"));
        h.setBlock(new BlockPos(6, 1, 1), block("cherry_cabinet"));
        h.setBlock(new BlockPos(2, 1, 2), block("mangrove_sink"));
        h.setBlock(new BlockPos(3, 1, 2), block("ivory_oven"));
        h.setBlock(new BlockPos(4, 1, 2), block("sage_fridge"));
        h.runAfterDelay(3, () -> {
            KitchenMultiBlock kitchen = KitchenMultiBlock.buildFromLocation(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)));
            List<IKitchenItemProvider> providers = kitchen.getItemProviders(FakePlayerFactory.getMinecraft(h.getLevel()).getInventory());
            h.assertTrue(providers.size() == 7, "Kitchen should find 3 stores, sink, fridge, oven output/tools, and player; found " + providers.size());
            h.assertTrue(kitchen.hasSmeltingProvider(), "Kitchen did not recognize variant oven");
            ItemStack remaining = kitchen.smeltItem(new ItemStack(Items.POTATO, 3), 3);
            h.assertTrue(remaining.isEmpty(), "Native oven did not accept kitchen smelting request");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void fridgeStacksAndSurvivesEitherHalfRemoval(GameTestHelper h) {
        for (boolean removeUpper : new boolean[]{true, false}) {
            BlockPos bottom = new BlockPos(removeUpper ? 2 : 5, 1, 2);
            h.setBlock(bottom, block("sage_fridge"));
            h.setBlock(bottom.above(), block("sage_fridge"));
            h.assertTrue(h.getBlockState(bottom).getValue(FridgeBlock.MODEL_TYPE) == FridgeBlock.FridgeModelType.LARGE_LOWER, "Lower fridge model did not join");
            h.assertTrue(h.getBlockState(bottom.above()).getValue(FridgeBlock.MODEL_TYPE) == FridgeBlock.FridgeModelType.LARGE_UPPER, "Upper fridge model did not join");
            FridgeBlockEntity fridge = (FridgeBlockEntity) h.getBlockEntity(bottom);
            h.assertTrue(fridge.getCombinedContainer().getContainerSize() == 54, "Double fridge did not expose 54 slots");
            h.setBlock(removeUpper ? bottom.above() : bottom, Blocks.AIR);
            BlockPos survivor = removeUpper ? bottom : bottom.above();
            h.assertTrue(h.getBlockState(survivor).getValue(FridgeBlock.MODEL_TYPE) == FridgeBlock.FridgeModelType.SMALL, "Surviving fridge did not shrink");
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 500)
    public static void ovenActuallyCooks(GameTestHelper h) {
        BlockPos p = new BlockPos(2, 1, 2);
        h.setBlock(p, block("blush_oven"));
        OvenBlockEntity oven = (OvenBlockEntity) h.getBlockEntity(p);
        oven.getFuelContainer().setItem(0, new ItemStack(Items.COAL));
        oven.getInputContainer().setItem(0, new ItemStack(Items.POTATO));
        h.succeedWhen(() -> {
            boolean cooked = false;
            for (int i = 0; i < oven.getInternalContainer().getContainerSize(); i++)
                cooked |= oven.getInternalContainer().getItem(i).is(Items.BAKED_POTATO);
            h.assertTrue(cooked, "Variant oven has not produced baked potato");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void toasterReloadsAndCompletesCycle(GameTestHelper h) {
        BlockPos p = new BlockPos(2, 1, 2);
        h.setBlock(p, block("charcoal_toaster"));
        ToasterBlockEntity toaster = (ToasterBlockEntity) h.getBlockEntity(p);
        toaster.getContainer().setItem(0, new ItemStack(Items.BREAD));
        toaster.setActive(true);
        CompoundTag saved = toaster.saveWithFullMetadata();
        saved.putInt("ToastTicks", 2);
        toaster.load(saved);
        h.runAfterDelay(5, () -> {
            h.assertTrue(!toaster.isActive() && toaster.getContainer().isEmpty(), "Toaster failed to finish the restored cycle");
            h.assertTrue(!h.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new net.minecraft.world.phys.AABB(h.absolutePos(p)).inflate(3)).isEmpty(), "Toaster did not eject its result");
            h.succeed();
        });
    }
}
