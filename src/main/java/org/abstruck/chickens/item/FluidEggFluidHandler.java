package org.abstruck.chickens.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

/**
 * 流体蛋的流体能力（FluidHandler.ITEM）：1 桶（1000 mB）容量 = 物品里的流体，
 * 与原版桶及各类储罐交互的通用通道；只能倒出不能装入；
 * EXECUTE 倒出后消耗 1 个蛋（FluidUtil 拿副本调 drain，再用 getContainer() 结果替换手部物品）。
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
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        Fluid fluid = this.fluid();
        if (fluid == null || resource.isEmpty() || !fluid.isSame(resource.getFluid())) {
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
            this.stack.shrink(1);
        }
        return drained;
    }

    private Fluid fluid() {
        return ItemData.getFluid(this.stack);
    }
}
