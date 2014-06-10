#!/bin/bash

arg="$1"
usage() {
    echo "Usage: `basename $0` [seed1|seed2|query|ctr1|ctr2]"
    exit 1
}

case $arg in
    seed1*)  cluster_module="seed1";;
    seed2*)  cluster_module="seed2";;
    query*)  cluster_module="query";;
    ctr1*)   cluster_module="counter1";;
    ctr2*)   cluster_module="counter2";;
    *) usage
esac

inloop_id_pgm=${cluster_module}
inloop_lock_file=.lock_${cluster_module}

if [ -f ${inloop_lock_file} ]
then
    kill `cat ${inloop_lock_file}`
    if [ $? -eq 0 ]
    then
        echo "Stopped `cat ${inloop_lock_file}`"
        rm ${inloop_lock_file}
        exit 0
    fi
fi
