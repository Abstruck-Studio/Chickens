package org.abstruck.chickens.compat.jei;

import net.minecraft.world.item.ItemStack;
import org.abstruck.chickens.breed.MutationRule;

import java.util.List;

/**
 * JEI 杂交配方：两只父母鸡 → 全部加权结果（主结果 + 回退）。
 * resultStacks 与 allResults 平行：每个结果的鸡物品（带 breed 组件）。
 */
public record MutationRecipe(
        ItemStack parentA,
        ItemStack parentB,
        List<ItemStack> resultStacks,
        List<MutationRule.WeightedResult> allResults
) {
}
