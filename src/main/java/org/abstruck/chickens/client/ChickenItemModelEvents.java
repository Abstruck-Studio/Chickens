package org.abstruck.chickens.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.DyeColor;
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
    private static final ModelResourceLocation DYE_EGG_ITEM =
            new ModelResourceLocation(new ResourceLocation(Chickens.MODID, "dye_egg"), "inventory");

    private ChickenItemModelEvents() {
    }

    /**
     * 染料蛋的 16 张贴图没有在模型 JSON 里引用，1.19.2 的图集只收模型引用的材质，
     * 必须在拼接前手动加进方块图集，否则取到的是 missing sprite。
     */
    @SubscribeEvent
    public static void onTextureStitch(net.minecraftforge.client.event.TextureStitchEvent.Pre event) {
        if (!InventoryMenu.BLOCK_ATLAS.equals(event.getAtlas().location())) {
            return;
        }
        for (DyeColor dye : DyeColor.values()) {
            event.addSprite(new ResourceLocation(Chickens.MODID, "item/dye_egg/" + dye.getName()));
        }
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
        // 染料蛋：按 NBT 颜色选预生成的贴图（不再走 item color 乘色）
        BakedModel dyeEgg = event.getModels().get(DYE_EGG_ITEM);
        if (dyeEgg != null) {
            event.getModels().put(DYE_EGG_ITEM, DyeEggBakedModel.create(dyeEgg));
            LOGGER.info("[chickens] 染料蛋模型已切换为 16 色静态贴图");
        } else {
            LOGGER.error("[chickens] 未找到染料蛋模型 {}", DYE_EGG_ITEM);
        }
    }
}
