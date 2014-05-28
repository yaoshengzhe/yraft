package com.github.yaoshengzhe.yraft.network;

import akka.actor.ActorRef;
import com.github.yaoshengzhe.yraft.Messages;
import scala.Tuple2;

public class ActorBasedCommunicator implements Communicator {

  private final ActorRef actorRef;

  public ActorBasedCommunicator(ActorRef actorRef) {
    this.actorRef = actorRef;
  }

  @Override
  public void send(Messages msg, byte[] data) {
    this.actorRef.tell(new Tuple2<Messages, byte[]>(msg, data), this.actorRef);
  }

}
