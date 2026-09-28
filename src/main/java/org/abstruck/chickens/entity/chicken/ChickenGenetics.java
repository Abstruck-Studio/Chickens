package org.abstruck.chickens.entity.chicken;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.abstruck.chickens.breed.BreedLookups;
import org.abstruck.chickens.config.ChickenConfig;

/**
 * 繁殖遗传规则：
 * - 同品种：每属性 = max(父母)，随后独立 upgradeChance 概率 +1（上限 10）
 * - 异品种：命中 mutation 规则 → 新品种（属性重置为 1）；未命中 → 父母品种二选一（属性重置为 1）
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
            // 同品种：属性取父母最大值，单个属性随机 +0~3（绝对上限 10），每代至少一项 +1
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
        if (ChickenConfig.MUTATION_ENABLED.get()) {
            var index = BreedLookups.mutationIndex(access);
            var results = BreedLookups.findResults(index, breedA, breedB);
            var mutated = BreedLookups.roll(results, random);
            if (mutated.isPresent()) {
                // 杂交出新品种：属性重置为 1（规格 1.2 硬规则）
                return new OffspringData(mutated.get(), (byte) 1, (byte) 1, (byte) 1);
            }
        }
        // 无规则命中 / 杂交关闭：父母品种二选一，属性重置为 1
        return new OffspringData(random.nextBoolean() ? breedA : breedB, (byte) 1, (byte) 1, (byte) 1);
    }

    /** 随机 +0~3（绝对上限 10） */
    private static byte addRandom(int base, RandomSource random) {
        return (byte) Math.min(10, base + random.nextInt(4));
    }

    /** 强制 +1（绝对上限 10；已满级时保持） */
    private static byte addOne(int base) {
        return (byte) Math.min(10, base + 1);
    }

    /** 后代数据快照 */
    public record OffspringData(ResourceLocation breed, byte growth, byte gain, byte strength) {
    }
}
