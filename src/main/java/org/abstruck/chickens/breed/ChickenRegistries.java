package org.abstruck.chickens.breed;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import org.abstruck.chickens.Chickens;

/** 数据包注册表的键与注册入口 */
public final class ChickenRegistries {
    public static final ResourceKey<Registry<ChickenBreed>> BREED =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Chickens.MODID, "breed"));

    public static final ResourceKey<Registry<MutationRule>> MUTATION =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Chickens.MODID, "mutation"));

    public static final ResourceKey<Registry<FluidEggEntry>> FLUID_EGG =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Chickens.MODID, "fluid_egg"));

    public static final ResourceKey<Registry<SpawnRule>> SPAWN_RULE =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Chickens.MODID, "spawn_rule"));

    private ChickenRegistries() {
    }

    public static void registerDataPackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(BREED, ChickenBreed.CODEC, ChickenBreed.CODEC);
        event.dataPackRegistry(MUTATION, MutationRule.CODEC, MutationRule.CODEC);
        event.dataPackRegistry(FLUID_EGG, FluidEggEntry.CODEC, FluidEggEntry.CODEC);
        event.dataPackRegistry(SPAWN_RULE, SpawnRule.CODEC, SpawnRule.CODEC);
    }
}
