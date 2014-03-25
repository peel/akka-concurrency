package io.github.peel

import akka.actor.{Actor, ActorRef}
import io.github.peel.EventSource.{UnregisterListener, RegisterListener}
import akka.actor.Actor.Receive

object EventSource {
  case class RegisterListener(listener: ActorRef)
  case class UnregisterListener(listener: ActorRef)
}

trait EventSource{
  def sendEvent[T](event: T): Unit
  def eventSourceReceive: Receive
}
trait ProductionEventSource extends EventSource{
  this: Actor =>

  var listeners = Vector.empty[ActorRef]

  def sendEvent[T](event: T):Unit = listeners.foreach{ _ ! event }

  def eventSourceReceive: Receive = {
    case RegisterListener(listener) => listeners = listeners :+ listener
    case UnregisterListener(listener) => listeners = listeners filter {_ != listener}
  }
}
