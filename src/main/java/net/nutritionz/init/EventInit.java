package net.nutritionz.init;

import java.util.List;

import de.siphalor.capsaicin.api.food.FoodEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.nutritionz.NutritionMain;
import net.nutritionz.access.HungerManagerAccess;
import net.nutritionz.compat.DehydrationCompat;
import net.nutritionz.network.NutritionServerPacket;

public class EventInit {

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> NutritionServerPacket.writeS2CItemNutritionPacket(handler));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                server.getPlayerManager().getPlayerList().forEach(player -> NutritionServerPacket.writeS2CItemNutritionPacket(player.networkHandler));
            }
        });
        // Capsaicin owns the food-consumption path in the merged mod.
        FoodEvents.EATEN.on(event -> {
            if (event.context().user() instanceof PlayerEntity player) {
                if (event.context().stack() != null) {
                    addNutrition(player, event.context().stack());
                } else if (event.context().blockState() != null) {
                    addNutrition(player, event.context().blockState().getBlock().asItem().getDefaultStack());
                }
            }
        });
        if (FabricLoader.getInstance().isModLoaded("dehydration")) {
            DehydrationCompat.init();
        }

        for (String mod : List.of("bakery", "beachparty", "brewery", "candlelight", "cornexpansion", "farm_and_charm",
                "herbalbrews", "lilis_lucky_lures", "meadow", "nethervinery", "vinery", "wildernature", "brewinandchewin",
                "culturaldelights", "expandeddelight", "farmersdelight", "moredelight", "natures_delight", "nethersdelight",
                "oceansdelight", "ubesdelight", "create", "createaddition", "adventurez", "amarite", "aquamirae", "betterend",
                "betternether", "biomemakeover", "bountifulfares", "culinaire", "dehydration", "exlinefishing", "fishofthieves",
                "hybrid-aquatic", "livingthings", "mobcatalog", "naturalist", "natures_spirit", "netherdepthsupgrade", "promenade",
                "regions_unexplored", "spawn", "spelunkery", "supplementaries", "tide", "wilderwild")) {
            registerPack(mod, mod + "_nutrition_compat");
        }
        registerPack("ad_astra", "adastra_nutrition_compat");
        registerPack("ends_delight", "endsdelight_nutrition_compat");
        registerPack("festive_delight", "festivedelight_nutrition_compat");
        registerPack("immersive_weathering", "immersiveweathering_nutrition_compat");
        registerPack("the_bumblezone", "thebumblezone_nutrition_compat");
        if (FabricLoader.getInstance().isModLoaded("dehydration")) {
            for (String mod : List.of("meadow", "nethervinery", "vinery")) {
                registerPack(mod, "dehydration_x_" + mod + "_nutrition_compat");
            }
        }
    }

    public static void addNutrition(PlayerEntity player, ItemStack stack) {
        if (NutritionMain.isEnabled() && !player.getWorld().isClient()) {
            List<Integer> nutrients = NutritionMain.NUTRITION_ITEM_MAP.get(stack.getItem());
            if (nutrients != null) {
                HungerManagerAccess hunger = (HungerManagerAccess) player.getHungerManager();
                for (int i = 0; i < nutrients.size(); i++) {
                    if (nutrients.get(i) > 0) {
                        hunger.addNutritionLevel(i, nutrients.get(i));
                    }
                }
            }
        }
    }

    private static void registerPack(String mod, String pack) {
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.isModLoaded(mod)) {
            ResourceManagerHelper.registerBuiltinResourcePack(new Identifier("wandererz", pack),
                    loader.getModContainer("wandererz").orElseThrow(), ResourcePackActivationType.DEFAULT_ENABLED);
        }
    }
}
