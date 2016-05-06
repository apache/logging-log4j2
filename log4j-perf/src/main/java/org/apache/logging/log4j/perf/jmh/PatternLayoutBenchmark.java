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
// java -jar log4j-perf/target/benchmarks.jar ".*PatternLayoutBench.*" -f 1 -wi 5 -i 10
// java -Dfile.encoding=UTF8       -jar log4j-perf/target/benchmarks.jar ".*PatternLayoutBench.*" -f 1 -wi 5 -i 10
// java -Dfile.encoding=ISO-8859-1 -jar log4j-perf/target/benchmarks.jar ".*PatternLayoutBench.*" -f 1 -wi 5 -i 10
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
    private final PatternLayout PATTERN_C = PatternLayout.createLayout("%c%n", null, null, null, CHARSET_DEFAULT, false, true, null, null);
    private final PatternLayout PATTERN_D = PatternLayout.createLayout("%d%n", null, null, null, CHARSET_DEFAULT, false, true, null, null);
    private final PatternLayout PATTERN_M_D_NOSPACE = PatternLayout.createLayout("%d%m%n", null, null, null, CHARSET_DEFAULT, false, true, null, null);
    private final PatternLayout PATTERN_M_C_NOSPACE = PatternLayout.createLayout("%c%m%n", null, null, null, CHARSET_DEFAULT, false, true, null, null);
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
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] stringGetBytes() {
        return STR.getBytes();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] byteArrayM() {
        return PATTERN_M.toByteArray(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] byteArraySpace() {
        return PATTERN_SPACE.toByteArray(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] byteArrayMC() {
        return PATTERN_M_C.toByteArray(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] byteArrayMCD() {
        return PATTERN_M_C_D.toByteArray(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] byteArrayMD() {
        return PATTERN_M_D.toByteArray(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] byteArrayMDEx() {
        return PATTERN_M_D_EX.toByteArray(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] byteArrayMEx() {
        return PATTERN_M_EX.toByteArray(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] byteArrayMCDEx() {
        return PATTERN_M_C_D_EX.toByteArray(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] byteArrayC() {
        return PATTERN_C.toByteArray(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] byteArrayD() {
        return PATTERN_D.toByteArray(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] byteArrayMDNoSpace() {
        return PATTERN_M_D_NOSPACE.toByteArray(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] byteArrayMCNoSpace() {
        return PATTERN_M_C_NOSPACE.toByteArray(EVENT);
    }
//---

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String serializableM() {
        return PATTERN_M.toSerializable(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String serializableSpace() {
        return PATTERN_SPACE.toSerializable(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String serializableMC() {
        return PATTERN_M_C.toSerializable(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String serializableMCD() {
        return PATTERN_M_C_D.toSerializable(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String serializableMD() {
        return PATTERN_M_D.toSerializable(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String serializableMDEx() {
        return PATTERN_M_D_EX.toSerializable(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String serializableMEx() {
        return PATTERN_M_EX.toSerializable(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String serializableMCDEx() {
        return PATTERN_M_C_D_EX.toSerializable(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String serializableC() {
        return PATTERN_C.toSerializable(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String serializableD() {
        return PATTERN_D.toSerializable(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String serializableMDNoSpace() {
        return PATTERN_M_D_NOSPACE.toSerializable(EVENT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String serializableMCNoSpace() {
        return PATTERN_M_C_NOSPACE.toSerializable(EVENT);
    }

}
