package org.abstruck.chickens.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.multiplayer.ClientLevel;
import org.abstruck.chickens.entity.chicken.ResourceChicken;
import org.abstruck.chickens.registry.ModDataComponents;
import org.abstruck.chickens.registry.ModEntities;

import java.util.HashMap;
import java.util.Map;

/**
 * 鸡物品图标 = 实体实时渲染（用户规格）。
 * 每品种缓存一只客户端假实体，用 EntityRenderDispatcher 直接画进物品渲染管线；
 * 无品种组件（原版鸡）渲染原版鸡。实体不会入世界、不会 tick，只用于取模型/纹理。
 */
public class ChickenIconRenderer extends BlockEntityWithoutLevelRenderer {
    private static final Map<ResourceLocation, ResourceChicken> DUMMIES = new HashMap<>();
    private static net.minecraft.world.entity.animal.Chicken vanillaDummy;

    public ChickenIconRenderer() {
        super(null, null);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return; // 主菜单无世界：不渲染（物品在游戏内才显示实体图标）
        }
        LivingEntity dummy = dummyFor(level, stack);
        if (dummy == null) {
            return;
        }
        // 幼年/成年状态随物品组件变化（幼年模型自动缩小）
        Boolean mature = stack.get(ModDataComponents.MATURE.get());
        if (dummy instanceof net.minecraft.world.entity.AgeableMob ageable) {
            ageable.setBaby(mature != null && !mature);
        }
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            // 传入姿态已带 ×16（16px = 1 单位）；物品渲染管线已 translate(-0.5,-0.5,-0.5)。
            // 缩放 0.95 → 鸡约 10px 高（用户调校后的尺寸）。不旋转 = 正面朝向。
            // 用户调校：先下调 3px、再上调 1px（净 -2px，即 -0.125 单位）
            poseStack.translate(0.5D, 0.15D - 0.1875D + 0.0625D, 0.5D);
            poseStack.scale(0.95F, 0.95F, 0.95F);
        } else {
            // 世界内（地面/展示框/手中）：1 单位 = 1 格，整鸡呈现
            poseStack.translate(0.5D, 0.0D, 0.5D);
            poseStack.scale(0.9F, 0.9F, 0.9F);
        }
        dummy.setYRot(0.0F);
        dummy.yBodyRot = 0.0F;
        dummy.yHeadRot = 0.0F;
        dummy.xRotO = 0.0F;
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        dispatcher.render(dummy, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    /** 品种 id → 假实体（原版鸡用 null key 单例）；世界切换时重建缓存。 */
    private static LivingEntity dummyFor(ClientLevel level, ItemStack stack) {
        ResourceLocation breed = stack.get(ModDataComponents.BREED.get());
        if (breed == null) {
            if (vanillaDummy == null || vanillaDummy.level() != level) {
                vanillaDummy = EntityType.CHICKEN.create(level);
            }
            return vanillaDummy;
        }
        ResourceChicken cached = DUMMIES.get(breed);
        if (cached == null || cached.level() != level) {
            cached = ModEntities.RESOURCE_CHICKEN.get().create(level);
            if (cached != null) {
                cached.setBreed(breed);
                DUMMIES.put(breed, cached);
            }
        }
        return cached;
    }
}
