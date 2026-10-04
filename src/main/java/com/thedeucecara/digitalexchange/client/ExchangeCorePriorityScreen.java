package com.thedeucecara.digitalexchange.client;

import com.thedeucecara.digitalexchange.menu.ExchangeCorePriorityMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ExchangeCorePriorityScreen extends AbstractContainerScreen<ExchangeCorePriorityMenu> {

    private EditBox priorityInput;

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

        this.priorityInput = new EditBox(this.font, x + 38, y + 40, 100, 20, Component.literal("Priority"));
        this.priorityInput.setValue(String.valueOf(this.menu.getPriority()));
        this.addRenderableWidget(this.priorityInput);

        this.addRenderableWidget(Button.builder(Component.literal("-10"), btn -> modifyPriority(-10))
                .bounds(x + 10, y + 40, 24, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+10"), btn -> modifyPriority(10))
                .bounds(x + 142, y + 40, 24, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Save"), btn -> savePriority())
                .bounds(x + 58, y + 70, 60, 20).build());
    }

    private void modifyPriority(int delta) {
        try {
            int current = Integer.parseInt(this.priorityInput.getValue());
            this.priorityInput.setValue(String.valueOf(current + delta));
        } catch (NumberFormatException ignored) {}
    }

    private void savePriority() {
        try {
            int val = Integer.parseInt(this.priorityInput.getValue());
            this.menu.setPriority(val);
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.closeContainer();
            }
        } catch (NumberFormatException ignored) {}
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
        graphics.drawString(this.font, this.title, 12, 12, 0xFFFFFF, false);
        graphics.drawString(this.font, "Network Storage Priority", 12, 26, 0x888888, false);
    }
}
