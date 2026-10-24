package org.abstruck.chickens.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.item.DyeEggColors;
import org.abstruck.chickens.item.ItemData;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 染料蛋模型：把原版蛋模型的 sprite 换成 16 张「按染料调色板生成」的静态贴图
 * （见 tools/gen_dye_egg_textures.py），由物品 NBT 的 {@code Color} 决定用哪张，不再乘色。
 * 颜色无法识别时回退到白色蛋贴图。
 */
public final class DyeEggBakedModel implements BakedModel {
    private final BakedModel delegate;
    private final Map<DyeColor, BakedModel> variants;
    private final BakedModel fallback;
    private final ItemOverrides overrides = new ColorOverrides(this);

    private DyeEggBakedModel(BakedModel delegate, Map<DyeColor, BakedModel> variants, BakedModel fallback) {
        this.delegate = delegate;
        this.variants = variants;
        this.fallback = fallback;
    }

    /** 烘焙完成时调用：为 16 种颜色各生成一份换过贴图的模型（1.19.2 此时图集已上传，可直接取 sprite） */
    public static BakedModel create(BakedModel delegate) {
        // 平面物品模型的面存放在 null 方向（ItemRenderer 最后一步就是取 null 方向的面）
        List<BakedQuad> baseQuads = delegate.getQuads(null, null, RandomSource.create(42L));
        if (baseQuads.isEmpty()) {
            return delegate;
        }
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        Map<DyeColor, BakedModel> variants = new EnumMap<>(DyeColor.class);
        for (DyeColor dye : DyeColor.values()) {
            TextureAtlasSprite sprite = atlas.apply(new ResourceLocation(Chickens.MODID,
                    "item/dye_egg/" + dye.getName()));
            // 贴图缺失时退回原版蛋模型，避免整个物品变成粉黑格
            if (sprite == null || sprite.getName().equals(MissingTextureAtlasSprite.getLocation())) {
                return delegate;
            }
            List<BakedQuad> quads = new ArrayList<>(baseQuads.size());
            for (BakedQuad quad : baseQuads) {
                quads.add(retexture(quad, sprite));
            }
            variants.put(dye, new Variant(delegate, List.copyOf(quads), sprite));
        }
        return new DyeEggBakedModel(delegate, variants, variants.get(DyeColor.WHITE));
    }

    /**
     * 把 quad 的贴图换成目标 sprite：面的 UV 在烘焙时已换算成整张图集的坐标，
     * 所以必须先反算回 sprite 内的 0~1 局部坐标，再用新 sprite 重新映射。
     */
    private static BakedQuad retexture(BakedQuad quad, TextureAtlasSprite sprite) {
        TextureAtlasSprite from = quad.getSprite();
        int[] vertices = quad.getVertices().clone();
        for (int i = 0; i < vertices.length; i += 8) {
            float localU = from.getUOffset(Float.intBitsToFloat(vertices[i + 4]));
            float localV = from.getVOffset(Float.intBitsToFloat(vertices[i + 5]));
            vertices[i + 4] = Float.floatToRawIntBits(sprite.getU(localU));
            vertices[i + 5] = Float.floatToRawIntBits(sprite.getV(localV));
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(),
                sprite, quad.isShade(), quad.hasAmbientOcclusion());
    }

    private BakedModel variantFor(ItemStack stack) {
        int color = ItemData.getColor(stack);
        if (color >= 0) {
            DyeColor dye = DyeEggColors.dyeOf(color);
            BakedModel model = dye != null ? this.variants.get(dye) : null;
            if (model != null) {
                return model;
            }
        }
        return this.fallback;
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
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.fallback.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return this.delegate.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.overrides;
    }

    @Override
    public BakedModel applyTransform(ItemTransforms.TransformType context, PoseStack poseStack,
                                     boolean applyLeftHandTransform) {
        this.delegate.applyTransform(context, poseStack, applyLeftHandTransform);
        return this;
    }

    /** 物品颜色 NBT → 对应颜色的模型 */
    private static final class ColorOverrides extends ItemOverrides {
        private final DyeEggBakedModel owner;

        private ColorOverrides(DyeEggBakedModel owner) {
            this.owner = owner;
        }

        @Override
        public BakedModel resolve(BakedModel originalModel, ItemStack stack, ClientLevel level,
                                  LivingEntity entity, int seed) {
            return this.owner.variantFor(stack);
        }
    }

    /** 单一颜色的蛋模型：面、粒子图标都换成该颜色的 sprite，其余委托原模型 */
    private static final class Variant implements BakedModel {
        private final BakedModel delegate;
        private final List<BakedQuad> quads;
        private final TextureAtlasSprite sprite;

        private Variant(BakedModel delegate, List<BakedQuad> quads, TextureAtlasSprite sprite) {
            this.delegate = delegate;
            this.quads = quads;
            this.sprite = sprite;
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            return state == null && side == null ? this.quads : List.of();
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
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return this.sprite;
        }

        @Override
        public ItemTransforms getTransforms() {
            return this.delegate.getTransforms();
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }

        @Override
        public BakedModel applyTransform(ItemTransforms.TransformType context, PoseStack poseStack,
                                         boolean applyLeftHandTransform) {
            this.delegate.applyTransform(context, poseStack, applyLeftHandTransform);
            return this;
        }
    }
}
