package org.yraft;

public enum Messages {
  VoteRequest(1),
  VoteResponse(2),
  AppendEntriesRequest(3),
  AppendEntriesResponse(4),
  HeartBeat(5);

  private final int id;

  private Messages(int id) {
    this.id = id;
  }

  public int getId() {
    return this.id;
  }
}
