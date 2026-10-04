package com.thedeucecara.digitalexchange.client;

import appeng.client.gui.implementations.PriorityScreen;
import com.thedeucecara.digitalexchange.DigitalExchangeMod;
import com.thedeucecara.digitalexchange.init.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = DigitalExchangeMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class DigitalExchangeClient {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.PRIORITY_MENU.get(), PriorityScreen::new);
    }
}
