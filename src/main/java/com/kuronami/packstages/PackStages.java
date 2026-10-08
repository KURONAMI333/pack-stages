package com.kuronami.packstages;

import com.kuronami.packstages.client.ClientStageState;
import com.kuronami.packstages.data.ServerStageData;
import com.kuronami.packstages.network.SyncPayloads;
import com.kuronami.packstages.player.PlayerStages;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Per-player stages for modpacks. A pack describes in datapack JSON what each stage unlocks
 * (crafting, item use, workstations and their screens) and which first-time pickups run commands;
 * stages are granted with {@code /pack_stages add}, e.g. from a Pufferfish's Skills command reward.
 */
@Mod(PackStages.MODID)
public final class PackStages {
    public static final String MODID = "pack_stages";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    /** Saved with the player and kept through death. */
    public static final Supplier<AttachmentType<PlayerStages>> PLAYER_STAGES = ATTACHMENTS.register("stages",
            () -> AttachmentType.builder(PlayerStages::new).serialize(PlayerStages.CODEC).copyOnDeath().build());

    public PackStages(IEventBus modBus, ModContainer container) {
        ATTACHMENTS.register(modBus);
        modBus.addListener(SyncPayloads::register);

        IEventBus bus = NeoForge.EVENT_BUS;
        bus.register(RestrictionEvents.class);
        bus.register(MilestoneTracker.class);
        bus.addListener((RegisterCommandsEvent e) -> StagesCommand.register(e.getDispatcher()));
        bus.addListener((AddReloadListenerEvent e) -> {
            e.addListener(new ServerStageData.StagesLoader(e.getRegistryAccess()));
            e.addListener(new ServerStageData.MilestonesLoader());
        });
        bus.addListener((TagsUpdatedEvent e) -> {
            if (e.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) ServerStageData.rebuild();
            else ClientStageState.invalidate();
        });
        // Fires for the joining player at login and for everyone after /reload.
        bus.addListener((OnDatapackSyncEvent e) -> e.getRelevantPlayers().forEach(p -> {
            StageAccess.syncDefinitions(p);
            StageAccess.syncStages(p);
        }));
        bus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> StageAccess.forget(e.getEntity()));
        bus.addListener((PlayerEvent.PlayerRespawnEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) StageAccess.syncStages(p);
        });
    }
}
