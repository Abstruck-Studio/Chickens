package org.abstruck.chickens.world;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import org.abstruck.chickens.registry.ModRegistry;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 自定义 biome modifier：与 {@code forge:add_spawns} 同构，额外支持 {@code category}
 * 字段指定注入的刷怪池类别（官方固定取实体自身类别，够不着下界/末地的 monster 池）。
 * 叠加式注入，可被任意数据包复用。
 */
public record AddSpawnsToCategoryBiomeModifier(HolderSet<Biome> biomes, Optional<MobCategory> category,
                                               List<SpawnerData> spawners) implements BiomeModifier {
    /** 与官方 add_spawns 字段对齐（spawners 允许单条或列表），仅多 category */
    public static final Codec<AddSpawnsToCategoryBiomeModifier> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(AddSpawnsToCategoryBiomeModifier::biomes),
                    MobCategory.CODEC.optionalFieldOf("category")
                            .forGetter(AddSpawnsToCategoryBiomeModifier::category),
                    Codec.either(SpawnerData.CODEC.listOf(), SpawnerData.CODEC).xmap(
                            either -> either.map(Function.identity(), List::of),
                            list -> list.size() == 1 ? Either.right(list.get(0)) : Either.left(list)
                    ).fieldOf("spawners").forGetter(AddSpawnsToCategoryBiomeModifier::spawners)
            ).apply(instance, AddSpawnsToCategoryBiomeModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD && this.biomes.contains(biome)) {
            for (SpawnerData spawner : this.spawners) {
                builder.getMobSpawnSettings().addSpawn(
                        this.category.orElse(spawner.type.getCategory()), spawner);
            }
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return ModRegistry.ADD_SPAWNS_TO_CATEGORY.get();
    }
}
