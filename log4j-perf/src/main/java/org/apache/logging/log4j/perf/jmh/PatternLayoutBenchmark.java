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

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * Tests Log4j2 PatternLayout performance.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*PatternLayout.*" -f 1 -wi 5 -i 5
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Thread)
public class PatternLayoutBenchmark {

    final static String STR = "AB!(%087936DZYXQWEIOP$#^~-=/><nb"; // length=32
    final static LogEvent EVENT = createLogEvent();
    private static final String STRING_ISO8859_1 = "ISO-8859-1";
    private static final Charset CHARSET_ISO8859_1 = Charset.forName(STRING_ISO8859_1);
    private static final Charset CHARSET_DEFAULT = Charset.defaultCharset();
    private static final String DEFAULT_ENCODING = CHARSET_DEFAULT.name();
    private static final String STRING_SHIFT_JIS = "SHIFT_JIS";
    private static final Charset CHARSET_SHIFT_JIS = Charset.forName(STRING_SHIFT_JIS);
    private final PatternLayout PATTERN_M = PatternLayout.createLayout("%m%n", null, null, null, CHARSET_DEFAULT, false, true, null, null);
    private final PatternLayout PATTERN_SPACE = PatternLayout.createLayout(" ", null, null, null, CHARSET_DEFAULT, false, true, null, null);
    private final PatternLayout PATTERN_M_C = PatternLayout.createLayout("%c %m%n", null, null, null, CHARSET_DEFAULT, false, true, null, null);
    private final PatternLayout PATTERN_M_C_D = PatternLayout.createLayout("%d %c %m%n", null, null, null, CHARSET_DEFAULT, false, true, null, null);
    private final PatternLayout PATTERN_M_D = PatternLayout.createLayout("%d %m%n", null, null, null, CHARSET_DEFAULT, false, true, null, null);
    private final PatternLayout PATTERN_M_EX = PatternLayout.createLayout("%m %ex%n", null, null, null, CHARSET_DEFAULT, false, true, null, null);
    private final PatternLayout PATTERN_M_D_EX = PatternLayout.createLayout("%d %m%ex%n", null, null, null, CHARSET_DEFAULT, false, true, null, null);
    private final PatternLayout PATTERN_M_C_D_EX = PatternLayout.createLayout("%d %c %m%ex%n", null, null, null, CHARSET_DEFAULT, false, true, null, null);

    private static LogEvent createLogEvent() {
        final Marker marker = null;
        final String fqcn = "com.mycom.myproject.mypackage.MyClass";
        final Level level = Level.DEBUG;
        final Message message = new SimpleMessage(STR);
        final Throwable t = null;
        final Map<String, String> mdc = null;
        final ContextStack ndc = null;
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
                .setContextMap(mdc) //
                .setContextStack(ndc) //
                .setThreadName(threadName) //
                .setSource(location) //
                .setTimeMillis(timestamp) //
                .build();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public byte[] throughputStringGetBytes() {
        return STR.getBytes();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public byte[] throughputPatternM() {
        return PATTERN_M.toByteArray(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public byte[] throughputPatternSpace() {
        return PATTERN_SPACE.toByteArray(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public byte[] throughputPatternMC() {
        return PATTERN_M_C.toByteArray(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public byte[] throughputPatternMCD() {
        return PATTERN_M_C_D.toByteArray(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public byte[] throughputPatternMD() {
        return PATTERN_M_D.toByteArray(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public byte[] throughputPatternMDEx() {
        return PATTERN_M_D_EX.toByteArray(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public byte[] throughputPatternMEx() {
        return PATTERN_M_EX.toByteArray(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public byte[] throughputPatternMCDEx() {
        return PATTERN_M_C_D_EX.toByteArray(EVENT);
    }

}
