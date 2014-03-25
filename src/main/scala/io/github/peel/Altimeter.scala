package io.github.peel

import akka.actor.{ActorLogging, Actor}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import io.github.peel.Altimeter.{AltitudeUpdate, RateChange}

object Altimeter{
  case class RateChange(amount: Float)
  case class AltitudeUpdate(altitude: Double)
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
  val ticker = context.system.scheduler.schedule(FiniteDuration(100, TimeUnit.MILLISECONDS), FiniteDuration(100, TimeUnit.MILLISECONDS), self, Tick)
  case object Tick

  def receive =  eventSourceReceive orElse altimeterReceive
  def altimeterReceive : Receive = {
    case RateChange(amount) => //climb rate changed
      rateOfClimb = amount.min(1.0f).max(-1.0f)*maxRateOfClimb
      log info s"io.github.peel.Altimeter changed rate of climb to $rateOfClimb"
    case Tick => //calcuate new altitude
      val tick = System.currentTimeMillis
      altitude = altitude+((tick-lastTick)/60000.0)*rateOfClimb
      sendEvent(AltitudeUpdate(altitude))
      lastTick=tick
  }

  override def postStop() = ticker.cancel()
}
