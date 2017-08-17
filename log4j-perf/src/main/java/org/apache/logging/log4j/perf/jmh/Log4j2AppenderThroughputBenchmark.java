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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/**
 * Benchmarks Log4j 2's appenders.
 */
// HOW TO RUN THIS TEST
// java -jar log4j-perf/target/benchmarks.jar ".*Log4j2AppenderThroughputBenchmark.*" -f 1 -wi 10 -i 20
//
// RUNNING THIS TEST WITH 4 THREADS:
// java -jar log4j-perf/target/benchmarks.jar ".*Log4j2AppenderThroughputBenchmark.*" -f 1 -wi 10 -i 20 -t 4
@State(Scope.Benchmark)
public class Log4j2AppenderThroughputBenchmark {

    @Param({"150"})
    public int messageSizeBytes;
    @Param({"File", "RAF", "MMap", "Console", "DirectConsole", "Noop", "Rewrite"})
    public String loggerType;

    private static Map<String, String> loggerTypeToAppenderKey = new HashMap<>();
    static {
        loggerTypeToAppenderKey.put("RAF", "RandomAccessFile");
        loggerTypeToAppenderKey.put("MMap", "MemoryMappedFile");
        loggerTypeToAppenderKey.put("Noop", "NoOp");
    }

    static Appender getAppenderByLogger(Logger logger, String loggerType) {
        String appenderKey = loggerTypeToAppenderKey.get(loggerType);
        if (appenderKey == null) {
            appenderKey = loggerType;
        }
        return ((org.apache.logging.log4j.core.Logger) logger).getAppenders().get(appenderKey);
    }

    private String message;
    private LogEvent event;
    private Logger logger;
    private Appender appender;

    static LogEvent createLogEvent(String messageString) {
        final Marker marker = null;
        final String fqcn = "com.mycom.myproject.mypackage.MyClass";
        final Level level = Level.DEBUG;
        final Message message = new SimpleMessage(messageString);
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

        message = new String(new byte[messageSizeBytes], StandardCharsets.US_ASCII);
        event = createLogEvent(message);
        logger = LogManager.getLogger(loggerType + "Logger");
        appender = getAppenderByLogger(logger, loggerType);
    }

    @TearDown
    public void tearDown() {
        System.clearProperty("log4j.configurationFile");
        deleteLogFiles();
    }

    static void deleteLogFiles() {
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
    public void end2end() {
        logger.debug(message);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void appender() {
        appender.append(event);
    }
}
