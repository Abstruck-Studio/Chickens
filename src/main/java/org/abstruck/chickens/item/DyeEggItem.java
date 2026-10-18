package org.abstruck.chickens.item;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.abstruck.chickens.entity.ThrownDyeEgg;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 染料蛋：颜色存物品 NBT（{@code Color}）；扔出砸中后按原版鸡蛋概率
 * （1/8 出鸡，其中 1/32 出 4 只）孵出对应颜色的幼年染料鸡。
 */
public class DyeEggItem extends Item {
    public DyeEggItem(Properties properties) {
        super(properties);
    }

    /** 染料颜色（1.19.2 的 DyeColor 只提供 float 数组形式） */
    public static int rgbOf(DyeColor dye) {
        float[] colors = dye.getTextureDiffuseColors();
        int r = Math.round(colors[0] * 255.0F);
        int g = Math.round(colors[1] * 255.0F);
        int b = Math.round(colors[2] * 255.0F);
        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    /** 按 rgb 找对应 DyeColor（找不到返回 null）；两边都截断到低 24 位再比较 */
    public static DyeColor dyeColorOf(int rgb) {
        int plain = rgb & 0xFFFFFF;
        for (DyeColor dye : DyeColor.values()) {
            if (rgbOf(dye) == plain) {
                return dye;
            }
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int color = ItemData.getColor(stack);
        if (color < 0) {
            return;
        }
        DyeColor dye = dyeColorOf(color);
        if (dye != null) {
            tooltip.add(Component.translatable("color.minecraft." + dye.getName())
                    .withStyle(Style.EMPTY.withColor(0xFF000000 | color)));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EGG_THROW,
                SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            ThrownDyeEgg egg = new ThrownDyeEgg(level, player);
            egg.setItem(stack.copy());
            egg.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(egg);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
