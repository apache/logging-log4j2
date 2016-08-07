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

import org.apache.logging.log4j.core.impl.ArrayContextData;
import org.apache.logging.log4j.core.impl.OpenHashMapContextData;
import org.apache.logging.log4j.core.util.BiConsumer;
import org.apache.logging.log4j.core.util.TriConsumer;
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
 * Compares performance of ArrayContextDataVsHashMapBenchmark.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
// (Quick build: mvn -DskipTests=true clean package -pl log4j-perf -am )
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*ArrayContextDataVsHashMapBenchmark.*" -f 1 -wi 10 -i 20 -tu ns -bm sample
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
public class ArrayContextDataVsHashMapBenchmark {

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
    private ArrayContextData contextData;
    private OpenHashMapContextData<String, Object> hashContextData;

    private HashMap<String, Object> populatedMap;
    private ArrayContextData populatedContextData;
    private OpenHashMapContextData<String, Object> populatedHashContextData;

    @Setup
    public void setup() {
        hashContextData = new OpenHashMapContextData<>();
        contextData = new ArrayContextData();
        map = new HashMap<>();

        keys = new String[count];
        Random r = new Random();
        for (int j = 0; j < keys.length; j++) {
            char[] str = new char[length];
            for (int i = 0; i < str.length; i++) {
                str[i] = (char) r.nextInt();
            }
            keys[j] = new String(str);
        }

        populatedMap = new HashMap<>();
        for (int i = 0; i < count; i++) {
            populatedMap.put(keys[i], value);
        }
        populatedContextData = new ArrayContextData();
        for (int i = 0; i < count; i++) {
            populatedContextData.putValue(keys[i], value);
        }
        populatedHashContextData = new OpenHashMapContextData<>();
        for (int i = 0; i < count; i++) {
            populatedHashContextData.putValue(keys[i], value);
        }
    }

    @Benchmark
    public ArrayContextData putAllArrayContextData() {
        contextData.clear();
        contextData.putAll(populatedContextData);
        return contextData;
    }

    @Benchmark
    public OpenHashMapContextData<String, Object> putAllHashContextData() {
        hashContextData.clear();
        hashContextData.putAll(populatedHashContextData);
        return hashContextData;
    }

    @Benchmark
    public Map putAllMap() {
        map.clear();
        map.putAll(populatedMap);
        return map;
    }

    @Benchmark
    public ArrayContextData cloneArrayContextData() {
        return new ArrayContextData(populatedContextData);
    }

    @Benchmark
    public OpenHashMapContextData<String, Object> cloneHashContextData() {
        return new OpenHashMapContextData<>(populatedHashContextData);
    }

    @Benchmark
    public Map cloneMap() {
        return new HashMap(populatedMap);
    }

    static TriConsumer<String, Object, int[]> COUNTER = new TriConsumer<String, Object, int[]>() {
        @Override
        public void accept(String s, Object o, int[] result) {
            result[0] += s.hashCode() + o.hashCode();
        }
    };

    @Benchmark
    public int iterateArrayContextDataTriConsumer() {
        final int[] result = {0};

        populatedContextData.forEach(COUNTER, result);
        return result[0];
    }

    @Benchmark
    public int iterateHashContextDataTriConsumer() {
        final int[] result = {0};

        populatedHashContextData.forEach(COUNTER, result);
        return result[0];
    }

    @Benchmark
    public int iterateArrayContextDataBiConsumer() {
        final int[] result = {0};

        populatedContextData.forEach(new BiConsumer<String, Object>() {
            @Override
            public void accept(String s, Object o) {
                result[0] += s.hashCode() + o.hashCode();
            }
        });
        return result[0];
    }

    @Benchmark
    public int iterateHashContextDataBiConsumer() {
        final int[] result = {0};

        populatedHashContextData.forEach(new BiConsumer<String, Object>() {
            @Override
            public void accept(String s, Object o) {
                result[0] += s.hashCode() + o.hashCode();
            }
        });
        return result[0];
    }

    @Benchmark
    public int iterateMap() {
        final int[] result = {0};

        for (Map.Entry<String, Object> entry : populatedMap.entrySet()) {
            result[0] += entry.getKey().hashCode() + entry.getValue().hashCode();
        }
        return result[0];
    }

    @Benchmark
    public int getValueArrayContextData() {
        String[] theKeys = keys;
        int c = count;
        int result = 0;
        for (int i = 0; i < c; i++) {
            Object val = populatedContextData.getValue(theKeys[i]);
            result += val.hashCode();// + theKeys[i].hashCode();
        }
        return result;
    }

    @Benchmark
    public int getValueHashContextData() {
        String[] theKeys = keys;
        int c = count;
        int result = 0;
        for (int i = 0; i < c; i++) {
            Object val = populatedHashContextData.getValue(theKeys[i]);
            result += val.hashCode();// + theKeys[i].hashCode();
        }
        return result;
    }

    @Benchmark
    public int getValueMap() {
        String[] theKeys = keys;
        int c = count;
        int result = 0;
        for (int i = 0; i < c; i++) {
            Object val = populatedMap.get(theKeys[i]);
            result += val.hashCode();// + theKeys[i].hashCode();
        }
        return result;
    }

    @Benchmark
    public int putArrayContextData() {
        String[] theKeys = keys;
        int c = count;
        for (int i = 0; i < c; i++) {
            contextData.putValue(theKeys[i], value);
        }
        return contextData.size();
    }

    @Benchmark
    public int putHashContextData() {
        String[] theKeys = keys;
        int c = count;
        for (int i = 0; i < c; i++) {
            hashContextData.putValue(theKeys[i], value);
        }
        return hashContextData.size();
    }

    @Benchmark
    public int putMap() {
        String[] theKeys = keys;
        int c = count;
        for (int i = 0; i < c; i++) {
            map.put(theKeys[i], value);
        }
        return map.size();
    }
}