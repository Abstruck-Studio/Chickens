package org.abstruck.chickens.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.breed.MutationRule;

import java.util.List;

/**
 * 杂交配方分类：鸡A + 鸡B → 全部结果（主结果 + 回退），概率标在每个结果下方。
 * 布局在 JEI 标准页宽（162px）内居中。
 */
public class MutationRecipeCategory extends AbstractRecipeCategory<MutationRecipe> {
    public static final RecipeType<MutationRecipe> TYPE =
            RecipeType.create(Chickens.MODID, "mutation", MutationRecipe.class);

    /** 内容宽 130（A 0 .. 回退输出 126），页宽 162 → 居中偏移 16 */
    private static final int X = 16;

    public MutationRecipeCategory(mezz.jei.api.gui.drawable.IDrawable icon) {
        super(TYPE, Component.translatable("jei.chickens.mutation"), icon, 162, 60);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MutationRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(X, 20).addIngredients(VanillaTypes.ITEM_STACK, List.of(recipe.parentA()))
                .setStandardSlotBackground();
        builder.addInputSlot(X + 28, 20).addIngredients(VanillaTypes.ITEM_STACK, List.of(recipe.parentB()))
                .setStandardSlotBackground();
        // 全部结果各自一个输出槽（主结果 + 回退父母），概率画在槽下方；槽间距 24（避免视觉堆叠）
        List<ItemStack> stacks = recipe.resultStacks();
        for (int i = 0; i < stacks.size(); i++) {
            builder.addOutputSlot(X + 72 + i * 26, 20)
                    .addIngredients(VanillaTypes.ITEM_STACK, List.of(stacks.get(i)))
                    .setOutputSlotBackground();
        }
    }

    @Override
    public void draw(MutationRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        // 两输入槽中心：A 中心 25、B 中心 53 → 中点 39；「+」宽约 6px → x = 36
        guiGraphics.drawString(font, "+", X + 20, 24, 0xFF808080, false);
        guiGraphics.drawString(font, "→", X + 54, 24, 0xFF555555, false);
        // 每个结果下方标注概率
        double total = ChickensJeiPlugin.totalWeight(recipe.allResults());
        List<MutationRule.WeightedResult> results = recipe.allResults();
        for (int i = 0; i < results.size(); i++) {
            MutationRule.WeightedResult result = results.get(i);
            String pct = String.format("%.0f%%", result.weight() / total * 100);
            guiGraphics.drawString(font, pct, X + 72 + i * 26, 42, 0xFF3F3F3F, false);
        }
    }
}
