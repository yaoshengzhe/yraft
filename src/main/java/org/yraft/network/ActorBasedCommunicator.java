package org.yraft.network;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import org.yraft.Messages;
import org.yraft.RaftServer;
import scala.Tuple2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ActorBasedCommunicator implements Communicator {

  private final ActorRef actorRef;
  private final ActorSystem actorSystem;
  private Map<Long, ActorRef> serverTable = Collections.EMPTY_MAP;

  public ActorBasedCommunicator(ActorRef actorRef, ActorSystem actorSystem) {
    this.actorRef = actorRef;
    this.actorSystem = actorSystem;
  }

  @Override
  public void setMembers(Map<Long, RaftServer> servers) {
    for (ActorRef server : this.serverTable.values()) {
      this.actorSystem.stop(server);
    }
    // TODO: implement setMembers
    this.serverTable = new HashMap<Long, ActorRef>();
  }

  @Override
  public void broadcast(Messages msg, byte[] data) {
    for (ActorRef server : this.serverTable.values()) {
      this.sendTo(server, msg, data);
    }
  }

  @Override
  public void sendTo(long candidateId, Messages msg, byte[] data) {
    ActorRef server = this.serverTable.get(candidateId);
    if (server != null) {
      this.sendTo(server, msg, data);
    }
  }

  private void sendTo(ActorRef server, Messages msg, byte[] data) {
    this.actorRef.tell(new Tuple2<Messages, byte[]>(msg, data), server);
  }

}
