import akka.actor.{Actor, ActorLogging, Props}

class IotSupervisor extends Actor with ActorLogging {
  override def receive: Receive = Actor.emptyBehavior

  override def preStart(): Unit = log.info("IoT application started")
  override def postStop(): Unit = log.info("IoT application stopped")
}

object IotSupervisor {
  def props(): Props = Props(new IotSupervisor)
}
