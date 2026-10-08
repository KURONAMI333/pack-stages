package com.kuronami.packstages;

import com.kuronami.packstages.data.ServerStageData;
import com.kuronami.packstages.data.StageDefinition;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * {@code /pack_stages add|remove|clear|list|has|stages|milestones reset}. Permission level 2, the same
 * level datapack functions and Pufferfish's Skills command rewards run at.
 */
public final class StagesCommand {
    private static final SuggestionProvider<CommandSourceStack> STAGE_NAMES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(ServerStageData.definitions().stream().map(StageDefinition::name), builder);
    private static final SuggestionProvider<CommandSourceStack> MILESTONE_IDS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(ServerStageData.milestones().stream().map(m -> m.id()), builder);

    private StagesCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pack_stages")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("stage", StringArgumentType.word()).suggests(STAGE_NAMES)
                                        .executes(StagesCommand::add))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("stage", StringArgumentType.word()).suggests(STAGE_NAMES)
                                        .executes(StagesCommand::remove))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(StagesCommand::clear)))
                .then(Commands.literal("list")
                        .executes(ctx -> list(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> list(ctx, EntityArgument.getPlayer(ctx, "target")))))
                .then(Commands.literal("has")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("stage", StringArgumentType.word()).suggests(STAGE_NAMES)
                                        .executes(StagesCommand::has))))
                .then(Commands.literal("stages").executes(StagesCommand::defined))
                .then(Commands.literal("milestones")
                        .then(Commands.literal("reset")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> resetMilestones(ctx, null))
                                        .then(Commands.argument("milestone", ResourceLocationArgument.id()).suggests(MILESTONE_IDS)
                                                .executes(ctx -> resetMilestones(ctx,
                                                        ResourceLocationArgument.getId(ctx, "milestone").toString())))))));
    }

    private static int add(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String stage = StringArgumentType.getString(ctx, "stage");
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        int changed = 0;
        for (ServerPlayer p : targets) if (StageAccess.add(p, stage)) changed++;
        int n = changed;
        boolean known = ServerStageData.definition(stage) != null;
        ctx.getSource().sendSuccess(() -> Component.translatable(
                known ? "pack_stages.command.add" : "pack_stages.command.add.undefined", stage, n), true);
        return changed;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String stage = StringArgumentType.getString(ctx, "stage");
        int changed = 0;
        for (ServerPlayer p : EntityArgument.getPlayers(ctx, "targets")) if (StageAccess.remove(p, stage)) changed++;
        int n = changed;
        ctx.getSource().sendSuccess(() -> Component.translatable("pack_stages.command.remove", stage, n), true);
        return changed;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int total = 0;
        for (ServerPlayer p : EntityArgument.getPlayers(ctx, "targets")) total += StageAccess.clear(p);
        int n = total;
        ctx.getSource().sendSuccess(() -> Component.translatable("pack_stages.command.clear", n), true);
        return total;
    }

    private static int list(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        var stages = StageAccess.data(target).stages();
        String joined = stages.isEmpty() ? "-" : String.join(", ", stages);
        ctx.getSource().sendSuccess(() -> Component.translatable("pack_stages.command.list",
                target.getDisplayName(), stages.size(), joined), false);
        return stages.size();
    }

    /** Returns 1 or 0 so {@code execute if|store result} can branch on it. */
    private static int has(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        String stage = StringArgumentType.getString(ctx, "stage");
        boolean has = StageAccess.data(target).stages().contains(stage);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                has ? "pack_stages.command.has.yes" : "pack_stages.command.has.no", target.getDisplayName(), stage), false);
        return has ? 1 : 0;
    }

    private static int defined(CommandContext<CommandSourceStack> ctx) {
        var defs = ServerStageData.definitions();
        String joined = defs.isEmpty() ? "-" : defs.stream().map(StageDefinition::name).sorted().collect(Collectors.joining(", "));
        int milestones = ServerStageData.milestones().size();
        ctx.getSource().sendSuccess(() -> Component.translatable("pack_stages.command.stages",
                defs.size(), joined, milestones), false);
        return defs.size();
    }

    private static int resetMilestones(CommandContext<CommandSourceStack> ctx, String id) throws CommandSyntaxException {
        int total = 0;
        for (ServerPlayer p : EntityArgument.getPlayers(ctx, "targets")) total += StageAccess.data(p).resetMilestones(id);
        int n = total;
        ctx.getSource().sendSuccess(() -> Component.translatable("pack_stages.command.milestones.reset", n), true);
        return total;
    }
}
