package io.github.peel

import io.github.peel.IsolatedLifeCycleSupervisor.{Started, WaitForStart}
import akka.actor.{ActorKilledException, ActorInitializationException, Actor}
import scala.concurrent.duration.Duration
import akka.actor.SupervisorStrategy.{Escalate, Resume, Stop}

object IsolatedLifeCycleSupervisor {
  case object WaitForStart
  case object Started
}

trait IsolatedLifeCycleSupervisor extends Actor{
  def receive = {
    case WaitForStart => sender ! Started
    case m => throw new Exception(s"Dont's call ${self.path.name} directly ${m}")
  }

  def childStarter(): Unit

  final override def preStart(){ childStarter() }
  final override def postRestart(reason: Throwable){ }
  final override def preRestart(reason: Throwable, message: Option[Any]){ }
}

abstract class IsolatedResumeSupervisor(maxNrRetries: Int = -1, withinTimeRange: Duration = Duration.Inf) extends IsolatedLifeCycleSupervisor{
  this: SupervisionStrategyFactory =>

  override val supervisorStrategy = makeStrategy(maxNrRetries, withinTimeRange){
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: Exception => Resume
    case _ => Escalate
  }
}

abstract class IsolatedStopSupervisor(maxNrRetries: Int = -1, withinTimeRange: Duration = Duration.Inf) extends IsolatedLifeCycleSupervisor{
  this: SupervisionStrategyFactory =>

  override val supervisorStrategy = makeStrategy(maxNrRetries, withinTimeRange){
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: Exception => Resume
    case _ => Escalate
  }
}
