package org.abstruck.chickens.entity.chicken;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.abstruck.chickens.breed.BreedLookups;

import java.util.Optional;

/**
 * 自然生成的品种分配（数据 {@code chickens:spawn_rule} 驱动）：
 * 命中规则 → 合并权重抽取品种；未命中任何规则 → 取消该次生成（删规则即可关闭对应生成）。
 * 能否进入某群系的生成池由 {@code #chickens:spawnable_biomes} 标签 + biome modifier 控制。
 *
 * <p>1.20.1 起 Forge 的 {@code LivingSpawnEvent.CheckSpawn} 被移除，改用
 * {@link MobSpawnEvent.FinalizeSpawn}（此时实体已建好、位置已定，可写品种或取消生成）。</p>
 */
public final class ChickenSpawnEvents {
    private ChickenSpawnEvents() {
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        // 只处理自然生成（刷怪蛋/命令/繁殖等不参与品种抽取，也不应被取消）
        MobSpawnType type = event.getSpawnType();
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.CHUNK_GENERATION) {
            return;
        }
        if (!(event.getEntity() instanceof ResourceChicken chicken)) {
            return;
        }
        Holder<Biome> biome = chicken.level().getBiome(chicken.blockPosition());
        Optional<ResourceLocation> breed = BreedLookups.pickSpawnBreed(
                chicken.level().dimension().location(), biome, chicken.getRandom());
        if (breed.isPresent()) {
            chicken.setBreed(breed.get());
        } else {
            // 无规则命中：不生成
            event.setSpawnCancelled(true);
        }
    }
}
