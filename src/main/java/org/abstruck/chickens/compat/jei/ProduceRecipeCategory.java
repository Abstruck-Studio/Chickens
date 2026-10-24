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

/** 产出配方分类：鸡 → 全部产物（第一个主产物，其余副产物），下方按权重标注概率（对应力量 1 的效果） */
public class ProduceRecipeCategory extends AbstractRecipeCategory<ProduceRecipe> {
    public static final RecipeType<ProduceRecipe> TYPE =
            RecipeType.create(Chickens.MODID, "produce", ProduceRecipe.class);

    private static final int MAX_OUTPUTS = 5;
    /** 内容宽 158，页宽 162 → 居中偏移 2 */
    private static final int X = 2;

    public ProduceRecipeCategory(mezz.jei.api.gui.drawable.IDrawable icon) {
        super(TYPE, Component.translatable("jei.chickens.produce"), icon, 162, 64);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ProduceRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(X, 20).addIngredients(VanillaTypes.ITEM_STACK, List.of(recipe.chicken()))
                .setStandardSlotBackground();
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
        // 力量 1 时的概率（主产物权重 = weight × 1，与副产物求和后按比例）
        List<ProduceRecipe.Output> outputs = recipe.outputs().stream().limit(MAX_OUTPUTS).toList();
        int total = outputs.stream().mapToInt(ProduceRecipe.Output::weight).sum();
        if (total <= 0) {
            return;
        }
        for (int i = 0; i < outputs.size(); i++) {
            ProduceRecipe.Output output = outputs.get(i);
            int pct = output.weight() * 100 / total;
            ChickensJeiPlugin.drawChance(guiGraphics, pct + "%", X + 36 + i * 26, 20);
        }
    }
}
