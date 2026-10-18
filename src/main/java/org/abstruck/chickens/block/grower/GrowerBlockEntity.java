package org.abstruck.chickens.block.grower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.abstruck.chickens.block.ExtractOutputOnlyHandler;
import org.abstruck.chickens.config.ChickenConfig;
import org.abstruck.chickens.item.ItemData;
import org.abstruck.chickens.registry.ModRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 培育箱方块实体：幼年鸡槽 + 种子槽 + 成年鸡输出槽。
 * 幼鸡 + 种子 + 输出可放 → 爱心进度（30 秒）→ 幼鸡变成年物品鸡（保留品种/属性），消耗 1 种子。
 */
public class GrowerBlockEntity extends BlockEntity {
    public static final int CHICKEN_IN = 0;
    public static final int SEEDS = 1;
    public static final int CHICKEN_OUT = 2;
    public static final int PROGRESS_STEP = 100;

    private final ItemStackHandler items = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (slot == CHICKEN_OUT) {
                return false;
            }
            if (slot == SEEDS) {
                return stack.is(net.minecraftforge.common.Tags.Items.SEEDS);
            }
            // 只收幼年鸡物品（成年鸡放入无反应）
            return stack.is(ModRegistry.CHICKEN.get()) && !ItemData.isMature(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            GrowerBlockEntity.this.setChanged();
            GrowerBlockEntity.this.syncToClients();
        }
    };

    /** 对外（漏斗/管道）视图：输入槽不可被抽取 */
    private final IItemHandler exposedItems = new ExtractOutputOnlyHandler(this.items, CHICKEN_OUT);
    private final LazyOptional<IItemHandler> exposedOptional = LazyOptional.of(() -> this.exposedItems);

    private int progress;

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? GrowerBlockEntity.this.progress : maxProgress();
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                GrowerBlockEntity.this.progress = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public GrowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.GROWER_BE.get(), pos, state);
    }

    public static int maxProgress() {
        return ChickenConfig.GROWER_TIME.get() * PROGRESS_STEP;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GrowerBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        if (!be.canGrow()) {
            be.progress = 0;
            return;
        }
        be.progress += PROGRESS_STEP;
        if (be.progress >= maxProgress()) {
            be.progress = 0;
            be.grow();
        }
    }

    /** 输出槽为空，或已有同品种同属性的成年鸡且未满堆叠 → 可继续培育 */
    private boolean canGrow() {
        if (this.items.getStackInSlot(CHICKEN_IN).isEmpty() || this.items.getStackInSlot(SEEDS).isEmpty()) {
            return false;
        }
        ItemStack out = this.items.getStackInSlot(CHICKEN_OUT);
        if (out.isEmpty()) {
            return true;
        }
        ItemStack baby = this.items.getStackInSlot(CHICKEN_IN);
        // 输入幼鸡转成年态后与输出槽比较（同数据才可合并堆叠）
        ItemStack expected = baby.copy();
        expected.setCount(1);
        ItemData.setMature(expected, true);
        return !ItemData.isMature(baby)
                && ItemStack.isSameItemSameTags(out, expected)
                && out.getCount() < out.getMaxStackSize();
    }

    private void grow() {
        ItemStack baby = this.items.getStackInSlot(CHICKEN_IN);
        if (baby.isEmpty()) {
            return;
        }
        ItemStack adult = baby.copy();
        adult.setCount(1);
        ItemData.setMature(adult, true);
        this.items.getStackInSlot(SEEDS).shrink(1);
        baby.shrink(1);
        ItemStack out = this.items.getStackInSlot(CHICKEN_OUT);
        if (out.isEmpty()) {
            this.items.setStackInSlot(CHICKEN_OUT, adult);
        } else {
            out.grow(1); // canGrow 已保证同数据且未满
        }
        this.setChanged();
    }

    public ItemStackHandler getItems() {
        return this.items;
    }

    /** 给漏斗/管道用的物品能力视图（输出槽可抽，鸡/种子槽不可抽） */
    public IItemHandler getExposedItems() {
        return this.exposedItems;
    }

    public int getProgress() {
        return this.progress;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return this.exposedOptional.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved() {
        this.exposedOptional.invalidate();
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", this.items.serializeNBT());
        tag.putInt("Progress", this.progress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            this.items.deserializeNBT(tag.getCompound("Inventory"));
        }
        this.progress = tag.getInt("Progress");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        this.saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncToClients() {
        if (this.level instanceof ServerLevel serverLevel && !serverLevel.isClientSide) {
            ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(this);
            for (ServerPlayer player : serverLevel.getChunkSource().chunkMap
                    .getPlayers(new ChunkPos(this.worldPosition), false)) {
                player.connection.send(packet);
            }
        }
    }
}
