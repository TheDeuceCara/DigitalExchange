package com.thedeucecara.digitalexchange.integration.ae2;

import com.thedeucecara.digitalexchange.config.ExchangeConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class BitValueCalculator {

    /**
     * Rejects items holding item inventories (Shulkers, Backpacks, Bundles).
     * Fluid and gas containers pass freely.
     */
    public static boolean isSafeToLearnOrDeposit(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container != null) {
            return container.nonEmptyStream().findAny().isEmpty();
        }
        return true;
    }

    /**
     * Creates a pristine template (100% durability, zero damage, stripped unbreakable tags).
     */
    public static ItemStack createPristineTemplate(ItemStack stack) {
        ItemStack clean = stack.copyWithCount(1);
        clean.remove(DataComponents.UNBREAKABLE);
        if (clean.isDamageableItem()) {
            clean.setDamageValue(0);
        }
        return clean;
    }

    public static long calculate(ItemStack stack) {
        return calculateBaseValue(stack);
    }

    /**
     * Baseline unscaled pristine cost (100% durability).
     */
    public static long calculatePristineBaseValue(ItemStack stack) {
        if (stack.isEmpty()) return 0L;

        long baseBits = DynamicRecipeGraph.getBaseValue(stack.getItem());
        if (baseBits <= 0L) {
            baseBits = 64L;
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

        long customBonus = evaluateCustomComponents(stack, false);
        return baseBits + enchantBonus + customBonus;
    }

    /**
     * Baseline value factoring in wear & stored liquids/gases.
     */
    public static long calculateBaseValue(ItemStack stack) {
        if (stack.isEmpty() || !isSafeToLearnOrDeposit(stack)) return 0L;

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

        long customBonus = evaluateCustomComponents(stack, true);
        return baseBits + enchantBonus + customBonus;
    }

    private static long evaluateCustomComponents(ItemStack stack, boolean includeFluidsAndGases) {
        long bonus = 0L;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            return 0L;
        }

        CompoundTag tag = customData.copyTag();

        if (tag.contains("apoth_rarity")) {
            String rarity = tag.getString("apoth_rarity").toLowerCase();
            bonus += switch (rarity) {
                case "apotheosis:common" -> 1_000L;
                case "apotheosis:uncommon" -> 4_000L;
                case "apotheosis:rare" -> 16_000L;
                case "apotheosis:epic" -> 64_000L;
                case "apotheosis:mythic" -> 256_000L;
                case "apotheosis:ancient" -> 1_024_000L;
                default -> 2_000L;
            };
        }
        if (tag.contains("apoth_affixes")) {
            bonus += tag.getCompound("apoth_affixes").size() * 8_000L;
        }

        if (tag.contains("gem") || tag.contains("apoth_gem")) {
            String purity = tag.getString("purity").toLowerCase();
            bonus += switch (purity) {
                case "cracked" -> 2_048L;
                case "chipped" -> 4_096L;
                case "flawed" -> 16_384L;
                case "flawless" -> 65_536L;
                case "perfect" -> 262_144L;
                default -> 8_192L;
            };
        }

        if (tag.contains("silentgear:construction")) {
            CompoundTag construction = tag.getCompound("silentgear:construction");
            ListTag parts = construction.getList("Parts", Tag.TAG_COMPOUND);
            for (int i = 0; i < parts.size(); i++) {
                CompoundTag part = parts.getCompound(i);
                String mat = part.getString("Material");
                bonus += evaluateSilentGearMaterial(mat);
            }
        }

        if (includeFluidsAndGases && tag.contains("mekData")) {
            CompoundTag mekData = tag.getCompound("mekData");
            if (mekData.contains("GasTanks")) {
                ListTag gasList = mekData.getList("GasTanks", Tag.TAG_COMPOUND);
                for (int i = 0; i < gasList.size(); i++) {
                    CompoundTag tank = gasList.getCompound(i);
                    long amount = tank.getLong("amount");
                    String gasName = tank.getString("gasName");
                    bonus += evaluateChemical(gasName, amount);
                }
            }
            if (mekData.contains("FluidTanks")) {
                ListTag fluidList = mekData.getList("FluidTanks", Tag.TAG_COMPOUND);
                for (int i = 0; i < fluidList.size(); i++) {
                    CompoundTag tank = fluidList.getCompound(i);
                    long amount = tank.getLong("Amount");
                    String fluid = tank.getString("FluidName");
                    bonus += evaluateFluid(fluid, amount);
                }
            }
        }

        if (bonus == 0L) {
            bonus += (long) tag.size() * 256L;
        }

        return bonus;
    }

    public static long calculateInputValue(ItemStack stack) {
        long raw = calculateBaseValue(stack);
        if (raw <= 0L) return 0L;
        double ratio = ExchangeConfig.COMMON.inputRatio.get();
        return Math.max(1L, (long) Math.floor(raw * ratio));
    }

    public static long calculateExtractCost(ItemStack stack) {
        long raw = calculatePristineBaseValue(stack);
        if (raw <= 0L) return 0L;
        double ratio = ExchangeConfig.COMMON.extractRatio.get();
        return Math.max(1L, (long) Math.ceil(raw * ratio));
    }

    public static long getMinimumWearCost(ItemStack stack) {
        long fullCost = calculateExtractCost(stack);
        if (fullCost <= 0L) return 0L;
        return Math.max(1L, (long) Math.ceil(fullCost * 0.25));
    }

    public static boolean canExtractWithThreshold(ItemStack template, long availableBits) {
        long fullCost = calculateExtractCost(template);
        if (fullCost <= 0) return false;
        if (availableBits >= fullCost) return true;
        if (template.isDamageableItem()) {
            return availableBits >= getMinimumWearCost(template);
        }
        return false;
    }

    private static long evaluateChemical(String gasId, long amount) {
        if (amount <= 0) return 0L;
        long perBucketRate = switch (gasId.toLowerCase()) {
            case "mekanism:hydrogen", "mekanism:oxygen" -> 128L;
            case "mekanism:chlorine", "mekanism:sulfur_dioxide" -> 512L;
            case "mekanism:sulfur_trioxide", "mekanism:sulfuric_acid" -> 2_048L;
            case "mekanism:uranium_oxide", "mekanism:uranium_hexafluoride" -> 16_384L;
            case "mekanism:fissile_fuel", "mekanism:antimatter" -> 262_144L;
            default -> 256L;
        };
        return (amount * perBucketRate) / 1000L;
    }

    private static long evaluateFluid(String fluidId, long amount) {
        if (amount <= 0) return 0L;
        long perBucketRate = switch (fluidId.toLowerCase()) {
            case "minecraft:water" -> 1L;
            case "minecraft:lava" -> 64L;
            default -> 512L;
        };
        return (amount * perBucketRate) / 1000L;
    }

    private static long evaluateSilentGearMaterial(String materialId) {
        if (materialId.contains("diamond")) return 16_384L;
        if (materialId.contains("crimson_steel") || materialId.contains("azure_silver")) return 32_768L;
        if (materialId.contains("tyrian_steel")) return 131_072L;
        if (materialId.contains("iron")) return 512L;
        return 1_024L;
    }
}
