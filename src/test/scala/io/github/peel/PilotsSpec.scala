package io.github.peel

import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpecLike
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.Await
import io.github.peel.Plane.GiveMeControl

class FakePilot extends Actor {
  def receive = {
    case _ => Actor.emptyBehavior
  }
}

object PilotsSpec {
  val copilotName = "Mary"
  val pilotName = "Mark"
  val configStr = s"""
      io.github.peel.flightcrew.copilotName = "$copilotName"
      io.github.peel.flightcrew.pilotName = "$pilotName"
    """
}

class PilotsSpec extends TestKit(ActorSystem("PilotsSpec", ConfigFactory.parseString(PilotsSpec.configStr)))
  with ImplicitSender
  with WordSpecLike
  with MustMatchers {

  import PilotsSpec._

  def nilActor: ActorRef = TestProbe().ref

  val pilotPath = s"/user/TestPilots/$pilotName"
  val copilotPath = s"/user/TestPilost/$copilotName"

  def pilotsReadyToGo(): ActorRef = {
    implicit val askTimeout = Timeout(4.seconds)

    val a = system.actorOf(
      Props(
        new IsolatedStopSupervisor with OneForOneStrategyFactory {
          override def childStarter() {
            context.actorOf(Props[FakePilot], pilotName)
            context.actorOf(Props(new Copilot(testActor, nilActor)), copilotName)
          }
        }
      ), "TestPilots")
    Await.result(a ? IsolatedLifeCycleSupervisor.WaitForStart, 3.seconds)
    system.actorFor(copilotPath) ! Pilot.ReadyToGo
    a
  }

  "Copilots" should {
    "take control when the Pilot dies" in {
      pilotsReadyToGo()
      system.actorFor(pilotPath) ! PoisonPill
      expectMsg(GiveMeControl)
      lastSender must be (system.actorFor(copilotPath))
    }
  }
}
