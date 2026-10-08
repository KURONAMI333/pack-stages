package com.kuronami.packstages.client;

import com.kuronami.packstages.PackStages;
import com.kuronami.packstages.data.StageDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/** Client-only hooks: the "unlocks at" tooltip line, hiding locked screens, clearing state on disconnect. */
@EventBusSubscriber(modid = PackStages.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().isEmpty()) return;
        List<StageDefinition> missing = ClientStageState.locks().allMissing(event.getItemStack().getItem(), ClientStageState.stages());
        if (missing.isEmpty()) return;
        MutableComponent names = Component.empty();
        for (int i = 0; i < missing.size(); i++) {
            if (i > 0) names.append(", ");
            names.append(missing.get(i).displayName());
        }
        event.getToolTip().add(Component.translatable("pack_stages.tooltip.locked", names).withStyle(ChatFormatting.GOLD));
    }

    /**
     * The server closes locked screens as soon as they open; cancelling here as well keeps the
     * client from showing them for a frame first.
     */
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof AbstractContainerScreen<?> screen)) return;
        MenuType<?> type;
        try {
            type = screen.getMenu().getType();
        } catch (UnsupportedOperationException e) {
            return; // player inventory and other untyped menus
        }
        if (ClientStageState.locks().missingForMenu(type, ClientStageState.stages()) != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientStageState.reset();
    }
}
