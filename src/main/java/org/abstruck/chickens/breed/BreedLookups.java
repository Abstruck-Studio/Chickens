package org.abstruck.chickens.breed;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * 品种/杂交等数据包注册表的运行时查询工具。
 * 数据里的引用惰性保存，查询一律需要传入 RegistryAccess
 * （服务端 ServerLevel#registryAccess，客户端 ClientPacketListener#registryAccess）。
 */
public final class BreedLookups {
    private static final Logger LOGGER = LogUtils.getLogger();

    private BreedLookups() {
    }

    public static Registry<ChickenBreed> breedRegistry(RegistryAccess access) {
        return access.registryOrThrow(ChickenRegistries.BREED);
    }

    public static Registry<MutationRule> mutationRegistry(RegistryAccess access) {
        return access.registryOrThrow(ChickenRegistries.MUTATION);
    }

    public static Registry<FluidEggEntry> fluidEggRegistry(RegistryAccess access) {
        return access.registryOrThrow(ChickenRegistries.FLUID_EGG);
    }

    public static Registry<SpawnRule> spawnRuleRegistry(RegistryAccess access) {
        return access.registryOrThrow(ChickenRegistries.SPAWN_RULE);
    }

    /** 旧品种 id 归一化：silver_dye → light_gray、log → oak_log（旧存档/旧数据包兼容），其余原样返回 */
    public static ResourceLocation normalizeBreedId(ResourceLocation id) {
        if (id.getNamespace().equals("chickens")) {
            if (id.getPath().equals("silver_dye")) {
                return ResourceLocation.fromNamespaceAndPath("chickens", "light_gray");
            }
            if (id.getPath().equals("log")) {
                return ResourceLocation.fromNamespaceAndPath("chickens", "oak_log");
            }
        }
        return id;
    }

    /** 查流体蛋的渲染颜色，找不到返回空（参数用 HolderLookup.Provider：物品 tooltip 路径只提供它，RegistryAccess 是其子类型） */
    public static OptionalInt colorOfFluid(HolderLookup.Provider registries, ResourceLocation fluidId) {
        Optional<HolderLookup.RegistryLookup<FluidEggEntry>> lookup = registries.lookup(ChickenRegistries.FLUID_EGG);
        if (lookup.isEmpty()) {
            return OptionalInt.empty();
        }
        Iterator<Holder.Reference<FluidEggEntry>> iterator = lookup.get().listElements().iterator();
        while (iterator.hasNext()) {
            FluidEggEntry entry = iterator.next().value();
            if (entry.fluid().equals(fluidId)) {
                return OptionalInt.of(entry.color());
            }
        }
        return OptionalInt.empty();
    }

    /** 解析流体 id 为 Holder&lt;Fluid&gt;（写入 chickens:fluid 组件用）；解析失败记日志并返回 null */
    public static net.minecraft.core.Holder<Fluid> fluidHolderOf(net.minecraft.core.HolderLookup.Provider provider, ResourceLocation fluidId) {
        net.minecraft.core.Holder<Fluid> holder = provider.lookupOrThrow(Registries.FLUID)
                .get(ResourceKey.create(Registries.FLUID, fluidId))
                .orElse(null);
        if (holder == null || holder.value() == net.minecraft.world.level.material.Fluids.EMPTY) {
            LOGGER.warn("[chickens] 品种引用了不存在的流体: {}", fluidId);
            return null;
        }
        return holder;
    }

    /** 解析流体引用（精确 id 或标签）；标签从已注册流体随机抽一个；解析失败记日志并返回 null */
    public static net.minecraft.core.Holder<Fluid> fluidHolderOf(
            net.minecraft.core.HolderLookup.Provider provider, ChickenBreed.FluidRef ref, RandomSource random) {
        if (ref.fluidId().isPresent()) {
            return fluidHolderOf(provider, ref.fluidId().get());
        }
        TagKey<Fluid> tag = ref.tag().orElse(null);
        if (tag != null) {
            net.minecraft.core.Holder<Fluid> holder = provider.lookupOrThrow(Registries.FLUID).get(tag)
                    .flatMap(set -> set.getRandomElement(random))
                    .orElse(null);
            if (holder != null && holder.value() != net.minecraft.world.level.material.Fluids.EMPTY) {
                return holder;
            }
        }
        LOGGER.warn("[chickens] 品种引用了不存在的流体标签（矿词）: {}", tag);
        return null;
    }

