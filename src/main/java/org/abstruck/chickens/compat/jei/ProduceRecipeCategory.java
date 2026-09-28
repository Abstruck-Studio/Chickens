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
import org.abstruck.chickens.Chickens;

import java.util.List;

/**
 * 产出配方分类：鸡 → 全部产物（第一个是主产物，其余副产物），
 * 每个产物下方按权重标注概率（只标注力量 1 的效果：主产物权重 = weight × 1）。
 * 布局在 JEI 标准页宽（162px）内居中。
 */
public class ProduceRecipeCategory extends AbstractRecipeCategory<ProduceRecipe> {
    public static final RecipeType<ProduceRecipe> TYPE =
            RecipeType.create(Chickens.MODID, "produce", ProduceRecipe.class);

    private static final int MAX_OUTPUTS = 5;
    /** 内容宽 158（输入 0 .. 最后一个输出 140+18），页宽 162 → 居中偏移 2 */
    private static final int X = 2;

    public ProduceRecipeCategory(mezz.jei.api.gui.drawable.IDrawable icon) {
        super(TYPE, Component.translatable("jei.chickens.produce"), icon, 162, 60);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ProduceRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(X, 20).addIngredients(VanillaTypes.ITEM_STACK, List.of(recipe.chicken()))
                .setStandardSlotBackground();
        // 输出槽间距 24（避免视觉堆叠）
        List<ProduceRecipe.Output> outputs = recipe.outputs().stream().limit(MAX_OUTPUTS).toList();
        for (int i = 0; i < outputs.size(); i++) {
            ProduceRecipe.Output output = outputs.get(i);
            builder.addOutputSlot(X + 36 + i * 26, 20)
                    .addIngredients(VanillaTypes.ITEM_STACK, List.of(output.stack()))
                    .setOutputSlotBackground();
        }
    }

    @Override
    public void draw(ProduceRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        guiGraphics.drawString(font, "→", X + 20, 24, 0xFF555555, false);
        // 每个产物下方标注概率（力量 1：主产物权重 = weight × 1，与副产物权重求和后按比例）
        List<ProduceRecipe.Output> outputs = recipe.outputs().stream().limit(MAX_OUTPUTS).toList();
        int total = outputs.stream().mapToInt(ProduceRecipe.Output::weight).sum();
        if (total <= 0) {
            return;
        }
        for (int i = 0; i < outputs.size(); i++) {
            ProduceRecipe.Output output = outputs.get(i);
            int pct = output.weight() * 100 / total;
            guiGraphics.drawString(font, pct + "%", X + 36 + i * 26, 42, 0xFF3F3F3F, false);
        }
    }
}
