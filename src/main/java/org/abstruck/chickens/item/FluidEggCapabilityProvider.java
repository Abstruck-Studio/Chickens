package org.abstruck.chickens.item;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** 流体蛋的物品能力提供者：只有 {@code ForgeCapabilities.FLUID_HANDLER_ITEM} */
public class FluidEggCapabilityProvider implements ICapabilityProvider {
    private final ItemStack stack;
    private final LazyOptional<IFluidHandlerItem> handler;

    public FluidEggCapabilityProvider(ItemStack stack) {
        this.stack = stack;
        this.handler = LazyOptional.of(() -> new FluidEggFluidHandler(stack));
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == ForgeCapabilities.FLUID_HANDLER_ITEM ? this.handler.cast() : LazyOptional.empty();
    }
}
