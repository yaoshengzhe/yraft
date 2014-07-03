package org.yraft.actor

import akka.actor.{ActorSystem, Actor}
import org.yraft.{Raft, Messages, RaftServer}
import org.yraft.protobuf.generated.RaftProtos.{AppendEntriesResponse, AppendEntriesRequest, VoteResponse, VoteRequest}
import org.yraft.exception.UnknownRaftMessageException

import com.typesafe.scalalogging.slf4j.Logger

import java.util.{Map => JMap}
import org.slf4j.LoggerFactory
import com.google.common.collect.ImmutableMap
import java.net.InetSocketAddress

object ActorMain {

  def newServer(servers: JMap[Integer, InetSocketAddress], serverId: Int, commitFilePath: String, actorSystem: ActorSystem): RaftServer = {
    Raft.newActorRaftServer(servers, serverId, commitFilePath, actorSystem)
  }

  def main(args: Array[String]) {

    val actorSystem = ActorSystem("YRaft")
    val commitFilePath = "~/tmp/raft"

    var servers = ImmutableMap.builder[Integer, InetSocketAddress]
            .put(0, InetSocketAddress.createUnresolved("localhost", 12340))
            .put(1, InetSocketAddress.createUnresolved("localhost", 12341))
            .put(2, InetSocketAddress.createUnresolved("localhost", 12342))
            .put(3, InetSocketAddress.createUnresolved("localhost", 12343))
            .put(4, InetSocketAddress.createUnresolved("localhost", 12344))
            .put(5, InetSocketAddress.createUnresolved("localhost", 12345))
            .build

    (0 to 5).foreach(i => newServer(servers, i, commitFilePath + i, actorSystem).start())
  }
}
