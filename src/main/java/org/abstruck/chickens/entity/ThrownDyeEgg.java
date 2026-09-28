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
import org.abstruck.chickens.registry.ModDataComponents;
import org.abstruck.chickens.registry.ModEntities;
import org.abstruck.chickens.registry.ModItems;

/**
 * 染料蛋投掷实体：命中后 100% 孵出与颜色对应的染料鸡品种（资源鸡实体，属性 1/1/1）。
 * 颜色从物品的 {@code minecraft:dyed_color} 组件读取（物品随投掷实体同步）。
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
            // 与原版鸡蛋相同的概率：1/8 出鸡；其中 1/32 出 4 只。全部为幼年鸡
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
                        chicken.setBaby(true); // 幼年（成长时间走配置 babyGrowthTime）
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
        // 砸中粒子（原版鸡蛋同款）
        for (int i = 0; i < 8; i++) {
            this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, this.getItem()),
                    this.getX(), this.getY(), this.getZ(),
                    (this.random.nextDouble() - 0.5) * 0.08, (this.random.nextDouble() - 0.5) * 0.08,
                    (this.random.nextDouble() - 0.5) * 0.08);
        }
    }

    /** 颜色 → 品种 id（1.12 的 16 种染料鸡；浅灰鸡品种 id 沿袭 1.12 的 silver_dye）。
     *  匹配只比较低 24 位（getTextureDiffuseColor 返回带 alpha 位的 int） */
    private static ResourceLocation breedFromColor(ItemStack stack) {
        DyedItemColor color = stack.get(net.minecraft.core.component.DataComponents.DYED_COLOR);
        if (color == null) {
            return ResourceLocation.fromNamespaceAndPath("chickens", "white");
        }
        int plain = color.rgb() & 0xFFFFFF;
        for (net.minecraft.world.item.DyeColor dye : net.minecraft.world.item.DyeColor.values()) {
            if ((dye.getTextureDiffuseColor() & 0xFFFFFF) == plain) {
                String id = dye == net.minecraft.world.item.DyeColor.LIGHT_GRAY ? "silver_dye" : dye.getName();
                return ResourceLocation.fromNamespaceAndPath("chickens", id);
            }
        }
        return ResourceLocation.fromNamespaceAndPath("chickens", "white");
    }
}
