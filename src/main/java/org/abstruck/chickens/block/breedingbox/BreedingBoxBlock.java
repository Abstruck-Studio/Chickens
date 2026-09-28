package org.abstruck.chickens.block.breedingbox;

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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 繁殖箱：UI 里放两只鸡 → 爱心进度 → 输出受精蛋。
 * 漏斗/管道经能力系统存取。放置时正面朝向玩家（FACING）；
 * HAS_SEEDS / IS_BREEDING 驱动外观变体（无种子空窗帘 / 繁殖中拉窗帘，1.12 roost 同款）。
 */
public class BreedingBoxBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty HAS_SEEDS = BooleanProperty.create("has_seeds");
    public static final BooleanProperty IS_BREEDING = BooleanProperty.create("is_breeding");
    private static final Component TITLE = Component.translatable("container.chickens.breeding_box");

    public BreedingBoxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HAS_SEEDS, false)
                .setValue(IS_BREEDING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_SEEDS, IS_BREEDING);
    }

    @Override
    protected MapCodec<BreedingBoxBlock> codec() {
        return simpleCodec(BreedingBoxBlock::new);
    }

    /** 21.1.252 的 HorizontalDirectionalBlock 没有放置逻辑（FACING 恒为默认 NORTH），
     *  这里按原版熔炉的写法：放置时正面朝向玩家 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BreedingBoxBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, blockState, be) -> {
            if (be instanceof BreedingBoxBlockEntity box) {
                BreedingBoxBlockEntity.tick(lvl, pos, blockState, box);
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
        BreedingBoxBlockEntity be = (BreedingBoxBlockEntity) level.getBlockEntity(pos);
        return new SimpleMenuProvider((containerId, inventory, player) ->
                new BreedingBoxMenu(containerId, inventory, be), TITLE);
    }

    /** 拆除时掉落内容物 */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BreedingBoxBlockEntity box) {
            for (int i = 0; i < box.getItems().getSlots(); i++) {
                ItemStack stack = box.getItems().getStackInSlot(i);
                if (!stack.isEmpty()) {
                    popResource(level, pos, stack);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
