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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.perf.appender.StringAppender;
import org.apache.logging.log4j.spi.DefaultThreadContextMap;
import org.apache.logging.log4j.spi.GarbageFreeOpenHashMapThreadContextMap;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Compares performance of ThreadContextMap implementations and ScopedContext
 */
// ============================== HOW TO RUN THIS TEST: ====================================
// (Quick build: mvn -DskipTests=true clean package -pl log4j-perf -am )
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*ThreadContextBenchmark2.*"
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ThreadContextBenchmark2 {

    private static final String[] KEYS = new String[] {
        "One",
        "Two",
        "Three",
        "Four",
        "Five",
        "Six",
        "Seven",
        "Eight",
        "Nine",
        "Ten",
        "Eleven",
        "Twelve",
        "Thriteen",
        "Fourteen",
        "Fifteen",
        "Sixteen"
    };
    private static final String[] VALUES =
            new String[] {"Alpha", "Beta", "Gamma", "Delta", "10", "100", "1000", "Hello"};
    private static final String[] NESTED =
            new String[] {Long.toString(System.currentTimeMillis()), "40", "Apache", "Logging"};

    private static final int LOOP_COUNT = 100;

    private static final Logger LOGGER = LogManager.getLogger(ThreadContextBenchmark2.class);

    private static final String DEFAULT_CONTEXT_MAP = "Default";
    private static final String NO_GC_OPENHASH_MAP = "NoGcOpenHash";
    private static final String NO_GC_ARRAY_MAP = "NoGcSortedArray";
    private static final Map<String, Class<? extends ThreadContextMap>> IMPLEMENTATIONS = new HashMap<>();

    static {
        IMPLEMENTATIONS.put(DEFAULT_CONTEXT_MAP, DefaultThreadContextMap.class);
        IMPLEMENTATIONS.put(NO_GC_OPENHASH_MAP, GarbageFreeOpenHashMapThreadContextMap.class);
        IMPLEMENTATIONS.put(
                NO_GC_ARRAY_MAP,
                GarbageFreeOpenHashMapThreadContextMap.SUPER); // GarbageFreeSortedArrayThreadContextMap.class);
    }

    @State(Scope.Benchmark)
    public static class ReadThreadContextState {

        @Param({"Default", "NoGcSortedArray"})
        public String threadContextMapAlias;

        @Setup
        public void setup() {
            System.setProperty(
                    "log4j2.threadContextMap",
                    IMPLEMENTATIONS.get(threadContextMapAlias).getName());
            for (int i = 0; i < VALUES.length; i++) {
                ThreadContext.put(KEYS[i], VALUES[i]);
            }
        }

        @TearDown
        public void teardown() {
            ThreadContext.clearMap();
        }
    }

    @State(Scope.Benchmark)
    public static class ThreadContextState {

        @Param({"Default", "CopySortedArray", "NoGcSortedArray", "StringArray"})
        public String threadContextMapAlias;

        @Setup
        public void setup() {
            System.setProperty(
                    "log4j2.threadContextMap",
                    IMPLEMENTATIONS.get(threadContextMapAlias).getName());
        }

        @TearDown
        public void teardown() {
            ThreadContext.clearMap();
        }
    }

    @State(Scope.Benchmark)
    public static class LogThreadContextState extends ReadThreadContextState {

        public StringAppender appender;
        public LoggerContext context;
        public int counter;

        @Setup
        @Override
        public void setup() {
            super.setup();
            context = (LoggerContext) LogManager.getContext(false);
            Configuration config = context.getConfiguration();
            PatternLayout layout = PatternLayout.newBuilder()
                    .withConfiguration(config)
                    .withPattern("%X %m%n")
                    .build();
            appender = StringAppender.createAppender("String", layout, null);
            appender.start();
            config.getAppenders().forEach((name, app) -> app.stop());
            config.getAppenders().clear();
            config.addAppender(appender);
            final LoggerConfig root = config.getRootLogger();
            root.getAppenders().forEach((name, appender) -> {
                root.removeAppender(name);
            });
            root.addAppender(appender, Level.DEBUG, null);
            root.setLevel(Level.DEBUG);
            context.updateLoggers();
        }

        @TearDown
        public void teardown() {
            System.out.println("Last entry: " + appender.getMessage());
            context.stop();
            counter = 0;
            super.teardown();
        }
    }

    @State(Scope.Benchmark)
    public static class LogBaselineState {

        public StringAppender appender;
        public LoggerContext context;
        public int counter;

        @Setup
        public void setup() {
            context = (LoggerContext) LogManager.getContext(false);
            Configuration config = context.getConfiguration();
            PatternLayout layout = PatternLayout.newBuilder()
                    .withConfiguration(config)
                    .withPattern("%X %m%n")
                    .build();
            appender = StringAppender.createAppender("String", layout, null);
            appender.start();
            config.getAppenders().forEach((name, app) -> app.stop());
            config.getAppenders().clear();
            config.addAppender(appender);
            final LoggerConfig root = config.getRootLogger();
            root.getAppenders().forEach((name, appender) -> {
                root.removeAppender(name);
            });
            root.addAppender(appender, Level.DEBUG, null);
            root.setLevel(Level.DEBUG);
            context.updateLoggers();
        }

        @TearDown
        public void teardown() {
            System.out.println("Last entry: " + appender.getMessage());
            context.stop();
            counter = 0;
        }
    }

    @Benchmark
    public void populateThreadContext(final Blackhole blackhole, ThreadContextState state) {
        for (int i = 0; i < VALUES.length; i++) {
            ThreadContext.put(KEYS[i], VALUES[i]);
        }
    }

    @Benchmark
    public void threadContextMap(final Blackhole blackhole, ReadThreadContextState state) {
        for (int i = 0; i < VALUES.length; i++) {
            blackhole.consume(ThreadContext.get(KEYS[i]));
        }
    }

    /*
     * This is equivalent to the typical ScopedContext case.
     */
    @Benchmark
    public void logThreadContextMap(final Blackhole blackhole, LogThreadContextState state) {
        LOGGER.info("log count: {}", state.counter++);
    }

    @Benchmark
    public void nestedThreadContextMap(final Blackhole blackhole, LogThreadContextState state) {
        for (int i = 0; i < 10; ++i) {
            LOGGER.info("outer log count: {}", i);
        }
        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.put(KEYS[8], NESTED[0])
                .put(KEYS[9], NESTED[1])
                .put(KEYS[10], NESTED[2])) {
            for (int i = 0; i < 100; ++i) {
                LOGGER.info("inner log count: {}", i);
            }
        }
    }

    /*
     * Log the baseline - no context variables.
     */
    @Benchmark
    public void logBaseline(final Blackhole blackhole, LogBaselineState state) {
        LOGGER.info("log count: {}", state.counter++);
    }
}
