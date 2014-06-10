package inloop.example.cluster

import akka.actor.{ Props, ActorLogging, Actor, ActorSystem }
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._

class ClusterMonitor extends Actor with ActorLogging {
  def receive = {
    case state: CurrentClusterState           => log.info("Current state: {}", state)
    case MemberUp(member)                     => log.info("Member is up: {}", member)
    case MemberRemoved(member, previousState) => log.info("Member removed: {}", member)
    case MemberExited(member)                 => log.info("Member exited: {}", member)
    case UnreachableMember(member)            => log.info("Member unreachable: {}", member)
    case LeaderChanged(member)                => log.info("Leader changed: {}", member)
    case RoleLeaderChanged(role, member)      => log.info("Role {} leader changed: {}", role, member)
    case _: ClusterMetricsChanged             => // ignore
    case e: ClusterDomainEvent                => //log.info("???: {}", e)
  }
}

object ClusterMonitor {

  val system = ActorSystem("ClusterExample")

  def main(args: Array[String]) {
    val cluster = Cluster(system)
    val monitor = system.actorOf(Props[ClusterMonitor])
    cluster.subscribe(monitor, classOf[ClusterDomainEvent])
  }

}