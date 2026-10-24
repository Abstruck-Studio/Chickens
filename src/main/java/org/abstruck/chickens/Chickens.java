package org.abstruck.chickens;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.abstruck.chickens.config.ChickenConfig;
import org.abstruck.chickens.entity.chicken.ChickenSpawnEvents;
import org.abstruck.chickens.network.ChickensNetwork;
import org.abstruck.chickens.registry.ModRegistry;

/**
 * Chickens（Forge 1.20.1 移植版）。
 *
 * <p>与 1.21.1 版功能一致：资源鸡、三维属性育种、杂交规则、自动化设施、
 * 数据包驱动的品种/杂交/流体蛋/自然生成；物品数据用 NBT（组件系统自 1.20.5 起才有）。</p>
 */
@Mod(Chickens.MODID)
public class Chickens {
    public static final String MODID = "chickens";

    public Chickens() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModRegistry.BLOCKS.register(modBus);
        ModRegistry.ITEMS.register(modBus);
        ModRegistry.BLOCK_ENTITIES.register(modBus);
        ModRegistry.ENTITY_TYPES.register(modBus);
        ModRegistry.MENU_TYPES.register(modBus);
        ModRegistry.BIOME_MODIFIER_SERIALIZERS.register(modBus);
        ModRegistry.CREATIVE_TABS.register(modBus);

        ChickensNetwork.register();

        // 服务端数据包内容载入 / 同步
        MinecraftForge.EVENT_BUS.register(org.abstruck.chickens.breed.ChickenDataEvents.class);
        // 自然生成的品种分配
        MinecraftForge.EVENT_BUS.register(ChickenSpawnEvents.class);
        // 染料蛋配方兜底染色
        MinecraftForge.EVENT_BUS.register(CommonEvents.class);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ChickenConfig.SPEC);
    }
}
