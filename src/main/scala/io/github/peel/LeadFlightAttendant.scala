package io.github.peel

import akka.actor.{Props, ActorRef, Actor}
import akka.actor.Actor.Receive
import io.github.peel.LeadFlightAttendant.{Attendant, GetFlightAttendant}
import scala.util.Random

trait AttendantCreationPolicy {
  val numberOfAttenants: Int =8
  def createAttendant: Actor = FlightAttendant()
}

trait LeadFlightAttendantProvider {
  def newLeadFlightAttendant: Actor = LeadFlightAttendant()
}

object LeadFlightAttendant {
  case object GetFlightAttendant
  case class Attendant(a: ActorRef)
  def apply() = new LeadFlightAttendant with AttendantCreationPolicy
}

class LeadFlightAttendant extends Actor{
  this: AttendantCreationPolicy =>

  override def preStart(){
    import scala.collection.JavaConverters._
    val attendantNames = context.system.settings.config.getStringList("io.github.peel.flightcrew.attendantNames").asScala
    attendantNames take numberOfAttenants foreach {
      name => context.actorOf(Props(createAttendant),name)
    }
  }

  def randomAttendant(): ActorRef = context.children.take(Random.nextInt(numberOfAttenants)+1).last

  def receive = {
    case GetFlightAttendant => sender ! Attendant(randomAttendant())
    case m => randomAttendant() forward m
  }
}
object FlightAttendantPathChecker extends App{
    val system = akka.actor.ActorSystem("PlaneSimulation")
    val lead = system.actorOf(Props(new LeadFlightAttendant with AttendantCreationPolicy), "LeadFlightAttendant")
    Thread.sleep(2000)
    system.shutdown()
}