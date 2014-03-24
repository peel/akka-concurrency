package io.github.peel

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.util.Timeout
import akka.pattern.ask
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration

import scala.concurrent.ExecutionContext.Implicits.global

object Avionics {
  implicit val timeout = Timeout(5, TimeUnit.SECONDS)
  val system = ActorSystem("PlaneSimulation")
  val plane = system.actorOf(Props[Plane], "io.github.peel.Plane")

  def main(args: Array[String]){
    val control = Await.result(
      (plane ? Plane.GiveMeControl).mapTo[ActorRef],
      FiniteDuration(5, TimeUnit.SECONDS)
    )
    system.scheduler.scheduleOnce(FiniteDuration(200, TimeUnit.MILLISECONDS)){
      control ! ControlSurfaces.StickBack(1f)
    }
    system.scheduler.scheduleOnce(FiniteDuration(1, TimeUnit.SECONDS)){
      control ! ControlSurfaces.StickBack(0f)
    }
    system.scheduler.scheduleOnce(FiniteDuration(3, TimeUnit.SECONDS)){
      control ! ControlSurfaces.StickBack(0.5f)
    }
    system.scheduler.scheduleOnce(FiniteDuration(4, TimeUnit.SECONDS)){
      control ! ControlSurfaces.StickBack(0f)
    }
    system.scheduler.scheduleOnce(FiniteDuration(5, TimeUnit.SECONDS)){
      system.shutdown()
    }
  }
}
