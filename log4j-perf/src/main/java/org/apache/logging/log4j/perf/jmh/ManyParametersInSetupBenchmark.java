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
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Benchmarks Parameterized logging after initial setup has used parameter
 * arrays significantly larger than the benchmark itself.
 */
// HOW TO RUN THIS TEST
// java -jar target/benchmarks.jar ".*ManyParametersInSetupBenchmark.*" -f 1 -i 10 -wi 20 -bm sample -tu ns
@State(Scope.Thread)
@Threads(1)
@Fork(1)
@Warmup(time = 3, iterations = 3)
@Measurement(time = 5, iterations = 3)
public class ManyParametersInSetupBenchmark {
    Logger logger;

    @Setup
    public void setUp() {
        System.setProperty("log4j.configurationFile", "log4j2-ManyParametersBenchmark.xml");
        logger = LogManager.getLogger(getClass());
        // Provide very large parameter buffers to MutableLogEvent and MutableParameterizedMessage.
        // This should not cause degraded performance in the "fewParameters" benchmark.
        logger.error("Parameterized {} {} {}", new Object[1_000_000]);
        logger.error("Second Parameterized {} {} {}", new Object[1_000_000]);
    }

    @TearDown
    public void tearDown() {
        System.clearProperty("log4j.configurationFile");
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void fewParameters() {
        logger.error("Hello {}", "World");
    }

}
