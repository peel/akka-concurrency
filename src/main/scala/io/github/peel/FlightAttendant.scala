package io.github.peel

import scala.util.Random
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import akka.actor.Actor
import io.github.peel.FlightAttendant.{Drink, GetDrink}

trait AttendantResponsiveness {
  val maxResponseTimeMS: Int
  def responseDuration = FiniteDuration(Random.nextInt(maxResponseTimeMS), TimeUnit.MILLISECONDS)
}
object FlightAttendant{
  case class GetDrink(drinkname: String)
  case class Drink(drinkname: String)

  def apply() = new FlightAttendant with AttendantResponsiveness{val maxResponseTimeMS=30000}
}
class FlightAttendant extends Actor{
  this: AttendantResponsiveness =>

  implicit val ec = context.dispatcher

  def receive = {
    case GetDrink(drinkname) => context.system.scheduler.scheduleOnce(responseDuration, sender, Drink(drinkname))
  }
}
