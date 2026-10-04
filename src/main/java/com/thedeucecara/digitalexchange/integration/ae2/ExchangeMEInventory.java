package com.thedeucecara.digitalexchange.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ExchangeMEInventory implements MEStorage {

    private final IExchangeCore core;

    public ExchangeMEInventory(IExchangeCore core) {
        this.core = core;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        long currentBits = core.getStoredBits();

        for (ItemStack learned : core.getLearnedItems()) {
            long unitCost = core.calculateValue(learned);
            if (unitCost <= 0) continue;

            // Compute affordable count from bits balance
            long affordable = currentBits / unitCost;
            
            // If the player has bits, show exact affordable amount.
            // If bits are 0, we advertise 0 or don't register so phantom pulls don't occur.
            if (affordable > 0) {
                out.add(AEItemKey.of(learned), affordable);
            }
        }
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (!(what instanceof AEItemKey itemKey)) return 0;

        ItemStack stack = itemKey.toStack();
        long unitCost = core.calculateValue(stack);
        if (unitCost <= 0) return 0;

        long currentBits = core.getStoredBits();
        long maxExtractable = Math.min(amount, currentBits / unitCost);
        if (maxExtractable <= 0) return 0;

        if (mode == Actionable.MODULATE) {
            core.deductBits(maxExtractable * unitCost);
        }
        return maxExtractable;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (!(what instanceof AEItemKey itemKey)) return 0;

        ItemStack stack = itemKey.toStack((int) Math.min(amount, 64));
        long unitCost = core.calculateValue(stack);
        if (unitCost <= 0) return 0;

        if (mode == Actionable.MODULATE) {
            core.addBits(unitCost * amount);
            core.learnItem(stack);
        }
        return amount;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("block.digitalexchange.exchange_core");
    }
}
