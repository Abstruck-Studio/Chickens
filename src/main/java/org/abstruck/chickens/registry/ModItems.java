package org.abstruck.chickens.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.item.AnalyzerItem;
import org.abstruck.chickens.item.ChickenCatcherItem;
import org.abstruck.chickens.item.ChickenItem;
import org.abstruck.chickens.item.DyeEggItem;
import org.abstruck.chickens.item.FluidEggFluidHandler;
import org.abstruck.chickens.item.FluidEggItem;

/** 物品注册。主类构造时必须调用 {@link #init()} 触发类初始化 */
public final class ModItems {
    /** 鸡物品：品种存 {@code chickens:breed} 组件，右键放置生成对应资源鸡 */
    public static final DeferredItem<Item> CHICKEN =
            Chickens.ITEMS.register("chicken", () -> new ChickenItem(new Item.Properties().stacksTo(16)));

    /** 鸡捕手：右键把鸡变成鸡物品（原版鸡按默认品种捕获） */
    public static final DeferredItem<Item> CATCHER =
            Chickens.ITEMS.register("catcher", () -> new ChickenCatcherItem(new Item.Properties()));

    /** 方块物品（DeferredRegister.Blocks 不会自动注册，必须显式注册） */
    public static final DeferredItem<Item> BREEDING_BOX =
            Chickens.ITEMS.register("breeding_box", () -> new BlockItem(ModBlocks.BREEDING_BOX.get(), new Item.Properties()));

    public static final DeferredItem<Item> GROWER =
            Chickens.ITEMS.register("grower", () -> new BlockItem(ModBlocks.GROWER.get(), new Item.Properties()));

    public static final DeferredItem<Item> NEST =
            Chickens.ITEMS.register("nest", () -> new BlockItem(ModBlocks.NEST.get(), new Item.Properties()));

    /** 鸡分析器：右键鸡显示品种/属性/下次产出时间 */
    public static final DeferredItem<Item> CHICKEN_ANALYZER =
            Chickens.ITEMS.register("chicken_analyzer", () -> new AnalyzerItem(new Item.Properties()));

    /** 流体蛋：流体存 {@code chickens:fluid} 组件，右键倒出；颜色由 {@code chickens:fluid_egg} 注册表定义 */
    public static final DeferredItem<Item> FLUID_EGG =
            Chickens.ITEMS.register("fluid_egg", () -> new FluidEggItem(new Item.Properties().stacksTo(64)));

    /** 染料蛋：投掷命中孵出对应染料鸡 */
    public static final DeferredItem<Item> DYE_EGG =
            Chickens.ITEMS.register("dye_egg", () -> new DyeEggItem(new Item.Properties().stacksTo(16)));

    /** 触发类初始化（无实际逻辑） */
    public static void init() {
    }

    /** 流体蛋暴露物品流体能力（IFluidHandlerItem），储罐类方块经 FluidUtil 读取即可倒入 */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, ctx) -> new FluidEggFluidHandler(stack), FLUID_EGG.get());
    }

    private ModItems() {
    }
}
