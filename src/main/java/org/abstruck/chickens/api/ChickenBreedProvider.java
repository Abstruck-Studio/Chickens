package org.abstruck.chickens.api;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import org.abstruck.chickens.breed.ChickenBreed;
import org.abstruck.chickens.breed.MutationRule;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 把品种/杂交规则写成数据包 JSON 的 datagen Provider（对外 API）。
 *
 * <p>1.19.2 的 {@link DataProvider} 是旧版接口（{@code run(CachedOutput)} 返回 void，
 * 路径从 {@link DataGenerator} 取），因此与 1.21.1 版的构造参数不同：这里接收 {@link DataGenerator}。</p>
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
    public void run(CachedOutput cache) throws IOException {
        for (Map.Entry<ResourceLocation, ChickenBreed> entry : breeds.entrySet()) {
            JsonElement json = ChickenBreed.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue())
                    .getOrThrow(false, error -> {
                    });
            DataProvider.saveStable(cache, json, jsonPath(entry.getKey(), "breed"));
        }
        for (Map.Entry<ResourceLocation, MutationRule> entry : mutations.entrySet()) {
            JsonElement json = MutationRule.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue())
                    .getOrThrow(false, error -> {
                    });
            DataProvider.saveStable(cache, json, jsonPath(entry.getKey(), "mutation"));
        }
    }

    private Path jsonPath(ResourceLocation id, String registryPath) {
        return this.generator.getOutputFolder(DataGenerator.Target.DATA_PACK)
                .resolve(this.modId).resolve("chickens").resolve(registryPath)
                .resolve(id.getPath() + ".json");
    }

    @Override
    public String getName() {
        return "Chicken breeds and mutations: " + this.modId;
    }
}
