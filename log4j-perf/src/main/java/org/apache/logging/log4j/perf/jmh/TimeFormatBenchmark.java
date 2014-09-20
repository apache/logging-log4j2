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

import org.apache.logging.log4j.core.util.Charsets;
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
// java -jar log4j-perf/target/benchmarks.jar ".*TimeFormat.*" -f 1 -wi 5 -i 5
//
// multiple threads (for example, 4 threads):
// java -jar log4j-perf/target/benchmarks.jar ".*TimeFormat.*" -f 1 -wi 5 -i 5 -t 4 -si true
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Thread)
public class TimeFormatBenchmark {

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    long midnightToday = 0;
    long midnightTomorrow = 0;

    @State(Scope.Thread)
    public static class BufferState {
        ByteBuffer buffer = ByteBuffer.allocate(12);
    }

    private long millisSinceMidnight(long now) {
        if (now >= midnightTomorrow) {
            midnightToday = calcMidnightMillis(0);
            midnightTomorrow = calcMidnightMillis(1);
        }
        return now - midnightToday;
    }

    private long calcMidnightMillis(int addDays) {
        // Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UCT"));
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, addDays);
        return cal.getTimeInMillis();
    }

    public static void main(String[] args) {
        System.out.println(new TimeFormatBenchmark().customFastFormatString(new BufferState()));
        System.out.println(new TimeFormatBenchmark().customFormatString(new BufferState()));
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void baseline() {
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String simpleDateFormatString() {
        return simpleDateFormat.format(new Date());
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int simpleDateFormatBytes(BufferState state) {
        String str = simpleDateFormat.format(new Date());
        byte[] bytes = str.getBytes(Charsets.UTF_8);
        state.buffer.clear();
        state.buffer.put(bytes);
        return state.buffer.position();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String customFastFormatString(BufferState state) {
        state.buffer.clear();
        fastFormat(System.currentTimeMillis(), state.buffer);
        return new String(state.buffer.array(), 0, state.buffer.position(), Charsets.UTF_8);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int customFastFormatBytes(BufferState state) {
        state.buffer.clear();
        fastFormat(System.currentTimeMillis(), state.buffer);
        return state.buffer.position();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String customFormatString(BufferState state) {
        state.buffer.clear();
        format(System.currentTimeMillis(), state.buffer);
        return new String(state.buffer.array(), 0, state.buffer.position(), Charsets.UTF_8);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int customFormatBytes(BufferState state) {
        state.buffer.clear();
        format(System.currentTimeMillis(), state.buffer);
        return state.buffer.position();
    }

    public ByteBuffer fastFormat(long time, ByteBuffer buffer) {
        // Calculate values by getting the ms values first and do then
        // shave off the hour minute and second values with multiplications
        // and bit shifts instead of simple but expensive divisions.

        // Get daytime in ms which does fit into an int
        // int ms = (int) (time % 86400000);
        int ms = (int) (millisSinceMidnight(time));

        // well ... it works
        int hour = (int) (((ms >> 7) * 9773437L) >> 38);
        ms -= 3600000 * hour;

        int minute = (int) (((ms >> 5) * 2290650L) >> 32);
        ms -= 60000 * minute;

        int second = ((ms >> 3) * 67109) >> 23;
        ms -= 1000 * second;

        // Hour
        // 13/128 is nearly the same as /10 for values up to 65
        int temp = (hour * 13) >> 7;
        buffer.put((byte) (temp + '0'));

        // Do subtract to get remainder instead of doing % 10
        buffer.put((byte) (hour - 10 * temp + '0'));
        buffer.put((byte) ':');

        // Minute
        // 13/128 is nearly the same as /10 for values up to 65
        temp = (minute * 13) >> 7;
        buffer.put((byte) (temp + '0'));

        // Do subtract to get remainder instead of doing % 10
        buffer.put((byte) (minute - 10 * temp + '0'));
        buffer.put((byte) ':');

        // Second
        // 13/128 is nearly the same as /10 for values up to 65
        temp = (second * 13) >> 7;
        buffer.put((byte) (temp + '0'));
        buffer.put((byte) (second - 10 * temp + '0'));
        buffer.put((byte) '.');

        // Millisecond
        // 41/4096 is nearly the same as /100
        temp = (ms * 41) >> 12;
        buffer.put((byte) (temp + '0'));

        ms -= 100 * temp;
        temp = (ms * 205) >> 11; // 205/2048 is nearly the same as /10
        buffer.put((byte) (temp + '0'));

        ms -= 10 * temp;
        buffer.put((byte) (ms + '0'));
        return buffer;
    }

    public ByteBuffer format(long time, ByteBuffer buffer) {
        // Calculate values by getting the ms values first and do then
        // calculate the hour minute and second values divisions.

        // Get daytime in ms which does fit into an int
        // int ms = (int) (time % 86400000);
        int ms = (int) (millisSinceMidnight(time));

        int hours = ms / 3600000;
        ms -= 3600000 * hours;

        int minutes = ms / 60000;
        ms -= 60000 * minutes;

        int seconds = ms / 1000;
        ms -= 1000 * seconds;

        // Hour
        int temp = hours / 10;
        buffer.put((byte) (temp + '0'));

        // Do subtract to get remainder instead of doing % 10
        buffer.put((byte) (hours - 10 * temp + '0'));
        buffer.put((byte) ':');

        // Minute
        temp = minutes / 10;
        buffer.put((byte) (temp + '0'));

        // Do subtract to get remainder instead of doing % 10
        buffer.put((byte) (minutes - 10 * temp + '0'));
        buffer.put((byte) ':');

        // Second
        temp = seconds / 10;
        buffer.put((byte) (temp + '0'));
        buffer.put((byte) (seconds - 10 * temp + '0'));
        buffer.put((byte) '.');

        // Millisecond
        temp = ms / 100;
        buffer.put((byte) (temp + '0'));

        ms -= 100 * temp;
        temp = ms / 10;
        buffer.put((byte) (temp + '0'));

        ms -= 10 * temp;
        buffer.put((byte) (ms + '0'));
        return buffer;
    }
}
