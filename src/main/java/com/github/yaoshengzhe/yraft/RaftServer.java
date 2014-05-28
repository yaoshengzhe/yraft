package com.github.yaoshengzhe.yraft;

import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.AppendEntriesRequest;
import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.AppendEntriesResponse;
import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.LogEntry;
import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.PersistentState;
import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.VoteRequest;
import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.VoteResponse;
import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.VoteResponse.VoteDecision;
import com.github.yaoshengzhe.yraft.statemachine.LocalDiskStateMachine;
import com.github.yaoshengzhe.yraft.statemachine.StateMachine;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.File;
import java.util.List;
import java.util.Map;

public class RaftServer {

  public static class Builder {

    private long candidateId;
    private StateMachine stateMachine;

    public Builder(long candidateId) {
      this.candidateId = candidateId;
    }

    public Builder setStateMachine(StateMachine stateMachine) {
      this.stateMachine = stateMachine;
      return this;
    }

    public RaftServer build() {
      return new RaftServer(this);
    }
  }

  private RaftServer(Builder builder) {
    this.candidateId = builder.candidateId;
    this.stateMachine = builder.stateMachine;
  }

  public static Builder newBuilder(long candidateId) {
    return new Builder(candidateId);
  }

  private StateMachine stateMachine;

  private Roles role = Roles.Follower;

  // Persistent States
  private PersistentState state = PersistentState.newBuilder()
          .setCurrentTerm(0)
          .setVoteFor(0)
          .build();

  private long candidateId = -1;
  private Map<Long, String> idHostLookupTable = Maps.newHashMap();

  private long currentTerm = 0;
  private long voteFor = -1;
  private List<LogEntry> entries = Lists.newArrayList();

  // Volatile States
  private long commmitIndex = 0;
  private long lastApplied = 0;

  // Leader only
  private Map<String, Long> nextIndex = Maps.newHashMap();
  private Map<String, Long> matchIndex = Maps.newHashMap();

  public RaftServer(long candidateId) {
    this.candidateId = candidateId;
  }

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
    } else if (this.role == Roles.Candidate && response.getVoteDecision() == VoteDecision.GRANTED) {

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
            .setCandidateId(this.candidateId)
            .setLastLogIndex(this.getLastLogIndex())
            .setLastLogTerm(this.getLastLogTerm())
            .setTerm(this.currentTerm)
            .build();
    resetTimer();
  }

  private long getLastLogIndex() {
    return this.entries.isEmpty() ? 0 : (this.entries.size() - 1);
  }

  private long getLastLogTerm() {
    return this.entries.isEmpty() ? 0 : (this.entries.get(this.entries.size() - 1)).getTerm();
  }

  private void resetTimer() {

  }

  public void run() {
    resetTimer();
  }

  public static void main(String[] args) {
    RaftServer node = RaftServer.newBuilder(1)
            .setStateMachine(new LocalDiskStateMachine(new File("")))
            .build();

    node.run();
  }
}
