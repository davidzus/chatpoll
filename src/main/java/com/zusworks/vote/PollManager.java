package com.zusworks.vote;

import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PollManager {

    private final List<PollEntity> polls = new ArrayList<>();

    public void createPoll(String displayName, String pollName) {
        PollEntity poll = new PollEntity(displayName, pollName);
        polls.add(poll);
    }

    public void startVote(PollEntity poll) {
        poll.setActive(true);
    }

    public void addOption(PollEntity poll, String option) {
        poll.addOption(option);
    }

    public List<String> getPollNames() {
        List<String> pollNames = new ArrayList<>();
        for (PollEntity poll : polls) {
            pollNames.add(poll.getName());
        }
        return pollNames;
    }

    public List<String> getActivePollNames() {
        List<String> pollNames = new ArrayList<>();
        for (PollEntity poll : polls) {
            if (poll.isActive()) {
                pollNames.add(poll.getName());
            }
        }
        return pollNames;
    }

    public List<String> getInactivePollNames() {
        List<String> pollNames = new ArrayList<>();
        for (PollEntity poll : polls) {
            if (!poll.isActive()) {
                pollNames.add(poll.getName());
            }
        }
        return pollNames;
    }

    public PollEntity getPollByName(String voteName) {
        String pollName = voteName.toLowerCase(Locale.ROOT);
        for (PollEntity poll : polls) {
            if (poll.getName().equals(pollName)) {
                return poll;
            }
        }
        return null;
    }

    public List<String> getPollOptions(String pollName) {
        PollEntity poll = getPollByName(pollName);
        if (poll != null) {
            return poll.getOptions();
        }
        return new ArrayList<>();
    }

    public void voteForPoll(PollEntity poll, String option, Player player) {
        poll.addVote(option, player.getUUID());
    }

    public boolean isActivePoll(PollEntity poll) {
        return poll.isActive();
    }

    public boolean hasVoted(PollEntity poll, Player player) {
        return poll.hasVoted(player.getUUID());
    }

    public List<PollEntity> getUnvotedActivePolls(Player player) {
        List<PollEntity> unvotedPolls = new ArrayList<>();
        for (PollEntity poll : polls) {
            if (poll.isActive() && !poll.hasVoted(player.getUUID())) {
                unvotedPolls.add(poll);
            }
        }
        return unvotedPolls;
    }

    public void endPoll(PollEntity poll) {
        poll.endPoll();
    }

    public void deletePoll(PollEntity poll) {
        polls.remove(poll);
    }

}
