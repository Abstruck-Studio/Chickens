package org.abstruck.chickens.api;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.abstruck.chickens.breed.ChickenBreed;
import org.abstruck.chickens.breed.MutationRule;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 把品种/杂交规则写成数据包 JSON 的 datagen Provider（对外 API）。
 *
 * <p>1.20.1 的 {@link DataProvider#run(CachedOutput)} 返回 {@link CompletableFuture}，
 * 输出目录改从 {@link PackOutput} 取。</p>
 */
public final class ChickenBreedProvider implements DataProvider {
    private final DataGenerator generator;
    private final String modId;
    private final Map<ResourceLocation, ChickenBreed> breeds = new LinkedHashMap<>();
    private final Map<ResourceLocation, MutationRule> mutations = new LinkedHashMap<>();

    public ChickenBreedProvider(DataGenerator generator, String modId) {
        this.generator = generator;
        this.modId = modId;
    }

    public ChickenBreedProvider addBreed(ResourceLocation breedId, ChickenBreed breed) {
        breeds.put(breedId, breed);
        return this;
    }

    public ChickenBreedProvider addMutation(ResourceLocation ruleId, MutationRule rule) {
        mutations.put(ruleId, rule);
        return this;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (Map.Entry<ResourceLocation, ChickenBreed> entry : breeds.entrySet()) {
            JsonElement json = ChickenBreed.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue())
                    .getOrThrow(false, error -> {
                    });
            futures.add(DataProvider.saveStable(cache, json, jsonPath(entry.getKey(), "breed")));
        }
        for (Map.Entry<ResourceLocation, MutationRule> entry : mutations.entrySet()) {
            JsonElement json = MutationRule.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue())
                    .getOrThrow(false, error -> {
                    });
            futures.add(DataProvider.saveStable(cache, json, jsonPath(entry.getKey(), "mutation")));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private Path jsonPath(ResourceLocation id, String registryPath) {
        return this.generator.getPackOutput().getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve(this.modId).resolve("chickens").resolve(registryPath)
                .resolve(id.getPath() + ".json");
    }

    @Override
    public String getName() {
        return "Chicken breeds and mutations: " + this.modId;
    }
}
