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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.logic.BlackHole;

/**
 * Tests the overhead of disabled logging.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// java -jar log4j-perf/target/microbenchmarks.jar ".*SimpleBenchmark.*" -f 1 -wi 5 -i 5
//
// Usage help:
// java -jar log4j-perf/target/microbenchmarks.jar -help
//
@State(Scope.Thread)
public class SimpleBenchmark {
    private static final String msg = "This is a test";
    private Logger logger;

    @Setup
    public void setup() {
        final Configuration config = ((LoggerContext) LogManager.getContext()).getConfiguration();
        if (!DefaultConfiguration.DEFAULT_NAME.equals(config.getName())) {
            System.out.println("Configuration was " + config.getName());
            ((LoggerContext) LogManager.getContext()).start(new DefaultConfiguration());
        }
        logger = LogManager.getLogger(SimpleBenchmark.class.getName());
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @GenerateMicroBenchmark
    public void testBaselineThroughput(final BlackHole bh) {
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @GenerateMicroBenchmark
    public void testIsDebugEnabledThroughput(final BlackHole bh) {
        bh.consume(logger.isDebugEnabled());
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @GenerateMicroBenchmark
    public void testIsEnabledLevelThroughput(final BlackHole bh) {
        bh.consume(logger.isEnabled(Level.DEBUG));
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @GenerateMicroBenchmark
    public void testDebugDisabledThroughput(final BlackHole bh) {
        logger.debug(msg);
    }


    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @GenerateMicroBenchmark
    public void testBaselineResponseTime(final BlackHole bh) {
    }

    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @GenerateMicroBenchmark
    public void testIsDebugEnabledResponseTime(final BlackHole bh) {
        bh.consume(logger.isDebugEnabled());
    }

    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @GenerateMicroBenchmark
    public void testIsEnabledLevelResponseTime(final BlackHole bh) {
        bh.consume(logger.isEnabled(Level.DEBUG));
    }

    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @GenerateMicroBenchmark
    public void testDebugDisabledResponseTime(final BlackHole bh) {
        logger.debug(msg);
    }
}
