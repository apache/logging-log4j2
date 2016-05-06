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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.perf.util.BenchmarkMessageParams;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import static org.apache.logging.log4j.perf.util.BenchmarkMessageParams.*;

/**
 * Tests Log4j-1.2 Async Appender performance.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*AsyncAppenderLog4j1Benchmark.*" -f 1 -wi 10 -i 20
//
// multiple threads (for example, 4 threads):
// java -jar log4j-perf/target/benchmarks.jar ".*AsyncAppenderLog4j1Benchmark.*" -f 1 -wi 10 -i 20 -t 4 -si true
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Benchmark)
public class AsyncAppenderLog4j1Benchmark {
    Logger logger;

    @Setup(Level.Trial)
    public void up() {
        System.setProperty("log4j.configuration", "perf-log4j12-async-noOpAppender.xml");
        logger = LogManager.getLogger(getClass());
    }

    @TearDown(Level.Trial)
    public void down() {
        LogManager.shutdown();
        new File("perftest.log").delete();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughputSimple() {
        logger.info(BenchmarkMessageParams.TEST);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput1Param() {
        logger.info("p1=" + one);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput2Params() {
        logger.info("p1=" + one + ", p2=" + two);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput3Params() {
        logger.info("p1=" + one + ", p2=" + two + ", p3=" + three);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput4Params() {
        logger.info("p1=" + one + ", p2=" + two + ", p3=" + three + ", p4=" + four);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput5Params() {
        logger.info("p1=" + one + ", p2=" + two + ", p3=" + three + ", p4=" + four + ", p5=" + five);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput6Params() {
        logger.info("p1=" + one + ", p2=" + two + ", p3=" + three + ", p4=" + four + ", p5=" + five + ", p6=" + six);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput7Params() {
        logger.info("p1=" + one + ", p2=" + two + ", p3=" + three + ", p4=" + four + ", p5=" + five + ", p6=" + six + ", p7=" + seven);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput8Params() {
        logger.info("p1=" + one + ", p2=" + two + ", p3=" + three + ", p4=" + four + ", p5=" + five + ", p6=" + six + ", p7=" + seven + ", p8=" + eight);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput9Params() {
        logger.info("p1=" + one + ", p2=" + two + ", p3=" + three + ", p4=" + four + ", p5=" + five + ", p6=" + six + ", p7=" + seven + ", p8=" + eight + ", p9=" + nine);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput10Params() {
        logger.info("p1=" + one + ", p2=" + two + ", p3=" + three + ", p4=" + four + ", p5=" + five + ", p6=" + six + ", p7=" + seven + ", p8=" + eight + ", p9=" + nine + ", p10=" + ten);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput11Params() {
        logger.info("p1=" + one + ", p2=" + two + ", p3=" + three + ", p4=" + four + ", p5=" + five + ", p6=" + six + ", p7=" + seven + ", p8=" + eight + ", p9=" + nine + ", p10=" + ten + ", p11=" + eleven);
    }
}