    /** 展示用（JEI 等无随机源场景）：流体标签取第一个已注册流体（保证显示稳定）；解析失败返回 null */
    public static net.minecraft.core.Holder<Fluid> fluidHolderOfForDisplay(
            net.minecraft.core.HolderLookup.Provider provider, ChickenBreed.FluidRef ref) {
        if (ref.fluidId().isPresent()) {
            return fluidHolderOf(provider, ref.fluidId().get());
        }
        TagKey<Fluid> tag = ref.tag().orElse(null);
        if (tag != null) {
            java.util.Optional<net.minecraft.core.HolderSet.Named<Fluid>> set =
                    provider.lookupOrThrow(Registries.FLUID).get(tag);
            if (set.isPresent()) {
                for (net.minecraft.core.Holder<Fluid> holder : set.get()) {
                    if (holder.value() != net.minecraft.world.level.material.Fluids.EMPTY) {
                        return holder;
                    }
                }
            }
        }
        return null;
    }

    /** 解析产物物品引用（精确 id 或标签）；标签从已注册物品随机抽一个；解析失败记日志并返回 null */
    public static Item resolveItem(RegistryAccess access, ChickenBreed.ItemRef ref, RandomSource random) {
        Registry<Item> registry = access.registryOrThrow(Registries.ITEM);
        if (ref.itemId().isPresent()) {
            ResourceLocation itemId = ref.itemId().get();
            Item item = registry.get(itemId);
            if (item == null) {
                LOGGER.warn("[chickens] 品种引用了不存在的物品: {}", itemId);
            }
            return item;
        }
        TagKey<Item> tag = ref.tag().orElse(null);
        if (tag != null) {
            Holder<Item> holder = registry.getRandomElementOf(tag, random).orElse(null);
            if (holder != null) {
                return holder.value();
            }
        }
        LOGGER.warn("[chickens] 品种引用了不存在的物品标签（矿词）: {}", tag);
        return null;
    }

    /** 展示用（JEI 等无随机源场景）：标签取第一个已注册物品（保证显示稳定）；解析失败返回 null */
    public static Item resolveItemForDisplay(RegistryAccess access, ChickenBreed.ItemRef ref) {
        Registry<Item> registry = access.registryOrThrow(Registries.ITEM);
        if (ref.itemId().isPresent()) {
            return registry.get(ref.itemId().get());
        }
        TagKey<Item> tag = ref.tag().orElse(null);
        if (tag != null) {
            for (Holder<Item> holder : registry.getTagOrEmpty(tag)) {
                return holder.value();
            }
        }
        return null;
    }

    /**
     * 按力量抽取产物：主产物有效权重 = max(1, weight) × 力量，力量 ≥ 10 必出主产物；无副产物恒主产物。
     * 返回完整 Product（含可选 fluid，产出 ItemStack 时写入 chickens:fluid 组件）。
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

    /** 父母对（构造时顺序规范化，与放槽顺序无关） */
    public record ParentPair(ResourceLocation first, ResourceLocation second) {
        public static ParentPair of(ResourceLocation a, ResourceLocation b) {
            return a.compareTo(b) <= 0 ? new ParentPair(a, b) : new ParentPair(b, a);
        }
    }

    /** 品种标签的成员：品种 JSON 自声明 tags 字段包含该标签的品种（按注册顺序，与数据包 tags 目录无关） */
    public static List<ResourceLocation> breedsWithTag(RegistryAccess access, TagKey<ChickenBreed> tag) {
        List<ResourceLocation> members = new ArrayList<>();
        for (Map.Entry<ResourceKey<ChickenBreed>, ChickenBreed> entry : breedRegistry(access).entrySet()) {
            if (entry.getValue().tags().contains(tag)) {
                members.add(entry.getKey().location());
            }
        }
        return members;
    }

