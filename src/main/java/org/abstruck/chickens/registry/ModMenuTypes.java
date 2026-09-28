package org.abstruck.chickens.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.block.breedingbox.BreedingBoxMenu;
import org.abstruck.chickens.block.grower.GrowerMenu;
import org.abstruck.chickens.block.nest.NestMenu;

/**
 * 菜单类型注册（阶段 5：孵化巢、繁殖箱）。
 */
public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, Chickens.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<BreedingBoxMenu>> BREEDING_BOX =
            MENU_TYPES.register("breeding_box", () -> new MenuType<>(BreedingBoxMenu::new, FeatureFlagSet.of()));

    public static final DeferredHolder<MenuType<?>, MenuType<GrowerMenu>> GROWER =
            MENU_TYPES.register("grower", () -> new MenuType<>(GrowerMenu::new, FeatureFlagSet.of()));

    public static final DeferredHolder<MenuType<?>, MenuType<NestMenu>> NEST =
            MENU_TYPES.register("nest", () -> new MenuType<>(NestMenu::new, FeatureFlagSet.of()));

    private ModMenuTypes() {
    }
}
