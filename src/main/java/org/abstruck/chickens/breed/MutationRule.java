package org.abstruck.chickens.breed;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 杂交规则：一对父母品种 + 加权结果表。
 * 数据包注册表 {@code chickens:mutation} 的条目，JSON 在
 * {@code data/<数据包命名空间>/chickens/mutation/<规则id>.json}。
 */
public record MutationRule(List<ResourceLocation> parents, List<WeightedResult> results) {

    /** 加权结果：品种 + 相对权重（同一父母对命中的全部规则合并后按权重抽取） */
    public record WeightedResult(ResourceLocation breed, double weight) {
        public static final Codec<WeightedResult> CODEC = RecordCodecBuilder.create(
                (RecordCodecBuilder.Instance<WeightedResult> instance) -> instance.group(
                        ResourceLocation.CODEC.fieldOf("breed").forGetter(WeightedResult::breed),
                        Codec.DOUBLE.fieldOf("weight").forGetter(WeightedResult::weight)
                ).apply(instance, WeightedResult::new));
    }

    public static final Codec<MutationRule> CODEC = RecordCodecBuilder.create(
            (RecordCodecBuilder.Instance<MutationRule> instance) -> instance.group(
                    ResourceLocation.CODEC.listOf().fieldOf("parents").forGetter(MutationRule::parents),
                    WeightedResult.CODEC.listOf().fieldOf("results").forGetter(MutationRule::results)
            ).apply(instance, MutationRule::new));
}
