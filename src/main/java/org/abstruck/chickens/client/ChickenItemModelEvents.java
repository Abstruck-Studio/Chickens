package org.abstruck.chickens.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.abstruck.chickens.Chickens;

/**
 * 客户端：把鸡物品模型换成定制渲染模型（isCustomRenderer = true），
 * 这样物品渲染才会使用 {@link ChickenIconRenderer}（实体实时图标）。
 *
 * <p>1.19.2 用 {@link ModelEvent.BakingCompleted}（1.21.1 是 {@code ModelEvent.ModifyBakingResult}）。</p>
 */
@Mod.EventBusSubscriber(modid = Chickens.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ChickenItemModelEvents {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final ModelResourceLocation CHICKEN_ITEM =
            new ModelResourceLocation(new ResourceLocation(Chickens.MODID, "chicken"), "inventory");

    private ChickenItemModelEvents() {
    }

    @SubscribeEvent
    public static void onBakingCompleted(ModelEvent.BakingCompleted event) {
        BakedModel original = event.getModels().get(CHICKEN_ITEM);
        if (original != null) {
            event.getModels().put(CHICKEN_ITEM, new ChickenIconBakedModel(original));
            LOGGER.info("[chickens] 鸡物品模型已切换为实体实时渲染（custom renderer）");
        } else {
            LOGGER.error("[chickens] 未找到鸡物品模型 {}，物品图标将退回平面贴图", CHICKEN_ITEM);
        }
    }
}
