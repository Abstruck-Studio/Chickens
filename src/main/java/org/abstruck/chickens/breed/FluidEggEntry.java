package org.abstruck.chickens.breed;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/**
 * 流体蛋定义（数据包注册表 {@code chickens:fluid_egg}）：流体 id + 蛋的渲染颜色。
 */
public record FluidEggEntry(ResourceLocation fluid, int color) {
    public static final Codec<FluidEggEntry> CODEC = RecordCodecBuilder.create(
            (RecordCodecBuilder.Instance<FluidEggEntry> instance) -> instance.group(
                    ResourceLocation.CODEC.fieldOf("fluid").forGetter(FluidEggEntry::fluid),
                    Codec.INT.fieldOf("color").forGetter(FluidEggEntry::color)
            ).apply(instance, FluidEggEntry::new));
}
