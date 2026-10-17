package com.thedeucecara.digitalexchange.integration;

import com.thedeucecara.digitalexchange.block.ExchangeCoreBlockEntity;
import com.thedeucecara.digitalexchange.integration.ae2.BitValueCalculator;
import net.minecraft.core.component.DataComponents;
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
        // Expose only learned items as slots. Slot count matches unique blueprints.
        return this.core.getLearnedItems().size();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        List<ItemStack> learned = this.core.getLearnedItems();
        if (slot < 0 || slot >= learned.size()) {
            return ItemStack.EMPTY;
        }

        ItemStack template = learned.get(slot);
        long unitCost = BitValueCalculator.calculateExtractCost(template);
        if (unitCost <= 0) return ItemStack.EMPTY;

        long storedBits = this.core.getStoredBits();

        // Strictly pristine: only show if the player can afford at least 1 full item
        long affordable = storedBits / unitCost;
        if (affordable <= 0) {
            return ItemStack.EMPTY;
        }

        int displayCount = (int) Math.min(affordable, (long) Integer.MAX_VALUE);
        ItemStack pristine = template.copyWithCount(displayCount);
        pristine.remove(DataComponents.UNBREAKABLE);
        if (pristine.isDamageableItem()) {
            pristine.setDamageValue(0);
        }
        return pristine;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        // Container protection check
        if (!BitValueCalculator.isSafeToLearnOrDeposit(stack)) {
            return stack;
        }

        // Calculates fractional bit value based on current durability/fluid contents
        long unitValue = BitValueCalculator.calculateInputValue(stack);
        if (unitValue <= 0) {
            return stack;
        }

        if (!simulate) {
            long totalGain = unitValue * stack.getCount();
            this.core.addBits(totalGain);

            // Learn blueprint as a 100% pristine template
            ItemStack cleanTemplate = BitValueCalculator.createPristineTemplate(stack);
            this.core.learnItem(cleanTemplate);
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
        long fullUnitCost = BitValueCalculator.calculateExtractCost(template);
        if (fullUnitCost <= 0) return ItemStack.EMPTY;

        long currentBits = this.core.getStoredBits();

        // Only allow extraction of full items
        long maxAffordable = currentBits / fullUnitCost;
        long toExtractLong = Math.min((long) amount, maxAffordable);
        int toExtract = (int) Math.min(toExtractLong, (long) Integer.MAX_VALUE);
        if (toExtract <= 0) return ItemStack.EMPTY;

        if (!simulate) {
            this.core.deductBits((long) toExtract * fullUnitCost);
        }

        ItemStack out = template.copyWithCount(toExtract);
        out.remove(DataComponents.UNBREAKABLE);
        if (out.isDamageableItem()) {
            out.setDamageValue(0);
        }
        return out;
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return BitValueCalculator.isSafeToLearnOrDeposit(stack) && BitValueCalculator.calculateInputValue(stack) > 0;
    }
}
