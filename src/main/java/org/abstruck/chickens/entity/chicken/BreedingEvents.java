package org.abstruck.chickens.entity.chicken;

import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import org.abstruck.chickens.config.ChickenConfig;
import org.abstruck.chickens.registry.ModDataComponents;
import org.abstruck.chickens.registry.ModItems;

/**
 * 繁殖改造（规格 2.3，配置 vanillaBreedingRework 开启时生效）：
 * 鸡交配不再直接生成幼体，改为掉落受精鸡蛋（携带品种 + 三维属性）。
 * 原版鸡对 → 无品种组件的蛋（孵出原版鸡）；资源鸡对 → 走 ChickenGenetics；
 * 混合对 → 资源鸡一方的品种 + 属性 1/1/1。
 */
public final class BreedingEvents {
    private BreedingEvents() {
    }

    @SubscribeEvent
    public static void onBabyEntitySpawn(BabyEntitySpawnEvent event) {
        if (!ChickenConfig.VANILLA_BREEDING_REWORK.get()) {
            return;
        }
        if (!(event.getParentA() instanceof Chicken) || !(event.getParentB() instanceof Chicken)) {
            return;
        }
        event.setCanceled(true);

        ItemStack egg = new ItemStack(ModItems.FERTILE_EGG.get());
        if (event.getParentA() instanceof ResourceChicken a && event.getParentB() instanceof ResourceChicken b) {
            ChickenGenetics.OffspringData data = ChickenGenetics.create(
                    a, b, event.getParentA().level().registryAccess(), event.getParentA().getRandom());
            egg.set(ModDataComponents.BREED.get(), data.breed());
            egg.set(ModDataComponents.STATS.get(),
                    new ChickenStats(data.growth(), data.gain(), data.strength()));
        } else if (event.getParentA() instanceof ResourceChicken resource) {
            egg.set(ModDataComponents.BREED.get(), resource.getBreedId());
            egg.set(ModDataComponents.STATS.get(), ChickenStats.DEFAULT);
        } else if (event.getParentB() instanceof ResourceChicken resource) {
            egg.set(ModDataComponents.BREED.get(), resource.getBreedId());
            egg.set(ModDataComponents.STATS.get(), ChickenStats.DEFAULT);
        }
        // 原版鸡对：不加 breed/stats 组件（孵出原版鸡）
        event.getParentA().spawnAtLocation(egg);
    }
}
