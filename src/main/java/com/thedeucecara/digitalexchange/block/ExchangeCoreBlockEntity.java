package com.thedeucecara.digitalexchange.block;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.storage.IStorageMounts;
import appeng.api.storage.IStorageProvider;
import appeng.blockentity.grid.AENetworkBlockEntity;
import com.thedeucecara.digitalexchange.init.ModBlockEntities;
import com.thedeucecara.digitalexchange.integration.ae2.BitValueCalculator;
import com.thedeucecara.digitalexchange.integration.ae2.ExchangeMEInventory;
import com.thedeucecara.digitalexchange.integration.ae2.IExchangeCore;
import com.thedeucecara.digitalexchange.menu.ExchangeCorePriorityMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class ExchangeCoreBlockEntity extends AENetworkBlockEntity implements 
        IExchangeCore, 
        IStorageProvider, 
        MenuProvider {

    private final ExchangeMEInventory inventory;
    private long storedBits = 0L;
    private int priority = 0;
    private final List<ItemStack> learnedItems = new ArrayList<>();

    protected final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? priority : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                priority = value;
                saveChanges();
                notifyGridOfStorageChange();
            }
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public ExchangeCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXCHANGE_CORE.get(), pos, state);
        this.inventory = new ExchangeMEInventory(this);

        // AENetworkBlockEntity automatically manages the mainNode lifecycle!
        this.getMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setExposedOnSides(EnumSet.allOf(Direction.class))
                .setIdlePowerUsage(1.0)
                .addService(IStorageProvider.class, this);
    }

    @Override
    public void mountInventories(IStorageMounts mounts) {
        mounts.mount(this.inventory, this.priority);
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
        this.saveChanges();
        this.notifyGridOfStorageChange();
    }

    public void notifyGridOfStorageChange() {
        if (this.getMainNode().isReady()) {
            var grid = this.getMainNode().getGrid();
            if (grid != null) {
                grid.getStorageService().refreshGlobalStorageProvider(this);
            }
        }
    }

    @Override
    public long getStoredBits() {
        return this.storedBits;
    }

    @Override
    public void addBits(long amount) {
        this.storedBits += amount;
        this.saveChanges();
        this.notifyGridOfStorageChange();
    }

    @Override
    public boolean deductBits(long amount) {
        if (this.storedBits >= amount) {
            this.storedBits -= amount;
            this.saveChanges();
            this.notifyGridOfStorageChange();
            return true;
        }
        return false;
    }

    @Override
    public void learnItem(ItemStack stack) {
        for (ItemStack existing : this.learnedItems) {
            if (ItemStack.isSameItemSameComponents(existing, stack)) {
                return;
            }
        }
        this.learnedItems.add(stack.copyWithCount(1));
        this.saveChanges();
        this.notifyGridOfStorageChange();
    }

    @Override
    public List<ItemStack> getLearnedItems() {
        return this.learnedItems;
    }

    @Override
    public long calculateValue(ItemStack stack) {
        return BitValueCalculator.calculateBaseValue(stack);
    }

    @Override
    public void saveChanges() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public void openMenu(ServerPlayer player) {
        player.openMenu(this, buf -> buf.writeBlockPos(this.worldPosition));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.digitalexchange.exchange_core");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new ExchangeCorePriorityMenu(windowId, playerInventory, this, this.containerData);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("StoredBits", this.storedBits);
        tag.putInt("Priority", this.priority);

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
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.storedBits = tag.getLong("StoredBits");
        this.priority = tag.getInt("Priority");
        this.learnedItems.clear();

        ListTag list = tag.getList("LearnedItems", Tag.TAG_COMPOUND);
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        for (int i = 0; i < list.size(); i++) {
            ItemStack.OPTIONAL_CODEC.parse(ops, list.get(i))
                    .resultOrPartial()
                    .ifPresent(this.learnedItems::add);
        }
    }
}
