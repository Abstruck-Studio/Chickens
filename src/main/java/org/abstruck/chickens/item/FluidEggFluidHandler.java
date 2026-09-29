package org.abstruck.chickens.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.abstruck.chickens.registry.ModDataComponents;

/**
 * 流体蛋的物品流体能力（{@code Capabilities.FluidHandler.ITEM}）。
 * 这是原版桶与储罐类方块（Create 分液池、各类流体储罐）交互的通用通道：
 * 储罐通过 {@code FluidUtil.tryEmptyContainer} / 直接 drain 读取容器内容。
 * <p>
 * 语义：单槽固定 1 桶（1000 mB）容量，等于组件里的流体；<b>只能倒出、不能装入</b>；
 * EXECUTE 倒出后消耗 1 个蛋（对传入的 ItemStack 引用 shrink，兼容 FluidUtil 的副本契约
 * ——它总是拿副本调 drain，再用 {@link #getContainer()} 的结果替换玩家手部）。
 */
public final class FluidEggFluidHandler implements IFluidHandlerItem {
    private final ItemStack stack;

    public FluidEggFluidHandler(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public ItemStack getContainer() {
        return this.stack;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        Fluid fluid = this.fluid();
        return fluid != null ? new FluidStack(fluid, FluidType.BUCKET_VOLUME) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return FluidType.BUCKET_VOLUME;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack resource) {
        return false; // 蛋不可装入流体
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0; // 蛋不可装入流体
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        Fluid fluid = this.fluid();
        if (fluid == null || !fluid.isSame(resource.getFluid())) {
            return FluidStack.EMPTY;
        }
        return drain(Math.min(resource.getAmount(), FluidType.BUCKET_VOLUME), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        Fluid fluid = this.fluid();
        if (fluid == null || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        FluidStack drained = new FluidStack(fluid, Math.min(maxDrain, FluidType.BUCKET_VOLUME));
        if (action.execute()) {
            this.stack.shrink(1); // 倒出后消耗一个蛋
        }
        return drained;
    }

    /** 组件里的流体；无组件返回 null */
    private Fluid fluid() {
        net.minecraft.core.Holder<Fluid> holder = this.stack.get(ModDataComponents.FLUID.get());
        return holder != null ? holder.value() : null;
    }
}
