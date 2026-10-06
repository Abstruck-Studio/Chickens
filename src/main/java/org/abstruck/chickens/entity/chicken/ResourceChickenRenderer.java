package org.abstruck.chickens.entity.chicken;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.breed.ChickenBreed;
import org.abstruck.chickens.registry.ModEntities;

import java.util.HashMap;
import java.util.Map;

/**
 * 资源鸡渲染器：复用原版鸡模型几何体（ChickenModel + ModelLayers.CHICKEN），纹理按品种选择：
 * 品种 JSON 的 texture 字段 → 约定路径 {@code chickens:textures/entity/chicken/<品种id>.png} → 原版鸡纹理。
 */
public class ResourceChickenRenderer extends MobRenderer<ResourceChicken, ChickenModel<ResourceChicken>> {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation FALLBACK_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/chicken/chicken.png");

    /** 品种 id → 最终纹理（含 .png），缓存避免每帧解析 */
    private static final Map<ResourceLocation, ResourceLocation> TEXTURE_CACHE = new HashMap<>();

    public ResourceChickenRenderer(EntityRendererProvider.Context context) {
        super(context, new ChickenModel<>(context.bakeLayer(ModelLayers.CHICKEN)), 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(ResourceChicken entity) {
        try {
            ResourceLocation breedId = entity.getBreedId();
            ResourceLocation texture = TEXTURE_CACHE.get(breedId);
            if (texture == null) {
                texture = resolveTexture(entity, breedId);
                // 缓存前验证纹理真实存在（资源重载时可能短暂不可用，避免把坏值缓存成隐形鸡）
                if (!Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
                    texture = FALLBACK_TEXTURE;
                }
                TEXTURE_CACHE.put(breedId, texture);
            }
            return texture;
        } catch (Exception e) {
            LOGGER.error("[chickens] 纹理解析失败，回退原版纹理: {}", e.toString());
            return FALLBACK_TEXTURE;
        }
    }

    /** 复刻原版 ChickenRenderer 的翅膀动画：基类 getBob 默认返回 tickCount + partialTick（翅膀一直转），
     *  需用 (sin(flap)+1) × flapSpeed 产生真正的拍动 */
    @Override
    protected float getBob(ResourceChicken chicken, float partialTicks) {
        float flap = Mth.lerp(partialTicks, chicken.oFlap, chicken.flap);
        float flapSpeed = Mth.lerp(partialTicks, chicken.oFlapSpeed, chicken.flapSpeed);
        return (Mth.sin(flap) + 1.0F) * flapSpeed;
    }

    private static ResourceLocation resolveTexture(ResourceChicken entity, ResourceLocation breedId) {
        ChickenBreed breed = entity.getBreed(entity.level().registryAccess());
        if (breed != null && breed.texture().isPresent()) {
            return withPng(breed.texture().get());
        }
        ResourceLocation derived = withPng(ResourceLocation.fromNamespaceAndPath(
                Chickens.MODID, "textures/entity/chicken/" + breedId.getPath()));
        if (Minecraft.getInstance().getResourceManager().getResource(derived).isPresent()) {
            return derived;
        }
        return FALLBACK_TEXTURE;
    }

    private static ResourceLocation withPng(ResourceLocation location) {
        return ResourceLocation.fromNamespaceAndPath(location.getNamespace(), location.getPath() + ".png");
    }

    /** 渲染器注册（仅客户端） */
    @EventBusSubscriber(modid = Chickens.MODID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.RESOURCE_CHICKEN.get(), ResourceChickenRenderer::new);
            // 染料蛋投掷实体（用物品模型渲染，自动带色）
            event.registerEntityRenderer(ModEntities.THROWN_DYE_EGG.get(),
                    net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        }
    }
}
