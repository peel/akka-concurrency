package io.github.peel

import akka.actor.{Actor, ActorSystem}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.scalatest.{Matchers, WordSpecLike, BeforeAndAfterAll}
import io.github.peel.EventSource.{UnregisterListener, RegisterListener}

class TestEventSource extends Actor with ProductionEventSource{
  def receive = eventSourceReceive
}

class EventSourceSpec extends TestKit(ActorSystem("EventSourceSpec")) with WordSpecLike with Matchers with BeforeAndAfterAll{
  override def afterAll(){ system.shutdown() }
  "EventSource" should {
    "allow us to register a listener" in {
      val real = TestActorRef[TestEventSource].underlyingActor
      real.receive(RegisterListener(testActor))
      real.listeners should contain (testActor)
    }
    "allow us deregister a listener" in {
      val real = TestActorRef[TestEventSource].underlyingActor
      real.receive(RegisterListener(testActor))
      real.receive(UnregisterListener(testActor))
      real.listeners.size should be (0)
    }
    "send message to actor after registration" in {
      val testA = TestActorRef[TestEventSource]
      testA ! RegisterListener(testActor)
      val msg: String = "Test"
      testA.underlyingActor.sendEvent(msg)
      expectMsg(msg)
    }
  }
}
