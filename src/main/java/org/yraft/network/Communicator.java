package org.yraft.network;

import org.yraft.Messages;

import java.net.InetSocketAddress;
import java.util.Map;

public interface Communicator {

  void setMembers(Map<Integer, InetSocketAddress> servers);

  void broadcast(Messages msg, byte[] data);

  void sendTo(int candidateId, Messages msg, byte[] data);

}
