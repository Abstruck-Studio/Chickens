package org.abstruck.chickens.client;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.block.breedingbox.BreedingBoxBlockEntity;
import org.abstruck.chickens.block.grower.GrowerBlockEntity;
import org.abstruck.chickens.block.nest.NestBlockEntity;
import org.abstruck.chickens.registry.ModRegistry;

/**
 * 三个设施方块的方块内渲染（原版箱子同款架构）：方块 getRenderShape = ENTITYBLOCK_ANIMATED
 * 跳过默认方块渲染，改由本 BER 统一渲染——方块模型所有面（含内壁）都用方块自身位置的世界光照；
 * 鸡槽物品画实体鸡模型。
 */
public final class ChickenBoxRenderer {
    /** 外层缩放（叠加物品渲染内部的 0.9）：整鸡约 0.72 格高 */
    private static final float SCALE = 0.85F;
    /** 方块内抬升 = 0.5 × SCALE，脚贴方块底 */
    private static final double CHICKEN_Y = 0.5D * SCALE;

    private ChickenBoxRenderer() {
    }

    /** 统一光照渲染方块模型（原版箱子同款：所有面用同一个世界光照） */
    private static void renderBoxModel(BlockState state, PoseStack pose, MultiBufferSource buffer,
                                       int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getModelManager().getBlockModelShaper().getBlockModel(state);
        minecraft.getBlockRenderer().getModelRenderer().renderModel(
                pose.last(), buffer.getBuffer(RenderType.cutoutMipped()), state, model,
                0.0F, 0.0F, 0.0F, packedLight, packedOverlay);
    }

    /** 在方块中心渲染鸡槽物品，鸡面朝方块正面（facing），光照与大世界鸡一致 */
    private static void renderChickenItem(ItemStack stack, Direction facing, PoseStack pose,
                                          MultiBufferSource buffer, int packedLight, int packedOverlay,
                                          net.minecraft.world.level.Level level) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        pose.pushPose();
        pose.translate(0.5D, CHICKEN_Y, 0.5D);
        // fixed 上下文与实体渲染管线各带一次 180° 旋转（互相抵消），
        // 外层补 180 - toYRot 使模型面（初始朝北）最终朝向 facing
        pose.mulPose(new org.joml.Quaternionf().rotationY((float) Math.toRadians(180.0F - facing.toYRot())));
        pose.scale(SCALE, SCALE, SCALE);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight,
                packedOverlay, pose, buffer, level, 0);
        pose.popPose();
    }

    /** 繁殖箱：只渲染方块模型（窗帘变体由 blockstates 模型体现） */
    public static class BreedingBoxRenderer implements BlockEntityRenderer<BreedingBoxBlockEntity> {
        public BreedingBoxRenderer(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(BreedingBoxBlockEntity be, float partialTick, PoseStack pose,
                           MultiBufferSource buffer, int packedLight, int packedOverlay) {
            renderBoxModel(be.getBlockState(), pose, buffer, packedLight, packedOverlay);
        }
    }

    public static class NestRenderer implements BlockEntityRenderer<NestBlockEntity> {
        public NestRenderer(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(NestBlockEntity be, float partialTick, PoseStack pose,
                           MultiBufferSource buffer, int packedLight, int packedOverlay) {
            renderBoxModel(be.getBlockState(), pose, buffer, packedLight, packedOverlay);
            ItemStack stack = be.getItems().getStackInSlot(NestBlockEntity.CHICKEN_IN);
            if (stack.isEmpty()) {
                return;
            }
            renderChickenItem(stack, be.getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                    pose, buffer, packedLight, packedOverlay, be.getLevel());
        }
    }

    public static class GrowerRenderer implements BlockEntityRenderer<GrowerBlockEntity> {
        public GrowerRenderer(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(GrowerBlockEntity be, float partialTick, PoseStack pose,
                           MultiBufferSource buffer, int packedLight, int packedOverlay) {
            renderBoxModel(be.getBlockState(), pose, buffer, packedLight, packedOverlay);
            ItemStack stack = be.getItems().getStackInSlot(GrowerBlockEntity.CHICKEN_IN);
            if (stack.isEmpty()) {
                return;
            }
            renderChickenItem(stack, be.getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                    pose, buffer, packedLight, packedOverlay, be.getLevel());
        }
    }

    /** 方块实体渲染器注册（仅客户端） */
    @Mod.EventBusSubscriber(modid = Chickens.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Register {
        private Register() {
        }

        @SubscribeEvent
        public static void registerBlockEntityRenderers(
                net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModRegistry.BREEDING_BOX_BE.get(), BreedingBoxRenderer::new);
            event.registerBlockEntityRenderer(ModRegistry.NEST_BE.get(), NestRenderer::new);
            event.registerBlockEntityRenderer(ModRegistry.GROWER_BE.get(), GrowerRenderer::new);
        }
    }
}
