package org.yraft;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yraft.exception.UnknownRaftMessageException;
import org.yraft.network.Communicator;
import org.yraft.protobuf.generated.RaftProtos.AppendEntriesRequest;
import org.yraft.protobuf.generated.RaftProtos.AppendEntriesResponse;
import org.yraft.protobuf.generated.RaftProtos.AppendEntriesResponse.AppendStatus;
import org.yraft.protobuf.generated.RaftProtos.LogEntry;
import org.yraft.protobuf.generated.RaftProtos.VoteRequest;
import org.yraft.protobuf.generated.RaftProtos.VoteResponse;
import org.yraft.protobuf.generated.RaftProtos.VoteResponse.VoteDecision;
import org.yraft.statemachine.StateMachine;
import org.yraft.statemachine.StateMachineException;
import org.yraft.timer.TimerService;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * RaftServer: a implementation for core Raft algorithm logic.
 * A RaftServer represents a virtual or physical Raft node and contains logic
 * for all three Raft roles: Follower, Candidate and Leader.
 *
 * This class is not thread-safe.
 */
public class RaftServer implements Closeable {

  private final Logger LOG = LoggerFactory.getLogger(this.getClass());

  public static class Builder {

    private int candidateId;
    private StateMachine stateMachine;
    private Communicator communicator;
    private TimerService electionTimeoutService;
    private TimerService heartbeatService;
    private Map<Integer, InetSocketAddress> servers;

    public Builder(int candidateId) {
      this.candidateId = candidateId;
    }

    public Builder setStateMachine(StateMachine stateMachine) {
      this.stateMachine = stateMachine;
      return this;
    }

    public Builder setServers(Map<Integer, InetSocketAddress> servers) {
      this.servers = servers;
      return this;
    }

    public Builder setElectionTimeoutService(TimerService electionTimeoutService) {
      this.electionTimeoutService = electionTimeoutService;
      return this;
    }

    public Builder setHeartbeatService(TimerService heartbeatService) {
      this.heartbeatService = heartbeatService;
      return this;
    }

    public Builder setCommunicator(Communicator communicator) {
      this.communicator = communicator;
      return this;
    }

    public RaftServer build() {
      return new RaftServer(this);
    }
  }

  public static Builder newBuilder(int candidateId) {
    return new Builder(candidateId);
  }

  private final StateMachine stateMachine;
  private final TimerService electionTimeoutService;
  private final Communicator communicator;
  private final int candidateId;

  private Map<Integer, InetSocketAddress> servers;

  // Start as a follower
  private volatile Roles role = Roles.Follower;

  // Persistent States
  private long currentTerm = 0;
  private int voteFor = -1;
  private final List<LogEntry> entries = Lists.newArrayList();

  // Volatile States
  private int commmitIndex = 0;
  private int lastApplied = 0;

  // Candidate only
  private final Set<Integer> voteGrantFrom = Sets.newHashSet();

  // Leader only

  // For each server, index of the next log entry to send to that server
  // (initialized to leader last log index + 1)
  private final Map<Integer, Integer> nextIndexTable = Maps.newHashMap();
  // For each server, index of highest log entry known to be replicated on server
  // (initialized to 0, increases monotonically)
  private final Map<Integer, Integer> matchIndexTable = Maps.newHashMap();

  private final TimerService heartBeatTimerService;
  private final ExecutorService communicatorService;

  private RaftServer(Builder builder) {

    checkNotNull(builder.servers, "Empty cluster...");
    checkArgument(builder.servers.containsKey(builder.candidateId),
            "Candidate id: " + builder.candidateId + " is not in given server addresses...");

    this.candidateId = builder.candidateId;

    this.stateMachine = builder.stateMachine;
    this.electionTimeoutService = builder.electionTimeoutService;
    this.heartBeatTimerService = builder.heartbeatService;
    this.servers = builder.servers;

    this.communicator = builder.communicator;
    this.communicator.setMembers(this.servers);
    this.communicator.setServer(this);

    this.electionTimeoutService.setRunnable(new Runnable() {
      @Override
      public void run() {
        onTimeout();
      }
    });
    this.heartBeatTimerService.setRunnable(new Runnable() {
      @Override
      public void run() {
        heartbeat();
      }
    });

    this.communicatorService = Executors.newSingleThreadExecutor();
  }

  public Roles asRole(Roles role) {
    Roles oldRole = this.role;
    this.role = role;
    return oldRole;
  }

  public int getCandidateId() {
    return this.candidateId;
  }

  public Roles getRole() {
    return this.role;
  }

