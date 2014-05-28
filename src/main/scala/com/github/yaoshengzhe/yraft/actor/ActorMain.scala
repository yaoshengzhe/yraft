package com.github.yaoshengzhe.yraft.actor

import akka.actor.{Props, ActorSystem, Actor}
import com.github.yaoshengzhe.yraft.statemachine.LocalDiskStateMachine
import java.io.File
import com.github.yaoshengzhe.yraft.network.ActorBasedCommunicator
import com.github.yaoshengzhe.yraft.{Messages, RaftServer}
import com.github.yaoshengzhe.yraft.protobuf.generated.RaftProtos.{AppendEntriesResponse, AppendEntriesRequest, VoteResponse, VoteRequest}
import com.github.yaoshengzhe.yraft.exception.UnknownRaftMessageException
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import com.github.yaoshengzhe.yraft.timer.ScheduledExecutorTimerService
import java.util.concurrent.TimeUnit

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

  def newServer(commitFilePath: String, actorSystem: ActorSystem): RaftServer = {

    val server: RaftServer = RaftServer.newBuilder
      .setStateMachine(new LocalDiskStateMachine(new File(commitFilePath)))
      .setTimerService(new ScheduledExecutorTimerService(150, TimeUnit.MILLISECONDS))
      .build()

    val serverActor = actorSystem.actorOf(Props(classOf[ServerActor], server))
    // Then set communicator, order is important here:)
    server.setCommunicator(new ActorBasedCommunicator(serverActor))
    server
  }

  def main(args: Array[String]) {

    val actorSystem = ActorSystem("YRaft")
    val commitFilePath = "~/tmp/raft"

    (0 to 10).foreach(i => newServer(commitFilePath + i, actorSystem).run())
  }
}
