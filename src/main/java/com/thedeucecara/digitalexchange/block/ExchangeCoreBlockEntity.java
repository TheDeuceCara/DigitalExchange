package com.thedeucecara.digitalexchange.block;

import com.thedeucecara.digitalexchange.init.ModBlockEntities;
import com.thedeucecara.digitalexchange.integration.ExchangeItemHandler;
import com.thedeucecara.digitalexchange.integration.ae2.BitValueCalculator;
import com.thedeucecara.digitalexchange.integration.ae2.IExchangeCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ExchangeCoreBlockEntity extends BlockEntity implements IExchangeCore, MenuProvider {

    private final ExchangeItemHandler itemHandler;
    private long storedBits = 0L;
    private final List<ItemStack> learnedItems = new ArrayList<>();

    public ExchangeCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXCHANGE_CORE.get(), pos, state);
        this.itemHandler = new ExchangeItemHandler(this);
    }

    public IItemHandler getItemHandler() {
        return this.itemHandler;
    }

    @Override
    public long getStoredBits() {
        return this.storedBits;
    }

    @Override
    public void addBits(long amount) {
        this.storedBits += amount;
        this.saveChanges();
    }

    @Override
    public boolean deductBits(long amount) {
        if (this.storedBits >= amount) {
            this.storedBits -= amount;
            this.saveChanges();
            return true;
        }
        return false;
    }

    @Override
    public void learnItem(ItemStack stack) {
        for (ItemStack existing : this.learnedItems) {
            if (ItemStack.isSameItemSameComponents(existing, stack)) {
                return;
            }
        }
        this.learnedItems.add(stack.copyWithCount(1));
        this.saveChanges();
    }

    @Override
    public List<ItemStack> getLearnedItems() {
        return this.learnedItems;
    }

    @Override
    public long calculateValue(ItemStack stack) {
        return BitValueCalculator.calculateBaseValue(stack);
    }

    public void saveChanges() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.digitalexchange.exchange_core");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("StoredBits", this.storedBits);

        ListTag list = new ListTag();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        for (ItemStack item : this.learnedItems) {
            ItemStack.OPTIONAL_CODEC.encodeStart(ops, item)
                    .resultOrPartial()
                    .ifPresent(list::add);
        }
        tag.put("LearnedItems", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.storedBits = tag.getLong("StoredBits");
        this.learnedItems.clear();

        ListTag list = tag.getList("LearnedItems", Tag.TAG_COMPOUND);
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        for (int i = 0; i < list.size(); i++) {
            ItemStack.OPTIONAL_CODEC.parse(ops, list.get(i))
                    .resultOrPartial()
                    .ifPresent(this.learnedItems::add);
        }
    }
}
