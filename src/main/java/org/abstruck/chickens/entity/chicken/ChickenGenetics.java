package org.abstruck.chickens.entity.chicken;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.abstruck.chickens.breed.BreedLookups;

/**
 * 繁殖遗传规则：
 * - 同品种：每属性取父母最大值后独立随机 +0~3（上限 10），每代至少一项 +1
 * - 异品种：命中 mutation 规则 → 新品种（属性重置为 1）；
 *   未命中 → 优先产出 tier 较低的品种（tier 相同才随机），属性同样重置为 1
 */
public final class ChickenGenetics {
    private ChickenGenetics() {
    }

    /** 一对资源鸡的后代数据（品种 + 三维属性） */
    public static OffspringData create(ResourceChicken parentA, ResourceChicken parentB,
                                       RegistryAccess access, RandomSource random) {
        return create(parentA.getBreedId(),
                new ChickenStats(parentA.getGrowth(), parentA.getGain(), parentA.getStrength()),
                parentB.getBreedId(),
                new ChickenStats(parentB.getGrowth(), parentB.getGain(), parentB.getStrength()),
                access, random);
    }

    /** 数据版入口（巢箱配对等无实体场景用） */
    public static OffspringData create(ResourceLocation breedA, ChickenStats statsA,
                                       ResourceLocation breedB, ChickenStats statsB,
                                       RegistryAccess access, RandomSource random) {
        if (breedA.equals(breedB)) {
            int growthBase = Math.max(statsA.growth(), statsB.growth());
            int gainBase = Math.max(statsA.gain(), statsB.gain());
            int strengthBase = Math.max(statsA.strength(), statsB.strength());
            byte growth = addRandom(growthBase, random);
            byte gain = addRandom(gainBase, random);
            byte strength = addRandom(strengthBase, random);
            if (growth == growthBase && gain == gainBase && strength == strengthBase) {
                // 至少提升一个属性
                switch (random.nextInt(3)) {
                    case 0 -> growth = addOne(growthBase);
                    case 1 -> gain = addOne(gainBase);
                    default -> strength = addOne(strengthBase);
                }
            }
            return new OffspringData(breedA, growth, gain, strength);
        }
        var index = BreedLookups.mutationIndex(access);
        var results = BreedLookups.findResults(index, breedA, breedB);
        var mutated = BreedLookups.roll(access, results, breedA, breedB, random);
        if (mutated.isPresent()) {
            return new OffspringData(mutated.get(), (byte) 1, (byte) 1, (byte) 1);
        }
        // 未命中配方：优先 tier 较低的一方（防止用低级鸡蹭高级鸡提前解锁）
        return new OffspringData(pickLowerTier(access, breedA, breedB, random), (byte) 1, (byte) 1, (byte) 1);
    }

    /** 无杂交配方时的回退：tier 较低者胜出，相同随机；解析不到的品种（被数据包移除）视为 tier 最高 */
    private static ResourceLocation pickLowerTier(RegistryAccess access, ResourceLocation breedA,
                                                  ResourceLocation breedB, RandomSource random) {
        var registry = BreedLookups.breedRegistry(access);
        var a = registry.get(breedA);
        var b = registry.get(breedB);
        int tierA = a != null ? a.tier() : Integer.MAX_VALUE;
        int tierB = b != null ? b.tier() : Integer.MAX_VALUE;
        if (tierA < tierB) {
            return breedA;
        }
        if (tierB < tierA) {
            return breedB;
        }
        return random.nextBoolean() ? breedA : breedB;
    }

    /** 随机 +0~3（上限 10） */
    private static byte addRandom(int base, RandomSource random) {
        return (byte) Math.min(10, base + random.nextInt(4));
    }

    private static byte addOne(int base) {
        return (byte) Math.min(10, base + 1);
    }

    public record OffspringData(ResourceLocation breed, byte growth, byte gain, byte strength) {
    }
}
