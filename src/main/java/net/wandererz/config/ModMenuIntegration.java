package net.wandererz.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.jobsaddon.config.JobsAddonConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.partyaddon.config.PartyAddonConfig;
import net.rpgdifficulty.config.RpgDifficultyConfigScreen;
import net.spoiledz.config.SpoiledZConfig;
import net.nutritionz.config.NutritionzConfig;
import draylar.tiered.config.TieredConfig;

@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModMenuIntegration::createSelectionScreen;
    }

    private static Screen createSelectionScreen(Screen parent) {
        return new ConfigSelectionScreen(parent);
    }

    private static class ConfigSelectionScreen extends Screen {

        private final Screen parent;

        protected ConfigSelectionScreen(Screen parent) {
            super(Text.translatable("config.wandererz.title"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int centerX = this.width / 2;
            int y = this.height / 2 - 94;

            this.addDrawableChild(ButtonWidget.builder(Text.translatable("config.wandererz.category.wandererz"),
                    (button) -> this.client.setScreen(AutoConfig.getConfigScreen(WandererzConfig.class, this).get())).dimensions(centerX - 100, y, 200, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.translatable("config.wandererz.category.jobsaddon"),
                    (button) -> this.client.setScreen(AutoConfig.getConfigScreen(JobsAddonConfig.class, this).get())).dimensions(centerX - 100, y + 24, 200, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.translatable("config.wandererz.category.rpgdifficulty"),
                    (button) -> this.client.setScreen(RpgDifficultyConfigScreen.create(this))).dimensions(centerX - 100, y + 48, 200, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.translatable("config.wandererz.category.tiered"),
                    (button) -> this.client.setScreen(AutoConfig.getConfigScreen(TieredConfig.class, this).get())).dimensions(centerX - 100, y + 72, 200, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.translatable("config.wandererz.category.partyaddon"),
                    (button) -> this.client.setScreen(AutoConfig.getConfigScreen(PartyAddonConfig.class, this).get())).dimensions(centerX - 100, y + 96, 200, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.translatable("config.wandererz.category.spoiledz"),
                    (button) -> this.client.setScreen(AutoConfig.getConfigScreen(SpoiledZConfig.class, this).get())).dimensions(centerX - 100, y + 120, 200, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.translatable("config.wandererz.category.nutritionz"),
                    (button) -> this.client.setScreen(AutoConfig.getConfigScreen(NutritionzConfig.class, this).get())).dimensions(centerX - 100, y + 144, 200, 20).build());

            this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, (button) -> this.client.setScreen(this.parent)).dimensions(centerX - 100, y + 168, 200, 20).build());
        }

        @Override
        public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
            super.render(drawContext, mouseX, mouseY, delta);
            drawContext.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 118, 0xFFFFFF);
        }

        @Override
        public void close() {
            MinecraftClient.getInstance().setScreen(this.parent);
        }
    }
}