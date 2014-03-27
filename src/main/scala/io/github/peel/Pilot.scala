package io.github.peel

import akka.actor.{ActorRef, Identify, Actor}
import io.github.peel.Pilot.{ReadyToGo, RelinquishControl}
import io.github.peel.Plane.{Controls, GiveMeControl}

trait PilotProvider{
  def newPilot(plane:ActorRef, autopilot:ActorRef, controls:ActorRef, altimeter:ActorRef): Actor = new Pilot(plane, autopilot, controls, altimeter)
  def newCopilot(plane:ActorRef, autopilot:ActorRef, controls:ActorRef, altimeter:ActorRef): Actor = new Copilot(plane, autopilot, controls, altimeter)
  def newAutopilot: Actor = new Autopilot
}

object Pilot {
  case object ReadyToGo
  case object RelinquishControl
}

class Pilot(plane: ActorRef, autopilot: ActorRef, var controls: ActorRef, altimeter: ActorRef) extends Actor{
  var copilot = context.system.deadLetters
  val copilotName = context.system.settings.config.getString("io.github.peel.flightcrew.copilotName")
  def receive =  {
    case ReadyToGo =>
      context.parent ! GiveMeControl
      copilot = context.system.actorFor(s"../$copilotName")
    case Controls(controlSurfaces) =>
      controls = controlSurfaces
  }
}
class Copilot(plane: ActorRef, autopilot: ActorRef, var controls: ActorRef, altimeter: ActorRef) extends Actor{
  var pilot = context.system.deadLetters
  val pilotName = context.system.settings.config.getString("io.github.peel.flightcrew.pilotName")
  def receive =  {
    case ReadyToGo =>
      context.parent ! GiveMeControl
      pilot = context.system.actorFor(s"../$pilotName")
  }
}
class Autopilot extends Actor{
  var controls = context.system.deadLetters
  var pilot = context.system.deadLetters
  var autopilot = context.system.deadLetters
  val pilotName = context.system.settings.config.getString("io.github.peel.flightcrew.pilotName")
  def receive = Actor.emptyBehavior
}
