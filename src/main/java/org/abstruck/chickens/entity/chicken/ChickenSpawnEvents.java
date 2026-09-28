package org.abstruck.chickens.entity.chicken;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/**
 * 自然生成的品种分配：
 * 主世界——燧石鸡、原木鸡、沙鸡三选一；下界——灵魂沙峡谷灵魂沙鸡与石英鸡对半，其他群系石英鸡。
 */
public final class ChickenSpawnEvents {
    private static final TagKey<Biome> SOUL_SAND_VALLEY =
            TagKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("soul_sand_valley"));

    private ChickenSpawnEvents() {
    }

    @SubscribeEvent
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        MobSpawnType spawnType = event.getSpawnType();
        if ((spawnType != MobSpawnType.NATURAL && spawnType != MobSpawnType.CHUNK_GENERATION)
                || !(event.getEntity() instanceof ResourceChicken chicken)) {
            return;
        }
        chicken.setBreed(pickBreed(chicken));
    }

    private static ResourceLocation pickBreed(ResourceChicken chicken) {
        Holder<Biome> biome = chicken.level().getBiome(chicken.blockPosition());
        if (chicken.level().dimension() == Level.NETHER) {
            return breed(biome.is(SOUL_SAND_VALLEY)
                    ? (chicken.getRandom().nextBoolean() ? "soul_sand" : "quartz")
                    : "quartz");
        }
        int roll = chicken.getRandom().nextInt(3);
        return breed(switch (roll) {
            case 0 -> "flint";
            case 1 -> "log";
            default -> "sand";
        });
    }

    private static ResourceLocation breed(String id) {
        return ResourceLocation.fromNamespaceAndPath("chickens", id);
    }
}
