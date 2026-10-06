package org.abstruck.chickens.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.block.breedingbox.BreedingBoxMenu;

/** 繁殖箱界面（贴图 175×165）；进度用爱心素材从左往右覆盖灰色进度条 */
public class BreedingBoxScreen extends AbstractContainerScreen<BreedingBoxMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Chickens.MODID, "textures/gui/breeding_box.png");
    /** 爱心覆盖素材（26×12） */
    private static final ResourceLocation HEART_BAR =
            ResourceLocation.fromNamespaceAndPath(Chickens.MODID, "textures/gui/heart_bar.png");
    private static final int HEART_BAR_W = 26;
    private static final int HEART_BAR_H = 12;
    /** 灰色进度条位置；BAR_W 必须 = HEART_BAR_W 保持 1:1 覆盖（写大会拉伸素材） */
    private static final int BAR_X = 84;
    private static final int BAR_Y = 40;
    private static final int BAR_W = HEART_BAR_W;
    private static final int BAR_H = 12;

    public BreedingBoxScreen(BreedingBoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 175;
        this.imageHeight = 165;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        int fill = this.menu.getScaledProgress() * BAR_W / 100;
        if (fill > 0) {
            int barX = this.leftPos + BAR_X;
            int barY = this.topPos + BAR_Y;
            graphics.enableScissor(barX, barY, barX + fill, barY + BAR_H);
            graphics.blit(HEART_BAR, barX, barY, 0, 0, BAR_W, BAR_H, HEART_BAR_W, HEART_BAR_H);
            graphics.disableScissor();
        }
    }
}
