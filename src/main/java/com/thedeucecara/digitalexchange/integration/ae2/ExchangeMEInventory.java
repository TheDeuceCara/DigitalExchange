package com.thedeucecara.digitalexchange.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ExchangeMEInventory implements MEStorage {

    private final IExchangeCore core;

    public ExchangeMEInventory(IExchangeCore core) {
        this.core = core;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        long currentBits = this.core.getStoredBits();

        for (ItemStack learned : this.core.getLearnedItems()) {
            long unitCost = BitValueCalculator.calculateExtractCost(learned);
            if (unitCost <= 0) continue;

            // 1. Can afford at least one full pristine item
            long affordable = currentBits / unitCost;
            if (affordable > 0) {
                out.add(AEItemKey.of(learned), affordable);
                continue;
            }

            // 2. Cannot afford full item: check 25% wear threshold for damageable tools
            if (learned.isDamageableItem() && currentBits >= (unitCost / 4L)) {
                // Advertise 1 available so the terminal displays it and allows extraction
                out.add(AEItemKey.of(learned), 1L);
            }
        }
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (!(what instanceof AEItemKey itemKey) || amount <= 0) return 0;

        ItemStack template = itemKey.toStack();
        long unitCost = BitValueCalculator.calculateExtractCost(template);
        if (unitCost <= 0) return 0;

        long currentBits = this.core.getStoredBits();

        // CASE 1: Standard full-cost extraction
        if (currentBits >= unitCost) {
            long maxExtractable = Math.min(amount, currentBits / unitCost);
            if (maxExtractable <= 0) return 0;

            if (mode == Actionable.MODULATE) {
                this.core.deductBits(maxExtractable * unitCost);
            }
            return maxExtractable;
        }

        // CASE 2: Fractional durability extraction (single item request)
        if (amount == 1 && template.isDamageableItem() && currentBits >= (unitCost / 4L)) {
            if (mode == Actionable.MODULATE) {
                this.core.deductBits(currentBits);
            }
            return 1;
        }

        return 0;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (!(what instanceof AEItemKey itemKey) || amount <= 0) return 0;

        ItemStack stack = itemKey.toStack((int) Math.min(amount, 64));

        // Container safeguard: Reject non-empty item inventories (Shulkers, Backpacks)
        if (!BitValueCalculator.isSafeToLearnOrDeposit(stack)) {
            return 0;
        }

        long unitGain = BitValueCalculator.calculateInputValue(stack);
        if (unitGain <= 0) return 0;

        if (mode == Actionable.MODULATE) {
            this.core.addBits(unitGain * amount);

            // Learn as pristine template (100% durability, zero damage)
            ItemStack cleanTemplate = BitValueCalculator.createPristineTemplate(stack);
            this.core.learnItem(cleanTemplate);
        }
        return amount;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("block.digitalexchange.exchange_core");
    }
}
