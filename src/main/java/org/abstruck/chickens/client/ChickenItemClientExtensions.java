package org.abstruck.chickens.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/** 鸡物品的客户端扩展：把图标渲染交给 ChickenIconRenderer（实体实时渲染）。 */
public final class ChickenItemClientExtensions implements IClientItemExtensions {
    public static final ChickenItemClientExtensions INSTANCE = new ChickenItemClientExtensions();

    private final ChickenIconRenderer renderer = new ChickenIconRenderer();

    private ChickenItemClientExtensions() {
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return this.renderer;
    }
}
