package com.thedeucecara.digitalexchange;

import com.thedeucecara.digitalexchange.init.ModBlockEntities;
import com.thedeucecara.digitalexchange.init.ModBlocks;
import com.thedeucecara.digitalexchange.integration.ae2.DynamicRecipeGraph;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@Mod(DigitalExchangeMod.MODID)
public class DigitalExchangeMod {
    public static final String MODID = "digitalexchange";

    public DigitalExchangeMod(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
    }

    private void onServerStarted(ServerStartedEvent event) {
        DynamicRecipeGraph.computeGraph(event.getServer());
    }
}
