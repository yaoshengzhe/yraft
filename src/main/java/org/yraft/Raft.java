package org.yraft;

import akka.actor.ActorSystem;
import org.yraft.network.ActorCommunicator;
import org.yraft.network.UDPCommunicator;
import org.yraft.statemachine.LocalDiskStateMachine;
import org.yraft.timer.HeartbeatTimerService;
import org.yraft.timer.RandomizedDelayTimerService;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class Raft {

  public static RaftServer newActorRaftServer(Map<Integer, InetSocketAddress> servers,
                                              int serverId, String commitFilePath,
                                              ActorSystem actorSystem) {

    return RaftServer.newBuilder(serverId)
            .setStateMachine(new LocalDiskStateMachine(new File(commitFilePath)))
            .setServers(servers)
            .setCommunicator(new ActorCommunicator(actorSystem))
            .setElectionTimeoutService(new RandomizedDelayTimerService(150, TimeUnit.MILLISECONDS, 100))
            .setHeartbeatService(new HeartbeatTimerService(100, TimeUnit.MILLISECONDS))
            .build();
  }

  public static RaftServer newUDPRaftServer(Map<Integer, InetSocketAddress> servers,
                                              int serverId, String commitFilePath, InetSocketAddress addr) throws SocketException {

    return RaftServer.newBuilder(serverId)
            .setStateMachine(new LocalDiskStateMachine(new File(commitFilePath)))
            .setServers(servers)
            .setCommunicator(new UDPCommunicator(addr))
            .setElectionTimeoutService(new RandomizedDelayTimerService(1000, TimeUnit.MILLISECONDS, 100))
            .setHeartbeatService(new HeartbeatTimerService(50, TimeUnit.MILLISECONDS))
            .build();
  }
}
