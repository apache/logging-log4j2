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
package org.apache.logging.log4j.layout.template.json;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.openjdk.jmh.annotations.Benchmark;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Benchmark suite for various JSON layouts.
 * <p>
 * You can run this test as follows:
 * <pre>
 * java \
 *     -jar log4j-perf/target/benchmarks.jar \
 *     -f 2 \
 *     -wi 3 -w 20s \
 *     -i 5 -r 30s \
 *     -prof gc \
 *     -rf json -rff log4j-perf/target/JsonTemplateLayoutBenchmarkResult.json \
 *     ".*JsonTemplateLayoutBenchmark.*"
 * </pre>
 */
public class JsonTemplateLayoutBenchmark {

    @Benchmark
    public static int fullJsonTemplateLayout4JsonLayout(
            final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(
                state,
                state.getJsonTemplateLayout4JsonLayout(),
                state.getFullLogEvents());
    }

    @Benchmark
    public static int liteJsonTemplateLayout4JsonLayout(
            final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(
                state,
                state.getJsonTemplateLayout4JsonLayout(),
                state.getLiteLogEvents());
    }

    @Benchmark
    public static int fullJsonTemplateLayout4EcsLayout(
            final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(
                state,
                state.getJsonTemplateLayout4EcsLayout(),
                state.getFullLogEvents());
    }

    @Benchmark
    public static int liteJsonTemplateLayout4EcsLayout(
            final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(
                state,
                state.getJsonTemplateLayout4EcsLayout(),
                state.getLiteLogEvents());
    }

    @Benchmark
    public static int fullJsonTemplateLayout4GelfLayout(
            final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(
                state,
                state.getJsonTemplateLayout4GelfLayout(),
                state.getFullLogEvents());
    }

    @Benchmark
    public static int liteJsonTemplateLayout4GelfLayout(
            final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(
                state,
                state.getJsonTemplateLayout4GelfLayout(),
                state.getLiteLogEvents());
    }

    @Benchmark
    public static int fullDefaultJsonLayout(
            final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(
                state,
                state.getDefaultJsonLayout(),
                state.getFullLogEvents());
    }

    @Benchmark
    public static int liteDefaultJsonLayout(
            final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(
                state,
                state.getDefaultJsonLayout(),
                state.getLiteLogEvents());
    }

    @Benchmark
    public static int fullCustomJsonLayout(
            final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(
                state,
                state.getCustomJsonLayout(),
                state.getFullLogEvents());
    }

    @Benchmark
    public static int liteCustomJsonLayout(
            final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(
                state,
                state.getCustomJsonLayout(),
                state.getLiteLogEvents());
    }

    @Benchmark
    public static int fullEcsLayout(
            final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(
                state,
                state.getEcsLayout(),
                state.getFullLogEvents());
    }

    @Benchmark
    public static int liteEcsLayout(
            final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(
                state,
                state.getEcsLayout(),
                state.getLiteLogEvents());
    }

    @Benchmark
    public static int fullGelfLayout(
            final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(
                state,
                state.getGelfLayout(),
                state.getFullLogEvents());
    }

    @Benchmark
    public static int liteGelfLayout(
            final JsonTemplateLayoutBenchmarkState state) {
        return benchmark(
                state,
                state.getGelfLayout(),
                state.getLiteLogEvents());
    }

    private static int benchmark(
            final JsonTemplateLayoutBenchmarkState state,
            final Layout<String> layout,
            final List<LogEvent> logEvents) {
        final int logEventIndex = state.nextLogEventIndex();
        final LogEvent logEvent = logEvents.get(logEventIndex);
        return benchmark(layout, logEvent, state.getByteBufferDestination());
    }

    private static int benchmark(
            final Layout<String> layout,
            final LogEvent logEvent,
            final ByteBufferDestination destination) {
        final ByteBuffer byteBuffer = destination.getByteBuffer();
        layout.encode(logEvent, destination);
        return byteBuffer.position();
    }

    public static void main(String[] args) throws IOException {
//        System.out.format("Ready?");
//        System.in.read();
        JsonTemplateLayoutBenchmarkState state = new JsonTemplateLayoutBenchmarkState();
        int retryCount = 500_000_000;
        measureEcs(state, retryCount);
        measureJtl(state, retryCount);
//        while (true) {
//            measureEcs(state, retryCount);
//        }
//        measureEcs(state, retryCount);
//        measureJtl(state, retryCount);
    }

    private static void measureJtl(JsonTemplateLayoutBenchmarkState state, int retryCount) {
        long startInstantNanos = System.nanoTime();
        for (int i = 0; i < retryCount; i++) {
            liteJsonTemplateLayout4EcsLayout(state);
        }
        System.out.format("%.3fs%n", (System.nanoTime() - startInstantNanos) / 1e9);
    }

    private static void measureEcs(JsonTemplateLayoutBenchmarkState state, int retryCount) {
        long startInstantNanos = System.nanoTime();
        for (int i = 0; i < retryCount; i++) {
            liteEcsLayout(state);
        }
        System.out.format("%.3fs%n", (System.nanoTime() - startInstantNanos) / 1e9);
    }

}
