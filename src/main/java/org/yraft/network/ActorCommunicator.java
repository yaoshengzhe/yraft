package org.yraft.network;


import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.google.common.collect.Maps;
import org.yraft.Messages;
import scala.Tuple2;

import java.net.InetSocketAddress;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class ActorCommunicator implements Communicator {

  private final ActorRef actorRef;
  private final ActorSystem actorSystem;
  private final Class<? extends Actor> actorClass;

  private Map<Integer, ActorRef> serverTable = Maps.newHashMap();

  public ActorCommunicator(ActorRef actorRef, ActorSystem actorSystem, Class<? extends Actor> actorClass) {
    this.actorRef = actorRef;
    this.actorSystem = actorSystem;
    this.actorClass = actorClass;
  }

  @Override
  public void setMembers(Map<Integer, InetSocketAddress> servers) {
    for (ActorRef server : this.serverTable.values()) {
      this.actorSystem.stop(server);
    }
    this.serverTable.clear();
    for (Map.Entry<Integer, InetSocketAddress> entry : servers.entrySet()) {
      ActorRef actor = actorSystem.actorSelection(toActorPath(entry.getValue())).anchor();
      checkNotNull(actor, "Cannot create actor: " + toActorPath(entry.getValue()));
      this.serverTable.put(entry.getKey(), actor);
    }
  }

  @Override
  public void broadcast(Messages msg, byte[] data) {
    for (ActorRef server : this.serverTable.values()) {
      this.sendTo(server, msg, data);
    }
  }

  @Override
  public void sendTo(int candidateId, Messages msg, byte[] data) {
    ActorRef server = this.serverTable.get(candidateId);
    if (server != null) {
      this.sendTo(server, msg, data);
    }
  }

  private void sendTo(ActorRef server, Messages msg, byte[] data) {
    this.actorRef.tell(new Tuple2<Messages, byte[]>(msg, data), server);
  }

  private String toActorPath(InetSocketAddress addr) {
    return String.format("akka.tcp://%s@%s:%d",
            this.actorClass.getSimpleName(),
            addr.getHostName(),
            addr.getPort());
  }
}
