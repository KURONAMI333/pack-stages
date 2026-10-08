package com.kuronami.packstages;

import com.kuronami.packstages.data.StageDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Refuses use of locked items, right-clicks on locked blocks and opening locked screens.
 * Interaction checks run on both sides so the client predicts the refusal; only the server talks.
 * Locked menu types are closed by the server here and hidden on the client by {@code ClientEvents}.
 */
public final class RestrictionEvents {
    private RestrictionEvents() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        BlockState state = event.getLevel().getBlockState(event.getPos());
        StageDefinition blockNeed = StageAccess.locks(player).missingForBlock(state.getBlock(), StageAccess.stages(player));
        if (blockNeed != null) {
            StageAccess.notifyLocked(player, "block", state.getBlock().getName(), blockNeed);
            if (!player.isSecondaryUseActive()) {
                // A plain right-click meant "open this"; don't let the held item be placed on it instead.
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                return;
            }
            // Sneaking: like vanilla, the block isn't used and the held item may still be placed.
            event.setUseBlock(TriState.FALSE);
        }
        ItemStack stack = event.getItemStack();
        StageDefinition itemNeed = useNeed(player, stack);
        if (itemNeed != null) {
            event.setUseItem(TriState.FALSE);
            // Only complain about the item when it is what the player was trying to use.
            if (blockNeed == null) StageAccess.notifyLocked(player, "use", stack.getHoverName(), itemNeed);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        StageDefinition need = useNeed(player, event.getItemStack());
        if (need != null) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            StageAccess.notifyLocked(player, "use", event.getItemStack().getHoverName(), need);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        StageDefinition need = useNeed(player, event.getItemStack());
        if (need != null) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            StageAccess.notifyLocked(player, "use", event.getItemStack().getHoverName(), need);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        Player player = event.getEntity();
        StageDefinition need = useNeed(player, event.getItemStack());
        if (need != null) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    /** Screens opened by any route (items, other blocks, other mods). Fires on the server only. */
    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        Player player = event.getEntity();
        AbstractContainerMenu menu = event.getContainer();
        if (menu instanceof InventoryMenu) return;
        MenuType<?> type;
        try {
            type = menu.getType();
        } catch (UnsupportedOperationException e) {
            return; // menus without a registered type
        }
        StageDefinition need = StageAccess.locks(player).missingForMenu(type, StageAccess.stages(player));
        if (need != null) {
            player.closeContainer();
            var key = BuiltInRegistries.MENU.getKey(type);
            StageAccess.notifyLocked(player, "menu", Component.literal(key == null ? "?" : key.toString()), need);
        }
    }

    private static StageDefinition useNeed(Player player, ItemStack stack) {
        if (stack.isEmpty()) return null;
        return StageAccess.locks(player).missingForUse(stack.getItem(), StageAccess.stages(player));
    }
}
