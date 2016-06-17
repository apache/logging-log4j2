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

import java.util.concurrent.TimeUnit;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.MemoryHandler;

import org.apache.logging.log4j.perf.util.BenchmarkMessageParams;
import org.apache.logging.log4j.perf.util.NoOpJULHandler;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * Tests JUL (java.util.logging) Memory Handler performance when including caller location information.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*MemoryHandlerJULLocationBenchmark.*" -f 1 -wi 10 -i 20
//
// multiple threads (for example, 4 threads):
// java -jar log4j-perf/target/benchmarks.jar ".*MemoryHandlerJULLocationBenchmark.*" -f 1 -wi 10 -i 20 -t 4 -si true
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Benchmark)
public class MemoryHandlerJULLocationBenchmark {

    Logger logger;
    MemoryHandler memoryHandler;

    @Setup(Level.Trial)
    public void up() {
        memoryHandler = new MemoryHandler(new NoOpJULHandler(), 262144, java.util.logging.Level.SEVERE);
        logger = Logger.getLogger(getClass().getName());
        logger.setUseParentHandlers(false);
        logger.addHandler(memoryHandler);
        logger.setLevel(java.util.logging.Level.ALL);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughputSimple() {
        final LogRecord logRecord = new LogRecord(java.util.logging.Level.INFO, BenchmarkMessageParams.TEST);
        logRecord.getSourceClassName(); // force location
        logger.log(logRecord);
    }
}
