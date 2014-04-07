package io.github.peel

import Altimeter.RateChange
import akka.actor.{Actor, ActorRef}
import io.github.peel.ControlSurfaces._
import io.github.peel.ControlSurfaces.StickForward
import io.github.peel.HeadingIndicator.BankChange
import io.github.peel.Altimeter.RateChange
import io.github.peel.ControlSurfaces.StickBack
import io.github.peel.ControlSurfaces.StickRight
import io.github.peel.ControlSurfaces.StickLeft

object ControlSurfaces{
  case class StickBack(amount: Float)
  case class StickForward(amount: Float)
  case class StickLeft(amount: Float)
  case class StickRight(amount: Float)
  case class HasControl(somePilot: ActorRef)
}
class ControlSurfaces(plane: ActorRef, altimeter: ActorRef, heading: ActorRef) extends Actor{
  def receive = controlledBy(context.system.deadLetters)
  def controlledBy(somePilot: ActorRef): Receive = {
    case StickBack(amount) if sender == somePilot =>
      altimeter ! RateChange(amount)
    case StickForward(amount) if sender == somePilot =>
      altimeter ! RateChange(-1*amount)
    case StickLeft(amount) if sender == somePilot =>
      heading ! BankChange(-1*amount)
    case StickRight(amount) if sender == somePilot =>
      heading ! BankChange(-1*amount)
    case HasControl(entity) if sender == plane =>
      context.become(controlledBy(entity))
  }
}
