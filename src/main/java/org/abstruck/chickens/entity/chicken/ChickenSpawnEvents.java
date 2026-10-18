package org.abstruck.chickens.entity.chicken;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.abstruck.chickens.breed.BreedLookups;

import java.util.Optional;

/**
 * 自然生成的品种分配（数据 {@code chickens:spawn_rule} 驱动）：
 * 命中规则 → 合并权重抽取品种；未命中任何规则 → 否决该次生成（删规则即可关闭对应生成）。
 * 能否进入某群系的生成池由 {@code #chickens:spawnable_biomes} 标签 + biome modifier 控制。
 */
public final class ChickenSpawnEvents {
    private ChickenSpawnEvents() {
    }

    @SubscribeEvent
    public static void onCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (event.isSpawner() || !(event.getEntity() instanceof ResourceChicken chicken)) {
            return;
        }
        Holder<Biome> biome = chicken.level.getBiome(chicken.blockPosition());
        Optional<net.minecraft.resources.ResourceLocation> breed = BreedLookups.pickSpawnBreed(
                chicken.level.dimension().location(), biome, chicken.getRandom());
        if (breed.isPresent()) {
            chicken.setBreed(breed.get());
        } else {
            // 无规则命中：不生成
            event.setResult(Event.Result.DENY);
        }
    }
}
