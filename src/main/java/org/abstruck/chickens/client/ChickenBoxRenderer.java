package org.abstruck.chickens.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.block.grower.GrowerBlockEntity;
import org.abstruck.chickens.block.nest.NestBlockEntity;
import org.abstruck.chickens.registry.ModBlockEntities;

/**
 * 鸡窝/培育箱方块内渲染鸡槽里的物品鸡（与背包图标同一渲染管线）：
 * 物品的定制渲染器（ChickenIconRenderer）本来就按品种/幼年状态画实体鸡模型，
 * 这里直接 renderStatic 物品即可，无需维护假实体。
 *
 * 位置推导：物品管线前置 translate(-0.5,-0.5,-0.5)，ChickenIconRenderer 世界分支
 * translate(0.5,0,0.5)+scale(0.9) → 鸡模型原点在方块中心的 (0.5, y-0.5·SCALE, 0.5)、
 * 整鸡高约 0.94×0.9×SCALE 格。CHICKEN_Y = 0.5×SCALE 使鸡脚贴方块底、整鸡在方块内部。
 * 朝向：item/generated 的 fixed 变换带 180° 旋转，外层补 facing.toYRot()-180 使鸡面朝方块正面。
 * 光照：方块内部无光，鸡固定全亮（FULL_BRIGHT）渲染。
 */
public final class ChickenBoxRenderer {
    /** 外层缩放（叠加物品渲染内部 0.9）：0.85 → 整鸡约 0.72 格高 */
    private static final float SCALE = 0.85F;
    /** 方块内抬升 = 0.5 × SCALE，脚贴方块底（用户可进游戏微调） */
    private static final double CHICKEN_Y = 0.5D * SCALE;

    private ChickenBoxRenderer() {
    }

    /** 在方块中心渲染鸡槽物品，鸡面朝方块正面（facing），全亮光照 */
    private static void renderChickenItem(ItemStack stack, Direction facing, PoseStack pose,
                                          MultiBufferSource buffer, int packedOverlay) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        pose.pushPose();
        pose.translate(0.5D, CHICKEN_Y, 0.5D);
        // fixed 上下文与实体渲染管线各带一次 180° 旋转（互相抵消），
        // 外层补 180 - toYRot 使模型面（初始朝北）最终朝向 facing
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        pose.scale(SCALE, SCALE, SCALE);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, packedOverlay,
                pose, buffer, Minecraft.getInstance().level, 0);
        pose.popPose();
    }

    /** 鸡窝：渲染鸡槽中的成年鸡物品 */
    public static class NestRenderer implements BlockEntityRenderer<NestBlockEntity> {
        public NestRenderer(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(NestBlockEntity be, float partialTick, PoseStack pose,
                           MultiBufferSource buffer, int packedLight, int packedOverlay) {
            ItemStack stack = be.getItems().getStackInSlot(NestBlockEntity.CHICKEN_IN);
            if (stack.isEmpty()) {
                return;
            }
            renderChickenItem(stack, be.getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                    pose, buffer, packedOverlay);
        }
    }

    /** 培育箱：渲染鸡槽中的幼年鸡物品 */
    public static class GrowerRenderer implements BlockEntityRenderer<GrowerBlockEntity> {
        public GrowerRenderer(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(GrowerBlockEntity be, float partialTick, PoseStack pose,
                           MultiBufferSource buffer, int packedLight, int packedOverlay) {
            ItemStack stack = be.getItems().getStackInSlot(GrowerBlockEntity.CHICKEN_IN);
            if (stack.isEmpty()) {
                return;
            }
            renderChickenItem(stack, be.getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                    pose, buffer, packedOverlay);
        }
    }

    @EventBusSubscriber(modid = Chickens.MODID, value = Dist.CLIENT)
    public static class Register {
        private Register() {
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.NEST.get(), NestRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.GROWER.get(), GrowerRenderer::new);
        }
    }
}
