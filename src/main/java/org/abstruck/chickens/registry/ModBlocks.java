package org.abstruck.chickens.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.block.breedingbox.BreedingBoxBlock;
import org.abstruck.chickens.block.grower.GrowerBlock;
import org.abstruck.chickens.block.nest.NestBlock;

/** 方块注册：繁殖箱、培育箱、鸡窝 */
public final class ModBlocks {
    /** noOcclusion：模型是镂空结构，不透光会把方块内照暗——noOcclusion 让光穿过（碰撞箱不变） */
    public static final DeferredBlock<Block> BREEDING_BOX = Chickens.BLOCKS.register("breeding_box",
            () -> new BreedingBoxBlock(BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final DeferredBlock<Block> GROWER = Chickens.BLOCKS.register("grower",
            () -> new GrowerBlock(BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final DeferredBlock<Block> NEST = Chickens.BLOCKS.register("nest",
            () -> new NestBlock(BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.WOOD)
                    .noOcclusion()));

    /** 触发类初始化（必须在主类构造期调用） */
    public static void init() {
    }

    private ModBlocks() {
    }
}
