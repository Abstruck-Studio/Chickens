package org.abstruck.chickens.registry;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.abstruck.chickens.Chickens;

/**
 * 自定义创造模式 Tab 的内容填充（1.20.1 用 {@link BuildCreativeModeTabContentsEvent}，
 * Tab 本身在 {@link ModRegistry#CHICKEN_TAB} 注册）。
 */
@Mod.EventBusSubscriber(modid = Chickens.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModCreativeTabs {
    private ModCreativeTabs() {
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == ModRegistry.CHICKEN_TAB.get()) {
            ModRegistry.fillTab(event);
        }
    }
}
