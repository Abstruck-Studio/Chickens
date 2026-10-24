package org.abstruck.chickens.client;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.breed.BreedLookups;
import org.abstruck.chickens.item.ItemData;
import org.abstruck.chickens.registry.ModRegistry;

import java.util.OptionalInt;

/** 客户端：方块渲染层 + 界面注册 + 物品染色 */
@Mod.EventBusSubscriber(modid = Chickens.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    /** 三个设施方块的渲染层：模型由 BER（ChickenBoxRenderer）统一光照渲染；同时注册菜单界面 */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModRegistry.BREEDING_BOX.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModRegistry.GROWER.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModRegistry.NEST.get(), RenderType.cutoutMipped());

            MenuScreens.register(ModRegistry.BREEDING_BOX_MENU.get(), BreedingBoxScreen::new);
            MenuScreens.register(ModRegistry.GROWER_MENU.get(), GrowerScreen::new);
            MenuScreens.register(ModRegistry.NEST_MENU.get(), NestScreen::new);
        });
    }

    /** 流体蛋按流体颜色染色（染料蛋颜色已烘进贴图，不再乘色）；返回值 0xAARRGGBB 必须补 alpha 位 */
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            ResourceLocation fluidId = ItemData.getFluidId(stack);
            if (fluidId == null) {
                return 0xFFFFFFFF;
            }
            // 颜色查 chickens:fluid_egg 数据；不可用时回退内置水/岩浆色
            OptionalInt color = BreedLookups.colorOfFluid(fluidId);
            if (color.isPresent()) {
                return 0xFF000000 | color.getAsInt();
            }
            if (new ResourceLocation("water").equals(fluidId)) {
                return 0xFF3F76E4;
            }
            if (new ResourceLocation("lava").equals(fluidId)) {
                return 0xFFFF8A28;
            }
            return 0xFFFFFFFF;
        }, ModRegistry.FLUID_EGG.get());
    }
}
