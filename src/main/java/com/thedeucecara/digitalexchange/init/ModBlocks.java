package com.thedeucecara.digitalexchange.init;

import com.thedeucecara.digitalexchange.DigitalExchangeMod;
import com.thedeucecara.digitalexchange.block.ExchangeCoreBlock;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DigitalExchangeMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DigitalExchangeMod.MODID);

    public static final DeferredBlock<ExchangeCoreBlock> EXCHANGE_CORE = BLOCKS.register("exchange_core", ExchangeCoreBlock::new);
    public static final DeferredItem<BlockItem> EXCHANGE_CORE_ITEM = ITEMS.registerSimpleBlockItem("exchange_core", EXCHANGE_CORE);
}