  /**
   * Invoked by leader to replicate log entries.
   *
   * Handling Logic:
   *     - Convert to Follower if request carries higher term
   *     - Invoke onHeartBeat if there is no entry list -> return
   *     - Response error if request carries lower term -> return
   *     - If the last log mismatch, remove our log until request.getPrevLogIndex,
   *       send error response -> return
   *     - Append all request entries, response success -> return
   */
  public void onAppendEntriesRequest(AppendEntriesRequest request) throws StateMachineException {

    handleLargerTerm(request.getTerm());

    if (request.getEntriesList().isEmpty()) {
      this.resetTimer();
    } else if (request.getTerm() < this.currentTerm) {
      this.communicator.sendTo(request.getLeaderId(), Messages.AppendEntriesResponse,
              newErrorAppendEntriesResponse().toByteArray());
    } else if (isLogEntryTermMisMatch(request)) {
      for (int i=this.getLastLogIndex(); i >= request.getPrevLogIndex(); --i) {
        entries.remove(i);
      }
      this.communicator.sendTo(request.getLeaderId(), Messages.AppendEntriesResponse,
              newErrorAppendEntriesResponse().toByteArray());
    } else {
      // Now append entries
      entries.addAll(request.getEntriesList());
      if (request.getLeaderCommitIndex() > this.commmitIndex) {
        this.commmitIndex = Math.min(request.getLeaderCommitIndex(), getLastLogIndex());
      }

      while (this.lastApplied < this.commmitIndex) {
        this.lastApplied++;
        this.stateMachine.commit(this.entries.get(this.lastApplied));
      }

      this.communicator.sendTo(request.getLeaderId(), Messages.AppendEntriesResponse,
              newSuccessAppendEntriesResponse().toByteArray());
    }
  }

  public void onAppendEntriesResponse(AppendEntriesResponse response) {

    // You are the leader, but if someone are better than you ...
    handleLargerTerm(response.getTerm());

    // If you are not the leader at this point, lol~
    if (this.role != Roles.Leader) {
      return;
    }

    switch (response.getStatus()) {
      case ERROR:
        int index = decNextIndexFor(response.getCandidateId());
        this.communicator.sendTo(response.getCandidateId(), Messages.AppendEntriesRequest,
                  newAppendEntriesRequest(this.entries.get(index)).toByteArray());
        break;
      case SUCCESS:
        incNextIndexFor(response.getCandidateId());
        break;
      default: ;
    }
  }

  /**
   * Invoked by candidates to gather votes.
   * Rules:
   * 1. Reply false if request.term < currentTerm
   * 2. If votedFor is null or candidateId, and candidate’s log is at
   *    least as up-to-date as receiver's log, grant vote.
   *
   */
  public void onVoteRequest(VoteRequest request) {

    handleLargerTerm(request.getTerm());

    VoteResponse.Builder builder = VoteResponse.newBuilder()
            .setTerm(this.currentTerm).setCandidateId(this.candidateId);

    if (request.getTerm() >= this.currentTerm &&
            !isLogLatestThan(request.getLastLogTerm(), request.getLastLogIndex()) &&
            this.voteFor == -1 || this.voteFor == request.getCandidateId()) {
      builder.setVoteDecision(VoteDecision.GRANTED);
      this.voteFor = request.getCandidateId();
    } else {
      builder.setVoteDecision(VoteDecision.DENIED);
    }

    this.communicator.sendTo(request.getCandidateId(), Messages.VoteResponse, builder.build().toByteArray());
  }

