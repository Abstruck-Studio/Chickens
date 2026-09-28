package org.abstruck.chickens.gametest;

import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.breed.BreedLookups;
import org.abstruck.chickens.breed.ChickenBreed;
import org.abstruck.chickens.breed.MutationRule;
import org.abstruck.chickens.entity.chicken.ResourceChicken;
import org.abstruck.chickens.registry.ModEntities;

import java.util.List;

/**
 * 无头验证。运行：.\gradlew.bat runGameTestServer
 * （自然生成相关代码已按用户要求移除，对应测试一并删除）
 */
@GameTestHolder(Chickens.MODID)
public final class ProductionGameTest {
    private ProductionGameTest() {
    }

    /** 资源鸡产出计时 → 掉落品种资源物（同时验证运行时品种注册表加载） */
    @GameTest(template = "gametest_empty", timeoutTicks = 600)
    public static void resourceChickenProducesItem(GameTestHelper helper) {
        ResourceChicken chicken = helper.spawn(ModEntities.RESOURCE_CHICKEN.get(), 1, 1, 1);
        chicken.setBreed(ResourceLocation.fromNamespaceAndPath("chickens", "flint"));
        chicken.setProductionTimer(30);
        helper.succeedWhen(() -> {
            AABB around = new AABB(chicken.blockPosition()).inflate(5.0D);
            if (helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).isEmpty()) {
                throw new GameTestAssertException("30 tick 后鸡周围仍没有产出物");
            }
        });
    }

    /** 同品种繁殖 → 后代品种一致且属性 ≥ 父母最大值（上限规则：≤ 父母最大 +3、绝对上限 10） */
    @GameTest(template = "gametest_empty", timeoutTicks = 100)
    public static void sameBreedOffspringInherits(GameTestHelper helper) {
        ResourceChicken a = helper.spawn(ModEntities.RESOURCE_CHICKEN.get(), 1, 1, 1);
        ResourceChicken b = helper.spawn(ModEntities.RESOURCE_CHICKEN.get(), 1, 1, 1);
        a.setBreed(ResourceLocation.fromNamespaceAndPath("chickens", "flint"));
        b.setBreed(ResourceLocation.fromNamespaceAndPath("chickens", "flint"));
        a.setGrowth((byte) 5);
        b.setGrowth((byte) 7);
        a.spawnChildFromBreeding(helper.getLevel(), b);
        AABB box = new AABB(helper.absolutePos(net.minecraft.core.BlockPos.ZERO)).inflate(6.0D);
        List<ResourceChicken> all = helper.getLevel().getEntitiesOfClass(ResourceChicken.class, box);
        boolean childOk = all.stream().anyMatch(c -> c.isBaby()
                && c.getBreedId().getPath().equals("flint")
                && c.getGrowth() >= 7 && c.getGrowth() <= 10
                && c.getGain() >= 1 && c.getStrength() >= 1
                && (c.getGrowth() > 7 || c.getGain() > 1 || c.getStrength() > 1));
        if (!childOk) {
            helper.fail("同品种后代遗传异常（每代至少一项属性应提升）: "
                    + all.stream().map(c -> c.getBreedId() + " g=" + c.getGrowth() + "/" + c.getGain() + "/" + c.getStrength()).toList());
            return;
        }
        helper.succeed();
    }

    /** 鸡窝放水鸡 → 产出物必须带 FLUID 组件（定位「水蛋无色」：组件是否真的设置） */
    @GameTest(template = "gametest_empty", timeoutTicks = 1400)
    public static void nestProducesFluidEgg(GameTestHelper helper) {
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(1, 2, 1);
        helper.setBlock(pos, org.abstruck.chickens.registry.ModBlocks.NEST.get());
        var be = helper.getBlockEntity(pos);
        if (!(be instanceof org.abstruck.chickens.block.nest.NestBlockEntity nest)) {
            helper.fail("鸡窝方块实体不存在");
            return;
        }
        net.minecraft.world.item.ItemStack chicken = new net.minecraft.world.item.ItemStack(org.abstruck.chickens.registry.ModItems.CHICKEN.get());
        chicken.set(org.abstruck.chickens.registry.ModDataComponents.BREED.get(),
                ResourceLocation.fromNamespaceAndPath("chickens", "water"));
        chicken.set(org.abstruck.chickens.registry.ModDataComponents.MATURE.get(), true);
        nest.getItems().setStackInSlot(org.abstruck.chickens.block.nest.NestBlockEntity.CHICKEN_IN, chicken);
        helper.succeedWhen(() -> {
            for (int i = org.abstruck.chickens.block.nest.NestBlockEntity.OUTPUT_START;
                 i < org.abstruck.chickens.block.nest.NestBlockEntity.OUTPUT_START
                         + org.abstruck.chickens.block.nest.NestBlockEntity.OUTPUT_COUNT; i++) {
                net.minecraft.world.item.ItemStack out = nest.getItems().getStackInSlot(i);
                if (!out.isEmpty()) {
                    ResourceLocation fluid = out.get(org.abstruck.chickens.registry.ModDataComponents.FLUID.get());
                    if (!ResourceLocation.fromNamespaceAndPath("minecraft", "water").equals(fluid)) {
                        throw new GameTestAssertException("产出物 FLUID 组件异常: " + fluid + "，物品=" + out.getItem());
                    }
                    return; // 组件正确：通过
                }
            }
            throw new GameTestAssertException("鸡窝尚未产出");
        });
    }

    /** DyeColor 匹配（定位「投出白鸡」：rgb 匹配逻辑是否正确） */
    @GameTest(template = "gametest_empty", timeoutTicks = 20)
    public static void dyeColorMatching(GameTestHelper helper) {
        net.minecraft.world.item.DyeColor lime = net.minecraft.world.item.DyeColor.LIME;
        int rgb = lime.getTextureDiffuseColor();
        if (org.abstruck.chickens.item.DyeEggItem.dyeColorOf(rgb) != lime) {
            helper.fail("dyeColorOf(" + rgb + ") 应匹配 LIME，实际="
                    + org.abstruck.chickens.item.DyeEggItem.dyeColorOf(rgb));
            return;
        }
        if (org.abstruck.chickens.item.DyeEggItem.dyeColorOf(0xFF000000 | rgb) != lime) {
            helper.fail("dyeColorOf(带 alpha) 应匹配 LIME");
            return;
        }
        helper.succeed();
    }

    /** 下界生成配置验证——鸡已进入下界群系的 monster 池（下界地面生成走 monster 通道） */
    @GameTest(template = "gametest_empty", timeoutTicks = 20)
    public static void netherSpawnConfig(GameTestHelper helper) {
        net.minecraft.core.RegistryAccess access = helper.getLevel().registryAccess();
        net.minecraft.world.level.biome.Biome biome = access
                .registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                .get(net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.BIOME,
                        ResourceLocation.withDefaultNamespace("crimson_forest")));
        if (biome == null) {
            helper.fail("crimson_forest 群系不存在");
            return;
        }
        boolean hasChicken = biome.getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.MONSTER)
                .unwrap().stream().anyMatch(s -> s.type == ModEntities.RESOURCE_CHICKEN.get());
        if (!hasChicken) {
            helper.fail("chickens:resource_chicken 不在下界群系的 monster 生成池中（群系覆盖未生效？）");
            return;
        }
        helper.succeed();
    }

    /** 精确复现自然生成链路——checkSpawnPosition 的返回值与实体存活。
     *  注：GameTest 的下界是超平坦世界（群系非真实下界），群系池检查由 netherSpawnConfig 负责 */
    @GameTest(template = "gametest_empty", timeoutTicks = 100)
    public static void netherSpawnPipelineDebug(GameTestHelper helper) {
        net.minecraft.server.level.ServerLevel nether =
                helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        if (nether == null) {
            helper.fail("下界不存在");
            return;
        }
        // 找下界地面
        int groundY = Integer.MIN_VALUE;
        for (int y = 100; y > -60; y--) {
            if (!nether.getBlockState(new net.minecraft.core.BlockPos(0, y, 0)).isAir()) {
                groundY = y + 1;
                break;
            }
        }
        if (groundY == Integer.MIN_VALUE) {
            helper.fail("GameTest 下界没有地面");
            return;
        }
        ResourceChicken chicken = ModEntities.RESOURCE_CHICKEN.get().create(nether);
        if (chicken == null) {
            helper.fail("实体创建失败");
            return;
        }
        chicken.setBreed(ResourceLocation.fromNamespaceAndPath("chickens", "quartz"));
        chicken.moveTo(0.5, groundY, 0.5, 0.0F, 0.0F);
        // 精确复现生成链路第 3 步（PositionCheck fire + checkSpawnRules + checkSpawnObstruction）
        boolean positionOk = net.neoforged.neoforge.event.EventHooks.checkSpawnPosition(
                chicken, nether, net.minecraft.world.entity.MobSpawnType.NATURAL);
        if (!positionOk) {
            net.minecraft.core.BlockPos pos = chicken.blockPosition();
            int sky = nether.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos);
            int block = nether.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos);
            boolean dark = net.minecraft.world.entity.monster.Monster.isDarkEnoughToSpawn(nether, pos, nether.random);
            helper.fail("checkSpawnPosition=false：天空光=" + sky + " 方块光=" + block
                    + " isDarkEnoughToSpawn=" + dark
                    + " 下方方块=" + nether.getBlockState(pos.below()));
            return;
        }
        chicken.finalizeSpawn(nether, nether.getCurrentDifficultyAt(chicken.blockPosition()),
                net.minecraft.world.entity.MobSpawnType.NATURAL, null);
        nether.addFreshEntityWithPassengers(chicken);
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(0, groundY, 0);
        helper.succeedWhen(() -> {
            if (nether.getEntitiesOfClass(ResourceChicken.class, new AABB(pos).inflate(6.0)).isEmpty()) {
                throw new GameTestAssertException("鸡加入下界后消失了（服务端存在消失路径）");
            }
        });
    }

    /** 品种/杂交注册表完整性——全部品种已加载、产物物品 id 可解析、规则引用的品种全部存在 */
    @GameTest(template = "gametest_empty", timeoutTicks = 100)
    public static void breedRegistryIntegrity(GameTestHelper helper) {
        RegistryAccess access = helper.getLevel().registryAccess();
        var breeds = BreedLookups.breedRegistry(access);
        var mutations = BreedLookups.mutationRegistry(access);

        String[] expected = {
                "flint", "snowball", "gunpowder", "log", "sand", "quartz", "soul_sand", "lava",
                "redstone", "glowstone", "iron", "coal", "clay", "slime", "water", "netherwart",
                "gold", "diamond", "blaze", "emerald", "ender", "ghast", "magma", "string",
                "glass", "leather", "pshard", "pcrystal", "obsidian",
                "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
                "silver_dye", "cyan", "purple", "blue", "brown", "green", "red", "black"
        };
        int count = 0;
        for (var ignored : breeds) {
            count++;
        }
        if (count != expected.length) {
            helper.fail("品种注册表数量不对：期望 " + expected.length + "，实际 " + count);
        }
        for (String id : expected) {
            ChickenBreed breed = breeds.get(ResourceLocation.fromNamespaceAndPath("chickens", id));
            if (breed == null) {
                helper.fail("品种缺失: " + id);
            }
            if (BreedLookups.resolveItem(access, breed.item().item()) == null) {
                helper.fail("主产物物品解析失败: " + id + " -> " + breed.item().item());
            }
            for (var byproduct : breed.byproducts()) {
                if (BreedLookups.resolveItem(access, byproduct.item()) == null) {
                    helper.fail("副产物物品解析失败: " + id + " -> " + byproduct.item());
                }
            }
        }
        int ruleCount = 0;
        for (MutationRule rule : mutations) {
            ruleCount++;
            for (ResourceLocation parent : rule.parents()) {
                if (breeds.get(parent) == null) {
                    helper.fail("规则父母品种无效: " + parent);
                }
            }
            for (MutationRule.WeightedResult result : rule.results()) {
                if (breeds.get(result.breed()) == null) {
                    helper.fail("规则结果品种无效: " + result.breed());
                }
            }
        }
        if (ruleCount != 34) {
            helper.fail("杂交规则数量不对：期望 34，实际 " + ruleCount);
        }
        int fluidEggCount = 0;
        for (var ignored : BreedLookups.fluidEggRegistry(access)) {
            fluidEggCount++;
        }
        if (fluidEggCount != 2) {
            helper.fail("流体蛋定义数量不对：期望 2（水/岩浆），实际 " + fluidEggCount);
        }
        helper.succeed();
    }

    /** 异品种杂交 → 命中 mutation 规则（雪球+火药 → 水 0.9，未命中回退父母 0.1），属性重置为 1 */
    @GameTest(template = "gametest_empty", timeoutTicks = 100)
    public static void mutationProducesWater(GameTestHelper helper) {
        ResourceChicken a = helper.spawn(ModEntities.RESOURCE_CHICKEN.get(), 1, 1, 1);
        ResourceChicken b = helper.spawn(ModEntities.RESOURCE_CHICKEN.get(), 1, 1, 1);
        a.setBreed(ResourceLocation.fromNamespaceAndPath("chickens", "snowball"));
        b.setBreed(ResourceLocation.fromNamespaceAndPath("chickens", "gunpowder"));
        a.setGrowth((byte) 9);
        b.setGrowth((byte) 9);
        a.spawnChildFromBreeding(helper.getLevel(), b);
        AABB box = new AABB(helper.absolutePos(net.minecraft.core.BlockPos.ZERO)).inflate(6.0D);
        List<ResourceChicken> all = helper.getLevel().getEntitiesOfClass(ResourceChicken.class, box);
        // 结果应为 water（0.9）或回退 snowball/gunpowder（0.1），属性一律重置 1/1/1
        boolean childOk = all.stream().anyMatch(c -> c.isBaby()
                && java.util.Set.of("water", "snowball", "gunpowder").contains(c.getBreedId().getPath())
                && c.getGrowth() == 1 && c.getGain() == 1 && c.getStrength() == 1);
        if (!childOk) {
            helper.fail("杂交后代异常: " + all.stream().map(c -> c.getBreedId() + " growth=" + c.getGrowth()).toList());
            return;
        }
        helper.succeed();
    }
}
