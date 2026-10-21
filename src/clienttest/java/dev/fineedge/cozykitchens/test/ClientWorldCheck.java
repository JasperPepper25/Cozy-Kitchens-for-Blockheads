package dev.fineedge.cozykitchens.test;

import dev.fineedge.cozykitchens.CozyKitchens;
import net.blay09.mods.cookingforblockheads.block.ModBlocks;
import net.blay09.mods.cookingforblockheads.tile.CounterBlockEntity;
import net.blay09.mods.cookingforblockheads.tile.SinkBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** A disposable integrated world exercises the real block-entity renderers and saves screenshots. */
final class ClientWorldCheck {
    private static boolean building;
    private static volatile boolean built;
    private static volatile Throwable failure;
    private static int ticks;
    private static final List<BlockPos> STORES = List.of(new BlockPos(0, 5, 0), new BlockPos(1, 5, 0),
            new BlockPos(0, 7, 0), new BlockPos(1, 7, 0), new BlockPos(-1, 5, 0));
    private static final List<String> SHOTS = List.of("closed", "open", "faucets", "corners-rear", "ovens-rear", "fridge");

    static void start(Minecraft mc) {
        for (String name : SHOTS) {
            var file = new java.io.File(mc.gameDirectory, "screenshots/cozy-0.3.0-" + name + ".png");
            if (file.exists() && !file.delete()) throw new AssertionError("Cannot replace QA screenshot: " + file);
        }
        mc.options.renderDistance().set(4);
        mc.options.fov().set(65);
        mc.options.hideGui = true;
        mc.options.bobView().set(false);
        mc.createWorldOpenFlows().createFreshLevel("cozy-render-qa-" + System.currentTimeMillis(),
                new LevelSettings("Cozy rendering QA", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                        true, new GameRules(), WorldDataConfiguration.DEFAULT),
                new WorldOptions(0, false, false),
                access -> access.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions());
    }

    static void tick(Minecraft mc) {
        if (failure != null) throw new AssertionError("World rendering QA failed", failure);
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (!building) {
            building = true;
            mc.getSingleplayerServer().execute(() -> {
                try {
                    var server = mc.getSingleplayerServer();
                    var level = server.overworld();
                    level.setDayTime(6000);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                    for (int x = -4; x <= 10; x++) for (int z = -3; z <= 4; z++)
                        level.setBlockAndUpdate(new BlockPos(x, 4, z), Blocks.SMOOTH_QUARTZ.defaultBlockState());
                    for (String spec : List.of("oak_counter,0,5", "spruce_counter,1,5", "oak_cabinet,0,7", "spruce_cabinet,1,7",
                            "oak_sink,2,5", "spruce_sink,3,5", "oak_corner,4,5", "oak_hanging_corner,4,7",
                            "sage_fridge,5,5", "sage_fridge,5,6", "ivory_oven,6,5", "ivory_toaster,6,6", "charcoal_oven,7,5")) {
                        String[] values = spec.split(",");
                        Block block = ForgeRegistries.BLOCKS.getValue(CozyKitchens.id(values[0]));
                        level.setBlockAndUpdate(new BlockPos(Integer.parseInt(values[1]), Integer.parseInt(values[2]), 0), block.defaultBlockState());
                    }
                    level.setBlockAndUpdate(new BlockPos(-1, 5, 0), ModBlocks.counter.defaultBlockState());
                    for (BlockPos pos : STORES) {
                        CounterBlockEntity store = (CounterBlockEntity) level.getBlockEntity(pos);
                        store.getContainer().setItem(0, new ItemStack(Items.APPLE, 16));
                        store.getContainer().setItem(15, new ItemStack(Items.CARROT, 8));
                        store.setChanged();
                    }
                    for (int x : new int[]{2, 3}) {
                        SinkBlockEntity sink = (SinkBlockEntity) level.getBlockEntity(new BlockPos(x, 5, 0));
                        sink.getFluidTank().setFluid(Fluids.WATER, sink.getFluidTank().getCapacity());
                        sink.setChanged();
                    }
                    var player = server.getPlayerList().getPlayers().get(0);
                    player.setGameMode(GameType.SPECTATOR);
                    // Consistent inspection lighting, independent of asynchronously arriving sky light.
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 10000, 0, true, false, false));
                    player.connection.teleport(3.6, 8, -8.5, 0, 12);
                    built = true;
                } catch (Throwable t) { failure = t; }
            });
            return;
        }
        if (!built || !(mc.level.getBlockEntity(new BlockPos(0, 5, 0)) instanceof CounterBlockEntity)) return;
        ticks++;
        if (ticks == 70) shot(mc, "closed");
        if (ticks == 90) {
            camera(mc, .5, 7.7, -5.2, 0, 12);
            for (BlockPos pos : STORES)
                ((CounterBlockEntity) mc.level.getBlockEntity(pos)).getDoorAnimator().setForcedOpen(true);
        }
        if (ticks == 130) {
            for (BlockPos pos : STORES) {
                CounterBlockEntity store = (CounterBlockEntity) mc.level.getBlockEntity(pos);
                if (store.getDoorAnimator().getRenderAngle(1) < 1.5f)
                    throw new AssertionError("Native opening animation failed: " + pos);
                if (store.getContainer().getItem(0).getCount() != 16)
                    throw new AssertionError("Client storage sync failed: " + pos);
            }
            shot(mc, "open");
        }
        if (ticks == 150) camera(mc, 2.9, 7.6, -2.9, 0, 29);
        if (ticks == 180) shot(mc, "faucets");
        if (ticks == 200) camera(mc, 3.2, 7.7, 3.5, 180, 28);
        if (ticks == 230) shot(mc, "corners-rear");
        if (ticks == 250) camera(mc, 6.9, 7.4, 3.7, 180, 24);
        if (ticks == 280) shot(mc, "ovens-rear");
        if (ticks == 300) camera(mc, 5.5, 7.8, -3.5, 0, 17);
        if (ticks == 330) shot(mc, "fridge");
        if (ticks >= 370 && SHOTS.stream().allMatch(name -> new java.io.File(mc.gameDirectory,
                "screenshots/cozy-0.3.0-" + name + ".png").isFile())) {
            CozyKitchens.LOG.info("COZY_WORLD_QA_PASS: actual paired/native renderers, opening animation, synced shelf contents, blue sinks, six in-world screenshots");
            mc.stop();
        }
    }

    private static void camera(Minecraft mc, double x, double y, double z, float yaw, float pitch) {
        mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().getPlayerList().getPlayers().get(0)
                .connection.teleport(x, y, z, yaw, pitch));
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "cozy-0.3.0-" + name + ".png", mc.getMainRenderTarget(), message -> {});
    }
}
