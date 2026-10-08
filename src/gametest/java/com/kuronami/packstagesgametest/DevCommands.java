package com.kuronami.packstagesgametest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Dev-only: {@code /execute as <player> run pst_dev ...} drives the player's own server-side
 * interaction code (the same calls a right-click or slot click packet makes).
 */
final class DevCommands {
    private DevCommands() {}

    static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("pst_dev").requires(s -> s.hasPermission(2))
                .then(Commands.literal("use").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                    var result = p.gameMode.useItemOn(p, p.serverLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                    return report(ctx.getSource(), p, "use " + result);
                })))
                .then(Commands.literal("put").then(Commands.argument("slot", IntegerArgumentType.integer(0))
                        .then(Commands.argument("item", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64)).executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    ResourceLocation id = net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "item");
                                    ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id), IntegerArgumentType.getInteger(ctx, "count"));
                                    p.containerMenu.getSlot(IntegerArgumentType.getInteger(ctx, "slot")).set(stack);
                                    p.containerMenu.broadcastChanges();
                                    return report(ctx.getSource(), p, "put " + id);
                                })))))
                .then(Commands.literal("button").then(Commands.argument("id", IntegerArgumentType.integer(0)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    AbstractContainerMenu m = p.containerMenu;
                    boolean ok = m.clickMenuButton(p, IntegerArgumentType.getInteger(ctx, "id"));
                    m.broadcastChanges();
                    return report(ctx.getSource(), p, "button " + ok);
                })))
                .then(Commands.literal("click").then(Commands.argument("slot", IntegerArgumentType.integer(0))
                        .then(Commands.literal("pickup").executes(ctx -> click(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "slot"), ClickType.PICKUP)))
                        .then(Commands.literal("quick").executes(ctx -> click(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "slot"), ClickType.QUICK_MOVE)))))
                .then(Commands.literal("state").executes(ctx -> report(ctx.getSource(), ctx.getSource().getPlayerOrException(), "state")))
                .then(Commands.literal("close").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    p.closeContainer();
                    return report(ctx.getSource(), p, "close");
                })));
    }

    private static int click(CommandSourceStack src, int slot, ClickType type) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        p.containerMenu.clicked(slot, 0, type, p);
        p.containerMenu.broadcastChanges();
        return report(src, p, "click " + slot + " " + type);
    }

    private static int report(CommandSourceStack src, ServerPlayer p, String what) {
        AbstractContainerMenu m = p.containerMenu;
        String menu;
        if (m == p.inventoryMenu) {
            menu = "none";
        } else {
            ResourceLocation key;
            try {
                key = BuiltInRegistries.MENU.getKey(m.getType());
            } catch (UnsupportedOperationException e) {
                key = null;
            }
            menu = String.valueOf(key);
        }
        StringBuilder slots = new StringBuilder();
        if (m != p.inventoryMenu) {
            for (int i = 0; i < m.slots.size() && i < 6; i++) {
                ItemStack s = m.getSlot(i).getItem();
                slots.append(i).append('=').append(s.isEmpty() ? "-" : BuiltInRegistries.ITEM.getKey(s.getItem()) + "x" + s.getCount()).append(' ');
            }
        }
        ItemStack carried = m.getCarried();
        String msg = what + " | menu=" + menu + " | slots: " + slots + "| carried="
                + (carried.isEmpty() ? "-" : BuiltInRegistries.ITEM.getKey(carried.getItem()) + "x" + carried.getCount());
        src.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }
}
