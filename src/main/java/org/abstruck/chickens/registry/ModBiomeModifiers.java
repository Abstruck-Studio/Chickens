package org.abstruck.chickens.registry;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.world.AddSpawnsToCategoryBiomeModifier;

/** biome modifier 序列化器注册（JSON 的 "type" 用这里的注册名）；
 *  add_spawns_to_category = 官方 add_spawns + 可指定刷怪池类别（下界/末地 monster 通道用） */
public final class ModBiomeModifiers {
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, Chickens.MODID);

    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<AddSpawnsToCategoryBiomeModifier>> ADD_SPAWNS_TO_CATEGORY =
            SERIALIZERS.register("add_spawns_to_category", () -> AddSpawnsToCategoryBiomeModifier.CODEC);

    private ModBiomeModifiers() {
    }
}
