package net.nutritionz;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.GameMode;
import net.nutritionz.access.HungerManagerAccess;
import net.nutritionz.data.NutritionLoader;
import net.nutritionz.init.ConfigInit;

public class NutritionGameTest implements FabricGameTest {

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void defaultsAndPersistence(TestContext context) {
        HungerManager hunger = new HungerManager();
        HungerManagerAccess nutrients = (HungerManagerAccess) hunger;
        NbtCompound oldSave = new NbtCompound();
        hunger.readNbt(oldSave);
        for (int i = 0; i < 5; i++) {
            context.assertTrue(nutrients.getNutritionLevel(i) == ConfigInit.CONFIG.maxNutrition / 2, "Old saves must start with neutral nutrition");
            nutrients.setNutritionLevel(i, 41 + i);
        }
        NbtCompound saved = new NbtCompound();
        hunger.writeNbt(saved);
        context.assertTrue(saved.contains("FatLevel", NbtElement.INT_TYPE), "All nutrition values must be saved as integers");
        HungerManager restored = new HungerManager();
        restored.readNbt(saved);
        for (int i = 0; i < 5; i++) {
            context.assertTrue(((HungerManagerAccess) restored).getNutritionLevel(i) == 41 + i, "Nutrition must survive save/load");
        }
        oldSave.putFloat("FatLevel", 99.0F);
        restored.readNbt(oldSave);
        context.assertTrue(((HungerManagerAccess) restored).getNutritionLevel(2) == 99, "Legacy upstream float saves must remain readable");
        nutrients.setNutritionLevel(0, -20);
        context.assertTrue(nutrients.getNutritionLevel(0) == 0, "Nutrition must not fall below zero");
        nutrients.addNutritionLevel(0, ConfigInit.CONFIG.maxNutrition + 10);
        context.assertTrue(nutrients.getNutritionLevel(0) == ConfigInit.CONFIG.maxNutrition, "Nutrition must not exceed its maximum");
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void foodIsCountedOnce(TestContext context) {
        ServerPlayerEntity player = player(context);
        HungerManagerAccess hunger = (HungerManagerAccess) player.getHungerManager();
        List<Integer> expected = NutritionMain.NUTRITION_ITEM_MAP.get(Items.APPLE);
        context.assertTrue(expected != null, "Vanilla food data must load");
        player.eatFood(context.getWorld(), Items.APPLE.getDefaultStack());
        for (int i = 0; i < 5; i++) {
            context.assertTrue(hunger.getNutritionLevel(i) == ConfigInit.CONFIG.maxNutrition / 2 + expected.get(i), "Food nutrients must be applied exactly once");
        }
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void cakeAddsNutrition(TestContext context) {
        ServerPlayerEntity player = player(context);
        player.getHungerManager().setFoodLevel(10);
        BlockPos pos = context.getAbsolutePos(new BlockPos(1, 2, 1));
        context.getWorld().setBlockState(pos.down(), Blocks.STONE.getDefaultState());
        context.getWorld().setBlockState(pos, Blocks.CAKE.getDefaultState());
        Blocks.CAKE.getDefaultState().onUse(context.getWorld(), player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
        HungerManagerAccess hunger = (HungerManagerAccess) player.getHungerManager();
        context.assertTrue(hunger.getNutritionLevel(0) == ConfigInit.CONFIG.maxNutrition / 2 + 4, "A cake slice must provide nutrition");
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void effectsTransitionAndReload(TestContext context) {
        ServerPlayerEntity player = player(context);
        HungerManagerAccess hunger = (HungerManagerAccess) player.getHungerManager();
        double base = player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        hunger.setNutritionLevel(1, ConfigInit.CONFIG.positiveNutrition);
        updateAtSecond(player);
        context.assertTrue(player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) == base + 2, "Positive protein effect must apply");
        updateAtSecond(player);
        context.assertTrue(player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) == base + 2, "Attribute modifiers must not stack");
        hunger.setNutritionLevel(1, ConfigInit.CONFIG.maxNutrition / 2);
        updateAtSecond(player);
        context.assertTrue(player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) == base, "Neutral nutrition must remove modifiers");
        hunger.setNutritionLevel(1, ConfigInit.CONFIG.positiveNutrition);
        updateAtSecond(player);
        UUID positiveModifier = NutritionMain.NUTRITION_POSITIVE_EFFECTS.get(1).attributes().get(EntityAttributes.GENERIC_ATTACK_DAMAGE).iterator().next().getId();
        hunger.setNutritionLevel(1, ConfigInit.CONFIG.negativeNutrition);
        updateAtSecond(player);
        UUID negativeModifier = NutritionMain.NUTRITION_NEGATIVE_EFFECTS.get(1).attributes().get(EntityAttributes.GENERIC_ATTACK_DAMAGE).iterator().next().getId();
        context.assertTrue(player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getModifier(positiveModifier) == null, "Switching extremes must remove the old modifier");
        context.assertTrue(player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getModifier(negativeModifier) != null, "Switching extremes must add the new modifier");
        double negativeBonus = NutritionMain.NUTRITION_NEGATIVE_EFFECTS.get(1).attributes().get(EntityAttributes.GENERIC_ATTACK_DAMAGE).iterator().next().getValue();
        context.assertTrue(player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) == base + negativeBonus, "Switching directly between extremes must replace modifiers");
        var oldEffects = NutritionMain.NUTRITION_NEGATIVE_EFFECTS.get(1);
        NutritionMain.NUTRITION_ITEM_MAP.put(Items.STONE, List.of(1, 1, 1, 1, 1));
        new NutritionLoader().reload(context.getWorld().getServer().getResourceManager());
        context.assertTrue(!NutritionMain.NUTRITION_ITEM_MAP.containsKey(Items.STONE), "Reload must clear stale food data");
        context.assertTrue(oldEffects != NutritionMain.NUTRITION_NEGATIVE_EFFECTS.get(1), "Reload must rebuild effects");
        updateAtSecond(player);
        context.assertTrue(player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) == base + negativeBonus, "Reload must replace existing effects without stacking");
        player.changeGameMode(GameMode.CREATIVE);
        updateAtSecond(player);
        context.assertTrue(player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) == base, "Creative players must not retain nutrition attributes");
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void disabledNutritionIsInert(TestContext context) {
        ServerPlayerEntity player = player(context);
        HungerManagerAccess hunger = (HungerManagerAccess) player.getHungerManager();
        boolean enabled = net.wandererz.init.ConfigInit.CONFIG.enableNutritionZ;
        try {
            net.wandererz.init.ConfigInit.CONFIG.enableNutritionZ = false;
            hunger.setNutritionLevel(1, ConfigInit.CONFIG.positiveNutrition);
            double base = player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
            int vitamins = hunger.getNutritionLevel(3);
            player.eatFood(context.getWorld(), Items.APPLE.getDefaultStack());
            context.assertTrue(hunger.getNutritionLevel(3) == vitamins, "Disabled nutrition must not count food");
            player.getHungerManager().setFoodLevel(10);
            player.getHungerManager().setSaturationLevel(0);
            player.getHungerManager().addExhaustion(4.1F);
            updateAtSecond(player);
            context.assertTrue(player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) == base, "Disabled nutrition must not affect player attributes");
            context.assertTrue(hunger.getNutritionLevel(1) == ConfigInit.CONFIG.positiveNutrition, "Disabled nutrition must not deplete nutrients");
        } finally {
            net.wandererz.init.ConfigInit.CONFIG.enableNutritionZ = enabled;
        }
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void exhaustionDepletesNutrition(TestContext context) {
        ServerPlayerEntity player = player(context);
        player.getHungerManager().setFoodLevel(10);
        player.getHungerManager().setSaturationLevel(0);
        player.getHungerManager().addExhaustion(4.1F);
        player.getHungerManager().update(player);
        for (int i = 0; i < 5; i++) {
            context.assertTrue(((HungerManagerAccess) player.getHungerManager()).getNutritionLevel(i) == ConfigInit.CONFIG.maxNutrition / 2 - 1,
                    "Hunger exhaustion must deplete each nutrient once");
        }
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void datapackOverridesDefaults(TestContext context) {
        context.assertTrue(NutritionMain.NUTRITION_ITEM_MAP.get(Items.DRIED_KELP).equals(List.of(7, 0, 0, 0, 9)), "Custom datapacks must override bundled food values");
        context.assertTrue(NutritionMain.NUTRITION_POSITIVE_EFFECTS.get(4).attributes().isEmpty(), "Replace must remove bundled nutrient attributes");
        context.assertTrue(NutritionMain.NUTRITION_POSITIVE_EFFECTS.get(4).statusEffects().get(0).getAmplifier() == 1, "Datapack status effects must load");
        context.assertTrue(NutritionMain.NUTRITION_NEGATIVE_EFFECTS.get(4).statusEffects().isEmpty(), "Empty effects must disable that side");
        new NutritionLoader().reload(context.getWorld().getServer().getResourceManager());
        context.assertTrue(NutritionMain.NUTRITION_ITEM_MAP.get(Items.DRIED_KELP).equals(List.of(7, 0, 0, 0, 9)), "Custom overrides must survive repeated reloads");
        context.complete();
    }

    private static ServerPlayerEntity player(TestContext context) {
        var server = context.getWorld().getServer();
        ServerPlayerEntity player = new ServerPlayerEntity(server, context.getWorld(), new GameProfile(UUID.randomUUID(), "NutritionTest"));
        player.networkHandler = new ServerPlayNetworkHandler(server, new ClientConnection(NetworkSide.SERVERBOUND), player);
        return player;
    }

    private static void updateAtSecond(ServerPlayerEntity player) {
        long time = player.getWorld().getTime();
        ServerWorldProperties properties = (ServerWorldProperties) player.getWorld().getLevelProperties();
        try {
            properties.setTime(time - time % 20);
            player.getHungerManager().update(player);
        } finally {
            properties.setTime(time);
        }
    }
}
