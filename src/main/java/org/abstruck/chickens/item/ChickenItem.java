package org.abstruck.chickens.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.abstruck.chickens.entity.chicken.ChickenStats;
import org.abstruck.chickens.entity.chicken.ResourceChicken;
import org.abstruck.chickens.registry.ModDataComponents;
import org.abstruck.chickens.registry.ModEntities;

import java.util.List;

/**
 * 鸡物品：右键方块放出资源鸡（携带品种/三维属性/自定义名/幼年状态）；
 * 无品种组件 = 鸡捕手抓的原版鸡，放出后保持原版鸡。图标由实体实时渲染（见 client.ChickenIconRenderer）。
 */
public class ChickenItem extends Item {
    public ChickenItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        var breed = stack.get(ModDataComponents.BREED.get());
        if (breed != null) {
            return Component.translatable("breed.chickens." + breed.getPath());
        }
        return super.getName(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        ResourceLocation breed = stack.get(ModDataComponents.BREED.get());
        Boolean mature = stack.get(ModDataComponents.MATURE.get());
        boolean baby = mature != null && !mature;

        if (breed == null) {
            var chicken = EntityType.CHICKEN.create(level);
            if (chicken == null) {
                return InteractionResult.PASS;
            }
            if (customName != null) {
                chicken.setCustomName(customName);
            }
            chicken.setBaby(baby);
            chicken.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, context.getRotation(), 0.0F);
            level.addFreshEntity(chicken);
            chicken.playSound(SoundEvents.CHICKEN_AMBIENT, 1.0F, 1.0F);
        } else {
            ResourceChicken chicken = ModEntities.RESOURCE_CHICKEN.get().create(level);
            if (chicken == null) {
                return InteractionResult.PASS;
            }
            chicken.setBreed(breed);
            chicken.setBaby(baby);
            ChickenStats stats = stack.get(ModDataComponents.STATS.get());
            if (stats != null) {
                chicken.setGrowth(stats.growth());
                chicken.setGain(stats.gain());
                chicken.setStrength(stats.strength());
            }
            if (customName != null) {
                chicken.setCustomName(customName);
            }
            chicken.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, context.getRotation(), 0.0F);
            level.addFreshEntity(chicken);
            chicken.playSound(SoundEvents.CHICKEN_AMBIENT, 1.0F, 1.0F);
        }
        if (!context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Boolean mature = stack.get(ModDataComponents.MATURE.get());
        tooltip.add(Component.translatable(mature != null && !mature
                ? "tooltip.chickens.baby" : "tooltip.chickens.adult").withStyle(ChatFormatting.GOLD));
        var stats = stack.get(ModDataComponents.STATS.get());
        if (stats != null) {
            tooltip.add(statBar("tooltip.chickens.growth", stats.growth()));
            tooltip.add(statBar("tooltip.chickens.gain", stats.gain()));
            tooltip.add(statBar("tooltip.chickens.strength", stats.strength()));
        }
    }

    /** 竖条样式：等级 1~10 映射为 10 段（蓝色 = 进度，灰色 = 空余） */
    private static Component statBar(String labelKey, byte value) {
        int filled = Math.clamp(value, 0, 10);
        return Component.translatable(labelKey)
                .append(Component.literal("|".repeat(filled)).withStyle(ChatFormatting.BLUE))
                .append(Component.literal("|".repeat(10 - filled)).withStyle(ChatFormatting.DARK_GRAY));
    }

}
