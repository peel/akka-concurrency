package io.github.peel

import akka.actor.{OneForOneStrategy, Props, ActorLogging, Actor}
import scala.concurrent.duration._
import io.github.peel.Altimeter.{AltitudeCalculated, CalculateAltitude, AltitudeUpdate, RateChange}
import akka.actor.SupervisorStrategy.Restart

trait AltimeterProvider{
 def newAltimeter: Actor = Altimeter()
}

object Altimeter{
  case class RateChange(amount: Float)
  case class AltitudeUpdate(altitude: Double)
  case class CalculateAltitude(lastTick: Long, tick: Long, roc: Double)
  case class AltitudeCalculated(newTick: Long, altitude: Double)
  def apply() = new Altimeter with ProductionEventSource
}

class Altimeter extends Actor with ActorLogging {
  this: EventSource =>
  implicit val ec = context.dispatcher
  val ceiling = 43000
  val maxRateOfClimb = 5000
  var rateOfClimb = 0f
  var altitude = 0d
  var lastTick = System.currentTimeMillis
  val ticker = context.system.scheduler.schedule(100.milliseconds, 100.milliseconds, self, Tick)

  override val supervisorStrategy = OneForOneStrategy(-1, Duration.Inf){case _ => Restart}

  val altitudeCalculator = context.actorOf(
    Props(
      new Actor {
        def receive = {
          case CalculateAltitude(lastTick, tick, roc) =>
              val alt = (tick - lastTick) / 60000.00 * roc
              sender ! AltitudeCalculated(tick, alt)
        }
      }
  ), "AltitudeCalculator")

  case object Tick

  def receive =  eventSourceReceive orElse altimeterReceive
  def altimeterReceive : Receive = {
    case RateChange(amount) =>
      rateOfClimb = amount.min(1.0f).max(-1.0f)*maxRateOfClimb
      log info s"Altimeter changed rate of climb to $rateOfClimb"
    case Tick =>
      val tick = System.currentTimeMillis
      altitudeCalculator ! CalculateAltitude(lastTick, tick, rateOfClimb)
      lastTick=tick
    case AltitudeCalculated(tick, altdelta) =>
      altitude += altdelta
      sendEvent(AltitudeUpdate(altitude))
  }

  override def postStop() = ticker.cancel()
}
