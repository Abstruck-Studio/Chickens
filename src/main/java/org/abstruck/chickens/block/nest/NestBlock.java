package org.abstruck.chickens.block.nest;

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
 * 鸡窝：放入成年鸡 → 按品种节奏产出产物（3×3 输出）。右键打开菜单。
 * 放置时正面朝向玩家；方块内实时渲染槽中的鸡实体（见 ChickenBoxRenderer）。
 */
public class NestBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private static final Component TITLE = Component.translatable("container.chickens.nest");

    public NestBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected MapCodec<NestBlock> codec() {
        return simpleCodec(NestBlock::new);
    }

    /** 渲染交给 BER 统一光照：围栏式模型的内壁不逐面取邻格光照（否则贴实体方块时对侧内壁全黑） */
    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /** 原版熔炉同款：放置时正面朝向玩家（HorizontalDirectionalBlock 本身无放置逻辑，FACING 恒为 NORTH） */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NestBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, blockState, be) -> {
            if (be instanceof NestBlockEntity nest) {
                NestBlockEntity.tick(lvl, pos, blockState, nest);
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
        NestBlockEntity be = (NestBlockEntity) level.getBlockEntity(pos);
        return new SimpleMenuProvider((containerId, inventory, player) ->
                new NestMenu(containerId, inventory, be), TITLE);
    }

    /** 拆除时掉落内容物 */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof NestBlockEntity nest) {
            for (int i = 0; i < nest.getItems().getSlots(); i++) {
                ItemStack stack = nest.getItems().getStackInSlot(i);
                if (!stack.isEmpty()) {
                    popResource(level, pos, stack);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
