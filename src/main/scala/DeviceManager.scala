object DeviceManager {
  final case class RequestTrackDevice(correlationId: Long, groupId: String, deviceId: String)
  final case class DeviceRegistered(correlationId: Long)
}
