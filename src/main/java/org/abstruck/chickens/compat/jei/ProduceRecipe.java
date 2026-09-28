package org.abstruck.chickens.compat.jei;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * JEI 产出配方：一只鸡 → 主产物 + 副产物列表（各带权重）。
 */
public record ProduceRecipe(ItemStack chicken, List<Output> outputs) {
    /** 单格输出：物品 + 权重 + 是否主产物 */
    public record Output(ItemStack stack, int weight, boolean main) {
    }
}
