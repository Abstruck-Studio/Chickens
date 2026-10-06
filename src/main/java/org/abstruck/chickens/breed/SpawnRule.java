package org.abstruck.chickens.breed;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * 自然生成规则（数据包注册表 {@code chickens:spawn_rule}）：维度/群系过滤 + 品种加权表 + 通过率 chance。
 * 多条命中时权重合并抽取，chance 取所有命中规则的最小值（瓶颈语义）。
 * 未命中任何规则时否决该次生成；能否进入某群系的生成池由 {@code #chickens:spawnable_biomes} 标签 + biome modifier 控制。
 */
public record SpawnRule(
        Optional<ResourceLocation> dimension,
        List<String> biomes,
        List<WeightedBreed> breeds,
        Optional<Double> chance) {

    /** 品种权重对（抽取语义与 mutation 的 results 相同） */
    public record WeightedBreed(ResourceLocation breed, int weight) {
        public static final Codec<WeightedBreed> CODEC = RecordCodecBuilder.create(
                (RecordCodecBuilder.Instance<WeightedBreed> instance) -> instance.group(
                        ResourceLocation.CODEC.fieldOf("breed").forGetter(WeightedBreed::breed),
                        Codec.INT.fieldOf("weight").forGetter(WeightedBreed::weight)
                ).apply(instance, WeightedBreed::new));
    }

    public static final Codec<SpawnRule> CODEC = RecordCodecBuilder.create(
            (RecordCodecBuilder.Instance<SpawnRule> instance) -> instance.group(
                    ResourceLocation.CODEC.optionalFieldOf("dimension").forGetter(SpawnRule::dimension),
                    Codec.STRING.listOf().optionalFieldOf("biomes", List.of()).forGetter(SpawnRule::biomes),
                    WeightedBreed.CODEC.listOf().fieldOf("breeds").forGetter(SpawnRule::breeds),
                    Codec.DOUBLE.optionalFieldOf("chance").forGetter(SpawnRule::chance)
            ).apply(instance, SpawnRule::new));
}
