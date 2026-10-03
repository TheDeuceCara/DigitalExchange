package com.thedeucecara.digitalexchange.init;

import com.thedeucecara.digitalexchange.DigitalExchangeMod;
import com.thedeucecara.digitalexchange.block.ExchangeCoreBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DigitalExchangeMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExchangeCoreBlockEntity>> EXCHANGE_CORE =
            BLOCK_ENTITIES.register("exchange_core", () ->
                    BlockEntityType.Builder.of(ExchangeCoreBlockEntity::new, ModBlocks.EXCHANGE_CORE.get()).build(null));
}
