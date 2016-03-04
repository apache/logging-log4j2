#!/bin/sh

if [ $# -ne 1 ]; then
    echo Usage: $0 version
    exit 1
fi

export MEM_OPTIONS="-Xms128m -Xmx128m"
export GC_OPTIONS="-verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationConcurrentTime -XX:+PrintGCApplicationStoppedTime"

LOG4J_OPTIONS="-Dlog4j.configurationFile=perf-CountingNoOpAppender.xml"
LOG4J_OPTIONS="${LOG4J_OPTIONS} -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector"
LOG4J_OPTIONS="${LOG4J_OPTIONS} -Dlog4j2.enable.threadlocals=true"
export LOG4J_OPTIONS

CP="log4j-api-${1}.jar:log4j-core-${1}.jar:disruptor-3.3.4.jar"
CP="${CP}:${HOME}/Documents/log4j/log4j-core/target/test-classes"
export CP

export MAIN="org.apache.logging.log4j.core.async.perftest.SimplePerfTest"

java ${MEM_OPTIONS} ${GC_OPTIONS} ${LOG4J_OPTIONS} -cp "${CP}" ${MAIN}
