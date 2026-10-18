package org.abstruck.chickens.block.breedingbox;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.abstruck.chickens.block.ExtractOutputOnlyHandler;
import org.abstruck.chickens.config.ChickenConfig;
import org.abstruck.chickens.entity.chicken.ChickenGenetics;
import org.abstruck.chickens.entity.chicken.ChickenStats;
import org.abstruck.chickens.item.ItemData;
import org.abstruck.chickens.registry.ModRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 繁殖箱方块实体：两个成年鸡槽 + 种子槽 + 3×3 输出。
 * 两槽有鸡、有种子、输出有空位 → 爱心进度（30 秒）→ 产出幼年物品鸡（遗传规则），消耗 1 种子。
 */
public class BreedingBoxBlockEntity extends BlockEntity {
    public static final int CHICKEN_A = 0;
    public static final int CHICKEN_B = 1;
    public static final int SEEDS = 2;
    public static final int OUTPUT_START = 3;
    public static final int OUTPUT_COUNT = 9;
    public static final int PROGRESS_STEP = 100;

    private final ItemStackHandler items = new ItemStackHandler(3 + OUTPUT_COUNT) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (slot >= OUTPUT_START) {
                return false;
            }
            if (slot == SEEDS) {
                return stack.is(net.minecraftforge.common.Tags.Items.SEEDS); // 任意种子（c:seeds）
            }
            return stack.is(ModRegistry.CHICKEN.get()) && ItemData.isMature(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            BreedingBoxBlockEntity.this.setChanged();
            BreedingBoxBlockEntity.this.syncToClients();
        }
    };

    /** 对外（漏斗/管道）视图：输入槽不可被抽取 */
    private final IItemHandler exposedItems = new ExtractOutputOnlyHandler(this.items, OUTPUT_START);
    private final LazyOptional<IItemHandler> exposedOptional = LazyOptional.of(() -> this.exposedItems);

    private int progress;

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? BreedingBoxBlockEntity.this.progress : maxProgress();
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                BreedingBoxBlockEntity.this.progress = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public BreedingBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.BREEDING_BOX_BE.get(), pos, state);
    }

    public static int maxProgress() {
        return ChickenConfig.BREEDING_BOX_BREED_TIME.get() * PROGRESS_STEP;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BreedingBoxBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        boolean canBreed = !be.items.getStackInSlot(CHICKEN_A).isEmpty()
                && !be.items.getStackInSlot(CHICKEN_B).isEmpty()
                && !be.items.getStackInSlot(SEEDS).isEmpty()
                && be.hasOutputSpace();
        if (!canBreed) {
            be.progress = 0;
        } else {
            be.progress += PROGRESS_STEP;
            if (be.progress >= maxProgress()) {
                be.progress = 0;
                be.breed(level);
            }
        }
        // 外观变体：有鸡无种子 → 空窗帘；有鸡有种子 → 拉窗帘；无鸡 → 开放
        BlockState expected = state
                .setValue(BreedingBoxBlock.IS_BREEDING,
                        !be.items.getStackInSlot(CHICKEN_A).isEmpty()
                                && !be.items.getStackInSlot(CHICKEN_B).isEmpty())
                .setValue(BreedingBoxBlock.HAS_SEEDS, !be.items.getStackInSlot(SEEDS).isEmpty());
        if (expected != state) {
            level.setBlock(pos, expected, 3);
        }
    }

    private boolean hasOutputSpace() {
        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT; i++) {
            if (this.items.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void breed(Level level) {
        ItemStack a = this.items.getStackInSlot(CHICKEN_A);
        ItemStack b = this.items.getStackInSlot(CHICKEN_B);
        ResourceLocation breedA = ItemData.getBreed(a);
        ResourceLocation breedB = ItemData.getBreed(b);
        ItemStack baby = new ItemStack(ModRegistry.CHICKEN.get());
        ItemData.setMature(baby, false);
        if (breedA == null && breedB == null) {
            // 原版鸡对：原版幼鸡物品（无品种数据）
        } else if (breedA == null || breedB == null) {
            ResourceLocation breed = breedA != null ? breedA : breedB;
            ItemData.setBreed(baby, breed);
            ItemData.setStats(baby, ChickenStats.DEFAULT);
        } else {
            ChickenGenetics.OffspringData data = ChickenGenetics.create(
                    breedA, statsOf(a), breedB, statsOf(b), level.random);
            ItemData.setBreed(baby, data.breed());
            ItemData.setStats(baby, new ChickenStats(data.growth(), data.gain(), data.strength()));
        }
        this.items.getStackInSlot(SEEDS).shrink(1);
        this.insertOutput(baby);
        this.setChanged();
    }

    /** 输出插入：同品种同属性优先堆叠（上限 16），其次空槽；全满则掉地上 */
    private void insertOutput(ItemStack stack) {
        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT; i++) {
            ItemStack existing = this.items.getStackInSlot(i);
            if (ItemStack.isSameItemSameTags(existing, stack)
                    && existing.getCount() < existing.getMaxStackSize()) {
                int move = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(move);
                stack.shrink(move);
                if (stack.isEmpty()) {
                    return;
                }
            }
        }
        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT; i++) {
            if (this.items.getStackInSlot(i).isEmpty()) {
                this.items.setStackInSlot(i, stack);
                return;
            }
        }
        if (this.level != null) {
            Block.popResource(this.level, this.worldPosition, stack);
        }
    }

    private static ChickenStats statsOf(ItemStack stack) {
        ChickenStats stats = ItemData.getStats(stack);
        return stats != null ? stats : ChickenStats.DEFAULT;
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

    /** 数据变化（放鸡/消耗种子/产出）后同步完整数据给客户端（BER 渲染、漏斗交互都依赖） */
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

    /** 主动把更新包发给追踪本区块的玩家（1.19.2 原版不会因 setChanged 自动发送 BE 数据包） */
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
