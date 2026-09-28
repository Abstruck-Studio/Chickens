package org.abstruck.chickens.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.abstruck.chickens.entity.chicken.ResourceChicken;

/**
 * 鸡分析器（规格 1.3）：鸡蛋 + 指南针合成；右键鸡显示品种、Tier、三维属性、距下次产出的时间。
 */
public class AnalyzerItem extends Item {
    public AnalyzerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(target instanceof ResourceChicken chicken)) {
            return InteractionResult.PASS;
        }
        player.displayClientMessage(Component.translatable("message.chickens.analyzer_header",
                Component.translatable("breed.chickens." + chicken.getBreedId().getPath())), false);
        player.displayClientMessage(Component.literal("  ")
                .append(bar("tooltip.chickens.growth", chicken.getGrowth()))
                .append("  ").append(bar("tooltip.chickens.gain", chicken.getGain()))
                .append("  ").append(bar("tooltip.chickens.strength", chicken.getStrength())), false);
        if (chicken.isBaby()) {
            // 幼年鸡：显示距成年时间（getAge 为负，取绝对值）
            player.displayClientMessage(Component.translatable("message.chickens.analyzer_growth",
                    Math.max(0, -chicken.getAge()) / 20), false);
        } else {
            player.displayClientMessage(Component.translatable("message.chickens.analyzer_next",
                    Math.max(0, chicken.getProductionTimer()) / 20), false);
        }
        return InteractionResult.SUCCESS;
    }

    private static Component bar(String labelKey, byte value) {
        int filled = Math.clamp(value, 0, 10);
        return Component.translatable(labelKey)
                .append(Component.literal("|".repeat(filled)).withStyle(ChatFormatting.BLUE))
                .append(Component.literal("|".repeat(10 - filled)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
