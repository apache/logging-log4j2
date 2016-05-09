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

import static org.apache.logging.log4j.perf.util.BenchmarkMessageParams.*;

/**
 * Tests JUL (java.util.logging) Memory Handler performance.
 * We explicitly specify the location so that JUL will not try to look up caller location information.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*MemoryHandlerJULBenchmark.*" -f 1 -wi 10 -i 20
//
// multiple threads (for example, 4 threads):
// java -jar log4j-perf/target/benchmarks.jar ".*MemoryHandlerJULBenchmark.*" -f 1 -wi 10 -i 20 -t 4 -si true
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Benchmark)
public class MemoryHandlerJULBenchmark {

    Logger logger;
    MemoryHandler memoryHandler;

    @Setup(Level.Trial)
    public void up() {
        memoryHandler = new MemoryHandler(new NoOpJULHandler(), 262144, java.util.logging.Level.SEVERE);
        logger = java.util.logging.Logger.getLogger(getClass().getName());
        logger.setUseParentHandlers(false);
        logger.addHandler(memoryHandler);
        logger.setLevel(java.util.logging.Level.ALL);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughputSimple() {
        logger.logp(java.util.logging.Level.INFO, getClass().getName(), "methodName", BenchmarkMessageParams.TEST);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput1Param() {
        logger.logp(java.util.logging.Level.INFO, getClass().getName(), "methodName", "p1={}", one);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput2Params() {
        logger.logp(java.util.logging.Level.INFO, getClass().getName(), "methodName", "p1={}, p2={}",
                new Object[]{one, two});
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput3Params() {
        logger.logp(java.util.logging.Level.INFO, getClass().getName(), "methodName","p1={}, p2={}, p3={}",
                new Object[]{one, two, three});
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput4Params() {
        logger.logp(java.util.logging.Level.INFO, getClass().getName(), "methodName",
                "p1={}, p2={}, p3={}, p4={}", new Object[]{one, two, three, four,});
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput5Params() {
        logger.logp(java.util.logging.Level.INFO, getClass().getName(), "methodName",
                "p1={}, p2={}, p3={}, p4={}, p5={}", new Object[]{one, two, three, four, five,});
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput6Params() {
        logger.logp(java.util.logging.Level.INFO, getClass().getName(), "methodName",
                "p1={}, p2={}, p3={}, p4={}, p5={}, p6={}",
                new Object[]{one, two, three, four, five, six,});
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput7Params() {
        logger.logp(java.util.logging.Level.INFO, getClass().getName(), "methodName",
                "p1={}, p2={}, p3={}, p4={}, p5={}, p6={}, p7={}",
                new Object[]{one, two, three, four, five, six, seven,});
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput8Params() {
        logger.logp(java.util.logging.Level.INFO, getClass().getName(), "methodName",
                "p1={}, p2={}, p3={}, p4={}, p5={}, p6={}, p7={}, p8={}",
                new Object[]{one, two, three, four, five, six, seven, eight,});
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput9Params() {
        logger.logp(java.util.logging.Level.INFO, getClass().getName(), "methodName",
                "p1={}, p2={}, p3={}, p4={}, p5={}, p6={}, p7={}, p8={}, p9={}",
                new Object[]{one, two, three, four, five, six, seven, eight, nine,});
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput10Params() {
        logger.logp(java.util.logging.Level.INFO, getClass().getName(), "methodName",
                "p1={}, p2={}, p3={}, p4={}, p5={}, p6={}, p7={}, p8={}, p9={}, p10={}",
                new Object[]{one, two, three, four, five, six, seven, eight, nine, ten});
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput11Params() {
        logger.logp(java.util.logging.Level.INFO, getClass().getName(), "methodName",
                "p1={}, p2={}, p3={}, p4={}, p5={}, p6={}, p7={}, p8={}, p9={}, p10={}, p11={}",
                new Object[]{one, two, three, four, five, six, seven, eight, nine, ten, eleven});
    }
}
