package com.thedeucecara.digitalexchange.integration;

import com.thedeucecara.digitalexchange.block.ExchangeCoreBlockEntity;
import com.thedeucecara.digitalexchange.integration.ae2.BitValueCalculator;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ExchangeItemHandler implements IItemHandler {

    private final ExchangeCoreBlockEntity core;

    public ExchangeItemHandler(ExchangeCoreBlockEntity core) {
        this.core = core;
    }

    @Override
    public int getSlots() {
        // Slot 0 to N-1: All learned items available for extraction
        // Slot N: A dedicated insertion slot for depositing items to earn Bits
        return core.getLearnedItems().size() + 1;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        List<ItemStack> learned = core.getLearnedItems();
        if (slot >= 0 && slot < learned.size()) {
            ItemStack template = learned.get(slot);
            long unitCost = BitValueCalculator.calculateExtractCost(template);
            if (unitCost <= 0) return ItemStack.EMPTY;

            long affordable = core.getStoredBits() / unitCost;
            if (affordable <= 0) return ItemStack.EMPTY;

            int count = (int) Math.min(affordable, template.getMaxStackSize());
            return template.copyWithCount(count);
        }
        return ItemStack.EMPTY; // Insertion slot appears empty
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        long unitValue = BitValueCalculator.calculateInputValue(stack);
        if (unitValue <= 0) {
            // Item has no value or cannot be dissolved; reject insertion
            return stack;
        }

        if (!simulate) {
            long totalGain = unitValue * stack.getCount();
            core.addBits(totalGain);
            core.learnItem(stack); // Automatically learns new items on deposit
        }

        return ItemStack.EMPTY; // Fully accepted and converted to Bits
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        List<ItemStack> learned = core.getLearnedItems();
        if (slot < 0 || slot >= learned.size() || amount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack template = learned.get(slot);
        long unitCost = BitValueCalculator.calculateExtractCost(template);
        if (unitCost <= 0) return ItemStack.EMPTY;

        long maxAffordable = core.getStoredBits() / unitCost;
        int toExtract = (int) Math.min(amount, Math.min(maxAffordable, template.getMaxStackSize()));
        if (toExtract <= 0) return ItemStack.EMPTY;

        if (!simulate) {
            core.deductBits(toExtract * unitCost);
        }

        return template.copyWithCount(toExtract);
    }

    @Override
    public int getSlotLimit(int slot) {
        List<ItemStack> learned = core.getLearnedItems();
        if (slot >= 0 && slot < learned.size()) {
            return learned.get(slot).getMaxStackSize();
        }
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        // Any item with a positive bit value can be inserted
        return BitValueCalculator.calculateInputValue(stack) > 0;
    }
}
