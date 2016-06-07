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

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/**
 * Benchmarks Log4j 2's Console, File, RandomAccessFile, MemoryMappedFile and Rewrite appender.
 */
// HOW TO RUN THIS TEST
// java -jar log4j-perf/target/benchmarks.jar ".*Log4j2AppenderComparisonBenchmark.*" -f 1 -wi 10 -i 20
//
// RUNNING THIS TEST WITH 4 THREADS:
// java -jar log4j-perf/target/benchmarks.jar ".*Log4j2AppenderComparisonBenchmark.*" -f 1 -wi 10 -i 20 -t 4
@State(Scope.Benchmark)
public class Log4j2AppenderComparisonBenchmark {
    public static final String MESSAGE = "Short message";
    private final LogEvent EVENT = createLogEvent();

    private Logger fileLogger;
    private Logger rafLogger;
    private Logger mmapLogger;
    private Logger consoleLogger;
    private Logger directConsoleLogger;
    private Logger noopLogger;
    private Logger rewriteLogger;
    private Appender fileAppender;
    private Appender rafAppender;
    private Appender mmapAppender;
    private Appender consoleAppender;
    private Appender directConsoleAppender;
    private Appender noopAppender;
    private Appender rewriteAppender;

    private static LogEvent createLogEvent() {
        final Marker marker = null;
        final String fqcn = "com.mycom.myproject.mypackage.MyClass";
        final Level level = Level.DEBUG;
        final Message message = new SimpleMessage(MESSAGE);
        final Throwable t = null;
        final Map<String, String> mdc = null;
        final ThreadContext.ContextStack ndc = null;
        final String threadName = "THREAD";
        final StackTraceElement location = null;
        final long timestamp = System.currentTimeMillis();

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

    @Setup
    public void setUp() throws Exception {
        System.setProperty("log4j.configurationFile", "log4j2-appenderComparison.xml");
        deleteLogFiles();

        fileLogger = LogManager.getLogger("FileLogger");
        rafLogger = LogManager.getLogger("RAFLogger");
        mmapLogger = LogManager.getLogger("MMapLogger");
        consoleLogger = LogManager.getLogger("ConsoleLogger");
        directConsoleLogger = LogManager.getLogger("DirectConsoleLogger");
        noopLogger = LogManager.getLogger("NoopLogger");
        rewriteLogger = LogManager.getLogger("RewriteLogger");

        fileAppender = ((org.apache.logging.log4j.core.Logger) fileLogger).getAppenders().get("File");
        rafAppender = ((org.apache.logging.log4j.core.Logger) rafLogger).getAppenders().get("RandomAccessFile");
        mmapAppender = ((org.apache.logging.log4j.core.Logger) mmapLogger).getAppenders().get("MemoryMappedFile");
        consoleAppender = ((org.apache.logging.log4j.core.Logger) consoleLogger).getAppenders().get("Console");
        directConsoleAppender = ((org.apache.logging.log4j.core.Logger) directConsoleLogger).getAppenders().get("DirectConsole");
        noopAppender = ((org.apache.logging.log4j.core.Logger) noopLogger).getAppenders().get("NoOp");
        rewriteAppender = ((org.apache.logging.log4j.core.Logger) rewriteLogger).getAppenders().get("Rewrite");
    }

    @TearDown
    public void tearDown() {
        System.clearProperty("log4j.configurationFile");
        deleteLogFiles();
    }

    private void deleteLogFiles() {
        final File log4j2File = new File ("target/testlog4j2.log");
        log4j2File.delete();
        final File log4jRandomFile = new File ("target/testRandomlog4j2.log");
        log4jRandomFile.delete();
        final File mmapFile = new File ("target/MemoryMappedFileAppenderTest.log");
        mmapFile.delete();
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void baseline() {
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void end2endRAF() {
        rafLogger.debug(MESSAGE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void appenderRAF() {
        rafAppender.append(EVENT);
    }


    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void end2endFile() {
        fileLogger.debug(MESSAGE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void appenderFile() {
        fileAppender.append(EVENT);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void end2endMMap() {
        mmapLogger.debug(MESSAGE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void appenderMMap() {
        mmapAppender.append(EVENT);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void end2endNoop() {
        noopLogger.debug(MESSAGE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void appenderNoop() {
        noopAppender.append(EVENT);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void end2endRewrite() {
        rewriteLogger.debug(MESSAGE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void appenderRewrite() {
        rewriteAppender.append(EVENT);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void end2endConsole() {
        consoleLogger.debug(MESSAGE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void appenderConsole() {
        consoleAppender.append(EVENT);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void end2endDirectConsole() {
        directConsoleLogger.debug(MESSAGE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void appenderDirectConsole() {
        directConsoleAppender.append(EVENT);
    }
}
