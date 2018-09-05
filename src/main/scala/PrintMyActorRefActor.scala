import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.io.StdIn

object ActorHierarchyExperiments extends App {
  val system = ActorSystem("test-system")

  val firstRef = system.actorOf(Props[PrintMyActorRefActor], "first-actor")
  println(s"First: $firstRef")
  firstRef ! "print"

  val first = system.actorOf(Props[StartStopActor1], "first")
  first ! "stop"

  val supervisingActor = system.actorOf(Props[SupervisingActor], "supervising-actor")
  supervisingActor ! "failChild"

  println(">>> Press ENTER to exit")
  try StdIn.readLine()
  finally system.terminate()
}

class PrintMyActorRefActor extends Actor {
  override def receive: Receive = {
    case "print" =>
      val secondRef = context.actorOf(Props.empty, "second-actor")
      println(s"Second: $secondRef")
  }
}

class StartStopActor1 extends Actor {
  override def preStart(): Unit = {
    println("first started")
    context.actorOf(Props[StartStopActor2], "second")
  }

  override def postStop(): Unit = println("first stopped")

  override def receive: Receive = {
    case "stop" => context.stop(self)
  }
}

class StartStopActor2 extends Actor {
  override def preStart(): Unit = println("second started")

  override def postStop(): Unit = println("second stopped")

  override def receive: Receive = Actor.emptyBehavior
}

class SupervisingActor extends Actor {
  val child: ActorRef = context.actorOf(Props[SupervisedActor], "supervised-actor")

  override def receive: Receive = {
    case "failChild" => child ! "fail"
  }
}

class SupervisedActor extends Actor {
  override def preStart(): Unit = println("supervised actor started")

  override def postStop(): Unit = println("supervised actor stopped")

  override def receive: Receive = {
    case "fail" =>
      println("supervised actor failed now")
      throw new Exception("I failed")
  }
}
