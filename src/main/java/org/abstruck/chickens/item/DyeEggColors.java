package org.abstruck.chickens.item;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.abstruck.chickens.registry.ModRegistry;

/**
 * 16 种染料蛋的「平均色」配色（数值取自 dye平均色.txt）：
 * 写入物品 NBT 的 {@code Color}，并负责颜色 → 染料种类反查。
 * 旧版用原版染料纯色生成的染料蛋仍能被 {@link #dyeOf(int)} 识别。
 */
public final class DyeEggColors {
    /** 下标即 {@link DyeColor#getId()} */
    private static final int[] RGB = new int[DyeColor.values().length];

    static {
        put(DyeColor.WHITE, 0x9998AF);
        put(DyeColor.ORANGE, 0xCD7F24);
        put(DyeColor.MAGENTA, 0xAC56AB);
        put(DyeColor.LIGHT_BLUE, 0x5A85C2);
        put(DyeColor.YELLOW, 0xB89E24);
        put(DyeColor.LIME, 0x5C9C18);
        put(DyeColor.PINK, 0xC980A9);
        put(DyeColor.GRAY, 0x6F6F6F);
        put(DyeColor.LIGHT_GRAY, 0x9998A5);
        put(DyeColor.CYAN, 0x246A89);
        put(DyeColor.PURPLE, 0x8641B0);
        put(DyeColor.BLUE, 0x2B52A4);
        put(DyeColor.BROWN, 0x6C3B17);
        put(DyeColor.GREEN, 0x3E5A15);
        put(DyeColor.RED, 0xAA373F);
        put(DyeColor.BLACK, 0x1C1A29);
    }

    private static void put(DyeColor dye, int rgb) {
        RGB[dye.getId()] = rgb & 0xFFFFFF;
    }

    private DyeEggColors() {
    }

    /** 该染料的平均色（低 24 位，不含 alpha） */
    public static int rgb(DyeColor dye) {
        return RGB[dye.getId()];
    }

    /** 按颜色反查染料种类：先匹配平均色，再兼容旧版原版染料色；都无法识别返回 null */
    public static DyeColor dyeOf(int rgb) {
        int plain = rgb & 0xFFFFFF;
        for (DyeColor dye : DyeColor.values()) {
            if (RGB[dye.getId()] == plain) {
                return dye;
            }
        }
        for (DyeColor dye : DyeColor.values()) {
            if (DyeEggItem.rgbOf(dye) == plain) {
                return dye;
            }
        }
        return null;
    }

    /** 生成一个带颜色的染料蛋物品 */
    public static ItemStack stack(DyeColor dye) {
        ItemStack stack = new ItemStack(ModRegistry.DYE_EGG.get());
        ItemData.setColor(stack, rgb(dye));
        return stack;
    }
}
