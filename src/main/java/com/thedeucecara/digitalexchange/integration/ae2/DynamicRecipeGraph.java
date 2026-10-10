package com.thedeucecara.digitalexchange.integration.ae2;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class DynamicRecipeGraph {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Item, Long> RESOLVED_BASE_VALUES = new HashMap<>();

    // Datapack Tag definitions
    public static final TagKey<Item> BLACKLIST_TAG = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath("digitalexchange", "blacklisted")
    );

    public static void computeGraph(MinecraftServer server) {
        RESOLVED_BASE_VALUES.clear();
        LOGGER.info("[DigitalExchange] Initializing dynamic item valuation graph...");

        // 1. Establish anchor and common tag baselines
        assignDynamicTagBaselines();

        var recipeManager = server.getRecipeManager();
        var recipes = recipeManager.getRecipes();

        boolean changed = true;
        int pass = 0;
        int maxPasses = 16;
        int errorCount = 0;

        while (changed && pass < maxPasses) {
            changed = false;
            pass++;

            for (RecipeHolder<?> holder : recipes) {
                try {
                    var recipe = holder.value();

                    if (recipe instanceof CraftingRecipe crafting) {
                        ItemStack output = crafting.getResultItem(server.registryAccess());

                        // SAFE-FAIL: Guard empty outputs or blacklisted items
                        if (output.isEmpty() || output.getCount() <= 0 || isBlacklisted(output.getItem())) {
                            continue;
                        }

                        long cost = evaluateIngredients(crafting.getIngredients());
                        if (cost > 0) {
                            long remainderRefund = calculateRemainderRefund(crafting.getIngredients());
                            long netCost = Math.max(1L, cost - remainderRefund);

                            int outputCount = Math.max(1, output.getCount());
                            long perItemCost = Math.max(1L, netCost / outputCount);

                            if (updateIfBetter(output.getItem(), perItemCost)) {
                                changed = true;
                            }
                        }
                    } else if (recipe instanceof SmeltingRecipe smelting) {
                        ItemStack output = smelting.getResultItem(server.registryAccess());

                        if (output.isEmpty() || output.getCount() <= 0 || isBlacklisted(output.getItem())) {
                            continue;
                        }

                        long inputCost = evaluateIngredients(smelting.getIngredients());
                        if (inputCost > 0) {
                            int outputCount = Math.max(1, output.getCount());
                            long perItemCost = Math.max(1L, (inputCost + 8L) / outputCount);

                            if (updateIfBetter(output.getItem(), perItemCost)) {
                                changed = true;
                            }
                        }
                    }
                } catch (Throwable t) {
                    if (pass == 1) {
                        errorCount++;
                        LOGGER.warn("[DigitalExchange] Safely skipped broken recipe '{}' during valuation: {}",
                                holder.id(), t.getMessage());
                    }
                }
            }
        }

        LOGGER.info("[DigitalExchange] Dynamic valuation converged in {} passes. Evaluated {} items (Skipped {} incompatible recipes).",
                pass, RESOLVED_BASE_VALUES.size(), errorCount);
    }

    private static void assignDynamicTagBaselines() {
        // Vanilla Anchors
        setBase(Items.DIRT, 1L);
        setBase(Items.COBBLESTONE, 1L);
        setBase(Items.STONE, 1L);
        setBase(Items.ANDESITE, 1L);
        setBase(Items.DIORITE, 1L);
        setBase(Items.GRANITE, 1L);
        setBase(Items.NETHERRACK, 1L);
        setBase(Items.END_STONE, 1L);
        setBase(Items.SAND, 1L);
        setBase(Items.GRAVEL, 4L);
        setBase(Items.STICK, 4L);
        setBase(Items.OAK_LOG, 32L);
        setBase(Items.COAL, 128L);
        setBase(Items.IRON_INGOT, 256L);
        setBase(Items.COPPER_INGOT, 128L);
        setBase(Items.GOLD_INGOT, 2048L);
        setBase(Items.DIAMOND, 8192L);
        setBase(Items.EMERALD, 8192L);
        setBase(Items.NETHERITE_INGOT, 65536L);
        setBase(Items.BUCKET, 768L);

        // Modded Tag Scanning across all namespaces
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR || isBlacklisted(item)) continue;
            try {
                Holder<Item> holder = item.builtInRegistryHolder();

                if (matchesTagPrefix(holder, "c", "ingots/")) {
                    if (matchesTag(holder, "c", "ingots/copper")) setBase(item, 128L);
                    else if (matchesTag(holder, "c", "ingots/tin")) setBase(item, 192L);
                    else if (matchesTag(holder, "c", "ingots/zinc") || matchesTag(holder, "c", "ingots/lead")) setBase(item, 256L);
                    else if (matchesTag(holder, "c", "ingots/silver") || matchesTag(holder, "c", "ingots/nickel")) setBase(item, 1024L);
                    else if (matchesTag(holder, "c", "ingots/uranium")) setBase(item, 4096L);
                    else setBase(item, 512L);
                } else if (matchesTagPrefix(holder, "c", "gems/")) {
                    setBase(item, 2048L);
                } else if (matchesTagPrefix(holder, "c", "raw_materials/")) {
                    setBase(item, 256L);
                } else if (matchesTagPrefix(holder, "c", "ores/")) {
                    setBase(item, 257L);
                } else if (matchesTagPrefix(holder, "c", "dusts/")) {
                    setBase(item, 128L);
                } else if (matchesTagPrefix(holder, "c", "storage_blocks/")) {
                    setBase(item, 2304L);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static long evaluateIngredients(NonNullList<Ingredient> ingredients) {
        long sum = 0L;
        for (Ingredient ing : ingredients) {
            if (ing.isEmpty()) continue;
            long lowestCost = Long.MAX_VALUE;

            ItemStack[] matchingStacks;
            try {
                matchingStacks = ing.getItems();
            } catch (Exception e) {
                return 0L;
            }

            for (ItemStack stack : matchingStacks) {
                if (stack.isEmpty() || isBlacklisted(stack.getItem())) continue;
                long val = getBaseValue(stack.getItem());
                if (val > 0 && val < lowestCost) {
                    lowestCost = val;
                }
            }

            if (lowestCost == Long.MAX_VALUE) {
                return 0L;
            }
            sum += lowestCost;
        }
        return sum;
    }

    private static long calculateRemainderRefund(NonNullList<Ingredient> ingredients) {
        long refund = 0L;
        for (Ingredient ing : ingredients) {
            if (ing.isEmpty()) continue;

            ItemStack[] matchingStacks;
            try {
                matchingStacks = ing.getItems();
            } catch (Exception e) {
                continue;
            }

            for (ItemStack stack : matchingStacks) {
                if (stack.isEmpty()) continue;
                try {
                    // NeoForge 1.21.1 remainder query
                    ItemStack remainder = stack.getCraftingRemainingItem();
                    if (!remainder.isEmpty()) {
                        long remVal = getBaseValue(remainder.getItem());
                        if (remVal > 0) {
                            refund += remVal;
                            break;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return refund;
    }

    public static boolean isBlacklisted(Item item) {
        return item.builtInRegistryHolder().is(BLACKLIST_TAG);
    }

    private static boolean updateIfBetter(Item item, long newCost) {
        long existing = RESOLVED_BASE_VALUES.getOrDefault(item, 0L);
        if (existing == 0L || newCost < existing) {
            RESOLVED_BASE_VALUES.put(item, newCost);
            return true;
        }
        return false;
    }

    private static void setBase(Item item, long value) {
        RESOLVED_BASE_VALUES.putIfAbsent(item, value);
    }

    public static long getBaseValue(Item item) {
        if (isBlacklisted(item)) return 0L;
        return RESOLVED_BASE_VALUES.getOrDefault(item, 0L);
    }

    private static boolean matchesTag(Holder<Item> holder, String namespace, String path) {
        return holder.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path)));
    }

    private static boolean matchesTagPrefix(Holder<Item> holder, String namespace, String prefix) {
        return holder.tags().anyMatch(tag ->
                tag.location().getNamespace().equals(namespace) && tag.location().getPath().startsWith(prefix)
        );
    }
}
