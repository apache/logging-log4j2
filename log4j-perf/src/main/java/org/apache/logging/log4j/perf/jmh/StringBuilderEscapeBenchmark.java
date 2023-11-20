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
package org.apache.logging.log4j.perf.jmh;

import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.util.StringBuilders;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * This benchmark tests encoding implementations.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// java -jar log4j-perf/target/benchmarks.jar ".*StringBuilderEscapeBenchmark.*" -f 1 -wi 5 -i 10
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Benchmark)
public class StringBuilderEscapeBenchmark {

    private static final String EVERY_CHARACTER_MUST_BE_ESCAPED_JSON = repeat("\t\"", 1024);
    private static final String EVERY_CHARACTER_MUST_BE_ESCAPED_XML = repeat("<\"&>", 512);

    @State(Scope.Thread)
    public static class ThreadState {
        StringBuilder buffer = new StringBuilder(1024 * 4);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int escapeJsonLargeString(final ThreadState state) {
        state.buffer.setLength(0);
        state.buffer.append(EVERY_CHARACTER_MUST_BE_ESCAPED_JSON);
        StringBuilders.escapeJson(state.buffer, 0);
        return state.buffer.length();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int escapeXmlLargeString(final ThreadState state) {
        state.buffer.setLength(0);
        state.buffer.append(EVERY_CHARACTER_MUST_BE_ESCAPED_XML);
        StringBuilders.escapeXml(state.buffer, 0);
        return state.buffer.length();
    }

    private static String repeat(final String str, final int times) {
        final StringBuilder sb = new StringBuilder(str.length() * times);
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}
