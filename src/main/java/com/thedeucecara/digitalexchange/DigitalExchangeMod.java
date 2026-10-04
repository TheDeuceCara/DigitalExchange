package com.thedeucecara.digitalexchange;

import com.thedeucecara.digitalexchange.config.ExchangeConfig;
import com.thedeucecara.digitalexchange.init.ModBlockEntities;
import com.thedeucecara.digitalexchange.init.ModBlocks;
import com.thedeucecara.digitalexchange.integration.ae2.DynamicRecipeGraph;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(DigitalExchangeMod.MODID)
public class DigitalExchangeMod {
    public static final String MODID = "digitalexchange";

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXCHANGE_TAB =
            CREATIVE_MODE_TABS.register("digitalexchange_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.digitalexchange"))
                    .icon(() -> new ItemStack(ModBlocks.EXCHANGE_CORE_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.EXCHANGE_CORE_ITEM.get());
                    }).build());

    public DigitalExchangeMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, ExchangeConfig.COMMON_SPEC, "digitalexchange-common.toml");

        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerCapabilities);

        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Expose standard ItemHandler capability to AE2 Storage Bus and all other mods
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.EXCHANGE_CORE.get(),
                (be, side) -> be.getItemHandler()
        );
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.EXCHANGE_CORE_ITEM.get());
        }
    }

    private void onServerStarted(ServerStartedEvent event) {
        DynamicRecipeGraph.computeGraph(event.getServer());
    }
}
