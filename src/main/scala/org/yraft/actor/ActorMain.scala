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

class ServerActor(server: RaftServer) extends Actor {

  val logger = Logger(LoggerFactory getLogger this.getClass.getSimpleName)

  def receive = {
    case (msg: Messages, data: Array[Byte]) =>
      handle(msg, data)
    case _ =>
      throw new UnknownRaftMessageException("Unknown message.")
  }

  def handle(msg: Messages, data: Array[Byte]): Unit = {
    msg match {
      case Messages.HeartBeat =>
        this.server.onHeartBeat()
      case Messages.VoteRequest =>
        logger.debug("Candidate: " + server.getCandidateId + " received VoteRequest: " + VoteRequest.parseFrom(data))
        this.server.onVoteRequest(VoteRequest.parseFrom(data))
      case Messages.VoteResponse =>
        logger.debug("Candidate: " + server.getCandidateId + " received VoteResponse: " + VoteResponse.parseFrom(data))
        this.server.onVoteResponse(VoteResponse.parseFrom(data))
      case Messages.AppendEntriesRequest =>
        this.server.onAppendEntriesRequest(AppendEntriesRequest.parseFrom(data))
      case Messages.AppendEntriesResponse =>
        this.server.onAppendEntriesResponse(AppendEntriesResponse.parseFrom(data))
      case _ =>
        throw new UnknownRaftMessageException("Unknown message: " + msg)
    }
  }
}

object ActorMain {

  def newServer(servers: JMap[Integer, InetSocketAddress], serverId: Int, commitFilePath: String, actorSystem: ActorSystem): RaftServer = {
    Raft.newActorBasedServer(servers, serverId, commitFilePath, actorSystem, classOf[ServerActor])
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

    servers = ImmutableMap.builder[Integer, InetSocketAddress]
            .put(0, InetSocketAddress.createUnresolved("localhost", 12340))
            .build
    (0 to 0).foreach(i => newServer(servers, i, commitFilePath + i, actorSystem).run())
  }
}
