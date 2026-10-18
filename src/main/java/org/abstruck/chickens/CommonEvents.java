package org.abstruck.chickens;

import net.minecraft.world.Container;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.abstruck.chickens.item.DyeEggItem;
import org.abstruck.chickens.item.ItemData;
import org.abstruck.chickens.registry.ModRegistry;

/**
 * 通用游戏事件。
 *
 * <p>1.19.2 的酿造/合成结果 JSON 不支持 NBT，所以染料蛋的 16 个配方只产出未染色的蛋，
 * 由 {@link PlayerEvent.ItemCraftedEvent} 读取合成格里的染料再写入颜色（等价于 1.21.1 的
 * {@code minecraft:dyed_color} 组件）。</p>
 */
public final class CommonEvents {
    private CommonEvents() {
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack crafted = event.getCrafting();
        if (crafted.isEmpty() || !crafted.is(ModRegistry.DYE_EGG.get()) || ItemData.getColor(crafted) >= 0) {
            return;
        }
        Container inventory = event.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack ingredient = inventory.getItem(i);
            if (ingredient.getItem() instanceof DyeItem dyeItem) {
                ItemData.setColor(crafted, DyeEggItem.rgbOf(dyeItem.getDyeColor()));
                return;
            }
        }
    }
}
