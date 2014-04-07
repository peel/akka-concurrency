package io.github.peel

import akka.actor.{Props, FSM, Actor, ActorRef}
import scala.concurrent.duration._
import io.github.peel.ControlSurfaces.{StickLeft, StickRight, StickBack, StickForward}
import io.github.peel.FlyingBehaviour._
import io.github.peel.FlyingBehaviour.Fly
import io.github.peel.Plane.{LostControl, Controls, GiveMeControl}
import io.github.peel.EventSource.{UnregisterListener, RegisterListener}
import io.github.peel.Altimeter.AltitudeUpdate
import io.github.peel.HeadingIndicator.HeadingUpdate
import io.github.peel.Pilot.RelinquishControl

trait FlyingProvider{
  def newFlyingBehaviour(plane: ActorRef, heading: ActorRef, altimeter: ActorRef): Props = Props(new FlyingBehaviour(plane, heading, altimeter))
}
object FlyingBehaviour {
  sealed trait State
  case object Idle extends State
  case object Flying extends State
  case object PreparingToFly extends State

  case class CourseTarget(altitude: Double, heading: Float, byMillis: Long)
  case class CourseStatus(altitude: Double, heading: Float, headingSinceMS: Long, altitudeSinceMS: Long)

  type Calculator = (CourseTarget, CourseStatus) => Any

  case class NewElevatorCalculator(f: Calculator)
  case class NewBankCalculator(f: Calculator)

  sealed trait Data
  case object Uninitialized extends Data
  case class FlightData(controls: ActorRef, elevCalc: Calculator, bankCalc: Calculator, target: CourseTarget, status: CourseStatus) extends Data

  case class Fly(target: CourseTarget)

  def currentMS = System.currentTimeMillis

  /**
   * Calculates the amount of elevator change to be made and returns it
   */
  def calcElevator(target: CourseTarget, status: CourseStatus): Any = {
    val alt = (target.altitude-status.altitude).toFloat
    val dur = target.byMillis-status.altitudeSinceMS
    if(alt<0) StickForward((alt/dur) * -1)
    else StickBack(alt/dur)
  }

  def calcAilerons(target: CourseTarget, status: CourseStatus): Any = {
    import scala.math.{abs, signum}
    val diff = target.heading-status.heading
    val dur = target.byMillis-status.headingSinceMS
    val amount = if(abs(diff)<180) diff
                 else signum(diff)*(abs(diff)-360f)
    if(amount>0) StickRight(amount/dur)
    else StickLeft((amount/dur) * -1)
  }
}

class FlyingBehaviour(plane: ActorRef, heading: ActorRef, altimeter: ActorRef) extends Actor with FSM[FlyingBehaviour.State, FlyingBehaviour.Data] {
  case object Adjust

  startWith(Idle, Uninitialized)

  def adjust(flightData: FlightData): FlightData ={
    val FlightData(c, elevCalc, bankCalc, t, s) = flightData
    c ! elevCalc(t, s)
    c ! bankCalc(t, s)
    flightData
  }

  when(Idle) {
    case Event(Fly(target), _) =>
      goto(PreparingToFly) using FlightData(
        context.system.deadLetters,
        calcElevator,
        calcAilerons,
        target,
        CourseStatus(-1, -1, 0, 0))
  }

  onTransition{
    case Idle -> PreparingToFly =>
      plane ! GiveMeControl
      heading ! RegisterListener(self)
      altimeter ! RegisterListener(self)
  }

  /**
   * Guards from premature state change
   */
  def preComplete(data: Data): Boolean = {
    data match {
      case FlightData(c, _, _, _, s) =>
        if(!c.isTerminated && s.heading != -1f && s.altitude != -1f) true
        else false
      case _ =>
        false
    }
  }

  when(PreparingToFly, stateTimeout = 5.seconds)(
    transform{
      case Event(HeadingUpdate(head), d: FlightData) =>
        stay using d.copy(status=d.status.copy(heading=head, headingSinceMS = currentMS))
      case Event(AltitudeUpdate(alt), d: FlightData) =>
        stay using d.copy(status=d.status.copy(altitude = alt, altitudeSinceMS = currentMS))
      case Event(Controls(ctrls), d: FlightData) =>
        stay using d.copy(controls = ctrls)
      case Event(StateTimeout, _) =>
        plane ! LostControl
        goto(Idle)
    } using {
      case s if preComplete(s.stateData) =>
        s.copy(stateName=Flying)
    }
  )

  onTransition{
    case PreparingToFly -> Flying =>
      setTimer("Adjustment", Adjust, 200.milliseconds, repeat = true)
  }

  when(Flying){
    case Event(AltitudeUpdate(alt), d: FlightData) =>
      stay using d.copy(status = d.status.copy(altitude = alt, altitudeSinceMS = currentMS))
    case Event(HeadingUpdate(head), d: FlightData) =>
      stay using d.copy(status=d.status.copy(heading=head, headingSinceMS = currentMS))
    case Event(Adjust, d: FlightData) =>
      stay using adjust(d)
    case Event(NewBankCalculator(f), d: FlightData) =>
      stay using d.copy(bankCalc = f)
    case Event(NewElevatorCalculator(f), d: FlightData) =>
      stay using d.copy(elevCalc = f)
  }

  onTransition {
    case Flying -> _ =>
      cancelTimer("Adjustment")
  }

  onTransition {
    case _ -> Idle =>
      heading ! UnregisterListener(self)
      altimeter ! UnregisterListener(self)
  }

  whenUnhandled {
    case Event(RelinquishControl, _) =>
      goto(Idle)
  }

  initialize
}
