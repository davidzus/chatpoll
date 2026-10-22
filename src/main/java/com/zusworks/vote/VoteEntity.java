package com.zusworks.vote;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VoteEntity {
    String voteName;
    List<String> voteOptions;
    List<UserVote> voteParticipants;
    boolean isVoteActive;

    public VoteEntity(String voteName) {
        this.voteName = voteName;
        this.voteOptions = new ArrayList<>();
        this.voteParticipants = new ArrayList<>();
        this.isVoteActive = false;
    }

    public void addOption(String option) {
        voteOptions.add(option);
    }

    public List<String> getOptions() {
        return this.voteOptions;
    }

    public void setActive(boolean isActive) {
        this.isVoteActive = isActive;
    }

    public String getName() {
        return this.voteName;
    }

    public void addVote(String option, UUID uuid) {
        this.voteParticipants.add(new UserVote(uuid, option));
    }

    public boolean isActive() {
        return this.isVoteActive;
    }
}
