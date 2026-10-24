package org.abstruck.chickens.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.block.breedingbox.BreedingBoxBlock;
import org.abstruck.chickens.block.breedingbox.BreedingBoxBlockEntity;
import org.abstruck.chickens.block.breedingbox.BreedingBoxMenu;
import org.abstruck.chickens.block.grower.GrowerBlock;
import org.abstruck.chickens.block.grower.GrowerBlockEntity;
import org.abstruck.chickens.block.grower.GrowerMenu;
import org.abstruck.chickens.block.nest.NestBlock;
import org.abstruck.chickens.block.nest.NestBlockEntity;
import org.abstruck.chickens.block.nest.NestMenu;
import org.abstruck.chickens.breed.BreedLookups;
import org.abstruck.chickens.breed.ChickenBreed;
import org.abstruck.chickens.breed.ChickenData;
import org.abstruck.chickens.breed.FluidEggEntry;
import org.abstruck.chickens.entity.ThrownDyeEgg;
import org.abstruck.chickens.entity.chicken.ResourceChicken;
import org.abstruck.chickens.item.AnalyzerItem;
import org.abstruck.chickens.item.ChickenCatcherItem;
import org.abstruck.chickens.item.ChickenItem;
import org.abstruck.chickens.item.DyeEggColors;
import org.abstruck.chickens.item.DyeEggItem;
import org.abstruck.chickens.item.FluidEggItem;
import org.abstruck.chickens.item.ItemData;
import org.abstruck.chickens.world.AddSpawnsToCategoryBiomeModifier;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 全部注册对象（1.20.1 使用 Forge 的 {@link DeferredRegister}；没有 NeoForge 的
 * {@code DeferredBlock}/{@code DeferredItem} 子类型，统一用 {@code RegistryObject}）。
 */
