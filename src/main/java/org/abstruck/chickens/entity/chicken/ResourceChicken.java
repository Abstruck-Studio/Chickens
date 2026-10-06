package org.abstruck.chickens.entity.chicken;

import com.mojang.logging.LogUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
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
 * 资源鸡：唯一实体类型承载所有品种。品种 id 与三维属性存 SynchedEntityData（自动同步）；
 * 产出计时器仅服务端用普通字段。继承原版鸡，原版下蛋被禁用（eggTime 置最大），产出完全由本类接管。
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

    /** 掉落表：品种 JSON 写了 loot 用之，否则显式指向原版鸡表
     *  （1.21.1 未指定时懒生成 chickens:entities/resource_chicken——不存在 → 无掉落） */
    @Override
    protected net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> getDefaultLootTable() {
        ChickenBreed breed = this.getBreed(this.level().registryAccess());
        if (breed != null && breed.loot().isPresent()) {
            return net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.LOOT_TABLE, breed.loot().get());
        }
        return net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath("minecraft", "entities/chicken"));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BREED, DEFAULT_BREED.toString());
        builder.define(DATA_GROWTH, (byte) 1);
        builder.define(DATA_GAIN, (byte) 1);
        builder.define(DATA_STRENGTH, (byte) 1);
    }

    public ResourceLocation getBreedId() {
        // 兼容旧存档：读取时懒归一化（silver_dye→light_gray、log→oak_log），写回时自动落成新 id
        return BreedLookups.normalizeBreedId(ResourceLocation.parse(this.entityData.get(DATA_BREED)));
    }

    public void setBreed(ResourceLocation breedId) {
        this.entityData.set(DATA_BREED, breedId.toString());
    }

    /** 从注册表解析品种数据；品种不存在（被数据包移除）返回 null */
    public ChickenBreed getBreed(RegistryAccess access) {
        return BreedLookups.breedRegistry(access).get(this.getBreedId());
    }

    @Override
    public void setBaby(boolean baby) {
        this.setAge(baby ? -ChickenConfig.BABY_GROWTH_TIME.get() : 0);
    }

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

    @Override
    public void aiStep() {
        this.eggTime = Integer.MAX_VALUE; // 禁用原版下蛋（原版 aiStep 会自减 eggTime 并在归零时下蛋）
        super.aiStep();
        if (this.level().isClientSide()) {
            this.spawnBreedParticles();
            return;
        }
        if (!this.isAlive() || this.isBaby()) {
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

    /** 品种粒子特效（仅客户端）：位置/速度公式与原版末影人一致；类型/概率/数量来自品种 JSON 的 particles 字段；
     *  粒子 id 解析不到（如引用了未安装模组的粒子）时静默跳过 */
    private void spawnBreedParticles() {
        ChickenBreed breed = this.getBreed(this.level().registryAccess());
        if (breed == null || breed.particles().isEmpty()) {
            return;
        }
        ChickenBreed.ParticleConfig config = breed.particles().get();
        if (this.random.nextFloat() >= config.chance()) {
            return;
        }
        if (!(BuiltInRegistries.PARTICLE_TYPE.get(config.type()) instanceof ParticleOptions options)) {
            return;
        }
        for (int i = 0; i < config.count(); i++) {
            this.level().addParticle(options,
                    this.getRandomX(config.spread()), this.getRandomY() - 0.25, this.getRandomZ(config.spread()),
                    (this.random.nextDouble() - 0.5) * 2.0, -this.random.nextDouble(), (this.random.nextDouble() - 0.5) * 2.0);
        }
    }

    /** 距下次产出的剩余 tick（鸡分析器用） */
    public int getProductionTimer() {
        return this.productionTimer;
    }

    /** 产出间隔：原版下蛋同款随机（6000~11999 tick）÷ (1 + growth × 系数)，下限 1 秒；
     *  实体鸡不读品种 JSON 的 interval（interval 只影响鸡窝方块） */
    private int computeProductionInterval() {
        double base = this.random.nextInt(6000) + 6000;
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
        Item item = BreedLookups.resolveItem(access, product.item(), this.random);
        if (item == null) {
            return;
        }
        int base = Mth.nextInt(this.random, breed.count().min(), breed.count().max());
        int bonus = (int) Math.round(base * this.getGain() * breed.gainMultiplier());
        int count = Math.max(1, base + bonus);
        ItemStack stack = new ItemStack(item, count);
        product.fluid().ifPresent(fluidRef -> {
            net.minecraft.core.Holder<net.minecraft.world.level.material.Fluid> fluid =
                    BreedLookups.fluidHolderOf(access, fluidRef, this.random);
            if (fluid != null) {
                stack.set(ModDataComponents.FLUID.get(), fluid);
            }
        });
        this.spawnAtLocation(stack);
        this.playSound(SoundEvents.CHICKEN_EGG, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /** 原版 Animal 在非草地返回路径光照成本——下界黑暗处为负值，导致鸡永远无法通过生成检查；
     *  下界/末地强制返回 0（允许黑暗中生成），主世界保持原版动物行为 */
    @Override
    public float getWalkTargetValue(net.minecraft.core.BlockPos pos, net.minecraft.world.level.LevelReader level) {
        if (level instanceof ServerLevel serverLevel
                && (serverLevel.dimension() == net.minecraft.world.level.Level.NETHER
                || serverLevel.dimension() == net.minecraft.world.level.Level.END)) {
            return 0.0F;
        }
        return super.getWalkTargetValue(pos, level);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public Component getDisplayName() {
        if (this.hasCustomName()) {
            return this.getCustomName();
        }
        return Component.translatable("breed.chickens." + this.getBreedId().getPath());
    }

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
            // 与原版鸡等其他父母：品种取自身，属性默认
            child.setBreed(this.getBreedId());
        }
        return child;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

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
        // 缺失键读取 0，被 setter 钳制回 1，旧存档安全
        this.setGrowth(tag.getByte("Growth"));
        this.setGain(tag.getByte("Gain"));
        this.setStrength(tag.getByte("Strength"));
        this.productionTimer = tag.getInt("ProductionTimer");
    }
}
