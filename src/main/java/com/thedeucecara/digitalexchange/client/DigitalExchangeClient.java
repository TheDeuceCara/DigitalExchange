package com.thedeucecara.digitalexchange.client;

import com.thedeucecara.digitalexchange.init.ModMenus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class DigitalExchangeClient {

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.PRIORITY_MENU.get(), ExchangeCorePriorityScreen::new);
    }
}
