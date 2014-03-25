package io.github.peel

import akka.actor.{Props, ActorSystem}

object FlightAttendantPathChecker extends App{
  val system = ActorSystem("PlaneSimulation")
  val lead = system.actorOf(Props(new LeadFlightAttendant with AttendantCreationPolicy), system.settings.config.getString("io.github.peel.flightcrew.leadAttendantName"))
  Thread.sleep(2000)
  system.shutdown()
}
