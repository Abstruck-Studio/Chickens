package org.abstruck.chickens.compat.jei;

import net.minecraft.world.item.ItemStack;
import org.abstruck.chickens.breed.MutationRule;

import java.util.List;

/**
 * JEI 杂交配方：两只父母鸡 → 全部加权结果。父母/结果均为槽内容列表：
 * 精确品种 1 个物品；#标签 父母 / $parent:标签 结果则是标签全部成员鸡（槽内循环查看）。
 * resultStacks 与 allResults 平行：每个结果槽的内容列表。
 */
public record MutationRecipe(
        List<ItemStack> parentA,
        List<ItemStack> parentB,
        List<List<ItemStack>> resultStacks,
        List<MutationRule.WeightedResult> allResults
) {
}
