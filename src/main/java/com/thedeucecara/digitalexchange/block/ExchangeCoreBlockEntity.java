package com.thedeucecara.digitalexchange.block;

import com.thedeucecara.digitalexchange.data.ExchangeSavedData;
import com.thedeucecara.digitalexchange.init.ModBlockEntities;
import com.thedeucecara.digitalexchange.integration.ExchangeItemHandler;
import com.thedeucecara.digitalexchange.integration.ae2.BitValueCalculator;
import com.thedeucecara.digitalexchange.integration.ae2.IExchangeCore;
import com.thedeucecara.digitalexchange.menu.ExchangeCorePriorityMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ExchangeCoreBlockEntity extends BlockEntity implements IExchangeCore, MenuProvider {

    private final ExchangeItemHandler itemHandler;
    private int priority = 0;

    protected final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? priority : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                priority = value;
                saveChanges();
            }
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public ExchangeCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXCHANGE_CORE.get(), pos, state);
        this.itemHandler = new ExchangeItemHandler(this);
    }

    public IItemHandler getItemHandler() {
        return this.itemHandler;
    }

    private ExchangeSavedData getData() {
        if (this.level instanceof ServerLevel serverLevel) {
            return ExchangeSavedData.get(serverLevel);
        }
        return null;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
        this.saveChanges();
    }

    @Override
    public long getStoredBits() {
        ExchangeSavedData data = getData();
        return data != null ? data.getStoredBits() : 0L;
    }

    @Override
    public void addBits(long amount) {
        ExchangeSavedData data = getData();
        if (data != null) {
            data.addBits(amount);
            this.saveChanges();
        }
    }

    @Override
    public boolean deductBits(long amount) {
        ExchangeSavedData data = getData();
        if (data != null && data.deductBits(amount)) {
            this.saveChanges();
            return true;
        }
        return false;
    }

    @Override
    public void learnItem(ItemStack stack) {
        ExchangeSavedData data = getData();
        if (data != null && !stack.isEmpty()) {
            // Guarantee item is registered as a pristine template (100% durability, zero damage)
            ItemStack cleanTemplate = BitValueCalculator.createPristineTemplate(stack);
            data.learnItem(cleanTemplate);
            this.saveChanges();
        }
    }

    @Override
    public List<ItemStack> getLearnedItems() {
        ExchangeSavedData data = getData();
        return data != null ? data.getLearnedItems() : new ArrayList<>();
    }

    @Override
    public long calculateValue(ItemStack stack) {
        // Evaluates full pristine value for baseline extraction cost queries
        return BitValueCalculator.calculatePristineBaseValue(stack);
    }

    public void saveChanges() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public void openMenu(ServerPlayer player) {
        player.openMenu(this, buf -> buf.writeBlockPos(this.worldPosition));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.digitalexchange.exchange_core");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new ExchangeCorePriorityMenu(windowId, playerInventory, this, this.containerData);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Priority", this.priority);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.priority = tag.getInt("Priority");
    }
}
