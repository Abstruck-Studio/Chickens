package org.abstruck.chickens.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.block.nest.NestMenu;

/**
 * 鸡窝界面（用户绘制 UI，贴图已裁剪为 175×168）。
 * 进度：GUI 内的灰色进度条 (62,41)-(87,51) 为槽位背景，
 * 进度推进时把爱心素材从左往右覆盖上去。
 */
public class NestScreen extends AbstractContainerScreen<NestMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Chickens.MODID, "textures/gui/nest.png");
    /** 爱心覆盖素材（26×12，裁自用户原图 (176,0)-(201,11)） */
    private static final ResourceLocation HEART_BAR =
            ResourceLocation.fromNamespaceAndPath(Chickens.MODID, "textures/gui/heart_bar.png");
    private static final int HEART_BAR_W = 26;
    private static final int HEART_BAR_H = 12;
    /** 灰色进度条在 GUI 内的位置（标记.md：62,41 ~ 87,51；用户微调上移 1px） */
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
