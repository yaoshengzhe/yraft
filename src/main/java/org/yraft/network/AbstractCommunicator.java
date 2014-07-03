package org.yraft.network;

import org.yraft.RaftServer;

public abstract class AbstractCommunicator implements Communicator {

  protected RaftServer server;

  @Override
  public void setServer(final RaftServer server) {
    this.server = server;
  }
}
