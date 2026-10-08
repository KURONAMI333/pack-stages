package com.kuronami.packstages.mixin;

import com.kuronami.packstages.CraftGate;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every slot click in every screen goes through doClick; refuse taking locked items out of output slots. */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    private void pack_stages$gateOutput(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        if (CraftGate.refuseClick((AbstractContainerMenu) (Object) this, slotId, clickType, player)) {
            ci.cancel();
        }
    }

    /**
     * Double-click gathering. Wrapped at the call site, so menus that override canTakeItemForPickAll
     * without calling super (most furnace-like modded screens never exclude their output) are covered.
     */
    @WrapOperation(method = "doClick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;canTakeItemForPickAll(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/inventory/Slot;)Z"))
    private boolean pack_stages$gatePickAll(AbstractContainerMenu menu, ItemStack carried, Slot slot, Operation<Boolean> original,
                                            int slotId, int button, ClickType clickType, Player player) {
        return original.call(menu, carried, slot) && !CraftGate.blocksPickAll(menu, slot, player);
    }
}
