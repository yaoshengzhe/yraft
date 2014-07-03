package org.yraft;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public enum Messages {
  VoteRequest(1),
  VoteResponse(2),
  AppendEntriesRequest(3),
  AppendEntriesResponse(4);

  private static Map<Integer, Messages> idMap = ImmutableMap.<Integer, Messages>builder()
          .put(VoteRequest.getId(), VoteRequest)
          .put(VoteResponse.getId(), VoteResponse)
          .put(AppendEntriesRequest.getId(), AppendEntriesRequest)
          .put(AppendEntriesResponse.getId(), AppendEntriesResponse)
          .build();

  private final int id;

  private Messages(int id) {
    this.id = id;
  }

  public int getId() {
    return this.id;
  }

  public static Messages fromId(int id) {
    return idMap.get(id);
  }
}
