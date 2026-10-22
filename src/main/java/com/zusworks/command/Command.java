package com.zusworks.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zusworks.vote.VoteEntity;
import com.zusworks.vote.VoteManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class Command {

    private static final VoteManager voteManager = new VoteManager();

    private static final SuggestionProvider<CommandSourceStack> VOTE_NAME_SUGGESTIONS = ((context, builder) -> SharedSuggestionProvider.suggest(
            voteManager.getVoteNames().stream().map(StringArgumentType::escapeIfRequired), builder
    ));

    private static final SuggestionProvider<CommandSourceStack> VOTE_OPTION_SUGGESTIONS = ((context, builder) -> SharedSuggestionProvider.suggest(
            voteManager.getVoteOptions(StringArgumentType.getString(context, "Vote Name")).stream().map(StringArgumentType::escapeIfRequired), builder
    ));

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("vote")
                            .then(Commands.argument("Vote Name", StringArgumentType.string())
                                    .suggests(VOTE_NAME_SUGGESTIONS)
                                    .then(Commands.argument("Option", StringArgumentType.greedyString())
                                            .suggests(VOTE_OPTION_SUGGESTIONS)
                                            .executes(Command::vote)
                                    )
                            )
                            .then(Commands.literal("create")
                                    .then(Commands.argument("Vote Name", StringArgumentType.string()).executes(Command::createVote))
                            )
                            .then(Commands.literal("list").executes(Command::listVotes))
                            .then(Commands.literal("start")
                                    .then(Commands.argument("Vote Name", StringArgumentType.string())
                                            .suggests(VOTE_NAME_SUGGESTIONS)
                                            .executes(Command::startVote)))
                            .then(Commands.literal("add")
                                    .then(Commands.argument("Vote Name", StringArgumentType.string())
                                            .suggests(VOTE_NAME_SUGGESTIONS)
                                            .then(Commands.argument("Option", StringArgumentType.greedyString())
                                                    .executes(Command::addOption)
                                            )
                                    )
                            )
            );
        });
    }

    private static int createVote(CommandContext<CommandSourceStack> context) {
        String voteNameValue = StringArgumentType.getString(context, "Vote Name");
        voteManager.createVote(voteNameValue);
        context.getSource().sendSuccess(() -> Component.literal("Successfully created vote: " + voteNameValue), false);
        return 1;
    }

    private static int listVotes(CommandContext<CommandSourceStack> context) {
        List<String> voteList = voteManager.getVoteNames();

        if (voteList.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No votes found"), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("Vote List: " + voteList), false);
        }
        return 1;
    }

    private static int startVote(CommandContext<CommandSourceStack> context) {
        String voteNameValue = StringArgumentType.getString(context, "Vote Name");
        VoteEntity vote = voteManager.getVoteByName(voteNameValue);

        if (vote == null) {
            context.getSource().sendSuccess(() -> Component.literal("Vote not found"), false);
        } else {
            voteManager.startVote(vote);
            context.getSource().sendSuccess(() -> Component.literal("Vote started: " + voteNameValue), false);
        }
        return 1;
    }

    private static int addOption(CommandContext<CommandSourceStack> context) {
        String voteNameValue = StringArgumentType.getString(context, "Vote Name");
        String option = StringArgumentType.getString(context, "Option");
        VoteEntity vote = voteManager.getVoteByName(voteNameValue);

        if (vote == null) {
            context.getSource().sendSuccess(() -> Component.literal("Vote not found"), false);
        } else {
            voteManager.addOption(vote, option);
            context.getSource().sendSuccess(() -> Component.literal("Option added to vote: " + voteNameValue), false);
        }

        return 1;
    }

    private static int vote(CommandContext<CommandSourceStack> context) {
        String voteNameValue = StringArgumentType.getString(context, "Vote Name");
        String voteOptionValue = StringArgumentType.getString(context, "Option");
        Player player = context.getSource().getPlayer();

        VoteEntity vote = voteManager.getVoteByName(voteNameValue);
        if (vote == null) {
            context.getSource().sendSuccess(() -> Component.literal("Vote not found"), false);
            return 0;
        }

        List<String> options = voteManager.getVoteOptions(vote.getName());
        if (!options.contains(voteOptionValue)) {
            context.getSource().sendSuccess(() -> Component.literal("Option not found in vote: " + voteNameValue), false);
            return 0;
        }

        voteManager.vote(vote, voteOptionValue, player);
        context.getSource().sendSuccess(() -> Component.literal("Voted for option: " + voteOptionValue + " in vote: " + voteNameValue), false);

        return 1;
    }

}

