package org.abstruck.chickens.block.nest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.abstruck.chickens.block.ExtractOutputOnlyHandler;
import org.abstruck.chickens.breed.BreedLookups;
import org.abstruck.chickens.breed.ChickenBreed;
import org.abstruck.chickens.config.ChickenConfig;
import org.abstruck.chickens.entity.chicken.ChickenStats;
import org.abstruck.chickens.registry.ModBlockEntities;
import org.abstruck.chickens.registry.ModDataComponents;
import org.abstruck.chickens.registry.ModItems;

/**
 * 鸡窝方块实体：成年鸡槽 + 3×3 输出。
 * 每 30 秒（配置）产出一次作物：资源鸡按品种产出（含 Gain/Strength 规则），原版鸡产原版鸡蛋。
 */
public class NestBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    public static final int CHICKEN_IN = 0;
    public static final int OUTPUT_START = 1;
    public static final int OUTPUT_COUNT = 9;
    public static final int PROGRESS_STEP = 100;

    private final ItemStackHandler items = new ItemStackHandler(1 + OUTPUT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot >= OUTPUT_START) {
                return false;
            }
            Boolean mature = stack.get(ModDataComponents.MATURE.get());
            return stack.is(ModItems.CHICKEN.get()) && (mature == null || mature);
        }

        @Override
        protected void onContentsChanged(int slot) {
            NestBlockEntity.this.setChanged();
            NestBlockEntity.this.syncToClients();
        }
    };

    /** 对外（漏斗/管道）视图：输入槽不可被抽取 */
    private final IItemHandler exposedItems = new ExtractOutputOnlyHandler(this.items, OUTPUT_START);

    private int progress;

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? NestBlockEntity.this.progress : maxProgress();
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                NestBlockEntity.this.progress = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public NestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NEST.get(), pos, state);
    }

    public static int maxProgress() {
        return ChickenConfig.NEST_PRODUCTION_TIME.get() * PROGRESS_STEP;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, NestBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        if (be.items.getStackInSlot(CHICKEN_IN).isEmpty() || !be.hasAnyOutputSpace()) {
            be.progress = 0; // 无鸡或输出满：停产（进度停摆，等玩家取走产物）
            return;
        }
        // 生长属性加速：进度 × (1 + growth × 系数)，与实体鸡同公式
        ChickenStats stats = be.items.getStackInSlot(CHICKEN_IN).get(ModDataComponents.STATS.get());
        int growth = stats != null ? stats.growth() : 1;
        be.progress += (int) Math.round(PROGRESS_STEP
                * (1.0 + growth * ChickenConfig.GROWTH_INTERVAL_FACTOR.get()));
        if (be.progress >= maxProgress()) {
            be.progress = 0;
            be.produce(level);
        }
    }

    /** 输出是否还有空间（任一空槽，或任一未满堆叠的槽——同物品可合并） */
    private boolean hasAnyOutputSpace() {
        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT; i++) {
            ItemStack s = this.items.getStackInSlot(i);
            if (s.isEmpty() || s.getCount() < s.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    /** 产出：槽里 N 只鸡 → 至多 N 份产物。每只鸡独立随机（主/副产物、数量），
     *  同物品合并堆叠；抽到槽里没有且无空位的物品时跳过该份（其余份照常产出，不掉落）。
     *  输出完全放不下（无空槽且无可合并空间）时由 tick 停产。 */
    private void produce(Level level) {
        // 下蛋音效（原版鸡下蛋同款）
        level.playSound(null, this.worldPosition, net.minecraft.sounds.SoundEvents.CHICKEN_EGG,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F,
                0.9F + level.random.nextFloat() * 0.2F);
        ItemStack chickenStack = this.items.getStackInSlot(CHICKEN_IN);
        int chickens = chickenStack.getCount();
        for (int i = 0; i < chickens; i++) {
            ItemStack output = produceStack(level, chickenStack);
            if (output.isEmpty()) {
                continue;
            }
            insertOutput(output); // 放不下（无同物品堆叠且无空槽）→ 跳过该份
        }
        this.setChanged();
    }

    /** 插入一份产物：同物品合并、空槽；放不下返回 false */
    private boolean insertOutput(ItemStack output) {
        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT; i++) {
            ItemStack existing = this.items.getStackInSlot(i);
            if (ItemStack.isSameItemSameComponents(existing, output)
                    && existing.getCount() < existing.getMaxStackSize()) {
                int move = Math.min(output.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(move);
                output.shrink(move);
                if (output.isEmpty()) {
                    return true;
                }
            }
        }
        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT; i++) {
            if (this.items.getStackInSlot(i).isEmpty()) {
                this.items.setStackInSlot(i, output);
                return true;
            }
        }
        return false;
    }

    /** 按品种规则计算产出（与实体产出同规则：count 随机 + gain 加成 + strength 替代产出） */
    private static ItemStack produceStack(Level level, ItemStack chickenStack) {
        var access = level.registryAccess();
        ResourceLocation breedId = chickenStack.get(ModDataComponents.BREED.get());
        if (breedId == null) {
            return new ItemStack(Items.EGG); // 原版鸡
        }
        ChickenBreed breed = BreedLookups.breedRegistry(access).get(breedId);
        if (breed == null) {
            return ItemStack.EMPTY;
        }
        ChickenStats stats = chickenStack.get(ModDataComponents.STATS.get());
        int strength = stats != null ? stats.strength() : 1;
        ChickenBreed.Product product = BreedLookups.pickOutputItem(breed, strength, level.random);
        Item item = BreedLookups.resolveItem(access, product.item());
        if (item == null) {
            return ItemStack.EMPTY;
        }
        int base = Mth.nextInt(level.random, breed.count().min(), breed.count().max());
        int gain = stats != null ? stats.gain() : 1;
        int bonus = (int) Math.round(base * gain * breed.gainMultiplier());
        ItemStack stack = new ItemStack(item, Math.max(1, base + bonus));
        product.fluid().ifPresent(fluidId -> stack.set(ModDataComponents.FLUID.get(), fluidId));
        return stack;
    }

    public ItemStackHandler getItems() {
        return this.items;
    }

    /** 给漏斗/管道用的物品能力视图（输出槽可抽，鸡槽不可抽） */
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
