package io.github.peel

import akka.actor.{Props, Actor, ActorSystem}
import akka.testkit.{TestActorRef, TestLatch, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import io.github.peel.Altimeter.{AltitudeUpdate, RateChange}
import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit

class AltimeterSpec extends TestKit(ActorSystem("AltimeterSpec")) with WordSpecLike with Matchers with BeforeAndAfterAll with ImplicitSender {
  override def afterAll(){system.shutdown()}

  class Helper{
    object EventSourceSpy{
      val latch = TestLatch(1)
    }

    trait EventSourceSpy extends EventSource {
      def sendEvent[T](event: T): Unit = EventSourceSpy.latch.countDown()
      def eventSourceReceive = Actor.emptyBehavior
    }

    def slicedAltimeter = new Altimeter with EventSourceSpy
    def actor() = {
        val a = TestActorRef[Altimeter](Props(slicedAltimeter))
        (a, a.underlyingActor)
      }
  }

  "Altimeter" should {
    "record rate of climb changes" in new Helper{
      val (_, real) = actor()
      real.receive(RateChange(1f))
      real.rateOfClimb should be (real.maxRateOfClimb)
    }
    "keep rate of climb changes within bounds" in new Helper {
      val (_, real) = actor()
      real.receive(RateChange(2f))
      real.rateOfClimb should be (real.maxRateOfClimb)
    }
    "calculate altitude changes" in new Helper{
      val ref = system.actorOf(Props(Altimeter()))
      ref ! EventSource.RegisterListener(testActor)
      ref ! RateChange(1f)
      fishForMessage(){
        case AltitudeUpdate(altitude) if altitude == 0f => false
        case AltitudeUpdate(altitude) => true
      }
    }
    "send events" in new Helper{
      val (ref, _) = actor()
      Await.ready(EventSourceSpy.latch, FiniteDuration(1, TimeUnit.SECONDS))
      EventSourceSpy.latch.isOpen should be (true)
    }
  }
}
