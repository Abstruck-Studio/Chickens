package org.abstruck.chickens;

import net.minecraft.world.Container;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.abstruck.chickens.item.DyeEggColors;
import org.abstruck.chickens.item.ItemData;
import org.abstruck.chickens.registry.ModRegistry;

/**
 * 通用游戏事件。
 *
 * <p>染料蛋配方已直接在 result 的 {@code nbt.Color} 里写入颜色（1.19.2 的 Forge 配方解析支持
 * result NBT），这里只是兜底：万一某个配方没带颜色，就从合成格里的染料补写。</p>
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
                ItemData.setColor(crafted, DyeEggColors.rgb(dyeItem.getDyeColor()));
                return;
            }
        }
    }
}
