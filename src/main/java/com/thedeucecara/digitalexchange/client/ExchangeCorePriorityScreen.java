package com.thedeucecara.digitalexchange.client;

import com.thedeucecara.digitalexchange.menu.ExchangeCorePriorityMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ExchangeCorePriorityScreen extends AbstractContainerScreen<ExchangeCorePriorityMenu> {

    public ExchangeCorePriorityScreen(ExchangeCorePriorityMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 110;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Minus buttons (ids: 0, 1, 2)
        this.addRenderableWidget(Button.builder(Component.literal("-100"), btn -> sendPriorityChange(0))
                .bounds(x + 10, y + 40, 32, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("-10"), btn -> sendPriorityChange(1))
                .bounds(x + 44, y + 40, 26, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("-1"), btn -> sendPriorityChange(2))
                .bounds(x + 72, y + 40, 20, 20).build());

        // Plus buttons (ids: 3, 4, 5)
        this.addRenderableWidget(Button.builder(Component.literal("+1"), btn -> sendPriorityChange(3))
                .bounds(x + 94, y + 40, 20, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+10"), btn -> sendPriorityChange(4))
                .bounds(x + 116, y + 40, 26, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+100"), btn -> sendPriorityChange(5))
                .bounds(x + 144, y + 40, 32, 20).build());

        // Close / Done button
        this.addRenderableWidget(Button.builder(Component.literal("Done"), btn -> {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.closeContainer();
            }
        }).bounds(x + 58, y + 75, 60, 20).build());
    }

    private void sendPriorityChange(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF2B2D30);
        graphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + this.imageHeight - 2, 0xFF1E1F22);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 12, 10, 0xFFFFFF, false);
        graphics.drawString(this.font, "Network Priority: " + this.menu.getPriority(), 12, 24, 0x00FFCC, false);
    }
}
