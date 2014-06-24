package org.yraft;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import org.yraft.network.ActorBasedCommunicator;
import org.yraft.statemachine.LocalDiskStateMachine;
import org.yraft.timer.RandomizedDelayTimerService;
import org.yraft.timer.ScheduledDelayTimerService;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class Raft {

  public static RaftServer newActorBasedServer(Map<Integer, InetSocketAddress> servers,
                                               int serverId, String commitFilePath,
                                               ActorSystem actorSystem, Class<? extends Actor> actorClass) {

    final RaftServer server = RaftServer.newBuilder(serverId)
            .setStateMachine(new LocalDiskStateMachine(new File(commitFilePath)))
            .setServers(servers)
            .build();

    server.setElectionTimeoutService(new RandomizedDelayTimerService(150, TimeUnit.MILLISECONDS, new Runnable() {
      @Override
      public void run() {
        server.onTimeout();
      }
    }, 100));

    server.setHeartbeatService(new ScheduledDelayTimerService(100, TimeUnit.MILLISECONDS, new Runnable() {
      @Override
      public void run() {
        server.heartbeat();
      }
    }));

    ActorRef serverActor = actorSystem.actorOf(Props.create(actorClass, server));
    // Then set communicator, order is important here:)
    server.setCommunicator(new ActorBasedCommunicator(serverActor, actorSystem, actorClass));
    return server;
  }
}
