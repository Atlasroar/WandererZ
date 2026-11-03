![WandererZ](docs/images/wandererz-title.png)

# WandererZ

WandererZ (formerly LevelZ) is a Fabric mod which adds player levels and skills, with a job system, RPG-style
mob difficulty scaling, an item tiering/reforging system, player parties, seasons-based food spoilage,
and nutrition built directly in — no separate addon downloads required.

### Features

- **Levels & Skills** — gain XP and level up skills (health, strength, agility, defense, stamina, luck,
  archery, trade, smithing, mining, farming, alchemy) that unlock blocks, items, and entities as you progress.
- **Jobs** — take on one of eight jobs (Miner, Farmer, Lumberjack, Fisher, Builder, Brewer, Smither, Warrior),
  earn job XP for job-specific actions, and level them up independently of your skills.
- **RPG Difficulty** — mobs get stronger with distance, time played, and height, with extra boss scaling and
  bonus XP/loot for tougher fights. Scaling is driven by your WandererZ player level.
- **Tiered** — tools, weapons, armor, and shields can randomly roll one of six rarity tiers (common through
  unique) with bonus attributes, plus an anvil-based reforging system to reroll an item's tier (modified by
  your smithing skill level and luck).
- **Party** — form parties with other players (invite, join, leave, kick), and share vanilla and WandererZ
  XP picked up by any group member, with a configurable group-size cap, group bonuses, and an on-screen HUD.
- **Food Spoilage** — food items spoil over time based on the current season (via Fabric Seasons or Serene
  Seasons, either one works), with cake, furnace/campfire/crafting results, items on the ground, and
  container contents all tracked; fully spoiled (0% freshness) food turns into Rotten Flesh, and eating food
  at 25% freshness or lower always inflicts the same fixed Poison/Nausea effect (never random).
- **Nutrition** — balance carbohydrates, protein, fat, vitamins, and minerals through food and cake.
  Nutrients decrease when hunger is depleted by exhaustion; high and low levels apply datapack-defined
  attribute/status effects. Open the nutrient icon in your survival inventory to view your meters,
  or hold Shift over food to see its nutrients. Food values, effects, and 55 optional food/drink
  compatibility packs are included from NutritionZ.
- **Deep configurability** — every system (leveling, jobs, difficulty scaling, tiering, parties, spoilage, nutrition)
  ships its own config screen via Mod Menu / Cloth Config, and most default settings can be overridden
  per-world with datapacks. Each merged integration (Jobs, RPG Difficulty, Tiered, Party, Food Spoilage, Nutrition) can
  also be individually toggled on/off from the WandererZ config's "Integrations" tab (requires a restart).
- **Wide compatibility** — built-in support/compat hooks for Mod Menu, Trinkets, REI, EMI, Jade, WTHIT,
  Origins, TreeChop, Farmer's Delight, Expanded Delight, Vinery, FallingTree, Numismatic Overhaul,
  EasyAnvils, TooltipFix, Clumps, Xaero's World Map, Capsaicin (bundled), and more.

### Installation

