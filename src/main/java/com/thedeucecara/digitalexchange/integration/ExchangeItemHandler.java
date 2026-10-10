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

            // 1. Can afford at least one full item
            if (storedBits >= unitCost) {
                long affordable = storedBits / unitCost;
                int displayCount = (int) Math.min(affordable, (long) Integer.MAX_VALUE);
                return template.copyWithCount(displayCount);
            }

            // 2. Cannot afford full item: check durability & 25% threshold
            if (template.isDamageableItem() && storedBits >= (unitCost / 4L)) {
                // Show 1 item available so the terminal/player can withdraw the worn variant
                return template.copyWithCount(1);
            }

            // 3. Below threshold or non-damageable: do not show in list
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        // Container protection: Reject Shulkers/Backpacks containing items
        // Tanks, Buckets, and Mekanism Gas Canisters pass through safely
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

            // Learn as pristine (100% durability, zero damage) item
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

        // CASE 1: Standard extraction (can afford 1 or more full items)
        if (currentBits >= fullUnitCost) {
            long maxAffordable = currentBits / fullUnitCost;
            long toExtractLong = Math.min((long) amount, maxAffordable);
            int toExtract = (int) Math.min(toExtractLong, (long) Integer.MAX_VALUE);
            if (toExtract <= 0) return ItemStack.EMPTY;

            if (!simulate) {
                this.core.deductBits((long) toExtract * fullUnitCost);
            }

            return template.copyWithCount(toExtract);
        }

        // CASE 2: Fractional durability extraction
        // Only applies to damageable items with at least 25% of the full bit cost
        if (template.isDamageableItem() && currentBits >= (fullUnitCost / 4L)) {
            int maxDurability = template.getMaxDamage();

            // Calculate affordable durability ratio: ratio = currentBits / fullUnitCost
            double ratio = (double) currentBits / (double) fullUnitCost;

            // Damage applied: max - (max * ratio)
            int appliedDamage = (int) Math.max(1, Math.min(maxDurability - 1, Math.floor(maxDurability * (1.0 - ratio))));

            ItemStack wornStack = template.copyWithCount(1);
            wornStack.set(DataComponents.DAMAGE, appliedDamage);

            if (!simulate) {
                // Deduct remaining bits consumed for this worn tool
                this.core.deductBits(currentBits);
            }

            return wornStack;
        }

        return ItemStack.EMPTY;
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
