package io.github.peel

import akka.actor.{Props, Actor, ActorLogging}
import io.github.peel.Plane.GiveMeControl
import io.github.peel.EventSource.RegisterListener
import io.github.peel.Altimeter.AltitudeUpdate

object Plane {
  // returns the control surface to the Actor that asks for them
  case object GiveMeControl
}
class Plane extends Actor with ActorLogging{
  val altimeter = context.actorOf(Props[Altimeter], "io.github.peel.Altimeter")
  val controls = context.actorOf(Props(new ControlSurfaces(altimeter)), "ControlSurfaces")

  override def preStart(){
    altimeter ! RegisterListener(self)
  }

  def receive = {
    case GiveMeControl =>
      log info "io.github.peel.Plane giving control."
      sender ! controls
    case AltitudeUpdate(altitude) =>
      log info s"Altitude is now $altitude"
  }
}
