package com.thedeucecara.digitalexchange.menu;

import com.thedeucecara.digitalexchange.block.ExchangeCoreBlockEntity;
import com.thedeucecara.digitalexchange.init.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class ExchangeCorePriorityMenu extends AbstractContainerMenu {

    private final ExchangeCoreBlockEntity blockEntity;
    private final ContainerData data;

    public ExchangeCorePriorityMenu(int windowId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(windowId, playerInv, (ExchangeCoreBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(1));
    }

    public ExchangeCorePriorityMenu(int windowId, Inventory playerInv, ExchangeCoreBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.PRIORITY_MENU.get(), windowId);
        this.blockEntity = blockEntity;
        this.data = data;
        this.addDataSlots(data);
    }

    public int getPriority() {
        return this.data.get(0);
    }

    /**
     * Handles button clicks sent from the client UI.
     * Minecraft automatically syncs clickMenuButton over the network to the server.
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        int current = this.data.get(0);
        int newPriority = current;

        switch (id) {
            case 0 -> newPriority = current - 100;
            case 1 -> newPriority = current - 10;
            case 2 -> newPriority = current - 1;
            case 3 -> newPriority = current + 1;
            case 4 -> newPriority = current + 10;
            case 5 -> newPriority = current + 100;
            default -> {
                return false;
            }
        }

        this.data.set(0, newPriority);
        if (this.blockEntity != null) {
            this.blockEntity.setPriority(newPriority);
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.blockEntity != null && !this.blockEntity.isRemoved();
    }
}
