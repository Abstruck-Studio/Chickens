package org.abstruck.chickens.block.nest;

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
 * 鸡窝菜单：成年鸡槽 + 3×3 输出 + 玩家背包；爱心产出进度条由 ContainerData 同步。
 */
public class NestMenu extends AbstractContainerMenu {
    private final NestBlockEntity blockEntity;
    private final ContainerData data;

    public NestMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, (NestBlockEntity) null);
    }

    public NestMenu(int containerId, Inventory playerInventory, NestBlockEntity blockEntity) {
        super(ModMenuTypes.NEST.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = blockEntity != null ? blockEntity.data : new SimpleContainerData(2);
        IItemHandler handler = blockEntity != null ? blockEntity.getItems()
                : new ItemStackHandler(1 + NestBlockEntity.OUTPUT_COUNT);
        this.addDataSlots(this.data);

        this.addSlot(new SlotItemHandler(handler, NestBlockEntity.CHICKEN_IN, 36, 38) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                Boolean mature = stack.get(ModDataComponents.MATURE.get());
                return stack.is(ModItems.CHICKEN.get()) && (mature == null || mature);
            }
        });
        for (int i = 0; i < NestBlockEntity.OUTPUT_COUNT; i++) {
            int slotIndex = NestBlockEntity.OUTPUT_START + i;
            this.addSlot(new SlotItemHandler(handler, slotIndex, 98 + (i % 3) * 18, 20 + (i / 3) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, 9 + row * 9 + col, 8 + col * 18, 87 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 145));
        }
    }

    /** 进度百分比（0~100），爱心条覆盖宽度由 Screen 换算 */
    public int getScaledProgress() {
        int total = NestBlockEntity.maxProgress();
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
            int invStart = NestBlockEntity.OUTPUT_START + NestBlockEntity.OUTPUT_COUNT;
            int invEnd = invStart + 36;
            if (index >= 0 && index < invStart) {
                if (!this.moveItemStackTo(stack, invStart, invEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(ModItems.CHICKEN.get())
                    && !this.moveItemStackTo(stack, NestBlockEntity.CHICKEN_IN,
                    NestBlockEntity.CHICKEN_IN + 1, false)) {
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
                player, ModBlocks.NEST.get());
    }
}
