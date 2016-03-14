/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.perf.jmh;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
/*
import java.lang.StackWalker;
import java.lang.StackWalker.StackFrame;
import java.util.List;
import java.util.stream.Collectors; */
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Thread)
public class StackTraceBenchmark {
    private ThreadMXBean threadBean;

    @Setup
    public void setup() {
        threadBean = ManagementFactory.getThreadMXBean();
    }

    @Benchmark
    public void testBaseline(final Blackhole bh) {
    }

    @Benchmark
    public StackTraceElement[] getStackTraceFromException() {
        return new Throwable().getStackTrace();
    }

    @Benchmark
    public StackTraceElement[] getStackTraceFromThread() {
        return Thread.currentThread().getStackTrace();
    }

    @Benchmark
    public StackTraceElement[] getStackTraceFromThrowableInfo() {
        long id = Thread.currentThread().getId();
        return threadBean.getThreadInfo(new long[] {id}, false, false)[0].getStackTrace();
    }

    @Benchmark
    public StackTraceElement[] getSubsetStackTraceFromThrowableInfo() {
        long id = Thread.currentThread().getId();
        return threadBean.getThreadInfo(id, 15).getStackTrace();
    }
    /*
    @Benchmark
    public List<StackFrame> getStackFrames() {
        return  StackWalker.getInstance().walk(s ->
                s.limit(15).collect(Collectors.toList()));
    } */

    /*
    @Benchmark
    public String getMethodFromThrowable() {
        return calcLocation("getMethodFromThrowable");

    }

    private String calcLocation(String methodName) {
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        StackTraceElement last = null;
        for (int i = stackTrace.length - 1; i > 0; i--) {
            if (methodName.equals(stackTrace[i].getMethodName())) {
                return last.getMethodName();
            }
            last = stackTrace[i];
        }
        return null;
    }

    @Benchmark
    public String getMethodFromStackWalker() {
        StackWalker walker = StackWalker.getInstance();
        walker.walk()
    } */


    // ============================== HOW TO RUN THIS TEST: ====================================
    //
    // In sampling mode (latency test):
    // java -jar log4j-perf/target/benchmarks.jar ".*StackTraceBenchmark.*" -i 5 -f 1 -wi 5 -bm sample -tu ns
    //
    // Throughput test:
    // java -jar benchmarks.jar ".*StackTraceBenchmark.*" -i 5 -f 1 -wi 5 -bm Throughput -tu ms
    //
    // Usage help:
    // java -jar log4j-perf/target/benchmarks.jar -help
    //
}
