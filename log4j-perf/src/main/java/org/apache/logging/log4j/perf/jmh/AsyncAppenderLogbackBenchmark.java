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
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.core.spi.LifeCycle;

/**
 * Tests Logback Async Appender performance.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*Async.*Benchmark.*" -f 1 -wi 5 -i 5
//
// multiple threads (for example, 4 threads):
// java -jar log4j-perf/target/benchmarks.jar ".*Async.*Benchmark.*" -f 1 -wi 5 -i 5 -t 4 -si true
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
public class AsyncAppenderLogbackBenchmark {

    final static char[] CHARS = new char[500];
    static {
        Arrays.fill(CHARS, 'a');
    }
    final static String TEST = new String(CHARS);

    @State(Scope.Benchmark)
    public static class NormalState {
        Logger logger;

        @Setup(Level.Trial)
        public void up() {
            System.setProperty("logback.configurationFile", "perf-logback-async.xml");
            logger = LoggerFactory.getLogger(getClass());
        }

        @TearDown(Level.Trial)
        public void down() {
            ((LifeCycle) LoggerFactory.getILoggerFactory()).stop();
            new File("perftest.log").delete();
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public boolean throughputBaseline(final NormalState e) {
        return e.logger.isInfoEnabled();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput(final NormalState e) {
        e.logger.info(TEST);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public boolean latencyBaseline(final NormalState e) {
        return e.logger.isInfoEnabled();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void latency(final NormalState e) {
        e.logger.info(TEST);
    }
}
