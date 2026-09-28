package org.abstruck.chickens.block;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 对外（漏斗/管道）暴露的物品视图：插入行为与底层 handler 一致（isItemValid 校验），
 * 但抽取只允许从输出槽进行——防止漏斗把输入槽里的鸡/种子抽走。
 * GUI 菜单不使用本视图（玩家在界面里拿取仍走原始 handler）。
 */
public class ExtractOutputOnlyHandler implements IItemHandler {
    private final IItemHandler delegate;
    private final int outputStart;

    public ExtractOutputOnlyHandler(IItemHandler delegate, int outputStart) {
        this.delegate = delegate;
        this.outputStart = outputStart;
    }

    @Override
    public int getSlots() {
        return this.delegate.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.delegate.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return this.delegate.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < this.outputStart) {
            return ItemStack.EMPTY; // 输入槽（鸡/种子）不可被抽取
        }
        return this.delegate.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return this.delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return this.delegate.isItemValid(slot, stack);
    }
}
