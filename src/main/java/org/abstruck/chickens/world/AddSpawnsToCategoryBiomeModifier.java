package org.abstruck.chickens.world;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.common.world.MobSpawnSettingsBuilder;
import org.abstruck.chickens.registry.ModBiomeModifiers;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 自定义 biome modifier：与 {@code neoforge:add_spawns} 同构，额外支持 {@code category}
 * 字段指定注入的刷怪池类别（官方固定取实体自身类别，够不着下界/末地的 monster 池）。
 * 叠加式注入，可被任意数据包复用。
 */
public record AddSpawnsToCategoryBiomeModifier(HolderSet<Biome> biomes, Optional<MobCategory> category,
        List<SpawnerData> spawners) implements BiomeModifier {
    /** 与官方 add_spawns 字段对齐（spawners 允许单条或列表），仅多 category */
    public static final MapCodec<AddSpawnsToCategoryBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(AddSpawnsToCategoryBiomeModifier::biomes),
                    MobCategory.CODEC.optionalFieldOf("category").forGetter(AddSpawnsToCategoryBiomeModifier::category),
                    Codec.either(SpawnerData.CODEC.listOf(), SpawnerData.CODEC).xmap(
                            either -> either.map(Function.identity(), List::of),
                            list -> list.size() == 1 ? Either.right(list.get(0)) : Either.left(list)
                    ).fieldOf("spawners").forGetter(AddSpawnsToCategoryBiomeModifier::spawners)
            ).apply(instance, AddSpawnsToCategoryBiomeModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD && this.biomes.contains(biome)) {
            MobSpawnSettingsBuilder spawns = builder.getMobSpawnSettings();
            for (SpawnerData spawner : this.spawners) {
                spawns.addSpawn(this.category.orElse(spawner.type.getCategory()), spawner);
            }
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return ModBiomeModifiers.ADD_SPAWNS_TO_CATEGORY.get();
    }
}
