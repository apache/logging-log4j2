/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.message;

import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.message.ParameterFormatter.MessagePatternAnalysis;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * This benchmark is not in the perf.jmh package because it tests the package-protected ParameterFormatter class.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*ParameterFormatterBench.*" -f 1 -wi 5 -i 10
//
// multiple threads (for example, 4 threads):
// java -jar log4j-perf/target/benchmarks.jar ".*ParameterFormatterBench.*" -f 1 -wi 5 -i 10 -t 4 -si true
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Benchmark)
public class ParameterFormatterBenchmark {

    private static final Object[] ARGS = {
        "arg1", "arg2", "arg3", "arg4", "arg5", "arg6", "arg7", "arg8", "arg9", "arg10"
    };

    @State(Scope.Thread)
    public static class ThreadState {

        private final MessagePatternAnalysis analysis = new MessagePatternAnalysis();

        private final StringBuilder buffer = new StringBuilder(2048);

        public ThreadState() {
            analysis.placeholderCharIndices = new int[10];
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int latency3Params(final ThreadState state) {
        return latencyParams(state, "p1={}, p2={}, p3={}");
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int latency5Params(final ThreadState state) {
        return latencyParams(state, "p1={}, p2={}, p3={}, p4={}, p5={}");
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int latency7Params(final ThreadState state) {
        return latencyParams(state, "p1={}, p2={}, p3={}, p4={}, p5={}, p6={}, p7={}");
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int latency9Params(final ThreadState state) {
        return latencyParams(state, "p1={}, p2={}, p3={}, p4={}, p5={}, p6={}, p7={}, p8={}, p9={}");
    }

    private static int latencyParams(final ThreadState state, final String pattern) {
        state.buffer.setLength(0);
        ParameterFormatter.analyzePattern(pattern, -1, state.analysis);
        ParameterFormatter.formatMessage(state.buffer, pattern, ARGS, state.analysis.placeholderCount, state.analysis);
        return state.buffer.length();
    }
}
