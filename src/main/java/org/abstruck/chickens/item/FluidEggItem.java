package org.abstruck.chickens.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.abstruck.chickens.registry.ModDataComponents;

import java.util.List;

/**
 * 流体蛋（阶段 6.15，规格 1.6 的液体蛋）：一个物品承载所有流体，
 * 流体类型存在 {@code chickens:fluid} 组件。右键方块像桶一样**倒出**流体源（不能装）；
 * 蛋的颜色由配置文件 fluidEggs 决定（见 ChickenConfig.colorOfFluid），渲染实时染色。
 * 可堆叠，鸡窝生产不会被桶卡格。
 */
public class FluidEggItem extends Item {
    public FluidEggItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation fluidId = stack.get(ModDataComponents.FLUID.get());
        if (fluidId == null) {
            return;
        }
        // 优先用流体源方块的本地化名（如「水」）；注册表不可用时退回流体注册名
        Component name = Component.literal(fluidId.toString());
        if (context.registries() != null) {
            Fluid fluid = context.registries().lookupOrThrow(Registries.FLUID)
                    .get(net.minecraft.resources.ResourceKey.create(Registries.FLUID, fluidId))
                    .map(net.minecraft.core.Holder::value).orElse(null);
            if (fluid != null) {
                name = fluid.defaultFluidState().createLegacyBlock().getBlock().getName();
            }
        }
        tooltip.add(name);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        ResourceLocation fluidId = stack.get(ModDataComponents.FLUID.get());
        Fluid fluid = fluidId != null
                ? level.registryAccess().registryOrThrow(Registries.FLUID).get(fluidId)
                : null;
        if (fluid == null) {
            return InteractionResult.FAIL; // 未定义的流体：不可倒出
        }
        // 完全模仿原版桶的 emptyContents 放置逻辑：
        // 点击的格子可替换/是空气 → 就在该格放；否则放到点击面的相邻格（仅可替换处）
        BlockPos clicked = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clicked);
        BlockPos target;
        if (clickedState.isAir() || clickedState.canBeReplaced(fluid)) {
            target = clicked;
        } else {
            target = clicked.relative(context.getClickedFace());
            BlockState adjacent = level.getBlockState(target);
            if (!adjacent.isAir() && !adjacent.canBeReplaced(fluid)) {
                return InteractionResult.FAIL;
            }
        }
        if (!level.isClientSide) {
            level.setBlock(target, fluid.defaultFluidState().createLegacyBlock(), 3);
            level.playSound(null, target, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
