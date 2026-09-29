package org.abstruck.chickens;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.abstruck.chickens.breed.BreedLookups;
import org.abstruck.chickens.breed.ChickenRegistries;
import org.abstruck.chickens.config.ChickenConfig;
import org.abstruck.chickens.entity.chicken.BreedingEvents;
import org.abstruck.chickens.registry.ModBlockEntities;
import org.abstruck.chickens.registry.ModBlocks;
import org.abstruck.chickens.registry.ModDataComponents;
import org.abstruck.chickens.registry.ModEntities;
import org.abstruck.chickens.registry.ModItems;
import org.abstruck.chickens.registry.ModMenuTypes;

@Mod("chickens")
public class Chickens {
    public static final String MODID = "chickens";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    /** 品种 Tab：注册表里所有品种的鸡物品各一个条目（内容在 BuildCreativeModeTabContentsEvent 里填充） */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CHICKEN_TAB =
            CREATIVE_MODE_TABS.register("chickens", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.chickens"))
                    .icon(() -> new ItemStack(ModItems.CHICKEN.get()))
                    .displayItems((parameters, output) -> {
                    })
                    .build());

    public Chickens(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        // 触发持有 DeferredRegister 条目的类初始化：
        // 必须在注册表冻结（RegisterEvent）前完成注册，否则运行时才初始化会崩
        ModItems.init();
        ModBlocks.init();

        // 数据包注册表：breed（品种）/ mutation（杂交）/ fluid_egg（流体蛋）/ spawn_rule（自然生成），带客户端同步
        modEventBus.addListener(ChickenRegistries::registerDataPackRegistries);

        // 实体属性（继承原版鸡）
        modEventBus.addListener(ModEntities::registerAttributes);

        // 繁殖箱/培育箱/鸡窝的物品能力（漏斗/管道兼容）
        modEventBus.addListener(ModBlockEntities::registerCapabilities);
        // 流体蛋的物品流体能力（储罐类方块兼容）
        modEventBus.addListener(ModItems::registerCapabilities);

        // 品种 Tab 内容（需要注册表，只能在事件里填）
        modEventBus.addListener(Chickens::addTabContents);

        // 实体生成位置规则
        modEventBus.addListener(ModEntities::registerSpawnPlacements);

        // 游戏事件：繁殖改造（掉受精蛋）、自然生成品种分配
        NeoForge.EVENT_BUS.register(BreedingEvents.class);
        NeoForge.EVENT_BUS.register(org.abstruck.chickens.entity.chicken.ChickenSpawnEvents.class);

        modContainer.registerConfig(ModConfig.Type.COMMON, ChickenConfig.SPEC);
    }

    /** 品种 Tab：工具/方块/蛋物品 + 原版鸡物品 + 注册表里所有品种的鸡物品条目（按 Tier 等级分组、组内按 id 排序） */
    private static void addTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CHICKEN_TAB.getKey()) {
            return;
        }
        event.accept(new ItemStack(ModItems.CATCHER.get()));
        event.accept(new ItemStack(ModItems.CHICKEN_ANALYZER.get()));
        event.accept(new ItemStack(ModItems.BREEDING_BOX.get()));
        event.accept(new ItemStack(ModItems.GROWER.get()));
        event.accept(new ItemStack(ModItems.NEST.get()));
        // 流体蛋：数据包注册表 chickens:fluid_egg 里每个条目一个（渲染按条目颜色染色）
        event.getParameters().holders().lookupOrThrow(ChickenRegistries.FLUID_EGG).listElements()
                .map(net.minecraft.core.Holder::value)
                .forEach(entry -> {
                    net.minecraft.core.Holder<net.minecraft.world.level.material.Fluid> fluid =
                            BreedLookups.fluidHolderOf(event.getParameters().holders(), entry.fluid());
                    if (fluid != null) {
                        ItemStack fluidEgg = new ItemStack(ModItems.FLUID_EGG.get());
                        fluidEgg.set(ModDataComponents.FLUID.get(), fluid);
                        event.accept(fluidEgg);
                    }
                });
        // 16 种染色蛋（每色一条目，rgb 统一截断低 24 位与配方一致）
        for (net.minecraft.world.item.DyeColor dye : net.minecraft.world.item.DyeColor.values()) {
            ItemStack dyeEgg = new ItemStack(ModItems.DYE_EGG.get());
            dyeEgg.set(net.minecraft.core.component.DataComponents.DYED_COLOR,
                    new net.minecraft.world.item.component.DyedItemColor(
                            dye.getTextureDiffuseColor() & 0xFFFFFF, false));
            event.accept(dyeEgg);
        }
        // 原版鸡物品（无品种组件，图标 = 原版鸡实时渲染）
        event.accept(new ItemStack(ModItems.CHICKEN.get()));
        // 品种条目：先按 tier 分组排序，组内按 id 排序
        event.getParameters().holders().lookupOrThrow(ChickenRegistries.BREED).listElements()
                .sorted(java.util.Comparator.<net.minecraft.core.Holder<org.abstruck.chickens.breed.ChickenBreed>>comparingInt(
                                holder -> holder.value().tier())
                        .thenComparing(holder -> holder.getKey().location()))
                .forEach(holder -> {
                    ItemStack stack = new ItemStack(ModItems.CHICKEN.get());
                    stack.set(ModDataComponents.BREED.get(), holder.getKey().location());
                    event.accept(stack);
                });
    }

}
