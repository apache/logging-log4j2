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
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.appender.MemoryMappedFileManager;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Used to choose approach for {@link MemoryMappedFileManager.Region#copyDataToBuffer(int, byte[], int, int)}.
 */
@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ByteArrayToByteBufferCopyBenchmark {

    public static final int SIZE = 1000;
    private static final byte[] array = new byte[SIZE];
    private static final ByteBuffer bb = ByteBuffer.allocate(SIZE).order(ByteOrder.nativeOrder());

    @Benchmark
    public int copyByteByByte() {
        byte b = 0;
        for (int i = 0; i < SIZE; i++) {
            bb.put(i, b = array[i]);
        }
        return b;
    }

    @Benchmark
    public int copyLongs() {
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            return copyLongsLittleEngian();
        } else {
            return copyLongsBigEngian();
        }
    }

    public int copyLongsLittleEngian() {
        int i = 0;
        for (; i < SIZE - 7; i += 8) {
            long l = array[i] | (array[i + 1] << 8) | (array[i + 2] << 16) | (array[i + 3] << 24) |
                    (((long) array[i + 4]) << 32) | (((long) array[i + 5]) << 40) | (((long) array[i + 6]) << 48) |
                    (((long) array[i + 7]) << 54);
            bb.putLong(i, l);
        }
        byte b = 0;
        for (; i < SIZE; i++) {
            bb.put(i, b = array[i]);
        }
        return b;
    }

    public int copyLongsBigEngian() {
        int i = 0;
        for (; i < SIZE - 7; i += 8) {
            long l = array[i + 7] | (array[i + 6] << 8) | (array[i + 5] << 16) | (array[i + 4] << 24) |
                    (((long) array[i + 3]) << 32) | (((long) array[i + 2]) << 40) | (((long) array[i + 1]) << 48) |
                    (((long) array[i]) << 54);
            bb.putLong(i, l);
        }
        byte b = 0;
        for (; i < SIZE; i++) {
            bb.put(i, b = array[i]);
        }
        return b;
    }
}
