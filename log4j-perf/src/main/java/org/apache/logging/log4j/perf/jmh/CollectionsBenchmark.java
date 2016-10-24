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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

// ============================== HOW TO RUN THIS TEST: ====================================
//
// In sampling mode (latency test):
// java -jar log4j-perf/target/benchmarks.jar ".*CollectionsBenchmark.*" -i 10 -f 1 -wi 5 -bm sample -tu ns
//
// Multi-threading test:
// java -jar benchmarks.jar ".*CollectionsBenchmark.*"  -i 10 -f 1 -wi 5 -bm sample -tu ns -t 4
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Benchmark)
public class CollectionsBenchmark {
    private final ConcurrentHashMap<String, Long> map1 = new ConcurrentHashMap<>();
    private final CopyOnWriteArraySet<Long> arraySet1 = new CopyOnWriteArraySet<>();
    private final CopyOnWriteArrayList<Long> arrayList1 = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, Long> map3 = new ConcurrentHashMap<>();
    private final CopyOnWriteArraySet<Long> arraySet3 = new CopyOnWriteArraySet<>();
    private final CopyOnWriteArrayList<Long> arrayList3 = new CopyOnWriteArrayList<>();

    @Setup
    public void setup() {
        for (int i = 0; i < 3; i++) {
            map3.put(String.valueOf(i), Long.valueOf(i));
            arraySet3.add(Long.valueOf(i));
            arrayList3.add(Long.valueOf(i));
        }
        map1.put(String.valueOf(1), Long.valueOf(1));
        arraySet1.add(Long.valueOf(1));
        arrayList1.add(Long.valueOf(1));
    }

    @Benchmark
    public void testBaseline(final Blackhole bh) {
    }

    @Benchmark
    public long iterMap1Element() {
        long total = 0;
        for (final Long value : map1.values()) {
            total += value;
        }
        return total;
    }

    @Benchmark
    public long iterArraySet1Element() {
        long total = 0;
        for (final Long value : arraySet1) {
            total += value;
        }
        return total;
    }

    @Benchmark
    public long iterArrayList1Element() {
        long total = 0;
        for (final Long value : arrayList1) {
            total += value;
        }
        return total;
    }

    @Benchmark
    public long iterMap3Elements() {
        long total = 0;
        for (final Long value : map3.values()) {
            total += value;
        }
        return total;
    }

    @Benchmark
    public long iterArraySet3Element() {
        long total = 0;
        for (final Long value : arraySet3) {
            total += value;
        }
        return total;
    }

    @Benchmark
    public long iterArrayList3Element() {
        long total = 0;
        for (final Long value : arrayList3) {
            total += value;
        }
        return total;
    }
}
