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
 * 鸡物品：右键方块放置，生成携带品种 + 三维属性 + 自定义名的资源鸡——
 * 相当于记录完整实体信息的"刷怪蛋"。
 * 无品种组件 = 鸡捕手抓的原版鸡，放出时保持原版鸡。
 * 图标 = 实体实时渲染（客户端扩展经 RegisterClientExtensionsEvent 注册，见 client 包）。
 */
public class ChickenItem extends Item {
    public ChickenItem(Properties properties) {
        super(properties);
    }

    /** 物品名跟随品种；无品种组件（原版鸡）显示通用名 */
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
            // 原版鸡：原样放出（不变成资源鸡）
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
        // 成熟度标记（金色；原版鸡/资源鸡都可为幼年）
        Boolean mature = stack.get(ModDataComponents.MATURE.get());
        tooltip.add(Component.translatable(mature != null && !mature
                ? "tooltip.chickens.baby" : "tooltip.chickens.adult").withStyle(ChatFormatting.GOLD));
        // 三项属性：5 段竖条（蓝色 = 进度，灰色 = 空余），等级 1~10 映射为 1~5 段
        var stats = stack.get(ModDataComponents.STATS.get());
        if (stats != null) {
            tooltip.add(statBar("tooltip.chickens.growth", stats.growth()));
            tooltip.add(statBar("tooltip.chickens.gain", stats.gain()));
            tooltip.add(statBar("tooltip.chickens.strength", stats.strength()));
        }
    }

    /** 竖条样式：10 段 = 10 级（满级 10 蓝条，最低 1 蓝 + 9 灰） */
    private static Component statBar(String labelKey, byte value) {
        int filled = Math.clamp(value, 0, 10);
        return Component.translatable(labelKey)
                .append(Component.literal("|".repeat(filled)).withStyle(ChatFormatting.BLUE))
                .append(Component.literal("|".repeat(10 - filled)).withStyle(ChatFormatting.DARK_GRAY));
    }

}
