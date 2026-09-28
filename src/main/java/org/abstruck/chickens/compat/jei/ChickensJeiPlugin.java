package org.abstruck.chickens.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.breed.BreedLookups;
import org.abstruck.chickens.breed.ChickenBreed;
import org.abstruck.chickens.breed.MutationRule;
import org.abstruck.chickens.registry.ModDataComponents;
import org.abstruck.chickens.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 联动：两个配方分类——
 * 杂交配方（chickens:mutation 注册表驱动）与产出配方（chickens:breed 注册表驱动）。
 * 经 @JeiPlugin 由 JEI 自动扫描加载；未安装 JEI 时本类不会被加载。
 */
@JeiPlugin
public class ChickensJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Chickens.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        // 分类 TAB 图标：物品鸡（抓住原版鸡得到的 chickens:chicken 物品）
        mezz.jei.api.gui.drawable.IDrawable icon = registration.getJeiHelpers().getGuiHelper()
                .createDrawableItemStack(new ItemStack(ModItems.CHICKEN.get()));
        registration.addRecipeCategories(new MutationRecipeCategory(icon), new ProduceRecipeCategory(icon));
    }

    /** 配方催化剂（配方页左上角显示的方块）：鸡杂交 = 繁殖箱、鸡产出 = 鸡窝 */
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModItems.BREEDING_BOX.get(), MutationRecipeCategory.TYPE);
        registration.addRecipeCatalyst(ModItems.NEST.get(), ProduceRecipeCategory.TYPE);
    }

    /** 按组件区分物品子类型：鸡物品按品种、流体蛋按流体、染料蛋按颜色（否则 JEI 合并成无组件版本） */
    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(ModItems.CHICKEN.get(), interpreter((ItemStack stack, UidContext context) -> {
            ResourceLocation breed = stack.get(ModDataComponents.BREED.get());
            return breed != null ? breed.toString() : "vanilla";
        }));
        registration.registerSubtypeInterpreter(ModItems.FLUID_EGG.get(), interpreter((ItemStack stack, UidContext context) -> {
            ResourceLocation fluid = stack.get(ModDataComponents.FLUID.get());
            return fluid != null ? fluid.toString() : "empty";
        }));
        registration.registerSubtypeInterpreter(ModItems.DYE_EGG.get(), interpreter((ItemStack stack, UidContext context) -> {
            DyedItemColor color = stack.get(net.minecraft.core.component.DataComponents.DYED_COLOR);
            return color != null ? String.valueOf(color.rgb() & 0xFFFFFF) : "undyed";
        }));
    }

    /** ISubtypeInterpreter 的两个抽象方法共用一个函数 */
    private static <T> ISubtypeInterpreter<T> interpreter(
            java.util.function.BiFunction<T, UidContext, String> fn) {
        return new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(T ingredient, UidContext context) {
                return fn.apply(ingredient, context);
            }

            @Override
            public String getLegacyStringSubtypeInfo(T ingredient, UidContext context) {
                return fn.apply(ingredient, context);
            }
        };
    }

    /** 把全部变体加进 JEI 物品列表：45 种物品鸡 + 16 色染料蛋 + 全部流体蛋 */
    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        List<ItemStack> extras = new ArrayList<>();
        if (minecraft.getConnection() != null) {
            RegistryAccess access = minecraft.getConnection().registryAccess();
            for (ResourceLocation id : BreedLookups.breedRegistry(access).keySet()) {
                ItemStack stack = new ItemStack(ModItems.CHICKEN.get());
                stack.set(ModDataComponents.BREED.get(), id);
                extras.add(stack);
            }
            for (var entry : BreedLookups.fluidEggRegistry(access)) {
                ItemStack stack = new ItemStack(ModItems.FLUID_EGG.get());
                stack.set(ModDataComponents.FLUID.get(), entry.fluid());
                extras.add(stack);
            }
        }
        for (DyeColor dye : DyeColor.values()) {
            ItemStack stack = new ItemStack(ModItems.DYE_EGG.get());
            stack.set(net.minecraft.core.component.DataComponents.DYED_COLOR,
                    new DyedItemColor(dye.getTextureDiffuseColor() & 0xFFFFFF, false));
            extras.add(stack);
        }
        registration.addExtraItemStacks(extras);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // 配方数据来自数据包注册表（已同步到客户端）
        Minecraft minecraft = Minecraft.getInstance();
        RegistryAccess access = minecraft.getConnection() != null
                ? minecraft.getConnection().registryAccess()
                : (minecraft.level != null ? minecraft.level.registryAccess() : null);
        if (access == null) {
            return; // 尚未进入世界
        }
        List<MutationRecipe> mutations = buildMutations(access);
        List<ProduceRecipe> produces = buildProduces(access);
        registration.addRecipes(MutationRecipeCategory.TYPE, mutations);
        registration.addRecipes(ProduceRecipeCategory.TYPE, produces);
        // 物品鸡的烧制配方（熔炉/烟熏炉/营火，与生鸡肉一致）：
        // JEI 的用途查询按组件 uid 精确匹配，自动发现的无组件配方匹配不到带品种的变种——
        // 手动向 vanilla 分类注册「输入含全部变种」的配方（输入槽可循环查看 46 种鸡）
        registerChickenCooking(access, registration);
    }

    /** 向 vanilla 熔炉/烟熏炉/营火分类注册变种版烧制配方 */
    private static void registerChickenCooking(RegistryAccess access, IRecipeRegistration registration) {
        List<ItemStack> variants = new ArrayList<>();
        variants.add(new ItemStack(ModItems.CHICKEN.get())); // 原版物品鸡
        for (ResourceLocation id : BreedLookups.breedRegistry(access).keySet()) {
            ItemStack stack = new ItemStack(ModItems.CHICKEN.get());
            stack.set(ModDataComponents.BREED.get(), id);
            variants.add(stack);
        }
        net.minecraft.world.item.crafting.Ingredient ingredient =
                net.minecraft.world.item.crafting.Ingredient.of(variants.toArray(ItemStack[]::new));
        ItemStack result = new ItemStack(net.minecraft.world.item.Items.COOKED_CHICKEN);
        net.minecraft.world.item.crafting.CookingBookCategory category =
                net.minecraft.world.item.crafting.CookingBookCategory.FOOD;
        ResourceLocation uid = ResourceLocation.fromNamespaceAndPath(Chickens.MODID, "chicken_cooking_jei");
        registration.addRecipes(mezz.jei.api.constants.RecipeTypes.SMELTING, List.of(
                new net.minecraft.world.item.crafting.RecipeHolder<>(uid,
                        new net.minecraft.world.item.crafting.SmeltingRecipe("chickens", category,
                                ingredient, result.copy(), 0.35F, 200))));
        registration.addRecipes(mezz.jei.api.constants.RecipeTypes.SMOKING, List.of(
                new net.minecraft.world.item.crafting.RecipeHolder<>(uid,
                        new net.minecraft.world.item.crafting.SmokingRecipe("chickens", category,
                                ingredient, result.copy(), 0.35F, 100))));
        registration.addRecipes(mezz.jei.api.constants.RecipeTypes.CAMPFIRE_COOKING, List.of(
                new net.minecraft.world.item.crafting.RecipeHolder<>(uid,
                        new net.minecraft.world.item.crafting.CampfireCookingRecipe("chickens", category,
                                ingredient, result.copy(), 0.35F, 600))));
    }

    /** 杂交配方：每条 mutation 规则一条（输出显示新品种，tooltip 列全部加权结果） */
    private static List<MutationRecipe> buildMutations(RegistryAccess access) {
        List<MutationRecipe> recipes = new ArrayList<>();
        var breeds = BreedLookups.breedRegistry(access);
        for (MutationRule rule : BreedLookups.mutationRegistry(access)) {
            if (rule.parents().size() != 2) {
                continue;
            }
            ItemStack parentA = chickenStack(access, rule.parents().get(0));
            ItemStack parentB = chickenStack(access, rule.parents().get(1));
            if (parentA.isEmpty() || parentB.isEmpty()) {
                continue;
            }
            // 每个加权结果一个鸡物品（与 allResults 平行）
            List<ItemStack> resultStacks = new ArrayList<>();
            boolean allValid = true;
            for (MutationRule.WeightedResult r : rule.results()) {
                ItemStack stack = chickenStack(access, r.breed());
                if (stack.isEmpty()) {
                    allValid = false;
                    break;
                }
                resultStacks.add(stack);
            }
            if (!allValid) {
                continue;
            }
            recipes.add(new MutationRecipe(parentA, parentB, List.copyOf(resultStacks),
                    List.copyOf(rule.results())));
        }
        return recipes;
    }

    /** 产出配方：每个品种一条（输入鸡物品，输出主产物 + 副产物列表）+ 原版鸡 → 原版鸡蛋 */
    private static List<ProduceRecipe> buildProduces(RegistryAccess access) {
        List<ProduceRecipe> recipes = new ArrayList<>();
        var breeds = BreedLookups.breedRegistry(access);
        for (ResourceLocation id : breeds.keySet()) {
            ChickenBreed breed = breeds.get(id);
            if (breed == null) {
                continue;
            }
            ItemStack chicken = chickenStack(access, id);
            if (chicken.isEmpty()) {
                continue;
            }
            List<ProduceRecipe.Output> outputs = new ArrayList<>();
            outputs.add(new ProduceRecipe.Output(
                    productStack(access, breed.item()), breed.item().weight(), true));
            for (ChickenBreed.Product byproduct : breed.byproducts()) {
                ItemStack stack = productStack(access, byproduct);
                if (!stack.isEmpty()) {
                    outputs.add(new ProduceRecipe.Output(stack, byproduct.weight(), false));
                }
            }
            recipes.add(new ProduceRecipe(chicken, List.copyOf(outputs)));
        }
        // 原版物品鸡（无品种组件）→ 原版鸡蛋
        ItemStack vanillaChicken = new ItemStack(ModItems.CHICKEN.get());
        recipes.add(new ProduceRecipe(vanillaChicken, List.of(
                new ProduceRecipe.Output(new ItemStack(net.minecraft.world.item.Items.EGG), 1, true))));
        return recipes;
    }

    private static ItemStack chickenStack(RegistryAccess access, ResourceLocation breedId) {
        if (!BreedLookups.breedRegistry(access).containsKey(breedId)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(ModItems.CHICKEN.get());
        stack.set(ModDataComponents.BREED.get(), breedId);
        return stack;
    }

    private static ItemStack itemStack(RegistryAccess access, ResourceLocation itemId) {
        var item = BreedLookups.resolveItem(access, itemId);
        return item != null ? new ItemStack(item) : ItemStack.EMPTY;
    }

    /** 产出物品 + 可选流体组件（流体蛋必须带 FLUID 组件，否则 JEI uid 对不上、查不到配方） */
    private static ItemStack productStack(RegistryAccess access, ChickenBreed.Product product) {
        ItemStack stack = itemStack(access, product.item());
        if (!stack.isEmpty()) {
            product.fluid().ifPresent(fluid -> stack.set(ModDataComponents.FLUID.get(), fluid));
        }
        return stack;
    }

    /** 供分类绘制概率文本用的工具：结果表总权重 */
    public static double totalWeight(List<MutationRule.WeightedResult> results) {
        return results.stream().mapToDouble(MutationRule.WeightedResult::weight).sum();
    }
}
