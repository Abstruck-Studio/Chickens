package org.abstruck.chickens.breed;

import com.mojang.logging.LogUtils;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * 品种/杂交注册表的运行时查询工具。
 * 品种里对物品的引用用 ResourceLocation 惰性保存，使用时经 RegistryAccess 解析
 * （原版组件同款做法），因此所有查询都要传入 RegistryAccess（服务端用
 * ServerLevel#registryAccess，客户端用 ClientPacketListener#registryAccess）。
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

    /** 查流体蛋的渲染颜色（注册表里找不到返回空——渲染回退不透明白） */
    public static OptionalInt colorOfFluid(RegistryAccess access, ResourceLocation fluidId) {
        Registry<FluidEggEntry> registry = fluidEggRegistry(access);
        for (FluidEggEntry entry : registry) {
            if (entry.fluid().equals(fluidId)) {
                return OptionalInt.of(entry.color());
            }
        }
        return OptionalInt.empty();
    }

    /** 把品种 JSON 里的流体 id 解析成 Holder<Fluid>（写入 chickens:fluid 组件用）；
     *  解析失败返回 null 并记日志。RegistryAccess 与 TAB 事件的 holders 都实现了 Provider。 */
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

    /** 把品种里的物品 id 解析成 Item；解析失败返回 null 并记日志。 */
    public static Item resolveItem(RegistryAccess access, ResourceLocation itemId) {
        Item item = access.registryOrThrow(Registries.ITEM).get(itemId);
        if (item == null) {
            LOGGER.warn("[chickens] 品种引用了不存在的物品: {}", itemId);
        }
        return item;
    }

    /**
     * 按力量抽取产出物（主/副产物权重制）：
     * 主产物有效权重 = 主产物 weight × 力量（力量越高主产物权重越高）；
     * 力量 10 必出主产物。无副产物时永远主产物。
     * 返回完整 Product（含可选 fluid 字段，产出 ItemStack 时写入 chickens:fluid 组件）。
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

    /** 父母对（顺序无关，构造时规范化）。 */
    public record ParentPair(ResourceLocation first, ResourceLocation second) {
        public static ParentPair of(ResourceLocation a, ResourceLocation b) {
            return a.compareTo(b) <= 0 ? new ParentPair(a, b) : new ParentPair(b, a);
        }
    }

    /**
     * 注册表加载后预计算：父母对 -> 命中的规则列表。
     * 顺带校验规则引用的品种是否存在（不存在的只记日志跳过，不崩服）。
     */
    public static Map<ParentPair, List<MutationRule>> mutationIndex(RegistryAccess access) {
        Registry<MutationRule> registry = mutationRegistry(access);
        Registry<ChickenBreed> breeds = breedRegistry(access);
        Map<ParentPair, List<MutationRule>> index = new HashMap<>();
        for (Map.Entry<ResourceKey<MutationRule>, MutationRule> entry : registry.entrySet()) {
            MutationRule rule = entry.getValue();
            if (rule.parents().size() != 2) {
                LOGGER.error("[chickens] 杂交规则 {} 的 parents 必须恰好 2 个，已跳过", entry.getKey().location());
                continue;
            }
            for (MutationRule.WeightedResult result : rule.results()) {
                if (!breeds.containsKey(result.breed())) {
                    LOGGER.error("[chickens] 杂交规则 {} 引用了不存在的品种 {}", entry.getKey().location(), result.breed());
                }
            }
            index.computeIfAbsent(
                    ParentPair.of(rule.parents().get(0), rule.parents().get(1)),
                    k -> new ArrayList<>()).add(rule);
        }
        return index;
    }

    /** 命中规则的结果表；无规则命中返回 Optional.empty()（调用方退回原版行为）。 */
    public static Optional<List<MutationRule.WeightedResult>> findResults(
            Map<ParentPair, List<MutationRule>> index, ResourceLocation breedA, ResourceLocation breedB) {
        List<MutationRule> rules = index.get(ParentPair.of(breedA, breedB));
        if (rules == null) {
            return Optional.empty();
        }
        return Optional.of(rules.stream().flatMap(rule -> rule.results().stream()).toList());
    }

    /** 按权重抽取一个结果品种；结果表为空返回 Optional.empty()。 */
    public static Optional<ResourceLocation> roll(
            Optional<List<MutationRule.WeightedResult>> results, RandomSource random) {
        if (results.isEmpty()) {
            return Optional.empty();
        }
        List<MutationRule.WeightedResult> list = results.get();
        double total = list.stream().mapToDouble(MutationRule.WeightedResult::weight).sum();
        double roll = random.nextDouble() * total;
        for (MutationRule.WeightedResult result : list) {
            roll -= result.weight();
            if (roll < 0) {
                return Optional.of(result.breed());
            }
        }
        return Optional.of(list.get(list.size() - 1).breed());
    }

    // ---------- 自然生成规则（chickens:spawn_rule） ----------

    /** 按自然生成规则抽取品种：合并维度+群系全部命中规则的权重后抽取；
     *  密度由命中规则的 chance（取最小，瓶颈语义）控制——未通过密度判定或无命中
     *  都返回 Optional.empty()（调用方否决该次生成）。 */
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

    /** 维度过滤：规则缺省 = 任意维度 */
    private static boolean matchesDimension(SpawnRule rule, ResourceLocation dimension) {
        return rule.dimension().isEmpty() || rule.dimension().get().equals(dimension);
    }

    /** 群系过滤：规则 biomes 为空 = 任意群系；元素以 # 开头是标签，否则是群系 id */
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
