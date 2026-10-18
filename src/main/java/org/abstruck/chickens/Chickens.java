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
import org.abstruck.chickens.registry.ModCreativeTabs;

/**
 * Chickens（Forge 1.19.2 移植版）。
 *
 * <p>与 1.21.1 版功能一致：69 种资源鸡（1.19.2 缺少樱花原木/竹子的对应资源，故未加入）、
 * 三维属性育种、杂交规则、自动化设施、数据包驱动的品种/杂交/流体蛋/自然生成。</p>
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

        // 1.19.2 的创造 Tab：Forge 补丁支持把自定义 CreativeModeTab 追加进静态表
        ModCreativeTabs.init();

        ChickensNetwork.register();

        // 服务端数据包内容载入 / 同步
        MinecraftForge.EVENT_BUS.register(org.abstruck.chickens.breed.ChickenDataEvents.class);
        // 自然生成的品种分配
        MinecraftForge.EVENT_BUS.register(ChickenSpawnEvents.class);
        // 染料蛋染色（1.19.2 配方结果不支持 NBT）
        MinecraftForge.EVENT_BUS.register(CommonEvents.class);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ChickenConfig.SPEC);
    }
}
