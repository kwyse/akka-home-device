import akka.actor.{ActorSystem, PoisonPill}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class DeviceGroupSpec extends TestKit(ActorSystem("DeviceGroupSpec"))
with WordSpecLike
with Matchers
with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A DeviceGroup" should {
    "register multiple devices" in {
      val probe = TestProbe()
      val groupActor = system.actorOf(DeviceGroup.props("group"))

      groupActor.tell(DeviceManager.RequestTrackDevice(1, "group", "device1"), probe.ref)
      probe.expectMsg(DeviceManager.DeviceRegistered(1))
      val device1 = probe.lastSender

      groupActor.tell(DeviceManager.RequestTrackDevice(2, "group", "device2"), probe.ref)
      probe.expectMsg(DeviceManager.DeviceRegistered(2))
      val device2 = probe.lastSender

      device1 should !==(device2)
    }

    "register a device that responds" in {
      val probe = TestProbe()
      val groupActor = system.actorOf(DeviceGroup.props("group"))

      groupActor.tell(DeviceManager.RequestTrackDevice(1, "group", "device"), probe.ref)
      probe.expectMsg(DeviceManager.DeviceRegistered(1))
      val device = probe.lastSender

      device.tell(Device.RecordTemperature(1, 20.0), probe.ref)
      probe.expectMsg(Device.TemperatureRecorded(1))
    }

    "return existing device actor for existing device ID" in {
      val probe = TestProbe()
      val groupActor = system.actorOf(DeviceGroup.props("group"))

      groupActor.tell(DeviceManager.RequestTrackDevice(1, "group", "device"), probe.ref)
      probe.expectMsg(DeviceManager.DeviceRegistered(1))
      val device1 = probe.lastSender

      groupActor.tell(DeviceManager.RequestTrackDevice(2, "group", "device"), probe.ref)
      probe.expectMsg(DeviceManager.DeviceRegistered(2))
      val device2 = probe.lastSender

      device1 should ===(device2)

      device1.tell(Device.RecordTemperature(3, 40.0), probe.ref)
      probe.expectMsg(Device.TemperatureRecorded(3))
      device2.tell(Device.ReadTemperature(4), probe.ref)
      probe.expectMsg(Device.RespondTemperature(4, Some(40.0)))
    }

    "ignore registration request with wrong group ID" in {
      val probe = TestProbe()
      val groupActor = system.actorOf(DeviceGroup.props("group"))

      groupActor.tell(DeviceManager.RequestTrackDevice(1, "wrongGroup", "device"), probe.ref)
      probe.expectNoMessage(500.milliseconds)
    }

    "list active devices" in {
      val probe = TestProbe()
      val groupActor = system.actorOf(DeviceGroup.props("groupId"))

      groupActor.tell(DeviceManager.RequestTrackDevice(1, "groupId", "device1"), probe.ref)
      probe.expectMsg(DeviceManager.DeviceRegistered(1))
      groupActor.tell(DeviceManager.RequestTrackDevice(2, "groupId", "device2"), probe.ref)
      probe.expectMsg(DeviceManager.DeviceRegistered(2))

      groupActor.tell(DeviceGroup.RequestDeviceList(1), probe.ref)
      probe.expectMsg(DeviceGroup.ReplyDeviceList(1, Set("device1", "device2")))
    }

    "list active devices after one shuts down" in {
      val probe = TestProbe()
      val groupActor = system.actorOf(DeviceGroup.props("groupId"))

      groupActor.tell(DeviceManager.RequestTrackDevice(1, "groupId", "device1"), probe.ref)
      probe.expectMsg(DeviceManager.DeviceRegistered(1))
      val deviceToShutdown = probe.lastSender

      groupActor.tell(DeviceManager.RequestTrackDevice(2, "groupId", "device2"), probe.ref)
      probe.expectMsg(DeviceManager.DeviceRegistered(2))

      groupActor.tell(DeviceGroup.RequestDeviceList(3), probe.ref)
      probe.expectMsg(DeviceGroup.ReplyDeviceList(3, Set("device1", "device2")))

      probe.watch(deviceToShutdown)
      deviceToShutdown ! PoisonPill
      probe.expectTerminated(deviceToShutdown)

      probe.awaitAssert {
        groupActor.tell(DeviceGroup.RequestDeviceList(4), probe.ref)
        probe.expectMsg(DeviceGroup.ReplyDeviceList(4, Set("device2")))
      }
    }
  }
}
