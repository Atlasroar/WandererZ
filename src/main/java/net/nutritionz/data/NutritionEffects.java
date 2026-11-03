package net.nutritionz.data;

import java.util.List;

import com.google.common.collect.Multimap;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffectInstance;

public record NutritionEffects(List<StatusEffectInstance> statusEffects,
        Multimap<EntityAttribute, EntityAttributeModifier> attributes) {
}
