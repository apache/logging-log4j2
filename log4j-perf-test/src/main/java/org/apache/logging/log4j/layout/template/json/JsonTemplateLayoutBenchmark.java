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
package org.apache.logging.log4j.layout.template.json;

import java.nio.ByteBuffer;
import java.util.List;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.openjdk.jmh.annotations.Benchmark;

/**
 * Benchmark suite for various JSON layouts.
 * <p>
 * You can run this test as follows:
 * <pre>{@code
 * java \
 *     -jar log4j-perf/target/benchmarks.jar \
 *     -f 2 \
 *     -wi 3 -w 20s \
 *     -i 5 -r 30s \
 *     -prof gc \
 *     -prof perfnorm \
 *     -prof "async:libPath=/path/to/libasyncProfiler.so;output=flamegraph" \
 *     -rf json -rff log4j-perf/target/JsonTemplateLayoutBenchmarkResult.json \
 *     ".*JsonTemplateLayoutBenchmark.*"
 * }</pre>
 * </p>
 */
public class JsonTemplateLayoutBenchmark {

    @Benchmark
    public static int fullJtl4JsonLayout(final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(state, state.getJtl4JsonLayout(), state.getFullLogEvents());
    }

    @Benchmark
    public static int liteJtl4JsonLayout(final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(state, state.getJtl4JsonLayout(), state.getLiteLogEvents());
    }

    @Benchmark
    public static int fullJtl4EcsLayout(final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(state, state.getJtl4EcsLayout(), state.getFullLogEvents());
    }

    @Benchmark
    public static int liteJtl4EcsLayout(final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(state, state.getJtl4EcsLayout(), state.getLiteLogEvents());
    }

    @Benchmark
    public static int fullJtl4GelfLayout(final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(state, state.getJtl4GelfLayout(), state.getFullLogEvents());
    }

    @Benchmark
    public static int liteJtl4GelfLayout(final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(state, state.getJtl4GelfLayout(), state.getLiteLogEvents());
    }

    @Benchmark
    public static int fullDefaultJsonLayout(final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(state, state.getDefaultJsonLayout(), state.getFullLogEvents());
    }

    @Benchmark
    public static int liteDefaultJsonLayout(final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(state, state.getDefaultJsonLayout(), state.getLiteLogEvents());
    }

    @Benchmark
    public static int fullCustomJsonLayout(final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(state, state.getCustomJsonLayout(), state.getFullLogEvents());
    }

    @Benchmark
    public static int liteCustomJsonLayout(final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(state, state.getCustomJsonLayout(), state.getLiteLogEvents());
    }

    @Benchmark
    public static int fullEcsLayout(final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(state, state.getEcsLayout(), state.getFullLogEvents());
    }

    @Benchmark
    public static int liteEcsLayout(final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(state, state.getEcsLayout(), state.getLiteLogEvents());
    }

    @Benchmark
    public static int fullGelfLayout(final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(state, state.getGelfLayout(), state.getFullLogEvents());
    }

    @Benchmark
    public static int liteGelfLayout(final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(state, state.getGelfLayout(), state.getLiteLogEvents());
    }

    private static int benchmark(
            final JsonTemplateLayoutBenchmarkState state, final Layout<?> layout, final List<LogEvent> logEvents) {
        final int logEventIndex = state.nextLogEventIndex();
        final LogEvent logEvent = logEvents.get(logEventIndex);
        return benchmark(layout, logEvent, state.getByteBufferDestination());
    }

    private static int benchmark(
            final Layout<?> layout, final LogEvent logEvent, final ByteBufferDestination destination) {
        final ByteBuffer byteBuffer = destination.getByteBuffer();
        layout.encode(logEvent, destination);
        return byteBuffer.position();
    }
}
