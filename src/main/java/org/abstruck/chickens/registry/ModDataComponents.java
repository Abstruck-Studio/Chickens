package org.abstruck.chickens.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.breed.BreedLookups;
import org.abstruck.chickens.entity.chicken.ChickenStats;

import java.util.function.Function;

/** 物品组件注册（1.20.5+ 物品自定义数据必须走 DataComponentType，不能直接写 NBT） */
public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Chickens.MODID);

    /** 鸡物品的品种引用（读取时经 {@link BreedLookups#normalizeBreedId} 归一化旧 id） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> BREED =
            DATA_COMPONENTS.register("breed", () -> DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC.xmap(BreedLookups::normalizeBreedId, Function.identity()))
                    .networkSynchronized(
                            ResourceLocation.STREAM_CODEC.map(BreedLookups::normalizeBreedId, Function.identity()))
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ChickenStats>> STATS =
            DATA_COMPONENTS.register("stats", () -> DataComponentType.<ChickenStats>builder()
                    .persistent(ChickenStats.CODEC)
                    .networkSynchronized(ChickenStats.STREAM_CODEC)
                    .build());

    /** 成熟标记（true = 成年；缺省按成年处理） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> MATURE =
            DATA_COMPONENTS.register("mature", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    /** 流体蛋的流体（存 Holder&lt;Fluid&gt;：组件值必须实现 equals/hashCode，FluidStack 不满足会被组件校验拒绝） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.core.Holder<Fluid>>> FLUID =
            DATA_COMPONENTS.register("fluid", () -> DataComponentType.<net.minecraft.core.Holder<Fluid>>builder()
                    .persistent(FluidStack.FLUID_NON_EMPTY_CODEC)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.holderRegistry(Registries.FLUID))
                    .build());

    private ModDataComponents() {
    }
}
