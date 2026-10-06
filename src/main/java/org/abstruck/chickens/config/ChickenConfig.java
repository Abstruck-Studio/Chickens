package org.abstruck.chickens.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 模组配置：产出乘数、设施节奏与幼年成长时间。
 */
public final class ChickenConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue PRODUCTION_INTERVAL_MULTIPLIER = BUILDER
            .comment("实体鸡产出间隔全局乘数（基础值为原版随机 5~10 分钟；鸡窝间隔不受此值影响）")
            .defineInRange("productionIntervalMultiplier", 1.0, 0.01, 100.0);

    public static final ModConfigSpec.DoubleValue GROWTH_INTERVAL_FACTOR = BUILDER
            .comment("每点 Growth 缩短产出间隔的比例（实际间隔 = 基础间隔 × 全局乘数 ÷ (1 + growth × 该系数)）")
            .defineInRange("growthIntervalFactor", 0.1, 0.0, 1.0);

    public static final ModConfigSpec.IntValue BREEDING_BOX_BREED_TIME = BUILDER
            .comment("繁殖箱两只鸡配对产出幼年鸡的时间（tick，600 = 30 秒）；需要种子")
            .defineInRange("breedingBoxBreedTime", 600, 20, 120000);

    public static final ModConfigSpec.IntValue GROWER_TIME = BUILDER
            .comment("培育箱幼年鸡成长时间（tick，600 = 30 秒）；需要小麦种子")
            .defineInRange("growerTime", 600, 20, 120000);

    public static final ModConfigSpec.IntValue BABY_GROWTH_TIME = BUILDER
            .comment("幼年鸡成长到成年所需时间（tick，6000 = 5 分钟；原版硬编码是 24000 = 20 分钟）")
            .defineInRange("babyGrowthTime", 6000, 100, 24000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ChickenConfig() {
    }
}
