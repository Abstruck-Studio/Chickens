package org.abstruck.chickens.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.block.breedingbox.BreedingBoxBlock;
import org.abstruck.chickens.block.grower.GrowerBlock;
import org.abstruck.chickens.block.nest.NestBlock;

/**
 * 方块注册（阶段 5：孵化巢、繁殖箱）。
 */
public final class ModBlocks {
    /** 繁殖箱：两只成年鸡配对产出幼年物品鸡（需小麦种子）。
     *  emissiveRendering：渲染自发光——围栏式模型内部无光照会全黑，此开关只影响自身渲染亮度、
     *  不进入世界光照引擎（lightLevel 会真的照亮周围，勿用） */
    public static final DeferredBlock<Block> BREEDING_BOX = Chickens.BLOCKS.register("breeding_box",
            () -> new BreedingBoxBlock(BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.WOOD)
                    .emissiveRendering((state, level, pos) -> true)));

    /** 培育箱：幼年鸡 + 小麦种子 → 成年物品鸡 */
    public static final DeferredBlock<Block> GROWER = Chickens.BLOCKS.register("grower",
            () -> new GrowerBlock(BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.WOOD)
                    .emissiveRendering((state, level, pos) -> true)));

    /** 鸡窝：成年鸡每 30 秒产一次作物 */
    public static final DeferredBlock<Block> NEST = Chickens.BLOCKS.register("nest",
            () -> new NestBlock(BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.WOOD)
                    .emissiveRendering((state, level, pos) -> true)));

    /** 触发类初始化（必须在主类构造期调用，见 ModItems.init 的教训） */
    public static void init() {
    }

    private ModBlocks() {
    }
}
