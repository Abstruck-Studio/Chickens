package org.abstruck.chickens.entity;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.abstruck.chickens.entity.chicken.ResourceChicken;
import org.abstruck.chickens.item.DyeEggColors;
import org.abstruck.chickens.registry.ModDataComponents;
import org.abstruck.chickens.registry.ModEntities;
import org.abstruck.chickens.registry.ModItems;

/**
 * 染料蛋投掷实体：命中后按原版鸡蛋概率孵出对应颜色的染料鸡幼体
 * （1/8 出鸡，其中 1/32 出 4 只）；颜色读物品的 {@code minecraft:dyed_color} 组件。
 */
public class ThrownDyeEgg extends ThrowableItemProjectile {
    public ThrownDyeEgg(EntityType<? extends ThrownDyeEgg> type, Level level) {
        super(type, level);
    }

    public ThrownDyeEgg(Level level, LivingEntity shooter) {
        super(ModEntities.THROWN_DYE_EGG.get(), shooter, level);
    }

    public ThrownDyeEgg(Level level, double x, double y, double z) {
        super(ModEntities.THROWN_DYE_EGG.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.DYE_EGG.get();
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
                    ResourceChicken chicken = ModEntities.RESOURCE_CHICKEN.get().create(this.level());
                    if (chicken != null) {
                        chicken.setBreed(breedId);
                        chicken.setBaby(true);
                        chicken.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                        if (!chicken.fudgePositionAfterSizeChange(
                                net.minecraft.world.entity.EntityDimensions.fixed(0.0F, 0.0F))) {
                            break;
                        }
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

    /** 颜色 → 品种 id（16 种染料鸡，id = DyeColor 名），配色见 {@link DyeEggColors} */
    private static ResourceLocation breedFromColor(ItemStack stack) {
        DyedItemColor color = stack.get(net.minecraft.core.component.DataComponents.DYED_COLOR);
        net.minecraft.world.item.DyeColor dye =
                color != null ? DyeEggColors.dyeOf(color.rgb()) : null;
        if (dye == null) {
            return ResourceLocation.fromNamespaceAndPath("chickens", "white");
        }
        return ResourceLocation.fromNamespaceAndPath("chickens", dye.getName());
    }
}
