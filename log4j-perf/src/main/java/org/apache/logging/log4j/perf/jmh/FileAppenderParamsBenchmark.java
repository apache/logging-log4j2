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
import java.util.logging.FileHandler;
import java.util.logging.Level;

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

import static org.apache.logging.log4j.util.Unbox.*;

/**
 * Benchmarks Log4j 2, Log4j 1, Logback and JUL using the DEBUG level which is enabled for this test. The configuration
 * for each uses a FileAppender
 */
// HOW TO RUN THIS TEST
// java -jar target/benchmarks.jar ".*FileAppenderParamsBenchmark.*" -f 1 -i 10 -wi 20 -bm sample -tu ns
@State(Scope.Thread)
public class FileAppenderParamsBenchmark {
    private FileHandler julFileHandler;
    Logger log4j2Logger;
    Logger log4j2RandomLogger;
    org.slf4j.Logger slf4jLogger;
    org.apache.log4j.Logger log4j1Logger;
    java.util.logging.Logger julLogger;
    int j, k, m;

    @Setup
    public void setUp() throws Exception {
        System.setProperty("log4j.configurationFile", "log4j2-perf.xml");
        System.setProperty("log4j.configuration", "log4j12-perf.xml");
        System.setProperty("logback.configurationFile", "logback-perf.xml");

        deleteLogFiles();

        log4j2Logger = LogManager.getLogger(getClass());
        log4j2RandomLogger = LogManager.getLogger("TestRandom");
        slf4jLogger = LoggerFactory.getLogger(getClass());
        log4j1Logger = org.apache.log4j.Logger.getLogger(getClass());

        julFileHandler = new FileHandler("target/testJulLog.log");
        julLogger = java.util.logging.Logger.getLogger(getClass().getName());
        julLogger.setUseParentHandlers(false);
        julLogger.addHandler(julFileHandler);
        julLogger.setLevel(Level.ALL);
        j = 0;
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
        final File log4jRandomFile = new File ("target/testRandomlog4j2.log");
        log4jRandomFile.delete();
        final File log4j2File = new File ("target/testlog4j2.log");
        log4j2File.delete();
        final File julFile = new File("target/testJulLog.log");
        julFile.delete();
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void param1Log4j1Concat() {
        log4j1Logger.debug("This is a debug [" + ++j + "] message");
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void param3Log4j1Concat() {
        log4j1Logger.debug("Val1=" + ++j + ", val2=" + ++k + ", val3=" + ++m);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void param1Log4j2RAF() {
        log4j2RandomLogger.debug("This is a debug [{}] message", box(++j));
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void param3Log4j2RAF() {
        log4j2RandomLogger.debug("Val1={}, val2={}, val3={}", box(++j), box(++k), box(++m));
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void param1Log4j2File() {
        log4j2Logger.debug("This is a debug [{}] message", box(++j));
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void param3Log4j2File() {
        log4j2Logger.debug("Val1={}, val2={}, val3={}", box(++j), box(++k), box(++m));
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void param1LogbackFile() {
        slf4jLogger.debug("This is a debug [{}] message", ++j);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void param3LogbackFile() {
        slf4jLogger.debug("Val1={}, val2={}, val3={}", (++j), (++k), (++m));
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void param1JulFile() {
        // must specify sourceClass or JUL will look it up by walking the stack trace!
        julLogger.logp(Level.INFO, getClass().getName(), "param1JulFile", "This is a debug [{}] message", ++j);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void param3JulFile() {
        // must specify sourceClass or JUL will look it up by walking the stack trace!
        julLogger.logp(Level.INFO, getClass().getName(), "param3JulFile", "Val1={}, val2={}, val3={}",
                new Object[]{++j, ++k, ++m});
    }
}
