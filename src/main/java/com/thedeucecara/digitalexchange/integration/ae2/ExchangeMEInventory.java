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
            long unitCost = BitValueCalculator.calculateExtractCost(learned);
            if (unitCost <= 0) continue;

            long affordable = currentBits / unitCost;
            if (affordable > 0) {
                out.add(AEItemKey.of(learned), affordable);
            }
        }
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (!(what instanceof AEItemKey itemKey)) return 0;

        ItemStack stack = itemKey.toStack();
        long unitCost = BitValueCalculator.calculateExtractCost(stack);
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
        long unitGain = BitValueCalculator.calculateInputValue(stack);
        if (unitGain <= 0) return 0;

        if (mode == Actionable.MODULATE) {
            core.addBits(unitGain * amount);
            core.learnItem(stack);
        }
        return amount;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("block.digitalexchange.exchange_core");
    }
}
