package io.github.peel

import Altimeter.RateChange
import akka.actor.{Actor, ActorRef}
import io.github.peel.ControlSurfaces.{StickForward, StickBack}

object ControlSurfaces{
  case class StickBack(amount: Float)
  case class StickForward(amount: Float)
}
class ControlSurfaces(altimeter: ActorRef) extends Actor{
  def receive = {
    case StickBack(amount) =>
      altimeter ! RateChange(amount)
    case StickForward(amount) =>
      altimeter ! RateChange(-1*amount)
  }
}
