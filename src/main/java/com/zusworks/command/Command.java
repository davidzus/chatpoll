package com.zusworks.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zusworks.vote.PollEntity;
import com.zusworks.vote.PollManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Command {

    private static final PollManager pollManager = new PollManager();

    private static final SuggestionProvider<CommandSourceStack> POLL_NAME_SUGGESTIONS = ((context, builder) -> SharedSuggestionProvider.suggest(
            pollManager.getPollNames().stream().map(StringArgumentType::escapeIfRequired), builder
    ));

    private static final SuggestionProvider<CommandSourceStack> ACTIVE_POLL_NAME_SUGGESTIONS = ((context, builder) -> SharedSuggestionProvider.suggest(
            pollManager.getActivePollNames().stream().map(StringArgumentType::escapeIfRequired), builder
    ));

    private static final SuggestionProvider<CommandSourceStack> INACTIVE_POLL_NAME_SUGGESTIONS = ((context, builder) -> SharedSuggestionProvider.suggest(
            pollManager.getInactivePollNames().stream().map(StringArgumentType::escapeIfRequired), builder
    ));

    private static final SuggestionProvider<CommandSourceStack> VOTE_OPTION_SUGGESTIONS = ((context, builder) -> SharedSuggestionProvider.suggest(
            pollManager.getPollOptions(StringArgumentType.getString(context, "Poll Name")), builder
    ));

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("poll")
                            .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                            .then(Commands.literal("create")
                                    .then(Commands.argument("Poll Name", StringArgumentType.string()).executes(Command::createPoll))
                            )
                            .then(Commands.literal("list").executes(Command::listPolls))
                            .then(Commands.literal("start")
                                    .then(Commands.argument("Poll Name", StringArgumentType.string())
                                            .suggests(POLL_NAME_SUGGESTIONS)
                                            .executes(Command::startPoll))
                            )
                            .then(Commands.literal("add")
                                    .then(Commands.argument("Poll Name", StringArgumentType.string())
                                            .suggests(POLL_NAME_SUGGESTIONS)
                                            .then(Commands.argument("Option", StringArgumentType.greedyString())
                                                    .executes(Command::addOption)
                                            )
                                    )
                            )
                            .then(Commands.literal("end").then(Commands.argument("Poll Name", StringArgumentType.string())
                                    .suggests(ACTIVE_POLL_NAME_SUGGESTIONS)
                                    .executes(Command::endPoll)
                            ))
                            .then(Commands.literal("delete").then(Commands.argument("Poll Name", StringArgumentType.string())
                                    .suggests(INACTIVE_POLL_NAME_SUGGESTIONS)
                                    .executes(Command::deletePoll)
                            ))
                            .then(Commands.literal("results").then(Commands.argument("Poll Name", StringArgumentType.string())
                                    .suggests(POLL_NAME_SUGGESTIONS)
                                    .executes(context -> showResults(context, false))
                                            .then(Commands.argument("Broadcast", BoolArgumentType.bool())
                                                    .executes(context -> showResults(context, BoolArgumentType.getBool(context, "Broadcast")))
                                    )))
            );

            dispatcher.register(Commands.literal("vote").then(Commands.argument("Poll Name", StringArgumentType.string())
                    .suggests(ACTIVE_POLL_NAME_SUGGESTIONS)
                    .then(Commands.argument("Option", StringArgumentType.greedyString())
                            .suggests(VOTE_OPTION_SUGGESTIONS)
                            .executes(Command::vote)
                    )
            ));
        });
    }

    private static int createPoll(CommandContext<CommandSourceStack> context) {
        String pollNameValue = StringArgumentType.getString(context, "Poll Name");

        if (pollManager.getPollByName(pollNameValue) != null) {
            sendError(context.getSource(), "A poll named ", pollNameValue, " already exists");
            return 0;
        }

        pollManager.createPoll(pollNameValue);
        sendInfo(context.getSource(), "Created poll ", pollNameValue);
        return 1;
    }

    private static int listPolls(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        List<String> pollList = pollManager.getPollNames();

        if (pollList.isEmpty()) {
            sendError(source, "No polls found");
            return 0;
        }

        source.sendSuccess(() -> prefix().append(Component.literal("Polls:").withStyle(ChatFormatting.YELLOW)), false);
        for (String pollName : pollList) {
            PollEntity poll = pollManager.getPollByName(pollName);
            MutableComponent status;
            if (poll.isActive()) {
                status = Component.literal("active").withStyle(ChatFormatting.GREEN);
            } else if (poll.hasEnded()) {
                status = Component.literal("ended").withStyle(ChatFormatting.RED);
            } else {
                status = Component.literal("not started").withStyle(ChatFormatting.GRAY);
            }
            source.sendSuccess(() -> Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(pollName).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY))
                    .append(status)
                    .append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY)), false);
        }
        return 1;
    }

    private static int startPoll(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String pollNameValue = StringArgumentType.getString(context, "Poll Name");
        PollEntity poll = pollManager.getPollByName(pollNameValue);

        if (poll == null) {
            sendError(source, "Poll ", pollNameValue, " not found");
            return 0;
        }

        if (poll.isActive()) {
            sendError(source, "Poll ", pollNameValue, " is already running");
            return 0;
        }

        if (poll.hasEnded()) {
            sendError(source, "Poll ", pollNameValue, " has already ended");
            return 0;
        }

        if (poll.getOptions().isEmpty()) {
            sendError(source, "Poll ", pollNameValue, " has no options yet");
            return 0;
        }

        pollManager.startVote(poll);
        sendInfo(source, "Started poll ", pollNameValue);
        return 1;
    }

    private static int addOption(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String pollNameValue = StringArgumentType.getString(context, "Poll Name");
        String option = StringArgumentType.getString(context, "Option");
        PollEntity poll = pollManager.getPollByName(pollNameValue);

        if (poll == null) {
            sendError(source, "Poll ", pollNameValue, " not found");
            return 0;
        }

        if (poll.isActive()) {
            sendError(source, "Cannot add options to a running poll");
            return 0;
        }

        if (poll.hasEnded()) {
            sendError(source, "Cannot add options to an ended poll");
            return 0;
        }

        if (poll.getOptions().contains(option)) {
            sendError(source, "Option ", option, " already exists");
            return 0;
        }

        pollManager.addOption(poll, option);
        sendInfo(source, "Added option ", option, " to ", pollNameValue);
        return 1;
    }

    private static int vote(CommandContext<CommandSourceStack> context) {
        String pollNameValue = StringArgumentType.getString(context, "Poll Name");
        String voteOptionValue = StringArgumentType.getString(context, "Option");
        CommandSourceStack source = context.getSource();
        Player player = source.getPlayer();

        if (player == null) {
            sendError(source, "Only players can vote");
            return 0;
        }

        PollEntity poll = pollManager.getPollByName(pollNameValue);
        if (poll == null) {
            sendError(source, "Poll ", pollNameValue, " not found");
            return 0;
        }

        if (!pollManager.isActivePoll(poll)) {
            sendError(source, "Poll ", pollNameValue, " is not active");
            return 0;
        }

        List<String> options = pollManager.getPollOptions(poll);
        if (!options.contains(voteOptionValue)) {
            sendError(source, "Option ", voteOptionValue, " not found in ", pollNameValue);
            return 0;
        }

        if (pollManager.hasVoted(poll, player)) {
            sendError(source, "You have already voted in this poll");
            return 0;
        }

        pollManager.voteForPoll(poll, voteOptionValue, player);
        sendInfo(source, "You voted for ", voteOptionValue);
        return 1;
    }

    public static int endPoll(CommandContext<CommandSourceStack> context) {
        String pollName = StringArgumentType.getString(context, "Poll Name");
        CommandSourceStack source = context.getSource();
        PollEntity poll = pollManager.getPollByName(pollName);

        if (poll == null) {
            sendError(source, "Poll ", pollName, " not found");
            return 0;
        }

        if (!pollManager.isActivePoll(poll)) {
            sendError(source, "Poll ", pollName, " is not active");
            return 0;
        }

        pollManager.endPoll(poll);
        sendInfo(source, "Ended poll ", pollName);
        sendResults(source, poll, false);
        return 1;
    }

    public static int deletePoll(CommandContext<CommandSourceStack> context) {
        String pollName = StringArgumentType.getString(context, "Poll Name");
        CommandSourceStack source = context.getSource();
        PollEntity poll = pollManager.getPollByName(pollName);

        if (poll == null) {
            sendError(source, "Poll ", pollName, " not found");
            return 0;
        }

        if (pollManager.isActivePoll(poll)) {
            sendError(source, "Poll ", pollName, " is still running, end it first");
            return 0;
        }

        pollManager.deletePoll(poll);
        sendInfo(source, "Deleted poll ", pollName);
        return 1;
    }

    public static int showResults(CommandContext<CommandSourceStack> context, boolean broadcast) {
        String pollName = StringArgumentType.getString(context, "Poll Name");
        CommandSourceStack source = context.getSource();
        PollEntity poll = pollManager.getPollByName(pollName);

        if (poll == null) {
            sendError(source, "Poll ", pollName, " not found");
            return 0;
        }

        sendResults(source, poll, broadcast);
        return 1;
    }

    private static void sendResults(CommandSourceStack source, PollEntity poll, boolean broadcast) {
        Map<String, Integer> counts = poll.getVoteCounts();
        int total = poll.getTotalVotes();

        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("━━━ ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(poll.getName()).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .append(Component.literal(poll.hasEnded() ? " (final)" : " (live)").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" ━━━").withStyle(ChatFormatting.GOLD)));

        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    int votes = entry.getValue();
                    double percent = total == 0 ? 0 : votes * 100.0 / total;
                    lines.add(Component.literal(" " + entry.getKey() + ": ").withStyle(ChatFormatting.AQUA)
                            .append(Component.literal(votes + (votes == 1 ? " vote" : " votes")).withStyle(ChatFormatting.WHITE))
                            .append(Component.literal(String.format(" (%.1f%%) ", percent)).withStyle(ChatFormatting.GRAY))
                            .append(progressBar(percent)));
                });

        lines.add(Component.literal(" Total votes: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(total)).withStyle(ChatFormatting.WHITE)));

        for (Component line : lines) {
            if (broadcast) {
                source.getServer().getPlayerList().broadcastSystemMessage(line, false);
            } else {
                source.sendSuccess(() -> line, false);
            }
        }
    }

    private static MutableComponent progressBar(double percent) {
        int filled = (int) Math.round(percent / 10);
        return Component.literal("■".repeat(filled)).withStyle(ChatFormatting.GREEN)
                .append(Component.literal("■".repeat(10 - filled)).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static MutableComponent prefix() {
        return Component.literal("[Poll] ").withStyle(ChatFormatting.GOLD);
    }

    private static MutableComponent message(ChatFormatting textColor, String... parts) {
        MutableComponent message = prefix();
        for (int i = 0; i < parts.length; i++) {
            message.append(Component.literal(parts[i]).withStyle(i % 2 == 0 ? textColor : ChatFormatting.AQUA));
        }
        return message;
    }

    private static void sendInfo(CommandSourceStack source, String... parts) {
        source.sendSuccess(() -> message(ChatFormatting.GREEN, parts), false);
    }

    private static void sendError(CommandSourceStack source, String... parts) {
        source.sendFailure(message(ChatFormatting.RED, parts));
    }

}
