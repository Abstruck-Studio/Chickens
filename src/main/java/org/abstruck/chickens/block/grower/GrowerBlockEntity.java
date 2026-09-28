package org.abstruck.chickens.block.grower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.abstruck.chickens.block.ExtractOutputOnlyHandler;
import org.abstruck.chickens.config.ChickenConfig;
import org.abstruck.chickens.registry.ModBlockEntities;
import org.abstruck.chickens.registry.ModDataComponents;
import org.abstruck.chickens.registry.ModItems;

/**
 * 培育箱方块实体：幼年鸡槽 + 种子槽 + 成年鸡输出槽。
 * 幼鸡 + 种子 + 输出空 → 爱心进度（30 秒）→ 幼鸡变**成年物品鸡**（保留品种/属性），消耗 1 种子。
 * 放入成年鸡无反应（输入槽只收幼年鸡）。
 */
public class GrowerBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    public static final int CHICKEN_IN = 0;
    public static final int SEEDS = 1;
    public static final int CHICKEN_OUT = 2;
    public static final int PROGRESS_STEP = 100;

    private final ItemStackHandler items = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == CHICKEN_OUT) {
                return false;
            }
            if (slot == SEEDS) {
                return stack.is(net.neoforged.neoforge.common.Tags.Items.SEEDS); // 任意种子（c:seeds）
            }
            // 只收幼年鸡物品（成年鸡放入无反应）
            Boolean mature = stack.get(ModDataComponents.MATURE.get());
            return stack.is(ModItems.CHICKEN.get()) && mature != null && !mature;
        }

        @Override
        protected void onContentsChanged(int slot) {
            GrowerBlockEntity.this.setChanged();
            GrowerBlockEntity.this.syncToClients();
        }
    };

    /** 对外（漏斗/管道）视图：输入槽不可被抽取 */
    private final IItemHandler exposedItems = new ExtractOutputOnlyHandler(this.items, CHICKEN_OUT);

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
        super(ModBlockEntities.GROWER.get(), pos, state);
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

    /** 输出槽为空，或已有同品种同组件的成年鸡且未满堆叠上限 → 可继续培育（同品种鸡在输出槽堆叠） */
    private boolean canGrow() {
        if (this.items.getStackInSlot(CHICKEN_IN).isEmpty() || this.items.getStackInSlot(SEEDS).isEmpty()) {
            return false;
        }
        ItemStack out = this.items.getStackInSlot(CHICKEN_OUT);
        if (out.isEmpty()) {
            return true;
        }
        ItemStack baby = this.items.getStackInSlot(CHICKEN_IN);
        Boolean babyMature = baby.get(ModDataComponents.MATURE.get());
        boolean babyIsBaby = babyMature != null && !babyMature;
        // 输出槽里的成年鸡必须与输入幼鸡同品种同组件（含品种/属性），否则等玩家取走
        ItemStack expected = baby.copyWithCount(1);
        expected.set(ModDataComponents.MATURE.get(), true);
        return babyIsBaby && ItemStack.isSameItemSameComponents(out, expected)
                && out.getCount() < out.getMaxStackSize();
    }

    private void grow() {
        ItemStack baby = this.items.getStackInSlot(CHICKEN_IN);
        if (baby.isEmpty()) {
            return;
        }
        ItemStack adult = baby.copyWithCount(1);
        adult.set(ModDataComponents.MATURE.get(), true); // 成年
        this.items.getStackInSlot(SEEDS).shrink(1);
        baby.shrink(1);
        ItemStack out = this.items.getStackInSlot(CHICKEN_OUT);
        if (out.isEmpty()) {
            this.items.setStackInSlot(CHICKEN_OUT, adult);
        } else {
            out.grow(1); // 同品种成年鸡堆叠（canGrow 已保证同组件且未满）
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", this.items.serializeNBT(registries));
        tag.putInt("Progress", this.progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            this.items.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        this.progress = tag.getInt("Progress");
    }

    /** 服务端数据变化后把完整数据同步给客户端（BER 渲染鸡依赖客户端槽位数据） */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        this.saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** 主动把更新包发给追踪本区块的玩家（1.21 原版不会自动发送 BE 数据包） */
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
