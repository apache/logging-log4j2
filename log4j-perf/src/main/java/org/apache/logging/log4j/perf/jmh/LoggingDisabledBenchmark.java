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
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.slf4j.LoggerFactory;

//import com.newrelic.api.agent.Trace;

/**
 * Benchmark logging with logging disabled.
 * // ============================== HOW TO RUN THIS TEST: ====================================
 * //
 * // single thread:
 * // java -jar log4j-perf/target/benchmarks.jar ".*LoggingDisabledBenchmark.*" -f 1 -wi 5 -i 10
 * //
 * // multiple threads (for example, 4 threads):
 * // java -jar log4j-perf/target/benchmarks.jar ".*LoggingDisabledBenchmark.*" -f 1 -wi 5 -i 10 -t 4 -si true
 * //
 * // Usage help:
 * // java -jar log4j-perf/target/benchmarks.jar -help
 * //
 */
@State(Scope.Thread)
public class LoggingDisabledBenchmark {

    Logger log4j2Logger;
    org.slf4j.Logger slf4jLogger;
    org.apache.log4j.Logger log4j1Logger;

    @Setup
    public void setUp() throws Exception {
        System.setProperty("log4j.configurationFile", "log4j2-perf2.xml");
        System.setProperty("log4j.configuration", "log4j12-perf2.xml");
        System.setProperty("logback.configurationFile", "logback-perf2.xml");

        deleteLogFiles();

        log4j2Logger = LogManager.getLogger(FileAppenderWithLocationBenchmark.class);
        slf4jLogger = LoggerFactory.getLogger(FileAppenderWithLocationBenchmark.class);
        log4j1Logger = org.apache.log4j.Logger.getLogger(FileAppenderWithLocationBenchmark.class);
    }

    @TearDown
    public void tearDown() {
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
        final File log4j2File = new File ("target/testlog4j2.log");
        log4j2File.delete();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime) @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void baseline() {
    }

    /*
      This benchmark tests the overhead of NewRelic on method calls. It is commented out so
      that we don't have to include the dependency during a "normal" build. Uncomment and add
      the New Relic Agent client dependency if you would like to test this.
    @Benchmark
    @BenchmarkMode(Mode.AverageTime) @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Trace(dispatcher = true)
    public void log4j2NewRelic() {
        log4j2Logger.debug("This won't be logged");
    } */

    @Benchmark
    @BenchmarkMode(Mode.AverageTime) @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void log4j2() {
        log4j2Logger.debug("This won't be logged");
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime) @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void slf4j() {
        slf4jLogger.debug("This won't be logged");
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime) @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void log4j2IsDebugEnabled() {
        if (log4j2Logger.isDebugEnabled()) {
            log4j2Logger.debug("This won't be logged");
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime) @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void slf4jIsDebugEnabled() {
        if (slf4jLogger.isDebugEnabled()) {
            slf4jLogger.debug("This won't be logged");
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime) @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void log4j1IsDebugEnabled() {
        if (log4j1Logger.isDebugEnabled()) {
            log4j1Logger.debug("This won't be logged");
        }
    }

}
