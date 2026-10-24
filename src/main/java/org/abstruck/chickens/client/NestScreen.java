package org.abstruck.chickens.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.block.nest.NestMenu;

/** 鸡窝界面（贴图 175×168）；进度用爱心素材从左往右覆盖灰色进度条 */
public class NestScreen extends AbstractContainerScreen<NestMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Chickens.MODID, "textures/gui/nest.png");
    private static final ResourceLocation HEART_BAR =
            new ResourceLocation(Chickens.MODID, "textures/gui/heart_bar.png");
    private static final int HEART_BAR_W = 26;
    private static final int HEART_BAR_H = 12;
    private static final int BAR_X = 62;
    private static final int BAR_Y = 40;
    private static final int BAR_W = 26;
    private static final int BAR_H = 12;

    public NestScreen(NestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 175;
        this.imageHeight = 168;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /** 只画容器标题，不画原版「物品栏」标签 */
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        int fill = this.menu.getScaledProgress() * BAR_W / 100;
        if (fill > 0) {
            guiGraphics.blit(HEART_BAR, this.leftPos + BAR_X, this.topPos + BAR_Y,
                    0.0F, 0.0F, fill, BAR_H, HEART_BAR_W, HEART_BAR_H);
        }
    }
}
