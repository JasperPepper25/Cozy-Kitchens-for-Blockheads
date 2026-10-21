package dev.fineedge.cozykitchens.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.fineedge.cozykitchens.CozyKitchens;
import net.blay09.mods.cookingforblockheads.client.render.CabinetRenderer;
import net.blay09.mods.cookingforblockheads.client.render.CounterRenderer;
import net.blay09.mods.cookingforblockheads.client.render.RenderUtils;
import net.blay09.mods.cookingforblockheads.tile.CabinetBlockEntity;
import net.blay09.mods.cookingforblockheads.tile.CounterBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

/** Two hinges for our cabinets; native renderers retain contents and original-block behaviour. */
public final class PairedDoorRenderer<T extends CounterBlockEntity> implements BlockEntityRenderer<T> {
    private final BlockEntityRenderer<T> original;
    private final BlockEntityRenderer<T> contents;
    private final boolean hanging;
    private final RandomSource random = RandomSource.create(42);

    private PairedDoorRenderer(BlockEntityRenderer<T> original, BlockEntityRenderer<T> contents, boolean hanging) {
        this.original = original;
        this.contents = contents;
        this.hanging = hanging;
    }

    public static PairedDoorRenderer<CounterBlockEntity> counter(BlockEntityRendererProvider.Context context) {
        return new PairedDoorRenderer<>(new CounterRenderer<>(context), new CounterRenderer<>(context) {
            @Override protected BakedModel getDoorModel(@Nullable DyeColor color, boolean flipped) { return emptyDoor(); }
        }, false);
    }

    public static PairedDoorRenderer<CabinetBlockEntity> cabinet(BlockEntityRendererProvider.Context context) {
        return new PairedDoorRenderer<>(new CabinetRenderer(context), new CabinetRenderer(context) {
            @Override protected BakedModel getDoorModel(@Nullable DyeColor color, boolean flipped) { return emptyDoor(); }
        }, true);
    }

    private static BakedModel emptyDoor() {
        return Minecraft.getInstance().getModelManager().getModel(CozyKitchens.id("block/empty"));
    }

    /** Model-space hinge at the outer rear edge of each leaf; native angle is in radians. */
    public static void applyDoorPose(PoseStack pose, boolean hanging, boolean right, float angle) {
        float x = (right ? 15.25f : .75f) / 16f;
        float z = (hanging ? 3.75f : 1.5f) / 16f;
        pose.translate(x, 0, z);
        pose.mulPose(Axis.YP.rotation(right ? -angle : angle));
        pose.translate(-x, 0, -z);
    }

    @Override
    public void render(T entity, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        CozyKitchens.Variant variant = CozyKitchens.variant(entity.getBlockState().getBlock());
        if (variant == null) {
            original.render(entity, partialTick, pose, buffer, light, overlay);
            return;
        }
        if (entity.getLevel() == null) return;
        // The native renderer draws shelf items and observes CFB's usual animation/configuration.
        contents.render(entity, partialTick, pose, buffer, light, overlay);
        float angle = entity.getDoorAnimator().getRenderAngle(partialTick);
        var dispatcher = Minecraft.getInstance().getBlockRenderer();
        for (int leaf = 0; leaf < 2; leaf++) {
            boolean right = leaf == 1;
            String part = (hanging ? "cabinet" : "counter") + "_door_" + (right ? "right" : "left");
            BakedModel model = Minecraft.getInstance().getModelManager()
                    .getModel(CozyKitchens.id("block/" + variant.finish() + "_" + part));
            pose.pushPose();
            RenderUtils.applyBlockAngle(pose, entity.getBlockState());
            pose.translate(-.5, 0, -.5);
            applyDoorPose(pose, hanging, right, angle);
            dispatcher.getModelRenderer().tesselateBlock(entity.getLevel(), model, entity.getBlockState(),
                    entity.getBlockPos(), pose, buffer.getBuffer(RenderType.solid()), false,
                    random, 0, overlay);
            pose.popPose();
        }
    }
}
