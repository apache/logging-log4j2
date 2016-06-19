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

import static org.apache.logging.log4j.perf.util.BenchmarkMessageParams.eight;
import static org.apache.logging.log4j.perf.util.BenchmarkMessageParams.eleven;
import static org.apache.logging.log4j.perf.util.BenchmarkMessageParams.five;
import static org.apache.logging.log4j.perf.util.BenchmarkMessageParams.four;
import static org.apache.logging.log4j.perf.util.BenchmarkMessageParams.nine;
import static org.apache.logging.log4j.perf.util.BenchmarkMessageParams.one;
import static org.apache.logging.log4j.perf.util.BenchmarkMessageParams.seven;
import static org.apache.logging.log4j.perf.util.BenchmarkMessageParams.six;
import static org.apache.logging.log4j.perf.util.BenchmarkMessageParams.ten;
import static org.apache.logging.log4j.perf.util.BenchmarkMessageParams.three;
import static org.apache.logging.log4j.perf.util.BenchmarkMessageParams.two;

/**
 * Tests Log4j2 Async Appender performance.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*AsyncAppenderLog4j2Benchmark.*"
//
// multiple threads (for example, 4 threads):
// java -jar log4j-perf/target/benchmarks.jar ".*AsyncAppenderLog4j2Benchmark.*" -t 4 -si true
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 10)
@Measurement(iterations = 20)
@Threads(1)
public class AsyncAppenderLog4j2Benchmark {
    Logger logger;

    @Param({
        "perf5AsyncApndNoLoc-noOpAppender.xml",
        "perf5AsyncApndDsrptrNoLoc-noOpAppender.xml",
        "perf5AsyncApndMpscQNoLoc-noOpAppender.xml",
        "perf5AsyncApndXferQNoLoc-noOpAppender.xml"
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

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput1Param() {
        logger.info("p1={}", one);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput2Params() {
        logger.info("p1={}, p2={}", one, two);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput3Params() {
        logger.info("p1={}, p2={}, p3={}", one, two, three);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput4Params() {
        logger.info("p1={}, p2={}, p3={}, p4={}", one, two, three, four);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput5Params() {
        logger.info("p1={}, p2={}, p3={}, p4={}, p5={}", one, two, three, four, five);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput6Params() {
        logger.info("p1={}, p2={}, p3={}, p4={}, p5={}, p6={}", one, two, three, four, five, six);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput7Params() {
        logger.info("p1={}, p2={}, p3={}, p4={}, p5={}, p6={}, p7={}", one, two, three, four, five, six, seven);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput8Params() {
        logger.info("p1={}, p2={}, p3={}, p4={}, p5={}, p6={}, p7={}, p8={}", one, two, three, four, five, six, seven,
                eight);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput9Params() {
        logger.info("p1={}, p2={}, p3={}, p4={}, p5={}, p6={}, p7={}, p8={}, p9={}", one, two, three, four, five, six,
                seven, eight, nine);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput10Params() {
        logger.info("p1={}, p2={}, p3={}, p4={}, p5={}, p6={}, p7={}, p8={}, p9={}, p10={}", one, two, three, four,
                five, six, seven, eight, nine, ten);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput11Params() {
        logger.info("p1={}, p2={}, p3={}, p4={}, p5={}, p6={}, p7={}, p8={}, p9={}, p10={}, p11={}", one, two, three,
                four, five, six, seven, eight, nine, ten, eleven);
    }
}
