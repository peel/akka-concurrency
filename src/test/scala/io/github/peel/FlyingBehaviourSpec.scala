package io.github.peel

import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import akka.testkit.{TestProbe, TestKit, ImplicitSender, TestFSMRef}
import akka.actor.{ActorSystem, ActorRef}
import io.github.peel.FlyingBehaviour._
import io.github.peel.HeadingIndicator.HeadingUpdate
import io.github.peel.FlyingBehaviour.Fly
import io.github.peel.HeadingIndicator.HeadingUpdate
import io.github.peel.Altimeter.AltitudeUpdate
import io.github.peel.Plane.Controls

class FlyingBehaviourSpec extends TestKit(ActorSystem("FlyingBehaviourSpec"))  with WordSpecLike with Matchers with ImplicitSender {
  val target = CourseTarget(0,0,0)
  def nilActor: ActorRef = TestProbe().ref
  def fsm(plane: ActorRef = nilActor, heading: ActorRef = nilActor, altimeter: ActorRef = nilActor) = {
    TestFSMRef(new FlyingBehaviour(plane, heading, altimeter))
  }

  "FlyingBehaviour" should {
    "start in the Idle state and with Uninitialized data" in {
      val a = fsm()
      a.stateName should be (Idle)
      a.stateData should be (Uninitialized)
    }
  }

  "PreparingToFly state" should {
    "stay in PreparingToFly state when only a HeadingUpdate is received" in {
      val a = fsm()
      a ! Fly(target)
      a ! HeadingUpdate(20)
      a.stateName should be (PreparingToFly)
      val sd = a.stateData.asInstanceOf[FlightData]
      sd.status.altitude should be (-1)
      sd.status.heading should be (20)
    }
    "move to Flying state when all parts are received" in {
      val a = fsm()
      a ! Fly(target)
      a ! HeadingUpdate(20)
      a ! AltitudeUpdate(20)
      a ! Controls(testActor)
      a.stateName should be (Flying)
      val sd = a.stateData.asInstanceOf[FlightData]
      sd.controls should be (testActor)
      sd.status.altitude should be (20)
      sd.status.heading should be (20)
    }
    "transitioning to Flying state" should {
      "create the Adjustment timer" in {
        val a = fsm()
        a.setState(PreparingToFly)
        a.setState(Flying)
        a.isTimerActive("Adjustment") should be (true)
      }
    }
  }
}
