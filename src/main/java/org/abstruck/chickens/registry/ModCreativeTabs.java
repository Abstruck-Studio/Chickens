package org.abstruck.chickens.registry;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.breed.BreedLookups;
import org.abstruck.chickens.breed.ChickenBreed;
import org.abstruck.chickens.breed.ChickenData;
import org.abstruck.chickens.breed.FluidEggEntry;
import org.abstruck.chickens.item.DyeEggColors;
import org.abstruck.chickens.item.ItemData;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 品种创造 Tab：工具/方块/蛋 + 原版鸡物品 + 全部品种鸡（按 Tier 分组，组内按 id 排序）。
 *
 * <p>1.19.2 没有 {@code CreativeModeTabEvent}（那是 1.19.3 才加入的 API），
 * 但 Forge 给 {@link CreativeModeTab} 打了补丁：{@code CreativeModeTab(String)} 构造器会把实例
 * 追加进静态表 {@code CreativeModeTab.TABS} 并分配自增 id，因此这里直接继承并自己填内容。
 * 新 Tab 会出现在创造模式背包的第二页（原版分页按钮可切换）。</p>
 */
public final class ModCreativeTabs extends CreativeModeTab {
    public static final String LANG_ID = "chickens";

    private static ModCreativeTabs instance;

    private ModCreativeTabs() {
        super(LANG_ID);
    }

    /** 在主类构造期调用一次（客户端/服务端共用同一静态表，集成服务器与客户端不会重复注册） */
    public static void init() {
        if (instance == null) {
            instance = new ModCreativeTabs();
            org.slf4j.Logger logger = com.mojang.logging.LogUtils.getLogger();
            logger.info("[chickens] 创造模式 Tab 已注册（id={}，第 {} 页）",
                    instance.getId(), instance.getTabPage() + 1);
        }
    }

    public static ModCreativeTabs get() {
        return instance;
    }

    @Override
    public ItemStack makeIcon() {
        return new ItemStack(ModRegistry.CHICKEN.get());
    }

    @Override
    public void fillItemList(NonNullList<ItemStack> items) {
        items.add(new ItemStack(ModRegistry.CATCHER.get()));
        items.add(new ItemStack(ModRegistry.CHICKEN_ANALYZER.get()));
        items.add(new ItemStack(ModRegistry.BREEDING_BOX_ITEM.get()));
        items.add(new ItemStack(ModRegistry.GROWER_ITEM.get()));
        items.add(new ItemStack(ModRegistry.NEST_ITEM.get()));
        // 流体蛋：数据里每个 fluid_egg 条目一个
        for (FluidEggEntry entry : ChickenData.get().fluidEggs().values()) {
            if (BreedLookups.fluidOf(entry.fluid()) != null) {
                ItemStack egg = new ItemStack(ModRegistry.FLUID_EGG.get());
                ItemData.setFluid(egg, entry.fluid());
                items.add(egg);
            }
        }
        // 16 种染色蛋（平均色见 DyeEggColors，与配方里的 NBT 一致）
        for (DyeColor dye : DyeColor.values()) {
            items.add(DyeEggColors.stack(dye));
        }
        items.add(new ItemStack(ModRegistry.CHICKEN.get()));
        List<Map.Entry<ResourceLocation, ChickenBreed>> breeds = ChickenData.sortedBreeds();
        breeds.sort(Comparator.<Map.Entry<ResourceLocation, ChickenBreed>>comparingInt(
                        entry -> entry.getValue().tier())
                .thenComparing(Map.Entry::getKey));
        for (Map.Entry<ResourceLocation, ChickenBreed> entry : breeds) {
            ItemStack stack = new ItemStack(ModRegistry.CHICKEN.get());
            ItemData.setBreed(stack, entry.getKey());
            items.add(stack);
        }
    }
}
