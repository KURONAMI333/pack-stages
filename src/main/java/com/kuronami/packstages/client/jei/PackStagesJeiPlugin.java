package com.kuronami.packstages.client.jei;

import com.kuronami.packstages.PackStages;
import com.kuronami.packstages.client.ClientStageState;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

/**
 * Hides items the player can't make or use yet from JEI's list (stages with {@code "jei": "hide"}),
 * and puts them back as stages are granted. Only loaded when JEI is installed.
 */
@JeiPlugin
public final class PackStagesJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(PackStages.MODID, "jei");
    private static IJeiRuntime runtime;
    /** Item -> the exact stacks removed from JEI for it. */
    private static final Map<Item, List<ItemStack>> HIDDEN = new HashMap<>();
    private static boolean listening;

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        HIDDEN.clear(); // a fresh runtime lists everything again
        if (!listening) {
            listening = true;
            ClientStageState.addListener(() -> Minecraft.getInstance().execute(PackStagesJeiPlugin::refresh));
        }
        refresh();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        HIDDEN.clear();
    }

    private static void refresh() {
        IJeiRuntime rt = runtime;
        if (rt == null) return;
        Set<Item> want = ClientStageState.locks().jeiHidden(ClientStageState.stages());

        // Put back exactly what we took out (every variant), nothing JEI didn't list itself.
        List<ItemStack> show = new ArrayList<>();
        HIDDEN.entrySet().removeIf(e -> {
            if (want.contains(e.getKey())) return false;
            show.addAll(e.getValue());
            return true;
        });
        if (!show.isEmpty()) rt.getIngredientManager().addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, show);

        // Take out every listed stack of a newly locked item (potions, enchanted books, mod subtypes).
        List<ItemStack> hide = new ArrayList<>();
        Set<Item> newlyLocked = new HashSet<>(want);
        newlyLocked.removeAll(HIDDEN.keySet());
        if (!newlyLocked.isEmpty()) {
            for (ItemStack stack : rt.getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK)) {
                if (newlyLocked.contains(stack.getItem())) {
                    hide.add(stack);
                    HIDDEN.computeIfAbsent(stack.getItem(), k -> new ArrayList<>()).add(stack);
                }
            }
            if (!hide.isEmpty()) rt.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hide);
        }
        if (!hide.isEmpty() || !show.isEmpty()) {
            PackStages.LOGGER.info("[pack_stages] JEI: hid {} stack(s), restored {} (items now hidden: {})",
                    hide.size(), show.size(), HIDDEN.size());
        }
    }
}
