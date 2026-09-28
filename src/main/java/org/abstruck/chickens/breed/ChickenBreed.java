package org.abstruck.chickens.breed;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 鸡品种的数据描述，是数据包注册表 {@code chickens:breed} 的条目类型。
 * 条目来自 JSON：{@code data/<数据包命名空间>/chickens/breed/<品种id>.json}。
 * 显示名不放在数据里，走语言键 {@code breed.chickens.<品种id>}。
 */
public record ChickenBreed(
        Product item,
        /**
         * 实体纹理位置（不含 .png 后缀，如 {@code chickens:entity/chicken/flint}）。
         * 缺省时渲染器按约定路径 {@code textures/entity/chicken/&lt;品种id&gt;.png} 查找，找不到则回退原版鸡纹理。
         */
        Optional<ResourceLocation> texture,
        int interval,
        Count count,
        double gainMultiplier,
        List<Product> byproducts,
        Optional<ResourceLocation> loot,
        /** 等级标记（仅用于创造 Tab 分类排序，无玩法效果；缺省 0） */
        int tier
) {
    public static final Codec<ChickenBreed> CODEC = RecordCodecBuilder.create(
            (RecordCodecBuilder.Instance<ChickenBreed> instance) -> instance.group(
                    Product.CODEC.fieldOf("item").forGetter(ChickenBreed::item),
                    ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(ChickenBreed::texture),
                    Codec.INT.fieldOf("interval").forGetter(ChickenBreed::interval),
                    Count.CODEC.fieldOf("count").forGetter(ChickenBreed::count),
                    Codec.DOUBLE.optionalFieldOf("gain_multiplier", 0.2).forGetter(ChickenBreed::gainMultiplier),
                    Product.CODEC.listOf().optionalFieldOf("byproducts", List.of())
                            .forGetter(ChickenBreed::byproducts),
                    ResourceLocation.CODEC.optionalFieldOf("loot").forGetter(ChickenBreed::loot),
                    Codec.INT.optionalFieldOf("tier", 0).forGetter(ChickenBreed::tier)
            ).apply(instance, ChickenBreed::new));

    /**
     * 产物（主/副共用，力量权重制）：主产物是 item 字段，副产物是 byproducts 列表（可多个）。
     * 抽取规则见 BreedLookups.pickOutputItem：主产物有效权重 = weight × 力量，力量 10 必出主产物。
     * fluid 字段（可选）：产出物为流体蛋（{@code chickens:fluid_egg}）时携带的流体 id，
     * 产出 ItemStack 时写入 {@code chickens:fluid} 组件。
     */
    public record Product(ResourceLocation item, int weight, Optional<ResourceLocation> fluid) {
        public static final Codec<Product> CODEC = RecordCodecBuilder.create(
                (RecordCodecBuilder.Instance<Product> instance) -> instance.group(
                        ResourceLocation.CODEC.fieldOf("item").forGetter(Product::item),
                        Codec.INT.fieldOf("weight").forGetter(Product::weight),
                        ResourceLocation.CODEC.optionalFieldOf("fluid").forGetter(Product::fluid)
                ).apply(instance, Product::new));
    }

    /** 单次产出数量区间 [min, max]，JSON 里写成数组 "count": [1, 2] */
    public record Count(int min, int max) {
        public static final Codec<Count> CODEC = Codec.INT.listOf(2, 2).xmap(
                list -> new Count(list.get(0), list.get(1)),
                count -> List.of(count.min(), count.max())
        );
    }
}
