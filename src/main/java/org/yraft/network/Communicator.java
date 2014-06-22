package org.yraft.network;

import org.yraft.Messages;
import org.yraft.RaftServer;

import java.util.Map;

public interface Communicator {

  void setMembers(Map<Long, RaftServer> servers);

  void broadcast(Messages msg, byte[] data);

  void sendTo(long candidateId, Messages msg, byte[] data);

}
