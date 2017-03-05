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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;

/**
 * Compares Log4j2 with Logback PatternLayout performance.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*PatternLayoutComparison.*" -f 1 -wi 10 -i 10 -tu ns -bm sample
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Thread)
public class PatternLayoutComparisonBenchmark {

    final static String STR = "AB!(%087936DZYXQWEIOP$#^~-=/><nb"; // length=32
    final static LogEvent LOG4J2EVENT = createLog4j2Event();
    private static final Charset CHARSET_DEFAULT = Charset.defaultCharset();
    private static final String LOG4JPATTERN = "%d %5p [%t] %c{1} %X{transactionId} - %m%n";
    private final PatternLayout LOG4J2_PATTERN_LAYOUT = PatternLayout.createLayout(LOG4JPATTERN, null,
            null, null, CHARSET_DEFAULT, false, true, null, null);

    private static LogEvent createLog4j2Event() {
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

    // Logback
    private static final String LOGBACKPATTERN = "%d %5p [%t] %c{0} %X{transactionId} - %m%n";
    private final PatternLayoutEncoder patternLayoutEncoder = new PatternLayoutEncoder();
    private final LoggerContext context = new LoggerContext();
    private final Logger logger = context.getLogger(PatternLayoutComparisonBenchmark.class);
    private final ILoggingEvent LOGBACKEVENT = makeLoggingEvent(STR);
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    @Setup
    public void setUp() throws IOException {
        patternLayoutEncoder.setPattern(LOGBACKPATTERN);
        patternLayoutEncoder.setContext(context);
        patternLayoutEncoder.start();
        ((ch.qos.logback.classic.PatternLayout) patternLayoutEncoder.getLayout()).setOutputPatternAsHeader(false);
    }

    ILoggingEvent makeLoggingEvent(final String message) {
        return new LoggingEvent(PatternLayoutComparisonBenchmark.class.getName(), logger,
                ch.qos.logback.classic.Level.DEBUG, message, null, null);
    }

    @Benchmark
    public byte[] logback() throws IOException {
        baos.reset();
        return patternLayoutEncoder.encode(LOGBACKEVENT);
    }

    @Benchmark
    public byte[] log4j2() {
        return LOG4J2_PATTERN_LAYOUT.toByteArray(LOG4J2EVENT);
    }

}
