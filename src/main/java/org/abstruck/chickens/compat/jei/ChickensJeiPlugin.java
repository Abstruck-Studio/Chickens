package org.abstruck.chickens.compat.jei;

import com.mojang.datafixers.util.Either;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.breed.BreedLookups;
import org.abstruck.chickens.breed.ChickenBreed;
import org.abstruck.chickens.breed.ChickenData;
import org.abstruck.chickens.breed.FluidEggEntry;
import org.abstruck.chickens.breed.MutationRule;
import org.abstruck.chickens.item.DyeEggColors;
import org.abstruck.chickens.item.ItemData;
import org.abstruck.chickens.registry.ModRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JEI 联动：杂交配方（chickens:mutation 数据驱动）与产出配方（chickens:breed 数据驱动）两个分类。
 * 1.19.2 使用 JEI 11 的 API（分类直接实现 {@code IRecipeCategory}，绘制用 {@code PoseStack}）。
 */
@JeiPlugin
public class ChickensJeiPlugin implements IModPlugin {
    private static IJeiRuntime runtime;
    /** 是否已经在「数据已就绪」的情况下注册过配方（避免重复注册） */
    private static boolean registeredWithData;
    /** 是否已经补全 JEI 物品列表变体（移除空白蛋 + 加 16 色/流体/品种鸡） */
    private static boolean ingredientsAdded;

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(Chickens.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new MutationRecipeCategory(guiHelper),
                new ProduceRecipeCategory(guiHelper));
    }

    /** 配方催化剂（配方页显示的工作方块）：杂交 = 繁殖箱、产出 = 鸡窝 */
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModRegistry.BREEDING_BOX_ITEM.get()),
                MutationRecipeCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModRegistry.NEST_ITEM.get()),
                ProduceRecipeCategory.TYPE);
    }

    /** 按物品数据区分子类型：鸡按品种、流体蛋按流体、染料蛋按颜色（否则 JEI 会把变体合并成一个） */
    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(ModRegistry.CHICKEN.get(), (stack, context) -> {
            ResourceLocation breed = ItemData.getBreed(stack);
            return breed != null ? breed.toString() : "vanilla";
        });
        registration.registerSubtypeInterpreter(ModRegistry.FLUID_EGG.get(), (stack, context) -> {
            ResourceLocation fluid = ItemData.getFluidId(stack);
            return fluid != null ? fluid.toString() : "empty";
        });
        registration.registerSubtypeInterpreter(ModRegistry.DYE_EGG.get(), (stack, context) -> {
            int color = ItemData.getColor(stack);
            return color >= 0 ? String.valueOf(color & 0xFFFFFF) : "undyed";
        });
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // 配方数据来自服务端同步的 chickens:breed / chickens:mutation 内容
        registration.addRecipes(MutationRecipeCategory.TYPE, buildMutations());
        registration.addRecipes(ProduceRecipeCategory.TYPE, buildProduces());
        registeredWithData = !ChickenData.get().breeds().isEmpty();
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        tryLateRegister();
    }

    /**
     * 客户端数据包可能比 JEI 构建配方更晚到达（多人服务器首次进入）。
     * 此时用 JEI 运行时的 addRecipes 补注册一次，避免配方页空白。
     */
    public static void tryLateRegister() {
        if (runtime == null) {
            return;
        }
        if (!registeredWithData && !ChickenData.get().breeds().isEmpty()) {
            runtime.getRecipeManager().addRecipes(MutationRecipeCategory.TYPE, buildMutations());
            runtime.getRecipeManager().addRecipes(ProduceRecipeCategory.TYPE, buildProduces());
            registeredWithData = true;
        }
        addIngredientVariants();
    }

    /**
     * JEI 物品列表：移除没有颜色的「空白」染料蛋/流体蛋，补上 16 色染料蛋、各流体蛋与全部品种鸡。
     * 数据未就绪（多人服务器首次进入）时等数据同步后的 tryLateRegister 再执行。
     */
    private static void addIngredientVariants() {
        if (runtime == null || ingredientsAdded || ChickenData.get().breeds().isEmpty()) {
            return;
        }
        var manager = runtime.getIngredientManager();
        manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, List.of(
                new ItemStack(ModRegistry.DYE_EGG.get()),
                new ItemStack(ModRegistry.FLUID_EGG.get())));
        List<ItemStack> added = new ArrayList<>();
        for (DyeColor dye : DyeColor.values()) {
            added.add(DyeEggColors.stack(dye));
        }
        for (FluidEggEntry entry : ChickenData.get().fluidEggs().values()) {
            if (BreedLookups.fluidOf(entry.fluid()) != null) {
                ItemStack egg = new ItemStack(ModRegistry.FLUID_EGG.get());
                ItemData.setFluid(egg, entry.fluid());
                added.add(egg);
            }
        }
        for (ResourceLocation breed : ChickenData.get().breeds().keySet()) {
            added.add(chickenStack(breed));
        }
        manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, added);
        ingredientsAdded = true;
    }

    /** 杂交配方：每条 mutation 规则一条；父母的 #标签 展开为标签全部成员（槽内循环查看） */
    private static List<MutationRecipe> buildMutations() {
        List<MutationRecipe> recipes = new ArrayList<>();
        for (MutationRule rule : ChickenData.get().mutations().values()) {
            if (rule.parents().size() != 2) {
                continue;
            }
            List<ItemStack> parentA = refStacks(rule.parents().get(0).value());
            List<ItemStack> parentB = refStacks(rule.parents().get(1).value());
            if (parentA.isEmpty() || parentB.isEmpty()) {
                continue;
            }
            List<List<ItemStack>> resultStacks = new ArrayList<>();
            boolean allValid = true;
            for (MutationRule.WeightedResult result : rule.results()) {
                List<ItemStack> stacks = refStacks(result.breed().value());
                if (stacks.isEmpty()) {
                    allValid = false;
                    break;
                }
                resultStacks.add(stacks);
            }
            if (!allValid) {
                continue;
            }
            recipes.add(new MutationRecipe(parentA, parentB, List.copyOf(resultStacks),
                    List.copyOf(rule.results())));
        }
        return recipes;
    }

    /** 引用的展示槽内容：精确品种 → 单个鸡物品；#标签（或 $parent:标签）→ 标签全部成员鸡（槽内循环） */
    private static List<ItemStack> refStacks(Either<ResourceLocation, TagKey<ChickenBreed>> value) {
        if (value.left().isPresent()) {
            ItemStack stack = chickenStack(value.left().get());
            return stack.isEmpty() ? List.of() : List.of(stack);
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (ResourceLocation member : BreedLookups.breedsWithTag(value.right().orElseThrow())) {
            ItemStack stack = chickenStack(member);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    /** 产出配方：每个品种一条（输入鸡物品，输出主产物 + 副产物列表）+ 原版鸡 → 原版鸡蛋 */
    private static List<ProduceRecipe> buildProduces() {
        List<ProduceRecipe> recipes = new ArrayList<>();
        for (Map.Entry<ResourceLocation, ChickenBreed> entry : ChickenData.sortedBreeds()) {
            ChickenBreed breed = entry.getValue();
            ItemStack chicken = chickenStack(entry.getKey());
            if (chicken.isEmpty()) {
                continue;
            }
            List<ProduceRecipe.Output> outputs = new ArrayList<>();
            outputs.add(new ProduceRecipe.Output(productStack(breed.item()), breed.item().weight(), true));
            for (ChickenBreed.Product byproduct : breed.byproducts()) {
                ItemStack stack = productStack(byproduct);
                if (!stack.isEmpty()) {
                    outputs.add(new ProduceRecipe.Output(stack, byproduct.weight(), false));
                }
            }
            recipes.add(new ProduceRecipe(chicken, List.copyOf(outputs)));
        }
        ItemStack vanillaChicken = new ItemStack(ModRegistry.CHICKEN.get());
        recipes.add(new ProduceRecipe(vanillaChicken, List.of(
                new ProduceRecipe.Output(new ItemStack(net.minecraft.world.item.Items.EGG), 1, true))));
        return recipes;
    }

    private static ItemStack chickenStack(ResourceLocation breedId) {
        if (!ChickenData.get().breeds().containsKey(breedId)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(ModRegistry.CHICKEN.get());
        ItemData.setBreed(stack, breedId);
        return stack;
    }

    /** 产出物品 + 可选流体数据（流体蛋必须带流体 NBT，否则 JEI uid 对不上、查不到配方） */
    private static ItemStack productStack(ChickenBreed.Product product) {
        // JEI 无随机源：标签类产物固定显示标签内第一个已注册物品（显示必须稳定）
        var item = BreedLookups.resolveItemForDisplay(product.item());
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        product.fluid().ifPresent(fluidRef -> {
            net.minecraft.world.level.material.Fluid fluid = BreedLookups.fluidOfForDisplay(fluidRef);
            if (fluid != null) {
                ItemData.setFluid(stack,
                        net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(fluid));
            }
        });
        return stack;
    }

    /** 结果表总权重（供分类绘制概率文本用） */
    public static double totalWeight(List<MutationRule.WeightedResult> results) {
        return results.stream().mapToDouble(MutationRule.WeightedResult::weight).sum();
    }
}
