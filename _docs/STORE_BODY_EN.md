<!-- Summary 正本は _docs/STORE_SUMMARY_EN.txt。公開時はこのコメント行を本文に含めない -->
<!-- GitHub URL は公開予定の意図URL。repo 未作成の段階では 404 -->

Give each player stages, and decide in datapack JSON what a player without a stage can't do: make certain items, use certain items, or open certain workstations.

It is a tool for pack and server makers who want progression — a profession, a tech tier, an unlock tree — without writing code. Stages are granted with a command, so a quest reward, an advancement function or any mod that can run commands can hand them out.

## What a stage can lock

- **craft** — the item can't be taken out of an output slot: crafting table and inventory grid, furnaces, smithing table, stonecutter, loom, and modded workstations whose output slot refuses inserted items.
- **use** — the item can't be right-clicked, placed or used on entities.
- **items** — both `craft` and `use`.
- **blocks** — right-clicking the block does nothing, so its screen doesn't open.
- **menus** — screens of these types close as soon as they open, however they were opened.
- **JEI** (optional) — locked items stay listed with an "Unlocks at" line in the tooltip (`"jei": "show"`, the default), or are hidden until unlocked (`"hide"`).

Every list accepts item ids, tags (`#c:ingots`), all items of a mod (`@modid`) and `*` wildcards. Ids of mods that aren't installed are ignored, so one data set can serve several setups. A refused player always sees why on the action bar, for example "You can't use the Furnace yet. Unlocks at: Smelting".

## Defining a stage

One file per stage, `data/<namespace>/pack_stages/stages/<path>.json` — `stages/smith/apprentice.json` is the stage `smith.apprentice`:

```json
{
  "display": "Smelting",
  "blocks": ["minecraft:furnace", "minecraft:blast_furnace"],
  "craft": ["minecraft:furnace"],
  "jei": "show"
}
```

## Commands

`/pack_stages add|remove <players> <stage>`, `clear`, `list`, `has` (returns 1 or 0, for `execute store result`), and `stages` (what the loaded data defines). Permission level 2. Stages are saved with the player and kept on death.

## Limits

- Machines that work without a player taking the result — hoppers pulling from a furnace, the Crafter, automation mods — are not gated. Lock the machine block itself if that matters.
- Workstations whose output slot accepts inserted items need `blocks` or `menus` to gate the whole station instead of single items.
- Needed on both the server and the client.

<p><a href="https://www.patreon.com/KURONAMI333"><img src="https://media.forgecdn.net/attachments/description/1564253/description_0b5d69f3-bf0d-4a38-8894-6883abaa65e9.png" width="250" height="100" alt="Support my mods on Patreon"></a> <a href="https://x.com/kuronami333"><img src="https://media.forgecdn.net/attachments/description/1564253/description_0ed421f9-f6e1-4334-8e1e-fac25c804fe6.png" width="250" height="100" alt="Follow @kuronami333 on X"></a> <a href="https://www.youtube.com/channel/UCoimycJCVZmxNKyX9Vr7Apg"><img src="https://media.forgecdn.net/attachments/description/1564253/description_b953be14-e0c1-4290-b5d3-5ae75107ce90.png" width="250" height="100" alt="Watch KURONAMI on YouTube"></a></p>

All Rights Reserved. Free to put in any modpack, on any platform, monetised or not - no permission needed, no credit required. Source is published so you can read exactly what it does.
Bugs and questions: comment on the CurseForge page, or DM [@kuronami333 on X](https://x.com/kuronami333).
Source code: https://github.com/KURONAMI333/pack-stages
