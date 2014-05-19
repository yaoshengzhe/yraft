package com.github.yaoshengzhe.yraft;

import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.AppendEntriesRequest;
import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.AppendEntriesResponse;
import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.LogEntry;
import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.PersistentState;
import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.VoteRequest;
import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.VoteResponse;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RaftServer {

  private Roles role = Roles.Follower;

  // Persistent States
  private PersistentState state = PersistentState.newBuilder()
          .setCurrentTerm(0)
          .setVoteFor(0)
          .build();

  private long currentTerm = 0;
  private long voteFor = -1;
  private List<LogEntry> entries = Lists.newArrayList();

  // Volatile States
  private long commmitIndex = 0;
  private long lastApplied = 0;

  // Leader only
  private Map<String, Long> nextIndex = Maps.newHashMap();
  private Map<String, Long> matchIndex = Maps.newHashMap();

  public void asRole(Roles role) {
    this.role = role;
  }

  private void onAppendEntriesRequest(AppendEntriesRequest request) {

  }

  private void onAppendEntriesResponse(AppendEntriesResponse response) {

    if (response.getTerm() > this.currentTerm) {
      onTransition(Roles.Follower);
    }
  }

  private void onVoteRequest(VoteRequest request) {

  }

  private void onVoteResponse(VoteResponse response) {
    if (response.getTerm() > this.currentTerm) {
      onTransition(Roles.Follower);
    }
  }

  private void onTimeout() {
    switch (this.role) {
      case Follower:
        onTransition(Roles.Candidate);
        startLeaderElection();
        break;
      case Candidate:
        startLeaderElection();
        break;
    }
  }

  private void onTransition(Roles newRole) {
    if (this.role == Roles.Follower && newRole == Roles.Candidate) {
      this.currentTerm++;
    } else if (this.role == Roles.Candidate && newRole == Roles.Leader) {

    } else if (this.role == Roles.Candidate && newRole == Roles.Follower) {

    } else if (this.role == Roles.Leader && newRole == Roles.Follower) {

    }
    this.asRole(newRole);
  }

  private void startLeaderElection() {
    VoteRequest request = VoteRequest.newBuilder()
            .setLastLogIndex(this.lastApplied)
            .setTerm(this.currentTerm)
            .build();
    resetTimer();
  }

  private void resetTimer() {

  }

  private void persist() throws IOException {
    byte[] data = this.state.toByteArray();
    Files.asByteSink(new File(""), FileWriteMode.APPEND).write(data);
  }

  public void run() {
    resetTimer();
  }

  public static void main(String[] args) {

    RaftServer node = new RaftServer();
    node.run();
  }
}
