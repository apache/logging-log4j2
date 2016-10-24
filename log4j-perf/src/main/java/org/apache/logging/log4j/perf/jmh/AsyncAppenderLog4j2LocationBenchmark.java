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
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.perf.util.BenchmarkMessageParams;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
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

/**
 * Tests Log4j2 Async Appender performance when including caller location information.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*AsyncAppenderLog4j2LocationBenchmark.*"
//
// multiple threads (for example, 4 threads):
// java -jar log4j-perf/target/benchmarks.jar ".*AsyncAppenderLog4j2LocationBenchmark.*" -t 4 -si true
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 10)
@Measurement(iterations = 20)
@Threads(1)
public class AsyncAppenderLog4j2LocationBenchmark {
    Logger logger;

    @Param({
        "perf5AsyncApndWithLoc-noOpAppender.xml",
        "perf5AsyncApndDsrptrWithLoc-noOpAppender.xml",
        "perf5AsyncApndMpscQWithLoc-noOpAppender.xml",
        "perf5AsyncApndXferQWithLoc-noOpAppender.xml"
    })
    public String configFileName;

    @Setup(Level.Trial)
    public void up() {
        System.setProperty("log4j.configurationFile", configFileName);
        logger = LogManager.getLogger(getClass());
    }

    @TearDown(Level.Trial)
    public void down() {
        ((LifeCycle) LogManager.getContext(false)).stop();
        new File("perftest.log").delete();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughputSimple() {
        logger.info(BenchmarkMessageParams.TEST);
    }

}
