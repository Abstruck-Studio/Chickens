package org.abstruck.chickens.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.abstruck.chickens.entity.chicken.ResourceChicken;
import org.abstruck.chickens.item.ItemData;
import org.abstruck.chickens.registry.ModRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * 鸡物品图标 = 实体实时渲染：每品种缓存一只客户端假实体（不入世界、不 tick，只取模型/纹理），
 * 用 EntityRenderDispatcher 直接画进物品渲染管线；无品种数据的渲染原版鸡。
 */
public class ChickenIconRenderer extends BlockEntityWithoutLevelRenderer {
    private static final Map<ResourceLocation, ResourceChicken> DUMMIES = new HashMap<>();
    private static net.minecraft.world.entity.animal.Chicken vanillaDummy;

    public ChickenIconRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemTransforms.TransformType context, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return; // 主菜单无世界：不渲染
        }
        LivingEntity dummy = dummyFor(level, stack);
        if (dummy == null) {
            return;
        }
        // 幼年/成年随物品数据切换（幼年模型自动缩小）
        if (dummy instanceof net.minecraft.world.entity.AgeableMob ageable) {
            ageable.setBaby(!ItemData.isMature(stack));
        }
        poseStack.pushPose();
        if (context == ItemTransforms.TransformType.GUI) {
            // 传入姿态已带 ×16（16px = 1 单位）；管线已 translate(-0.5,-0.5,-0.5)。
            // 缩放 0.95 → 鸡约 10px 高；不旋转 = 正面朝向
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

    /** 品种 id → 假实体（原版鸡用单独字段）；世界切换时重建缓存 */
    private static LivingEntity dummyFor(ClientLevel level, ItemStack stack) {
        ResourceLocation breed = ItemData.getBreed(stack);
        if (breed == null) {
            if (vanillaDummy == null || vanillaDummy.level != level) {
                vanillaDummy = EntityType.CHICKEN.create(level);
            }
            return vanillaDummy;
        }
        ResourceChicken cached = DUMMIES.get(breed);
        if (cached == null || cached.level != level) {
            cached = ModRegistry.RESOURCE_CHICKEN.get().create(level);
            if (cached != null) {
                cached.setBreed(breed);
                DUMMIES.put(breed, cached);
            }
        }
        return cached;
    }
}
