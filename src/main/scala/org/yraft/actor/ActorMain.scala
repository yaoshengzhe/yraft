package org.yraft.actor

import akka.actor.{Props, ActorSystem, Actor}
import org.yraft.statemachine.LocalDiskStateMachine
import org.yraft.network.ActorBasedCommunicator
import org.yraft.{Raft, Messages, RaftServer}
import org.yraft.protobuf.generated.RaftProtos.{AppendEntriesResponse, AppendEntriesRequest, VoteResponse, VoteRequest}
import org.yraft.exception.UnknownRaftMessageException
import org.yraft.timer.ScheduledExecutorTimerService

import com.typesafe.scalalogging.slf4j.Logger

import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit
import scala.collection.parallel.mutable

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
        this.server.onHeartBeat(AppendEntriesRequest.parseFrom(data))
      case Messages.VoteRequest =>
        logger.debug("Received VoteRequest: " + VoteRequest.parseFrom(data))
        this.server.onVoteRequest(VoteRequest.parseFrom(data))
      case Messages.VoteResponse =>
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

  def newServer(serverId: Int, commitFilePath: String, actorSystem: ActorSystem): RaftServer = {
    Raft.newActorBasedServer(serverId, idServerMap, commitFilePath, actorSystem, classOf[ServerActor])
  }

  def main(args: Array[String]) {

    val actorSystem = ActorSystem("YRaft")
    val commitFilePath = "~/tmp/raft"

    (0 to 5).foreach(i => newServer(i, commitFilePath + i, actorSystem).run())
  }
}
