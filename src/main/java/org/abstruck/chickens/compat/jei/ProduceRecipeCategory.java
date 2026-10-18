package org.abstruck.chickens.compat.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.registry.ModRegistry;

import java.util.List;

/** 产出配方分类：鸡 → 全部产物（第一个主产物，其余副产物），下方按权重标注概率（对应力量 1 的效果） */
public class ProduceRecipeCategory implements IRecipeCategory<ProduceRecipe> {
    public static final RecipeType<ProduceRecipe> TYPE =
            RecipeType.create(Chickens.MODID, "produce", ProduceRecipe.class);

    private static final int MAX_OUTPUTS = 5;
    /** 内容宽 158，页宽 162 → 居中偏移 2 */
    private static final int X = 2;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;
    private final Component title;

    public ProduceRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(162, 60);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.CHICKEN.get()));
        this.slot = guiHelper.getSlotDrawable();
        this.title = Component.translatable("jei.chickens.produce");
    }

    @Override
    public RecipeType<ProduceRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ProduceRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, X + 2, 20)
                .addItemStack(recipe.chicken())
                .setBackground(this.slot, -1, -1);
        List<ProduceRecipe.Output> outputs = recipe.outputs().stream().limit(MAX_OUTPUTS).toList();
        for (int i = 0; i < outputs.size(); i++) {
            ProduceRecipe.Output output = outputs.get(i);
            builder.addSlot(RecipeIngredientRole.OUTPUT, X + 38 + i * 26, 20)
                    .addItemStack(output.stack())
                    .setBackground(this.slot, -1, -1);
        }
    }

    @Override
    public void draw(ProduceRecipe recipe, IRecipeSlotsView recipeSlotsView, PoseStack pose,
                     double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        font.draw(pose, "→", X + 20, 24, 0xFF555555);
        // 力量 1 时的概率（主产物权重 = weight × 1，与副产物求和后按比例）
        List<ProduceRecipe.Output> outputs = recipe.outputs().stream().limit(MAX_OUTPUTS).toList();
        int total = outputs.stream().mapToInt(ProduceRecipe.Output::weight).sum();
        if (total <= 0) {
            return;
        }
        for (int i = 0; i < outputs.size(); i++) {
            ProduceRecipe.Output output = outputs.get(i);
            int pct = output.weight() * 100 / total;
            font.draw(pose, pct + "%", X + 37 + i * 26, 42, 0xFF3F3F3F);
        }
    }
}
