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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * Tests how expensive constructing a varargs array is.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*Varargs.*" -f 1 -wi 5 -i 10
//
// multiple threads (for example, 4 threads):
// java -jar log4j-perf/target/benchmarks.jar ".*Varargs.*" -f 1 -wi 5 -i 10 -t 4 -si true
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Benchmark)
public class VarargsBenchmark {

    public static void main(final String[] args) {
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void baseline() {
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long varargParams() {
        return varargMethod("example {} {} {} {} {} {} {} {} {} {}", "one", "two", "three", "four", "five",
                "six", "seven", "eight", "nine", "ten");
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long individualParams() {
        return individualArgMethod("example {} {} {} {} {} {} {} {} {} {}", "one", "two", "three", "four", "five",
                "six", "seven", "eight", "nine", "ten");
    }

    private long varargMethod(final String string, final String... p) {
        return string.length() + p[0].length() + p[1].length() + p[2].length() + p[3].length() + p[4].length()
                + p[5].length() + p[6].length() + p[7].length() + p[8].length() + p[9].length();
    }

    private long individualArgMethod(final String string, final String p1, final String p2, final String p3, final String p4, final String p5, final String p6,
            final String p7, final String p8, final String p9, final String p10) {
        return string.length() + p1.length() + p2.length() + p3.length() + p4.length() + p5.length() + p6.length()
                + p7.length() + p8.length() + p9.length() + p10.length();
    }
}