WandererZ is a mod built for the [Fabric Loader](https://fabricmc.net/). It requires
[Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api),
[Cloth Config API](https://www.curseforge.com/minecraft/mc-mods/cloth-config),
[LibZ](https://www.curseforge.com/minecraft/mc-mods/libz), and
[AutoTag](https://www.curseforge.com/minecraft/mc-mods/autotag) to be installed separately. The food
spoilage system additionally needs a season/calendar provider — either
[Fabric Seasons](https://modrinth.com/mod/fabric-seasons) or [Serene Seasons](https://modrinth.com/mod/serene-seasons)
(which also requires [GlitchCore](https://modrinth.com/mod/glitchcore)) — installed separately; either one works,
and if neither is present, spoilage timing is simply disabled rather than crashing. All other dependencies
(including Capsaicin, which powers the food spoilage system) are bundled or optional compatibility hooks
installed with the mod. Nutrition uses the already-bundled Capsaicin events and has no season-provider
requirement; Dehydration is optional for drink nutrition. Do not install standalone NutritionZ alongside
WandererZ, as its functionality is already included.
[Mod Menu](https://www.curseforge.com/minecraft/mc-mods/modmenu) is optional but recommended — if present,
it gives you an in-game "WandererZ" entry that opens a chooser for the WandererZ, Jobs, RPG Difficulty,
Tiered, Party, Food Spoilage, and Nutrition config screens. Without it, those configs can still be edited
directly under `config/WandererZ/`. Nutrition uses `nutritionz.json5`, and its restart toggle is
`enableNutritionZ` in `wandererz.json5`. A legacy `config/nutritionz.json5` is migrated automatically.

### License

WandererZ is licensed under GPLv3. The merged Jobs, RPG Difficulty, Tiered, Party, Food Spoilage, and Nutrition systems
retain their original upstream license terms (GPLv3 and MIT) in their source headers/notices.
Nutrition code, textures, translations, and compatibility data were imported from
[xR4YM0ND/NutritionZ's 1.20 branch](https://github.com/xR4YM0ND/NutritionZ/tree/de5d32a33840bb39ee0aaa1bd64ea5cb14435eec)
(authors Globox_Z and xR4YM0ND). Its full MIT notice is preserved in `LICENSE-NutritionZ` and distributed
inside the built jar. Local integration changes cover merged entrypoints/configuration, Capsaicin
consumption, persistence, synchronization, reloads, and attribute lifecycle handling.

### Documentation

Full player and developer documentation — including per-skill/job/difficulty/tiering/party/spoilage
configuration options, the commands reference, and **datapack creation guides for the original five merged
mods** — lives on
the [project wiki](https://github.com/Atlasroar/LevelZ/wiki). Start with the
[Player Guide](https://github.com/Atlasroar/LevelZ/wiki/Player-Guide) or the
[Developer Guide](https://github.com/Atlasroar/LevelZ/wiki/Developer-Guide).

### Nutrition configuration and datapacks

The default maximum is 300 per nutrient, with low/high thresholds of 30/270. New and existing players
without nutrition save data start at half the maximum. Server values and thresholds are authoritative;
inventory-button coordinates and icon item IDs are client preferences. Nutrition bonuses are independent
of skill, equipment, and job bonuses and do not apply in creative or spectator mode.

Add food values in `data/<namespace>/nutrition/<name>.json`:

```json
{
  "minecraft:apple": {
    "carbohydrates": 0,
    "protein": 0,
    "fat": 0,
    "vitamins": 15,
    "minerals": 5
  }
}
```

Values must be nonnegative integers; omitted nutrients default to zero. A later definition replaces the
entire five-value entry for that item. For effects, use
`data/<namespace>/nutrition_manager/<name>.json`, for example:

```json
{
  "protein": {
    "replace": true,
    "positive": {
      "minecraft:generic.attack_damage": { "operation": "ADDITION", "value": 2.0 }
    },
    "negative": {
      "minecraft:weakness": { "duration": 319, "amplifier": 0 }
    }
  }
}
```

The five group keys are `carbohydrates`, `protein`, `fat`, `vitamins`, and `minerals`. Each provided
`positive`/`negative` object replaces that side's effects; `replace: true` clears both sides first.
Use `{}` for a side with no effects. Attribute operations are `ADDITION`, `MULTIPLY_BASE`, or
`MULTIPLY_TOTAL`; status effects use a tick duration and zero-based amplifier.

Matching resource paths follow Minecraft's normal pack priority. For different paths, the bundled
`nutritionz` namespace loads first, followed by other namespaces in identifier order, so custom
definitions can override defaults. `/reload` rebuilds nutrition data, removes obsolete attribute bonuses,
and resends food tooltip data to connected clients. Optional compatibility packs enable by default when
their corresponding mod is installed and appear as `wandererz:*_nutrition_compat` datapacks.

Developers can run the headless Fabric integration suite with `.\gradlew.bat runGameTest` on Windows
or `./gradlew runGameTest` on other platforms. Test mod sources are separate from the distributable jar.

### Commands

`/playerstats playername add skill integer` — Increase the specific skill by the integer value\
`/playerstats playername remove skill integer` — Decrease the specific skill by the integer value\
`/playerstats playername set skill integer` — Set the specific skill to the integer value\
`/playerstats playername get skill` — Print the specific skill level\
`/jobmanager playername add jobname integer` — Increase the specific job by the integer value\
`/jobmanager playername remove jobname integer` — Decrease the specific job by the integer value\
`/jobmanager playername set jobname integer` — Set the specific job to the integer value\
`/jobmanager playername get jobname` — Print the specific job level\
`/info material` — Print the material string of the item in hand\
`/tiered tier playername common|uncommon|rare|epic|legendary|unique` — Force the player's held item to a tier\
`/tiered untier playername` — Remove the tier from the player's held item\
`/party join` — Accept your current group invitation\
`/party leave` — Leave your current group
