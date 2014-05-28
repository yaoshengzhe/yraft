package com.github.yaoshengzhe.yraft;

import com.github.yaoshengzhe.yraft.network.Communicator;
import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.AppendEntriesRequest;
import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.AppendEntriesResponse;
import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.LogEntry;
import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.PersistentState;
import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.VoteRequest;
import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.VoteResponse;
import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.VoteResponse.VoteDecision;
import com.github.yaoshengzhe.yraft.statemachine.LocalDiskStateMachine;
import com.github.yaoshengzhe.yraft.statemachine.StateMachine;
import com.github.yaoshengzhe.yraft.timer.TimerService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RaftServer implements Closeable {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public static class Builder {

    private long candidateId;
    private StateMachine stateMachine;
    private Communicator communicator;
    private TimerService timerService;

    public Builder(long candidateId) {
      this.candidateId = candidateId;
    }

    public Builder setStateMachine(StateMachine stateMachine) {
      this.stateMachine = stateMachine;
      return this;
    }

    public Builder setCommunicator(Communicator communicator) {
      this.communicator = communicator;
      return this;
    }

    public Builder setTimerService(TimerService timerService) {
      this.timerService = timerService;
      return this;
    }

    public RaftServer build() {
      return new RaftServer(this);
    }
  }

  private RaftServer(Builder builder) {
    this.candidateId = builder.candidateId;
    this.stateMachine = builder.stateMachine;
    this.communicator = builder.communicator;
    this.timerService = builder.timerService;
    this.timerService.init(new Runnable() {
      @Override
      public void run() {
        onTimeout();
      }
    });
  }

  public static Builder newBuilder(long candidateId) {
    return new Builder(candidateId);
  }

  private final StateMachine stateMachine;
  private final TimerService timerService;
  private Communicator communicator;

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

  public void asRole(Roles role) {
    this.role = role;
  }

  public void onAppendEntriesRequest(AppendEntriesRequest request) {

  }

  public void onAppendEntriesResponse(AppendEntriesResponse response) {

    if (response.getTerm() > this.currentTerm) {
      onTransition(Roles.Follower);
    }
  }

  public void onVoteRequest(VoteRequest request) {

  }

  public void onVoteResponse(VoteResponse response) {
    if (response.getTerm() > this.currentTerm) {
      onTransition(Roles.Follower);
    } else if (this.role == Roles.Candidate && response.getVoteDecision() == VoteDecision.GRANTED) {

    }
  }

  public synchronized void setCommunicator(Communicator communicator) {
    this.communicator = communicator;
  }

  public void onTimeout() {
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

  @Override
  public void close() throws IOException {
    this.timerService.stop();
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

    if (logger.isDebugEnabled()) {
      logger.debug("Start Leader Election...");
    }

    // Should never happen
    if (this.role != Roles.Candidate) {
      throw new IllegalStateException("Your role is: " + this.role +
              "! Do not have permission to start leader election.");
    }

    VoteRequest request = VoteRequest.newBuilder()
            .setCandidateId(this.candidateId)
            .setLastLogIndex(this.getLastLogIndex())
            .setLastLogTerm(this.getLastLogTerm())
            .setTerm(this.currentTerm)
            .build();

    this.communicator.send(Messages.VoteRequest, request.toByteArray());
    resetTimer();
  }

  private long getLastLogIndex() {
    return this.entries.isEmpty() ? 0 : (this.entries.size() - 1);
  }

  private long getLastLogTerm() {
    return this.entries.isEmpty() ? 0 : (this.entries.get(this.entries.size() - 1)).getTerm();
  }

  private void resetTimer() {
    this.timerService.reset();
  }

  public void run() {
    resetTimer();
  }

}
