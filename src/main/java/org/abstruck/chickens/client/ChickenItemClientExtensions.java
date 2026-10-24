package org.abstruck.chickens.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** 鸡物品客户端扩展：图标渲染交给 ChickenIconRenderer */
public final class ChickenItemClientExtensions implements IClientItemExtensions {
    public static final ChickenItemClientExtensions INSTANCE = new ChickenItemClientExtensions();

    private ChickenIconRenderer renderer;

    private ChickenItemClientExtensions() {
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (this.renderer == null) {
            this.renderer = new ChickenIconRenderer();
        }
        return this.renderer;
    }
}
