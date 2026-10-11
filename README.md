# Pack Stages

Per-player stages for modpacks. The pack decides, in datapack JSON, what each stage unlocks: which items can be made, which items can be used, and which workstations (and their screens) can be opened. Stages are granted with a command, so any progression mod that can run commands (for example Pufferfish's Skills command rewards) can drive them without a code dependency.

- Minecraft 1.21.1, NeoForge
- Required on both server and client
- Optional: JEI (locked items are listed with an "Unlocks at" hint, or hidden)

## Commands

Permission level 2 (the level datapack functions and Pufferfish's Skills command rewards use).

| Command | |
|---|---|
| `/pack_stages add <players> <stage>` | Give a stage |
| `/pack_stages remove <players> <stage>` | Take a stage away |
| `/pack_stages clear <players>` | Remove all stages |
| `/pack_stages list [player]` | Show a player's stages |
| `/pack_stages has <player> <stage>` | Returns 1 or 0 (`execute if`/`store result`) |
| `/pack_stages stages` | Show the stages the loaded data defines |

Stages are saved with the player and kept on death.

## Defining stages

One file per stage: `data/<namespace>/pack_stages/stages/<path>.json`. The stage name is the path with `/` turned into `.` — `stages/potter/journeyman.json` is the stage `potter.journeyman`. A player **without** the stage is refused everything the file lists.

```json
{
  "display": "Potter - Journeyman",
  "blocks": ["lilis_pottery:kiln"],
  "craft": ["lilis_pottery:kiln", "lilis_pottery:*vase", "lilis_pottery:*amphore"],
  "use": [],
  "items": [],
  "menus": [],
  "jei": "show"
}
```

| Field | Effect without the stage |
|---|---|
| `display` | Name shown in messages and tooltips. Plain text or a text component (`{"translate": "...", "fallback": "..."}`). Defaults to the stage name |
| `craft` | The item can't be taken out of an output slot (click, shift-click, hotbar keys, drop, double-click gathering): crafting table and inventory grid, furnace/smoker/blast furnace, smithing table, stonecutter, loom, and modded workstations whose output slot refuses inserted items (for example Lili's Pottery's pottery table and kiln). Anvil, grindstone, villager trades and horse equipment are not treated as making something |
| `use` | The item can't be right-clicked, placed, or used on entities |
| `items` | Both `craft` and `use` |
| `blocks` | Right-clicking the block does nothing, so its screen doesn't open |
| `menus` | Screens of these menu types close as soon as they open, however they were opened |
| `jei` | `show` (default): locked items stay listed; the tooltip says what unlocks them. `hide`: they are removed from JEI's list until unlocked |

Every list accepts an id (`minecraft:stick`), a tag (`#c:ingots`), every entry of a mod (`@lilis_pottery`), or an id with `*` wildcards (`lilis_pottery:*_vase`). Ids of mods that aren't installed are ignored, so one data set can serve several packs. If several files (or datapacks) define the same stage name, their lists are combined. A thing listed under several stages needs all of them.

Players always see why they were refused, on the action bar: "You can't use the Kiln yet. Unlocks at: Potter - Journeyman".

## With Pufferfish's Skills

Use a command reward on the skill. `lock_command` runs when the skill is taken back (respec), so the stage follows the skill:

```json
"rewards": [
  { "type": "puffish_skills:command", "data": {
    "unlock_command": "pack_stages add @s potter.journeyman",
    "lock_command": "pack_stages remove @s potter.journeyman" } }
]
```

A complete example (a Potter profession with Lili's Pottery: apprentice unlocks the pottery table and basic pots, journeyman the kiln, vases and amphorae) is in [`examples/artisan_potter`](examples/artisan_potter).

## Not covered

- Machines that work without a player taking the result (hoppers pulling from a furnace, the Crafter, automation mods) are not gated. Lock the machine block itself if that matters.
- Mods whose output slot accepts inserted items, or that hand results over without a slot, need `blocks`/`menus` to gate the whole workstation instead of single items.
- A modded slot that holds a single item and refuses a second one looks like an output slot. Don't list such equipment items under `craft`/`items` unless refusing to take them out is what you want.

## License

All Rights Reserved. Modpack inclusion is welcome.

Feedback: the CurseForge comments, or DM [@kuronami333](https://x.com/kuronami333) on X.
