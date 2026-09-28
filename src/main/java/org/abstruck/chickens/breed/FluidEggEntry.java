package org.abstruck.chickens.breed;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/**
 * 流体蛋定义（数据包注册表 {@code chickens:fluid_egg} 的条目）：
 * 流体 id + 蛋的渲染颜色（RGB，渲染时补 alpha）。
 * JSON：{@code data/<命名空间>/chickens/fluid_egg/<id>.json}——
 * 其他模组可在自己的 jar 里带同名 JSON 给本模组增加流体蛋（零代码兼容）。
 * 网络 codec 复用加载 codec（21.1.252 的 dataPackRegistry 第三参数是 Codec）。
 */
public record FluidEggEntry(ResourceLocation fluid, int color) {
    public static final Codec<FluidEggEntry> CODEC = RecordCodecBuilder.create(
            (RecordCodecBuilder.Instance<FluidEggEntry> instance) -> instance.group(
                    ResourceLocation.CODEC.fieldOf("fluid").forGetter(FluidEggEntry::fluid),
                    Codec.INT.fieldOf("color").forGetter(FluidEggEntry::color)
            ).apply(instance, FluidEggEntry::new));
}
