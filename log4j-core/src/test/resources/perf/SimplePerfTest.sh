#!/bin/sh
#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
if [ $# -eq 0 ]; then
    echo Usage: $0 api-version core-version
    exit 1
fi

DIR=$HOME
NOW=$(date +%Y%m%d-%H%M%S)

GC_OPTIONS="-XX:+UnlockDiagnosticVMOptions -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps"
GC_OPTIONS="${GC_OPTIONS} -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationConcurrentTime -XX:+PrintGCApplicationStoppedTime"
GC_OPTIONS="${GC_OPTIONS} -XX:+PrintGCCause -Xloggc:$DIR/gc-$NOW.log"
#GC_OPTIONS="${GC_OPTIONS} -XX:+PrintSafepointStatistics -XX:+LogVMOutput -XX:LogFile=$DIR/safepoint$NOW.log"
GC_OPTIONS=

# Needs -XX:+UnlockDiagnosticVMOptions (as first VM arg)
#VM_OPTIONS="-XX:+PrintCompilation -XX:+PrintInlining"

LOG4J_OPTIONS="-Dlog4j.configurationFile=perf-CountingNoOpAppender.xml"
LOG4J_OPTIONS="${LOG4J_OPTIONS} -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector"
LOG4J_OPTIONS="${LOG4J_OPTIONS} -Dlog4j2.enable.threadlocals=true"
LOG4J_OPTIONS="${LOG4J_OPTIONS} -DAsyncLogger.WaitStrategy=Block"
LOG4J_OPTIONS="${LOG4J_OPTIONS} -Dlog4j.format.msg.async=true"
export LOG4J_OPTIONS

CP="log4j-api-${1}.jar:log4j-core-${2}.jar:disruptor-3.3.4.jar"
CP="${CP}:log4j-core-2.6-SNAPSHOT-tests.jar"
#CP="${CP}:${HOME}/Documents/log4j/log4j-core/target/test-classes"
export CP

export MEM_OPTIONS="-Xms128m -Xmx128m"
export MAIN="org.apache.logging.log4j.core.async.perftest.SimplePerfTest"

#PERF_OPTIONS="-e cycles,instructions,cache-references,cache-misses,branches,branch-misses,L1-dcache-loads,L1-dcache-load-misses,dTLB-loads,dTLB-load-misses"
PERF_OPTIONS=
#perf stat ${PERF_OPTIONS} java ${MEM_OPTIONS} ${GC_OPTIONS} ${VM_OPTIONS} ${LOG4J_OPTIONS} -cp "${CP}" ${MAIN}

java ${MEM_OPTIONS} ${GC_OPTIONS} ${VM_OPTIONS} ${LOG4J_OPTIONS} -cp "${CP}" ${MAIN}
