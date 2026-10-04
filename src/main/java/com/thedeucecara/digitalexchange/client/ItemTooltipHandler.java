package com.thedeucecara.digitalexchange.client;

import com.thedeucecara.digitalexchange.DigitalExchangeMod;
import com.thedeucecara.digitalexchange.integration.ae2.BitValueCalculator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.text.NumberFormat;
import java.util.Locale;

@EventBusSubscriber(modid = DigitalExchangeMod.MODID, value = Dist.CLIENT)
public class ItemTooltipHandler {

    private static final NumberFormat FORMATTER = NumberFormat.getInstance(Locale.US);

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        long bitValue = BitValueCalculator.calculate(stack);
        if (bitValue > 0) {
            event.getToolTip().add(
                    Component.literal("Bits: ")
                            .withStyle(ChatFormatting.DARK_AQUA)
                            .append(Component.literal(FORMATTER.format(bitValue)).withStyle(ChatFormatting.AQUA))
            );
        }
    }
}