  /**
   * If votes received from majority of servers: become leader
   * Only server in Candidate role need to response this message.
   */
  public void onVoteResponse(VoteResponse response) {

    handleLargerTerm(response.getTerm());

    // If you are not Candidate, better to quit...
    if (this.role != Roles.Candidate) {
      return;
    }

    if (response.getVoteDecision() == VoteDecision.GRANTED) {
      this.voteGrantFrom.add(response.getCandidateId());

      if (this.voteGrantFrom.size() > (servers.size() / 2)) {
        onTransition(Roles.Leader);
      }
    }
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
    this.electionTimeoutService.stop();
    this.communicatorService.shutdown();
    try {
      this.communicatorService.awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public void heartbeat() {

    if (this.role != Roles.Leader) {
      throw new IllegalStateException(String.format("Only leader can send heartbeat messages! " +
              "Server: %s, Role: %s", this.candidateId, this.role));
    }

    AppendEntriesRequest request = AppendEntriesRequest.newBuilder()
            .setTerm(this.currentTerm)
            .setLeaderId(this.candidateId)
            .setPrevLogIndex(this.getLastLogIndex())
            .setPrevLogTerm(this.getLastLogTerm())
            .setLeaderCommitIndex(this.commmitIndex)
            .build();

    this.communicator.broadcast(Messages.AppendEntriesRequest, request.toByteArray());
  }

  public void start() {
    resetTimer();
    this.communicatorService.submit(new Runnable() {
      @Override
      public void run() {
        communicator.run();
      }
    });
  }

  public void receive(Messages msg, byte[] data) throws Exception {

    switch (msg) {
      case VoteRequest:
        LOG.debug("Candidate: " + this.getCandidateId() + " received VoteRequest: " + VoteRequest.parseFrom(data));
        this.onVoteRequest(VoteRequest.parseFrom(data));
        break;
      case VoteResponse:
        LOG.debug("Candidate: " + this.getCandidateId() + " received VoteResponse: " + VoteResponse.parseFrom(data));
        this.onVoteResponse(VoteResponse.parseFrom(data));
        break;
      case AppendEntriesRequest:
        this.onAppendEntriesRequest(AppendEntriesRequest.parseFrom(data));
        break;
      case AppendEntriesResponse:
        this.onAppendEntriesResponse(AppendEntriesResponse.parseFrom(data));
      default:
        throw new UnknownRaftMessageException("Unknown message: " + msg);
    }
  }

  @Override
  public String toString() {
    return String.format("{RaftServer: id -> %d, role -> %s, term -> %d}",
            this.candidateId,
            this.role,
            this.currentTerm);
  }

  private void onTransition(Roles newRole) {
    Roles oldRole = this.asRole(newRole);

    this.voteGrantFrom.clear();

    if (oldRole == Roles.Follower && newRole == Roles.Candidate) {
    } else if (oldRole == Roles.Follower && newRole == Roles.Follower) {

    } else if (oldRole == Roles.Candidate && newRole == Roles.Leader) {
      LOG.debug("Candidate " + this.candidateId + ", you are the leader!");
      // immediately send heartbeat
      heartbeat();
      this.heartBeatTimerService.start();
    } else if (oldRole == Roles.Candidate && newRole == Roles.Follower) {

    } else if (oldRole == Roles.Leader && newRole == Roles.Follower) {
      this.heartBeatTimerService.stop();
      this.nextIndexTable.clear();
      this.matchIndexTable.clear();
    } else {
      throw new IllegalStateException("You are in Role: " + oldRole +
              " but want to act as: " + newRole + ", and this is impossible and not allowed.");
    }
  }

  private void startLeaderElection() {

    if (LOG.isDebugEnabled()) {
      LOG.debug("Timeout: " + this.electionTimeoutService.getRecentTimeoutInMills() + "ms. " +
              "Candidate: " + this.candidateId + " starts Leader Election...");

    }

    // Should never happen
    if (this.role != Roles.Candidate) {
      throw new IllegalStateException("Your role is: " + this.role +
              "! You do not have permission to start leader election.");
    }

    this.currentTerm++;

    VoteRequest request = VoteRequest.newBuilder()
            .setCandidateId(this.candidateId)
            .setLastLogIndex(this.getLastLogIndex())
            .setLastLogTerm(this.getLastLogTerm())
            .setTerm(this.currentTerm)
            .build();

    this.voteFor = this.getCandidateId();
    this.communicator.broadcast(Messages.VoteRequest, request.toByteArray());
    resetTimer();
  }

  private void handleLargerTerm(long term) {
    if (term > this.currentTerm) {
      this.currentTerm = term;
      this.voteFor = -1;
      this.onTransition(Roles.Follower);
    }
  }

  private boolean isLogLatestThan(long term, long index) {

    long lastLogTerm = this.getLastLogTerm();
    return lastLogTerm > term || (lastLogTerm == term && this.getLastLogIndex() < index);
  }

  private int getLastLogIndex() {
    return this.entries.isEmpty() ? -1 : (this.entries.size() - 1);
  }

  private long getLastLogTerm() {
    return this.entries.isEmpty() ? 0 : (this.entries.get(this.entries.size() - 1)).getTerm();
  }

  private void resetTimer() {
    this.electionTimeoutService.reset();
  }

  private AppendEntriesResponse newErrorAppendEntriesResponse() {
    return AppendEntriesResponse.newBuilder()
            .setTerm(this.currentTerm)
            .setStatus(AppendStatus.ERROR)
            .setCandidateId(this.candidateId)
            .build();
  }

  private AppendEntriesResponse newSuccessAppendEntriesResponse() {
    return AppendEntriesResponse.newBuilder()
            .setTerm(this.currentTerm)
            .setStatus(AppendStatus.SUCCESS)
            .setCandidateId(this.candidateId)
            .build();
  }

  private AppendEntriesRequest newAppendEntriesRequest(LogEntry entry) {
    return AppendEntriesRequest.newBuilder()
            .setTerm(this.currentTerm)
            .setLeaderId(this.candidateId)
            .setPrevLogIndex(this.getLastLogIndex())
            .setPrevLogTerm(this.getLastLogTerm())
            .setLeaderCommitIndex(this.commmitIndex)
            .addEntries(entry)
            .build();
  }

  private boolean isLogEntryTermMisMatch(AppendEntriesRequest request) {
    return request.getPrevLogIndex() <= this.getLastLogIndex() &&
           request.getPrevLogTerm() != this.entries.get(request.getPrevLogIndex()).getTerm();
  }

  private int decNextIndexFor(int candidateId) {
    Integer val = this.nextIndexTable.get(candidateId);
    if (val == null) {
      val = 0;
    } else {
      val--;
    }
    this.nextIndexTable.put(candidateId, val);
    return val;
  }

  private int incNextIndexFor(int candidateId) {
    Integer val = this.nextIndexTable.get(candidateId);
    if (val == null) {
      val = 0;
    } else {
      val++;
    }
    this.nextIndexTable.put(candidateId, val);
    return val;
  }

}
