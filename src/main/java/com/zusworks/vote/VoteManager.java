package com.zusworks.vote;

import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class VoteManager {

    private final List<VoteEntity> votes = new ArrayList<>();

    public void createVote(String voteName) {
        VoteEntity vote = new VoteEntity(voteName);
        votes.add(vote);
    }

    public void startVote(VoteEntity vote) {
        vote.setActive(true);
    }

    public void stopVote(VoteEntity vote) {
        vote.setActive(false);
    }

    public void addOption(VoteEntity vote, String option) {
        vote.addOption(option);
    }

    public List<String> getVoteNames() {
        List<String> voteNames = new ArrayList<>();
        for (VoteEntity vote : votes) {
            voteNames.add(vote.getName());
        }
        return voteNames;
    }

    public VoteEntity getVoteByName(String voteName) {
        for (VoteEntity vote : votes) {
            if (vote.getName().equals(voteName)) {
                return vote;
            }
        }
        return null;
    }

    public List<String> getVoteOptions(String voteName) {
        VoteEntity vote = getVoteByName(voteName);
        if (vote != null) {
            return vote.getOptions();
        }
        return null;
    }

    public void vote(VoteEntity vote, String option, Player player) {
        vote.addVote(option, player.getUUID());
    }

}
