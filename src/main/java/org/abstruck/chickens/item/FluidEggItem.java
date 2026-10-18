package org.abstruck.chickens.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import org.abstruck.chickens.breed.BreedLookups;

import javax.annotation.Nullable;
import java.util.List;
import java.util.OptionalInt;

/**
 * 流体蛋：一个物品承载所有流体，流体类型存在物品 NBT 里；放置逻辑复刻原版 BucketItem
 * （目标格选择、其他模组储罐兼容（LiquidBlockContainer）、汽化钩子、下界水蒸发）。
 * 与桶不同：只能倒出、不能装；消耗 1 个蛋（创造不消耗）；可堆叠。
 */
public class FluidEggItem extends Item {
    public FluidEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Fluid fluid = ItemData.getFluid(stack);
        if (fluid == null) {
            return;
        }
        // 用流体源方块的本地化名（如「水」）
        Component name = fluid.defaultFluidState().createLegacyBlock().getBlock().getName();
        // 颜色来自数据内容 chickens:fluid_egg（与物品模型染色同源）
        ResourceLocation fluidId = ItemData.getFluidId(stack);
        OptionalInt color = BreedLookups.colorOfFluid(fluidId);
        if (color.isPresent()) {
            name = name.copy().withStyle(Style.EMPTY.withColor(0xFF000000 | color.getAsInt()));
        }
        tooltip.add(name);
    }

    /** 复刻 BucketItem.use 的放置分支（本物品不能装流体，无吸取分支） */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        BlockPos clickedPos = hitResult.getBlockPos();
        Direction direction = hitResult.getDirection();
        BlockPos adjacentPos = clickedPos.relative(direction);
        if (!level.mayInteract(player, clickedPos) || !player.mayUseItemAt(adjacentPos, direction, stack)) {
            return InteractionResultHolder.fail(stack);
        }
        Fluid fluid = ItemData.getFluid(stack);
        if (!(fluid instanceof FlowingFluid flowing)) {
            return InteractionResultHolder.fail(stack);
        }
        BlockState clickedState = level.getBlockState(clickedPos);
        BlockPos target = canBlockContainFluid(level, clickedPos, clickedState, fluid)
                ? clickedPos : adjacentPos;
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

    /** 原版桶同款：方块实现 LiquidBlockContainer（储罐等）才能接收该流体 */
    private boolean canBlockContainFluid(Level level, BlockPos pos, BlockState state,
                                         Fluid fluid) {
        return state.getBlock() instanceof LiquidBlockContainer liquidContainer
                && liquidContainer.canPlaceLiquid(level, pos, state, fluid);
    }

    /** 复刻 BucketItem.emptyContents 的放置逻辑（含其他模组储罐兼容、汽化钩子、下界水蒸发） */
    private boolean emptyContents(@Nullable Player player, Level level, BlockPos pos, FlowingFluid flowing) {
        BlockState blockstate = level.getBlockState(pos);
        Block block = blockstate.getBlock();
        boolean canBeReplaced = blockstate.canBeReplaced(flowing);
        boolean canPlace = blockstate.isAir() || canBeReplaced
                || block instanceof LiquidBlockContainer liquidContainer
                && liquidContainer.canPlaceLiquid(level, pos, blockstate, flowing);
        if (!canPlace) {
            return false;
        }
        FluidStack fluidStack = new FluidStack(flowing, FluidType.BUCKET_VOLUME);
        // 自定义流体汽化钩子（Forge FluidType 扩展点）
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
        // 其他模组储罐：把流体交给方块处理，而不是直接放置流体方块
        if (block instanceof LiquidBlockContainer liquidContainer
                && liquidContainer.canPlaceLiquid(level, pos, blockstate, flowing)) {
            liquidContainer.placeLiquid(level, pos, blockstate, flowing.getSource(false));
            this.playEmptySound(player, level, pos, flowing);
            return true;
        }
        if (!level.isClientSide && canBeReplaced && !blockstate.getMaterial().isLiquid()) {
            level.destroyBlock(pos, true);
        }
        if (!level.setBlock(pos, flowing.defaultFluidState().createLegacyBlock(), 11)
                && !blockstate.getFluidState().isSource()) {
            return false;
        }
        this.playEmptySound(player, level, pos, flowing);
        return true;
    }

    /** 原版桶同款音效：走 FluidType 的 sound 钩子并发送 FLUID_PLACE 游戏事件 */
    private void playEmptySound(@Nullable Player player, LevelAccessor level, BlockPos pos, Fluid fluid) {
        SoundEvent soundEvent = fluid.getFluidType().getSound(player, level, pos, SoundActions.BUCKET_EMPTY);
        if (soundEvent == null) {
            soundEvent = fluid.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
        }
        level.playSound(player, pos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
    }

    /** 暴露物品流体能力（IFluidHandlerItem），储罐类模组经 FluidUtil 读取即可倒入 */
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FluidEggCapabilityProvider(stack);
    }
}
