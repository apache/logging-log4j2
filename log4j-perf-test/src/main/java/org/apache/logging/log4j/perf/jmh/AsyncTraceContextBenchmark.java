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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.async.RingBufferLogEvent;
import org.apache.logging.log4j.core.async.RingBufferLogEventTranslator;
import org.apache.logging.log4j.core.util.ClockFactory;
import org.apache.logging.log4j.core.util.DummyNanoClock;
import org.apache.logging.log4j.message.SimpleMessage;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class AsyncTraceContextBenchmark {

    private SimpleMessage message;

    @Setup
    public void setup() {
        message = new SimpleMessage("Test performance message");
    }

    /**
     * Reusable thread-local state to match how Disruptor events and translators
     * are held per-thread in production.
     */
    @State(Scope.Thread)
    public static class ThreadState {
        final RingBufferLogEvent event = new RingBufferLogEvent();
        final RingBufferLogEventTranslator translator = new RingBufferLogEventTranslator();
    }

    @Benchmark
    public void baseline(final ThreadState state) {
        state.translator.setBasicValues(
                null,
                "Logger",
                null,
                "FQCN",
                Level.INFO,
                message,
                null,
                null,
                null,
                ClockFactory.getClock(),
                new DummyNanoClock());
        state.translator.translateTo(state.event, 0);
    }

    @Benchmark
    public void threadContextTracing(final ThreadState state) {
        ThreadContext.put("traceId", "4bf92f3577b34da6a3ce929d0e0e4736");
        ThreadContext.put("spanId", "00f067aa0ba902b7");
        ThreadContext.put("traceFlags", "01");

        try {
            state.translator.setBasicValues(
                    null,
                    "Logger",
                    null,
                    "FQCN",
                    Level.INFO,
                    message,
                    null,
                    ThreadContext.getImmutableStack(),
                    null,
                    ClockFactory.getClock(),
                    new DummyNanoClock());
            state.translator.translateTo(state.event, 0);
        } finally {
            ThreadContext.clearMap();
        }
    }

    @Benchmark
    public void nativeTracing(final ThreadState state) {
        state.translator.setBasicValues(
                null,
                "Logger",
                null,
                "FQCN",
                Level.INFO,
                message,
                null,
                null,
                null,
                ClockFactory.getClock(),
                new DummyNanoClock(),
                "4bf92f3577b34da6a3ce929d0e0e4736",
                "00f067aa0ba902b7",
                "01");
        state.translator.translateTo(state.event, 0);
    }

    public static void main(final String[] args) throws Exception {
        new Runner(new OptionsBuilder()
                        .include(AsyncTraceContextBenchmark.class.getSimpleName())
                        .build())
                .run();
    }
}
