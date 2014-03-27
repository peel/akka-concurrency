package io.github.peel

import akka.actor.{ActorRef, Props, Actor, ActorLogging}
import akka.pattern.ask
import io.github.peel.Plane.{Controls, GiveMeControl}
import io.github.peel.EventSource.RegisterListener
import io.github.peel.Altimeter.AltitudeUpdate
import io.github.peel.Pilot.ReadyToGo
import scala.concurrent.Await
import scala.concurrent.duration._
import io.github.peel.IsolatedLifeCycleSupervisor.WaitForStart

object Plane {
  // returns the control surface to the Actor that asks for them
  case object GiveMeControl
  case class Controls(controls: ActorRef)
}
class Plane extends Actor with ActorLogging{
  this: AltimeterProvider
    with PilotProvider
    with LeadFlightAttendantProvider =>

  val cfgstr = "io.github.peel.flightcrew"
  val config = context.system.settings.config
  val pilotName = config.getString(s"$cfgstr.pilotName")
  val copilotName = config.getString(s"$cfgstr.copilotName")
  val attendantName = config.getString(s"$cfgstr.leadAttendantName")

  def actorForControls(name: String) = context.actorFor("Equipment/"+name)

  def startEquipment() {
    val controls = context.actorOf(
      Props(new IsolatedResumeSupervisor with OneForOneStrategyFactory {
        override def childStarter() {
          val alt = context.actorOf(Props(newAltimeter), "Altimeter")
          context.actorOf(Props(newAutopilot), "Autopilot")
          context.actorOf(Props(new ControlSurfaces(alt)), "ControlSurfaces")
        }
      }), "Equipment")
    Await.result(controls ? WaitForStart, 1.second)
  }

  def startPeople() {
    val plane = self
    val controls = actorForControls("ControlSurfaces")
    val autopilot = actorForControls("Autopilot")
    val altimeter = actorForControls("Altimeter")
    val people = context.actorOf(
      Props(new IsolatedStopSupervisor with OneForOneStrategyFactory {
        override def childStarter() {
          context.actorOf(Props(newPilot(plane, autopilot, controls, altimeter)), pilotName)
          context.actorOf(Props(newCopilot(plane, altimeter)), copilotName)
        }
      }), "Pilots")
    context.actorOf(Props(newLeadFlightAttendant), attendantName)
    Await.result(people ? WaitForStart, 1.second)
  }

  override def preStart(){
    startEquipment()
    startPeople()
    actorForControls("Altimeter") ! RegisterListener(self)
    actorForControls(pilotName) ! ReadyToGo
    actorForControls(copilotName) ! ReadyToGo
  }

  def receive = {
    case GiveMeControl =>
      log info "io.github.peel.Plane giving control."
      sender ! actorForControls("ControlSurfaces")
    case AltitudeUpdate(altitude) =>
      log info s"Altitude is now $altitude"
  }
}
