package inloop.example.cluster

import akka.contrib.pattern.ClusterSharding
import akka.pattern.ask
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success

class CounterQuery extends Actor with ActorLogging {

  implicit val ec = context.dispatcher
  lazy val counter1Region = {
    ClusterSharding(context.system).start(
      typeName = "Counter1",
      entryProps = None,
      idExtractor = Counter1.idExtractor,
      shardResolver = Counter1.shardResolver)
    ClusterSharding(context.system).shardRegion("Counter1")
  }

  lazy val counter2Region = {
    ClusterSharding(context.system).start(
      typeName = "Counter2",
      entryProps = None,
      idExtractor = Counter2.idExtractor,
      shardResolver = Counter2.shardResolver)
    ClusterSharding(context.system).shardRegion("Counter2")
  }

  val tickTask = context.system.scheduler.schedule(3.seconds, 3.seconds, self, CounterQuery.Tick)

  def receive = {
    case CounterQuery.Tick =>
      check(counter1Region)
      check(counter2Region)
    case CounterQuery.AskShard(shardName) =>
      shardName match {
        case "Counter1" =>
          check(counter1Region)
        case "Counter2" =>
          check(counter2Region)
      }
  }

  def check(region: ActorRef) {
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
    query ! CounterQuery.AskShard("Counter1")
    query ! CounterQuery.AskShard("Counter2")
  }
}