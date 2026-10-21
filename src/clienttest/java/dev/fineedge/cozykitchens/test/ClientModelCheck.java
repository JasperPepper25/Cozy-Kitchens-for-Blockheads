package dev.fineedge.cozykitchens.test;

import dev.fineedge.cozykitchens.CozyKitchens;
import net.blay09.mods.cookingforblockheads.client.ModModels;
import net.blay09.mods.cookingforblockheads.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Development-only headless client smoke check; never included in the release JAR. */
@Mod.EventBusSubscriber(modid = CozyKitchens.ID, value = Dist.CLIENT)
public final class ClientModelCheck {
    private static boolean done;
    private static volatile int reloadStage;
    private static volatile Throwable reloadError;
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (done || event.phase != TickEvent.Phase.END || mc.getOverlay() != null || mc.screen == null) return;
        if (reloadStage == 1) return;
        if (reloadError != null) throw new AssertionError("Resource reload failed", reloadError);
        int checked = 0;
        for (CozyKitchens.Variant v : CozyKitchens.VARIANTS) {
            BlockState state = v.block().get().defaultBlockState();
            verify(mc.getBlockRenderer().getBlockModel(state), state, v.path() + " body", true);
            verify(mc.getItemRenderer().getModel(new ItemStack(v.item().get()), null, null, 0), null, v.path() + " item", true);
            List<BakedModel> doors = switch (v.kind()) {
                case COUNTER -> List.of(ModModels.counterDoors.get(0).get(), ModModels.counterDoorsFlipped.get(0).get());
                case CABINET -> List.of(ModModels.cabinetDoors.get(0).get(), ModModels.cabinetDoorsFlipped.get(0).get());
                case OVEN -> List.of(ModModels.ovenDoors.get(0).get(), ModModels.ovenDoorsActive.get(0).get());
                case FRIDGE -> List.of(ModModels.fridgeDoor.get(), ModModels.fridgeDoorFlipped.get(),
                        ModModels.fridgeDoorLargeLower.get(), ModModels.fridgeDoorLargeUpper.get(),
                        ModModels.fridgeDoorLargeLowerFlipped.get(), ModModels.fridgeDoorLargeUpperFlipped.get());
                default -> List.of();
            };
            for (BakedModel door : doors) {
                verify(door, state, v.path() + " animated door", true);
                ResourceLocation expected = v.kind() == CozyKitchens.Kind.COUNTER || v.kind() == CozyKitchens.Kind.CABINET
                        ? CozyKitchens.id("block/cabinet/" + v.finish() + "_cabinet_front")
                        : new ResourceLocation("minecraft", "block/" + Map.of("ivory", "white_terracotta", "sage", "green_terracotta",
                        "blush", "pink_terracotta", "charcoal", "gray_terracotta").get(v.finish()));
                boolean finishFound = quads(door, state).stream().anyMatch(q -> q.getSprite().contents().name()
                        .equals(expected));
                if (!finishFound) throw new AssertionError("Wrong animated door finish: " + v.path());
            }
            if (v.kind() == CozyKitchens.Kind.SINK) {
                verify(ModModels.sinkLiquid.get(), state, v.path() + " water", true);
                BakedModel expected = mc.getModelManager().getModel(CozyKitchens.id("block/" + v.finish() + "_sink_liquid"));
                if (!quads(expected, state).equals(quads(ModModels.sinkLiquid.get(), state)))
                    throw new AssertionError("Wrong sink water model: " + v.path());
                String faucet = Set.of("spruce", "jungle", "dark_oak", "mangrove", "cherry", "crimson")
                        .contains(v.finish()) ? "gold_block" : "black_concrete";
                requireSprite(mc.getBlockRenderer().getBlockModel(state), state,
                        new ResourceLocation("minecraft", "block/" + faucet), v.path() + " faucet");
            }
            if (v.finish().equals("pale_oak")) {
                for (String part : List.of("side", "top")) {
                    requireSprite(mc.getBlockRenderer().getBlockModel(state), state,
                            CozyKitchens.id("block/cabinet/pale_oak_cabinet_" + part), v.path() + " " + part);
                }
            }
            checked++;
        }
        BlockState nativeState = ModBlocks.counter.defaultBlockState();
        BakedModel original = mc.getModelManager().getModel(new ResourceLocation("cookingforblockheads", "block/counter_door"));
        if (!quads(original, nativeState).equals(quads(ModModels.counterDoors.get(0).get(), nativeState)))
            throw new AssertionError("Original CFB door changed");
        BlockState nativeSink = ModBlocks.sink.defaultBlockState();
        BakedModel originalWater = mc.getModelManager().getModel(new ResourceLocation("cookingforblockheads", "block/sink_liquid"));
        if (!quads(originalWater, nativeSink).equals(quads(ModModels.sinkLiquid.get(), nativeSink)))
            throw new AssertionError("Original CFB sink water changed");
        if (reloadStage == 0) {
            reloadStage = 1;
            CozyKitchens.LOG.info("COZY_MODEL_QA_INITIAL_PASS: {} variants; reloading resource packs", checked);
            mc.reloadResourcePacks().whenComplete((unused, failure) -> { reloadError = failure; reloadStage = 2; });
            return;
        }
        done = true;
        CozyKitchens.LOG.info("COZY_MODEL_QA_PASS: {} variants, body/item meshes, animated finishes, gold/black faucets, custom pale oak side/end sprites, sink water, native fallbacks and resource reload verified", checked);
        mc.stop();
    }

    private static void requireSprite(BakedModel model, BlockState state, ResourceLocation sprite, String label) {
        if (quads(model, state).stream().noneMatch(q -> q.getSprite().contents().name().equals(sprite)))
            throw new AssertionError("Missing expected sprite in " + label + ": " + sprite);
    }

    private static void verify(BakedModel model, BlockState state, String label, boolean nonempty) {
        List<BakedQuad> quads = quads(model, state);
        if (nonempty && quads.isEmpty()) throw new AssertionError("No quads in " + label);
        for (BakedQuad quad : quads) {
            if (quad.getSprite().contents().name().getPath().equals("missingno"))
                throw new AssertionError("Missing sprite in " + label);
        }
    }

    private static List<BakedQuad> quads(BakedModel model, BlockState state) {
        List<BakedQuad> quads = new ArrayList<>();
        RandomSource random = RandomSource.create(42);
        quads.addAll(model.getQuads(state, null, random, ModelData.EMPTY, null));
        for (Direction side : Direction.values()) quads.addAll(model.getQuads(state, side, random, ModelData.EMPTY, null));
        return quads;
    }
}
