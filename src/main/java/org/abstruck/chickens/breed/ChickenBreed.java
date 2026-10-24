package org.abstruck.chickens.breed;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 鸡品种的数据描述（数据包注册表 {@code chickens:breed} 的条目）。
 * 显示名走语言键 {@code breed.chickens.<品种id>}，不存在数据里。
 *
 * <p>1.19.2 没有 {@code DataPackRegistryEvent}，本模组用自带的 JSON 数据加载器
 * （{@link ChickenData}）读取同样的文件与同样的 JSON 格式，并把结果同步给客户端。</p>
 */
public record ChickenBreed(
        Product item,
        Optional<ResourceLocation> texture,
        Count count,
        double gainMultiplier,
        List<Product> byproducts,
        Optional<ResourceLocation> loot,
        int tier,
        List<TagKey<ChickenBreed>> tags,
        Optional<ParticleConfig> particles,
        Optional<Integer> interval
) {
    /** tags 字段双形态：单个字符串或字符串数组 */
    private static final Codec<List<TagKey<ChickenBreed>>> TAGS_CODEC = Codec.either(
            TagKey.hashedCodec(ChickenRegistries.BREED),
            TagKey.hashedCodec(ChickenRegistries.BREED).listOf()
    ).xmap(
            either -> either.map(List::of, Function.identity()),
            tags -> tags.size() == 1 ? Either.left(tags.get(0)) : Either.right(tags)
    );

    public static final Codec<ChickenBreed> CODEC = RecordCodecBuilder.create(
            (RecordCodecBuilder.Instance<ChickenBreed> instance) -> instance.group(
                    Product.CODEC.fieldOf("item").forGetter(ChickenBreed::item),
                    ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(ChickenBreed::texture),
                    Count.CODEC.fieldOf("count").forGetter(ChickenBreed::count),
                    Codec.DOUBLE.optionalFieldOf("gain_multiplier", 0.2).forGetter(ChickenBreed::gainMultiplier),
                    Product.CODEC.listOf().optionalFieldOf("byproducts", List.of())
                            .forGetter(ChickenBreed::byproducts),
                    ResourceLocation.CODEC.optionalFieldOf("loot").forGetter(ChickenBreed::loot),
                    Codec.INT.optionalFieldOf("tier", 0).forGetter(ChickenBreed::tier),
                    TAGS_CODEC.optionalFieldOf("tags", List.of()).forGetter(ChickenBreed::tags),
                    ParticleConfig.CODEC.optionalFieldOf("particles").forGetter(ChickenBreed::particles),
                    Codec.INT.optionalFieldOf("interval").forGetter(ChickenBreed::interval)
            ).apply(instance, ChickenBreed::new));

    /** 产物（主/副共用）：主产物 = item，副产物 = byproducts 列表；fluid 可选，为流体蛋产物携带的流体引用。 */
    public record Product(ItemRef item, int weight, Optional<FluidRef> fluid) {
        public static final Codec<Product> CODEC = RecordCodecBuilder.create(
                (RecordCodecBuilder.Instance<Product> instance) -> instance.group(
                        ItemRef.CODEC.fieldOf("item").forGetter(Product::item),
                        Codec.INT.fieldOf("weight").forGetter(Product::weight),
                        FluidRef.CODEC.optionalFieldOf("fluid").forGetter(Product::fluid)
                ).apply(instance, Product::new));
    }

    /** 产物物品引用：精确物品 id 或物品标签（矿词，产出时从已注册成员随机抽一个） */
    public record ItemRef(Either<ResourceLocation, TagKey<Item>> value) {
        public static final Codec<ItemRef> CODEC = Codec.either(
                ResourceLocation.CODEC,
                TagKey.hashedCodec(Registries.ITEM)
        ).xmap(ItemRef::new, ItemRef::value);

        public static ItemRef of(ResourceLocation item) {
            return new ItemRef(Either.left(item));
        }

        public static ItemRef of(TagKey<Item> tag) {
            return new ItemRef(Either.right(tag));
        }

        public Optional<ResourceLocation> itemId() {
            return this.value.left();
        }

        public Optional<TagKey<Item>> tag() {
            return this.value.right();
        }
    }

    /** 流体引用：精确流体 id 或流体标签（矿词） */
    public record FluidRef(Either<ResourceLocation, TagKey<Fluid>> value) {
        public static final Codec<FluidRef> CODEC = Codec.either(
                ResourceLocation.CODEC,
                TagKey.hashedCodec(Registries.FLUID)
        ).xmap(FluidRef::new, FluidRef::value);

        public static FluidRef of(ResourceLocation fluid) {
            return new FluidRef(Either.left(fluid));
        }

        public static FluidRef of(TagKey<Fluid> tag) {
            return new FluidRef(Either.right(tag));
        }

        public Optional<ResourceLocation> fluidId() {
            return this.value.left();
        }

        public Optional<TagKey<Fluid>> tag() {
            return this.value.right();
        }
    }

    /** 单次产出数量区间 [min, max]，JSON 里写成数组 "count": [1, 2] */
    public record Count(int min, int max) {
        public static final Codec<Count> CODEC = Codec.INT.listOf().comapFlatMap(
                list -> list.size() == 2
                        ? DataResult.success(new Count(list.get(0), list.get(1)))
                        : DataResult.error(() -> "count 必须是 [min, max] 两个整数"),
                count -> List.of(count.min(), count.max()));
    }

    /** 环境粒子配置：类型 + chance / count / spread；简写形态（纯字符串）= 全部默认参数 */
    public record ParticleConfig(ResourceLocation type, float chance, int count, float spread) {
        public static final float DEFAULT_CHANCE = 1.0F;
        public static final int DEFAULT_COUNT = 2;
        public static final float DEFAULT_SPREAD = 0.5F;

        public static final Codec<ParticleConfig> CODEC = Codec.either(
                ResourceLocation.CODEC,
                RecordCodecBuilder.<ParticleConfig>create(instance -> instance.group(
                        ResourceLocation.CODEC.fieldOf("type").forGetter(ParticleConfig::type),
                        Codec.FLOAT.optionalFieldOf("chance", DEFAULT_CHANCE).forGetter(ParticleConfig::chance),
                        Codec.INT.optionalFieldOf("count", DEFAULT_COUNT).forGetter(ParticleConfig::count),
                        Codec.FLOAT.optionalFieldOf("spread", DEFAULT_SPREAD).forGetter(ParticleConfig::spread)
                ).apply(instance, ParticleConfig::new))
        ).xmap(
                either -> either.map(id -> new ParticleConfig(id, DEFAULT_CHANCE, DEFAULT_COUNT, DEFAULT_SPREAD),
                        Function.identity()),
                config -> config.chance() == DEFAULT_CHANCE && config.count() == DEFAULT_COUNT
                        && config.spread() == DEFAULT_SPREAD
                        ? Either.left(config.type()) : Either.right(config)
        );
    }
}
