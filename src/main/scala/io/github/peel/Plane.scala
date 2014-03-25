package io.github.peel

import akka.actor.{ActorRef, Props, Actor, ActorLogging}
import io.github.peel.Plane.{Controls, GiveMeControl}
import io.github.peel.EventSource.RegisterListener
import io.github.peel.Altimeter.AltitudeUpdate
import io.github.peel.Pilot.ReadyToGo

object Plane {
  // returns the control surface to the Actor that asks for them
  case object GiveMeControl
  case class Controls(controls: ActorRef)
}
class Plane extends Actor with ActorLogging{
  val cfgstr = "io.github.peel.flightcrew"
  val config = context.system.settings.config
  val altimeter = context.actorOf(Props(Altimeter()), "Altimeter")
  val controls = context.actorOf(Props(new ControlSurfaces(altimeter)), "ControlSurfaces")
  val pilot = context.actorOf(Props[Pilot], config.getString(s"$cfgstr.pilotName"))
  val copilot = context.actorOf(Props[Copilot], config.getString(s"$cfgstr.copilotName"))
  val autopilot = context.actorOf(Props[Autopilot], "Autopilot")
  val flightAttendant = context.actorOf(Props[LeadFlightAttendant], config.getString(s"$cfgstr.leadAttendantName"))
  pilot :: copilot :: Nil foreach(_ ! ReadyToGo)

  override def preStart(){
    altimeter ! RegisterListener(self)
  }

  def receive = {
    case GiveMeControl =>
      log info "io.github.peel.Plane giving control."
      sender ! Controls(controls)
    case AltitudeUpdate(altitude) =>
      log info s"Altitude is now $altitude"
  }
}
