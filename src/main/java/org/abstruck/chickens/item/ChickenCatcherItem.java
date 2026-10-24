package org.abstruck.chickens.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.abstruck.chickens.entity.chicken.ChickenStats;
import org.abstruck.chickens.entity.chicken.ResourceChicken;
import org.abstruck.chickens.registry.ModRegistry;

/**
 * 鸡捕手：右键任意鸡（含原版鸡）→ 捕获为鸡物品，品种/三维属性/自定义名/幼年状态完整保存；
 * 原版鸡捕获物不带品种数据，放出后仍是原版鸡。
 * 128 点耐久：每次成功捕获消耗 1 点，耐久耗尽后物品损毁（创造模式不消耗）。
 */
public class ChickenCatcherItem extends Item {
    public ChickenCatcherItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        if (player.level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(target instanceof net.minecraft.world.entity.animal.Chicken chicken)) {
            return InteractionResult.PASS;
        }
        ItemStack chickenItem = new ItemStack(ModRegistry.CHICKEN.get());
        ItemData.setMature(chickenItem, !chicken.isBaby());
        if (chicken instanceof ResourceChicken resource) {
            ResourceLocation breed = resource.getBreedId();
            ChickenStats stats = new ChickenStats(resource.getGrowth(), resource.getGain(), resource.getStrength());
            ItemData.setBreed(chickenItem, breed);
            ItemData.setStats(chickenItem, stats);
        }
        if (chicken.hasCustomName()) {
            chickenItem.setHoverName(chicken.getCustomName());
        }
        target.discard();
        if (!player.getInventory().add(chickenItem)) {
            player.drop(chickenItem, false);
        }
        // 128 点耐久：每次捕获消耗 1 点，耗尽时损毁；创造模式自动不消耗
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        return InteractionResult.SUCCESS;
    }
}
