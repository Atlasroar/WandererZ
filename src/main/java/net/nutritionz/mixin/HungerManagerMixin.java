package net.nutritionz.mixin;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.MathHelper;
import net.nutritionz.NutritionMain;
import net.nutritionz.access.HungerManagerAccess;
import net.nutritionz.data.NutritionEffects;
import net.nutritionz.init.ConfigInit;

@Mixin(HungerManager.class)
public class HungerManagerMixin implements HungerManagerAccess {

    @Unique
    private static final String[] NUTRITION_KEYS = {"CarbohydrateLevel", "ProteinLevel", "FatLevel", "VitaminLevel", "MineralLevel"};
    @Unique
    private final int[] nutritionz$levels = {ConfigInit.CONFIG.maxNutrition / 2, ConfigInit.CONFIG.maxNutrition / 2,
            ConfigInit.CONFIG.maxNutrition / 2, ConfigInit.CONFIG.maxNutrition / 2, ConfigInit.CONFIG.maxNutrition / 2};
    @Unique
    private final Map<Integer, NutritionEffects> nutritionz$appliedEffects = new HashMap<>();

    @Inject(method = "update", at = @At(value = "INVOKE_ASSIGN", target = "Ljava/lang/Math;max(II)I", ordinal = 0))
    private void updateNutritionMixin(PlayerEntity player, CallbackInfo info) {
        if (NutritionMain.isEnabled()) {
            for (int i = 0; i < this.nutritionz$levels.length; i++) {
                decrementNutritionLevel(i, 1);
            }
        }
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void updateNutritionEffectsMixin(PlayerEntity player, CallbackInfo info) {
        if (!NutritionMain.isEnabled() || player.getWorld().isClient() || player.getWorld().getTime() % 20 != 0) {
            return;
        }
        for (int i = 0; i < this.nutritionz$levels.length; i++) {
            NutritionEffects effects = null;
            if (!player.isCreative() && !player.isSpectator()) {
                if (getNutritionLevel(i) <= ConfigInit.CONFIG.negativeNutrition) {
                    effects = NutritionMain.NUTRITION_NEGATIVE_EFFECTS.get(i);
                } else if (getNutritionLevel(i) >= ConfigInit.CONFIG.positiveNutrition) {
                    effects = NutritionMain.NUTRITION_POSITIVE_EFFECTS.get(i);
                }
            }
            NutritionEffects previous = this.nutritionz$appliedEffects.get(i);
            if (previous != effects) {
                if (previous != null) {
                    player.getAttributes().removeModifiers(previous.attributes());
                }
                if (effects != null) {
                    player.getAttributes().addTemporaryModifiers(effects.attributes());
                    this.nutritionz$appliedEffects.put(i, effects);
                } else {
                    this.nutritionz$appliedEffects.remove(i);
                }
            }
            if (effects != null) {
                for (StatusEffectInstance effect : effects.statusEffects()) {
                    StatusEffectInstance current = player.getStatusEffect(effect.getEffectType());
                    if (current == null || current.getDuration() < effect.getDuration() - 50) {
                        player.addStatusEffect(new StatusEffectInstance(effect));
                    }
                }
            }
        }
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void readNbtMixin(NbtCompound nbt, CallbackInfo info) {
        for (int i = 0; i < NUTRITION_KEYS.length; i++) {
            setNutritionLevel(i, nbt.contains(NUTRITION_KEYS[i], NbtElement.NUMBER_TYPE) ? nbt.getInt(NUTRITION_KEYS[i]) : ConfigInit.CONFIG.maxNutrition / 2);
        }
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void writeNbtMixin(NbtCompound nbt, CallbackInfo info) {
        for (int i = 0; i < NUTRITION_KEYS.length; i++) {
            nbt.putInt(NUTRITION_KEYS[i], this.nutritionz$levels[i]);
        }
    }

    @Override
    public void addNutritionLevel(int type, int level) {
        setNutritionLevel(type, (int) Math.max(0L, Math.min(ConfigInit.CONFIG.maxNutrition, (long) getNutritionLevel(type) + level)));
    }

    @Override
    public void decrementNutritionLevel(int type, int level) {
        setNutritionLevel(type, (int) Math.max(0L, Math.min(ConfigInit.CONFIG.maxNutrition, (long) getNutritionLevel(type) - level)));
    }

    @Override
    public void setNutritionLevel(int type, int level) {
        this.nutritionz$levels[type] = MathHelper.clamp(level, 0, ConfigInit.CONFIG.maxNutrition);
    }

    @Override
    public int getNutritionLevel(int type) {
        return this.nutritionz$levels[type];
    }
}
