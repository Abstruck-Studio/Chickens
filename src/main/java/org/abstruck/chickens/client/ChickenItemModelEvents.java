package org.abstruck.chickens.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.registry.ModDataComponents;
import org.abstruck.chickens.registry.ModItems;

/** 客户端：鸡物品的定制渲染（实体实时图标）——模型替换 + 客户端扩展注册。 */
@EventBusSubscriber(modid = Chickens.MODID, value = Dist.CLIENT)
public final class ChickenItemModelEvents {
    private static final ModelResourceLocation CHICKEN_ITEM =
            new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(Chickens.MODID, "chicken"), "inventory");

    private ChickenItemModelEvents() {
    }

    /** 把鸡物品的模型换成定制渲染模型（isCustomRenderer = true） */
    @SubscribeEvent
    public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BakedModel original = event.getModels().get(CHICKEN_ITEM);
        if (original != null) {
            event.getModels().put(CHICKEN_ITEM, new ChickenIconBakedModel(original));
        }
    }

    /** 注册客户端扩展（21.1.252 起 initializeClient 已弃用，改用本事件） */
    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(ChickenItemClientExtensions.INSTANCE, ModItems.CHICKEN.get());
    }

    /** 三个设施方块的渲染层：模型由 BER（ChickenBoxRenderer）在 cutoutMipped 层统一光照渲染 */
    @SubscribeEvent
    public static void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                org.abstruck.chickens.registry.ModBlocks.BREEDING_BOX.get(), net.minecraft.client.renderer.RenderType.cutoutMipped());
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                org.abstruck.chickens.registry.ModBlocks.GROWER.get(), net.minecraft.client.renderer.RenderType.cutoutMipped());
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                org.abstruck.chickens.registry.ModBlocks.NEST.get(), net.minecraft.client.renderer.RenderType.cutoutMipped());
    }

    /** 繁殖箱 / 培育箱 / 鸡窝界面 */
    @SubscribeEvent
    public static void registerMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(org.abstruck.chickens.registry.ModMenuTypes.BREEDING_BOX.get(), BreedingBoxScreen::new);
        event.register(org.abstruck.chickens.registry.ModMenuTypes.GROWER.get(), GrowerScreen::new);
        event.register(org.abstruck.chickens.registry.ModMenuTypes.NEST.get(), NestScreen::new);
    }

    /** 染料蛋按 dyed_color 组件染色、流体蛋按 chickens:fluid 组件染色。
     *  ItemColor 返回值是 0xAARRGGBB，RGB 颜色必须补 alpha 位，否则物品透明。 */
    @SubscribeEvent
    public static void registerItemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            net.minecraft.world.item.component.DyedItemColor color =
                    stack.get(net.minecraft.core.component.DataComponents.DYED_COLOR);
            return color != null ? 0xFF000000 | color.rgb() : 0xFFFFFFFF;
        }, ModItems.DYE_EGG.get());
        event.register((stack, tintIndex) -> {
            net.minecraft.core.Holder<net.minecraft.world.level.material.Fluid> fluidHolder =
                    stack.get(ModDataComponents.FLUID.get());
            if (fluidHolder == null) {
                return 0xFFFFFFFF;
            }
            net.minecraft.resources.ResourceLocation fluidId =
                    fluidHolder.unwrapKey().map(net.minecraft.resources.ResourceKey::location).orElse(null);
            if (fluidId == null) {
                return 0xFFFFFFFF;
            }
            // 颜色查数据包注册表 chickens:fluid_egg（其他模组可带 JSON 添加）；
            // 注册表不可用/未同步时回退内置水/岩浆色
            if (Minecraft.getInstance().level != null) {
                java.util.OptionalInt color = org.abstruck.chickens.breed.BreedLookups.colorOfFluid(
                        Minecraft.getInstance().level.registryAccess(), fluidId);
                // 注意：本机 JDK 的 OptionalInt 没有 mapToObj（精简版 JDK），只能 isPresent 判断
                if (color.isPresent()) {
                    return 0xFF000000 | color.getAsInt();
                }
            }
            if (net.minecraft.resources.ResourceLocation.withDefaultNamespace("water").equals(fluidId)) {
                return 0xFF3F76E4;
            }
            if (net.minecraft.resources.ResourceLocation.withDefaultNamespace("lava").equals(fluidId)) {
                return 0xFFFF8A28;
            }
            return 0xFFFFFFFF;
        }, ModItems.FLUID_EGG.get());
    }
}
