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

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.util.Unbox;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * Benchmarks the Unbox class.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*Unbox.*" -f 1 -wi 10 -i 15
//
// Default config with multiple threads (for example, 4 threads):
// java -Dlog4j2.enable.threadlocals=true -jar log4j-perf/target/benchmarks.jar ".*Unbox.*" -f 1 -wi 10 -i 15 -t 4
//
// Web-app safe config with multiple threads (for example, 4 threads):
// java -Dlog4j2.enable.threadlocals=false -jar log4j-perf/target/benchmarks.jar ".*Unbox.*" -f 1 -wi 10 -i 15 -t 4
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Benchmark)
public class UnboxBenchmark {

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public StringBuilder boxBoolean() {
        return Unbox.box(true);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public StringBuilder boxInt() {
        return Unbox.box(12345);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public StringBuilder boxLong() {
        return Unbox.box(12345L);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public StringBuilder boxDouble() {
        return Unbox.box(12345.12345);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public StringBuilder boxFloat() {
        return Unbox.box(12345.12345f);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public StringBuilder boxShort() {
        return Unbox.box((short) 123);
    }
}
