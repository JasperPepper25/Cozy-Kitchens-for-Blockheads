package dev.fineedge.cozykitchens.test;

import dev.fineedge.cozykitchens.CozyKitchens;
import dev.fineedge.cozykitchens.client.PairedDoorRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
import net.blay09.mods.cookingforblockheads.client.ModModels;
import net.blay09.mods.cookingforblockheads.block.ModBlocks;
import net.blay09.mods.cookingforblockheads.block.BlockKitchen;
import net.blay09.mods.cookingforblockheads.block.CounterBlock;
import net.blay09.mods.cookingforblockheads.client.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
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
        if (event.phase != TickEvent.Phase.END) return;
        if (done) { ClientWorldCheck.tick(mc); return; }
        if (mc.getOverlay() != null || mc.screen == null) return;
        if (reloadStage == 1) return;
        if (reloadError != null) throw new AssertionError("Resource reload failed", reloadError);
        int checked = 0;
        for (CozyKitchens.Variant v : CozyKitchens.VARIANTS) {
            BlockState state = v.block().get().defaultBlockState();
            verify(mc.getBlockRenderer().getBlockModel(state), state, v.path() + " body", true);
            verify(mc.getItemRenderer().getModel(new ItemStack(v.item().get()), null, null, 0), null, v.path() + " item", true);
            List<BakedModel> doors = switch (v.kind()) {
                case COUNTER, CABINET -> List.of("left", "right").stream().map(half -> mc.getModelManager()
                        .getModel(CozyKitchens.id("block/" + v.finish() + "_" + v.kind().path + "_door_" + half))).toList();
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
                        : CozyKitchens.id("block/ceramic/" + v.finish());
                boolean finishFound = quads(door, state).stream().anyMatch(q -> q.getSprite().contents().name()
                        .equals(expected));
                if (!finishFound) throw new AssertionError("Wrong animated door finish: " + v.path());
            }
            if (v.kind() == CozyKitchens.Kind.COUNTER || v.kind() == CozyKitchens.Kind.CABINET) {
                var entity = ((EntityBlock) state.getBlock()).newBlockEntity(BlockPos.ZERO, state);
                if (!(mc.getBlockEntityRenderDispatcher().getRenderer(entity) instanceof PairedDoorRenderer<?>))
                    throw new AssertionError("Paired renderer was replaced: " + v.path());
                verifyHinges(state, v.kind() == CozyKitchens.Kind.CABINET);
            }
            if (v.kind() == CozyKitchens.Kind.SINK) {
                verify(ModModels.sinkLiquid.get(), state, v.path() + " water", true);
                BakedModel expected = mc.getModelManager().getModel(CozyKitchens.id("block/" + v.finish() + "_sink_liquid"));
                if (!quads(expected, state).equals(quads(ModModels.sinkLiquid.get(), state)))
                    throw new AssertionError("Wrong sink water model: " + v.path());
                int nativeBlue = mc.getBlockColors().getColor(ModBlocks.sink.defaultBlockState(), null, null, 0);
                if (nativeBlue != 0x3f76e4 || mc.getBlockColors().getColor(state, null, null, 0) != nativeBlue)
                    throw new AssertionError("Missing sink water tint: " + v.path());
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
        CozyKitchens.LOG.info("COZY_MODEL_QA_PASS: {} variants; body/item meshes, ceramic finishes, atlas UV bounds, paired hinges, renderer registration, gold/black faucets, blue water tint, native fallbacks and resource reload verified", checked);
        ClientWorldCheck.start(mc);
    }

    private static void verifyHinges(BlockState state, boolean hanging) {
        for (Direction facing : Direction.Plane.HORIZONTAL) for (boolean flipped : new boolean[]{false, true}) {
            BlockState placed = state.setValue(BlockKitchen.FACING, facing).setValue(CounterBlock.FLIPPED, flipped);
            for (boolean right : new boolean[]{false, true}) {
                float hingeX = (right ? 15.25f : .75f) / 16;
                float hingeZ = (hanging ? 3.75f : 1.5f) / 16;
                for (int degrees : new int[]{0, 30, 60, 90}) {
                    PoseStack pose = new PoseStack();
                    PairedDoorRenderer.applyDoorPose(pose, hanging, right, (float) Math.toRadians(degrees));
                    Vector3f hinge = new Vector3f(hingeX, .5f, hingeZ);
                    Vector3f actual = pose.last().pose().transformPosition(new Vector3f(hinge));
                    if (actual.distance(hinge) > 1e-6) throw new AssertionError("Door hinge moves");
                    Vector3f freeEdge = pose.last().pose().transformPosition(new Vector3f(.5f, .5f, hingeZ));
                    if (degrees > 0 && freeEdge.z >= hingeZ) throw new AssertionError("Door swings into cabinet");
                    if (degrees == 90 && Math.abs(freeEdge.x - hingeX) > 1e-6)
                        throw new AssertionError("Door does not open fully");
                    PoseStack oriented = new PoseStack();
                    RenderUtils.applyBlockAngle(oriented, placed);
                    oriented.translate(-.5, 0, -.5);
                    Vector3f closedHinge = oriented.last().pose().transformPosition(new Vector3f(hinge));
                    PairedDoorRenderer.applyDoorPose(oriented, hanging, right, (float) Math.toRadians(degrees));
                    Vector3f openHinge = oriented.last().pose().transformPosition(new Vector3f(hinge));
                    Vector3f openEdge = oriented.last().pose().transformPosition(new Vector3f(.5f, .5f, hingeZ));
                    if (openHinge.distance(closedHinge) > 1e-6)
                        throw new AssertionError("Placed hinge moves: " + facing + "/" + flipped);
                    if (degrees > 0 && new Vector3f(openEdge).sub(openHinge).dot(facing.step()) <= 0)
                        throw new AssertionError("Placed door swings into cabinet: " + facing + "/" + flipped);
                }
            }
        }
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
            var sprite = quad.getSprite();
            int[] vertices = quad.getVertices();
            int stride = vertices.length / 4;
            for (int i = 0; i < 4; i++) {
                float u = Float.intBitsToFloat(vertices[i * stride + 4]);
                float v = Float.intBitsToFloat(vertices[i * stride + 5]);
                if (u < sprite.getU0() - 1e-6 || u > sprite.getU1() + 1e-6 ||
                        v < sprite.getV0() - 1e-6 || v > sprite.getV1() + 1e-6)
                    throw new AssertionError("UV leaves texture atlas sprite in " + label);
            }
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
