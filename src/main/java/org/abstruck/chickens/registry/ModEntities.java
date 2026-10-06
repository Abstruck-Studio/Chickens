package org.abstruck.chickens.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Chicken;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.entity.ThrownDyeEgg;
import org.abstruck.chickens.entity.chicken.ResourceChicken;

/** 实体类型注册 */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Chickens.MODID);

    /** 唯一实体类型，所有品种共用（品种存在实体的同步数据里）。
     *  canSpawnFarFromPlayer：解除 creature 的「玩家 32 格内」限制（下界走 monster 通道时生成位置在 24~128 格） */
    public static final DeferredHolder<EntityType<?>, EntityType<ResourceChicken>> RESOURCE_CHICKEN =
            ENTITY_TYPES.register("resource_chicken",
                    () -> EntityType.Builder.of(ResourceChicken::new, MobCategory.CREATURE)
                            .sized(0.4F, 0.7F).canSpawnFarFromPlayer().build("resource_chicken"));

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownDyeEgg>> THROWN_DYE_EGG =
            ENTITY_TYPES.register("thrown_dye_egg",
                    () -> EntityType.Builder.<ThrownDyeEgg>of(ThrownDyeEgg::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).build("thrown_dye_egg"));

    private ModEntities() {
    }

    /** 属性与原版鸡一致 */
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(RESOURCE_CHICKEN.get(), Chicken.createAttributes().build());
    }

    /** 生成位置规则按维度分流：主世界用动物规则（要求明亮），下界/末地用怪物规则（要求黑暗，不带 PEACEFUL 检查） */
    public static void registerSpawnPlacements(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) {
        event.register(RESOURCE_CHICKEN.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) -> {
                    if (level instanceof net.minecraft.server.level.ServerLevel serverLevel
                            && (serverLevel.dimension() == net.minecraft.world.level.Level.NETHER
                            || serverLevel.dimension() == net.minecraft.world.level.Level.END)) {
                        return net.minecraft.world.entity.monster.Monster.isDarkEnoughToSpawn(serverLevel, pos, random);
                    }
                    return net.minecraft.world.entity.animal.Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random);
                },
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
