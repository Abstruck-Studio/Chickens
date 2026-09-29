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
import org.abstruck.chickens.entity.chicken.ChickenStats;

/**
 * 物品组件注册。1.20.5+ 物品上的自定义数据必须走 DataComponentType（不能直接写 NBT）。
 */
public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Chickens.MODID);

    /** 鸡物品 / 受精蛋上的品种引用 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> BREED =
            DATA_COMPONENTS.register("breed", () -> DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build());

    /** 鸡物品 / 受精蛋上的三维属性快照 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ChickenStats>> STATS =
            DATA_COMPONENTS.register("stats", () -> DataComponentType.<ChickenStats>builder()
                    .persistent(ChickenStats.CODEC)
                    .networkSynchronized(ChickenStats.STREAM_CODEC)
                    .build());

    /** 鸡物品上的成熟标记（true = 成年；缺省按成年处理。图标渲染与放出时据此显示/生成幼体） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> MATURE =
            DATA_COMPONENTS.register("mature", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    /** 流体蛋上的流体（Holder<Fluid>，原版 potion_contents 同款——组件值必须实现 equals/hashCode，
     *  FluidStack 不满足会被 NeoForge 组件校验拒绝）：渲染颜色、倒出液体与物品流体能力都由它决定 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.core.Holder<Fluid>>> FLUID =
            DATA_COMPONENTS.register("fluid", () -> DataComponentType.<net.minecraft.core.Holder<Fluid>>builder()
                    .persistent(FluidStack.FLUID_NON_EMPTY_CODEC)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.holderRegistry(Registries.FLUID))
                    .build());

    private ModDataComponents() {
    }
}
