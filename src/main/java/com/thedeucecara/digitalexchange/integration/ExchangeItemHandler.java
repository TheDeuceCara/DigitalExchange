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
        return this.core.getLearnedItems().size() + 1;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        List<ItemStack> learned = this.core.getLearnedItems();
        if (slot >= 0 && slot < learned.size()) {
            ItemStack template = learned.get(slot);
            long unitCost = BitValueCalculator.calculateExtractCost(template);
            if (unitCost <= 0) return ItemStack.EMPTY;

            long affordable = this.core.getStoredBits() / unitCost;
            if (affordable <= 0) return ItemStack.EMPTY;

            // Clamp total affordable items to Integer.MAX_VALUE rather than 64
            int displayCount = (int) Math.min(affordable, (long) Integer.MAX_VALUE);
            return template.copyWithCount(displayCount);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        long unitValue = BitValueCalculator.calculateInputValue(stack);
        if (unitValue <= 0) {
            return stack;
        }

        if (!simulate) {
            long totalGain = unitValue * stack.getCount();
            this.core.addBits(totalGain);
            this.core.learnItem(stack);
        }

        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        List<ItemStack> learned = this.core.getLearnedItems();
        if (slot < 0 || slot >= learned.size() || amount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack template = learned.get(slot);
        long unitCost = BitValueCalculator.calculateExtractCost(template);
        if (unitCost <= 0) return ItemStack.EMPTY;

        long maxAffordable = this.core.getStoredBits() / unitCost;
        long toExtractLong = Math.min((long) amount, maxAffordable);
        int toExtract = (int) Math.min(toExtractLong, (long) Integer.MAX_VALUE);
        if (toExtract <= 0) return ItemStack.EMPTY;

        if (!simulate) {
            this.core.deductBits(toExtract * unitCost);
        }

        return template.copyWithCount(toExtract);
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return BitValueCalculator.calculateInputValue(stack) > 0;
    }
}
