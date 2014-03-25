package io.github.peel

import akka.actor.{Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.scalatest.{Matchers, WordSpecLike}
import io.github.peel.FlightAttendant.{Drink, GetDrink}
import com.typesafe.config.ConfigFactory

object TestFlightAttendant{
  def apply() = new FlightAttendant with AttendantResponsiveness{val maxResponseTimeMS = 1}
}
class FlightAttendantSpec extends TestKit(ActorSystem("FlightAttendantSpec", ConfigFactory.parseString("akka.scheduler.tick-duration=1ms"))) with ImplicitSender with WordSpecLike with Matchers{
  "FlightAttendant" should{
    "get a drink when asked" in {
      val actor = TestActorRef(Props(TestFlightAttendant()))
      actor ! GetDrink("Soda")
      expectMsg(Drink("Soda"))
    }
  }

}