    /** 预计算「父母对 → 命中规则」索引：#标签 父母展开为全部成员品种组合；引用不存在的品种只记日志跳过（不崩服） */
    public static Map<ParentPair, List<MutationRule>> mutationIndex(RegistryAccess access) {
        Registry<MutationRule> registry = mutationRegistry(access);
        Registry<ChickenBreed> breeds = breedRegistry(access);
        Map<ParentPair, List<MutationRule>> index = new HashMap<>();
        Map<TagKey<ChickenBreed>, List<ResourceLocation>> tagCache = new HashMap<>();
        for (Map.Entry<ResourceKey<MutationRule>, MutationRule> entry : registry.entrySet()) {
            MutationRule rule = entry.getValue();
            ResourceLocation ruleId = entry.getKey().location();
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
            List<ResourceLocation> sideA = expandParents(access, breeds, tagCache, rule.parents().get(0), ruleId);
            List<ResourceLocation> sideB = expandParents(access, breeds, tagCache, rule.parents().get(1), ruleId);
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
    private static List<ResourceLocation> expandParents(RegistryAccess access, Registry<ChickenBreed> breeds,
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
        List<ResourceLocation> members = tagCache.computeIfAbsent(tag, t -> breedsWithTag(access, t));
        if (members.isEmpty()) {
            LOGGER.error("[chickens] 杂交规则 {} 引用了没有成员的品种标签 #{}", ruleId, tag.location());
        }
        return members;
    }

    /** 命中规则的合并结果表；无规则命中返回 Optional.empty()（调用方退回原版行为） */
    public static Optional<List<MutationRule.WeightedResult>> findResults(
            Map<ParentPair, List<MutationRule>> index, ResourceLocation breedA, ResourceLocation breedB) {
        List<MutationRule> rules = index.get(ParentPair.of(breedA, breedB));
        if (rules == null) {
            return Optional.empty();
        }
        return Optional.of(rules.stream().flatMap(rule -> rule.results().stream()).toList());
    }

    /** 按权重抽取一个结果；结果表为空返回 Optional.empty()；$parent: 标记结合本次实际父母解析 */
    public static Optional<ResourceLocation> roll(RegistryAccess access,
            Optional<List<MutationRule.WeightedResult>> results,
            ResourceLocation breedA, ResourceLocation breedB, RandomSource random) {
        if (results.isEmpty()) {
            return Optional.empty();
        }
        List<MutationRule.WeightedResult> list = results.get();
        double total = list.stream().mapToDouble(MutationRule.WeightedResult::weight).sum();
        double roll = random.nextDouble() * total;
        MutationRule.WeightedResult picked = list.get(list.size() - 1);
        for (MutationRule.WeightedResult result : list) {
            roll -= result.weight();
            if (roll < 0) {
                picked = result;
                break;
            }
        }
        return Optional.of(resolveResult(access, picked.breed(), breedA, breedB, random));
    }

    /** 精确 id 原样返回；$parent:标签 → 实际父母中带该标签的那只（都带随机取一；都没带记日志兜底） */
    private static ResourceLocation resolveResult(RegistryAccess access, MutationRule.ResultRef ref,
            ResourceLocation breedA, ResourceLocation breedB, RandomSource random) {
        if (ref.breedId().isPresent()) {
            return ref.breedId().get();
        }
        TagKey<ChickenBreed> tag = ref.tag().orElseThrow();
        Registry<ChickenBreed> breeds = breedRegistry(access);
        List<ResourceLocation> candidates = new ArrayList<>(2);
        for (ResourceLocation id : List.of(breedA, breedB)) {
            ChickenBreed breed = breeds.get(id);
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

    /** 合并命中规则的品种权重抽取；密度 chance 取命中规则最小值；未命中规则或未通过密度判定返回 Optional.empty()（否决生成） */
    public static Optional<ResourceLocation> pickSpawnBreed(
            RegistryAccess access, ResourceLocation dimension,
            net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome, RandomSource random) {
        List<SpawnRule.WeightedBreed> pool = new ArrayList<>();
        double chance = 1.0;
        for (SpawnRule rule : spawnRuleRegistry(access)) {
            if (matchesDimension(rule, dimension) && matchesBiome(rule, biome)) {
                pool.addAll(rule.breeds());
                chance = Math.min(chance, rule.chance().orElse(1.0));
            }
        }
        if (pool.isEmpty() || chance <= 0.0 || random.nextDouble() >= chance) {
            return Optional.empty();
        }
        double total = pool.stream().mapToDouble(SpawnRule.WeightedBreed::weight).sum();
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

    private static boolean matchesBiome(SpawnRule rule, net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome) {
        if (rule.biomes().isEmpty()) {
            return true;
        }
        for (String entry : rule.biomes()) {
            if (entry.startsWith("#")) {
                TagKey<net.minecraft.world.level.biome.Biome> tag = TagKey.create(
                        Registries.BIOME, ResourceLocation.parse(entry.substring(1)));
                if (biome.is(tag)) {
                    return true;
                }
            } else if (biome.unwrapKey()
                    .map(key -> key.location().equals(ResourceLocation.parse(entry)))
                    .orElse(false)) {
                return true;
            }
        }
        return false;
    }
}
