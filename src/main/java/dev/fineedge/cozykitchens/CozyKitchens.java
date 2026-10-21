package dev.fineedge.cozykitchens;

import com.mojang.logging.LogUtils;
import net.blay09.mods.cookingforblockheads.KitchenMultiBlock;
import net.blay09.mods.cookingforblockheads.block.ModBlocks;
import net.blay09.mods.cookingforblockheads.tile.ModBlockEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Supplier;

/** Additive variants: the original CFB block entities own all functional state. */
@Mod(CozyKitchens.ID)
public final class CozyKitchens {
    public static final String ID = "cozykitchens";
    public static final Logger LOG = LogUtils.getLogger();
    public static final String[] WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "bamboo", "crimson", "warped", "pale_oak"};
    public static final String[] FINISHES = {"ivory", "sage", "blush", "charcoal"};
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(RegistriesKey.TABS, ID);
    public static final List<Variant> VARIANTS = new ArrayList<>();
    private static final Map<Block, Variant> BY_BLOCK = new IdentityHashMap<>();

    public enum Kind {
        COUNTER("counter", KitchenVariants.Counter::new, () -> ModBlockEntities.counter.get()),
        CABINET("cabinet", KitchenVariants.Cabinet::new, () -> ModBlockEntities.cabinet.get()),
        COOKING_TABLE("cooking_table", KitchenVariants.CookingTable::new, () -> ModBlockEntities.cookingTable.get()),
        SINK("sink", KitchenVariants.Sink::new, () -> ModBlockEntities.sink.get()),
        CORNER("corner", KitchenVariants.Corner::new, () -> ModBlockEntities.corner.get()),
        HANGING_CORNER("hanging_corner", KitchenVariants.HangingCorner::new, () -> null),
        OVEN("oven", KitchenVariants.Oven::new, () -> ModBlockEntities.oven.get()),
        FRIDGE("fridge", KitchenVariants.Fridge::new, () -> ModBlockEntities.fridge.get()),
        TOASTER("toaster", KitchenVariants.Toaster::new, () -> ModBlockEntities.toaster.get());

        public final String path;
        final Supplier<Block> factory;
        final Supplier<BlockEntityType<?>> entityType;
        Kind(String path, Supplier<Block> factory, Supplier<BlockEntityType<?>> entityType) {
            this.path = path; this.factory = factory; this.entityType = entityType;
        }
    }

    public record Variant(String finish, Kind kind, RegistryObject<Block> block, RegistryObject<Item> item) {
        public String path() { return finish + "_" + kind.path; }
    }

    public CozyKitchens() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        for (String wood : WOODS) {
            if (wood.equals("pale_oak") && !ModList.get().isLoaded("vanillabackport")) continue;
            for (Kind kind : Kind.values()) {
                if (kind.ordinal() <= Kind.HANGING_CORNER.ordinal()) register(wood, kind);
            }
        }
        for (String finish : FINISHES) {
            for (Kind kind : new Kind[]{Kind.OVEN, Kind.FRIDGE, Kind.TOASTER}) register(finish, kind);
        }
        TABS.register("kitchen", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.cozykitchens"))
                .icon(() -> new ItemStack(VARIANTS.get(0).item().get()))
                .displayItems((parameters, output) -> VARIANTS.forEach(v -> output.accept(v.item().get())))
                .build());
        BLOCKS.register(bus); ITEMS.register(bus); TABS.register(bus);
        bus.addListener(this::commonSetup);
        bus.addListener(this::addOptionalData);
    }

    private static void register(String finish, Kind kind) {
        String path = finish + "_" + kind.path;
        RegistryObject<Block> block = BLOCKS.register(path, kind.factory);
        RegistryObject<Item> item = ITEMS.register(path, () -> new BlockItem(block.get(), new Item.Properties()));
        VARIANTS.add(new Variant(finish, kind, block, item));
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Map<BlockEntityType<?>, Set<Block>> additions = new IdentityHashMap<>();
            for (Variant v : VARIANTS) {
                Block block = v.block().get();
                BY_BLOCK.put(block, v);
                BlockEntityType<?> type = v.kind().entityType.get();
                if (type != null) additions.computeIfAbsent(type, t -> new HashSet<>(t.validBlocks)).add(block);
                else KitchenMultiBlock.registerConnectorBlock(block);
            }
            // Merge once per type, retaining the original blocks and other add-ons' registrations.
            additions.forEach((type, blocks) -> type.validBlocks = Set.copyOf(blocks));
            LOG.info("Cozy Kitchens: {} variants registered; native Cooking for Blockheads entities enabled", VARIANTS.size());
        });
    }

    public static Variant variant(Block block) { return BY_BLOCK.get(block); }
    public static ResourceLocation id(String path) { return new ResourceLocation(ID, path); }

    private void addOptionalData(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA || !ModList.get().isLoaded("vanillabackport")) return;
        var path = ModList.get().getModFileById(ID).getFile().findResource("resourcepacks", "pale_oak");
        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate(ID + ":pale_oak", Component.literal("Cozy Kitchens — Pale Oak"),
                    true, packId -> new PathPackResources(packId, path, true), PackType.SERVER_DATA,
                    Pack.Position.TOP, PackSource.BUILT_IN);
            if (pack != null) consumer.accept(pack);
        });
    }

    /** Keeps the creative registry key in common code without a client class reference. */
    private static final class RegistriesKey {
        static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<CreativeModeTab>> TABS =
                net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB;
    }
}
