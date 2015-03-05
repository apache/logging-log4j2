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
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.slf4j.LoggerFactory;

/**
 * Benchmarks Log4j 2, Log4j 1, and Logback using the DEBUG level which is disabled for this test. One of the primary
 * performance concerns of logging frameworks is adding minimal overhead when logging is disabled. Some users disable
 * all logging in production, while others disable finer logging levels in production. This benchmark demonstrates the
 * overhead in calling {@code logger.isDebugEnabled()} and {@code logger.debug()}.
 */
// HOW TO RUN THIS TEST
// java -jar target/benchmarks.jar ".*DebugDisabledBenchmark.*" -f 1 -i 5 -wi 5 -bm sample -tu ns
@State(Scope.Thread)
public class DebugDisabledBenchmark {
    Logger log4jLogger;
    org.slf4j.Logger slf4jLogger;
    org.apache.log4j.Logger log4jClassicLogger;
    Integer j;

    @Setup
    public void setUp() {
        System.setProperty("log4j.configurationFile", "log4j2-perf2.xml");
        System.setProperty("log4j.configuration", "log4j12-perf2.xml");
        System.setProperty("logback.configurationFile", "logback-perf2.xml");

        log4jLogger = LogManager.getLogger(DebugDisabledBenchmark.class);
        slf4jLogger = LoggerFactory.getLogger(DebugDisabledBenchmark.class);
        log4jClassicLogger = org.apache.log4j.Logger.getLogger(DebugDisabledBenchmark.class);
        j = Integer.valueOf(2);
    }

    @TearDown
    public void tearDown() {
        System.clearProperty("log4j.configurationFile");
        System.clearProperty("log4j.configuration");
        System.clearProperty("logback.configurationFile");
    }

    @Benchmark
    public boolean baseline() {
        return true;
    }

    @Benchmark
    public boolean log4jIsDebugEnabled() {
        return log4jLogger.isDebugEnabled();
    }

    @Benchmark
    public boolean slf4jIsDebugEnabled() {
        return slf4jLogger.isDebugEnabled();
    }

    @Benchmark
    public boolean log4jClassicIsDebugEnabled() {
        return log4jClassicLogger.isDebugEnabled();
    }

    @Benchmark
    public void log4jDebugStringConcatenation() {
        log4jLogger.debug("This is a debug [" + j + "] message");
    }

    @Benchmark
    public void slf4jDebugStringConcatenation() {
        slf4jLogger.debug("This is a debug [" + j + "] message");
    }

    @Benchmark
    public void log4jClassicDebugStringConcatenation() {
        log4jClassicLogger.debug("This is a debug [" + j + "] message");
    }

    @Benchmark
    public void log4jDebugParameterizedString() {
        log4jLogger.debug("This is a debug [{}] message", j);
    }

    @Benchmark
    public void slf4jDebugParameterizedString() {
        slf4jLogger.debug("This is a debug [{}] message", j);
    }
}
