package org.abstruck.chickens.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.block.breedingbox.BreedingBoxBlockEntity;
import org.abstruck.chickens.block.grower.GrowerBlockEntity;
import org.abstruck.chickens.block.nest.NestBlockEntity;

/**
 * 方块实体注册。物品能力（漏斗/管道）经 RegisterCapabilitiesEvent 暴露。
 */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Chickens.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BreedingBoxBlockEntity>> BREEDING_BOX =
            BLOCK_ENTITIES.register("breeding_box",
                    () -> BlockEntityType.Builder.of(BreedingBoxBlockEntity::new, ModBlocks.BREEDING_BOX.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GrowerBlockEntity>> GROWER =
            BLOCK_ENTITIES.register("grower",
                    () -> BlockEntityType.Builder.of(GrowerBlockEntity::new, ModBlocks.GROWER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NestBlockEntity>> NEST =
            BLOCK_ENTITIES.register("nest",
                    () -> BlockEntityType.Builder.of(NestBlockEntity::new, ModBlocks.NEST.get()).build(null));

    private ModBlockEntities() {
    }

    /** 漏斗/管道兼容（只暴露输出槽可抽取的视图，输入槽的鸡/种子不被抽走） */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BREEDING_BOX.get(), (be, side) -> be.getExposedItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GROWER.get(), (be, side) -> be.getExposedItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, NEST.get(), (be, side) -> be.getExposedItems());
    }
}
