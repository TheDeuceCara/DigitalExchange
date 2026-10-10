package com.thedeucecara.digitalexchange.data;

import com.thedeucecara.digitalexchange.integration.ae2.BitValueCalculator;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public class ExchangeSavedData extends SavedData {

    private static final String DATA_NAME = "digitalexchange_knowledge";
    private long storedBits = 0L;
    private final List<ItemStack> learnedItems = new ArrayList<>();

    public static ExchangeSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new Factory<>(ExchangeSavedData::new, ExchangeSavedData::load),
                DATA_NAME
        );
    }

    public long getStoredBits() {
        return this.storedBits;
    }

    public void addBits(long amount) {
        this.storedBits += amount;
        this.setDirty();
    }

    public boolean deductBits(long amount) {
        if (this.storedBits >= amount) {
            this.storedBits -= amount;
            this.setDirty();
            return true;
        }
        return false;
    }

    /**
     * Sanitizes incoming item templates to 100% pristine durability
     * and guards against saving non-empty inventory containers.
     */
    public void learnItem(ItemStack stack) {
        if (stack.isEmpty() || !BitValueCalculator.isSafeToLearnOrDeposit(stack)) {
            return;
        }

        // Clean template: strips damage so it is stored at 100% pristine condition
        ItemStack pristine = BitValueCalculator.createPristineTemplate(stack);

        for (ItemStack existing : this.learnedItems) {
            if (ItemStack.isSameItemSameComponents(existing, pristine)) {
                return;
            }
        }

        this.learnedItems.add(pristine);
        this.setDirty();
    }

    public List<ItemStack> getLearnedItems() {
        return this.learnedItems;
    }

    public static ExchangeSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ExchangeSavedData data = new ExchangeSavedData();
        data.storedBits = tag.getLong("StoredBits");
        data.learnedItems.clear();

        ListTag list = tag.getList("LearnedItems", Tag.TAG_COMPOUND);
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        for (int i = 0; i < list.size(); i++) {
            ItemStack.OPTIONAL_CODEC.parse(ops, list.get(i))
                    .resultOrPartial()
                    .ifPresent(stack -> {
                        // Ensure legacy/existing saves are canonicalized to pristine templates
                        ItemStack pristine = BitValueCalculator.createPristineTemplate(stack);
                        data.learnedItems.add(pristine);
                    });
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("StoredBits", this.storedBits);

        ListTag list = new ListTag();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        for (ItemStack item : this.learnedItems) {
            ItemStack.OPTIONAL_CODEC.encodeStart(ops, item)
                    .resultOrPartial()
                    .ifPresent(list::add);
        }
        tag.put("LearnedItems", list);
        return tag;
    }
}
