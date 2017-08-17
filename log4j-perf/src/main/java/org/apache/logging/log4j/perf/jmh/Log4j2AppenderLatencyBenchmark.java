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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.HdrHistogram.ConcurrentHistogram;
import org.HdrHistogram.Histogram;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.perf.IdleStrategy;
import org.apache.logging.log4j.perf.NoOpIdleStrategy;
import org.apache.logging.log4j.perf.Pacer;
import org.apache.logging.log4j.perf.YieldIdleStrategy;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Latency benchmarks Log4j 2's appenders.
 */
// HOW TO RUN THIS TEST
// java -jar log4j-perf/target/benchmarks.jar ".*Log4j2AppenderLatencyBenchmark.end2end" -t 3 -p loggerType=MMap -p idle=noop -p eventsPerSecPerThread=2000
// Then find the result in log4j-perf/target/MMapLatency.dat
@Fork(1)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 1, time = 60)
@State(Scope.Benchmark)
public class Log4j2AppenderLatencyBenchmark {

    private static final List<String> messageChoice = new ArrayList<>();
    private static final List<LogEvent> eventChoice = new ArrayList<>();
    private static final String smallestMessage;
    private static final LogEvent smallestEvent;
    private static final int choiceLimit;
    static {
        // The last message size is chosen to fit StringBuilderEncoder.DEFAULT_BYTE_BUFFER_SIZE
        int[] messageSizes = new int[] {150, 300, 1000, 3000, 8000};
        for (int messageSize : messageSizes) {
            int sizeSlots = messageSizes[messageSizes.length - 1] / messageSize;
            String message = new String(new byte[messageSize], StandardCharsets.US_ASCII);
            LogEvent event = Log4j2AppenderThroughputBenchmark.createLogEvent(message);
            for (int i = 0; i < sizeSlots; i++) {
                messageChoice.add(message);
                eventChoice.add(event);
            }
        }
        choiceLimit = messageChoice.size();
        smallestMessage = messageChoice.get(0);
        smallestEvent = eventChoice.get(0);
    }

    @Param({"File", "RAF", "MMap", "Console", "DirectConsole", "Noop", "Rewrite"})
    public String loggerType;
    private Logger logger;
    private Appender appender;

    @Param({"noop", "yield"})
    public String idle;
    private IdleStrategy idleStrategy;

    @Param({"200", "2000", "20000", "200000"})
    public int eventsPerSecPerThread;

    private Histogram histogram;

    private long operationStartMillis;

    @Setup(Level.Trial)
    public void setUp() throws Exception {
        System.setProperty("log4j.configurationFile", "log4j2-appenderComparison.xml");
        Log4j2AppenderThroughputBenchmark.deleteLogFiles();

        logger = LogManager.getLogger(loggerType + "Logger");
        appender = Log4j2AppenderThroughputBenchmark.getAppenderByLogger(logger, loggerType);

        if (idle.equals("noop")) {
            idleStrategy = new NoOpIdleStrategy();
        } else if (idle.equals("yield")) {
            idleStrategy = new YieldIdleStrategy();
        }
    }

    @Setup(Level.Iteration)
    public void iterationSetUp() {
        histogram = new ConcurrentHistogram(TimeUnit.SECONDS.toNanos(5), 3);
        operationStartMillis = System.currentTimeMillis();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws FileNotFoundException {
        try (PrintStream out = new PrintStream(new FileOutputStream("target/" + loggerType + "Latency.dat"))) {
            histogram.outputPercentileDistribution(out, 1000.0);
        }
        System.clearProperty("log4j.configurationFile");
        Log4j2AppenderThroughputBenchmark.deleteLogFiles();
    }

    @State(Scope.Thread)
    public static class PacerContainer {
        private Pacer pacer;

        @Setup(Level.Iteration)
        public void setUp(Log4j2AppenderLatencyBenchmark s) {
            pacer = new Pacer(s.eventsPerSecPerThread, s.idleStrategy);
        }
    }

    private long millisFromIterationStart() {
        return System.currentTimeMillis() - operationStartMillis;
    }

    @Benchmark
    public void end2end(PacerContainer pacerContainer) {
        pacerContainer.pacer.acquire(1);
        String message = messageChoice.get(ThreadLocalRandom.current().nextInt(choiceLimit));
        long start = System.nanoTime();
        logger.debug(message);
        long end = System.nanoTime();
        if (message == smallestMessage && millisFromIterationStart() > 1000) {
            histogram.recordValue(end - start);
        }
    }

    @Benchmark
    public void appender(PacerContainer pacerContainer) {
        pacerContainer.pacer.acquire(1);
        LogEvent event = eventChoice.get(ThreadLocalRandom.current().nextInt(choiceLimit));
        long start = System.nanoTime();
        appender.append(event);
        long end = System.nanoTime();
        if (event == smallestEvent && millisFromIterationStart() > 1000) {
            histogram.recordValue(end - start);
        }
    }
}
