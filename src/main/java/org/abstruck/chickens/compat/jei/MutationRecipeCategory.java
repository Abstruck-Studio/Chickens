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
import org.abstruck.chickens.breed.MutationRule;
import org.abstruck.chickens.registry.ModRegistry;

import java.util.List;

/** 杂交配方分类：鸡A + 鸡B → 全部结果（主结果 + 回退），概率标在每个结果下方 */
public class MutationRecipeCategory implements IRecipeCategory<MutationRecipe> {
    public static final RecipeType<MutationRecipe> TYPE =
            RecipeType.create(Chickens.MODID, "mutation", MutationRecipe.class);

    /** 内容宽 130，页宽 162 → 居中偏移 16 */
    private static final int X = 16;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;
    private final Component title;

    public MutationRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(162, 64);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.CHICKEN.get()));
        this.slot = guiHelper.getSlotDrawable();
        this.title = Component.translatable("jei.chickens.mutation");
    }

    @Override
    public RecipeType<MutationRecipe> getRecipeType() {
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
    public void setRecipe(IRecipeLayoutBuilder builder, MutationRecipe recipe, IFocusGroup focuses) {
        // 槽内容可能是多只鸡（#标签 父母 / $parent:标签 结果），JEI 会在槽内循环展示
        builder.addSlot(RecipeIngredientRole.INPUT, X + 2, 20)
                .addItemStacks(recipe.parentA())
                .setBackground(this.slot, -1, -1);
        builder.addSlot(RecipeIngredientRole.INPUT, X + 30, 20)
                .addItemStacks(recipe.parentB())
                .setBackground(this.slot, -1, -1);
        List<List<ItemStack>> stacks = recipe.resultStacks();
        for (int i = 0; i < stacks.size(); i++) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, X + 74 + i * 26, 20)
                    .addItemStacks(stacks.get(i))
                    .setBackground(this.slot, -1, -1);
        }
    }

    @Override
    public void draw(MutationRecipe recipe, IRecipeSlotsView recipeSlotsView, PoseStack pose,
                     double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        font.draw(pose, "+", X + 22, 24, 0xFF808080);
        font.draw(pose, "→", X + 54, 24, 0xFF555555);
        double total = ChickensJeiPlugin.totalWeight(recipe.allResults());
        List<MutationRule.WeightedResult> results = recipe.allResults();
        for (int i = 0; i < results.size(); i++) {
            MutationRule.WeightedResult result = results.get(i);
            String pct = String.format("%.0f%%", result.weight() / total * 100);
            // 槽位中心 x+8；槽底 y+16，下一行留 5px 间距 → y = 43（分类高 64）
            int slotX = X + 74 + i * 26;
            font.draw(pose, pct, slotX + 8 - font.width(pct) / 2, 43, 0xFF3F3F3F);
        }
    }
}
