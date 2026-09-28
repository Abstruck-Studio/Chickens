package org.abstruck.chickens.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.abstruck.chickens.entity.chicken.ChickenStats;
import org.abstruck.chickens.entity.chicken.ResourceChicken;
import org.abstruck.chickens.registry.ModDataComponents;
import org.abstruck.chickens.registry.ModItems;

/**
 * 鸡捕手：右键任意鸡（含原版鸡）→ 变成鸡物品，品种/三维属性/自定义名完整保存。
 * 原版鸡捕获的物品**不带**品种与属性组件，放出时保持原版鸡。捕手消耗 1 个（创造模式不消耗）。
 */
public class ChickenCatcherItem extends Item {
    public ChickenCatcherItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(target instanceof net.minecraft.world.entity.animal.Chicken chicken)) {
            return InteractionResult.PASS;
        }
        ItemStack chickenItem = new ItemStack(ModItems.CHICKEN.get());
        // 成熟度标记：幼年鸡保持幼年（原版鸡也可为幼年）
        chickenItem.set(ModDataComponents.MATURE.get(), !chicken.isBaby());
        if (chicken instanceof ResourceChicken resource) {
            ResourceLocation breed = resource.getBreedId();
            ChickenStats stats = new ChickenStats(resource.getGrowth(), resource.getGain(), resource.getStrength());
            chickenItem.set(ModDataComponents.BREED.get(), breed);
            chickenItem.set(ModDataComponents.STATS.get(), stats);
        }
        // 原版鸡：不加 breed/stats 组件，放出时仍是原版鸡
        if (chicken.hasCustomName()) {
            chickenItem.set(DataComponents.CUSTOM_NAME, chicken.getCustomName());
        }
        target.discard();
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (!player.getInventory().add(chickenItem)) {
            player.drop(chickenItem, false);
        }
        return InteractionResult.SUCCESS;
    }
}
