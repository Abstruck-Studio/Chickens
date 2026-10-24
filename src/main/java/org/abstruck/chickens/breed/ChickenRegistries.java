package org.abstruck.chickens.breed;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.abstruck.chickens.Chickens;

/**
 * 四个「数据包注册表」的键。
 *
 * <p>1.21.1 用 {@code DataPackRegistryEvent.NewRegistry} 注册同步注册表；1.19.2 没有这套 API，
 * 因此这里只保留注册表键（用于 {@code TagKey} 的命名空间），实际内容由 {@link ChickenData}
 * 直接扫描 {@code data/<命名空间>/chickens/<类型>/*.json} 载入，并通过网络包同步给客户端。
 * 注册表 id 与目录结构保持不变，数据包写法完全一致。</p>
 */
public final class ChickenRegistries {
    public static final ResourceKey<Registry<ChickenBreed>> BREED =
            ResourceKey.createRegistryKey(new ResourceLocation(Chickens.MODID, "breed"));

    public static final ResourceKey<Registry<MutationRule>> MUTATION =
            ResourceKey.createRegistryKey(new ResourceLocation(Chickens.MODID, "mutation"));

    public static final ResourceKey<Registry<FluidEggEntry>> FLUID_EGG =
            ResourceKey.createRegistryKey(new ResourceLocation(Chickens.MODID, "fluid_egg"));

    public static final ResourceKey<Registry<SpawnRule>> SPAWN_RULE =
            ResourceKey.createRegistryKey(new ResourceLocation(Chickens.MODID, "spawn_rule"));

    private ChickenRegistries() {
    }
}
