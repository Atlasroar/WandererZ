package net.nutritionz.compat;

import net.dehydration.api.DrinkEvent;
import net.nutritionz.init.EventInit;

public class DehydrationCompat {

    public static void init() {
        DrinkEvent.EVENT.register((stack, player) -> {
            // Edible drinks are already handled by Capsaicin's food event.
            if (!stack.isFood()) {
                EventInit.addNutrition(player, stack);
            }
        });
    }
}
