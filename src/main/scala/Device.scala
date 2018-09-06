import akka.actor.{Actor, ActorLogging, Props}

class Device(groupId: String, deviceId: String) extends Actor with ActorLogging {
  import Device._

  var lastTemperatureReading: Option[Double] = None

  override def preStart(): Unit = log.info("Device actor {}-{} started", groupId, deviceId)
  override def postStop(): Unit = log.info("Device actor {}-{} stopped", groupId, deviceId)

  override def receive: Receive = {
    case ReadTemperature(id) =>
      sender() ! RespondTemperature(id, lastTemperatureReading)

    case RecordTemperature(id, value) =>
      lastTemperatureReading = Some(value)
      log.info("Recorded temperature reading {} with correlation ID {}", value, id)

      sender() ! TemperatureRecorded(id)

    // TODO: Matching on hardcoded IDs seems retarded
    case DeviceManager.RequestTrackDevice(correlationId, `groupId`, `deviceId`) =>
      log.info("Registering device {}-{}", groupId, deviceId)
      sender() ! DeviceManager.DeviceRegistered(correlationId)

    case DeviceManager.RequestTrackDevice(_, group, device) =>
      log.warning("Ignoring track device request for {}-{}, this actor is responsible for {}-{}",
        group, device,
        this.groupId, this.deviceId
      )
  }
}

object Device {
  def props(groupId: String, deviceId: String): Props = Props(new Device(groupId, deviceId))

  final case class RecordTemperature(correlationId: Long, value: Double)
  final case class TemperatureRecorded(correlationId: Long)

  final case class ReadTemperature(correlationId: Long)
  final case class RespondTemperature(correlationId: Long, value: Option[Double])
}
