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
 * 方块注册：繁殖箱、培育箱、鸡窝。
 */
public final class ModBlocks {
    /** 繁殖箱：两只成年鸡配对产出幼年物品鸡（需小麦种子）。
     *  noOcclusion：模型是围栏式镂空结构，但碰撞箱是完整方块——若不透光，光照引擎会把
     *  它当不透明遮挡体，光进不来 → 方块内鸡发暗、模型内侧面全黑。noOcclusion 让光
     *  自由穿过（原版栅栏同款），方块格光照与周围世界一致；碰撞箱不动，交互不受影响 */
    public static final DeferredBlock<Block> BREEDING_BOX = Chickens.BLOCKS.register("breeding_box",
            () -> new BreedingBoxBlock(BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.WOOD)
                    .noOcclusion()));

    /** 培育箱：幼年鸡 + 小麦种子 → 成年物品鸡 */
    public static final DeferredBlock<Block> GROWER = Chickens.BLOCKS.register("grower",
            () -> new GrowerBlock(BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.WOOD)
                    .noOcclusion()));

    /** 鸡窝：成年鸡每 30 秒产一次作物 */
    public static final DeferredBlock<Block> NEST = Chickens.BLOCKS.register("nest",
            () -> new NestBlock(BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.WOOD)
                    .noOcclusion()));

    /** 触发类初始化（必须在主类构造期调用，见 ModItems.init 的教训） */
    public static void init() {
    }

    private ModBlocks() {
    }
}
