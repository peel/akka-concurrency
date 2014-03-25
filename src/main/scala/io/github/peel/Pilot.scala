package io.github.peel

import akka.actor.{Identify, Actor}
import io.github.peel.Pilot.{ReadyToGo, RelinquishControl}
import io.github.peel.Plane.{Controls, GiveMeControl}

object Pilot {
  case object ReadyToGo
  case object RelinquishControl
}
class Pilot extends Actor{
  var controls = context.system.deadLetters
  var copilot = context.system.deadLetters
  var autopilot = context.system.deadLetters
  val copilotName = context.system.settings.config.getString("io.github.peel.flightcrew.copilotName")
  def receive =  {
    case ReadyToGo =>
      context.parent ! GiveMeControl
      copilot = context.system.actorFor(s"../$copilotName")
      autopilot = context.system.actorFor("../Autopilot")
    case Controls(controlSurfaces) =>
      controls = controlSurfaces
  }
}
class Copilot extends Actor{
  var controls = context.system.deadLetters
  var pilot = context.system.deadLetters
  var autopilot = context.system.deadLetters
  val pilotName = context.system.settings.config.getString("io.github.peel.flightcrew.pilotName")
  def receive =  {
    case ReadyToGo =>
      context.parent ! GiveMeControl
      pilot = context.system.actorFor(s"../$pilotName")
      autopilot = context.system.actorFor("../Autopilot")
  }
}
class Autopilot extends Actor{
  var controls = context.system.deadLetters
  var pilot = context.system.deadLetters
  var autopilot = context.system.deadLetters
  val pilotName = context.system.settings.config.getString("io.github.peel.flightcrew.pilotName")
  def receive = Actor.emptyBehavior
}
