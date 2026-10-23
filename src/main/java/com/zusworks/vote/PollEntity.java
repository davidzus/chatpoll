package com.zusworks.vote;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PollEntity {
    String pollName;
    String displayName;
    List<String> pollOptions;
    List<UserVote> pollParticipants;
    boolean isPollActive;
    boolean hasEnded;

    public PollEntity(String displayName, String pollName) {
        this.displayName = displayName;
        this.pollName = pollName;
        this.pollOptions = new ArrayList<>();
        this.pollParticipants = new ArrayList<>();
        this.isPollActive = false;
        this.hasEnded = false;
    }

    public void addOption(String option) {
        pollOptions.add(option);
    }

    public String findOption(String input) {
        for (String option : pollOptions) {
            if (option.equalsIgnoreCase(input)) {
                return option;
            }
        }
        return null;
    }

    public List<String> getOptions() {
        return this.pollOptions;
    }

    public void setActive(boolean isActive) {
        this.isPollActive = isActive;
    }

    public String getName() {
        return this.pollName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void addVote(String option, UUID uuid) {
        this.pollParticipants.add(new UserVote(uuid, option));
    }

    public boolean hasVoted(UUID uuid) {
        for (UserVote vote : pollParticipants) {
            if (vote.getUserUUID().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    public boolean isActive() {
        return this.isPollActive;
    }

    public boolean hasEnded() {
        return this.hasEnded;
    }

    public Map<String, Integer> getVoteCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String option : pollOptions) {
            counts.put(option, 0);
        }
        for (UserVote vote : pollParticipants) {
            counts.merge(vote.getVoteOption(), 1, Integer::sum);
        }
        return counts;
    }

    public int getTotalVotes() {
        return this.pollParticipants.size();
    }

    public void endPoll() {
        this.isPollActive = false;
        this.hasEnded = true;
    }
}
