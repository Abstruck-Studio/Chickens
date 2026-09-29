package org.abstruck.chickens.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.abstruck.chickens.registry.ModDataComponents;

import java.util.List;

/**
 * 受精鸡蛋：繁殖改造（vanillaBreedingRework）开启时由鸡交配掉落，
 * 携带品种 + 三维属性组件。
 * 无品种组件 = 原版鸡的受精蛋。
 */
public class FertileEggItem extends Item {
    public FertileEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var breed = stack.get(ModDataComponents.BREED.get());
        if (breed != null) {
            tooltip.add(Component.translatable("breed.chickens." + breed.getPath()));
        }
    }
}
