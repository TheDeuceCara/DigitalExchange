package com.thedeucecara.digitalexchange.block;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.storage.MEStorage;
import com.thedeucecara.digitalexchange.init.ModBlockEntities;
import com.thedeucecara.digitalexchange.integration.ae2.BitValueCalculator;
import com.thedeucecara.digitalexchange.integration.ae2.ExchangeMEInventory;
import com.thedeucecara.digitalexchange.integration.ae2.IExchangeCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class ExchangeCoreBlockEntity extends BlockEntity implements IInWorldGridNodeHost, IExchangeCore {

    private final IManagedGridNode mainNode;
    private final ExchangeMEInventory inventory;
    private long storedBits = 0L;
    private final List<ItemStack> learnedItems = new ArrayList<>();

    public ExchangeCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXCHANGE_CORE.get(), pos, state);
        this.inventory = new ExchangeMEInventory(this);
        this.mainNode = GridHelper.createManagedNode(this, new ExchangeGridListener())
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(MEStorage.class, this.inventory);
    }

    @Override
    public IGridNode getGridNode(Direction dir) {
        return this.mainNode.getNode();
    }

    @Override
    public long getStoredBits() {
        return this.storedBits;
    }

    @Override
    public void addBits(long amount) {
        this.storedBits += amount;
        setChanged();
    }

    @Override
    public boolean deductBits(long amount) {
        if (this.storedBits >= amount) {
            this.storedBits -= amount;
            setChanged();
            return true;
        }
        return false;
    }

    @Override
    public void learnItem(ItemStack stack) {
        for (ItemStack existing : learnedItems) {
            if (ItemStack.isSameItemSameComponents(existing, stack)) {
                return;
            }
        }
        learnedItems.add(stack.copyWithCount(1));
        setChanged();
    }

    @Override
    public List<ItemStack> getLearnedItems() {
        return this.learnedItems;
    }

    @Override
    public long calculateValue(ItemStack stack) {
        return BitValueCalculator.calculate(stack);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("StoredBits", this.storedBits);

        ListTag list = new ListTag();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);

        for (ItemStack item : this.learnedItems) {
            ItemStack.OPTIONAL_CODEC.encodeStart(ops, item)
                    .resultOrPartial()
                    .ifPresent(list::add);
        }
        tag.put("LearnedItems", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.storedBits = tag.getLong("StoredBits");
        this.learnedItems.clear();

        ListTag list = tag.getList("LearnedItems", Tag.TAG_COMPOUND);
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);

        for (int i = 0; i < list.size(); i++) {
            ItemStack.OPTIONAL_CODEC.parse(ops, list.get(i))
                    .resultOrPartial()
                    .ifPresent(this.learnedItems::add);
        }
    }

    private static class ExchangeGridListener implements appeng.api.networking.IGridNodeListener<ExchangeCoreBlockEntity> {
        @Override
        public void onSecurityBreak(ExchangeCoreBlockEntity nodeOwner) {}
        @Override
        public void onSaveChanges(ExchangeCoreBlockEntity nodeOwner) {
            nodeOwner.setChanged();
        }
    }
}
