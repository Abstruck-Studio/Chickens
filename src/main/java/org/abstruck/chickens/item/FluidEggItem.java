package org.abstruck.chickens.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.abstruck.chickens.registry.ModDataComponents;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 流体蛋：一个物品承载所有流体，流体类型存在 {@code chickens:fluid} 组件。
 * 放置流体的实现与原版 {@link net.minecraft.world.item.BucketItem} 完全一致
 * （同样只重写 {@link #use}：右键方块的 useOn 链路返回 PASS 后客户端会自动
 * fallback 到 useItem → 本方法）：
 * <ul>
 *   <li>目标格选择：点击的方块能装流体（{@link LiquidBlockContainer}）就用点击格，否则用点击面相邻格</li>
 *   <li>放置顺序：{@code LiquidBlockContainer.placeLiquid}（其他模组储罐的兼容入口）→
 *       流体汽化钩子 → 下界水蒸发 → 直接放置流体方块</li>
 *   <li>音效走 {@code FluidType.getSound} 钩子并发送 {@code FLUID_PLACE} 游戏事件</li>
 * </ul>
 * 与桶的不同：只能**倒出**流体源（不能装）；消耗 1 个蛋（创造不消耗）；可堆叠。
 */
public class FluidEggItem extends Item {
    public FluidEggItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        net.minecraft.core.Holder<Fluid> fluidHolder = stack.get(ModDataComponents.FLUID.get());
        if (fluidHolder == null) {
            return;
        }
        // 用流体源方块的本地化名（如「水」）
        tooltip.add(fluidHolder.value().defaultFluidState().createLegacyBlock().getBlock().getName());
    }

    /**
     * 复刻 BucketItem.use 的放置分支（本物品不能装流体，无吸取分支）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hitResult.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(stack);
        } else if (hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        } else {
            BlockPos clickedPos = hitResult.getBlockPos();
            Direction direction = hitResult.getDirection();
            BlockPos adjacentPos = clickedPos.relative(direction);
            if (!level.mayInteract(player, clickedPos) || !player.mayUseItemAt(adjacentPos, direction, stack)) {
                return InteractionResultHolder.fail(stack);
            }
            net.minecraft.core.Holder<Fluid> fluidHolder = stack.get(ModDataComponents.FLUID.get());
            Fluid fluid = fluidHolder != null ? fluidHolder.value() : null;
            if (!(fluid instanceof FlowingFluid flowing)) {
                return InteractionResultHolder.fail(stack); // 未定义的流体：不可倒出
            }
            BlockState clickedState = level.getBlockState(clickedPos);
            BlockPos target = canBlockContainFluid(player, level, clickedPos, clickedState, fluid) ? clickedPos : adjacentPos;
            if (this.emptyContents(player, level, target, flowing)) {
                if (player instanceof ServerPlayer serverPlayer) {
                    CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, target, stack);
                }
                player.awardStat(Stats.ITEM_USED.get(this));
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
            return InteractionResultHolder.fail(stack);
        }
    }

    /**
     * 判断方块能否接收该流体（原版桶同款：LiquidBlockContainer 接口）。
     */
    private boolean canBlockContainFluid(@Nullable Player player, Level level, BlockPos pos, BlockState state, Fluid fluid) {
        return state.getBlock() instanceof LiquidBlockContainer liquidContainer
                && liquidContainer.canPlaceLiquid(player, level, pos, state, fluid);
    }

    /**
     * 复刻 BucketItem.emptyContents 的放置逻辑（含其他模组储罐兼容、汽化钩子、下界水蒸发）。
     */
    private boolean emptyContents(@Nullable Player player, Level level, BlockPos pos, FlowingFluid flowing) {
        BlockState blockstate = level.getBlockState(pos);
        Block block = blockstate.getBlock();
        boolean canBeReplaced = blockstate.canBeReplaced(flowing);
        boolean canPlace = blockstate.isAir() || canBeReplaced
                || block instanceof LiquidBlockContainer liquidContainer
                && liquidContainer.canPlaceLiquid(player, level, pos, blockstate, flowing);
        if (!canPlace) {
            return false;
        }
        FluidStack fluidStack = new FluidStack(flowing, FluidType.BUCKET_VOLUME);
        // 自定义流体汽化钩子（原版桶同款，NeoForge FluidType 扩展点）
        if (flowing.getFluidType().isVaporizedOnPlacement(level, pos, fluidStack)) {
            flowing.getFluidType().onVaporize(player, level, pos, fluidStack);
            return true;
        }
        // 下界水蒸发（原版桶同款音效与粒子）
        if (level.dimensionType().ultraWarm() && flowing.is(FluidTags.WATER)) {
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            level.playSound(player, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
                    0.5F, 2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);
            for (int i = 0; i < 8; i++) {
                level.addParticle(ParticleTypes.LARGE_SMOKE,
                        (double) x + Math.random(), (double) y + Math.random(), (double) z + Math.random(),
                        0.0, 0.0, 0.0);
            }
            return true;
        }
        // 能装流体的方块（其他模组储罐）：把流体交给方块处理，而不是直接放置流体方块
        if (block instanceof LiquidBlockContainer liquidContainer
                && liquidContainer.canPlaceLiquid(player, level, pos, blockstate, flowing)) {
            liquidContainer.placeLiquid(level, pos, blockstate, flowing.getSource(false));
            this.playEmptySound(player, level, pos, flowing);
            return true;
        }
        if (!level.isClientSide && canBeReplaced && !blockstate.liquid()) {
            level.destroyBlock(pos, true);
        }
        if (!level.setBlock(pos, flowing.defaultFluidState().createLegacyBlock(), 11) && !blockstate.getFluidState().isSource()) {
            return false;
        }
        this.playEmptySound(player, level, pos, flowing);
        return true;
    }

    /**
     * 复刻 BucketItem.playEmptySound：音效走 FluidType.getSound 钩子（岩浆/其他流体各配其音），
     * 并发送 FLUID_PLACE 游戏事件（幽匿感测体等监听）。
     */
    private void playEmptySound(@Nullable Player player, LevelAccessor level, BlockPos pos, Fluid fluid) {
        SoundEvent soundEvent = fluid.getFluidType().getSound(player, level, pos, SoundActions.BUCKET_EMPTY);
        if (soundEvent == null) {
            soundEvent = fluid.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
        }
        level.playSound(player, pos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
    }
}
