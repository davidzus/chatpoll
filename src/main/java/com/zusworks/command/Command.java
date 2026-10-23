package com.zusworks.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zusworks.vote.PollEntity;
import com.zusworks.vote.PollManager;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

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

    private static final String PERMISSION_PREFIX = "chatpoll.command.";
    private static final String[] POLL_SUBCOMMANDS = {"create", "list", "start", "add", "end", "delete", "results"};

    private static Predicate<CommandSourceStack> poll(String subcommand) {
        return Permissions.require(PERMISSION_PREFIX + "poll." + subcommand, PermissionLevel.GAMEMASTERS);
    }

    private static boolean canUseAnyPollCommand(CommandSourceStack source) {
        for (String subcommand : POLL_SUBCOMMANDS) {
            if (poll(subcommand).test(source)) {
                return true;
            }
        }
        return false;
    }

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("poll")
                            .requires(Command::canUseAnyPollCommand)
                            .then(Commands.literal("create")
                                    .requires(poll("create"))
                                    .then(Commands.argument("Poll Name", StringArgumentType.string()).executes(Command::createPoll))
                            )
                            .then(Commands.literal("list")
                                    .requires(poll("list"))
                                    .executes(Command::listPolls))
                            .then(Commands.literal("start")
                                    .requires(poll("start"))
                                    .then(Commands.argument("Poll Name", StringArgumentType.string())
                                            .suggests(POLL_NAME_SUGGESTIONS)
                                            .executes(Command::startPoll))
                            )
                            .then(Commands.literal("add")
                                    .requires(poll("add"))
                                    .then(Commands.argument("Poll Name", StringArgumentType.string())
                                            .suggests(POLL_NAME_SUGGESTIONS)
                                            .then(Commands.argument("Option", StringArgumentType.greedyString())
                                                    .executes(Command::addOption)
                                            )
                                    )
                            )
                            .then(Commands.literal("end")
                                    .requires(poll("end"))
                                    .then(Commands.argument("Poll Name", StringArgumentType.string())
                                            .suggests(ACTIVE_POLL_NAME_SUGGESTIONS)
                                            .executes(Command::endPoll)
                            ))
                            .then(Commands.literal("delete")
                                    .requires(poll("delete"))
                                    .then(Commands.argument("Poll Name", StringArgumentType.string())
                                            .suggests(INACTIVE_POLL_NAME_SUGGESTIONS)
                                            .executes(Command::deletePoll)
                            ))
                            .then(Commands.literal("results")
                                    .requires(poll("results"))
                                    .then(Commands.argument("Poll Name", StringArgumentType.string())
                                            .suggests(POLL_NAME_SUGGESTIONS)
                                            .executes(context -> showResults(context, false))
                                            .then(Commands.argument("Broadcast", BoolArgumentType.bool())
                                                    .requires(poll("results.broadcast"))
                                                    .executes(context -> showResults(context, BoolArgumentType.getBool(context, "Broadcast")))
                                    )))
            );

            dispatcher.register(Commands.literal("vote")
                    .requires(Permissions.require(PERMISSION_PREFIX + "vote", PermissionLevel.ALL))
                    .executes(Command::listVotablePolls)
                    .then(Commands.argument("Poll Name", StringArgumentType.string())
                            .suggests(ACTIVE_POLL_NAME_SUGGESTIONS)
                            .executes(Command::listVoteOptions)
                            .then(Commands.argument("Option", StringArgumentType.greedyString())
                                    .suggests(VOTE_OPTION_SUGGESTIONS)
                                    .executes(Command::vote)
                            )
                    ));
        });
    }

    private static int createPoll(CommandContext<CommandSourceStack> context) {
        String pollNameValue = StringArgumentType.getString(context, "Poll Name");
        String pollName = pollNameValue.toLowerCase(Locale.ROOT);

        PollEntity existingPoll = pollManager.getPollByName(pollName);
        if (existingPoll != null) {
            sendError(context.getSource(), "A poll named ", existingPoll.getDisplayName(), " already exists");
            return 0;
        }

        pollManager.createPoll(pollNameValue, pollName);
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
                    .append(Component.literal(poll.getDisplayName()).withStyle(ChatFormatting.AQUA))
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
            sendError(source, "Poll ", poll.getDisplayName(), " is already running");
            return 0;
        }

        if (poll.hasEnded()) {
            sendError(source, "Poll ", poll.getDisplayName(), " has already ended");
            return 0;
        }

        if (poll.getOptions().isEmpty()) {
            sendError(source, "Poll ", poll.getDisplayName(), " has no options yet");
            return 0;
        }

        pollManager.startVote(poll);
        sendInfo(source, "Started poll ", poll.getDisplayName());
        broadcastPollStart(source, poll);
        return 1;
    }

    private static void broadcastPollStart(CommandSourceStack source, PollEntity poll) {
        String optionsCommand = "/vote " + StringArgumentType.escapeIfRequired(poll.getName());
        Component message = prefix()
                .append(Component.literal("A new poll has started: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(poll.getDisplayName()).withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand(optionsCommand))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to see the options")))))
                .append(Component.literal(" (" + String.join(", ", poll.getOptions()) + ")").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" Use ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(optionsCommand).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" to vote!").withStyle(ChatFormatting.YELLOW));
        source.getServer().getPlayerList().broadcastSystemMessage(message, false);
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

        String existingOption = poll.findOption(option);
        if (existingOption != null) {
            sendError(source, "Option ", existingOption, " already exists");
            return 0;
        }

        pollManager.addOption(poll, option);
        sendInfo(source, "Added option ", option, " to ", poll.getDisplayName());
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
            sendError(source, "Poll ", poll.getDisplayName(), " is not active");
            return 0;
        }

        String option = poll.findOption(voteOptionValue);
        if (option == null) {
            sendError(source, "Option ", voteOptionValue, " not found in ", poll.getDisplayName());
            return 0;
        }

        if (pollManager.hasVoted(poll, player)) {
            sendError(source, "You have already voted in this poll");
            return 0;
        }

        pollManager.voteForPoll(poll, option, player);
        sendInfo(source, "You voted for ", option);
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
            sendError(source, "Poll ", poll.getDisplayName(), " is not active");
            return 0;
        }

        pollManager.endPoll(poll);
        sendInfo(source, "Ended poll ", poll.getDisplayName());
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
            sendError(source, "Poll ", poll.getDisplayName(), " is still running, end it first");
            return 0;
        }

        pollManager.deletePoll(poll);
        sendInfo(source, "Deleted poll ", poll.getDisplayName());
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
                .append(Component.literal(poll.getDisplayName()).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
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

    public static void notifyUnvotedPolls(ServerPlayer player) {
        List<PollEntity> unvotedPolls = pollManager.getUnvotedActivePolls(player);
        if (unvotedPolls.isEmpty()) {
            return;
        }

        player.sendSystemMessage(prefix()
                .append(Component.literal("You haven't voted on " + unvotedPolls.size()
                        + (unvotedPolls.size() == 1 ? " poll" : " polls") + " yet. Use ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("/vote <poll> <option>").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" to vote:").withStyle(ChatFormatting.YELLOW)));

        for (PollEntity poll : unvotedPolls) {
            String optionsCommand = "/vote " + StringArgumentType.escapeIfRequired(poll.getName());
            player.sendSystemMessage(Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(poll.getDisplayName()).withStyle(style -> style
                            .withColor(ChatFormatting.AQUA)
                            .withClickEvent(new ClickEvent.RunCommand(optionsCommand))
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to see the options")))))
                    .append(Component.literal(" (" + String.join(", ", poll.getOptions()) + ")").withStyle(ChatFormatting.GRAY)));
        }
    }

    private static int listVotablePolls(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        List<String> activePolls = pollManager.getActivePollNames();

        if (activePolls.isEmpty()) {
            sendError(source, "There are no active polls right now");
            return 0;
        }

        Player player = source.getPlayer();
        source.sendSuccess(() -> prefix()
                .append(Component.literal("Active polls (use ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("/vote <poll>").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" to see the options):").withStyle(ChatFormatting.YELLOW)), false);

        for (String pollName : activePolls) {
            PollEntity poll = pollManager.getPollByName(pollName);
            boolean voted = player != null && pollManager.hasVoted(poll, player);
            String optionsCommand = "/vote " + StringArgumentType.escapeIfRequired(poll.getName());
            source.sendSuccess(() -> Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(poll.getDisplayName()).withStyle(style -> style
                            .withColor(ChatFormatting.AQUA)
                            .withClickEvent(new ClickEvent.RunCommand(optionsCommand))
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to see the options")))))
                    .append(voted
                            ? Component.literal(" (voted)").withStyle(ChatFormatting.GREEN)
                            : Component.literal(" (not voted)").withStyle(ChatFormatting.GRAY)), false);
        }
        return 1;
    }

    private static int listVoteOptions(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String pollNameValue = StringArgumentType.getString(context, "Poll Name");
        PollEntity poll = pollManager.getPollByName(pollNameValue);

        if (poll == null) {
            sendError(source, "Poll ", pollNameValue, " not found");
            return 0;
        }

        if (!pollManager.isActivePoll(poll)) {
            sendError(source, "Poll ", poll.getDisplayName(), " is not active");
            return 0;
        }

        Player player = source.getPlayer();
        if (player != null && pollManager.hasVoted(poll, player)) {
            sendInfo(source, "You have already voted in ", poll.getDisplayName());
            return 1;
        }

        source.sendSuccess(() -> prefix()
                .append(Component.literal("Options for ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(poll.getDisplayName()).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" (use ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("/vote " + poll.getName() + " <option>").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("):").withStyle(ChatFormatting.YELLOW)), false);

        for (String option : poll.getOptions()) {
            String voteCommand = "/vote " + StringArgumentType.escapeIfRequired(poll.getName()) + " " + option;
            source.sendSuccess(() -> Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(option).withStyle(style -> style
                            .withColor(ChatFormatting.AQUA)
                            .withClickEvent(new ClickEvent.SuggestCommand(voteCommand))
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to vote for " + option))))), false);
        }
        return 1;
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
