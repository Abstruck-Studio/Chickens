package org.abstruck.chickens.entity.chicken;

import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.registry.ModRegistry;

/** 实体属性与生成位置规则（1.19.2 用 Forge 的事件注册） */
@Mod.EventBusSubscriber(modid = Chickens.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntitySetup {
    private ModEntitySetup() {
    }

    /** 属性与原版鸡一致 */
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModRegistry.RESOURCE_CHICKEN.get(), net.minecraft.world.entity.animal.Chicken.createAttributes().build());
    }

    /** 生成位置规则按维度分流：主世界用动物规则（要求明亮），下界/末地用怪物规则（要求黑暗，不带 PEACEFUL 检查） */
    @SubscribeEvent
    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(ModRegistry.RESOURCE_CHICKEN.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) -> {
                    if (level instanceof net.minecraft.server.level.ServerLevel serverLevel
                            && (serverLevel.dimension() == Level.NETHER || serverLevel.dimension() == Level.END)) {
                        return Monster.isDarkEnoughToSpawn(serverLevel, pos, random);
                    }
                    return Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random);
                },
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
