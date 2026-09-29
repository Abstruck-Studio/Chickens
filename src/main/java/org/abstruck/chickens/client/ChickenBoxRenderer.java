package org.abstruck.chickens.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.block.breedingbox.BreedingBoxBlockEntity;
import org.abstruck.chickens.block.grower.GrowerBlockEntity;
import org.abstruck.chickens.block.nest.NestBlockEntity;
import org.abstruck.chickens.registry.ModBlockEntities;

/**
 * 三个设施方块的方块内渲染（原版箱子同款架构）：
 * 方块 getRenderShape = ENTITYBLOCK_ANIMATED 跳过默认方块模型渲染，改由本 BER 统一渲染：
 * <ul>
 *   <li>方块模型用 ModelBlockRenderer.renderModel 的统一光照版本——所有面（含内壁）
 *       都用方块自身位置的世界光照。默认方块渲染逐面取【法线方向邻格】的光照，
 *       围栏式模型的内壁面会采样到对侧邻格——贴实体方块（如草方块挡住火把光）时
 *       对侧内壁全黑，不符合直觉</li>
 *   <li>鸡槽物品按品种/幼年组件画实体鸡模型（与背包图标同一渲染管线），
 *       光照同样用世界光照（与大世界里的鸡一致）</li>
 * </ul>
 */
public final class ChickenBoxRenderer {
    /** 外层缩放（叠加物品渲染内部 0.9）：0.85 → 整鸡约 0.72 格高 */
    private static final float SCALE = 0.85F;
    /** 方块内抬升 = 0.5 × SCALE，脚贴方块底（用户可进游戏微调） */
    private static final double CHICKEN_Y = 0.5D * SCALE;

    private ChickenBoxRenderer() {
    }

    /** 统一光照渲染方块模型（原版箱子同款：所有面用同一个世界光照，无逐面邻格采样） */
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
                                          MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        pose.pushPose();
        pose.translate(0.5D, CHICKEN_Y, 0.5D);
        // fixed 上下文与实体渲染管线各带一次 180° 旋转（互相抵消），
        // 外层补 180 - toYRot 使模型面（初始朝北）最终朝向 facing
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        pose.scale(SCALE, SCALE, SCALE);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight,
                packedOverlay, pose, buffer, Minecraft.getInstance().level, 0);
        pose.popPose();
    }

    /** 繁殖箱：只渲染方块模型（无种子/繁殖中的窗帘变体由 blockstates 模型体现） */
    public static class BreedingBoxRenderer implements BlockEntityRenderer<BreedingBoxBlockEntity> {
        public BreedingBoxRenderer(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(BreedingBoxBlockEntity be, float partialTick, PoseStack pose,
                           MultiBufferSource buffer, int packedLight, int packedOverlay) {
            renderBoxModel(be.getBlockState(), pose, buffer, packedLight, packedOverlay);
        }
    }

    /** 鸡窝：方块模型 + 鸡槽中的成年鸡物品 */
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
                    pose, buffer, packedLight, packedOverlay);
        }
    }

    /** 培育箱：方块模型 + 鸡槽中的幼年鸡物品 */
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
                    pose, buffer, packedLight, packedOverlay);
        }
    }

    @EventBusSubscriber(modid = Chickens.MODID, value = Dist.CLIENT)
    public static class Register {
        private Register() {
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.BREEDING_BOX.get(), BreedingBoxRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.NEST.get(), NestRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.GROWER.get(), GrowerRenderer::new);
        }
    }
}
