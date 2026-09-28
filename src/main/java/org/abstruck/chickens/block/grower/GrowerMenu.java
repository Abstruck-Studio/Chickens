package org.abstruck.chickens.block.grower;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.abstruck.chickens.registry.ModBlocks;
import org.abstruck.chickens.registry.ModDataComponents;
import org.abstruck.chickens.registry.ModItems;
import org.abstruck.chickens.registry.ModMenuTypes;

/**
 * 培育箱菜单：幼年鸡输入槽 + 种子槽 + 成年鸡输出槽 + 玩家背包。
 */
public class GrowerMenu extends AbstractContainerMenu {
    private final GrowerBlockEntity blockEntity;
    private final ContainerData data;

    public GrowerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, (GrowerBlockEntity) null);
    }

    public GrowerMenu(int containerId, Inventory playerInventory, GrowerBlockEntity blockEntity) {
        super(ModMenuTypes.GROWER.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = blockEntity != null ? blockEntity.data : new SimpleContainerData(2);
        IItemHandler handler = blockEntity != null ? blockEntity.getItems() : new ItemStackHandler(3);
        this.addDataSlots(this.data);

        this.addSlot(new SlotItemHandler(handler, GrowerBlockEntity.CHICKEN_IN, 62, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                Boolean mature = stack.get(ModDataComponents.MATURE.get());
                return stack.is(ModItems.CHICKEN.get()) && mature != null && !mature;
            }
        });
        this.addSlot(new SlotItemHandler(handler, GrowerBlockEntity.SEEDS, 26, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(net.neoforged.neoforge.common.Tags.Items.SEEDS); // 任意种子（c:seeds）
            }
        });
        this.addSlot(new SlotItemHandler(handler, GrowerBlockEntity.CHICKEN_OUT, 134, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, 9 + row * 9 + col, 8 + col * 18, 51 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 109));
        }
    }

    /** 进度百分比（0~100），爱心条覆盖宽度由 Screen 换算 */
    public int getScaledProgress() {
        int total = GrowerBlockEntity.maxProgress();
        int progress = this.data.get(0);
        return total == 0 ? 0 : progress * 100 / total;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invStart = 3;
            int invEnd = invStart + 36;
            if (index >= 0 && index < invStart) {
                if (!this.moveItemStackTo(stack, invStart, invEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(ModItems.CHICKEN.get())
                    && !this.moveItemStackTo(stack, GrowerBlockEntity.CHICKEN_IN,
                    GrowerBlockEntity.CHICKEN_IN + 1, false)) {
                if (!this.moveItemStackTo(stack, GrowerBlockEntity.SEEDS,
                        GrowerBlockEntity.SEEDS + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(net.neoforged.neoforge.common.Tags.Items.SEEDS)
                    && !this.moveItemStackTo(stack, GrowerBlockEntity.SEEDS,
                    GrowerBlockEntity.SEEDS + 1, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.blockEntity == null) {
            return true;
        }
        return AbstractContainerMenu.stillValid(
                ContainerLevelAccess.create(this.blockEntity.getLevel(), this.blockEntity.getBlockPos()),
                player, ModBlocks.GROWER.get());
    }
}
