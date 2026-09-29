package org.abstruck.chickens.entity.chicken;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import org.abstruck.chickens.breed.BreedLookups;

/**
 * 自然生成的品种分配（数据包注册表 {@code chickens:spawn_rule} 驱动）：
 * 生成位置命中规则 → 合并权重抽取品种；未命中任何规则 → 否决该次生成
 * （规则表是唯一真相，数据包删规则即可关闭对应群系的生成）。
 * 生成入口（鸡能否进入某群系的生成池）由 #chickens:spawnable_biomes 标签
 * + biome modifier 控制，见 data/chickens/neoforge/biome_modifier/。
 */
public final class ChickenSpawnEvents {
    private ChickenSpawnEvents() {
    }

    @SubscribeEvent
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        MobSpawnType spawnType = event.getSpawnType();
        if ((spawnType != MobSpawnType.NATURAL && spawnType != MobSpawnType.CHUNK_GENERATION)
                || !(event.getEntity() instanceof ResourceChicken chicken)) {
            return;
        }
        RegistryAccess access = chicken.level().registryAccess();
        Holder<Biome> biome = chicken.level().getBiome(chicken.blockPosition());
        BreedLookups.pickSpawnBreed(access, chicken.level().dimension().location(), biome, chicken.getRandom())
                .ifPresentOrElse(
                        chicken::setBreed,
                        () -> event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL)); // 无规则命中：不生成
    }
}
