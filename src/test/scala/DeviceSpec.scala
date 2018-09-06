import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class DeviceSpec extends TestKit(ActorSystem("DeviceSpec"))
with WordSpecLike
with Matchers
with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A Device" should {
    "reply with empty reading if temperature is unknown" in {
      val probe = TestProbe()
      val deviceActor = system.actorOf(Device.props("group", "device"))

      deviceActor.tell(Device.ReadTemperature(correlationId = 42), probe.ref)
      val response = probe.expectMsgType[Device.RespondTemperature]
      response.correlationId should ===(42)
      response.value should ===(None)
    }

    "reply with the latest temperature reading if known" in {
      val probe = TestProbe()
      val deviceActor = system.actorOf(Device.props("group", "device"))

      deviceActor.tell(Device.RecordTemperature(1, 24.0), probe.ref)
      probe.expectMsg(Device.TemperatureRecorded(1))

      deviceActor.tell(Device.ReadTemperature(correlationId = 2), probe.ref)
      val response1 = probe.expectMsgType[Device.RespondTemperature]
      response1.correlationId should ===(2)
      response1.value should ===(Some(24))

      deviceActor.tell(Device.RecordTemperature(3, 32.6), probe.ref)
      probe.expectMsg(Device.TemperatureRecorded(3))

      deviceActor.tell(Device.ReadTemperature(correlationId = 4), probe.ref)
      val response2 = probe.expectMsgType[Device.RespondTemperature]
      response2.correlationId should ===(4)
      response2.value should ===(Some(32.6))
    }
  }
}
