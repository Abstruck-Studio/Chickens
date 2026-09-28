package org.abstruck.chickens.breed;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import org.abstruck.chickens.Chickens;

/**
 * 本模组的两个数据包注册表的键与注册入口。
 * 注意：NeoForge 21.1.252 里这个事件类叫 {@link DataPackRegistryEvent.NewRegistry}，
 * 不是旧版的 RegisterDataPackRegistryEvent。
 */
public final class ChickenRegistries {
    /** chickens:breed —— 鸡品种。JSON：data/&lt;ns&gt;/chickens/breed/&lt;id&gt;.json */
    public static final ResourceKey<Registry<ChickenBreed>> BREED =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Chickens.MODID, "breed"));

    /** chickens:mutation —— 杂交规则。JSON：data/&lt;ns&gt;/chickens/mutation/&lt;id&gt;.json */
    public static final ResourceKey<Registry<MutationRule>> MUTATION =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Chickens.MODID, "mutation"));

    /** chickens:fluid_egg —— 流体蛋定义（流体 id + 渲染颜色）。
     *  JSON：data/&lt;ns&gt;/chickens/fluid_egg/&lt;id&gt;.json；其他模组可带 JSON 增加流体蛋 */
    public static final ResourceKey<Registry<FluidEggEntry>> FLUID_EGG =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Chickens.MODID, "fluid_egg"));

    private ChickenRegistries() {
    }

    /**
     * 在主类构造器里调用：{@code modEventBus.addListener(ChickenRegistries::registerDataPackRegistries)}
     * 全部注册表都带网络 codec（与加载 codec 同构），会自动同步到客户端。
     */
    public static void registerDataPackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(BREED, ChickenBreed.CODEC, ChickenBreed.CODEC);
        event.dataPackRegistry(MUTATION, MutationRule.CODEC, MutationRule.CODEC);
        event.dataPackRegistry(FLUID_EGG, FluidEggEntry.CODEC, FluidEggEntry.CODEC);
    }
}
