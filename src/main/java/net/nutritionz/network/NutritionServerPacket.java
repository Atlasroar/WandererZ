package net.nutritionz.network;

import java.util.Map;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.nutritionz.NutritionMain;
import net.nutritionz.access.HungerManagerAccess;
import net.nutritionz.data.NutritionEffects;
import net.nutritionz.init.ConfigInit;

public class NutritionServerPacket {

    public static final Identifier NUTRITION_SYNC_PACKET = new Identifier("nutritionz", "nutrition_sync_packet");
    public static final Identifier SEND_NUTRITION_PACKET = new Identifier("nutritionz", "send_nutrition_packet");
    public static final Identifier ITEM_NUTRITION_PACKET = new Identifier("nutritionz", "item_nutrition_packet");
    public static final Identifier EFFECT_NUTRITION_PACKET = new Identifier("nutritionz", "effect_nutrition_packet");

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(SEND_NUTRITION_PACKET, (server, player, handler, buffer, sender) -> {
            server.execute(() -> {
                writeS2CNutritionPacket(player, (HungerManagerAccess) player.getHungerManager());
                writeS2CEffectNutritionPacket(handler);
            });
        });
    }

    public static void writeS2CNutritionPacket(ServerPlayerEntity player, HungerManagerAccess hunger) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(ConfigInit.CONFIG.maxNutrition);
        buf.writeInt(ConfigInit.CONFIG.negativeNutrition);
        buf.writeInt(ConfigInit.CONFIG.positiveNutrition);
        for (int i = 0; i < 5; i++) {
            buf.writeInt(hunger.getNutritionLevel(i));
        }
        ServerPlayNetworking.send(player, NUTRITION_SYNC_PACKET, buf);
    }

    public static void writeS2CItemNutritionPacket(ServerPlayNetworkHandler handler) {
        if (!ServerPlayNetworking.canSend(handler.player, ITEM_NUTRITION_PACKET)) {
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(NutritionMain.NUTRITION_ITEM_MAP.size());
        NutritionMain.NUTRITION_ITEM_MAP.forEach((item, levels) -> {
            buf.writeIdentifier(Registries.ITEM.getId(item));
            levels.forEach(buf::writeInt);
        });
        ServerPlayNetworking.send(handler.player, ITEM_NUTRITION_PACKET, buf);
    }

    public static void writeS2CEffectNutritionPacket(ServerPlayNetworkHandler handler) {
        PacketByteBuf buf = PacketByteBufs.create();
        writeEffects(buf, NutritionMain.NUTRITION_POSITIVE_EFFECTS);
        writeEffects(buf, NutritionMain.NUTRITION_NEGATIVE_EFFECTS);
        ServerPlayNetworking.send(handler.player, EFFECT_NUTRITION_PACKET, buf);
    }

    private static void writeEffects(PacketByteBuf buf, Map<Integer, NutritionEffects> effects) {
        buf.writeInt(effects.size());
        effects.forEach((nutrient, value) -> {
            buf.writeInt(nutrient);
            buf.writeInt(value.statusEffects().size());
            value.statusEffects().forEach(effect -> {
                buf.writeIdentifier(Registries.STATUS_EFFECT.getId(effect.getEffectType()));
                buf.writeInt(effect.getDuration());
                buf.writeInt(effect.getAmplifier());
            });
            buf.writeInt(value.attributes().size());
            value.attributes().forEach((attribute, modifier) -> {
                buf.writeIdentifier(Registries.ATTRIBUTE.getId(attribute));
                buf.writeUuid(modifier.getId());
                buf.writeDouble(modifier.getValue());
                buf.writeEnumConstant(modifier.getOperation());
            });
        });
    }
}
