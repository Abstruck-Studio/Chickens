package org.abstruck.chickens.entity.chicken;

import com.mojang.logging.LogUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.abstruck.chickens.registry.ModEntities;
import org.abstruck.chickens.breed.BreedLookups;
import org.abstruck.chickens.breed.ChickenBreed;
import org.abstruck.chickens.config.ChickenConfig;
import org.abstruck.chickens.registry.ModDataComponents;
import org.slf4j.Logger;

/**
 * 资源鸡：唯一实体类型承载所有品种（延续 1.12「更多鸡」的单实体+数据模型）。
 * 品种 id 与三维属性（growth/gain/strength）存 SynchedEntityData，自动同步客户端；
 * 产出计时器只在服务端有意义，用普通字段（不占用同步通道）。
 * <p>
 * 注意：继承原版鸡（{@code net.minecraft.world.entity.animal.Chicken}），
 * 原版的下蛋逻辑被禁用（eggTime 置最大），产出完全由本类接管。
 */
public class ResourceChicken extends net.minecraft.world.entity.animal.Chicken {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation DEFAULT_BREED = ResourceLocation.fromNamespaceAndPath("chickens", "flint");

    private static final EntityDataAccessor<String> DATA_BREED = SynchedEntityData.defineId(ResourceChicken.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Byte> DATA_GROWTH = SynchedEntityData.defineId(ResourceChicken.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_GAIN = SynchedEntityData.defineId(ResourceChicken.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_STRENGTH = SynchedEntityData.defineId(ResourceChicken.class, EntityDataSerializers.BYTE);

    /** 距下次产出的剩余 tick；-1 = 尚未初始化（首次 tick 时按品种计算）。仅服务端使用。 */
    private int productionTimer = -1;

    public ResourceChicken(EntityType<? extends ResourceChicken> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BREED, DEFAULT_BREED.toString());
        builder.define(DATA_GROWTH, (byte) 1);
        builder.define(DATA_GAIN, (byte) 1);
        builder.define(DATA_STRENGTH, (byte) 1);
    }

    // ---------- 品种 ----------

    public ResourceLocation getBreedId() {
        return ResourceLocation.parse(this.entityData.get(DATA_BREED));
    }

    public void setBreed(ResourceLocation breedId) {
        this.entityData.set(DATA_BREED, breedId.toString());
    }

    /** 从注册表解析品种数据；品种不存在（被数据包移除）返回 null。 */
    public ChickenBreed getBreed(RegistryAccess access) {
        return BreedLookups.breedRegistry(access).get(this.getBreedId());
    }

    // ---------- 幼年成长（原版固定 20 分钟，改为可配置） ----------

    @Override
    public void setBaby(boolean baby) {
        this.setAge(baby ? -ChickenConfig.BABY_GROWTH_TIME.get() : 0);
    }

    // ---------- 三维属性（1~10） ----------

    public byte getGrowth() {
        return this.entityData.get(DATA_GROWTH);
    }

    public void setGrowth(byte value) {
        this.entityData.set(DATA_GROWTH, (byte) Math.clamp(value, 1, 10));
    }

    public byte getGain() {
        return this.entityData.get(DATA_GAIN);
    }

    public void setGain(byte value) {
        this.entityData.set(DATA_GAIN, (byte) Math.clamp(value, 1, 10));
    }

    public byte getStrength() {
        return this.entityData.get(DATA_STRENGTH);
    }

    public void setStrength(byte value) {
        this.entityData.set(DATA_STRENGTH, (byte) Math.clamp(value, 1, 10));
    }

    // ---------- 产出 ----------

    @Override
    public void aiStep() {
        this.eggTime = Integer.MAX_VALUE; // 禁用原版下蛋（原版 aiStep 会自减 eggTime 并在归零时下蛋）
        super.aiStep();
        if (this.level().isClientSide() || !this.isAlive() || this.isBaby()) {
            return;
        }
        if (this.productionTimer < 0) {
            this.productionTimer = this.computeProductionInterval();
        }
        if (--this.productionTimer <= 0) {
            this.produce();
            this.productionTimer = this.computeProductionInterval();
        }
    }

    /** 测试/调试用：直接设定产出倒计时（tick）。 */
    public void setProductionTimer(int ticks) {
        this.productionTimer = ticks;
    }

    /** 距下次产出的剩余 tick（鸡分析器用） */
    public int getProductionTimer() {
        return this.productionTimer;
    }

    /** 基础间隔 × 全局乘数 ÷ (1 + growth × 系数)，下限 1 秒。 */
    private int computeProductionInterval() {
        ChickenBreed breed = this.getBreed(this.level().registryAccess());
        double base = breed != null ? breed.interval() : 6000;
        double growthFactor = 1.0 + this.getGrowth() * ChickenConfig.GROWTH_INTERVAL_FACTOR.get();
        return Math.max(20, (int) Math.round(base * ChickenConfig.PRODUCTION_INTERVAL_MULTIPLIER.get() / growthFactor));
    }

    private void produce() {
        RegistryAccess access = this.level().registryAccess();
        ChickenBreed breed = this.getBreed(access);
        if (breed == null) {
            LOGGER.warn("[chickens] 产出失败：无法解析品种 {}（注册表为空？）", this.getBreedId());
            return;
        }
        ChickenBreed.Product product = BreedLookups.pickOutputItem(breed, this.getStrength(), this.random);
        Item item = BreedLookups.resolveItem(access, product.item());
        if (item == null) {
            return;
        }
        int base = Mth.nextInt(this.random, breed.count().min(), breed.count().max());
        int bonus = (int) Math.round(base * this.getGain() * breed.gainMultiplier());
        int count = Math.max(1, base + bonus);
        ItemStack stack = new ItemStack(item, count);
        product.fluid().ifPresent(fluidId -> stack.set(ModDataComponents.FLUID.get(), fluidId));
        this.spawnAtLocation(stack);
        this.playSound(SoundEvents.CHICKEN_EGG, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
    }

    // ---------- 防消失 ----------

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /**
     * 生成位置价值（PathfinderMob.checkSpawnRules 用「>= 0」判断能否生成）。
     * 原版 Animal 在非草地返回路径光照成本——下界黑暗处为负值，导致鸡永远无法通过生成检查。
     * 下界强制返回 0（允许在黑暗中生成），主世界保持原版动物行为。
     */
    @Override
    public float getWalkTargetValue(net.minecraft.core.BlockPos pos, net.minecraft.world.level.LevelReader level) {
        if (level instanceof ServerLevel serverLevel && serverLevel.dimension() == net.minecraft.world.level.Level.NETHER) {
            return 0.0F;
        }
        return super.getWalkTargetValue(pos, level);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    // ---------- 显示名 ----------

    @Override
    public Component getDisplayName() {
        if (this.hasCustomName()) {
            return this.getCustomName();
        }
        return Component.translatable("breed.chickens." + this.getBreedId().getPath());
    }

    // ---------- 繁殖遗传（阶段 4：同品种升级、异品种 mutation、杂交重置属性） ----------

    @Override
    public ResourceChicken getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        ResourceChicken child = ModEntities.RESOURCE_CHICKEN.get().create(level);
        if (child == null) {
            return null;
        }
        if (otherParent instanceof ResourceChicken other) {
            ChickenGenetics.OffspringData data =
                    ChickenGenetics.create(this, other, level.registryAccess(), level.random);
            child.setBreed(data.breed());
            child.setGrowth(data.growth());
            child.setGain(data.gain());
            child.setStrength(data.strength());
        } else {
            // 与（原版鸡等）其他父母：品种取自身，属性默认
            child.setBreed(this.getBreedId());
        }
        return child;
    }

    // ---------- 摔落免疫（模组规格：资源鸡摔不死） ----------

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    // ---------- 存档 ----------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Breed", this.getBreedId().toString());
        tag.putByte("Growth", this.getGrowth());
        tag.putByte("Gain", this.getGain());
        tag.putByte("Strength", this.getStrength());
        tag.putInt("ProductionTimer", this.productionTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Breed")) {
            this.setBreed(ResourceLocation.parse(tag.getString("Breed")));
        }
        // getByte 对缺失键返回 0，setGrowth 会钳制回 1，旧存档安全
        this.setGrowth(tag.getByte("Growth"));
        this.setGain(tag.getByte("Gain"));
        this.setStrength(tag.getByte("Strength"));
        this.productionTimer = tag.getInt("ProductionTimer");
    }
}
