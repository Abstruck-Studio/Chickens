package org.abstruck.chickens.client;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.List;

/**
 * 鸡物品的 BakedModel 包装：只把 isCustomRenderer 置为 true，
 * 让 ItemRenderer 走 IClientItemExtensions 的定制渲染（实体实时渲染），其余全部委托原模型。
 */
public final class ChickenIconBakedModel implements BakedModel {
    private final BakedModel delegate;

    public ChickenIconBakedModel(BakedModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isCustomRenderer() {
        return true;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        return this.delegate.getQuads(state, side, random);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.delegate.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return this.delegate.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return this.delegate.usesBlockLight();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.delegate.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return this.delegate.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.delegate.getOverrides();
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean applyLeftHandTransform) {
        this.delegate.applyTransform(context, poseStack, applyLeftHandTransform);
        return this;
    }
}
