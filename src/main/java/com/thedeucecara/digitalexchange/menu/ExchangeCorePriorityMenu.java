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

    // Client constructor (from FriendlyByteBuf)
    public ExchangeCorePriorityMenu(int windowId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(windowId, playerInv, (ExchangeCoreBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(1));
    }

    // Server constructor
    public ExchangeCorePriorityMenu(int windowId, Inventory playerInv, ExchangeCoreBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.PRIORITY_MENU.get(), windowId);
        this.blockEntity = blockEntity;
        this.data = data;
        addDataSlots(data);
    }

    public ExchangeCoreBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    public int getPriority() {
        return this.data.get(0);
    }

    public void setPriority(int priority) {
        this.data.set(0, priority);
        if (this.blockEntity != null) {
            this.blockEntity.setPriority(priority);
        }
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
