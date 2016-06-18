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
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/**
 * Tests Log4j2 Async Loggers performance.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*AsyncLoggersLocationBenchmark.*" -f 1 -wi 10 -i 20
//
// multiple threads (for example, 4 threads):
// java -jar log4j-perf/target/benchmarks.jar ".*AsyncLoggersLocationBenchmark.*" -f 1 -wi 10 -i 20 -t 4 -si true
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Thread)
public class AsyncLoggersLocationBenchmark {

    Logger logger;

    @Setup(Level.Trial)
    public void up() {
        System.setProperty("log4j.configurationFile", "perf-WithoutAnyAppender-location.xml");
        System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
        System.setProperty("AsyncLogger.RingBufferSize", "262144");
        System.setProperty("AsyncLogger.WaitStrategy", "Yield");
        //System.setProperty("log4j2.enable.threadlocals", "true");
        //System.setProperty("log4j.format.msg.async", "true");

        logger = LogManager.getLogger(getClass());
        new File("perftest.log").delete();
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
