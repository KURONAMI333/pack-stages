package com.kuronami.packstages;

import com.kuronami.packstages.data.Milestone;
import com.kuronami.packstages.data.ServerStageData;
import com.kuronami.packstages.player.PlayerStages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runs milestone commands the first time a player holds a listed item, however it was obtained
 * (any workstation, a furnace, a chest, a trade). Looks at the inventory and the cursor every
 * 10 ticks, and right away when an item is crafted or smelted.
 */
public final class MilestoneTracker {
    private static final int INTERVAL = 10;

    private MilestoneTracker() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % INTERVAL == 0) {
            scan(player);
        }
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) check(player, List.of(event.getCrafting()));
    }

    @SubscribeEvent
    public static void onSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) check(player, List.of(event.getSmelting()));
    }

    /** Checks the whole inventory plus whatever is on the cursor. */
    public static void scan(ServerPlayer player) {
        if (ServerStageData.milestones().isEmpty()) return;
        List<ItemStack> stacks = new ArrayList<>(player.getInventory().getContainerSize() + 1);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) stacks.add(player.getInventory().getItem(i));
        stacks.add(player.containerMenu.getCarried());
        check(player, stacks);
    }

    private static void check(ServerPlayer player, List<ItemStack> stacks) {
        Map<Item, List<Milestone>> index = ServerStageData.milestoneIndex();
        if (index.isEmpty()) return;
        PlayerStages data = StageAccess.data(player);
        List<String> toRun = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            List<Milestone> hits = index.get(stack.getItem());
            if (hits == null) continue;
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            for (Milestone m : hits) {
                String key = m.each() ? m.id() + "|" + itemId : m.id().toString();
                if (data.markMilestone(key)) {
                    for (String command : m.commands()) toRun.add(command.replace("{item}", itemId));
                }
            }
        }
        if (toRun.isEmpty()) return;
        // Run on the next tick: the crafted/smelted events fire in the middle of a slot click, and
        // commands such as /clear or ones that open screens must not run inside it. The record is
        // written now, so a milestone pays out at most once even if its command fails.
        MinecraftServer server = player.server;
        server.tell(new TickTask(server.getTickCount() + 1, () -> {
            if (!player.isRemoved()) toRun.forEach(command -> run(player, command));
        }));
    }

    /** Same source shape as a function or a Pufferfish's Skills command reward: the player as @s. */
    private static void run(ServerPlayer player, String command) {
        MinecraftServer server = player.server;
        CommandSourceStack source = server.createCommandSourceStack()
                .withPosition(player.position())
                .withRotation(player.getRotationVector())
                .withLevel(player.serverLevel())
                .withEntity(player)
                .withSuppressedOutput()
                .withPermission(server.getFunctionCompilationLevel());
        server.getCommands().performPrefixedCommand(source, command);
    }
}
