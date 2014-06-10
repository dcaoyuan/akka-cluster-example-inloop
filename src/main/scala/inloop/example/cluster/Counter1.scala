package inloop.example.cluster

import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.ReceiveTimeout
import akka.pattern.ask
import akka.contrib.pattern.ClusterSharding
import akka.contrib.pattern.ShardRegion
import akka.persistence.EventsourcedProcessor
import scala.concurrent.duration._

case object Increment
case object Decrement
case class Get(counterId: Long)
case class EntryEnvelope(id: Long, payload: Any)

case object Stop
case class CounterChanged(delta: Int)

class Counter1 extends EventsourcedProcessor with ActorLogging {
  import ShardRegion.Passivate

  //context.setReceiveTimeout(120.seconds)

  var count = 0

  def updateState(event: CounterChanged) {
    log.info("Counter1 got event {}", event)
    count += event.delta
    self.path
  }

  def receiveRecover: Receive = {
    case evt: CounterChanged => updateState(evt)
  }

  def receiveCommand: Receive = {
    case Increment      => updateState(CounterChanged(+1)) //persist(CounterChanged(+1))(updateState)
    case Decrement      => updateState(CounterChanged(-1)) //persist(CounterChanged(-1))(updateState)
    case Get(_)         => sender() ! (count, self.path.address.hostPort)
    case ReceiveTimeout => context.parent ! Passivate(stopMessage = Stop)
    case Stop           => context.stop(self)
  }
}

object Counter1 {
  val idExtractor: ShardRegion.IdExtractor = {
    case EntryEnvelope(id, payload) => (id.toString, payload)
    case msg @ Get(id)              => (id.toString, msg)
  }

  val shardResolver: ShardRegion.ShardResolver = msg => msg match {
    case EntryEnvelope(id, _) => (id % 10).toString
    case Get(id)              => (id % 10).toString
  }

  /**
   * Could also be called by counter1Region users,
   * in what ever cases, cluster sharding should be started first.
   */
  lazy val region = {
    val system = ClusterMonitor.system
    ClusterSharding(system).start(
      typeName = "Counter1",
      entryProps = Some(Props[Counter1]),
      idExtractor = idExtractor,
      shardResolver = shardResolver)
    ClusterSharding(system).shardRegion("Counter1")
  }

  def main(args: Array[String]) {
    region
  }
}