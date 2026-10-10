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
     * Creates a pristine template (100% durability, empty fluids) to store in knowledge.
     */
    public static ItemStack createPristineTemplate(ItemStack stack) {
        ItemStack clean = stack.copyWithCount(1);
        if (clean.isDamageableItem()) {
            clean.remove(DataComponents.DAMAGE);
        }
        return clean;
    }

    /**
     * Legacy/default calculate routing
     */
    public static long calculate(ItemStack stack) {
        return calculateBaseValue(stack);
    }

    /**
     * Calculates the pristine 100% baseline cost of an item template (unaffected by damage).
     */
    public static long calculatePristineBaseValue(ItemStack stack) {
        if (stack.isEmpty()) return 0L;

        // 1. Base Item Cost from Dynamic Recipe Graph / Common Tags
        long baseBits = DynamicRecipeGraph.getBaseValue(stack.getItem());
        if (baseBits <= 0L) {
            baseBits = 64L;
        }

        // 2. Enchantments & Stored Books
        long enchantBonus = 0L;
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) {
            enchantments = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        }

        for (var entry : enchantments.entrySet()) {
            int level = entry.getIntValue();
            enchantBonus += 1024L * (1L << Math.max(0, level - 1));
        }

        // 3. Custom Components: Apotheosis Gems, Modular Gear
        long customBonus = evaluateCustomComponents(stack, false);

        return baseBits + enchantBonus + customBonus;
    }

    /**
     * Calculates the current baseline Bit value of an item, taking durability and stored contents into account.
     */
    public static long calculateBaseValue(ItemStack stack) {
        if (stack.isEmpty() || !isSafeToLearnOrDeposit(stack)) return 0L;

        // 1. Base Item Cost from Dynamic Recipe Graph / Common Tags
        long baseBits = DynamicRecipeGraph.getBaseValue(stack.getItem());
        if (baseBits <= 0L) {
            baseBits = 64L;
        }

        // 2. Strict Durability / Integrity Scaling
        if (stack.isDamageableItem() && stack.isDamaged()) {
            double integrity = (double) (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
            baseBits = (long) Math.max(1, Math.floor(baseBits * integrity));
        }

        // 3. Enchantments & Stored Books
        long enchantBonus = 0L;
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) {
            enchantments = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        }

        for (var entry : enchantments.entrySet()) {
            int level = entry.getIntValue();
            enchantBonus += 1024L * (1L << Math.max(0, level - 1));
        }

        // 4. Custom Components: Apotheosis Gems, Modular Gear, Fluids & Gases
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

        // --- Apotheosis Affixes & Gear Rarity ---
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

        // --- Apotheosis Gem Evaluation (Purity, Type, Sockets) ---
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

        // --- Silent Gear Modular Construction ---
        if (tag.contains("silentgear:construction")) {
            CompoundTag construction = tag.getCompound("silentgear:construction");
            ListTag parts = construction.getList("Parts", Tag.TAG_COMPOUND);
            for (int i = 0; i < parts.size(); i++) {
                CompoundTag part = parts.getCompound(i);
                String mat = part.getString("Material");
                bonus += evaluateSilentGearMaterial(mat);
            }
        }

        // --- Mekanism Gases, Chemicals & Infusions ---
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

        // Generic modded tag fallback
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

    /**
     * Checks if the player has enough bits to withdraw an item,
     * applying the 25% minimum threshold for damageable gear.
     */
    public static boolean canExtractWithThreshold(ItemStack template, long availableBits) {
        long fullCost = calculateExtractCost(template);
        if (fullCost <= 0) return false;

        if (availableBits >= fullCost) return true;

        // If the item has durability, verify if available bits are at least 25% of full cost
        if (template.isDamageableItem()) {
            return availableBits >= (fullCost / 4L);
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
