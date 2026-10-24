package org.abstruck.chickens.entity;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.abstruck.chickens.entity.chicken.ResourceChicken;
import org.abstruck.chickens.item.DyeEggItem;
import org.abstruck.chickens.item.ItemData;
import org.abstruck.chickens.registry.ModRegistry;

/**
 * 染料蛋投掷实体：命中后按原版鸡蛋概率孵出对应颜色的染料鸡幼体
 * （1/8 出鸡，其中 1/32 出 4 只）；颜色读物品 NBT 的 {@code Color}。
 */
public class ThrownDyeEgg extends ThrowableItemProjectile {
    public ThrownDyeEgg(EntityType<? extends ThrownDyeEgg> type, Level level) {
        super(type, level);
    }

    public ThrownDyeEgg(Level level, LivingEntity shooter) {
        super(ModRegistry.THROWN_DYE_EGG.get(), shooter, level);
    }

    public ThrownDyeEgg(Level level, double x, double y, double z) {
        super(ModRegistry.THROWN_DYE_EGG.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModRegistry.DYE_EGG.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            if (this.random.nextInt(8) == 0) {
                int count = 1;
                if (this.random.nextInt(32) == 0) {
                    count = 4;
                }
                ResourceLocation breedId = breedFromColor(this.getItem());
                for (int i = 0; i < count; i++) {
                    ResourceChicken chicken = ModRegistry.RESOURCE_CHICKEN.get().create(this.level());
                    if (chicken != null) {
                        chicken.setBreed(breedId);
                        chicken.setBaby(true);
                        chicken.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                        this.level().addFreshEntity(chicken);
                    }
                }
            }
            this.discard();
        }
        for (int i = 0; i < 8; i++) {
            this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, this.getItem()),
                    this.getX(), this.getY(), this.getZ(),
                    (this.random.nextDouble() - 0.5) * 0.08, (this.random.nextDouble() - 0.5) * 0.08,
                    (this.random.nextDouble() - 0.5) * 0.08);
        }
    }

    /** 颜色 → 品种 id（16 种染料鸡，id = DyeColor 名） */
    private static ResourceLocation breedFromColor(ItemStack stack) {
        int color = ItemData.getColor(stack);
        if (color < 0) {
            return new ResourceLocation("chickens", "white");
        }
        net.minecraft.world.item.DyeColor dye = DyeEggItem.dyeColorOf(color);
        return new ResourceLocation("chickens", dye != null ? dye.getName() : "white");
    }
}
