package org.abstruck.chickens.breed;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * 品种/杂交等数据内容的运行时查询工具。
 *
 * <p>1.21.1 的数据包注册表查询需要 {@code RegistryAccess}；1.19.2 的内容由 {@link ChickenData}
 * 统一持有（服务端重载 / 客户端同步包），因此这里的查询全部是静态的，签名也相应简化。</p>
 */
public final class BreedLookups {
    private static final Logger LOGGER = LogUtils.getLogger();

    private BreedLookups() {
    }

    public static Map<ResourceLocation, ChickenBreed> breedRegistry() {
        return ChickenData.get().breeds();
    }

    public static Map<ResourceLocation, MutationRule> mutationRegistry() {
        return ChickenData.get().mutations();
    }

    public static Map<ResourceLocation, FluidEggEntry> fluidEggRegistry() {
        return ChickenData.get().fluidEggs();
    }

    public static Map<ResourceLocation, SpawnRule> spawnRuleRegistry() {
        return ChickenData.get().spawnRules();
    }

    public static ChickenBreed breed(ResourceLocation id) {
        return id == null ? null : ChickenData.get().breeds().get(normalizeBreedId(id));
    }

    /** 旧品种 id 归一化：silver_dye → light_gray、log → oak_log（旧存档/旧数据包兼容），其余原样返回 */
    public static ResourceLocation normalizeBreedId(ResourceLocation id) {
        if (id != null && id.getNamespace().equals("chickens")) {
            if (id.getPath().equals("silver_dye")) {
                return new ResourceLocation("chickens", "light_gray");
            }
            if (id.getPath().equals("log")) {
                return new ResourceLocation("chickens", "oak_log");
            }
        }
        return id;
    }

    // ------------------------------------------------------------------ 流体

    /** 查流体蛋的渲染颜色，找不到返回空 */
    public static OptionalInt colorOfFluid(ResourceLocation fluidId) {
        if (fluidId == null) {
            return OptionalInt.empty();
        }
        for (FluidEggEntry entry : ChickenData.get().fluidEggs().values()) {
            if (entry.fluid().equals(fluidId)) {
                return OptionalInt.of(entry.color());
            }
        }
        return OptionalInt.empty();
    }

    /** 流体 id → 已注册流体；不存在/为空返回 null */
    public static Fluid fluidOf(ResourceLocation fluidId) {
        if (fluidId == null) {
            return null;
        }
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
        if (fluid == null || fluid == Fluids.EMPTY) {
            LOGGER.warn("[chickens] 品种引用了不存在的流体: {}", fluidId);
            return null;
        }
        return fluid;
    }

    /** 解析流体引用（精确 id 或标签）；标签从已注册流体随机抽一个；解析失败记日志并返回 null */
    public static Fluid fluidOf(ChickenBreed.FluidRef ref, RandomSource random) {
        if (ref.fluidId().isPresent()) {
            return fluidOf(ref.fluidId().get());
        }
        TagKey<Fluid> tag = ref.tag().orElse(null);
        if (tag != null) {
            for (Fluid fluid : ForgeRegistries.FLUIDS.tags().getTag(tag)) {
                if (fluid != Fluids.EMPTY) {
                    return fluid;
                }
            }
            LOGGER.warn("[chickens] 品种引用了不存在的流体标签（矿词）: {}", tag.location());
            return null;
        }
        LOGGER.warn("[chickens] 品种引用了不存在的流体标签（矿词）: {}", tag);
        return null;
    }

