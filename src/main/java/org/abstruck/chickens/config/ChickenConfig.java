package org.abstruck.chickens.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 模组配置骨架。阶段 2 只放顶层开关与全局乘数，
 * 后续阶段（4 繁殖 / 5 孵化 / 6 设施 / 7 能源）按需补细节。
 */
public final class ChickenConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue MUTATION_ENABLED = BUILDER
            .comment("是否允许异品种杂交产生新品种")
            .define("mutationEnabled", true);

    public static final ModConfigSpec.BooleanValue VANILLA_BREEDING_REWORK = BUILDER
            .comment("鸡交配时不再直接生成幼体，改为掉落受精蛋（阶段 4 生效）")
            .define("vanillaBreedingRework", false);

    public static final ModConfigSpec.DoubleValue PRODUCTION_INTERVAL_MULTIPLIER = BUILDER
            .comment("全局产出间隔乘数（品种 JSON 里的 interval 为基础值）")
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

    public static final ModConfigSpec.IntValue NEST_PRODUCTION_TIME = BUILDER
            .comment("鸡窝产出间隔（tick，600 = 30 秒）")
            .defineInRange("nestProductionTime", 600, 20, 120000);

    public static final ModConfigSpec.IntValue BABY_GROWTH_TIME = BUILDER
            .comment("幼年鸡成长到成年所需时间（tick，6000 = 5 分钟；原版硬编码是 24000 = 20 分钟）")
            .defineInRange("babyGrowthTime", 6000, 100, 24000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ChickenConfig() {
    }
}
