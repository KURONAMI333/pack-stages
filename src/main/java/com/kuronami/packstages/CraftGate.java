package com.kuronami.packstages;

import com.kuronami.packstages.data.StageDefinition;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Stops a player taking a craft-locked item out of an output slot.
 *
 * <p>An output slot is any slot outside the player's inventory that refuses to have its own item put
 * back ({@code mayPlace} is false): the crafting-table and inventory result, furnace/smoker/blast
 * furnace output, smithing, stonecutter, loom, and most modded workstations built on vanilla slots.
 * Ordinary storage slots accept their own item, so chests are never affected.
 *
 * <p>Screens that hand back an item the player already had (anvil, grindstone), sell it (villager
 * trades) or hold one equipment item (horse saddle/armour) are not treated as making something.
 */
public final class CraftGate {
    private CraftGate() {}

    /** Click/shift-click/hotbar-swap/drop on a locked output: refuse and say why. */
    public static boolean refuseClick(AbstractContainerMenu menu, int slotId, ClickType clickType, Player player) {
        if (clickType != ClickType.PICKUP && clickType != ClickType.QUICK_MOVE
                && clickType != ClickType.SWAP && clickType != ClickType.THROW) {
            return false;
        }
        if (slotId < 0 || slotId >= menu.slots.size()) return false;
        Slot slot = menu.slots.get(slotId);
        StageDefinition need = lockedOutput(menu, slot, player);
        if (need == null) return false;
        StageAccess.notifyLocked(player, "craft", slot.getItem().getHoverName(), need);
        return true;
    }

    /** Double-click gathering pulls matching stacks from every slot; skip locked outputs. */
    public static boolean blocksPickAll(AbstractContainerMenu menu, Slot slot, Player player) {
        return lockedOutput(menu, slot, player) != null;
    }

    private static StageDefinition lockedOutput(AbstractContainerMenu menu, Slot slot, Player player) {
        if (slot.container instanceof Inventory) return null;
        if (menu instanceof AnvilMenu || menu instanceof GrindstoneMenu
                || menu instanceof MerchantMenu || menu instanceof HorseInventoryMenu) {
            return null;
        }
        ItemStack stack = slot.getItem();
        if (stack.isEmpty() || slot.mayPlace(stack)) return null;
        return StageAccess.locks(player).missingForCraft(stack.getItem(), StageAccess.stages(player));
    }
}
