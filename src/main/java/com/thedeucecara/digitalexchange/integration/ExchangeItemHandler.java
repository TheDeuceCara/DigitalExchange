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
        return this.core.getLearnedItems().size() + 1;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        List<ItemStack> learned = this.core.getLearnedItems();
        if (slot >= 0 && slot < learned.size()) {
            ItemStack template = learned.get(slot);
            long unitCost = BitValueCalculator.calculateExtractCost(template);
            if (unitCost <= 0) return ItemStack.EMPTY;

            long storedBits = this.core.getStoredBits();

            // 1. Can afford at least one full pristine item
            if (storedBits >= unitCost) {
                long affordable = storedBits / unitCost;
                int displayCount = (int) Math.min(affordable, (long) Integer.MAX_VALUE);
                ItemStack out = template.copyWithCount(displayCount);
                out.remove(DataComponents.UNBREAKABLE);
                out.setDamageValue(0); // Explicitly pristine
                return out;
            }

            // 2. Fractional durability (25% to 99% cost)
            long minWearThreshold = Math.max(1L, (long) Math.ceil(unitCost * 0.25));
            if (template.isDamageableItem() && storedBits >= minWearThreshold) {
                // Return the EXACT worn stack so AE2 creates an AEItemKey that matches the extracted stack!
                return createWornStack(template, storedBits, unitCost);
            }

            // 3. Below 25% or non-damageable
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        if (!BitValueCalculator.isSafeToLearnOrDeposit(stack)) {
            return stack;
        }

        long unitValue = BitValueCalculator.calculateInputValue(stack);
        if (unitValue <= 0) {
            return stack;
        }

        if (!simulate) {
            long totalGain = unitValue * stack.getCount();
            this.core.addBits(totalGain);

            ItemStack cleanTemplate = BitValueCalculator.createPristineTemplate(stack);
            cleanTemplate.remove(DataComponents.UNBREAKABLE);
            cleanTemplate.setDamageValue(0);
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

        // CASE 1: Standard pristine extraction
        if (currentBits >= fullUnitCost) {
            long maxAffordable = currentBits / fullUnitCost;
            long toExtractLong = Math.min((long) amount, maxAffordable);
            int toExtract = (int) Math.min(toExtractLong, (long) Integer.MAX_VALUE);
            if (toExtract <= 0) return ItemStack.EMPTY;

            if (!simulate) {
                this.core.deductBits((long) toExtract * fullUnitCost);
            }

            ItemStack out = template.copyWithCount(toExtract);
            out.remove(DataComponents.UNBREAKABLE);
            out.setDamageValue(0);
            return out;
        }

        // CASE 2: Fractional durability extraction (25% to 99% cost)
        long minWearThreshold = Math.max(1L, (long) Math.ceil(fullUnitCost * 0.25));
        if (template.isDamageableItem() && currentBits >= minWearThreshold) {
            ItemStack wornStack = createWornStack(template, currentBits, fullUnitCost);

            int maxDurability = wornStack.getMaxDamage();
            int currentDamage = wornStack.getDamageValue();
            double remainingRatio = (double) (maxDurability - currentDamage) / (double) maxDurability;
            long bitsConsumed = Math.max(1L, (long) Math.ceil(fullUnitCost * remainingRatio));
            bitsConsumed = Math.min(currentBits, bitsConsumed);

            if (!simulate) {
                this.core.deductBits(bitsConsumed);
            }

            return wornStack;
        }

        return ItemStack.EMPTY;
    }

    private ItemStack createWornStack(ItemStack template, long bits, long fullCost) {
        ItemStack stack = template.copyWithCount(1);
        stack.remove(DataComponents.UNBREAKABLE);

        int maxDurability = stack.getMaxDamage();
        if (maxDurability <= 0) return stack;

        double bitRatio = Math.min(0.99, (double) bits / (double) fullCost);
        int targetDamage = (int) Math.floor(maxDurability * (1.0 - bitRatio));
        targetDamage = Math.max(1, Math.min(maxDurability - 1, targetDamage));

        stack.setDamageValue(targetDamage);
        return stack;
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
