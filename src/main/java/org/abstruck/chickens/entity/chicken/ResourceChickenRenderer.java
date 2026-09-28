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
 * 资源鸡渲染器：直接复用原版鸡的模型几何体（ChickenModel + ModelLayers.CHICKEN），
 * 纹理按品种选择（不做染色）：
 * 1) 品种 JSON 里的 texture 字段（不含 .png 后缀）；
 * 2) 约定路径 {@code chickens:textures/entity/chicken/&lt;品种id&gt;.png}（用户把 PNG 放进 assets 即可，不用改 JSON）；
 * 3) 回退原版鸡纹理。
 */
public class ResourceChickenRenderer extends MobRenderer<ResourceChicken, ChickenModel<ResourceChicken>> {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation FALLBACK_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/chicken/chicken.png");

    /** 品种 id → 最终纹理（含 .png），缓存避免每帧解析。 */
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
                // 防御：缓存前验证纹理真实存在（资源管理器在维度切换/重载时可能短暂不可用，
                // 避免把坏值缓存下来导致「隐形鸡」直到重启）
                if (!Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
                    texture = FALLBACK_TEXTURE;
                }
                TEXTURE_CACHE.put(breedId, texture);
            }
            return texture;
        } catch (Exception e) {
            // 防御：纹理解析无论如何不能把鸡变隐形
            LOGGER.error("[chickens] 纹理解析失败，回退原版纹理: {}", e.toString());
            return FALLBACK_TEXTURE;
        }
    }

    /**
     * 复刻原版 ChickenRenderer 的翅膀动画驱动：
     * LivingEntityRenderer 把 getBob 的返回值作为第 4 个参数传给 setupAnim，
     * ChickenModel 用它设翅膀 zRot。基类默认实现返回 tickCount + partialTick
     * （永远增长的数 → 翅膀一直旋转），原版用 (sin(flap)+1)×flapSpeed 产生真正的拍动。
     */
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

    /** 客户端事件：渲染器注册（仅客户端类；21.1.252 里 MOD 总线是默认值，bus 参数已过时） */
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
