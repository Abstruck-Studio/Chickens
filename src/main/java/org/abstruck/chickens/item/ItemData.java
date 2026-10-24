package org.abstruck.chickens.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.abstruck.chickens.breed.BreedLookups;
import org.abstruck.chickens.entity.chicken.ChickenStats;

/**
 * 物品自定义数据的 NBT 读写。
 *
 * <p>1.21.1 用 {@code DataComponentType}；1.19.2 没有组件系统，等价信息放在物品 NBT 里
 * （同样随物品栏、JEI 子类型、漏斗传输自动同步）。</p>
 */
public final class ItemData {
    /** 品种 id（字符串） */
    public static final String BREED = "Breed";
    /** 三维属性（子标签 growth / gain / strength） */
    public static final String STATS = "Stats";
    /** 成熟标记（true = 成年；缺省按成年处理） */
    public static final String MATURE = "Mature";
    /** 流体蛋的流体 id（字符串） */
    public static final String FLUID = "Fluid";
    /** 染料蛋的颜色（RGB 整数） */
    public static final String COLOR = "Color";

    private ItemData() {
    }

    public static ResourceLocation getBreed(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(BREED)) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(BREED));
        return BreedLookups.normalizeBreedId(id);
    }

    public static void setBreed(ItemStack stack, ResourceLocation breedId) {
        if (breedId == null) {
            remove(stack, BREED);
        } else {
            stack.getOrCreateTag().putString(BREED, breedId.toString());
        }
    }

    public static ChickenStats getStats(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(STATS)) {
            return null;
        }
        CompoundTag stats = tag.getCompound(STATS);
        return new ChickenStats((byte) stats.getInt("growth"), (byte) stats.getInt("gain"),
                (byte) stats.getInt("strength"));
    }

    public static void setStats(ItemStack stack, ChickenStats stats) {
        if (stats == null) {
            remove(stack, STATS);
            return;
        }
        CompoundTag statsTag = new CompoundTag();
        statsTag.putInt("growth", stats.growth());
        statsTag.putInt("gain", stats.gain());
        statsTag.putInt("strength", stats.strength());
        stack.getOrCreateTag().put(STATS, statsTag);
    }

    /** 成熟标记：缺省按成年处理（无该标签 = true） */
    public static boolean isMature(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null || !tag.contains(MATURE) || tag.getBoolean(MATURE);
    }

    public static void setMature(ItemStack stack, boolean mature) {
        stack.getOrCreateTag().putBoolean(MATURE, mature);
    }

    public static Fluid getFluid(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(FLUID)) {
            return null;
        }
        return BreedLookups.fluidOf(ResourceLocation.tryParse(tag.getString(FLUID)));
    }

    public static ResourceLocation getFluidId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(FLUID)) {
            return null;
        }
        return ResourceLocation.tryParse(tag.getString(FLUID));
    }

    public static void setFluid(ItemStack stack, ResourceLocation fluidId) {
        if (fluidId == null) {
            remove(stack, FLUID);
        } else {
            stack.getOrCreateTag().putString(FLUID, fluidId.toString());
        }
    }

    /** 染料蛋颜色（0xRRGGBB）；缺省返回 -1 */
    public static int getColor(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(COLOR)) {
            return -1;
        }
        return tag.getInt(COLOR) & 0xFFFFFF;
    }

    public static void setColor(ItemStack stack, int rgb) {
        stack.getOrCreateTag().putInt(COLOR, rgb & 0xFFFFFF);
    }

    private static void remove(ItemStack stack, String key) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(key);
            if (tag.isEmpty()) {
                stack.setTag(null);
            }
        }
    }
}
