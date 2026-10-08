package com.kuronami.packstages.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stage definitions resolved against the registries and tags: for each thing, which stages it needs.
 * Built on both sides (server for enforcement, client for tooltips, JEI and click prediction).
 */
public final class Locks {
    public static final Locks EMPTY = new Locks(List.of());

    private final Map<Item, List<StageDefinition>> craft = new HashMap<>();
    private final Map<Item, List<StageDefinition>> use = new HashMap<>();
    private final Map<Block, List<StageDefinition>> blocks = new HashMap<>();
    private final Map<MenuType<?>, List<StageDefinition>> menus = new HashMap<>();
    private final Map<Item, List<StageDefinition>> jeiHidden = new HashMap<>();

    public Locks(Collection<StageDefinition> definitions) {
        for (StageDefinition def : definitions) {
            String where = "stage " + def.name();
            Set<Item> both = EntryMatcher.resolve(BuiltInRegistries.ITEM, def.items(), where);
            Set<Item> craftOnly = EntryMatcher.resolve(BuiltInRegistries.ITEM, def.craft(), where);
            Set<Item> useOnly = EntryMatcher.resolve(BuiltInRegistries.ITEM, def.use(), where);
            for (Item item : both) {
                add(craft, item, def);
                add(use, item, def);
            }
            craftOnly.forEach(item -> add(craft, item, def));
            useOnly.forEach(item -> add(use, item, def));
            EntryMatcher.resolve(BuiltInRegistries.BLOCK, def.blocks(), where).forEach(b -> add(blocks, b, def));
            EntryMatcher.resolve(BuiltInRegistries.MENU, def.menus(), where).forEach(m -> add(menus, m, def));
            if (def.jei() == StageDefinition.JeiMode.HIDE) {
                both.forEach(item -> add(jeiHidden, item, def));
                craftOnly.forEach(item -> add(jeiHidden, item, def));
                useOnly.forEach(item -> add(jeiHidden, item, def));
            }
        }
    }

    public boolean isEmpty() {
        return craft.isEmpty() && use.isEmpty() && blocks.isEmpty() && menus.isEmpty();
    }

    /** First stage the player lacks to take {@code item} out of a crafting/output slot, or null. */
    public StageDefinition missingForCraft(Item item, Set<String> stages) {
        return firstMissing(craft.get(item), stages);
    }

    public StageDefinition missingForUse(Item item, Set<String> stages) {
        return firstMissing(use.get(item), stages);
    }

    public StageDefinition missingForBlock(Block block, Set<String> stages) {
        return firstMissing(blocks.get(block), stages);
    }

    public StageDefinition missingForMenu(MenuType<?> type, Set<String> stages) {
        return firstMissing(menus.get(type), stages);
    }

    /** Every stage still needed before the item (or the block it places) is fully usable. Tooltip helper. */
    public List<StageDefinition> allMissing(Item item, Set<String> stages) {
        List<StageDefinition> out = new ArrayList<>();
        collect(out, craft.get(item), stages);
        collect(out, use.get(item), stages);
        if (item instanceof BlockItem blockItem) collect(out, blocks.get(blockItem.getBlock()), stages);
        return out;
    }

    /** Items JEI should hide for a player with {@code stages}. */
    public Set<Item> jeiHidden(Set<String> stages) {
        Set<Item> out = new java.util.HashSet<>();
        jeiHidden.forEach((item, defs) -> {
            if (firstMissing(defs, stages) != null) out.add(item);
        });
        return out;
    }

    private static StageDefinition firstMissing(List<StageDefinition> defs, Set<String> stages) {
        if (defs == null) return null;
        for (StageDefinition def : defs) {
            if (!stages.contains(def.name())) return def;
        }
        return null;
    }

    private static void collect(List<StageDefinition> out, List<StageDefinition> defs, Set<String> stages) {
        if (defs == null) return;
        for (StageDefinition def : defs) {
            if (!stages.contains(def.name()) && !out.contains(def)) out.add(def);
        }
    }

    private static <K> void add(Map<K, List<StageDefinition>> map, K key, StageDefinition def) {
        List<StageDefinition> list = map.computeIfAbsent(key, k -> new ArrayList<>(1));
        if (!list.contains(def)) list.add(def);
    }
}
