package org.abstruck.chickens.registry;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
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
import org.abstruck.chickens.entity.ThrownDyeEgg;
import org.abstruck.chickens.entity.chicken.ResourceChicken;
import org.abstruck.chickens.item.AnalyzerItem;
import org.abstruck.chickens.item.ChickenCatcherItem;
import org.abstruck.chickens.item.ChickenItem;
import org.abstruck.chickens.item.DyeEggItem;
import org.abstruck.chickens.item.FluidEggItem;
import org.abstruck.chickens.world.AddSpawnsToCategoryBiomeModifier;

/**
 * 全部注册对象（1.19.2 使用 Forge 的 {@link DeferredRegister}；没有 NeoForge 的
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

    // ------------------------------------------------------------------ 方块
    /** noOcclusion：模型是镂空结构，不透光会把方块内照暗——noOcclusion 让光穿过（碰撞箱不变） */
    public static final net.minecraftforge.registries.RegistryObject<Block> BREEDING_BOX =
            BLOCKS.register("breeding_box", () -> new BreedingBoxBlock(BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.WOOD)
                    .strength(2.0F).sound(SoundType.WOOD).noOcclusion()));

    public static final net.minecraftforge.registries.RegistryObject<Block> GROWER =
            BLOCKS.register("grower", () -> new GrowerBlock(BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.WOOD)
                    .strength(2.0F).sound(SoundType.WOOD).noOcclusion()));

    public static final net.minecraftforge.registries.RegistryObject<Block> NEST =
            BLOCKS.register("nest", () -> new NestBlock(BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.WOOD)
                    .strength(2.0F).sound(SoundType.WOOD).noOcclusion()));

    // ------------------------------------------------------------------ 物品
    /** 鸡物品：品种存 NBT，右键放置生成对应资源鸡（只进自定义 Tab，避免 Misc 里出现重复/空白条目） */
    public static final net.minecraftforge.registries.RegistryObject<Item> CHICKEN =
            ITEMS.register("chicken", () -> new ChickenItem(new Item.Properties().stacksTo(16)));

    /** 鸡捕手：右键把鸡变成鸡物品（原版鸡按默认品种捕获）；128 点耐久，每次捕获消耗 1 点 */
    public static final net.minecraftforge.registries.RegistryObject<Item> CATCHER =
            ITEMS.register("catcher", () -> new ChickenCatcherItem(
                    new Item.Properties().durability(128).tab(CreativeModeTab.TAB_TOOLS)));

    public static final net.minecraftforge.registries.RegistryObject<Item> BREEDING_BOX_ITEM =
            ITEMS.register("breeding_box", () -> new BlockItem(BREEDING_BOX.get(), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS)));

    public static final net.minecraftforge.registries.RegistryObject<Item> GROWER_ITEM =
            ITEMS.register("grower", () -> new BlockItem(GROWER.get(), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS)));

    public static final net.minecraftforge.registries.RegistryObject<Item> NEST_ITEM =
            ITEMS.register("nest", () -> new BlockItem(NEST.get(), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS)));

    /** 鸡分析器：右键鸡显示品种/属性/下次产出时间 */
    public static final net.minecraftforge.registries.RegistryObject<Item> CHICKEN_ANALYZER =
            ITEMS.register("chicken_analyzer", () -> new AnalyzerItem(new Item.Properties().tab(CreativeModeTab.TAB_TOOLS)));

    /** 流体蛋：流体存 NBT，右键倒出；颜色由 {@code chickens:fluid_egg} 数据定义（只进自定义 Tab） */
    public static final net.minecraftforge.registries.RegistryObject<Item> FLUID_EGG =
            ITEMS.register("fluid_egg", () -> new FluidEggItem(new Item.Properties().stacksTo(64)));

    /** 染料蛋：投掷命中孵出对应染料鸡（只进自定义 Tab） */
    public static final net.minecraftforge.registries.RegistryObject<Item> DYE_EGG =
            ITEMS.register("dye_egg", () -> new DyeEggItem(new Item.Properties().stacksTo(16)));

    // ------------------------------------------------------------------ 方块实体
    public static final net.minecraftforge.registries.RegistryObject<BlockEntityType<BreedingBoxBlockEntity>> BREEDING_BOX_BE =
            BLOCK_ENTITIES.register("breeding_box",
                    () -> BlockEntityType.Builder.of(BreedingBoxBlockEntity::new, BREEDING_BOX.get()).build(null));

    public static final net.minecraftforge.registries.RegistryObject<BlockEntityType<GrowerBlockEntity>> GROWER_BE =
            BLOCK_ENTITIES.register("grower",
                    () -> BlockEntityType.Builder.of(GrowerBlockEntity::new, GROWER.get()).build(null));

    public static final net.minecraftforge.registries.RegistryObject<BlockEntityType<NestBlockEntity>> NEST_BE =
            BLOCK_ENTITIES.register("nest",
                    () -> BlockEntityType.Builder.of(NestBlockEntity::new, NEST.get()).build(null));

    // ------------------------------------------------------------------ 实体
    /** 唯一实体类型，所有品种共用（品种存在实体的同步数据里）。
     *  canSpawnFarFromPlayer：解除 creature 的「玩家 32 格内」限制（下界 monster 通道生成位置更远） */
    public static final net.minecraftforge.registries.RegistryObject<EntityType<ResourceChicken>> RESOURCE_CHICKEN =
            ENTITY_TYPES.register("resource_chicken",
                    () -> EntityType.Builder.of(ResourceChicken::new, net.minecraft.world.entity.MobCategory.CREATURE)
                            .sized(0.4F, 0.7F).canSpawnFarFromPlayer().build("resource_chicken"));

    public static final net.minecraftforge.registries.RegistryObject<EntityType<ThrownDyeEgg>> THROWN_DYE_EGG =
            ENTITY_TYPES.register("thrown_dye_egg",
                    () -> EntityType.Builder.<ThrownDyeEgg>of(ThrownDyeEgg::new,
                                    net.minecraft.world.entity.MobCategory.MISC)
                            .sized(0.25F, 0.25F).build("thrown_dye_egg"));

    // ------------------------------------------------------------------ 菜单
    public static final net.minecraftforge.registries.RegistryObject<MenuType<BreedingBoxMenu>> BREEDING_BOX_MENU =
            MENU_TYPES.register("breeding_box", () -> new MenuType<>(BreedingBoxMenu::new));

    public static final net.minecraftforge.registries.RegistryObject<MenuType<GrowerMenu>> GROWER_MENU =
            MENU_TYPES.register("grower", () -> new MenuType<>(GrowerMenu::new));

    public static final net.minecraftforge.registries.RegistryObject<MenuType<NestMenu>> NEST_MENU =
            MENU_TYPES.register("nest", () -> new MenuType<>(NestMenu::new));

    // ------------------------------------------------------------------ biome modifier 序列化器
    /** add_spawns_to_category = 官方 add_spawns + 可指定刷怪池类别（下界/末地 monster 通道用） */
    public static final net.minecraftforge.registries.RegistryObject<Codec<? extends BiomeModifier>> ADD_SPAWNS_TO_CATEGORY =
            BIOME_MODIFIER_SERIALIZERS.register("add_spawns_to_category",
                    () -> AddSpawnsToCategoryBiomeModifier.CODEC);

    private ModRegistry() {
    }
}
