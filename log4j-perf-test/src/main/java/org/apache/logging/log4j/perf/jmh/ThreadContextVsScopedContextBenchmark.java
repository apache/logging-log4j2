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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.ScopedContext;
import org.apache.logging.log4j.ScopedContext.Instance;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.internal.map.StringArrayThreadContextMap;
import org.apache.logging.log4j.spi.CopyOnWriteOpenHashMapThreadContextMap;
import org.apache.logging.log4j.spi.DefaultThreadContextMap;
import org.apache.logging.log4j.spi.GarbageFreeOpenHashMapThreadContextMap;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Compares performance of ThreadContextMap implementations and ScopedContext
 */
// ============================== HOW TO RUN THIS TEST: ====================================
// (Quick build: mvn -DskipTests=true clean package -pl log4j-perf -am )
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*ThreadContextVsScopedContextBenchmark.*"
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class ThreadContextVsScopedContextBenchmark {
    private static final String DEFAULT_CONTEXT_MAP = "Default";
    private static final String STRING_ARRAY_MAP = "StringArray";
    private static final String COPY_OPENHASH_MAP = "CopyOpenHash";
    private static final String COPY_ARRAY_MAP = "CopySortedArray";
    private static final String NO_GC_OPENHASH_MAP = "NoGcOpenHash";
    private static final String NO_GC_ARRAY_MAP = "NoGcSortedArray";
    private static final Map<String, Class<? extends ThreadContextMap>> IMPLEMENTATIONS = new HashMap<>();

    static {
        IMPLEMENTATIONS.put(DEFAULT_CONTEXT_MAP, DefaultThreadContextMap.class);
        IMPLEMENTATIONS.put(STRING_ARRAY_MAP, StringArrayThreadContextMap.class);
        IMPLEMENTATIONS.put(COPY_OPENHASH_MAP, CopyOnWriteOpenHashMapThreadContextMap.class);
        IMPLEMENTATIONS.put(
                COPY_ARRAY_MAP,
                CopyOnWriteOpenHashMapThreadContextMap.SUPER); // CopyOnWriteSortedArrayThreadContextMap.class);
        IMPLEMENTATIONS.put(NO_GC_OPENHASH_MAP, GarbageFreeOpenHashMapThreadContextMap.class);
        IMPLEMENTATIONS.put(
                NO_GC_ARRAY_MAP,
                GarbageFreeOpenHashMapThreadContextMap.SUPER); // GarbageFreeSortedArrayThreadContextMap.class);
    }

    @Param({"Default", "CopyOpenHash", "CopySortedArray", "NoGcOpenHash", "NoGcSortedArray", "StringArray"})
    public String threadContextMapAlias;

    @Param({"16"})
    public int count;

    private final int KEY_LENGTH = 16;
    private String[] keys;
    private String[] values;

    @Setup
    public void setup() {
        System.setProperty(
                "log4j2.threadContextMap",
                IMPLEMENTATIONS.get(threadContextMapAlias).getName());
        keys = new String[count];
        values = new String[count];
        final Random r = new Random();
        for (int j = 0; j < keys.length; j++) {
            final char[] str = new char[KEY_LENGTH];
            for (int i = 0; i < str.length; i++) {
                str[i] = (char) r.nextInt();
            }
            keys[j] = new String(str);
            values[j] = new String(str);
        }
    }

    @Benchmark
    public void threadContextMap(final Blackhole blackhole) {
        for (int i = 0; i < count; i++) {
            ThreadContext.put(keys[i], values[i]);
        }
        blackhole.consume(values);
        ThreadContext.clearMap();
    }

    @Benchmark
    public void scopedContext(final Blackhole blackhole) {
        Instance instance = ScopedContext.where(keys[0], values[0]);
        for (int i = 1; i < count; i++) {
            instance.where(keys[i], values[i]);
        }
        instance.run(() -> blackhole.consume(values));
    }
}
