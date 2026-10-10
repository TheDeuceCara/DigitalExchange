package com.thedeucecara.digitalexchange.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class CustomBitValuesLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<Item, Long> CUSTOM_VALUES = new HashMap<>();

    public CustomBitValuesLoader() {
        super(GSON, "digitalexchange/custom_values");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        CUSTOM_VALUES.clear();
        int loadedCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            JsonElement element = entry.getValue();
            if (!element.isJsonObject()) continue;

            JsonObject json = element.getAsJsonObject();
            for (String key : json.keySet()) {
                ResourceLocation itemId = ResourceLocation.tryParse(key);
                if (itemId != null && BuiltInRegistries.ITEM.containsKey(itemId)) {
                    Item item = BuiltInRegistries.ITEM.get(itemId);
                    long bitCost = json.get(key).getAsLong();
                    if (bitCost > 0) {
                        CUSTOM_VALUES.put(item, bitCost);
                        loadedCount++;
                    }
                }
            }
        }
        LOGGER.info("[DigitalExchange] Loaded {} custom Bit overrides from datapacks.", loadedCount);
    }

    public static Long getCustomValue(Item item) {
        return CUSTOM_VALUES.get(item);
    }

    public static boolean hasCustomValue(Item item) {
        return CUSTOM_VALUES.containsKey(item);
    }

    public static Map<Item, Long> getAllCustomValues() {
        return CUSTOM_VALUES;
    }
}
