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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.logic.BlackHole;

import java.io.Serializable;

@State(Scope.Thread)
public class Log4jLogEventBenchmark {
    private static Message MESSAGE;
    private static Throwable ERROR;

    @Setup
    public void setup() {
        MESSAGE = new SimpleMessage("Test message");
        ERROR = new Exception("test");
    }

    @GenerateMicroBenchmark
    public void testBaseline(final BlackHole bh) {
    }

    @GenerateMicroBenchmark
    public void testNoException(final BlackHole bh) {
        final Throwable t = null;
        final Log4jLogEvent event = new Log4jLogEvent("a.b.c", null, "a.b.c", Level.INFO, MESSAGE, t);
        bh.consume(event);
    }

    @GenerateMicroBenchmark
    public void testNoExceptionUsingBuilder(final BlackHole bh) {
        final LogEvent event = Log4jLogEvent.newBuilder()
            .setLoggerName("a.b.c")
            .setLoggerFqcn("a.b.c")
            .setLevel(Level.INFO)
            .setMessage(MESSAGE)
            .build();
        bh.consume(event);
    }

    @GenerateMicroBenchmark
    public void testException(final BlackHole bh) {
        final LogEvent event = Log4jLogEvent.newBuilder()
            .setLoggerName("a.b.c")
            .setLoggerFqcn("a.b.c")
            .setLevel(Level.INFO)
            .setMessage(MESSAGE)
            .setThrown(ERROR)
            .build();
        bh.consume(event);
    }

    @GenerateMicroBenchmark
    public void testSourceLocation(final BlackHole bh) {
        final LogEvent event = Log4jLogEvent.newBuilder()
            .setLoggerName(this.getClass().getName())
            .setLoggerFqcn(this.getClass().getName())
            .setLevel(Level.INFO)
            .setMessage(MESSAGE)
            .build();
        event.setIncludeLocation(true);
        final StackTraceElement source = event.getSource();
        bh.consume(source);
    }

    @GenerateMicroBenchmark
    public void testSerializationWithoutException(final BlackHole bh) {
        final Log4jLogEvent event = new Log4jLogEvent("a.b.c", null, "a.b.c", Level.INFO, MESSAGE, null);
        final Serializable serializable = Log4jLogEvent.serialize(event, false);
        bh.consume(serializable);
    }

    @GenerateMicroBenchmark
    public void testSerializationWithException(final BlackHole bh) {
        final Log4jLogEvent event = new Log4jLogEvent("a.b.c", null, "a.b.c", Level.INFO, MESSAGE, ERROR);
        final Serializable serializable = Log4jLogEvent.serialize(event, false);
        bh.consume(serializable);
    }

    // ============================== HOW TO RUN THIS TEST: ====================================
    //
    // In sampling mode (latency test):
    // java -jar log4j-perf/target/microbenchmarks.jar ".*Log4jLogEventBenchmark.*" -i 5 -f 1 -wi 5 -bm sample -tu ns
    //
    // Throughput test:
    // java -jar microbenchmarks.jar ".*Log4jLogEventBenchmark.*" -i 5 -f 1 -wi 5 -bm Throughput -tu ms
    //
    // Usage help:
    // java -jar log4j-perf/target/microbenchmarks.jar -help
    //
}
