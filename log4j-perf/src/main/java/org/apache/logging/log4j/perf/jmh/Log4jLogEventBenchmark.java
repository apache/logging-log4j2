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

import java.io.Serializable;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Thread)
public class Log4jLogEventBenchmark {
    private static Message MESSAGE;
    private static Throwable ERROR;

    @Setup
    public void setup() {
        MESSAGE = new SimpleMessage("Test message");
        ERROR = new Exception("test");
    }

    @Benchmark
    public void testBaseline(final Blackhole bh) {
    }

    @Benchmark
    public LogEvent createLogEventWithoutException() {
        return new Log4jLogEvent("a.b.c", null, "a.b.c", Level.INFO, MESSAGE, null, null);
    }

    @Benchmark
    public LogEvent createLogEventWithoutExceptionUsingBuilder() {
        return Log4jLogEvent.newBuilder().setLoggerName("a.b.c").setLoggerFqcn("a.b.c").setLevel(Level.INFO)
                .setMessage(MESSAGE).build();
    }

    @Benchmark
    public LogEvent createLogEventWithExceptionUsingBuilder() {
        return Log4jLogEvent.newBuilder().setLoggerName("a.b.c").setLoggerFqcn("a.b.c").setLevel(Level.INFO)
                .setMessage(MESSAGE).setThrown(ERROR).build();
    }

    @Benchmark
    public StackTraceElement getSourceLocationOfLogEvent() {
        final LogEvent event = Log4jLogEvent.newBuilder().setLoggerName(this.getClass().getName())
                .setLoggerFqcn(this.getClass().getName()).setLevel(Level.INFO).setMessage(MESSAGE).build();
        event.setIncludeLocation(true);
        return event.getSource();
    }

    @Benchmark
    public Serializable createSerializableLogEventProxyWithoutException() {
        final Log4jLogEvent event = new Log4jLogEvent("a.b.c", null, "a.b.c", Level.INFO, MESSAGE, null, null);
        return Log4jLogEvent.serialize(event, false);
    }

    @Benchmark
    public Serializable createSerializableLogEventProxyWithException(final Blackhole bh) {
        final Log4jLogEvent event = new Log4jLogEvent("a.b.c", null, "a.b.c", Level.INFO, MESSAGE, null, ERROR);
        return Log4jLogEvent.serialize(event, false);
    }

    // ============================== HOW TO RUN THIS TEST: ====================================
    //
    // In sampling mode (latency test):
    // java -jar log4j-perf/target/benchmarks.jar ".*Log4jLogEventBenchmark.*" -i 5 -f 1 -wi 5 -bm sample -tu ns
    //
    // Throughput test:
    // java -jar benchmarks.jar ".*Log4jLogEventBenchmark.*" -i 5 -f 1 -wi 5 -bm Throughput -tu ms
    //
    // Usage help:
    // java -jar log4j-perf/target/benchmarks.jar -help
    //
}
