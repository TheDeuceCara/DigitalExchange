package com.thedeucecara.digitalexchange.integration.ae2;

import net.minecraft.world.item.ItemStack;
import java.util.List;

public interface IExchangeCore {
    long getStoredBits();
    void addBits(long amount);
    boolean deductBits(long amount);
    void learnItem(ItemStack stack);
    List<ItemStack> getLearnedItems();
    long calculateValue(ItemStack stack);
}
