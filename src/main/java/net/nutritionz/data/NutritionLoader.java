package net.nutritionz.data;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import com.google.common.collect.LinkedHashMultimap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.nutritionz.NutritionMain;

public class NutritionLoader implements SimpleSynchronousResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger("NutritionZ");
    private static final List<String> NUTRIENTS = List.of("carbohydrates", "protein", "fat", "vitamins", "minerals");

    @Override
    public Identifier getFabricId() {
        return new Identifier("nutritionz", "loader");
    }

    @Override
    public void reload(ResourceManager manager) {
        NutritionMain.NUTRITION_ITEM_MAP.clear();
        NutritionMain.NUTRITION_POSITIVE_EFFECTS.clear();
        NutritionMain.NUTRITION_NEGATIVE_EFFECTS.clear();
        load(manager, "nutrition", (id, data) -> {
            for (String itemId : data.keySet()) {
                Identifier item = new Identifier(itemId);
                if (!Registries.ITEM.containsId(item)) {
                    LOGGER.warn("Unknown nutrition item {} in {}", item, id);
                    continue;
                }
                JsonObject definition = data.getAsJsonObject(itemId);
                List<Integer> levels = new ArrayList<>();
                for (String nutrient : NUTRIENTS) {
                    int amount = JsonHelper.getInt(definition, nutrient, 0);
                    if (amount < 0) {
                        throw new JsonParseException("Negative nutrition amount for " + itemId + ": " + nutrient);
                    }
                    levels.add(amount);
                }
                NutritionMain.NUTRITION_ITEM_MAP.put(Registries.ITEM.get(item), List.copyOf(levels));
            }
        });
        load(manager, "nutrition_manager", (id, data) -> {
            for (int i = 0; i < NUTRIENTS.size(); i++) {
                if (!data.has(NUTRIENTS.get(i))) {
                    continue;
                }
                JsonObject definition = data.getAsJsonObject(NUTRIENTS.get(i));
                if (JsonHelper.getBoolean(definition, "replace", false)) {
                    NutritionMain.NUTRITION_POSITIVE_EFFECTS.remove(i);
                    NutritionMain.NUTRITION_NEGATIVE_EFFECTS.remove(i);
                }
                if (definition.has("positive")) {
                    NutritionMain.NUTRITION_POSITIVE_EFFECTS.put(i, readEffects(definition.getAsJsonObject("positive"), i, "positive"));
                }
                if (definition.has("negative")) {
                    NutritionMain.NUTRITION_NEGATIVE_EFFECTS.put(i, readEffects(definition.getAsJsonObject("negative"), i, "negative"));
                }
            }
        });
    }

    private static void load(ResourceManager manager, String path, BiConsumer<Identifier, JsonObject> consumer) {
        // Load bundled defaults before cross-namespace overrides; Minecraft selects the highest-priority copy of each file.
        manager.findResources(path, id -> id.getPath().endsWith(".json")).entrySet().stream()
                .sorted(Comparator.comparing((java.util.Map.Entry<Identifier, Resource> entry) ->
                        !entry.getKey().getNamespace().equals("nutritionz")).thenComparing(entry -> entry.getKey().toString()))
                .forEach(entry -> {
                    try (Reader reader = entry.getValue().getReader()) {
                        consumer.accept(entry.getKey(), JsonParser.parseReader(reader).getAsJsonObject());
                    } catch (IOException | JsonParseException | IllegalArgumentException | IllegalStateException e) {
                        LOGGER.error("Failed to load nutrition resource {}", entry.getKey(), e);
                    }
                });
    }

    private static NutritionEffects readEffects(JsonObject data, int nutrient, String polarity) {
        List<StatusEffectInstance> effects = new ArrayList<>();
        var attributes = LinkedHashMultimap.<EntityAttribute, EntityAttributeModifier>create();
        for (String name : data.keySet()) {
            Identifier id = new Identifier(name);
            JsonObject definition = data.getAsJsonObject(name);
            if (Registries.STATUS_EFFECT.containsId(id)) {
                effects.add(new StatusEffectInstance(Registries.STATUS_EFFECT.get(id), JsonHelper.getInt(definition, "duration"),
                        JsonHelper.getInt(definition, "amplifier", 0), false, false, true));
            } else if (Registries.ATTRIBUTE.containsId(id)) {
                UUID uuid = UUID.nameUUIDFromBytes(("nutritionz:" + nutrient + ":" + polarity + ":" + id).getBytes(StandardCharsets.UTF_8));
                attributes.put(Registries.ATTRIBUTE.get(id), new EntityAttributeModifier(uuid, "NutritionZ " + polarity,
                        JsonHelper.getFloat(definition, "value"), Operation.valueOf(JsonHelper.getString(definition, "operation").toUpperCase(java.util.Locale.ROOT))));
            } else {
                throw new JsonParseException("Unknown nutrition effect or attribute: " + id);
            }
        }
        return new NutritionEffects(List.copyOf(effects), attributes);
    }
}
