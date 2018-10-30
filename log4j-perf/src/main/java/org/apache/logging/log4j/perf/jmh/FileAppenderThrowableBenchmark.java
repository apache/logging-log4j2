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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Level;

/**
 * Benchmarks Log4j 2, Log4j 1, Logback and JUL using the ERROR level which is enabled for this test.
 * The configuration for each writes to disk.
 */
// HOW TO RUN THIS TEST
// java -jar target/benchmarks.jar ".*FileAppenderThrowableBenchmark.*" -f 1 -i 10 -wi 20 -bm sample -tu ns
@State(Scope.Thread)
@Threads(1)
@Fork(1)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 3, time = 3)
public class FileAppenderThrowableBenchmark {

    private static final Throwable THROWABLE = getSimpleThrowable();
    private static final Throwable COMPLEX_THROWABLE = getComplexThrowable();

    private static Throwable getSimpleThrowable() {
        return new IllegalStateException("Test Throwable");
    }

    interface ThrowableHelper {
        void action();
    }

    // Used to create a deeper stack with many different classes
    // This makes the ThrowableProxy Map<String, CacheEntry> cache
    // perform more closely to real applications.
    interface TestIface0 extends ThrowableHelper {}
    interface TestIface1 extends ThrowableHelper {}
    interface TestIface2 extends ThrowableHelper {}
    interface TestIface3 extends ThrowableHelper {}
    interface TestIface4 extends ThrowableHelper {}
    interface TestIface5 extends ThrowableHelper {}
    interface TestIface6 extends ThrowableHelper {}
    interface TestIface7 extends ThrowableHelper {}
    interface TestIface8 extends ThrowableHelper {}
    interface TestIface9 extends ThrowableHelper {}
    interface TestIface10 extends ThrowableHelper {}
    interface TestIface11 extends ThrowableHelper {}
    interface TestIface12 extends ThrowableHelper {}
    interface TestIface13 extends ThrowableHelper {}
    interface TestIface14 extends ThrowableHelper {}
    interface TestIface15 extends ThrowableHelper {}
    interface TestIface16 extends ThrowableHelper {}
    interface TestIface17 extends ThrowableHelper {}
    interface TestIface18 extends ThrowableHelper {}
    interface TestIface19 extends ThrowableHelper {}
    interface TestIface20 extends ThrowableHelper {}
    interface TestIface21 extends ThrowableHelper {}
    interface TestIface22 extends ThrowableHelper {}
    interface TestIface23 extends ThrowableHelper {}
    interface TestIface24 extends ThrowableHelper {}
    interface TestIface25 extends ThrowableHelper {}
    interface TestIface26 extends ThrowableHelper {}
    interface TestIface27 extends ThrowableHelper {}
    interface TestIface28 extends ThrowableHelper {}
    interface TestIface29 extends ThrowableHelper {}
    interface TestIface30 extends ThrowableHelper {}

    private static Throwable getComplexThrowable() {
        ThrowableHelper helper = new ThrowableHelper() {
            @Override
            public void action() {
                throw new IllegalStateException("Test Throwable");
            }
        };
        try {
            for (int i = 0; i < 31; i++) {
                final ThrowableHelper delegate = helper;
                helper = (ThrowableHelper) Proxy.newProxyInstance(
                        FileAppenderThrowableBenchmark.class.getClassLoader(),
                        new Class<?>[]{Class.forName(FileAppenderThrowableBenchmark.class.getName() + "$TestIface" + (i % 31))},
                        new InvocationHandler() {
                            @Override
                            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                                try {
                                    return method.invoke(delegate, args);
                                } catch (final InvocationTargetException e) {
                                    throw e.getCause();
                                }
                            }
                        });
            }
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException("Failed to create stack", e);
        }
        try {
            helper.action();
        } catch (final IllegalStateException e) {
            return e;
        }
        throw new IllegalStateException("Failed to create throwable");
    }

    private FileHandler julFileHandler;
    Logger log4j2ExtendedThrowable;
    Logger log4j2ExtendedThrowableAsync;
    Logger log4j2SimpleThrowable;
    Logger log4j2SimpleThrowableAsync;
    org.slf4j.Logger slf4jLogger;
    org.apache.log4j.Logger log4j1Logger;
    java.util.logging.Logger julLogger;

    @Setup
    public void setUp() throws Exception {
        deleteLogFiles();
        System.setProperty("log4j2.is.webapp", "false");
        System.setProperty("log4j.configurationFile", "log4j2-perf-file-throwable.xml");
        System.setProperty("log4j.configuration", "log4j12-perf-file-throwable.xml");
        System.setProperty("logback.configurationFile", "logback-perf-file-throwable.xml");

        log4j2ExtendedThrowable = LogManager.getLogger("RAFExtendedException");
        log4j2ExtendedThrowableAsync = LogManager.getLogger("async.RAFExtendedException");
        log4j2SimpleThrowable = LogManager.getLogger("RAFSimpleException");
        log4j2SimpleThrowableAsync = LogManager.getLogger("async.RAFSimpleException");
        slf4jLogger = LoggerFactory.getLogger(getClass());
        log4j1Logger = org.apache.log4j.Logger.getLogger(getClass());

        julFileHandler = new FileHandler("target/testJulLog.log");
        julLogger = java.util.logging.Logger.getLogger(getClass().getName());
        julLogger.setUseParentHandlers(false);
        julLogger.addHandler(julFileHandler);
        julLogger.setLevel(Level.ALL);
    }

    @TearDown
    public void tearDown() {
        System.clearProperty("log4j2.is.webapp");
        System.clearProperty("log4j.configurationFile");
        System.clearProperty("log4j.configuration");
        System.clearProperty("logback.configurationFile");
        deleteLogFiles();
    }

    private void deleteLogFiles() {
        final File logbackFile = new File("target/testlogback.log");
        logbackFile.delete();
        final File log4jFile = new File ("target/testlog4j.log");
        log4jFile.delete();
        final File log4jRandomFile = new File ("target/extended-exception.log");
        log4jRandomFile.delete();
        final File log4j2File = new File ("target/simple-exception.log");
        log4j2File.delete();
        final File julFile = new File("target/testJulLog.log");
        julFile.delete();
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void log4j1() {
        log4j1Logger.error("Caught an exception", THROWABLE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void complexLog4j1() {
        log4j1Logger.error("Caught an exception", COMPLEX_THROWABLE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void log4j2Throwable() {
        log4j2SimpleThrowable.error("Caught an exception", THROWABLE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void complexLog4j2Throwable() {
        log4j2SimpleThrowable.error("Caught an exception", COMPLEX_THROWABLE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void complexLog4j2ThrowableAsync() {
        log4j2SimpleThrowableAsync.error("Caught an exception", COMPLEX_THROWABLE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void log4j2ExtendedThrowable() {
        log4j2ExtendedThrowable.error("Caught an exception", THROWABLE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void complexLog4j2ExtendedThrowable() {
        log4j2ExtendedThrowable.error("Caught an exception", COMPLEX_THROWABLE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void complexLog4j2ExtendedThrowableAsync() {
        log4j2ExtendedThrowableAsync.error("Caught an exception", COMPLEX_THROWABLE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void logbackFile() {
        slf4jLogger.error("Caught an exception", THROWABLE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void complexLogbackFile() {
        slf4jLogger.error("Caught an exception", COMPLEX_THROWABLE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void julFile() {
        // must specify sourceClass or JUL will look it up by walking the stack trace!
        julLogger.logp(Level.SEVERE, getClass().getName(), "param1JulFile", "Caught an exception", THROWABLE);
    }
}
