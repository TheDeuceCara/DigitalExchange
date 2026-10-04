package com.thedeucecara.digitalexchange.init;

import com.thedeucecara.digitalexchange.DigitalExchangeMod;
import com.thedeucecara.digitalexchange.menu.ExchangeCorePriorityMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, DigitalExchangeMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ExchangeCorePriorityMenu>> PRIORITY_MENU =
            MENUS.register("priority_menu", () -> IMenuTypeExtension.create(ExchangeCorePriorityMenu::new));
}
