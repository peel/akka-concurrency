package io.github.peel

import akka.actor.{Terminated, ActorRef, Identify, Actor}
import io.github.peel.Pilot.{ReadyToGo, RelinquishControl}
import io.github.peel.Plane.{Controls, GiveMeControl}
import io.github.peel.FlyingBehaviour.{NewElevatorCalculator, Fly, CourseTarget, Calculator}
import akka.actor.FSM.{CurrentState, Transition, SubscribeTransitionCallBack}
import io.github.peel.DrinkingBehaviour.{FeelingLikeZaphod, FeelingTipsy, FeelingSober}

trait PilotProvider{
  def newPilot(plane:ActorRef, autopilot:ActorRef, controls:ActorRef, altimeter:ActorRef): Actor = new Pilot(plane, autopilot, controls, altimeter) with DrinkingProvider with FlyingProvider
  def newCopilot(plane:ActorRef, altimeter:ActorRef): Actor = new Copilot(plane, altimeter) with FlyingProvider
  def newAutopilot: Actor = new Autopilot
}

object Pilot {
  import FlyingBehaviour._
  import ControlSurfaces._

  case object ReadyToGo
  case object RelinquishControl

  val tipsyCalcElevator: Calculator = {
    (target, status) =>
      val msg = calcElevator(target, status)
      msg match {
        case StickForward(amt) => StickForward(amt*1.03f)
        case StickBack(amt) => StickBack(amt*1.03f)
        case m => m
      }
  }
  val tipsyCalcAilerons: Calculator = {
    (target, status) =>
    val msg = calcAilerons(target, status)
    msg match{
      case StickLeft(amt) => StickLeft(amt*1.03f)
      case StickRight(amt) => StickRight(amt*1.03f)
      case m => m
    }
  }

  val zaphodCalcElevator: Calculator = {
    (target, status) =>
      val msg = calcElevator(target, status)
      msg match {
        case StickForward(amt) => StickForward(1f)
        case StickBack(amt) => StickBack(1f)
        case m => m
      }
  }
  val zaphodCalcAilerons: Calculator = {
    (target, status) =>
      val msg = calcAilerons(target, status)
      msg match{
        case StickLeft(amt) => StickLeft(1f)
        case StickRight(amt) => StickRight(1f)
        case m => m
      }
  }

}

class Pilot(plane: ActorRef, autopilot: ActorRef, var heading: ActorRef, altimeter: ActorRef) extends Actor{
  this: DrinkingProvider with FlyingProvider =>

  import FlyingBehaviour._
  import Pilot._
  import ControlSurfaces._

  var copilot = context.system.deadLetters
  val copilotName = context.system.settings.config.getString("io.github.peel.flightcrew.copilotName")

  def setCourse(flyer: ActorRef){
    flyer ! Fly(CourseTarget(20000, 250, System.currentTimeMillis+30000))
  }

  override def preStart(){
    context.actorOf(newDrinkingBehaviour(self), "DrinkingBehaviour")
    context.actorOf(newFlyingBehaviour(plane, heading, altimeter), "FlyingBehaviour")
  }

  def bootstrap: Receive = {
    case ReadyToGo =>
      val copilot = context.actorFor("../"+copilotName)
      val flyer = context.actorFor("FlyingBehaviour")
      flyer ! SubscribeTransitionCallBack(self)
      setCourse(flyer)
      context.become(sober(copilot, flyer))
  }

  def sober(copilot: ActorRef, flyer: ActorRef): Receive ={
    case FeelingSober =>
    case FeelingTipsy => becomeTipsy(copilot, flyer)
    case FeelingLikeZaphod => becomeZaphod(copilot, flyer)
  }

  def tipsy(copilot: ActorRef, flyer: ActorRef): Receive ={
    case FeelingSober => becomeSober(copilot, flyer)
    case FeelingTipsy =>
    case FeelingLikeZaphod => becomeZaphod(copilot, flyer)
  }

  def zaphod(copilot: ActorRef, flyer: ActorRef): Receive ={
    case FeelingSober => becomeSober(copilot, flyer)
    case FeelingTipsy => becomeTipsy(copilot, flyer)
    case FeelingLikeZaphod =>
  }

  def idle: Receive ={
    case _ =>
  }

  def becomeSober(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(calcElevator)
    flyer ! NewBankCalculator(calcAilerons)
    context.become(sober(copilot, flyer))
  }

  def becomeTipsy(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(tipsyCalcElevator)
    flyer ! NewElevatorCalculator(tipsyCalcAilerons)
    context.become(tipsy(copilot, flyer))
  }

  def becomeZaphod(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(zaphodCalcElevator)
    flyer ! NewElevatorCalculator(zaphodCalcAilerons)
    context.become(zaphod(copilot, flyer))
  }

  override def unhandled(msg: Any): Unit = {
    msg match {
      case Transition(_,_,Flying) =>
        setCourse(sender)
      case Transition(_,_,Idle) =>
        context.become(idle)
      case Transition(_,_,_) =>
      case CurrentState(_, _) =>
      case m => super.unhandled(m)
    }
  }

  def receive = bootstrap
}
class Copilot(plane: ActorRef, altimeter: ActorRef) extends Actor{
  var pilot = context.system.deadLetters
  val pilotName = context.system.settings.config.getString("io.github.peel.flightcrew.pilotName")
  def receive =  {
    case ReadyToGo =>
      context.parent ! GiveMeControl
      pilot = context.system.actorFor(s"../$pilotName")
      context.watch(pilot)
    case Terminated(_) =>
      plane ! GiveMeControl
  }
}
class Autopilot extends Actor{
  var controls = context.system.deadLetters
  var pilot = context.system.deadLetters
  var autopilot = context.system.deadLetters
  val pilotName = context.system.settings.config.getString("io.github.peel.flightcrew.pilotName")
  def receive = Actor.emptyBehavior
}
