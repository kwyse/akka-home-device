import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

class DeviceGroup(groupId: String) extends Actor with ActorLogging  {
  import DeviceGroup._

  var deviceIdToActor = Map.empty[String, ActorRef]
  var deviceActorToId = Map.empty[ActorRef, String]

  override def receive: Receive = {
    case trackMsg @ DeviceManager.RequestTrackDevice(_, `groupId`, _) =>
      deviceIdToActor.get(trackMsg.deviceId) match {
        case Some(device) => device forward trackMsg
        case None =>
          log.info("Creating device actor for {}", trackMsg.deviceId)
          val deviceActor = context.actorOf(
            Device.props(groupId, trackMsg.deviceId),
            s"device-${trackMsg.deviceId}"
          )

          context.watch(deviceActor)
          deviceIdToActor += trackMsg.deviceId -> deviceActor
          deviceActorToId += deviceActor -> trackMsg.deviceId
          deviceActor forward trackMsg
      }

    case DeviceManager.RequestTrackDevice(_, group, _) =>
      log.warning(
        "Ignoring track device request for {}, this actor is responsible for {}",
        group,
        this.groupId
      )

    case RequestDeviceList(correlationId) =>
      sender() ! ReplyDeviceList(correlationId, deviceIdToActor.keySet)

    case Terminated(deviceActor) =>
      val deviceId = deviceActorToId(deviceActor)
      log.info("Device actor {} has been terminated", deviceId)
      deviceIdToActor -= deviceId
      deviceActorToId -= deviceActor
  }
}

object DeviceGroup {
  def props(groupId: String): Props = Props(new DeviceGroup(groupId))

  final case class RequestDeviceList(correlationId: Long)
  final case class ReplyDeviceList(correlationId: Long, ids: Set[String])
}
