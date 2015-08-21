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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.core.util.datetime.CustomTimeFormat;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * Tests performance of various DateFormatters in a thread-safe manner.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*ThreadsafeDateFormat.*" -f 1 -wi 5 -i 10
//
// multiple threads (for example, 4 threads):
// java -jar log4j-perf/target/benchmarks.jar ".*ThreadsafeDateFormat.*" -f 1 -wi 5 -i 10 -t 4 -si true
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Benchmark)
public class ThreadsafeDateFormatBenchmark {

    private final Date date = new Date();
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private final ThreadLocal<SimpleDateFormat> threadLocal = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("HH:mm:ss.SSS");
        }
    };

    private final ThreadLocal<Formatter> localFormat = new ThreadLocal<Formatter>() {
        @Override
        protected Formatter initialValue() {
            return new Formatter();
        }
    };

    private final FastDateFormat fastFormat = FastDateFormat.getInstance("HH:mm:ss.SSS");
    private final CustomTimeFormat customFormat = CustomTimeFormat.createIfSupported("HH:mm:ss.SSS");
    private final CustomFormatReuseBuffer customFormatReuseBuffer = new CustomFormatReuseBuffer();
    
    private class CurrentTime {
        private final long timestamp;
        private final String formatted;

        public CurrentTime(final long timestamp) {
            this.timestamp = timestamp;
            this.formatted = fastFormat.format(timestamp);
        }
    }

    private class CustomCurrentTime {
        private final long timestamp;
        private final String formatted;

        public CustomCurrentTime(final long timestamp) {
            this.timestamp = timestamp;
            this.formatted = customFormat.format(timestamp);
        }
    }

    private class Formatter {
        private final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
        private long timestamp;
        private String formatted;

        public Formatter() {
            this.timestamp = 0;
        }

        public String format(final long timestamp) {
            if (timestamp != this.timestamp) {
                this.timestamp = timestamp;
                formatted = format.format(timestamp);
            }
            return formatted;
        }
    }

    private class CustomFormatReuseBuffer {
        private final CustomTimeFormat customFormat = CustomTimeFormat.createIfSupported("HH:mm:ss.SSS");
        private long timestamp;
        private String formatted;
        private final ThreadLocal<char[]> reusableBuffer = new ThreadLocal<char[]>() {
            @Override
            protected char[] initialValue() {
                return new char[255];
            }
        };

        public CustomFormatReuseBuffer() {
            this.timestamp = 0;
        }

        public String format(final long timestamp) {
            if (timestamp != this.timestamp) {
                this.timestamp = timestamp;
                char[] buffer = reusableBuffer.get();
                int len = customFormat.format(timestamp, buffer, 0);
                formatted = new String(buffer, 0, len);
            }
            return formatted;
        }
    }

    private final long currentTimestamp = 0;
    private String cachedTime = null;

    private final AtomicReference<CurrentTime> currentTime = new AtomicReference<>(new CurrentTime(System.currentTimeMillis()));
    private final AtomicReference<CustomCurrentTime> customCurrentTime = new AtomicReference<>(new CustomCurrentTime(System.currentTimeMillis()));

    public static void main(final String[] args) {
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void baseline() {
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String synchronizedSimpleDateFmt() {
        final long timestamp = System.currentTimeMillis();
        synchronized (simpleDateFormat) {
            if (timestamp != currentTimestamp) {
                cachedTime = simpleDateFormat.format(date);
            }
            return cachedTime;
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String threadLocalSimpleDateFmt() {
        final long timestamp = System.currentTimeMillis();
        return threadLocal.get().format(timestamp);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String cachedThrdLocalSimpleDateFmt() {
        final long timestamp = System.currentTimeMillis();
        return localFormat.get().format(timestamp);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String cachedThrdLocalCustomFormat() {
        final long timestamp = System.currentTimeMillis();
        return customFormatReuseBuffer.format(timestamp);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String fastFormat() {
        return fastFormat.format(System.currentTimeMillis());
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String customFormat() {
        return customFormat.format(System.currentTimeMillis());
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String atomicFastFormat() {
        final long timestamp = System.currentTimeMillis();
        final CurrentTime current = currentTime.get();
        if (timestamp != current.timestamp) {
            final CurrentTime newTime = new CurrentTime(timestamp);
            if (currentTime.compareAndSet(current, newTime)) {
                return newTime.formatted;
            } else {
                return currentTime.get().formatted;
            }

        }
        return current.formatted;
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String atomicCustomFormat() {
        final long timestamp = System.currentTimeMillis();
        final CustomCurrentTime current = customCurrentTime.get();
        if (timestamp != current.timestamp) {
            final CustomCurrentTime newTime = new CustomCurrentTime(timestamp);
            if (customCurrentTime.compareAndSet(current, newTime)) {
                return newTime.formatted;
            } else {
                return customCurrentTime.get().formatted;
            }

        }
        return current.formatted;
    }
}
