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
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks Log4j 2, Log4j 1, and Logback using the DEBUG level which is enabled for this test. The configuration
 * for each uses a FileAppender
 */
// HOW TO RUN THIS TEST
// java -jar target/benchmarks.jar ".*FileAppenderBenchmark.*" -f 1 -i 5 -wi 5 -bm sample -tu ns
@State(Scope.Thread)
public class FileAppenderBenchmark {
    Logger log4j2Logger;
    Logger log4j2RandomLogger;
    org.slf4j.Logger slf4jLogger;
    org.apache.log4j.Logger log4j1Logger;
    int j;

    @Setup
    public void setUp() {
        System.setProperty("log4j.configurationFile", "log4j2-perf.xml");
        System.setProperty("log4j.configuration", "log4j12-perf.xml");
        System.setProperty("logback.configurationFile", "logback-perf.xml");

        File logbackFile = new File("target/testlogback.log");
        logbackFile.delete();
        File log4jFile = new File ("target/testlog4j.log");
        log4jFile.delete();
        File log4j2File = new File ("target/testlog4j2.log");
        log4j2File.delete();
        File log4j2RAF = new File ("target/testRandomlog4j2.log");
        log4j2RAF.delete();

        log4j2Logger = LogManager.getLogger(FileAppenderBenchmark.class);
        log4j2RandomLogger = LogManager.getLogger("TestRandom");
        slf4jLogger = LoggerFactory.getLogger(FileAppenderBenchmark.class);
        log4j1Logger = org.apache.log4j.Logger.getLogger(FileAppenderBenchmark.class);
        j = 0;
    }

    @TearDown
    public void tearDown() {
        System.clearProperty("log4j.configurationFile");
        System.clearProperty("log4j.configuration");
        System.clearProperty("logback.configurationFile");

        File logbackFile = new File("target/testlogback.log");
        logbackFile.delete();
        File log4jFile = new File ("target/testlog4j.log");
        log4jFile.delete();
        File log4jRandomFile = new File ("target/testRandomlog4j2.log");
        log4jRandomFile.delete();
        File log4j2File = new File ("target/testlog4j2.log");
        log4j2File.delete();
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public boolean baseline() {
        ++j;
        return true;
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public void log4j2RAFStringConcatenation() {
        log4j2RandomLogger.debug("This is a debug [" + ++j + "] message");
    }


    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public void log4j2StringConcatenation() {
        log4j2Logger.debug("This is a debug [" + ++j + "] message");
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public void slf4jStringConcatenation() {
        slf4jLogger.debug("This is a debug [" + ++j + "] message");
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public void log4j1StringConcatenation() {
        log4j1Logger.debug("This is a debug [" + ++j + "] message");
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public void log4j2RAFParameterizedString() {
        log4j2RandomLogger.debug("This is a debug [{}] message", ++j);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public void log4j2ParameterizedString() {
        log4j2Logger.debug("This is a debug [{}] message", ++j);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public void slf4jParameterizedString() {
        slf4jLogger.debug("This is a debug [{}] message", ++j);
    }
}
