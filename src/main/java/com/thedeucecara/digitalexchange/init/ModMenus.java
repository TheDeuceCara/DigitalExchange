package com.thedeucecara.digitalexchange.init;

import appeng.menu.implementations.PriorityMenu;
import com.thedeucecara.digitalexchange.DigitalExchangeMod;
import com.thedeucecara.digitalexchange.block.ExchangeCoreBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, DigitalExchangeMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<PriorityMenu>> PRIORITY_MENU =
            MENUS.register("priority_menu", () -> IMenuTypeExtension.create((windowId, inv, data) -> {
                var pos = data.readBlockPos();
                if (inv.player.level().getBlockEntity(pos) instanceof ExchangeCoreBlockEntity core) {
                    return new PriorityMenu(ModMenus.PRIORITY_MENU.get(), windowId, inv, core);
                }
                return null;
            }));
}
