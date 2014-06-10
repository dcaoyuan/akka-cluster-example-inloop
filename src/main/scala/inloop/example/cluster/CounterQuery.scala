package inloop.example.cluster

import akka.pattern.ask
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.cluster.Cluster
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success

class CounterQuery extends Actor with ActorLogging {

  implicit val ec = context.dispatcher
  val counter1Region = Counter1.region
  //val counter2Region = Counter2.region
  val tickTask = context.system.scheduler.schedule(3.seconds, 3.seconds, self, CounterQuery.Tick)

  def receive = {
    case CounterQuery.Tick => check
    case CounterQuery.AskShard(shardName) =>
      check
  }

  def check {
    counter1Region.ask(Get(100))(2.seconds).onComplete {
      case Success(x) => log.info("Got: {}" + x)
      case Failure(x) => log.error(x, "Error: {}", x.getMessage)
    }

    counter1Region ! EntryEnvelope(99, Increment)
    counter1Region.ask(Get(99))(2.seconds).onComplete {
      case Success(x) => log.info("Got: {}" + x)
      case Failure(x) => log.error(x, "Error: {}", x.getMessage)
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
  }
}