public final class ModRegistry {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Chickens.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Chickens.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Chickens.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Chickens.MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Chickens.MODID);
    public static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, Chickens.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Chickens.MODID);

    // ------------------------------------------------------------------ 方块
    /** noOcclusion：模型是镂空结构，不透光会把方块内照暗——noOcclusion 让光穿过（碰撞箱不变） */
    public static final RegistryObject<Block> BREEDING_BOX =
            BLOCKS.register("breeding_box", () -> new BreedingBoxBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion()));

    public static final RegistryObject<Block> GROWER =
            BLOCKS.register("grower", () -> new GrowerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion()));

    public static final RegistryObject<Block> NEST =
            BLOCKS.register("nest", () -> new NestBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion()));

    // ------------------------------------------------------------------ 物品
    /** 鸡物品：品种存 NBT，右键放置生成对应资源鸡 */
    public static final RegistryObject<Item> CHICKEN =
            ITEMS.register("chicken", () -> new ChickenItem(new Item.Properties().stacksTo(16)));

    /** 鸡捕手：右键把鸡变成鸡物品（原版鸡按默认品种捕获）；128 点耐久，每次捕获消耗 1 点 */
    public static final RegistryObject<Item> CATCHER =
            ITEMS.register("catcher", () -> new ChickenCatcherItem(new Item.Properties().durability(128)));

    public static final RegistryObject<Item> BREEDING_BOX_ITEM =
            ITEMS.register("breeding_box", () -> new BlockItem(BREEDING_BOX.get(), new Item.Properties()));

    public static final RegistryObject<Item> GROWER_ITEM =
            ITEMS.register("grower", () -> new BlockItem(GROWER.get(), new Item.Properties()));

    public static final RegistryObject<Item> NEST_ITEM =
            ITEMS.register("nest", () -> new BlockItem(NEST.get(), new Item.Properties()));

    /** 鸡分析器：右键鸡显示品种/属性/下次产出时间 */
    public static final RegistryObject<Item> CHICKEN_ANALYZER =
            ITEMS.register("chicken_analyzer", () -> new AnalyzerItem(new Item.Properties()));

    /** 流体蛋：流体存 NBT，右键倒出；颜色由 {@code chickens:fluid_egg} 数据定义 */
    public static final RegistryObject<Item> FLUID_EGG =
            ITEMS.register("fluid_egg", () -> new FluidEggItem(new Item.Properties().stacksTo(64)));

    /** 染料蛋：投掷命中孵出对应染料鸡 */
    public static final RegistryObject<Item> DYE_EGG =
            ITEMS.register("dye_egg", () -> new DyeEggItem(new Item.Properties().stacksTo(16)));

    // ------------------------------------------------------------------ 方块实体
    public static final RegistryObject<BlockEntityType<BreedingBoxBlockEntity>> BREEDING_BOX_BE =
            BLOCK_ENTITIES.register("breeding_box",
                    () -> BlockEntityType.Builder.of(BreedingBoxBlockEntity::new, BREEDING_BOX.get()).build(null));

    public static final RegistryObject<BlockEntityType<GrowerBlockEntity>> GROWER_BE =
            BLOCK_ENTITIES.register("grower",
                    () -> BlockEntityType.Builder.of(GrowerBlockEntity::new, GROWER.get()).build(null));

    public static final RegistryObject<BlockEntityType<NestBlockEntity>> NEST_BE =
            BLOCK_ENTITIES.register("nest",
                    () -> BlockEntityType.Builder.of(NestBlockEntity::new, NEST.get()).build(null));

    // ------------------------------------------------------------------ 实体
    /** 唯一实体类型，所有品种共用（品种存在实体的同步数据里）。
     *  canSpawnFarFromPlayer：解除 creature 的「玩家 32 格内」限制（下界 monster 通道生成位置更远） */
    public static final RegistryObject<EntityType<ResourceChicken>> RESOURCE_CHICKEN =
            ENTITY_TYPES.register("resource_chicken",
                    () -> EntityType.Builder.of(ResourceChicken::new, net.minecraft.world.entity.MobCategory.CREATURE)
                            .sized(0.4F, 0.7F).canSpawnFarFromPlayer().build("resource_chicken"));

    public static final RegistryObject<EntityType<ThrownDyeEgg>> THROWN_DYE_EGG =
            ENTITY_TYPES.register("thrown_dye_egg",
                    () -> EntityType.Builder.<ThrownDyeEgg>of(ThrownDyeEgg::new,
                                    net.minecraft.world.entity.MobCategory.MISC)
                            .sized(0.25F, 0.25F).build("thrown_dye_egg"));

    // ------------------------------------------------------------------ 菜单
    public static final RegistryObject<MenuType<BreedingBoxMenu>> BREEDING_BOX_MENU =
            MENU_TYPES.register("breeding_box", () -> new MenuType<>(BreedingBoxMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<MenuType<GrowerMenu>> GROWER_MENU =
            MENU_TYPES.register("grower", () -> new MenuType<>(GrowerMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<MenuType<NestMenu>> NEST_MENU =
            MENU_TYPES.register("nest", () -> new MenuType<>(NestMenu::new, FeatureFlags.DEFAULT_FLAGS));

    // ------------------------------------------------------------------ 创造模式 Tab
    public static final RegistryObject<CreativeModeTab> CHICKEN_TAB =
            CREATIVE_TABS.register("chickens", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.chickens"))
                    .icon(() -> new ItemStack(CHICKEN.get()))
                    .build());

    // ------------------------------------------------------------------ biome modifier 序列化器
    /** add_spawns_to_category = 官方 add_spawns + 可指定刷怪池类别（下界/末地 monster 通道用） */
    public static final RegistryObject<Codec<? extends BiomeModifier>> ADD_SPAWNS_TO_CATEGORY =
            BIOME_MODIFIER_SERIALIZERS.register("add_spawns_to_category",
                    () -> AddSpawnsToCategoryBiomeModifier.CODEC);

    private ModRegistry() {
    }

    /** 品种 Tab 内容：工具/方块/蛋 + 原版鸡物品 + 全部品种鸡（按 Tier 分组，组内按 id 排序） */
    public static void fillTab(CreativeModeTab.Output output) {
        output.accept(new ItemStack(CATCHER.get()));
        output.accept(new ItemStack(CHICKEN_ANALYZER.get()));
        output.accept(new ItemStack(BREEDING_BOX_ITEM.get()));
        output.accept(new ItemStack(GROWER_ITEM.get()));
        output.accept(new ItemStack(NEST_ITEM.get()));
        // 流体蛋：数据里每个 fluid_egg 条目一个
        for (FluidEggEntry entry : ChickenData.get().fluidEggs().values()) {
            if (BreedLookups.fluidOf(entry.fluid()) != null) {
                ItemStack egg = new ItemStack(FLUID_EGG.get());
                ItemData.setFluid(egg, entry.fluid());
                output.accept(egg);
            }
        }
        // 16 种染色蛋（平均色）
        for (DyeColor dye : DyeColor.values()) {
            output.accept(DyeEggColors.stack(dye));
        }
        output.accept(new ItemStack(CHICKEN.get()));
        List<Map.Entry<ResourceLocation, ChickenBreed>> breeds = ChickenData.sortedBreeds();
        breeds.sort(Comparator.<Map.Entry<ResourceLocation, ChickenBreed>>comparingInt(
                        entry -> entry.getValue().tier())
                .thenComparing(Map.Entry::getKey));
        for (Map.Entry<ResourceLocation, ChickenBreed> entry : breeds) {
            ItemStack stack = new ItemStack(CHICKEN.get());
            ItemData.setBreed(stack, entry.getKey());
            output.accept(stack);
        }
    }
}