    /** 展示用（JEI 等无随机源场景）：标签取第一个已注册流体；解析失败返回 null */
    public static Fluid fluidOfForDisplay(ChickenBreed.FluidRef ref) {
        if (ref.fluidId().isPresent()) {
            return fluidOf(ref.fluidId().get());
        }
        TagKey<Fluid> tag = ref.tag().orElse(null);
        if (tag != null) {
            for (Fluid fluid : ForgeRegistries.FLUIDS.tags().getTag(tag)) {
                if (fluid != Fluids.EMPTY) {
                    return fluid;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ 物品

    /** 解析产物物品引用（精确 id 或标签）；标签从已注册物品随机抽一个；解析失败记日志并返回 null */
    public static Item resolveItem(ChickenBreed.ItemRef ref, RandomSource random) {
        if (ref.itemId().isPresent()) {
            ResourceLocation itemId = ref.itemId().get();
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null || item == Items.AIR) {
                LOGGER.warn("[chickens] 品种引用了不存在的物品: {}", itemId);
                return null;
            }
            return item;
        }
        TagKey<Item> tag = ref.tag().orElse(null);
        if (tag != null) {
            List<Item> members = new ArrayList<>();
            for (Item item : ForgeRegistries.ITEMS.tags().getTag(tag)) {
                if (item != Items.AIR) {
                    members.add(item);
                }
            }
            if (!members.isEmpty()) {
                return members.get(random.nextInt(members.size()));
            }
        }
        LOGGER.warn("[chickens] 品种引用了不存在的物品标签（矿词）: {}",
                tag == null ? null : tag.location());
        return null;
    }

    /** 展示用（JEI 等无随机源场景）：标签取第一个已注册物品（显示必须稳定） */
    public static Item resolveItemForDisplay(ChickenBreed.ItemRef ref) {
        if (ref.itemId().isPresent()) {
            Item item = ForgeRegistries.ITEMS.getValue(ref.itemId().get());
            return item == Items.AIR ? null : item;
        }
        TagKey<Item> tag = ref.tag().orElse(null);
        if (tag != null) {
            for (Item item : ForgeRegistries.ITEMS.tags().getTag(tag)) {
                if (item != Items.AIR) {
                    return item;
                }
            }
        }
        return null;
    }

    /**
     * 按力量抽取产物：主产物有效权重 = max(1, weight) × 力量，力量 ≥ 10 必出主产物；无副产物恒主产物。
     */
    public static ChickenBreed.Product pickOutputItem(ChickenBreed breed, int strength, RandomSource random) {
        List<ChickenBreed.Product> subs = breed.byproducts();
        int mainWeight = Math.max(1, breed.item().weight()) * strength;
        if (subs.isEmpty() || strength >= 10) {
            return breed.item();
        }
        int total = mainWeight;
        for (ChickenBreed.Product sub : subs) {
            total += sub.weight();
        }
        int roll = random.nextInt(total);
        if (roll < mainWeight) {
            return breed.item();
        }
        roll -= mainWeight;
        for (ChickenBreed.Product sub : subs) {
            roll -= sub.weight();
            if (roll < 0) {
                return sub;
            }
        }
        return subs.get(subs.size() - 1);
    }

    // ------------------------------------------------------------------ 杂交

    /** 父母对（构造时顺序规范化，与放槽顺序无关） */
    public record ParentPair(ResourceLocation first, ResourceLocation second) {
        public static ParentPair of(ResourceLocation a, ResourceLocation b) {
            return a.compareTo(b) <= 0 ? new ParentPair(a, b) : new ParentPair(b, a);
        }
    }

    /** 品种标签的成员：品种 JSON 自声明 tags 字段包含该标签的品种 */
    public static List<ResourceLocation> breedsWithTag(TagKey<ChickenBreed> tag) {
        return breedsWithTag(ChickenData.get().breeds(), tag);
    }

    /** 同上，但指定品种表（构建索引时传入"正在解析中的"品种表，此时静态快照尚未替换） */
    private static List<ResourceLocation> breedsWithTag(Map<ResourceLocation, ChickenBreed> breeds,
                                                        TagKey<ChickenBreed> tag) {
        List<ResourceLocation> members = new ArrayList<>();
        for (Map.Entry<ResourceLocation, ChickenBreed> entry : breeds.entrySet()) {
            if (entry.getValue().tags().contains(tag)) {
                members.add(entry.getKey());
            }
        }
        return members;
    }

    /** 预计算「父母对 → 命中规则」索引：#标签 父母展开为全部成员品种组合；引用不存在的品种只记日志跳过（不崩服） */
    public static Map<ParentPair, List<MutationRule>> buildMutationIndex(
            Map<ResourceLocation, ChickenBreed> breeds, Map<ResourceLocation, MutationRule> mutations) {
        Map<ParentPair, List<MutationRule>> index = new HashMap<>();
        Map<TagKey<ChickenBreed>, List<ResourceLocation>> tagCache = new HashMap<>();
        for (Map.Entry<ResourceLocation, MutationRule> entry : mutations.entrySet()) {
            MutationRule rule = entry.getValue();
            ResourceLocation ruleId = entry.getKey();
            if (rule.parents().size() != 2) {
                LOGGER.error("[chickens] 杂交规则 {} 的 parents 必须恰好 2 个，已跳过", ruleId);
                continue;
            }
            for (MutationRule.WeightedResult result : rule.results()) {
                MutationRule.ResultRef ref = result.breed();
                if (ref.breedId().isPresent() && !breeds.containsKey(ref.breedId().get())) {
                    LOGGER.error("[chickens] 杂交规则 {} 引用了不存在的品种 {}", ruleId, ref.breedId().get());
                }
            }
            List<ResourceLocation> sideA = expandParents(breeds, tagCache, rule.parents().get(0), ruleId);
            List<ResourceLocation> sideB = expandParents(breeds, tagCache, rule.parents().get(1), ruleId);
            for (ResourceLocation a : sideA) {
                for (ResourceLocation b : sideB) {
                    if (!a.equals(b)) {
                        index.computeIfAbsent(ParentPair.of(a, b), k -> new ArrayList<>()).add(rule);
                    }
                }
            }
        }
        return index;
    }

    /** 父母引用展开：精确 id → 单元素；#标签 → 全部成员；引用不存在时记日志并返回空表（该规则作废） */
    private static List<ResourceLocation> expandParents(Map<ResourceLocation, ChickenBreed> breeds,
                                                       Map<TagKey<ChickenBreed>, List<ResourceLocation>> tagCache,
                                                       MutationRule.BreedRef ref, ResourceLocation ruleId) {
        if (ref.breedId().isPresent()) {
            ResourceLocation id = ref.breedId().get();
            if (!breeds.containsKey(id)) {
                LOGGER.error("[chickens] 杂交规则 {} 引用了不存在的品种 {}", ruleId, id);
                return List.of();
            }
            return List.of(id);
        }
        TagKey<ChickenBreed> tag = ref.tag().orElseThrow();
        List<ResourceLocation> members = tagCache.computeIfAbsent(tag, t -> breedsWithTag(breeds, t));
        if (members.isEmpty()) {
            LOGGER.error("[chickens] 杂交规则 {} 引用了没有成员的品种标签 #{}", ruleId, tag.location());
        }
        return members;
    }

    /** 命中规则的合并结果表；无规则命中返回 Optional.empty()（调用方退回按 tier 回退） */
    public static Optional<List<MutationRule.WeightedResult>> findResults(ResourceLocation breedA, ResourceLocation breedB) {
        List<MutationRule> rules = ChickenData.get().mutationIndex().get(ParentPair.of(breedA, breedB));
        if (rules == null) {
            return Optional.empty();
        }
        List<MutationRule.WeightedResult> results = new ArrayList<>();
        for (MutationRule rule : rules) {
            results.addAll(rule.results());
        }
        return Optional.of(results);
    }

    /** 按权重抽取一个结果；结果表为空返回 Optional.empty()；$parent: 标记结合本次实际父母解析 */
    public static Optional<ResourceLocation> roll(Optional<List<MutationRule.WeightedResult>> results,
                                                  ResourceLocation breedA, ResourceLocation breedB,
                                                  RandomSource random) {
        if (results.isEmpty()) {
            return Optional.empty();
        }
        List<MutationRule.WeightedResult> list = results.get();
        double total = 0.0;
        for (MutationRule.WeightedResult result : list) {
            total += result.weight();
        }
        double roll = random.nextDouble() * total;
        MutationRule.WeightedResult picked = list.get(list.size() - 1);
        for (MutationRule.WeightedResult result : list) {
            roll -= result.weight();
            if (roll < 0) {
                picked = result;
                break;
            }
        }
        return Optional.of(resolveResult(picked.breed(), breedA, breedB, random));
    }

    /** 精确 id 原样返回；$parent:标签 → 实际父母中带该标签的那只（都带随机取一；都没带记日志兜底） */
    private static ResourceLocation resolveResult(MutationRule.ResultRef ref,
                                                  ResourceLocation breedA, ResourceLocation breedB,
                                                  RandomSource random) {
        if (ref.breedId().isPresent()) {
            return ref.breedId().get();
        }
        TagKey<ChickenBreed> tag = ref.tag().orElseThrow();
        List<ResourceLocation> candidates = new ArrayList<>(2);
        for (ResourceLocation id : List.of(breedA, breedB)) {
            ChickenBreed breed = ChickenData.get().breeds().get(id);
            if (breed != null && breed.tags().contains(tag)) {
                candidates.add(id);
            }
        }
        if (candidates.isEmpty()) {
            LOGGER.warn("[chickens] 杂交结果的 $parent:{} 标记在本次父母 {} × {} 里没有匹配品种，随机取一只兜底",
                    tag.location(), breedA, breedB);
            candidates.addAll(List.of(breedA, breedB));
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    // ------------------------------------------------------------------ 自然生成

    /** 合并命中规则的品种权重抽取；密度 chance 取命中规则最小值；未命中规则或未通过密度判定返回 Optional.empty() */
    public static Optional<ResourceLocation> pickSpawnBreed(ResourceLocation dimension,
                                                            Holder<Biome> biome, RandomSource random) {
        List<SpawnRule.WeightedBreed> pool = new ArrayList<>();
        double chance = 1.0;
        for (SpawnRule rule : ChickenData.get().spawnRules().values()) {
            if (matchesDimension(rule, dimension) && matchesBiome(rule, biome)) {
                pool.addAll(rule.breeds());
                chance = Math.min(chance, rule.chance().orElse(1.0));
            }
        }
        if (pool.isEmpty() || chance <= 0.0 || random.nextDouble() >= chance) {
            return Optional.empty();
        }
        double total = 0.0;
        for (SpawnRule.WeightedBreed entry : pool) {
            total += entry.weight();
        }
        double roll = random.nextDouble() * total;
        for (SpawnRule.WeightedBreed entry : pool) {
            roll -= entry.weight();
            if (roll < 0) {
                return Optional.of(entry.breed());
            }
        }
        return Optional.of(pool.get(pool.size() - 1).breed());
    }

    private static boolean matchesDimension(SpawnRule rule, ResourceLocation dimension) {
        return rule.dimension().isEmpty() || rule.dimension().get().equals(dimension);
    }

    private static boolean matchesBiome(SpawnRule rule, Holder<Biome> biome) {
        if (rule.biomes().isEmpty()) {
            return true;
        }
        for (String entry : rule.biomes()) {
            if (entry.startsWith("#")) {
                TagKey<Biome> tag = TagKey.create(Registries.BIOME, new ResourceLocation(entry.substring(1)));
                if (biome.is(tag)) {
                    return true;
                }
            } else if (biome.unwrapKey().map(key -> key.location().equals(new ResourceLocation(entry)))
                    .orElse(false)) {
                return true;
            }
        }
        return false;
    }
}
