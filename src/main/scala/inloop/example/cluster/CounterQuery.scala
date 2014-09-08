package inloop.example.cluster

import akka.contrib.pattern.ClusterSharding
import akka.pattern.ask
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success

class CounterQuery extends Actor with ActorLogging {

  implicit val ec = context.dispatcher

  lazy val counter1Region = {
    ClusterSharding(context.system).start(
      typeName = Counter1.shardName,
      entryProps = None,
      idExtractor = Counter1.idExtractor,
      shardResolver = Counter1.shardResolver)

    ClusterSharding(context.system).shardRegion(Counter1.shardName)
  }

  lazy val counter2Region = {
    ClusterSharding(context.system).start(
      typeName = Counter2.shardName,
      entryProps = None,
      idExtractor = Counter2.idExtractor,
      shardResolver = Counter2.shardResolver)

    ClusterSharding(context.system).shardRegion(Counter2.shardName)
  }

  val tickTask = context.system.scheduler.schedule(3.seconds, 3.seconds, self, CounterQuery.Tick)

  def receive = {
    case CounterQuery.Tick =>
      check(Counter1.shardName)
      check(Counter2.shardName)
    case CounterQuery.AskShard(shardName) =>
      check(shardName)
  }

  def check(shardName: String) {
    val region = shardName match {
      case Counter1.shardName => counter1Region
      case Counter2.shardName => counter2Region
    }

    (0 to 10) foreach { id =>
      region ! EntryEnvelope(id, Increment)
      region.ask(Get(id))(2.seconds).onComplete {
        case Success(x) => log.info("Got: id={}, count={}", id, x)
        case Failure(x) => log.error(x, "Error: {}", x.getMessage)
      }
    }
  }
}

object CounterQuery {
  case class AskShard(name: String)
  private case object Tick

  val system = ClusterMonitor.system

  def main(args: Array[String]) {
    //val cluster = Cluster(system)
    val query = system.actorOf(Props[CounterQuery])
    query ! CounterQuery.AskShard(Counter1.shardName)
    query ! CounterQuery.AskShard(Counter2.shardName)
  }
}