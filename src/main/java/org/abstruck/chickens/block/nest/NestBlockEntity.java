package org.abstruck.chickens.block.nest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import org.abstruck.chickens.breed.BreedLookups;
import org.abstruck.chickens.breed.ChickenBreed;
import org.abstruck.chickens.config.ChickenConfig;
import org.abstruck.chickens.entity.chicken.ChickenStats;
import org.abstruck.chickens.item.ItemData;
import org.abstruck.chickens.registry.ModRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 鸡窝方块实体：成年鸡槽 + 3×3 输出。
 * 产出节奏：品种数据的 {@code interval} 优先，缺省 600 tick（30 秒），受 Growth 加速。
 * 资源鸡按品种产出，原版鸡产原版鸡蛋。
 */
public class NestBlockEntity extends BlockEntity {
    public static final int CHICKEN_IN = 0;
    public static final int OUTPUT_START = 1;
    public static final int OUTPUT_COUNT = 9;
    public static final int PROGRESS_STEP = 100;

    /** 品种数据未写 interval 时的默认产出间隔（tick，600 = 30 秒） */
    public static final int DEFAULT_PRODUCTION_INTERVAL = 600;

    private final ItemStackHandler items = new ItemStackHandler(1 + OUTPUT_COUNT) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (slot >= OUTPUT_START) {
                return false;
            }
            return stack.is(ModRegistry.CHICKEN.get()) && ItemData.isMature(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            NestBlockEntity.this.setChanged();
            NestBlockEntity.this.syncToClients();
        }
    };

    /** 对外（漏斗/管道）视图：输入槽不可被抽取 */
    private final IItemHandler exposedItems = new ExtractOutputOnlyHandler(this.items, OUTPUT_START);
    private final LazyOptional<IItemHandler> exposedOptional = LazyOptional.of(() -> this.exposedItems);

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
        super(ModRegistry.NEST_BE.get(), pos, state);
    }

    /** 产出一次所需总进度（= 产出间隔 × PROGRESS_STEP）。按品种实时解析 */
    public int maxProgress() {
        return this.productionInterval() * PROGRESS_STEP;
    }

    /** 产出间隔（tick）：品种数据的 interval 优先；缺省（原版鸡/未写/解析失败）600；下限 20 tick */
    private int productionInterval() {
        int fallback = DEFAULT_PRODUCTION_INTERVAL;
        ItemStack chickenStack = this.items.getStackInSlot(CHICKEN_IN);
        if (chickenStack.isEmpty() || this.level == null) {
            return fallback;
        }
        ResourceLocation breedId = ItemData.getBreed(chickenStack);
        if (breedId == null) {
            return fallback; // 原版鸡
        }
        ChickenBreed breed = BreedLookups.breed(breedId);
        if (breed == null || breed.interval().isEmpty()) {
            return fallback;
        }
        return Math.max(20, breed.interval().get());
    }

    public static void tick(Level level, BlockPos pos, BlockState state, NestBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        if (be.items.getStackInSlot(CHICKEN_IN).isEmpty() || !be.hasAnyOutputSpace()) {
            be.progress = 0; // 无鸡或输出满：停产（等玩家取走产物）
            return;
        }
        // Growth 加速：进度 × (1 + growth × 系数)，与实体鸡同公式
        ChickenStats stats = ItemData.getStats(be.items.getStackInSlot(CHICKEN_IN));
        int growth = stats != null ? stats.growth() : 1;
        be.progress += (int) Math.round(PROGRESS_STEP
                * (1.0 + growth * ChickenConfig.GROWTH_INTERVAL_FACTOR.get()));
        if (be.progress >= be.maxProgress()) {
            be.progress = 0;
            be.produce(level);
        }
    }

    /** 输出是否有空间：任一空槽，或任一未满堆叠的槽 */
    private boolean hasAnyOutputSpace() {
        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT; i++) {
            ItemStack s = this.items.getStackInSlot(i);
            if (s.isEmpty() || s.getCount() < s.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    /** 产出：槽里 N 只鸡 → 至多 N 份产物，每只鸡独立随机（主/副产物、数量）；
     *  同物品合并堆叠，放不下的份跳过（输出满由 tick 停产保障） */
    private void produce(Level level) {
        level.playSound(null, this.worldPosition, SoundEvents.CHICKEN_EGG,
                SoundSource.BLOCKS, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
        ItemStack chickenStack = this.items.getStackInSlot(CHICKEN_IN);
        int chickens = chickenStack.getCount();
        for (int i = 0; i < chickens; i++) {
            ItemStack output = produceStack(level, chickenStack);
            if (output.isEmpty()) {
                continue;
            }
            insertOutput(output);
        }
        this.setChanged();
    }

    /** 插入一份产物：同物品合并、空槽；放不下返回 false */
    private boolean insertOutput(ItemStack output) {
        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT; i++) {
            ItemStack existing = this.items.getStackInSlot(i);
            if (ItemStack.isSameItemSameTags(existing, output)
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

    /** 按品种规则计算产物（同实体鸡：count 随机 + gain 加成 + strength 权重） */
    private static ItemStack produceStack(Level level, ItemStack chickenStack) {
        ResourceLocation breedId = ItemData.getBreed(chickenStack);
        if (breedId == null) {
            return new ItemStack(Items.EGG); // 原版鸡
        }
        ChickenBreed breed = BreedLookups.breed(breedId);
        if (breed == null) {
            return ItemStack.EMPTY;
        }
        ChickenStats stats = ItemData.getStats(chickenStack);
        int strength = stats != null ? stats.strength() : 1;
        ChickenBreed.Product product = BreedLookups.pickOutputItem(breed, strength, level.random);
        Item item = BreedLookups.resolveItem(product.item(), level.random);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        int base = Mth.nextInt(level.random, breed.count().min(), breed.count().max());
        int gain = stats != null ? stats.gain() : 1;
        int bonus = (int) Math.round(base * gain * breed.gainMultiplier());
        ItemStack stack = new ItemStack(item, Math.max(1, base + bonus));
        product.fluid().ifPresent(fluidRef -> {
            net.minecraft.world.level.material.Fluid fluid = BreedLookups.fluidOf(fluidRef, level.random);
            if (fluid != null) {
                ItemData.setFluid(stack,
                        net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(fluid));
            }
        });
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
