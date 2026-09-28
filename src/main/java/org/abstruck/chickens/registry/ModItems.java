package org.abstruck.chickens.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.item.AnalyzerItem;
import org.abstruck.chickens.item.ChickenCatcherItem;
import org.abstruck.chickens.item.ChickenItem;
import org.abstruck.chickens.item.DyeEggItem;
import org.abstruck.chickens.item.FertileEggItem;
import org.abstruck.chickens.item.FluidEggItem;

/**
 * 物品注册。按用户规格不需要刷怪蛋（已移除）。
 * 本类持有 DeferredRegister 条目，主类构造时必须调用 {@link #init()} 触发类初始化。
 */
public final class ModItems {
    /** 鸡物品：品种由 {@code chickens:breed} 组件区分，右键放置生成对应资源鸡；堆叠上限 16 */
    public static final DeferredItem<Item> CHICKEN =
            Chickens.ITEMS.register("chicken", () -> new ChickenItem(new Item.Properties().stacksTo(16)));

    /** 鸡捕手：右键鸡把它变成鸡物品（原版鸡也可，按默认品种捕获） */
    public static final DeferredItem<Item> CATCHER =
            Chickens.ITEMS.register("catcher", () -> new ChickenCatcherItem(new Item.Properties()));

    /** 受精鸡蛋：繁殖改造开启时鸡交配掉落；阶段 5 蛋巢孵化 */
    public static final DeferredItem<Item> FERTILE_EGG =
            Chickens.ITEMS.register("fertile_egg", () -> new FertileEggItem(new Item.Properties()));

    /** 鸡粪（阶段 5 巢箱产出；阶段 7 粪肥能源链的原料） */
    public static final DeferredItem<Item> MANURE =
            Chickens.ITEMS.register("manure", () -> new Item(new Item.Properties()));

    /** 方块物品（21.1.252 的 DeferredRegister.Blocks 不会自动注册，必须显式注册） */
    public static final DeferredItem<Item> BREEDING_BOX =
            Chickens.ITEMS.register("breeding_box", () -> new BlockItem(ModBlocks.BREEDING_BOX.get(), new Item.Properties()));

    public static final DeferredItem<Item> GROWER =
            Chickens.ITEMS.register("grower", () -> new BlockItem(ModBlocks.GROWER.get(), new Item.Properties()));

    public static final DeferredItem<Item> NEST =
            Chickens.ITEMS.register("nest", () -> new BlockItem(ModBlocks.NEST.get(), new Item.Properties()));

    /** 鸡分析器（阶段 6 保留）：右键鸡显示品种/属性/下次产出时间 */
    public static final DeferredItem<Item> CHICKEN_ANALYZER =
            Chickens.ITEMS.register("chicken_analyzer", () -> new AnalyzerItem(new Item.Properties()));

    /** 流体蛋：统一的水蛋/岩浆蛋/自定义流体蛋，流体类型存 {@code chickens:fluid} 组件；
     *  右键方块倒出流体源（不能装）。流体-颜色表在配置文件 fluidEggs 里 */
    public static final DeferredItem<Item> FLUID_EGG =
            Chickens.ITEMS.register("fluid_egg", () -> new FluidEggItem(new Item.Properties().stacksTo(64)));

    /** 染料蛋：鸡蛋+染料合成，颜色存 dyed_color 组件，扔出孵对应染料鸡 */
    public static final DeferredItem<Item> DYE_EGG =
            Chickens.ITEMS.register("dye_egg", () -> new DyeEggItem(new Item.Properties().stacksTo(16)));

    /** 触发类初始化（无实际逻辑） */
    public static void init() {
    }

    private ModItems() {
    }
}
