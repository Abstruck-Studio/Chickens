package org.abstruck.chickens.block;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;

/**
 * 对外（漏斗/管道）暴露的物品视图：插入与底层一致（isItemValid 校验），
 * 但抽取只允许从输出槽进行——防止漏斗把输入槽里的鸡/种子抽走。GUI 菜单不用本视图。
 */
public class ExtractOutputOnlyHandler implements IItemHandler {
    private final IItemHandlerModifiable delegate;
    private final int outputStart;

    public ExtractOutputOnlyHandler(IItemHandlerModifiable delegate, int outputStart) {
        this.delegate = delegate;
        this.outputStart = outputStart;
    }

    @Override
    public int getSlots() {
        return this.delegate.getSlots();
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.delegate.getStackInSlot(slot);
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return this.delegate.insertItem(slot, stack, simulate);
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < this.outputStart) {
            return ItemStack.EMPTY;
        }
        return this.delegate.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return this.delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return this.delegate.isItemValid(slot, stack);
    }
}
