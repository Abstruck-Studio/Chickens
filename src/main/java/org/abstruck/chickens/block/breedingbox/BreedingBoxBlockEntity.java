package org.abstruck.chickens.block.breedingbox;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.abstruck.chickens.block.ExtractOutputOnlyHandler;
import org.abstruck.chickens.config.ChickenConfig;
import org.abstruck.chickens.entity.chicken.ChickenGenetics;
import org.abstruck.chickens.entity.chicken.ChickenStats;
import org.abstruck.chickens.registry.ModBlockEntities;
import org.abstruck.chickens.registry.ModDataComponents;
import org.abstruck.chickens.registry.ModItems;

/**
 * 繁殖箱方块实体：两个成年鸡槽 + 种子槽 + 3×3 输出。
 * 两槽有鸡、有种子、输出有空位 → 爱心进度（30 秒）→ 直接产出**幼年物品鸡**（遗传规则），消耗 1 种子。
 */
public class BreedingBoxBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    public static final int CHICKEN_A = 0;
    public static final int CHICKEN_B = 1;
    public static final int SEEDS = 2;
    public static final int OUTPUT_START = 3;
    public static final int OUTPUT_COUNT = 9;
    public static final int PROGRESS_STEP = 100;

    private final ItemStackHandler items = new ItemStackHandler(3 + OUTPUT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot >= OUTPUT_START) {
                return false;
            }
            if (slot == SEEDS) {
                return stack.is(net.neoforged.neoforge.common.Tags.Items.SEEDS); // 任意种子（c:seeds）
            }
            Boolean mature = stack.get(ModDataComponents.MATURE.get());
            return stack.is(ModItems.CHICKEN.get()) && (mature == null || mature);
        }

        @Override
        protected void onContentsChanged(int slot) {
            BreedingBoxBlockEntity.this.setChanged();
            BreedingBoxBlockEntity.this.syncToClients();
        }
    };

    /** 对外（漏斗/管道）视图：输入槽不可被抽取 */
    private final IItemHandler exposedItems = new ExtractOutputOnlyHandler(this.items, OUTPUT_START);

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
        super(ModBlockEntities.BREEDING_BOX.get(), pos, state);
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
            be.progress = 0; // 条件不满足：进度重置
        } else {
            be.progress += PROGRESS_STEP;
            if (be.progress >= maxProgress()) {
                be.progress = 0;
                be.breed(level);
            }
        }
        // 外观变体同步（1.12 roost 同款语义）：is_breeding = 两槽都有鸡；
        // 有鸡无种子 → 空窗帘（感叹号）；有鸡有种子 → 拉窗帘；无鸡 → 开放
        BlockState expected = state
                .setValue(BreedingBoxBlock.IS_BREEDING,
                        !be.items.getStackInSlot(CHICKEN_A).isEmpty() && !be.items.getStackInSlot(CHICKEN_B).isEmpty())
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
        ResourceLocation breedA = a.get(ModDataComponents.BREED.get());
        ResourceLocation breedB = b.get(ModDataComponents.BREED.get());
        ItemStack baby = new ItemStack(ModItems.CHICKEN.get());
        baby.set(ModDataComponents.MATURE.get(), false); // 幼年
        if (breedA == null && breedB == null) {
            // 原版鸡对：原版幼鸡物品（无组件）
        } else if (breedA == null || breedB == null) {
            ResourceLocation breed = breedA != null ? breedA : breedB;
            baby.set(ModDataComponents.BREED.get(), breed);
            baby.set(ModDataComponents.STATS.get(), ChickenStats.DEFAULT);
        } else {
            ChickenGenetics.OffspringData data = ChickenGenetics.create(
                    breedA, statsOf(a), breedB, statsOf(b), level.registryAccess(), level.random);
            baby.set(ModDataComponents.BREED.get(), data.breed());
            baby.set(ModDataComponents.STATS.get(),
                    new ChickenStats(data.growth(), data.gain(), data.strength()));
        }
        this.items.getStackInSlot(SEEDS).shrink(1);
        this.insertOutput(baby);
        this.setChanged();
    }

    /** 输出插入：同品种同组件的鸡优先堆叠（上限 16），其次空槽；全满则掉地上 */
    private void insertOutput(ItemStack stack) {
        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT; i++) {
            ItemStack existing = this.items.getStackInSlot(i);
            if (ItemStack.isSameItemSameComponents(existing, stack)
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
        // 输出满：掉在地上
        if (this.level != null) {
            Block.popResource(this.level, this.worldPosition, stack);
        }
    }

    private static ChickenStats statsOf(ItemStack stack) {
        ChickenStats stats = stack.get(ModDataComponents.STATS.get());
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

    /** 服务端数据变化（放鸡/消耗种子/产出）后把完整数据同步给客户端，
     *  否则客户端方块实体看不到槽位内容（BER 渲染、漏斗交互都依赖它） */
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
