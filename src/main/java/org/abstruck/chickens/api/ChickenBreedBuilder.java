package org.abstruck.chickens.api;

import net.minecraft.resources.ResourceLocation;
import org.abstruck.chickens.breed.ChickenBreed;

import java.util.List;
import java.util.Optional;

/**
 * 给其他模组用的程序化品种构建器。
 * <p>
 * 注意：品种注册表是数据包注册表，没有"运行时注册"入口——
 * 构建出的 {@link ChickenBreed} 交给 {@link ChickenBreedProvider}（datagen）
 * 写成 {@code data/<你的模组id>/chickens/breed/<品种id>.json} 随你的 jar 发布，
 * 或者干脆手写 JSON（零代码，和 tags 一个套路）。
 */
public final class ChickenBreedBuilder {
    private final ResourceLocation item;
    private int itemWeight = 10;
    private Optional<ResourceLocation> texture = Optional.empty();
    private int interval = 6000;
    private ChickenBreed.Count count = new ChickenBreed.Count(1, 1);
    private double gainMultiplier = 0.2;
    private final List<ChickenBreed.Product> byproducts = new java.util.ArrayList<>();
    private Optional<ResourceLocation> loot = Optional.empty();
    private Optional<ResourceLocation> mainFluid = Optional.empty();
    private int tier = 0;

    private ChickenBreedBuilder(ResourceLocation item) {
        this.item = item;
    }

    public static ChickenBreedBuilder of(ResourceLocation item) {
        return new ChickenBreedBuilder(item);
    }

    /** 主产物权重（缺省 10）。抽取时有效权重 = 权重 × 力量。 */
    public ChickenBreedBuilder itemWeight(int weight) {
        this.itemWeight = weight;
        return this;
    }

    /** 实体纹理（不含 .png 后缀），如 {@code chickens:entity/chicken/flint} */
    public ChickenBreedBuilder texture(ResourceLocation texture) {
        this.texture = Optional.of(texture);
        return this;
    }

    public ChickenBreedBuilder interval(int ticks) {
        this.interval = ticks;
        return this;
    }

    public ChickenBreedBuilder count(int min, int max) {
        this.count = new ChickenBreed.Count(min, max);
        return this;
    }

    public ChickenBreedBuilder gainMultiplier(double multiplier) {
        this.gainMultiplier = multiplier;
        return this;
    }

    /** 副产物（力量权重制，见 ChickenBreed.Product） */
    public ChickenBreedBuilder byproduct(ResourceLocation item, int weight) {
        this.byproducts.add(new ChickenBreed.Product(item, weight, Optional.empty()));
        return this;
    }

    /** 副产物为流体蛋时携带的流体（写入 chickens:fluid 组件） */
    public ChickenBreedBuilder byproduct(ResourceLocation item, int weight, ResourceLocation fluid) {
        this.byproducts.add(new ChickenBreed.Product(item, weight, Optional.of(fluid)));
        return this;
    }

    /** 主产物为流体蛋时携带的流体 */
    public ChickenBreedBuilder fluid(ResourceLocation fluid) {
        this.mainFluid = Optional.of(fluid);
        return this;
    }

    /** 等级标记（仅创造 Tab 分类排序，缺省 0） */
    public ChickenBreedBuilder tier(int tier) {
        this.tier = tier;
        return this;
    }

    public ChickenBreedBuilder loot(ResourceLocation lootTable) {
        this.loot = Optional.of(lootTable);
        return this;
    }

    public ChickenBreed build() {
        return new ChickenBreed(new ChickenBreed.Product(item, itemWeight, mainFluid), texture, interval, count,
                gainMultiplier, List.copyOf(byproducts), loot, tier);
    }
}
