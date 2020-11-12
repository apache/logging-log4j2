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
import org.apache.logging.log4j.core.async.AsyncLoggerContext;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;
import org.apache.logging.log4j.core.util.Constants;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
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
@State(Scope.Benchmark)
@Threads(1)
@Fork(1)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 3, time = 3)
public class FileAppenderThrowableBenchmark {
    static {
        // log4j2
        System.setProperty("log4j2.is.webapp", "false");
        System.setProperty("log4j.configurationFile", "log4j2-perf-file-throwable.xml");
        // log4j 1.2
        System.setProperty("log4j.configuration", "log4j12-perf-file-throwable.xml");
        // logback
        System.setProperty("logback.configurationFile", "logback-perf-file-throwable.xml");
    }

    private static final Throwable THROWABLE = getSimpleThrowable();
    private static final Throwable COMPLEX_THROWABLE = getComplexThrowable();

    @SuppressWarnings("unused") // Set by JMH
    @Param
    private LoggingConfiguration loggingConfiguration;

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
        ThrowableHelper helper = () -> {
            throw new IllegalStateException("Test Throwable");
        };
        try {
            for (int i = 0; i < 31; i++) {
                final ThrowableHelper delegate = helper;
                helper = (ThrowableHelper) Proxy.newProxyInstance(
                        FileAppenderThrowableBenchmark.class.getClassLoader(),
                        new Class<?>[]{Class.forName(FileAppenderThrowableBenchmark.class.getName() + "$TestIface" + (i % 31))},
                        (InvocationHandler) (proxy, method, args) -> {
                            try {
                                return method.invoke(delegate, args);
                            } catch (final InvocationTargetException e) {
                                throw e.getCause();
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


    @Setup
    public void setUp() throws Exception {
        deleteLogFiles();
        loggingConfiguration.setUp();
    }

    @TearDown
    public void tearDown() throws Exception{
        loggingConfiguration.tearDown();
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

    @SuppressWarnings("unused") // Used by JMH
    public enum LoggingConfiguration {
        LOG4J2_EXTENDED_THROWABLE() {
            Logger logger;
            @Override
            void setUp() throws Exception {
                logger = LogManager.getLogger("RAFExtendedException");
            }

            @Override
            void tearDown() throws Exception {

            }

            @Override
            void log(String message, Throwable throwable) {
                logger.error(message, throwable);
            }
        },
        LOG4J2_EXTENDED_THROWABLE_ASYNC() {
            Logger logger;
            @Override
            void setUp() throws Exception {
                System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR,
                        AsyncLoggerContextSelector.class.getName());
                logger = LogManager.getLogger("RAFExtendedException");
                if (!AsyncLoggerContext.class.equals(LogManager.getContext(false).getClass())) {
                    throw new IllegalStateException("Expected an AsyncLoggerContext");
                }
            }

            @Override
            void tearDown() throws Exception {

            }

            @Override
            void log(String message, Throwable throwable) {
                logger.error(message, throwable);
            }
        },
        LOG4J2_EXTENDED_THROWABLE_ASYNC_CONFIG() {
            Logger logger;
            @Override
            void setUp() throws Exception {
                logger = LogManager.getLogger("async.RAFExtendedException");
            }

            @Override
            void tearDown() throws Exception {

            }

            @Override
            void log(String message, Throwable throwable) {
                logger.error(message, throwable);
            }
        },
        LOG4J2_THROWABLE() {
            Logger logger;
            @Override
            void setUp() throws Exception {
                logger = LogManager.getLogger("RAFSimpleException");
            }

            @Override
            void tearDown() throws Exception {

            }

            @Override
            void log(String message, Throwable throwable) {
                logger.error(message, throwable);
            }
        },
        LOG4J2_THROWABLE_ASYNC() {
            Logger logger;
            @Override
            void setUp() throws Exception {
                System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR,
                        AsyncLoggerContextSelector.class.getName());
                logger = LogManager.getLogger("RAFSimpleException");
                if (!AsyncLoggerContext.class.equals(LogManager.getContext(false).getClass())) {
                    throw new IllegalStateException("Expected an AsyncLoggerContext");
                }
            }

            @Override
            void tearDown() throws Exception {

            }

            @Override
            void log(String message, Throwable throwable) {
                logger.error(message, throwable);
            }
        },
        LOG4J2_THROWABLE_ASYNC_CONFIG() {
            Logger logger;
            @Override
            void setUp() throws Exception {
                logger = LogManager.getLogger("async.RAFSimpleException");
            }

            @Override
            void tearDown() throws Exception {

            }

            @Override
            void log(String message, Throwable throwable) {
                logger.error(message, throwable);
            }
        },
        LOG4J1() {
            org.apache.log4j.Logger logger;
            @Override
            void setUp() throws Exception {
                logger = org.apache.log4j.Logger.getLogger(FileAppenderThrowableBenchmark.class);
            }

            @Override
            void tearDown() throws Exception {

            }

            @Override
            void log(String message, Throwable throwable) {
                logger.error(message, throwable);
            }
        },
        LOGBACK() {
            org.slf4j.Logger logger;

            @Override
            void setUp() throws Exception {
                logger = LoggerFactory.getLogger(FileAppenderThrowableBenchmark.class);
            }

            @Override
            void tearDown() throws Exception {

            }

            @Override
            void log(String message, Throwable throwable) {
                logger.error(message, throwable);
            }
        },
        JUL() {
            private FileHandler julFileHandler;
            private java.util.logging.Logger logger;

            @Override
            void setUp() throws Exception {
                julFileHandler = new FileHandler("target/testJulLog.log");
                logger = java.util.logging.Logger.getLogger(getClass().getName());
                logger.setUseParentHandlers(false);
                logger.addHandler(julFileHandler);
                logger.setLevel(Level.ALL);
            }

            @Override
            void tearDown() throws Exception {
                julFileHandler.close();
            }

            @Override
            void log(String message, Throwable throwable) {
                // must specify sourceClass or JUL will look it up by walking the stack trace!
                logger.logp(Level.SEVERE, FileAppenderThrowableBenchmark.class.getName(), "param1JulFile", message, throwable);
            }
        };

        abstract void setUp() throws Exception;

        abstract void tearDown() throws Exception;

        abstract void log(String message, Throwable throwable);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void simpleThrowable() {
        loggingConfiguration.log("Caught an exception", THROWABLE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void complexThrowable() {
        loggingConfiguration.log("Caught an exception", COMPLEX_THROWABLE);
    }
}
