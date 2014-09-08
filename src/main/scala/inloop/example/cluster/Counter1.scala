package inloop.example.cluster

import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.ReceiveTimeout
import akka.pattern.ask
import akka.contrib.pattern.ClusterSharding
import akka.contrib.pattern.ShardRegion
import akka.persistence.PersistentActor
import scala.concurrent.duration._

case object Increment
case object Decrement
case class Get(counterId: Long)
case class EntryEnvelope(id: Long, payload: Any)

case object Stop
case class CounterChanged(delta: Int)

class Counter1 extends PersistentActor with ActorLogging {
  import ShardRegion.Passivate

  //context.setReceiveTimeout(120.seconds)

  var count = 0

  override def persistenceId = self.path.toStringWithoutAddress

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
    case Get(_)         => sender() ! (count, "From Counter1: " + self.path.address.port)
    case ReceiveTimeout => context.parent ! Passivate(stopMessage = Stop)
    case Stop           => context.stop(self)
  }
}

object Counter1 {
  val shardName = "Counter1"

  val idExtractor: ShardRegion.IdExtractor = {
    case EntryEnvelope(id, payload) => (id.toString, payload)
    case msg @ Get(id)              => (id.toString, msg)
  }

  val shardResolver: ShardRegion.ShardResolver = msg => msg match {
    case EntryEnvelope(id, _) => (id % 10).toString
    case Get(id)              => (id % 10).toString
  }

  private def startShard = {
    val system = ClusterMonitor.system
    ClusterSharding(system).start(
      typeName = shardName,
      entryProps = Some(Props[Counter1]),
      idExtractor = idExtractor,
      shardResolver = shardResolver)
  }

  private lazy val region2 = {
    val system = ClusterMonitor.system
    ClusterSharding(system).start(
      typeName = Counter2.shardName,
      entryProps = None,
      idExtractor = Counter2.idExtractor,
      shardResolver = Counter2.shardResolver)

    ClusterSharding(system).shardRegion(Counter2.shardName)
  }

  def main(args: Array[String]) {
    startShard
    region2
  }
}