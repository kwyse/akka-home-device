import akka.actor.ActorSystem

import scala.io.StdIn

object IotApp extends App {
  val system = ActorSystem("iot-system")

  try {
    val supervisor = system.actorOf(IotSupervisor.props(), "iot-supervisor")
    StdIn.readLine()
  } finally {
    system.terminate()
  }
}
