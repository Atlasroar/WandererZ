package net.nutritionz.util;

import java.util.List;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.nutritionz.network.NutritionClientPacket;

public class NutritionUtil {

    private static final List<String> NUTRITION_TOOLTIPS = List.of("item.nutritionz.carbohydrates", "item.nutritionz.protein", "item.nutritionz.fat", "item.nutritionz.vitamins",
            "item.nutritionz.minerals");

    public static void addNutritionToolTip(ItemStack stack, List<Text> list) {
        if (Screen.hasShiftDown() && NutritionClientPacket.ITEMS.containsKey(stack.getItem())) {
            List<Integer> nutritionList = NutritionClientPacket.ITEMS.get(stack.getItem());

            boolean hasNutritions = false;
            for (int i = 0; i < nutritionList.size(); i++) {
                if (nutritionList.get(i) > 0) {
                    if (!hasNutritions) {
                        list.add(ScreenTexts.EMPTY);
                        list.add(Text.translatable("item.nutritionz.nutrients"));
                    }
                    list.add(Text.translatable(NUTRITION_TOOLTIPS.get(i), nutritionList.get(i)).formatted(Formatting.GREEN));
                    hasNutritions = true;
                }
            }
        }
    }

}
