package org.abstruck.chickens.api;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.abstruck.chickens.breed.ChickenBreed;
import org.abstruck.chickens.breed.MutationRule;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 把品种/杂交规则写成数据包 JSON 的 datagen Provider（对外 API 的一部分）。
 * 用法：其他模组在 GatherDataEvent 里 {@code event.addProvider(new ChickenBreedProvider(event.getGenerator().getPackOutput(), MODID).addBreed(...))}。
 * 输出路径：{@code data/<modId>/chickens/breed/<id>.json}（注册表键命名空间是 chickens，所以中间一段固定是 chickens）。
 */
public final class ChickenBreedProvider implements DataProvider {
    private final PackOutput output;
    private final String modId;
    private final Map<ResourceLocation, ChickenBreed> breeds = new LinkedHashMap<>();
    private final Map<ResourceLocation, MutationRule> mutations = new LinkedHashMap<>();

    public ChickenBreedProvider(PackOutput output, String modId) {
        this.output = output;
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
        CompletableFuture<?>[] writes = new CompletableFuture<?>[breeds.size() + mutations.size()];
        int i = 0;
        for (Map.Entry<ResourceLocation, ChickenBreed> entry : breeds.entrySet()) {
            JsonElement json = ChickenBreed.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue()).getOrThrow();
            writes[i++] = DataProvider.saveStable(cache, json, jsonPath(entry.getKey(), "breed"));
        }
        for (Map.Entry<ResourceLocation, MutationRule> entry : mutations.entrySet()) {
            JsonElement json = MutationRule.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue()).getOrThrow();
            writes[i++] = DataProvider.saveStable(cache, json, jsonPath(entry.getKey(), "mutation"));
        }
        return CompletableFuture.allOf(writes);
    }

    private Path jsonPath(ResourceLocation id, String registryPath) {
        return output.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve(modId).resolve("chickens").resolve(registryPath)
                .resolve(id.getPath() + ".json");
    }

    @Override
    public String getName() {
        return "Chicken breeds and mutations: " + modId;
    }
}
