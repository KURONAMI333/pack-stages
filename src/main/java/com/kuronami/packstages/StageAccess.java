package com.kuronami.packstages;

import com.kuronami.packstages.client.ClientStageState;
import com.kuronami.packstages.data.Locks;
import com.kuronami.packstages.data.ServerStageData;
import com.kuronami.packstages.data.StageDefinition;
import com.kuronami.packstages.network.SyncPayloads;
import com.kuronami.packstages.player.PlayerStages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The one place that answers "does this player have the stage" on either side. */
public final class StageAccess {
    private static final Map<UUID, Long> LAST_MESSAGE = new HashMap<>();

    private StageAccess() {}

    public static PlayerStages data(Player player) {
        return player.getData(PackStages.PLAYER_STAGES);
    }

    /** Stages of {@code player}; on the client this is the local player's synced copy. */
    public static Set<String> stages(Player player) {
        return player.level().isClientSide ? ClientStageState.stages() : data(player).stages();
    }

    public static Locks locks(Player player) {
        return player.level().isClientSide ? ClientStageState.locks() : ServerStageData.locks();
    }

    public static boolean add(ServerPlayer player, String stage) {
        boolean changed = data(player).add(stage);
        if (changed) syncStages(player);
        return changed;
    }

    public static boolean remove(ServerPlayer player, String stage) {
        boolean changed = data(player).remove(stage);
        if (changed) syncStages(player);
        return changed;
    }

    public static int clear(ServerPlayer player) {
        int n = data(player).clear();
        if (n > 0) syncStages(player);
        return n;
    }

    public static void syncStages(ServerPlayer player) {
        send(player, new SyncPayloads.Stages(new ArrayList<>(data(player).stages())));
    }

    public static void syncDefinitions(ServerPlayer player) {
        send(player, new SyncPayloads.Definitions(List.copyOf(ServerStageData.definitions())));
    }

    private static void send(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (player.connection == null || !player.connection.hasChannel(payload)) return;
        PacketDistributor.sendToPlayer(player, payload);
    }

    /**
     * Tells the player why something was refused, on the action bar. Server side only, and at most
     * once every 20 ticks so held-down clicks don't spam.
     */
    public static void notifyLocked(Player player, String key, Component what, StageDefinition needed) {
        if (player.level().isClientSide || !(player instanceof ServerPlayer)) return;
        long now = player.level().getGameTime();
        Long last = LAST_MESSAGE.get(player.getUUID());
        if (last != null && now - last < 20 && now >= last) return;
        LAST_MESSAGE.put(player.getUUID(), now);
        player.displayClientMessage(Component.translatable("pack_stages.locked." + key, what, needed.displayName()), true);
    }

    public static void forget(Player player) {
        LAST_MESSAGE.remove(player.getUUID());
    }
}
