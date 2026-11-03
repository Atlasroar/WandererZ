package net.nutritionz.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "WandererZ/nutritionz")
@Config.Gui.Background("minecraft:textures/block/stone.png")
public class NutritionzConfig implements ConfigData {

    public int maxNutrition = 300;
    public int negativeNutrition = 30;
    public int positiveNutrition = 270;
    public int posX = 162;
    public int posY = 5;
    @Comment("Used for the nutrition gui")
    public String carbohydrateItemId = "minecraft:sugar";
    public String proteinItemId = "minecraft:chicken";
    public String fatItemId = "minecraft:porkchop";
    public String vitaminItemId = "minecraft:apple";
    public String mineralItemId = "minecraft:iron_nugget";

    @Override
    public void validatePostLoad() throws ValidationException {
        if (maxNutrition <= 0 || negativeNutrition < 0 || negativeNutrition >= positiveNutrition || positiveNutrition > maxNutrition) {
            throw new ValidationException("Nutrition thresholds must satisfy 0 <= negativeNutrition < positiveNutrition <= maxNutrition");
        }
    }
}
