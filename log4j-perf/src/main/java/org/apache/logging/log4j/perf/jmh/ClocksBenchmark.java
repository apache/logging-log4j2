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
import java.util.concurrent.locks.LockSupport;

import org.apache.logging.log4j.core.util.CachedClock;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.CoarseCachedClock;
import org.apache.logging.log4j.core.util.SystemClock;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
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
// java -jar log4j-perf/target/benchmarks.jar ".*Clocks.*" -f 1 -wi 5 -i 5
//
// multiple threads (for example, 4 threads):
// java -jar log4j-perf/target/benchmarks.jar ".*Clocks.*" -f 1 -wi 5 -i 5 -t 4 -si true
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Thread)
public class ClocksBenchmark {

    Clock systemClock = new SystemClock();
    Clock cachedClock;
    Clock oldCachedClock;
    Clock coarseCachedClock;
    Clock fixedClock;
    Clock fixedFinalClock;

    @Setup(Level.Trial)
    public void up() {
        cachedClock = CachedClock.instance();
        oldCachedClock = OldCachedClock.instance();
        coarseCachedClock = CoarseCachedClock.instance();
        fixedClock = new FixedTimeClock(System.nanoTime());
        fixedFinalClock = new FixedFinalTimeClock(System.nanoTime());
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void baseline() {
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long systemCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long systemClock() {
        return systemClock.currentTimeMillis();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long cachedClock() {
        return cachedClock.currentTimeMillis();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long oldCachedClock() {
        return oldCachedClock.currentTimeMillis();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long coarseCachedClock() {
        return coarseCachedClock.currentTimeMillis();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long fixedClock() {
        return fixedClock.currentTimeMillis();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long fixedFinalClock() {
        return fixedFinalClock.currentTimeMillis();
    }

    private static final class FixedTimeClock implements Clock {
        private final long fixedTime;

        public FixedTimeClock(final long fixedTime) {
            this.fixedTime = fixedTime;
        }

        @Override
        public long currentTimeMillis() {
            return fixedTime;
        }
    }

    private static final class FixedFinalTimeClock implements Clock {
        private final long fixedFinalTime;

        public FixedFinalTimeClock(final long fixedTime) {
            this.fixedFinalTime = fixedTime;
        }

        @Override
        public long currentTimeMillis() {
            return fixedFinalTime;
        }
    }

    private static final class OldCachedClock implements Clock {
        private static final int UPDATE_THRESHOLD = 0x3FF;
        private static volatile OldCachedClock instance;
        private static final Object INSTANCE_LOCK = new Object();
        private volatile long millis = System.currentTimeMillis();
        private volatile short count = 0;

        private OldCachedClock() {
            final Thread updater = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        final long time = System.currentTimeMillis();
                        millis = time;

                        // avoid explicit dependency on sun.misc.Util
                        LockSupport.parkNanos(1000 * 1000);
                    }
                }
            }, "Clock Updater Thread");
            updater.setDaemon(true);
            updater.start();
        }

        public static OldCachedClock instance() {
            // LOG4J2-819: use lazy initialization of threads
            if (instance == null) {
                synchronized (INSTANCE_LOCK) {
                    if (instance == null) {
                        instance = new OldCachedClock();
                    }
                }
            }
            return instance;
        }

        @Override
        public long currentTimeMillis() {

            // improve granularity: also update time field every 1024 calls.
            // (the bit fiddling means we don't need to worry about overflows)
            if ((++count & UPDATE_THRESHOLD) == UPDATE_THRESHOLD) {
                millis = System.currentTimeMillis();
            }
            return millis;
        }
    }
}
