#!/bin/bash

if [ -z "${JAVA_HOME}" ]
then
    echo "Please set environment JAVA_HOME";
    exit 1
fi

arg="$1"
usage() {
    echo "Usage: `basename $0` [tran|conn|fron]"
    exit 1
}

dir_conf=../conf

cluster_system="ClusterExample"
cluster_seed="127.0.0.1:2551"
cluster_hostname="127.0.0.1"  #"$(/sbin/ifconfig | grep -A 1 'em2' | tail -1 | cut -d ':' -f 2 | cut -d ' ' -f 1)"
transport_hostname="127.0.0.1" #"$(/sbin/ifconfig | grep -A 1 'em1' | tail -1 | cut -d ':' -f 2 | cut -d ' ' -f 1)"

cluster_port=0
case $arg in
    seed1*)  cluster_module="seed1"; akka_args=""; inloop_class_pgm="inloop.example.cluster.ClusterMonitor";;
    seed2*)  cluster_module="seed2"; akka_args=""; inloop_class_pgm="inloop.example.cluster.ClusterMonitor";;
    query*)  cluster_module="query"; akka_args=""; inloop_class_pgm="inloop.example.cluster.CounterQuery";;
    stat1*)  cluster_module="counter1"; akka_args="" inloop_class_pgm="inloop.example.cluster.Counter1";;
    stat2*)  cluster_module="counter2"; akka_args=""; inloop_class_pgm="inloop.example.cluster.Counter2";;
    *) usage
esac

inloop_id_pgm=${cluster_module}
inloop_lock_file=.lock_${cluster_module}
inloop_conf=../conf/${cluster_module}.conf
logback_conf=../conf/logback_${cluster_module}.xml

export JAVA=${JAVA_HOME}/bin/java
export FLAGS="-server -Dfile.encoding=UTF8 -XX:+UseNUMA -XX:+UseCondCardMark -XX:-UseBiasedLocking"
export HEAP="-Xms256M -Xmx10240M -Xss1M"
export GC="-XX:+UseParallelGC"

NOW=$(date +"%Y-%m-%dT%H%M%S")

cp="";
for f in ../lib/*.jar;
do cp=${f}":"${cp};
done;
cp=${dir_conf}":"${cp};


$JAVA $FLAGS $HEAP $GC -Dconfig.file=${inloop_conf} -Dlogback.configurationFile=${logback_conf} ${akka_args} -cp ${cp} ${inloop_class_pgm} > ../logs/${cluster_module}_${NOW}_rt.log &
inloop_pid=$!
echo $inloop_pid > ./${inloop_lock_file}
echo "Started ${inloop_id_pgm}, pid is $inloop_pid"

