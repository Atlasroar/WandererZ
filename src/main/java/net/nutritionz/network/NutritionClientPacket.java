package net.nutritionz.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.LinkedHashMultimap;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.nutritionz.data.NutritionEffects;

@Environment(EnvType.CLIENT)
public class NutritionClientPacket {

    // Integrated servers share static state with the client, so keep received data separate.
    public static final Map<Item, List<Integer>> ITEMS = new HashMap<>();
    public static final Map<Integer, NutritionEffects> POSITIVE_EFFECTS = new HashMap<>();
    public static final Map<Integer, NutritionEffects> NEGATIVE_EFFECTS = new HashMap<>();
    public static int[] levels = new int[5];
    public static int maxNutrition = 300;
    public static int negativeNutrition = 30;
    public static int positiveNutrition = 270;

    public static void init() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        ClientPlayConnectionEvents.INIT.register((handler, client) -> clear());
        ClientPlayNetworking.registerGlobalReceiver(NutritionServerPacket.NUTRITION_SYNC_PACKET, (client, handler, buf, sender) -> {
            int maximum = buf.readInt();
            int negative = buf.readInt();
            int positive = buf.readInt();
            int[] nutrients = new int[5];
            for (int i = 0; i < nutrients.length; i++) {
                nutrients[i] = buf.readInt();
            }
            client.execute(() -> {
                maxNutrition = maximum;
                negativeNutrition = negative;
                positiveNutrition = positive;
                levels = nutrients;
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(NutritionServerPacket.ITEM_NUTRITION_PACKET, (client, handler, buf, sender) -> {
            Map<Item, List<Integer>> items = new HashMap<>();
            int size = buf.readInt();
            for (int i = 0; i < size; i++) {
                Item item = Registries.ITEM.get(buf.readIdentifier());
                List<Integer> nutrients = new ArrayList<>();
                for (int j = 0; j < 5; j++) {
                    nutrients.add(buf.readInt());
                }
                items.put(item, List.copyOf(nutrients));
            }
            client.execute(() -> {
                ITEMS.clear();
                ITEMS.putAll(items);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(NutritionServerPacket.EFFECT_NUTRITION_PACKET, (client, handler, buf, sender) -> {
            Map<Integer, NutritionEffects> positive = readEffects(buf);
            Map<Integer, NutritionEffects> negative = readEffects(buf);
            client.execute(() -> {
                POSITIVE_EFFECTS.clear();
                POSITIVE_EFFECTS.putAll(positive);
                NEGATIVE_EFFECTS.clear();
                NEGATIVE_EFFECTS.putAll(negative);
            });
        });
    }

    private static Map<Integer, NutritionEffects> readEffects(PacketByteBuf buf) {
        Map<Integer, NutritionEffects> result = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            int nutrient = buf.readInt();
            List<StatusEffectInstance> effects = new ArrayList<>();
            int effectCount = buf.readInt();
            for (int j = 0; j < effectCount; j++) {
                effects.add(new StatusEffectInstance(Registries.STATUS_EFFECT.get(buf.readIdentifier()), buf.readInt(), buf.readInt(), false, false, true));
            }
            var attributes = LinkedHashMultimap.<EntityAttribute, EntityAttributeModifier>create();
            int attributeCount = buf.readInt();
            for (int j = 0; j < attributeCount; j++) {
                EntityAttribute attribute = Registries.ATTRIBUTE.get(buf.readIdentifier());
                attributes.put(attribute, new EntityAttributeModifier(buf.readUuid(), "NutritionZ", buf.readDouble(), buf.readEnumConstant(EntityAttributeModifier.Operation.class)));
            }
            result.put(nutrient, new NutritionEffects(List.copyOf(effects), attributes));
        }
        return result;
    }

    private static void clear() {
        ITEMS.clear();
        POSITIVE_EFFECTS.clear();
        NEGATIVE_EFFECTS.clear();
        levels = new int[5];
    }

    public static void writeC2SNutritionPacket() {
        ClientPlayNetworking.send(NutritionServerPacket.SEND_NUTRITION_PACKET, PacketByteBufs.empty());
    }
}
