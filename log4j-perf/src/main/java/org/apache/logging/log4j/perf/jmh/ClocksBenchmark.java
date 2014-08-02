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

import org.apache.logging.log4j.core.util.CachedClock;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.CoarseCachedClock;
import org.apache.logging.log4j.core.util.SystemClock;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * Tests performance of various clock implementation.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// java -jar log4j-perf/target/microbenchmarks.jar ".*Clocks.*" -f 1 -wi 5 -i 5
//
// multiple threads (for example, 4 threads):
// java -jar log4j-perf/target/microbenchmarks.jar ".*Clocks.*" -f 1 -wi 5 -i 5 -t 4 -si true
//
// Usage help:
// java -jar log4j-perf/target/microbenchmarks.jar -help
//
@State(Scope.Thread)
public class ClocksBenchmark {

    Clock systemClock = new SystemClock();
    Clock cachedClock;
    Clock coarseCachedClock;
    Clock fixedClock;
    Clock fixedFinalClock;

    @Setup(Level.Trial)
    public void up() {
        cachedClock = CachedClock.instance();
        coarseCachedClock = CoarseCachedClock.instance();
        fixedClock = new FixedTimeClock(System.nanoTime());
        fixedFinalClock = new FixedFinalTimeClock(System.nanoTime());
    }

    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void baseline() {
    }

    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long langSystemCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long systemClock() {
        return systemClock.currentTimeMillis();
    }

    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long cachedClock() {
        return cachedClock.currentTimeMillis();
    }

    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long coarseCachedClock() {
        return coarseCachedClock.currentTimeMillis();
    }

    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long fixedClock() {
        return fixedClock.currentTimeMillis();
    }

    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long fixedFinalClock() {
        return fixedFinalClock.currentTimeMillis();
    }

    private static final class FixedTimeClock implements Clock {
        private long fixedTime;

        public FixedTimeClock(long fixedTime) {
            this.fixedTime = fixedTime;
        }

        @Override
        public long currentTimeMillis() {
            return fixedTime;
        }
    }

    private static final class FixedFinalTimeClock implements Clock {
        private final long fixedFinalTime;

        public FixedFinalTimeClock(long fixedTime) {
            this.fixedFinalTime = fixedTime;
        }

        @Override
        public long currentTimeMillis() {
            return fixedFinalTime;
        }
    }
}
