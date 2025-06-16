/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.perf.jmh;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * Tests performance of various DateFormatters in a thread-safe manner.
 */
@State(Scope.Benchmark)
public class ThreadsafeDateFormatBenchmark {

    private final Date date = new Date();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private final ThreadLocal<SimpleDateFormat> threadLocalSDFormat = new ThreadLocal<>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("HH:mm:ss.SSS");
        }
    };

    private final ThreadLocal<FormatterSimple> threadLocalCachedSDFormat = new ThreadLocal<>() {
        @Override
        protected FormatterSimple initialValue() {
            return new FormatterSimple();
        }
    };

    private final FastDateFormat fastDateFormat = FastDateFormat.getInstance("HH:mm:ss.SSS");
    private final FixedDateFormat fixedDateFormat = FixedDateFormat.createIfSupported("HH:mm:ss.SSS");
    private final FormatterFixedReuseBuffer formatFixedReuseBuffer = new FormatterFixedReuseBuffer();

    private class CachedTimeFastFormat {
        private final long timestamp;
        private final String formatted;

        public CachedTimeFastFormat(final long timestamp) {
            this.timestamp = timestamp;
            this.formatted = fastDateFormat.format(timestamp);
        }
    }

    private class CachedTimeFixedFmt {
        private final long timestamp;
        private final String formatted;

        public CachedTimeFixedFmt(final long timestamp) {
            this.timestamp = timestamp;
            this.formatted = fixedDateFormat.format(timestamp);
        }
    }

    private static class FormatterSimple {
        private final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
        private long timestamp;
        private String formatted;

        public FormatterSimple() {
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

    private static class FormatterFixedReuseBuffer {
        private final FixedDateFormat customFormat = FixedDateFormat.createIfSupported("HH:mm:ss.SSS");
        private long timestamp;
        private String formatted;
        private final ThreadLocal<char[]> reusableBuffer = new ThreadLocal<>() {
            @Override
            protected char[] initialValue() {
                return new char[255];
            }
        };

        public FormatterFixedReuseBuffer() {
            this.timestamp = 0;
        }

        public String format(final long timestamp) {
            if (timestamp != this.timestamp) {
                this.timestamp = timestamp;
                final char[] buffer = reusableBuffer.get();
                final int len = customFormat.format(timestamp, buffer, 0);
                formatted = new String(buffer, 0, len);
            }
            return formatted;
        }
    }

    private final long currentTimestamp = 0;
    private String cachedTime = null;

    private final AtomicReference<CachedTimeFastFormat> cachedTimeFastFmt =
            new AtomicReference<>(new CachedTimeFastFormat(System.currentTimeMillis()));
    private final AtomicReference<CachedTimeFixedFmt> cachedTimeFixedFmt =
            new AtomicReference<>(new CachedTimeFixedFmt(System.currentTimeMillis()));

    public static void main(final String[] args) {}

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void baseline() {}

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
    public String dateTimeFormatter() {
        final LocalDateTime now = LocalDateTime.now();
        return dateTimeFormatter.format(now);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String threadLocalSimpleDateFmt() {
        final long timestamp = System.currentTimeMillis();
        return threadLocalSDFormat.get().format(timestamp);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String cachedThrdLocalSimpleDateFmt() {
        final long timestamp = System.currentTimeMillis();
        return threadLocalCachedSDFormat.get().format(timestamp);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String cachedThrdLocalCustomFormat() {
        final long timestamp = System.currentTimeMillis();
        return formatFixedReuseBuffer.format(timestamp);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String fastDateFormat() {
        return fastDateFormat.format(System.currentTimeMillis());
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String fixedDateFormat() {
        return fixedDateFormat.format(System.currentTimeMillis());
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String atomicFastFormat() {
        final long timestamp = System.currentTimeMillis();
        final CachedTimeFastFormat current = cachedTimeFastFmt.get();
        if (timestamp != current.timestamp) {
            final CachedTimeFastFormat newTime = new CachedTimeFastFormat(timestamp);
            if (cachedTimeFastFmt.compareAndSet(current, newTime)) {
                return newTime.formatted;
            }
            return cachedTimeFastFmt.get().formatted;
        }
        return current.formatted;
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String atomicFixedFormat() {
        final long timestamp = System.currentTimeMillis();
        final CachedTimeFixedFmt current = cachedTimeFixedFmt.get();
        if (timestamp != current.timestamp) {
            final CachedTimeFixedFmt newTime = new CachedTimeFixedFmt(timestamp);
            if (cachedTimeFixedFmt.compareAndSet(current, newTime)) {
                return newTime.formatted;
            }
            return cachedTimeFixedFmt.get().formatted;
        }
        return current.formatted;
    }
}
