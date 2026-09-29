package org.abstruck.chickens.block.grower;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 培育箱：幼年鸡 + 小麦种子 → 30 秒 → 成年物品鸡。右键打开菜单。
 * 放置时正面朝向玩家（FACING）；方块内实时渲染槽中的鸡实体（见 GrowerChickenRenderer）。
 */
public class GrowerBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private static final Component TITLE = Component.translatable("container.chickens.grower");

    public GrowerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected MapCodec<GrowerBlock> codec() {
        return simpleCodec(GrowerBlock::new);
    }

    /** 模型渲染交给 BER 统一光照（原版箱子同款 RenderShape）：围栏式模型的内壁
     *  不再逐面取邻格光照（贴实体方块时对侧内壁会全黑） */
    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /** 21.1.252 的 HorizontalDirectionalBlock 没有放置逻辑（FACING 恒为默认 NORTH），
     *  这里按原版熔炉的写法：放置时正面朝向玩家 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GrowerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, blockState, be) -> {
            if (be instanceof GrowerBlockEntity grower) {
                GrowerBlockEntity.tick(lvl, pos, blockState, grower);
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(this.getMenuProvider(state, level, pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        GrowerBlockEntity be = (GrowerBlockEntity) level.getBlockEntity(pos);
        return new SimpleMenuProvider((containerId, inventory, player) ->
                new GrowerMenu(containerId, inventory, be), TITLE);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof GrowerBlockEntity grower) {
            for (int i = 0; i < grower.getItems().getSlots(); i++) {
                ItemStack stack = grower.getItems().getStackInSlot(i);
                if (!stack.isEmpty()) {
                    popResource(level, pos, stack);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
