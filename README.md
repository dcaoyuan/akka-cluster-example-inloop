akka-cluster-example-inloop
===========================

Simple akka cluster example.

## To run: ##

1. Install cassandra and start it.
1. sbt clean compile xitrum-package
1. cd target/xitrum/bin
1. ./start.sh seed1
1. ./start.sh seed2
1. ./start.sh stat1
1. ./start.sh stat2
1. ./start.sh query
1. ./start_driver.sh
1. cd ../logs
1. tail -f driver_rt.log


## Note ##
To got multiple sharding actors run on separated sub-set of nodes, and can be accessed from other nodes in the cluster, for example:

* Counter1 runs on node1, node2, node3
* Counter2 runs on node7, node8, node9
* Query want to query both Counter1 sharding and Counter2 sharding.

You have to:

1. All those sharding nodes should contain at lease one same role in "actor.cluster.roles"
1. All sharding nodes should have the role in "actor.cluster.roles" which is the same as its sharding role
1. Query can have one or none whatever role in "akka.contrib.cluster.sharding.role"
1. ClusterSharding(system).start() with entryProps = Some(Props[Counter1]) on node1, node2, node3
1. ClusterSharding(system).start() with entryProps = Some(Props[Counter2]) on node7, node8, node9
1. ClusterSharding(system).start() with entryProps = None on node of Query for shardings of Counter1 and Counter2.


Note:

1. cluster singleton proxy or cluster sharding proxy does not need to contain the corresponding role. 
1. Start sharding or its proxy will try to create sharding coordinate singleton on the oldest node, so the oldest node has to contain those (singleton, sharding) corresponding roles and start these sharding/singleton entry or proxy. 
1. If the sharding coordinate is not be created/located in cluster yet, the sharding proxy in other node could not identify the coordinate singleton, which means, if you want to a sharding proxy to work properly and which has no corresponding role contained, you have to wait for the coordinate singleton is ready in cluster.

* The sharding's singleton coordinator will be created and located at the oldest node.
* Anyway, to free the nodes starting order, the first started node (oldest) should start all sharding sevices (or proxy) and singleton manager (or proxy) and thus has to contain all those corresponding roles,

In Source code:

1. When try to start sharding

``` scala
HasNecessaryClusterRole: Boolean = Role.forall(cluster.selfRoles.contains)
```

Means the sharding.role is empty or the role is contained in cluster.roles

If true, will try to start/check coordingtorSingletonManager, and
start this node as part of sharding if entryProp is not empty 

2. When shardRegion is started

``` scala
  def matchingRole(member: Member): Boolean = role match {
    case None    ⇒ true
    case Some(r) ⇒ member.hasRole(r)
  }
```

means my role is empty or is contained in member's cluster.roles

Only if true, this node can get updated members list for select correct coordinateSingleton

So, a node could be a sharding proxy, should meet atleast 2:
its sharding.role should be empty or the member which holds the coordinateSingleton should contain it.
