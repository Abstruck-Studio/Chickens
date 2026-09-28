package org.abstruck.chickens.item;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import org.abstruck.chickens.entity.ThrownDyeEgg;

import java.util.List;

/**
 * 染料蛋（1.12「更多鸡」的 colored_egg）：鸡蛋 + 染料合成，颜色存 {@code minecraft:dyed_color} 组件。
 * 扔出砸中后按原版鸡蛋概率（1/8，其中 1/32 出 4 只）孵出对应颜色的幼年染料鸡（规格 1.1）。
 */
public class DyeEggItem extends Item implements ProjectileItem {
    public DyeEggItem(Item.Properties properties) {
        super(properties);
    }

    /** 按 rgb 找对应 DyeColor（找不到返回 null）。两边都截断低 24 位再比较。 */
    public static DyeColor dyeColorOf(int rgb) {
        int plain = rgb & 0xFFFFFF;
        for (DyeColor dye : DyeColor.values()) {
            if ((dye.getTextureDiffuseColor() & 0xFFFFFF) == plain) {
                return dye;
            }
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        DyedItemColor color = stack.get(net.minecraft.core.component.DataComponents.DYED_COLOR);
        if (color == null) {
            return;
        }
        DyeColor dye = dyeColorOf(color.rgb());
        if (dye != null) {
            // 直接显示颜色名（如「红」），文字渲染成该颜色
            tooltip.add(Component.translatable("color.minecraft." + dye.getName())
                    .withStyle(Style.EMPTY.withColor(0xFF000000 | (color.rgb() & 0xFFFFFF))));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EGG_THROW,
                SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            ThrownDyeEgg egg = new ThrownDyeEgg(level, player);
            egg.setItem(stack);
            egg.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(egg);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        ThrownDyeEgg egg = new ThrownDyeEgg(level, pos.x(), pos.y(), pos.z());
        egg.setItem(stack);
        return egg;
    }
}
