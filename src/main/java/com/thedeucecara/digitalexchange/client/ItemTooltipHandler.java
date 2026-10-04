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

        long unitDeposit = BitValueCalculator.calculateInputValue(stack);
        long unitWithdraw = BitValueCalculator.calculateExtractCost(stack);
        int count = stack.getCount();

        if (unitDeposit > 0 || unitWithdraw > 0) {
            if (unitDeposit == unitWithdraw) {
                // Single Item Cost
                Component line = Component.literal("Bits: ")
                        .withStyle(ChatFormatting.DARK_AQUA)
                        .append(Component.literal(FORMATTER.format(unitWithdraw)).withStyle(ChatFormatting.AQUA));
                
                // Stack Multiplier (if holding more than 1)
                if (count > 1) {
                    long totalWithdraw = unitWithdraw * count;
                    line = line.copy().append(
                            Component.literal(" (" + FORMATTER.format(totalWithdraw) + " total)")
                                     .withStyle(ChatFormatting.DARK_GRAY)
                    );
                }
                event.getToolTip().add(line);
            } else {
                // Split Difficulty (Deposit vs Withdraw)
                Component depLine = Component.literal("Deposit: ")
                        .withStyle(ChatFormatting.DARK_AQUA)
                        .append(Component.literal(FORMATTER.format(unitDeposit) + " Bits").withStyle(ChatFormatting.AQUA));
                
                Component withLine = Component.literal("Withdraw: ")
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(FORMATTER.format(unitWithdraw) + " Bits").withStyle(ChatFormatting.YELLOW));

                if (count > 1) {
                    depLine = depLine.copy().append(
                            Component.literal(" (" + FORMATTER.format(unitDeposit * count) + " total)")
                                     .withStyle(ChatFormatting.DARK_GRAY)
                    );
                    withLine = withLine.copy().append(
                            Component.literal(" (" + FORMATTER.format(unitWithdraw * count) + " total)")
                                     .withStyle(ChatFormatting.DARK_GRAY)
                    );
                }
                event.getToolTip().add(depLine);
                event.getToolTip().add(withLine);
            }
        }
    }
}
