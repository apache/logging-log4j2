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

NOW=$(date +%Y%m%d-%H%M%S)

GC_OPTIONS="-XX:+UnlockDiagnosticVMOptions -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationConcurrentTime -XX:+PrintGCApplicationStoppedTime"
#GC_OPTIONS="${GC_OPTIONS} -XX:GuaranteedSafepointInterval=500000"
GC_OPTIONS="${GC_OPTIONS} -XX:+PrintGCCause  -XX:+PrintSafepointStatistics -XX:+LogVMOutput -XX:LogFile=safepoint$NOW.log"
COMPILE_OPTIONS="-XX:CompileCommand=dontinline,org.apache.logging.log4j.core.async.perftest.NoOpIdleStrategy::idle"

#VM_OPTIONS="-XX:+UnlockDiagnosticVMOptions -XX:+PrintCompilation -XX:+PrintInlining"

LOG4J_OPTIONS=
#LOG4J_OPTIONS="-Dlog4j.configurationFile=perf-CountingNoOpAppender.xml"
#LOG4J_OPTIONS="-Dlog4j.configurationFile=perf3PlainNoLoc.xml"
#LOG4J_OPTIONS="-Dlog4j.configurationFile=perf7MixedNoLoc.xml"
LOG4J_OPTIONS="-Dlog4j.configurationFile=perf5AsyncApndNoLoc.xml"

#LOG4J_OPTIONS="-Dlog4j.configuration=perf-log4j12.xml"
#LOG4J_OPTIONS="-Dlog4j.configuration=perf-log4j12-async.xml"
#LOG4J_OPTIONS="-Dlogback.configurationFile=perf-logback-async.xml"
#LOG4J_OPTIONS="-Dlogback.configurationFile=perf-logback.xml"

#LOG4J_OPTIONS="${LOG4J_OPTIONS} -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector"
LOG4J_OPTIONS="${LOG4J_OPTIONS} -Dlog4j2.enable.threadlocals=true"
LOG4J_OPTIONS="${LOG4J_OPTIONS} -Dlog4j2.enable.direct.encoders=true"
LOG4J_OPTIONS="${LOG4J_OPTIONS} -DAsyncLogger.WaitStrategy=busySpin"
LOG4J_OPTIONS="${LOG4J_OPTIONS} -DAsyncLoggerConfig.WaitStrategy=busySpin"
#LOG4J_OPTIONS="${LOG4J_OPTIONS} -Dlog4j.format.msg.async=true"
export LOG4J_OPTIONS

CP=".:HdrHistogram-2.1.8.jar:disruptor-3.3.4.jar:log4j-1.2.17.jar:slf4j-api-1.7.21.jar:slf4j-ext-1.7.21.jar:logback-core-1.1.7.jar:logback-classic-1.1.7.jar:log4j-api-2.6-SNAPSHOT.jar:log4j-core-2.6-SNAPSHOT.jar:log4j-core-2.6-SNAPSHOT-tests.jar"

RUNNER=RunLog4j2
RESULTDIR=ApndLog4j2
java -Xms1G -Xmx1G $GC_OPTIONS $COMPILE_OPTIONS $VM_OPTIONS $JFR_OPTIONS $LOG4J_OPTIONS -cp $CP org.apache.logging.log4j.core.async.perftest.ResponseTimeTest 1 10000 $RUNNER
java -Xms1G -Xmx1G $GC_OPTIONS $COMPILE_OPTIONS $VM_OPTIONS $JFR_OPTIONS $LOG4J_OPTIONS -cp $CP org.apache.logging.log4j.core.async.perftest.ResponseTimeTest 2 5000 $RUNNER
java -Xms1G -Xmx1G $GC_OPTIONS $COMPILE_OPTIONS $VM_OPTIONS $JFR_OPTIONS $LOG4J_OPTIONS -cp $CP org.apache.logging.log4j.core.async.perftest.ResponseTimeTest 4 2500 $RUNNER
mkdir async$RESULTDIR-10k
mv *k? async$RESULTDIR-10k
mv nohup.out async$RESULTDIR-10k
mv safepoint$NOW.log async$RESULTDIR-10k


