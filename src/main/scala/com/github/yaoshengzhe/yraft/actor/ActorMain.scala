package com.github.yaoshengzhe.yraft.actor

import akka.actor.{Props, ActorSystem, Actor}

class ServerActor extends Actor {

  def receive = {
    case "Hello" =>
      println("World")
  }
}

object ActorMain {
  val actorSystem = ActorSystem("Yraft")
  val serverActor = actorSystem.actorOf(Props[ServerActor])

  def send (msg: String) {
    serverActor ! msg
    Thread.sleep(100)
  }

  def main(args: Array[String]) {
    send("Hello")
    send("Hello")
    actorSystem.shutdown()
  }
}
