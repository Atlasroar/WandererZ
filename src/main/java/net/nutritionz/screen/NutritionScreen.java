package net.nutritionz.screen;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.nutritionz.data.NutritionEffects;
import net.nutritionz.network.NutritionClientPacket;
import net.nutritionz.init.ConfigInit;
import net.nutritionz.init.RenderInit;

@Environment(EnvType.CLIENT)
public class NutritionScreen extends Screen {

    private int x;
    private int y;
    private int refreshTicks;
    private final List<ItemStack> nutritionItems = List.of(new ItemStack(Registries.ITEM.get(new Identifier(ConfigInit.CONFIG.carbohydrateItemId))),
            new ItemStack(Registries.ITEM.get(new Identifier(ConfigInit.CONFIG.proteinItemId))), new ItemStack(Registries.ITEM.get(new Identifier(ConfigInit.CONFIG.fatItemId))),
            new ItemStack(Registries.ITEM.get(new Identifier(ConfigInit.CONFIG.vitaminItemId))), new ItemStack(Registries.ITEM.get(new Identifier(ConfigInit.CONFIG.mineralItemId))));
    private final List<Text> nutritionTexts = List.of(Text.translatable("screen.nutritionz.carbohydrates"), Text.translatable("screen.nutritionz.protein"), Text.translatable("screen.nutritionz.fat"),
            Text.translatable("screen.nutritionz.vitamins"), Text.translatable("screen.nutritionz.minerals"));

    public NutritionScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();
        this.x = this.width / 2 - (176 / 2);
        this.y = this.height / 2 - (141 / 2);
    }

    @Override
    public void tick() {
        super.tick();
        if (++this.refreshTicks % 20 == 0) {
            NutritionClientPacket.writeC2SNutritionPacket();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        context.drawTexture(RenderInit.NUTRITION_ICONS, this.x, this.y, 0, 0, 176, 142);
        context.drawText(this.textRenderer, this.title, this.x + 176 / 2 - this.textRenderer.getWidth(this.title) / 2, this.y + 7, 0x3F3F3F, false);
        int extraY = 0;
        int extraBarY = 0;
        for (int i = 0; i < this.nutritionItems.size(); i++) {
            context.drawItem(this.nutritionItems.get(i), this.x + 7, this.y + 25 + extraY);
            context.drawText(this.textRenderer, this.nutritionTexts.get(i), this.x + 28, this.y + 26 + extraY, 0x3F3F3F, false);
            context.drawTexture(RenderInit.NUTRITION_ICONS, this.x + 27, this.y + 36 + extraY, 0, 206 + extraBarY, 141, 5);
            if (this.client.player != null) {
                if (NutritionClientPacket.levels[i] > 0) {
                    context.drawTexture(RenderInit.NUTRITION_ICONS, this.x + 27, this.y + 36 + extraY, 0, 211 + extraBarY,
                            140 * NutritionClientPacket.levels[i] / NutritionClientPacket.maxNutrition, 5);
                }
                context.drawText(this.textRenderer, Text.translatable("screen.nutritionz.nutritionValue", NutritionClientPacket.levels[i], NutritionClientPacket.maxNutrition), this.x + 127,
                        this.y + 26 + extraY, 0x3F3F3F, false);
                List<Text> tooltips = new ArrayList<>();
                int negativeWidth = 140 * NutritionClientPacket.negativeNutrition / NutritionClientPacket.maxNutrition;
                int positiveStart = 140 * NutritionClientPacket.positiveNutrition / NutritionClientPacket.maxNutrition;
                if (isPointWithinBounds(27, 36 + extraY, negativeWidth, 5, mouseX, mouseY)) {
                    addEffectTooltips(tooltips, NutritionClientPacket.NEGATIVE_EFFECTS.get(i));
                } else if (isPointWithinBounds(27 + positiveStart, 36 + extraY, 140 - positiveStart, 5, mouseX, mouseY)) {
                    addEffectTooltips(tooltips, NutritionClientPacket.POSITIVE_EFFECTS.get(i));
                }
                if (!tooltips.isEmpty()) {
                    context.drawTooltip(textRenderer, tooltips, mouseX, mouseY);
                }
            }
            extraY += 23;
            extraBarY += 10;
        }
        if (isPointWithinBounds(5, 5, 11, 10, mouseX, mouseY)) {
            context.drawTexture(RenderInit.NUTRITION_ICONS, this.x + 5, this.y + 5, 187, 0, 11, 10);
        } else {
            context.drawTexture(RenderInit.NUTRITION_ICONS, this.x + 5, this.y + 5, 176, 0, 11, 10);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isPointWithinBounds(5, 5, 11, 10, mouseX, mouseY)) {
            this.client.setScreen(new InventoryScreen(this.client.player));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static void addEffectTooltips(List<Text> tooltips, NutritionEffects effects) {
        if (effects != null) {
            effects.statusEffects().forEach(effect -> tooltips.add(effect.getEffectType().getName()));
            effects.attributes().keySet().forEach(attribute -> tooltips.add(Text.translatable(attribute.getTranslationKey())));
        }
    }

    private boolean isPointWithinBounds(int x, int y, int width, int height, double pointX, double pointY) {
        int i = this.x;
        int j = this.y;
        return (pointX -= (double) i) >= (double) (x - 1) && pointX < (double) (x + width + 1) && (pointY -= (double) j) >= (double) (y - 1) && pointY < (double) (y + height + 1);
    }

}
