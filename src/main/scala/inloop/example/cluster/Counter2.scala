package inloop.example.cluster

import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.ReceiveTimeout
import akka.pattern.ask
import akka.contrib.pattern.ClusterSharding
import akka.contrib.pattern.ShardRegion
import akka.persistence.EventsourcedProcessor
import scala.concurrent.duration._

class Counter2 extends EventsourcedProcessor with ActorLogging {
  import ShardRegion.Passivate

  //context.setReceiveTimeout(120.seconds)

  var count = 10000

  def updateState(event: CounterChanged) {
    log.info("Counter2 Got event {}", event)
    count += event.delta
    self.path
  }

  def receiveRecover: Receive = {
    case evt: CounterChanged => updateState(evt)
  }

  def receiveCommand: Receive = {
    case Increment      => updateState(CounterChanged(+1)) //persist(CounterChanged(+1))(updateState)
    case Decrement      => updateState(CounterChanged(-1)) //persist(CounterChanged(-1))(updateState)
    case Get(_)         => sender() ! (count, "From Counter2: " + self.path.address.port)
    case ReceiveTimeout => context.parent ! Passivate(stopMessage = Stop)
    case Stop           => context.stop(self)
  }
}

object Counter2 {
  val idExtractor: ShardRegion.IdExtractor = {
    case EntryEnvelope(id, payload) => (id.toString, payload)
    case msg @ Get(id)              => (id.toString, msg)
  }

  val shardResolver: ShardRegion.ShardResolver = msg => msg match {
    case EntryEnvelope(id, _) => (id % 10).toString
    case Get(id)              => (id % 10).toString
  }

  def startSharding = {
    val system = ClusterMonitor.system
    ClusterSharding(system).start(
      typeName = "Counter2",
      entryProps = Some(Props[Counter2]),
      idExtractor = idExtractor,
      shardResolver = shardResolver)
  }

  lazy val region1 = {
    val system = ClusterMonitor.system
    ClusterSharding(system).start(
      typeName = "Counter1",
      entryProps = None,
      idExtractor = Counter1.idExtractor,
      shardResolver = Counter1.shardResolver)

    ClusterSharding(system).shardRegion("Counter1")
  }

  def main(args: Array[String]) {
    startSharding
    region1
  }
}