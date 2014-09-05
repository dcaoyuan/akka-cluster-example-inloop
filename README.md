akka-cluster-example-inloop
===========================

Simple akka cluster example.

To run:

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