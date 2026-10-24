package org.abstruck.chickens.breed;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 四个数据包注册表（breed / mutation / fluid_egg / spawn_rule）在 1.19.2 上的运行时容器。
 *
 * <p>1.19.2 没有 {@code DataPackRegistryEvent.NewRegistry} 这样的同步数据包注册表 API，
 * 所以本模组自行扫描 {@code data/<命名空间>/chickens/<类型>/*.json}，用与 1.21.1 完全相同的
 * Codec / JSON 格式解析，然后把「原始 JSON」通过 {@code chickens:sync_data} 网络包同步给客户端。
 * 数据包覆盖语义不变：同名文件由高优先级数据包覆盖（{@link ResourceManager#listResources} 已按优先级合并）。</p>
 *
 * <p>服务端与客户端共享同一份静态快照，查询入口是 {@link BreedLookups}。</p>
 */
public final class ChickenData {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 数据类型（目录名，data/&lt;ns&gt;/chickens/&lt;type&gt;/*.json） */
    public static final String BREED = "breed";
    public static final String MUTATION = "mutation";
    public static final String FLUID_EGG = "fluid_egg";
    public static final String SPAWN_RULE = "spawn_rule";
    public static final List<String> TYPES = List.of(BREED, MUTATION, FLUID_EGG, SPAWN_RULE);

    private static final Snapshot EMPTY = new Snapshot(Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

    private static volatile Snapshot snapshot = EMPTY;
    /** 最近一次载入的 JSON 原文（服务端用于同步给客户端；key 见 {@link #readRaw}） */
    private static volatile Map<String, String> raw = Map.of();

    private ChickenData() {
    }

    /** 一份完整内容快照（服务端重载或客户端收到同步包后替换） */
    public record Snapshot(
            Map<ResourceLocation, ChickenBreed> breeds,
            Map<ResourceLocation, MutationRule> mutations,
            Map<ResourceLocation, FluidEggEntry> fluidEggs,
            Map<ResourceLocation, SpawnRule> spawnRules,
            Map<BreedLookups.ParentPair, List<MutationRule>> mutationIndex
    ) {
        public int totalCount() {
            return this.breeds.size() + this.mutations.size() + this.fluidEggs.size() + this.spawnRules.size();
        }
    }

    public static Snapshot get() {
        return snapshot;
    }

    /** 最近一次载入的原始 JSON（同步包内容） */
    public static Map<String, String> raw() {
        return raw;
    }

    public static boolean isEmpty() {
        return snapshot == EMPTY || snapshot.breeds().isEmpty();
    }

    /**
     * 扫描资源管理器，读出四个目录下的全部 JSON 原文。
     * key 形如 {@code breed/chickens/flint}，value 为文件内容。
     */
    public static Map<String, String> readRaw(ResourceManager resourceManager) {
        Map<String, String> raw = new LinkedHashMap<>();
        for (String type : TYPES) {
            String prefix = "chickens/" + type + "/";
            Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                    "chickens/" + type, path -> path.getPath().endsWith(".json"));
            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                ResourceLocation file = entry.getKey();
                String path = file.getPath();
                if (!path.startsWith(prefix)) {
                    continue;
                }
                String relative = path.substring(prefix.length(), path.length() - ".json".length());
                String key = type + "/" + file.getNamespace() + "/" + relative;
                try (BufferedReader reader = entry.getValue().openAsReader()) {
                    StringBuilder builder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line).append('\n');
                    }
                    raw.put(key, builder.toString());
                } catch (Exception e) {
                    LOGGER.error("[chickens] 读取数据文件失败 {}: {}", file, e.toString());
                }
            }
        }
        return raw;
    }

    /** 解析原始 JSON 并替换当前快照（服务端重载 / 客户端收到同步包时调用） */
    public static void applyRaw(Map<String, String> rawJson) {
        Map<ResourceLocation, ChickenBreed> breeds = new LinkedHashMap<>();
        Map<ResourceLocation, MutationRule> mutations = new LinkedHashMap<>();
        Map<ResourceLocation, FluidEggEntry> fluidEggs = new LinkedHashMap<>();
        Map<ResourceLocation, SpawnRule> spawnRules = new LinkedHashMap<>();

        List<String> keys = new ArrayList<>(rawJson.keySet());
        keys.sort(String::compareTo);
        for (String key : keys) {
            int firstSlash = key.indexOf('/');
            if (firstSlash < 0) {
                continue;
            }
            String type = key.substring(0, firstSlash);
            // key 形如 <类型>/<命名空间>/<路径>：must not use ResourceLocation.tryParse on the
            // slash separated remainder（1.19.2 的 tryParse 只识别 ':' 分隔，会把整段当作 minecraft 的路径）
            String rest = key.substring(firstSlash + 1);
            int secondSlash = rest.indexOf('/');
            if (secondSlash <= 0 || secondSlash >= rest.length() - 1) {
                LOGGER.error("[chickens] 非法的数据条目路径: {}", key);
                continue;
            }
            ResourceLocation id = new ResourceLocation(rest.substring(0, secondSlash),
                    rest.substring(secondSlash + 1));
            JsonElement json;
            try {
                json = JsonParser.parseString(rawJson.get(key));
            } catch (Exception e) {
                LOGGER.error("[chickens] JSON 解析失败 {}: {}", key, e.toString());
                continue;
            }
            switch (type) {
                case BREED -> parse(ChickenBreed.CODEC, id, json, breeds, key);
                case MUTATION -> parse(MutationRule.CODEC, id, json, mutations, key);
                case FLUID_EGG -> parse(FluidEggEntry.CODEC, id, json, fluidEggs, key);
                case SPAWN_RULE -> parse(SpawnRule.CODEC, id, json, spawnRules, key);
                default -> LOGGER.error("[chickens] 未知的数据类型: {}", key);
            }
        }
        Map<BreedLookups.ParentPair, List<MutationRule>> index = BreedLookups.buildMutationIndex(breeds, mutations);
        raw = Map.copyOf(rawJson);
        snapshot = new Snapshot(
                new LinkedHashMap<>(breeds),
                new LinkedHashMap<>(mutations),
                new LinkedHashMap<>(fluidEggs),
                new LinkedHashMap<>(spawnRules),
                index);
        LOGGER.info("[chickens] 已加载 {} 个品种、{} 条杂交规则、{} 个流体蛋、{} 条生成规则",
                breeds.size(), mutations.size(), fluidEggs.size(), spawnRules.size());
    }

    private static <T> void parse(Codec<T> codec, ResourceLocation id, JsonElement json,
                                  Map<ResourceLocation, T> target, String key) {
        codec.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> LOGGER.error("[chickens] 数据条目 {} 解析失败: {}", key, error))
                .ifPresent(value -> target.put(id, value));
    }

    /** 客户端/服务端切换世界时清空（避免旧存档数据残留） */
    public static void clear() {
        snapshot = EMPTY;
    }

    /** 快照是否包含某个品种 */
    public static boolean hasBreed(ResourceLocation id) {
        return snapshot.breeds().containsKey(id);
    }

    /** 全部品种（按 id 排序，供创造 Tab 与 JEI 稳定输出） */
    public static List<Map.Entry<ResourceLocation, ChickenBreed>> sortedBreeds() {
        List<Map.Entry<ResourceLocation, ChickenBreed>> entries = new ArrayList<>(snapshot.breeds().entrySet());
        entries.sort(Map.Entry.comparingByKey());
        return entries;
    }

}
