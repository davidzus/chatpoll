package com.zusworks.vote;

import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class PollManager {

    private final List<PollEntity> polls = new ArrayList<>();

    public void createPoll(String pollName) {
        PollEntity poll = new PollEntity(pollName);
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
        for (PollEntity poll : polls) {
            if (poll.getName().equals(voteName)) {
                return poll;
            }
        }
        return null;
    }

    public List<String> getPollOptions(PollEntity poll) {
        return poll.getOptions();
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

    public void endPoll(PollEntity poll) {
        poll.endPoll();
    }

    public void deletePoll(PollEntity poll) {
        polls.remove(poll);
    }

}
