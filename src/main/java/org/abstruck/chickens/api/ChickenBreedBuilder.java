package org.abstruck.chickens.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import org.abstruck.chickens.breed.ChickenBreed;

import java.util.List;
import java.util.Optional;

/**
 * 给其他模组用的程序化品种构建器：构建出的 ChickenBreed 交给 ChickenBreedProvider（datagen）
 * 写成 data/&lt;模组id&gt;/chickens/breed/&lt;品种id&gt;.json 随 jar 发布
 * （也可以直接手写 JSON）。
 */
public final class ChickenBreedBuilder {
    private final ChickenBreed.ItemRef item;
    private int itemWeight = 10;
    private Optional<ResourceLocation> texture = Optional.empty();
    private ChickenBreed.Count count = new ChickenBreed.Count(1, 1);
    private double gainMultiplier = 0.2;
    private final List<ChickenBreed.Product> byproducts = new java.util.ArrayList<>();
    private Optional<ResourceLocation> loot = Optional.empty();
    private Optional<ChickenBreed.FluidRef> mainFluid = Optional.empty();
    private Optional<ChickenBreed.ParticleConfig> particles = Optional.empty();
    private Optional<Integer> interval = Optional.empty();
    private int tier = 0;
    private final List<TagKey<ChickenBreed>> tags = new java.util.ArrayList<>();

    private ChickenBreedBuilder(ChickenBreed.ItemRef item) {
        this.item = item;
    }

    public static ChickenBreedBuilder of(ResourceLocation item) {
        return new ChickenBreedBuilder(ChickenBreed.ItemRef.of(item));
    }

    /** 主产物为物品标签（矿词）：产出时从标签内已注册物品随机抽一个 */
    public static ChickenBreedBuilder of(TagKey<Item> item) {
        return new ChickenBreedBuilder(ChickenBreed.ItemRef.of(item));
    }

    /** 主产物权重（缺省 10）；抽取时有效权重 = 权重 × 力量 */
    public ChickenBreedBuilder itemWeight(int weight) {
        this.itemWeight = weight;
        return this;
    }

    /** 实体纹理（不含 .png 后缀），如 {@code chickens:entity/chicken/flint} */
    public ChickenBreedBuilder texture(ResourceLocation texture) {
        this.texture = Optional.of(texture);
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
        this.byproducts.add(new ChickenBreed.Product(ChickenBreed.ItemRef.of(item), weight, Optional.empty()));
        return this;
    }

    /** 副产物为物品标签（矿词）：产出时从标签内已注册物品随机抽一个 */
    public ChickenBreedBuilder byproduct(TagKey<Item> item, int weight) {
        this.byproducts.add(new ChickenBreed.Product(ChickenBreed.ItemRef.of(item), weight, Optional.empty()));
        return this;
    }

    /** 副产物为流体蛋时携带的流体 */
    public ChickenBreedBuilder byproduct(ResourceLocation item, int weight, ResourceLocation fluid) {
        this.byproducts.add(new ChickenBreed.Product(ChickenBreed.ItemRef.of(item), weight,
                Optional.of(ChickenBreed.FluidRef.of(fluid))));
        return this;
    }

    /** 主产物为流体蛋时携带的流体 */
    public ChickenBreedBuilder fluid(ResourceLocation fluid) {
        this.mainFluid = Optional.of(ChickenBreed.FluidRef.of(fluid));
        return this;
    }

    /** 主产物为流体蛋且流体写标签（矿词） */
    public ChickenBreedBuilder fluid(TagKey<Fluid> fluid) {
        this.mainFluid = Optional.of(ChickenBreed.FluidRef.of(fluid));
        return this;
    }

    /** 等级标记（Tier）：杂交无配方时优先产出 tier 较低者，也用于创造 Tab 排序；缺省 0 */
    public ChickenBreedBuilder tier(int tier) {
        this.tier = tier;
        return this;
    }

    /** 品种所属标签（供杂交规则分组引用） */
    public ChickenBreedBuilder tag(TagKey<ChickenBreed> tag) {
        this.tags.add(tag);
        return this;
    }

    public ChickenBreedBuilder loot(ResourceLocation lootTable) {
        this.loot = Optional.of(lootTable);
        return this;
    }

    /** 环境粒子特效（缺省无粒子）：粒子注册表 id（如 minecraft:portal），默认每 tick 2 颗 */
    public ChickenBreedBuilder particles(ResourceLocation particleId) {
        this.particles = Optional.of(new ChickenBreed.ParticleConfig(particleId,
                ChickenBreed.ParticleConfig.DEFAULT_CHANCE, ChickenBreed.ParticleConfig.DEFAULT_COUNT,
                ChickenBreed.ParticleConfig.DEFAULT_SPREAD));
        return this;
    }

    /** 环境粒子特效（完整参数） */
    public ChickenBreedBuilder particles(ResourceLocation particleId, float chance, int count, float spread) {
        this.particles = Optional.of(new ChickenBreed.ParticleConfig(particleId, chance, count, spread));
        return this;
    }

    /** 鸡窝产出间隔（tick，600 = 30 秒）；缺省用内置默认 600；只影响鸡窝 */
    public ChickenBreedBuilder interval(int ticks) {
        this.interval = Optional.of(ticks);
        return this;
    }

    public ChickenBreed build() {
        return new ChickenBreed(new ChickenBreed.Product(item, itemWeight, mainFluid), texture, count,
                gainMultiplier, List.copyOf(byproducts), loot, tier, List.copyOf(tags), particles, interval);
    }
}
