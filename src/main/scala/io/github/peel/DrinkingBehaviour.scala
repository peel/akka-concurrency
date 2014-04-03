package io.github.peel

import akka.actor.{Props, Actor, ActorRef}
import scala.concurrent.duration._
import scala.util.Random
import io.github.peel.DrinkingBehaviour.{FeelingLikeZaphod, FeelingTipsy, FeelingSober, LevelChanged}

object DrinkingBehaviour {
  case class LevelChanged(level: Float)

  case object FeelingSober
  case object FeelingTipsy
  case object FeelingLikeZaphod

  def apply(drinker: ActorRef) = new DrinkingBehaviour(drinker) with DrinkingResolution
}

trait DrinkingProvider {
  def newDrinkingBehaviour(drinker:ActorRef): Props = Props(DrinkingBehaviour(drinker))
}

trait DrinkingResolution{
  def initialSobering: FiniteDuration = 1.second
  def soberingInterval: FiniteDuration = 1.second
  def drinkingInterval: FiniteDuration = Random.nextInt(300).seconds
}

class DrinkingBehaviour(drinker: ActorRef) extends Actor {
  this: DrinkingResolution =>

  var currentLevel = 0f
  val scheduler = context.system.scheduler

  val sobering = scheduler.schedule(initialSobering, soberingInterval, self, LevelChanged(-0.0001f))

  override def postStop(){
    sobering.cancel()
  }

  override def preStart(){
    drink()
  }

  def drink() = scheduler.scheduleOnce(drinkingInterval, self, LevelChanged(0.005f))

  def receive = {
    case LevelChanged(amount) =>
      currentLevel = (currentLevel+amount).max(0f)
      drinker ! (if(currentLevel <= 0.01f){
                  drink()
                  FeelingSober
                }else if(currentLevel<=0.03f){
                  drink()
                  FeelingTipsy
                }else FeelingLikeZaphod)
  }
}
