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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.perf.nogc.OpenHashStringMap;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.TriConsumer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Compares performance of SortedArrayStringMap vs. OpenHashMap vs. JDK HashMap.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
// (Quick build: mvn -DskipTests=true clean package -pl log4j-perf -am )
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*SortedArrayVsHashMapBenchmark.*" -f 1 -wi 10 -i 20 -tu ns -bm sample
//
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class SortedArrayVsHashMapBenchmark {

    //@Param({"1", "2", "5", "11", "23", "47", "95", "191", "383"})
    //@Param({"1", "5", "50", "500"})
    @Param({ "5",  "500"})
    public int count;

    //@Param({"5", "50"})
    @Param({"20"})
    public int length;

    private String[] keys;
    private static Object value = new Object();
    private HashMap<String, Object> map;
    private SortedArrayStringMap sortedStringArrayMap;
    private OpenHashStringMap<String, Object> openHashMapContextData;

    private HashMap<String, Object> populatedMap;
    private SortedArrayStringMap populatedSortedStringArrayMap;
    private OpenHashStringMap<String, Object> populatedOpenHashContextData;

    @Setup
    public void setup() {
        openHashMapContextData = new OpenHashStringMap<>();
        sortedStringArrayMap = new SortedArrayStringMap();
        map = new HashMap<>();

        keys = new String[count];
        final Random r = new Random();
        for (int j = 0; j < keys.length; j++) {
            final char[] str = new char[length];
            for (int i = 0; i < str.length; i++) {
                str[i] = (char) r.nextInt();
            }
            keys[j] = new String(str);
        }

        populatedMap = new HashMap<>();
        for (int i = 0; i < count; i++) {
            populatedMap.put(keys[i], value);
        }
        populatedSortedStringArrayMap = new SortedArrayStringMap();
        for (int i = 0; i < count; i++) {
            populatedSortedStringArrayMap.putValue(keys[i], value);
        }
        populatedOpenHashContextData = new OpenHashStringMap<>();
        for (int i = 0; i < count; i++) {
            populatedOpenHashContextData.putValue(keys[i], value);
        }
    }

    @Benchmark
    public SortedArrayStringMap putAllArrayContextData() {
        sortedStringArrayMap.clear();
        sortedStringArrayMap.putAll(populatedSortedStringArrayMap);
        return sortedStringArrayMap;
    }

    @Benchmark
    public OpenHashStringMap<String, Object> putAllHashContextData() {
        openHashMapContextData.clear();
        openHashMapContextData.putAll(populatedOpenHashContextData);
        return openHashMapContextData;
    }

    @Benchmark
    public Map putAllMap() {
        map.clear();
        map.putAll(populatedMap);
        return map;
    }

    @Benchmark
    public SortedArrayStringMap cloneArrayContextData() {
        return new SortedArrayStringMap(populatedSortedStringArrayMap);
    }

    @Benchmark
    public OpenHashStringMap<String, Object> cloneHashContextData() {
        return new OpenHashStringMap<>(populatedOpenHashContextData);
    }

    @Benchmark
    public Map cloneMap() {
        return new HashMap(populatedMap);
    }

    static TriConsumer<String, Object, int[]> COUNTER = new TriConsumer<String, Object, int[]>() {
        @Override
        public void accept(final String s, final Object o, final int[] result) {
            result[0] += s.hashCode() + o.hashCode();
        }
    };

    @Benchmark
    public int iterateArrayContextDataTriConsumer() {
        final int[] result = {0};

        populatedSortedStringArrayMap.forEach(COUNTER, result);
        return result[0];
    }

    @Benchmark
    public int iterateHashContextDataTriConsumer() {
        final int[] result = {0};

        populatedOpenHashContextData.forEach(COUNTER, result);
        return result[0];
    }

    @Benchmark
    public int iterateArrayContextDataBiConsumer() {
        final int[] result = {0};

        populatedSortedStringArrayMap.forEach(new BiConsumer<String, Object>() {
            @Override
            public void accept(final String s, final Object o) {
                result[0] += s.hashCode() + o.hashCode();
            }
        });
        return result[0];
    }

    @Benchmark
    public int iterateHashContextDataBiConsumer() {
        final int[] result = {0};

        populatedOpenHashContextData.forEach(new BiConsumer<String, Object>() {
            @Override
            public void accept(final String s, final Object o) {
                result[0] += s.hashCode() + o.hashCode();
            }
        });
        return result[0];
    }

    @Benchmark
    public int iterateMap() {
        final int[] result = {0};

        for (final Map.Entry<String, Object> entry : populatedMap.entrySet()) {
            result[0] += entry.getKey().hashCode() + entry.getValue().hashCode();
        }
        return result[0];
    }

    @Benchmark
    public Object getValueArrayContextData() {
        return populatedSortedStringArrayMap.getValue(keys[count - 1]);
    }

    @Benchmark
    public Object getValueHashContextData() {
        return populatedOpenHashContextData.getValue(keys[count - 1]);
    }

    @Benchmark
    public Object getValueMap() {
        return populatedMap.get(keys[count - 1]);
    }

    @Benchmark
    public int putArrayContextData() {
        populatedSortedStringArrayMap.putValue("someKey", "someValue");
        return populatedSortedStringArrayMap.size();
    }

    @Benchmark
    public int putHashContextData() {
        openHashMapContextData.put("someKey", "someValue");
        return openHashMapContextData.size();
    }

    @Benchmark
    public int putMap() {
        map.put("someKey", "someValue");
        return map.size();
    }
}