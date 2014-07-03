package org.yraft.network;

import akka.actor.UntypedActor;
import org.yraft.Messages;
import org.yraft.RaftServer;
import scala.Tuple2;

public class ServerActor extends UntypedActor {

  private final RaftServer server;

  public ServerActor(RaftServer server) {
    this.server = server;
  }

  public void onReceive(Object message) throws Exception {
    if (message instanceof Tuple2) {
      Tuple2<Messages, byte[]> pair = (Tuple2<Messages, byte[]>)message;
      server.receive(pair._1(), pair._2());
    } else {
      unhandled(message);
    }
  }
}