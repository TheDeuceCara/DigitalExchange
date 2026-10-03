package com.thedeucecara.digitalexchange.integration.ae2;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class BitValueCalculator {

    public static long calculate(ItemStack stack) {
        if (stack.isEmpty()) return 0L;

        long baseBits = DynamicRecipeGraph.getBaseValue(stack.getItem());
        if (baseBits <= 0L) {
            baseBits = 64L;
        }

        if (stack.isDamageableItem() && stack.isDamaged()) {
            double integrity = (double) (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
            baseBits = (long) Math.max(1, Math.floor(baseBits * integrity));
        }

        long enchantBonus = 0L;
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) {
            enchantments = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        }

        for (var entry : enchantments.entrySet()) {
            int level = entry.getIntValue();
            enchantBonus += 1024L * (1L << Math.max(0, level - 1));
        }

        long customDataBonus = 0L;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty()) {
            CompoundTag tag = customData.copyTag();

            if (tag.contains("apoth_rarity")) {
                String rarity = tag.getString("apoth_rarity");
                customDataBonus += switch (rarity.toLowerCase()) {
                    case "apotheosis:common" -> 500L;
                    case "apotheosis:uncommon" -> 2_000L;
                    case "apotheosis:rare" -> 8_000L;
                    case "apotheosis:epic" -> 32_000L;
                    case "apotheosis:mythic" -> 128_000L;
                    case "apotheosis:ancient" -> 512_000L;
                    default -> 1_000L;
                };
            }
            if (tag.contains("apoth_affixes")) {
                customDataBonus += tag.getCompound("apoth_affixes").size() * 4_000L;
            }

            if (tag.contains("silentgear:construction")) {
                CompoundTag construction = tag.getCompound("silentgear:construction");
                ListTag parts = construction.getList("Parts", Tag.TAG_COMPOUND);
                for (int i = 0; i < parts.size(); i++) {
                    CompoundTag part = parts.getCompound(i);
                    String mat = part.getString("Material");
                    customDataBonus += evaluateSilentGearMaterial(mat);
                }
            }

            if (customDataBonus == 0L) {
                customDataBonus += (long) tag.size() * 256L;
            }
        }

        return baseBits + enchantBonus + customDataBonus;
    }

    private static long evaluateSilentGearMaterial(String materialId) {
        if (materialId.contains("diamond")) return 8192L * 2;
        if (materialId.contains("crimson_steel") || materialId.contains("azure_silver")) return 32768L;
        if (materialId.contains("tyrian_steel")) return 131072L;
        if (materialId.contains("iron")) return 256L * 2;
        return 1024L;
    }
}
