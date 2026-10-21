package dev.fineedge.cozykitchens.client;

import dev.fineedge.cozykitchens.CozyKitchens;
import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.cookingforblockheads.client.ModModels;
import net.blay09.mods.cookingforblockheads.tile.ModBlockEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Variant materials and paired doors driven by the native CFB block entities. */
@Mod.EventBusSubscriber(modid = CozyKitchens.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KitchenModels {
    private KitchenModels() {}

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(CozyKitchens.id("block/empty"));
        for (String wood : CozyKitchens.WOODS) {
            if (wood.equals("pale_oak") && !ModList.get().isLoaded("vanillabackport")) continue;
            for (String part : new String[]{"counter_door_left", "counter_door_right", "cabinet_door_left", "cabinet_door_right", "sink_liquid"})
                event.register(CozyKitchens.id("block/" + wood + "_" + part));
        }
        for (String finish : CozyKitchens.FINISHES) {
            for (String part : new String[]{"oven_door", "oven_door_active", "oven_door_handle", "fridge_door",
                    "fridge_door_flipped", "fridge_large_door_lower", "fridge_large_door_lower_flipped",
                    "fridge_large_door_upper", "fridge_large_door_upper_flipped"})
                event.register(CozyKitchens.id("block/" + finish + "_" + part));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.counter.get(), PairedDoorRenderer::counter);
        event.registerBlockEntityRenderer(ModBlockEntities.cabinet.get(), PairedDoorRenderer::cabinet);
    }

    @SubscribeEvent
    public static void registerWaterColors(RegisterColorHandlersEvent.Block event) {
        // Same water blue as CFB 16.0.15's sink. Its registration covers only its own block.
        event.register((state, level, pos, tintIndex) -> 0x3f76e4,
                CozyKitchens.VARIANTS.stream().filter(v -> v.kind() == CozyKitchens.Kind.SINK)
                        .map(v -> v.block().get()).toArray(net.minecraft.world.level.block.Block[]::new));
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ModModels.sinkLiquid = route(ModModels.sinkLiquid, "sink_liquid");
            for (int i = 0; i < ModModels.ovenDoors.size(); i++) {
                ModModels.ovenDoors.set(i, route(ModModels.ovenDoors.get(i), "oven_door"));
                ModModels.ovenDoorsActive.set(i, route(ModModels.ovenDoorsActive.get(i), "oven_door_active"));
                ModModels.ovenDoorHandles.set(i, route(ModModels.ovenDoorHandles.get(i), "oven_door_handle"));
            }
            ModModels.fridgeDoor = route(ModModels.fridgeDoor, "fridge_door");
            ModModels.fridgeDoorFlipped = route(ModModels.fridgeDoorFlipped, "fridge_door_flipped");
            ModModels.fridgeDoorLargeLower = route(ModModels.fridgeDoorLargeLower, "fridge_large_door_lower");
            ModModels.fridgeDoorLargeLowerFlipped = route(ModModels.fridgeDoorLargeLowerFlipped, "fridge_large_door_lower_flipped");
            ModModels.fridgeDoorLargeUpper = route(ModModels.fridgeDoorLargeUpper, "fridge_large_door_upper");
            ModModels.fridgeDoorLargeUpperFlipped = route(ModModels.fridgeDoorLargeUpperFlipped, "fridge_large_door_upper_flipped");
        });
    }

    private static DeferredObject<BakedModel> route(DeferredObject<BakedModel> source, String part) {
        // The source resolves again on F3+T. Never retain a mesh from the previous texture atlas.
        return new DeferredObject<BakedModel>(source.getIdentifier(), source::get) {
            private BakedModel previous;
            private BakedModel routed;
            @Override public BakedModel get() {
                BakedModel current = source.get();
                if (current != previous) { previous = current; routed = new DoorModel(current, part); }
                return routed;
            }
        };
    }

    private static final class DoorModel extends BakedModelWrapper<BakedModel> {
        private final String part;
        DoorModel(BakedModel original, String part) { super(original); this.part = part; }
        private BakedModel select(@Nullable BlockState state) {
            CozyKitchens.Variant v = state == null ? null : CozyKitchens.variant(state.getBlock());
            return v == null ? originalModel : Minecraft.getInstance().getModelManager()
                    .getModel(CozyKitchens.id("block/" + v.finish() + "_" + part));
        }
        @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
            return select(state).getQuads(state, side, random);
        }
        @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                                 ModelData data, @Nullable RenderType renderType) {
            return select(state).getQuads(state, side, random, data, renderType);
        }
    }
}
