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

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * Tests performance of various time format implementation.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*TimeFormat.*" -f 1 -wi 5 -i 10
//
// multiple threads (for example, 4 threads):
// java -jar log4j-perf/target/benchmarks.jar ".*TimeFormat.*" -f 1 -wi 5 -i 5 -t 4 -si true
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Benchmark)
public class TimeFormatBenchmark {

    ThreadLocal<SimpleDateFormat> threadLocalSimpleDateFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("HH:mm:ss.SSS");
        }
    };
    FastDateFormat fastDateFormat = FastDateFormat.getInstance("HH:mm:ss.SSS");
    FixedDateFormat fixedDateFormat = FixedDateFormat.createIfSupported(new String[]{"ABSOLUTE"});
    volatile long midnightToday = 0;
    volatile long midnightTomorrow = 0;

    @State(Scope.Thread)
    public static class BufferState {
        final ByteBuffer buffer = ByteBuffer.allocate(12);
        final StringBuilder stringBuilder = new StringBuilder(12);
        final char[] charArray = new char[12];
    }

    private long millisSinceMidnight(final long now) {
        if (now >= midnightTomorrow) {
            midnightToday = calcMidnightMillis(now, 0);
            midnightTomorrow = calcMidnightMillis(now, 1);
        }
        return now - midnightToday;
    }

    private long calcMidnightMillis(final long time, final int addDays) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, addDays);
        return cal.getTimeInMillis();
    }

    public static void main(final String[] args) {
        System.out.println(new TimeFormatBenchmark().fixedBitFiddlingReuseCharArray(new BufferState()));
        System.out.println(new TimeFormatBenchmark().fixedFormatReuseStringBuilder(new BufferState()));
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String simpleDateFormat() {
        return threadLocalSimpleDateFormat.get().format(new Date());
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String fastDateFormatCreateNewStringBuilder() {
        return fastDateFormat.format(new Date());
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String fastDateFormatReuseStringBuilder(final BufferState state) {
        state.stringBuilder.setLength(0);
        fastDateFormat.format(new Date(), state.stringBuilder);
        return new String(state.stringBuilder);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String fixedBitFiddlingReuseCharArray(final BufferState state) {
        final int len = formatCharArrayBitFiddling(System.currentTimeMillis(), state.charArray, 0);
        return new String(state.charArray, 0, len);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String fixedDateFormatCreateNewCharArray(final BufferState state) {
        return fixedDateFormat.format(System.currentTimeMillis());
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String fixedDateFormatReuseCharArray(final BufferState state) {
        final int len = fixedDateFormat.format(System.currentTimeMillis(), state.charArray, 0);
        return new String(state.charArray, 0, len);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String fixedFormatReuseStringBuilder(final BufferState state) {
        state.stringBuilder.setLength(0);
        formatStringBuilder(System.currentTimeMillis(), state.stringBuilder);
        return new String(state.stringBuilder);
    }

    int formatCharArrayBitFiddling(final long time, final char[] buffer, int pos) {
        // Calculate values by getting the ms values first and do then
        // shave off the hour minute and second values with multiplications
        // and bit shifts instead of simple but expensive divisions.

        // Get daytime in ms which does fit into an int
        // int ms = (int) (time % 86400000);
        int ms = (int) (millisSinceMidnight(time));

        // well ... it works
        final int hour = (int) (((ms >> 7) * 9773437L) >> 38);
        ms -= 3600000 * hour;

        final int minute = (int) (((ms >> 5) * 2290650L) >> 32);
        ms -= 60000 * minute;

        final int second = ((ms >> 3) * 67109) >> 23;
        ms -= 1000 * second;

        // Hour
        // 13/128 is nearly the same as /10 for values up to 65
        int temp = (hour * 13) >> 7;
        buffer[pos++] = ((char) (temp + '0'));

        // Do subtract to get remainder instead of doing % 10
        buffer[pos++] = ((char) (hour - 10 * temp + '0'));
        buffer[pos++] = ((char) ':');

        // Minute
        // 13/128 is nearly the same as /10 for values up to 65
        temp = (minute * 13) >> 7;
        buffer[pos++] = ((char) (temp + '0'));

        // Do subtract to get remainder instead of doing % 10
        buffer[pos++] = ((char) (minute - 10 * temp + '0'));
        buffer[pos++] = ((char) ':');

        // Second
        // 13/128 is nearly the same as /10 for values up to 65
        temp = (second * 13) >> 7;
        buffer[pos++] = ((char) (temp + '0'));
        buffer[pos++] = ((char) (second - 10 * temp + '0'));
        buffer[pos++] = ((char) '.');

        // Millisecond
        // 41/4096 is nearly the same as /100
        temp = (ms * 41) >> 12;
        buffer[pos++] = ((char) (temp + '0'));

        ms -= 100 * temp;
        temp = (ms * 205) >> 11; // 205/2048 is nearly the same as /10
        buffer[pos++] = ((char) (temp + '0'));

        ms -= 10 * temp;
        buffer[pos++] = ((char) (ms + '0'));
        return pos;
    }

    StringBuilder formatStringBuilder(final long time, final StringBuilder buffer) {
        // Calculate values by getting the ms values first and do then
        // calculate the hour minute and second values divisions.

        // Get daytime in ms which does fit into an int
        // int ms = (int) (time % 86400000);
        int ms = (int) (millisSinceMidnight(time));

        final int hours = ms / 3600000;
        ms -= 3600000 * hours;

        final int minutes = ms / 60000;
        ms -= 60000 * minutes;

        final int seconds = ms / 1000;
        ms -= 1000 * seconds;

        // Hour
        int temp = hours / 10;
        buffer.append((char) (temp + '0'));

        // Do subtract to get remainder instead of doing % 10
        buffer.append((char) (hours - 10 * temp + '0'));
        buffer.append((char) ':');

        // Minute
        temp = minutes / 10;
        buffer.append((char) (temp + '0'));

        // Do subtract to get remainder instead of doing % 10
        buffer.append((char) (minutes - 10 * temp + '0'));
        buffer.append((char) ':');

        // Second
        temp = seconds / 10;
        buffer.append((char) (temp + '0'));
        buffer.append((char) (seconds - 10 * temp + '0'));
        buffer.append((char) '.');

        // Millisecond
        temp = ms / 100;
        buffer.append((char) (temp + '0'));

        ms -= 100 * temp;
        temp = ms / 10;
        buffer.append((char) (temp + '0'));

        ms -= 10 * temp;
        buffer.append((char) (ms + '0'));
        return buffer;
    }

    int formatCharArray(final long time, final char[] buffer, int pos) {
        // Calculate values by getting the ms values first and do then
        // calculate the hour minute and second values divisions.

        // Get daytime in ms which does fit into an int
        // int ms = (int) (time % 86400000);
        int ms = (int) (millisSinceMidnight(time));

        final int hours = ms / 3600000;
        ms -= 3600000 * hours;

        final int minutes = ms / 60000;
        ms -= 60000 * minutes;

        final int seconds = ms / 1000;
        ms -= 1000 * seconds;

        // Hour
        int temp = hours / 10;
        buffer[pos++] = ((char) (temp + '0'));

        // Do subtract to get remainder instead of doing % 10
        buffer[pos++] = ((char) (hours - 10 * temp + '0'));
        buffer[pos++] = ((char) ':');

        // Minute
        temp = minutes / 10;
        buffer[pos++] = ((char) (temp + '0'));

        // Do subtract to get remainder instead of doing % 10
        buffer[pos++] = ((char) (minutes - 10 * temp + '0'));
        buffer[pos++] = ((char) ':');

        // Second
        temp = seconds / 10;
        buffer[pos++] = ((char) (temp + '0'));
        buffer[pos++] = ((char) (seconds - 10 * temp + '0'));
        buffer[pos++] = ((char) '.');

        // Millisecond
        temp = ms / 100;
        buffer[pos++] = ((char) (temp + '0'));

        ms -= 100 * temp;
        temp = ms / 10;
        buffer[pos++] = ((char) (temp + '0'));

        ms -= 10 * temp;
        buffer[pos++] = ((char) (ms + '0'));
        return pos;
    }
}
