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
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.GelfLayout;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.perf.util.DemoAppender;
import org.apache.logging.log4j.util.StringMap;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/**
 * Benchmarks Log4j 2 GelfLayout.
 */
@State(Scope.Thread)
public class GelfLayoutBenchmark {
    private static final CharSequence MESSAGE =
            "This is rather long and chatty log message with quite some interesting information and a bit of fun in it which is suitable here";
    private static final LogEvent EVENT = createLogEvent();
    private static final KeyValuePair[] ADDITIONAL_FIELDS = KeyValuePair.EMPTY_ARRAY;

    private static LogEvent createLogEvent() {
        final Marker marker = null;
        final String fqcn = "com.mycom.myproject.mypackage.MyClass";
        final org.apache.logging.log4j.Level level = org.apache.logging.log4j.Level.DEBUG;
        final Message message = new SimpleMessage(MESSAGE);
        final Throwable t = null;
        final StringMap mdc = null;
        final ThreadContext.ContextStack ndc = null;
        final String threadName = null;
        final StackTraceElement location = null;
        final long timestamp = 12345678;

        return Log4jLogEvent.newBuilder() //
                .setLoggerName("name(ignored)") //
                .setMarker(marker) //
                .setLoggerFqcn(fqcn) //
                .setLevel(level) //
                .setMessage(message) //
                .setThrown(t) //
                .setContextData(mdc) //
                .setContextStack(ndc) //
                .setThreadName(threadName) //
                .setSource(location) //
                .setTimeMillis(timestamp) //
                .build();
    }

    Appender appender;
    int j;

    @Setup
    public void setUp() {
        System.setProperty("log4j2.enable.direct.encoders", "true");

        appender = new DemoAppender(GelfLayout.newBuilder()
                .setConfiguration(new NullConfiguration())
                .setHost("host")
                .setAdditionalFields(ADDITIONAL_FIELDS)
                .setCompressionType(GelfLayout.CompressionType.OFF)
                .setCompressionThreshold(0)
                .setIncludeStacktrace(true)
                .setIncludeThreadContext(true)
                .build());

        j = 0;
    }

    @TearDown
    public void tearDown() {
        System.clearProperty("log4j2.enable.direct.encoders");
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public boolean baseline() {
        ++j;
        return true;
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public void log4j2Gelf() {
        appender.append(EVENT);
    }
}
