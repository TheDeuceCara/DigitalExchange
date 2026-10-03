package com.thedeucecara.digitalexchange.integration.ae2;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
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

    public static void computeGraph(MinecraftServer server) {
        RESOLVED_BASE_VALUES.clear();
        LOGGER.info("[DigitalExchange] Initializing dynamic item valuation graph...");

        assignDynamicTagBaselines();

        var recipeManager = server.getRecipeManager();
        var recipes = recipeManager.getRecipes();

        boolean changed = true;
        int pass = 0;
        int maxPasses = 16;

        while (changed && pass < maxPasses) {
            changed = false;
            pass++;

            for (RecipeHolder<?> holder : recipes) {
                var recipe = holder.value();

                if (recipe instanceof CraftingRecipe crafting) {
                    ItemStack output = crafting.getResultItem(server.registryAccess());
                    if (output.isEmpty()) continue;

                    long cost = evaluateIngredients(crafting.getIngredients());
                    if (cost > 0) {
                        long perItemCost = Math.max(1L, cost / output.getCount());
                        if (updateIfBetter(output.getItem(), perItemCost)) {
                            changed = true;
                        }
                    }
                } else if (recipe instanceof SmeltingRecipe smelting) {
                    ItemStack output = smelting.getResultItem(server.registryAccess());
                    if (output.isEmpty()) continue;

                    long inputCost = evaluateIngredients(smelting.getIngredients());
                    if (inputCost > 0) {
                        long perItemCost = Math.max(1L, (inputCost + 8L) / output.getCount());
                        if (updateIfBetter(output.getItem(), perItemCost)) {
                            changed = true;
                        }
                    }
                }
            }
        }

        LOGGER.info("[DigitalExchange] Dynamic valuation converged in {} passes. Evaluated {} items.", pass, RESOLVED_BASE_VALUES.size());
    }

    private static void assignDynamicTagBaselines() {
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

        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
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
            }
        }
    }

    private static long evaluateIngredients(java.util.List<Ingredient> ingredients) {
        long sum = 0L;
        for (Ingredient ing : ingredients) {
            if (ing.isEmpty()) continue;
            long lowestCost = Long.MAX_VALUE;

            for (ItemStack stack : ing.getItems()) {
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
