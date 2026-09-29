package org.abstruck.chickens.breed;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * 自然生成规则（数据包注册表 {@code chickens:spawn_rule}）：
 * 一条规则 = 维度过滤 + 群系过滤 + 品种加权表 + 生成通过率 chance。
 * 同一生成位置命中多条规则时所有权重合并后抽取（增量友好：加新规则即加新品种，
 * 无需覆盖整条）；chance 取所有命中规则的<b>最小值</b>（瓶颈语义：密度由最严的规则决定，
 * 只加品种的规则不写 chance 不影响密度）。
 * <p>
 * 生成入口（鸡能否进入某群系的生成池）由 {@code #chickens:spawnable_biomes}
 * 标签 + biome modifier 控制；本表决定「是否生成 / 生成哪只鸡」——
 * 未命中任何规则时该次生成被否决（规则表是唯一真相）。
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
