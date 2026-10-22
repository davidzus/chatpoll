package com.zusworks.vote;

import java.util.UUID;

public class UserVote {
    UUID userUUID;
    String voteOption;

    public UserVote(UUID userUUID, String voteOption) {
        this.userUUID = userUUID;
        this.voteOption = voteOption;
    }

    public UUID getUserUUID() {
        return this.userUUID;
    }

    public String getVoteOption() {
        return this.voteOption;
    }
}
